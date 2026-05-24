package engine.render;

import engine.Scene;
import engine.Entity.CameraComponent;
import engine.Entity.Entity;
import engine.Entity.PhysicsComponent;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Map;

public class RenderCore {

    private final int width;
    private final int height;

    private final BufferedImage frameBuffer;
    private final int[] pixels;
    private final double[] zBuffer;

    private final Texture textureManager;
    private final Shader shader;
    private final SpriteRenderer spriteRenderer;

    public RenderCore(int width, int height, int scale, Texture textureManager) {
        this.width = width;
        this.height = height;

        this.frameBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();
        this.zBuffer = new double[width];

        this.textureManager = textureManager;
        this.shader = new Shader();
        this.spriteRenderer = new SpriteRenderer(textureManager, shader);
    }

    public BufferedImage getFrameBuffer() {
        return frameBuffer;
    }

    public void render(Scene scene, UiManager uiManager) {

        clear();

        Entity player = scene.getPlayer();
        if (player == null || player.physics == null) return;

        for (int x = 0; x < width; x++) zBuffer[x] = Double.MAX_VALUE;

        renderFloorAndCeiling(player);
        renderWalls(scene, player);
        spriteRenderer.render(scene, pixels, width, height, zBuffer);

        if (uiManager != null) {
            renderUiOverlay(scene, uiManager);
        }
    }
    private void clear() {
        Arrays.fill(pixels, 0xFF000000);
    }

    private void renderUiOverlay(Scene scene, UiManager uiManager) {
        for (Entity entity : scene.getEntities()) {
            if (entity.ui == null) continue;
            if (!entity.ui.visible) continue;

            int[] uiPixels = uiManager.getPixels(entity.ui.uiId);
            if (uiPixels == null) continue;

            int uiW = uiManager.getWidth(entity.ui.uiId);
            int uiH = uiManager.getHeight(entity.ui.uiId);

            int startX = entity.ui.x;
            int startY = entity.ui.y;

            for (int y = 0; y < uiH; y++) {
                int screenY = startY + y;
                if (screenY < 0 || screenY >= height) continue;

                for (int x = 0; x < uiW; x++) {
                    int screenX = startX + x;
                    if (screenX < 0 || screenX >= width) continue;

                    int color = uiPixels[x + y * uiW];
                    int alpha = (color >> 24) & 0xFF;

                    if (alpha == 0) continue;

                    pixels[screenX + screenY * width] = color;
            }
        }
    }
    }
    private void renderFloorAndCeiling(Entity player) {

        PhysicsComponent phys = player.physics;
        CameraComponent cam = player.camera;

        double rayDirX0 = cam.dirX - cam.planeX;
        double rayDirY0 = cam.dirY - cam.planeY;
        double rayDirX1 = cam.dirX + cam.planeX;
        double rayDirY1 = cam.dirY + cam.planeY;

        int halfHeight = height / 2;

        for (int y = halfHeight; y < height; y++) {

            double p = y - halfHeight;
            double posZ = 0.5 * height;
            double rowDistance = posZ / p;

            double stepX = rowDistance * (rayDirX1 - rayDirX0) / width;
            double stepY = rowDistance * (rayDirY1 - rayDirY0) / width;

            double floorX = phys.x + rowDistance * rayDirX0;
            double floorY = phys.y + rowDistance * rayDirY0;

            for (int x = 0; x < width; x++) {

                //int cellX = (int) floorX;
                //int cellY = (int) floorY;

                //boolean grid =
                //    (Math.abs(floorX - cellX) < 0.03) ||
                //    (Math.abs(floorY - cellY) < 0.03);

                int floorColor = textureManager.getFloorPixel(floorX, floorY);
                int ceilingColor = textureManager.getCeilingPixel(floorX, floorY);

                // int floorColor = grid ? 0xFF555555 : 0xFF444444;
                // int ceilingColor = grid ? 0xFF333333 : 0xFF222222;

                pixels[x + y * width] = floorColor;
                pixels[x + (height - y) * width] = ceilingColor;

                floorX += stepX;
                floorY += stepY;
            }
        }
    }

    private void renderWalls(Scene scene, Entity player) {
        PhysicsComponent phys = player.physics;
        CameraComponent cam = player.camera;

        double posX = phys.x;
        double posY = phys.y;

        double dirX = cam.dirX;
        double dirY = cam.dirY;

        double planeX = cam.planeX;
        double planeY = cam.planeY;        

        for (int x = 0; x < width; x++) {
            double cameraX = 2.0 * x / width - 1.0;

            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX;
            int mapY = (int) posY;

            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1.0 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1.0 / rayDirY);

            int stepX;
            int stepY;

            double sideDistX;
            double sideDistY;

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (posX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            }

            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (posY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;
            }

            int side = 0;
            boolean hit = false;

            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }

                if (mapX < 0 || mapX >= scene.getWidth() || mapY < 0 || mapY >= scene.getHeight()) {
                    hit = true;
                    continue;
                }

                if (scene.getTile(mapX, mapY) > 0) {
                    hit = true;
                }
            }

            double perpWallDist;

            if (side == 0) {
                perpWallDist = sideDistX - deltaDistX;
            } else {
                perpWallDist = sideDistY - deltaDistY;
            }

            zBuffer[x] = perpWallDist;

            int lineHeight = (int) (height / perpWallDist);

            int drawStart = Math.max(0, -lineHeight / 2 + height / 2);
            int drawEnd = Math.min(height - 1, lineHeight / 2 + height / 2);

            double wallX;

            if (side == 0) {
                wallX = posY + perpWallDist * rayDirY;
            } else {
                wallX = posX + perpWallDist * rayDirX;
            }

            wallX -= Math.floor(wallX);
            wallX = Math.max(0.0, Math.min(0.999, wallX));

            if (side == 0 && rayDirX < 0) {
                wallX = 1.0 - wallX;
            }

            if (side == 1 && rayDirY > 0) {
                wallX = 1.0 - wallX;
            }

            double step = 1.0 / lineHeight;
            double texPos = (drawStart - height / 2.0 + lineHeight / 2.0) * step;

            int texId = scene.getTile(mapX, mapY);

            for (int y = drawStart; y < drawEnd; y++) {
                double v = texPos;
                texPos += step;

                int color = textureManager.getWallPixel(texId, wallX, v);

                if (side == 1) {
                    color = shader.tint(color, 0x000000, 0.25);
                }

                color = shader.applyFog(color, perpWallDist);

                pixels[x + y * width] = color;
            }
        }
    }
}