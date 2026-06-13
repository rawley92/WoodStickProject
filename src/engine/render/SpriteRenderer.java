package engine.render;

import engine.Scene;
import engine.Entity.Entity;
import engine.boot.AssetRegistry;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * 씬의 활성 엔티티 이미지를 카메라 공간으로 투영해 프레임버퍼에 합성한다.
 * 벽 렌더링에서 만들어진 zBuffer를 사용해 가려짐을 처리한다.
 */
public class SpriteRenderer {

    private static final double MIN_VISIBLE_DEPTH = 0.12;
    private static final double MIN_PROJECTED_DEPTH = 0.65;

    private final Shader shader;

    private final Map<Integer, int[]> spriteCache = new HashMap<>();
    private final Map<Integer, Integer> widthCache = new HashMap<>();
    private final Map<Integer, Integer> heightCache = new HashMap<>();

    /**
     * 스프라이트 렌더러를 생성한다.
     * 실제 텍스처 로드는 엔티티별 assetId를 기준으로 loadSprite()에서 지연 수행한다.
     */
    public SpriteRenderer(Texture ignored, Shader shader) {
        this.shader = shader;
    }

    /**
     * 엔티티의 assetId에 해당하는 스프라이트 이미지를 로드하고 캐싱한다.
     * AssetRegistry 경로 해석과 ImageIO 픽셀 추출이 이 메서드 안에서 처리된다.
     */
    private int[] loadSprite(Entity sprite) {

        String id = sprite.assetId;

        if (id == null || id.isEmpty()) return null;

        // 스프라이트도 AssetRegistry index를 캐시 키로 사용한다.
        // 같은 이미지가 여러 엔티티에서 쓰여도 파일은 한 번만 읽는다.
        int assetIndex = AssetRegistry.getIndex(id);
        if (assetIndex < 0) {
            System.err.println("[SPRITE ERROR] Missing asset index: " + id);
            return null;
        }

        if (spriteCache.containsKey(assetIndex)) {
            return spriteCache.get(assetIndex);
        }

        String path = AssetRegistry.getPath(assetIndex);
        if (path == null) {
            System.err.println("[SPRITE ERROR] Missing asset: " + id);
            return null;
        }

        try {
            BufferedImage img = ImageIO.read(new File(path));
            int w = img.getWidth();
            int h = img.getHeight();

            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);

            spriteCache.put(assetIndex, pixels);
            widthCache.put(assetIndex, w);
            heightCache.put(assetIndex, h);

            return pixels;

        } catch (Exception e) {
            System.err.println("[SPRITE ERROR] Failed loading: " + path);
            return null;
        }
    }

    /**
     * 현재 씬의 활성 스프라이트 엔티티를 렌더링한다.
     * 정렬과 개별 투영 처리는 distanceSq(), renderSprite()로 분리되어 있다.
     */
    public void render(
            Scene scene,
            int[] pixels,
            int width,
            int height,
            double[] zBuffer
    ) {

        Entity player = scene.getPlayer();
        if (player == null || player.physics == null || player.camera == null) return;

        List<Entity> sprites = new ArrayList<>();

        for (Entity e : scene.getEntities()) {
            // 플레이어와 비활성/파괴/무이미지 엔티티는 스프라이트 렌더 대상이 아니다.
            if (e != player
                    && e.isActive
                    && !e.isDestroyed
                    && e.physics != null
                    && e.assetId != null
                    && !e.assetId.isEmpty()) {
                sprites.add(e);
            }
        }

        // 원근 합성을 위해 먼 스프라이트부터 먼저 그린다.
        // 가까운 스프라이트가 뒤에 그려지며 앞쪽 픽셀을 덮는다.
        sprites.sort(
                Comparator.comparingDouble((Entity e) ->
                        distanceSq(player, e)
                ).reversed()
        );

        for (Entity sprite : sprites) {
            renderSprite(sprite, player, pixels, width, height, zBuffer);
        }
    }

    /**
     * 두 엔티티 사이의 제곱 거리를 계산한다.
     * 실제 거리값이 필요 없으므로 정렬 비용을 줄이기 위해 제곱근을 생략한다.
     */
    private double distanceSq(Entity a, Entity b) {
        double dx = a.physics.x - b.physics.x;
        double dy = a.physics.y - b.physics.y;
        return dx * dx + dy * dy;
    }

    /**
     * 단일 스프라이트를 카메라 공간에 투영해 화면 픽셀로 합성한다.
     * 깊이 판정은 zBuffer를 사용하고, 색상 후처리는 Shader에 위임한다.
     */
    private void renderSprite(
            Entity sprite,
            Entity player,
            int[] pixels,
            int width,
            int height,
            double[] zBuffer
    ) {

        int[] texture = loadSprite(sprite);
        if (texture == null) return;

        int assetIndex = AssetRegistry.getIndex(sprite.assetId);
        int texW = widthCache.get(assetIndex);
        int texH = heightCache.get(assetIndex);

        // 플레이어 기준 상대 위치로 변환한다.
        double spriteX = sprite.physics.x - player.physics.x;
        double spriteY = sprite.physics.y - player.physics.y;

        // 카메라 행렬의 역행렬 계수다.
        // 월드 상대 좌표를 카메라 공간(transformX/Y)으로 바꾸는 데 사용한다.
        double invDet =
                1.0 / (player.camera.planeX * player.camera.dirY
                     - player.camera.dirX * player.camera.planeY);

        double transformX = invDet * (player.camera.dirY * spriteX - player.camera.dirX * spriteY);

        double transformY = invDet * (-player.camera.planeY * spriteX + player.camera.planeX * spriteY);

        if (transformY <= MIN_VISIBLE_DEPTH) return;

        // 너무 가까운 스프라이트가 화면을 과도하게 덮지 않도록 투영 깊이에 하한을 둔다.
        double projectedDepth = Math.max(MIN_PROJECTED_DEPTH, transformY);

        int spriteScreenX = (int)((width / 2.0) * (1.0 + transformX / projectedDepth));

        // int spriteHeight = Math.abs((int)(height / transformY));
        // int spriteWidth  = Math.abs((int)(height / transformY));

        double spriteScale = sprite.render != null ? sprite.render.scale : 1.0;

        // 높이는 거리의 역수에 비례하고, 폭은 원본 이미지 종횡비를 유지한다.
        int spriteHeight = Math.abs((int)((height / projectedDepth) * spriteScale));

        int spriteWidth = Math.abs((int)(spriteHeight * (texW / (double)texH)));

        int drawStartY = Math.max(0, -spriteHeight / 2 + height / 2);
        int drawEndY   = Math.min(height - 1, spriteHeight / 2 + height / 2);

        int drawStartX = Math.max(0, -spriteWidth / 2 + spriteScreenX);
        int drawEndX   = Math.min(width - 1, spriteWidth / 2 + spriteScreenX);

        if (drawEndX <= drawStartX || drawEndY <= drawStartY) return;

        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {

            if (stripe < 0 || stripe >= width) continue;

            // 해당 화면 컬럼의 벽보다 뒤에 있으면 그리지 않는다.
            if (transformY >= zBuffer[stripe]) continue;

            double u = (stripe - drawStartX) / (double)(drawEndX - drawStartX);

            int texX = (int)(u * (texW - 1));

            for (int y = drawStartY; y < drawEndY; y++) {

                double v = (y - drawStartY) / (double)(drawEndY - drawStartY);
                int texY = (int)(v * (texH - 1));

                int color = texture[texX + texY * texW];

                // 검정 투명 배경을 가진 스프라이트를 투명 픽셀처럼 취급한다.
                if ((color & 0x00FFFFFF) == 0) continue;

                color = color | 0xFF000000;
                color = shader.applyFog(color, transformY);

                pixels[stripe + y * width] = color;
            }
        }
    }
}
