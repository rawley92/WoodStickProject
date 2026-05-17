package engine.render;

import engine.Entity;
import engine.Scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class SpriteRenderer {

    private final Texture textureManager;
    private final Shader shader;

    public SpriteRenderer(Texture textureManager, Shader shader) {
        this.textureManager = textureManager;
        this.shader = shader;
    }

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

        for (Entity e : scene.getEntities()) {
            if (e != player) {
                sprites.add(e);
            }
        }

        for (Entity sprite : sprites) {

            double dx = sprite.x - player.x;
            double dy = sprite.y - player.y;

            sprite.distSq = dx * dx + dy * dy;
        }

        sprites.sort(Comparator.comparingDouble((Entity e) -> e.distSq).reversed());
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

    private void renderSprite(
            Entity sprite,
            Entity player,
            int[] pixels,
            int width,
            int height,
            double[] zBuffer
    ) {

        double spriteX = sprite.x - player.x;
        double spriteY = sprite.y - player.y;

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

        if (transformY <= 0.01) {
            return;
        }

        int spriteScreenX =
                (int)(
                        (width / 2.0)
                        * (1 + transformX / transformY)
                );

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
        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
            double u =  (stripe - drawStartX) / (double)spriteWidth;
            if (
                    transformY > 0
                    && stripe >= 0
                    && stripe < width
                    && transformY < zBuffer[stripe]
            ) {

                for (int y = drawStartY; y < drawEndY; y++) {

                    double v =
                            (y - drawStartY)
                            / (double)spriteHeight;

                    int color = textureManager.getPixel(sprite.assetId, dirIndex, sprite.currentFrame, u, v);
                    int alpha = (color >> 24) & 0xFF;

                    if (alpha == 0) {
                        continue;
                    }
                    color = shader.applyFog( color, transformY);
                    pixels[stripe + y * width] = color;
                }
            }
        }
    }
}