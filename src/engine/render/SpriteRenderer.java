package engine.render;

import engine.Scene;
import engine.Entity.Entity;
import engine.boot.AssetRegistry;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import javax.imageio.ImageIO;

public class SpriteRenderer {

    private static final double MIN_VISIBLE_DEPTH = 0.12;
    private static final double MIN_PROJECTED_DEPTH = 0.65;

    private final Shader shader;

    private final Map<Integer, int[]> spriteCache = new HashMap<>();
    private final Map<Integer, Integer> widthCache = new HashMap<>();
    private final Map<Integer, Integer> heightCache = new HashMap<>();

    public SpriteRenderer(Texture ignored, Shader shader) {
        this.shader = shader;
    }
    private int[] loadSprite(Entity sprite) {

        String id = sprite.assetId;

        if (id == null || id.isEmpty()) return null;

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
            if (e != player
                    && e.isActive
                    && !e.isDestroyed
                    && e.physics != null
                    && e.assetId != null
                    && !e.assetId.isEmpty()) {
                sprites.add(e);
            }
        }

        sprites.sort(
                Comparator.comparingDouble((Entity e) ->
                        distanceSq(player, e)
                ).reversed()
        );

        for (Entity sprite : sprites) {
            renderSprite(sprite, player, pixels, width, height, zBuffer);
        }
    }

    private double distanceSq(Entity a, Entity b) {
        double dx = a.physics.x - b.physics.x;
        double dy = a.physics.y - b.physics.y;
        return dx * dx + dy * dy;
    }

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

        double spriteX = sprite.physics.x - player.physics.x;
        double spriteY = sprite.physics.y - player.physics.y;

        double invDet =
                1.0 / (player.camera.planeX * player.camera.dirY
                     - player.camera.dirX * player.camera.planeY);

        double transformX = invDet * (player.camera.dirY * spriteX - player.camera.dirX * spriteY);

        double transformY = invDet * (-player.camera.planeY * spriteX + player.camera.planeX * spriteY);

        if (transformY <= MIN_VISIBLE_DEPTH) return;

        double projectedDepth = Math.max(MIN_PROJECTED_DEPTH, transformY);

        int spriteScreenX = (int)((width / 2.0) * (1.0 + transformX / projectedDepth));

        // int spriteHeight = Math.abs((int)(height / transformY));
        // int spriteWidth  = Math.abs((int)(height / transformY));

        double spriteScale = sprite.render != null ? sprite.render.scale : 1.0;

        int spriteHeight = Math.abs((int)((height / projectedDepth) * spriteScale));

        int spriteWidth = Math.abs((int)(spriteHeight * (texW / (double)texH)));

        int drawStartY = Math.max(0, -spriteHeight / 2 + height / 2);
        int drawEndY   = Math.min(height - 1, spriteHeight / 2 + height / 2);

        int drawStartX = Math.max(0, -spriteWidth / 2 + spriteScreenX);
        int drawEndX   = Math.min(width - 1, spriteWidth / 2 + spriteScreenX);

        if (drawEndX <= drawStartX || drawEndY <= drawStartY) return;

        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {

            if (stripe < 0 || stripe >= width) continue;

            if (transformY >= zBuffer[stripe]) continue;

            double u = (stripe - drawStartX) / (double)(drawEndX - drawStartX);

            int texX = (int)(u * (texW - 1));

            for (int y = drawStartY; y < drawEndY; y++) {

                double v = (y - drawStartY) / (double)(drawEndY - drawStartY);
                int texY = (int)(v * (texH - 1));

                int color = texture[texX + texY * texW];

                if ((color & 0x00FFFFFF) == 0) continue;

                color = color | 0xFF000000;
                color = shader.applyFog(color, transformY);

                pixels[stripe + y * width] = color;
            }
        }
    }
}
