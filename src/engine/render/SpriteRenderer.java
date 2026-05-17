package engine.render;

import engine.Scene;
import engine.Entity.Entity;

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
        // Safety check to ensure player and necessary components exist
        if (player == null || player.physics == null || player.camera == null) return;

        List<Entity> sprites = new ArrayList<>();

        for (Entity e : scene.getEntities()) {
            // Only collect active, non-player entities that have physics and rendering components
            if (e != player && e.physics != null && e.render != null) {
                sprites.add(e);
            }
        }

        // ✔ Sort sprites by distance using component properties (Far to Near)
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
        // Access coordinates safely via the PhysicsComponent
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
        // Access relative positions through the physics component
        double spriteX = sprite.physics.x - player.physics.x;
        double spriteY = sprite.physics.y - player.physics.y;

        // Assuming direction and camera plane properties live inside CameraComponent
        double dirX = player.camera.dirX;
        double dirY = player.camera.dirY;
        double planeX = player.camera.planeX;
        double planeY = player.camera.planeY;

        double invDet = 1.0 / (planeX * dirY - dirX * planeY);

        double transformX = invDet * (dirY * spriteX - dirX * spriteY);
        double transformY = invDet * (-planeY * spriteX + planeX * spriteY);

        if (transformY <= 0.01) return; // Sprite is behind the screen or too close

        int spriteScreenX = (int)((width / 2.0) * (1 + transformX / transformY));

        int spriteHeight = Math.abs((int)(height / transformY));

        int drawStartY = -spriteHeight / 2 + height / 2;
        int drawEndY = spriteHeight / 2 + height / 2;

        drawStartY = Math.max(0, drawStartY);
        drawEndY = Math.min(height - 1, drawEndY);

        int spriteWidth = Math.abs((int)(height / transformY));

        int drawStartX = -spriteWidth / 2 + spriteScreenX;
        int drawEndX = spriteWidth / 2 + spriteScreenX;

        drawStartX = Math.max(0, drawStartX);
        drawEndX = Math.min(width - 1, drawEndX);

        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {

            if (transformY > 0 &&
                stripe >= 0 &&
                stripe < width &&
                transformY < zBuffer[stripe]) {

                double u = (stripe - drawStartX) / (double)spriteWidth;

                for (int y = drawStartY; y < drawEndY; y++) {

                    double v = (y - drawStartY) / (double)spriteHeight;

                    // Assuming currentFrame is defined inside your RenderComponent.
                    // If it's not yet implemented, you can fallback to a default value like 0.
                    int currentFrame = (sprite.render != null) ? sprite.render.currentFrame : 0;

                    int color = textureManager.getPixel(
                            sprite.assetId,
                            0,
                            currentFrame,
                            u,
                            v
                    );

                    int alpha = (color >> 24) & 0xFF;
                    if (alpha == 0) continue; // Skip transparent pixels

                    color = shader.applyFog(color, transformY);

                    pixels[stripe + y * width] = color;
                }
            }
        }
    }
}