package engine.render;

import engine.Entity;
import engine.Scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [Engine/Render/SpriteRenderer]
 * 레이캐스트 월드 위에 스프라이트(NPC, 아이템)를 투영 렌더링.
 *
 * 기능:
 * - 거리 기반 정렬
 * - 카메라 공간 변환
 * - 3D 원근 투영
 * - ZBuffer 기반 벽 가림 처리
 * - 방향 스프라이트(8방향)
 * - 투명 픽셀 처리
 */
public class SpriteRenderer {

    private final Texture textureManager;
    private final Shader shader;

    public SpriteRenderer(Texture textureManager, Shader shader) {
        this.textureManager = textureManager;
        this.shader = shader;
    }

    /**
     * 스프라이트 전체 렌더링
     */
    public void render(
            Scene scene,
            int[] pixels,
            int width,
            int height,
            double[] zBuffer
    ) {

        Entity player = scene.getPlayer();

        if (player == null) {
            return;
        }

        List<Entity> sprites = new ArrayList<>();

        // 플레이어 제외
        for (Entity e : scene.getEntities()) {
            if (e != player) {
                sprites.add(e);
            }
        }

        // 거리 계산
        for (Entity sprite : sprites) {

            double dx = sprite.x - player.x;
            double dy = sprite.y - player.y;

            sprite.distSq = dx * dx + dy * dy;
        }

        // 먼 순서부터 정렬
        sprites.sort(
                Comparator.comparingDouble((Entity e) -> e.distSq)
                        .reversed()
        );

        // 개별 스프라이트 렌더
        for (Entity sprite : sprites) {

            renderSprite(
                    sprite,
                    player,
                    pixels,
                    width,
                    height,
                    zBuffer
            );
        }
    }

    /**
     * 단일 스프라이트 렌더
     */
    private void renderSprite(
            Entity sprite,
            Entity player,
            int[] pixels,
            int width,
            int height,
            double[] zBuffer
    ) {

        // ===== 카메라 상대 좌표 =====

        double spriteX = sprite.x - player.x;
        double spriteY = sprite.y - player.y;

        // ===== 역행렬 계산 =====

        double invDet =
                1.0 /
                (
                    player.planeX * player.dirY
                    - player.dirX * player.planeY
                );

        double transformX =
                invDet *
                (
                    player.dirY * spriteX
                    - player.dirX * spriteY
                );

        double transformY =
                invDet *
                (
                    -player.planeY * spriteX
                    + player.planeX * spriteY
                );

        // 카메라 뒤면 스킵
        if (transformY <= 0.01) {
            return;
        }

        // ===== 화면 위치 =====

        int spriteScreenX =
                (int)(
                        (width / 2.0)
                        * (1 + transformX / transformY)
                );

        // ===== 스프라이트 크기 =====

        int spriteHeight =
                Math.abs((int)(height / transformY));

        int drawStartY =
                -spriteHeight / 2 + height / 2;

        int drawEndY =
                spriteHeight / 2 + height / 2;

        if (drawStartY < 0) {
            drawStartY = 0;
        }

        if (drawEndY >= height) {
            drawEndY = height - 1;
        }

        int spriteWidth =
                Math.abs((int)(height / transformY));

        int drawStartX =
                -spriteWidth / 2 + spriteScreenX;

        int drawEndX =
                spriteWidth / 2 + spriteScreenX;

        if (drawStartX < 0) {
            drawStartX = 0;
        }

        if (drawEndX >= width) {
            drawEndX = width - 1;
        }

        // ===== 방향 스프라이트 계산 =====

        double relativeAngle =
                Math.atan2(
                        player.y - sprite.y,
                        player.x - sprite.x
                );

        int dirIndex =
                textureManager.calculateDirIndex(
                        sprite.rotation,
                        relativeAngle
                );

        // ===== 수직 스트라이프 렌더 =====

        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {

            // 텍스처 U좌표
            double u =
                    (stripe - drawStartX)
                    / (double)spriteWidth;

            // 벽 뒤 체크
            if (
                    transformY > 0
                    && stripe >= 0
                    && stripe < width
                    && transformY < zBuffer[stripe]
            ) {

                // 세로 픽셀
                for (int y = drawStartY; y < drawEndY; y++) {

                    double v =
                            (y - drawStartY)
                            / (double)spriteHeight;

                    int color =
                            textureManager.getPixel(
                                    sprite.assetId,
                                    dirIndex,
                                    sprite.currentFrame,
                                    u,
                                    v
                            );

                    // ===== 투명 픽셀 처리 =====

                    int alpha =
                            (color >> 24) & 0xFF;

                    if (alpha == 0) {
                        continue;
                    }

                    // ===== 거리 안개 =====

                    color =
                            shader.applyFog(
                                    color,
                                    transformY
                            );

                    pixels[
                            stripe + y * width
                    ] = color;
                }
            }
        }
    }
}