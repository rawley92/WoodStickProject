package engine.render;

import engine.Scene;
import engine.Entity.CameraComponent;
import engine.Entity.Entity;
import engine.Entity.PhysicsComponent;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * 2.5D 레이캐스팅 기반으로 현재 씬을 프레임버퍼에 그리는 중심 렌더러다.
 * 벽/바닥/천장 렌더링을 맡고, 스프라이트와 UI 합성은 전용 객체에 위임한다.
 */
public class RenderCore {

    private final int width;
    private final int height;

    private final BufferedImage frameBuffer;
    private final int[] pixels;
    private final double[] zBuffer;

    private final Texture textureManager;
    private final Shader shader;
    private final SpriteRenderer spriteRenderer;

    /**
     * 레이캐스팅 렌더러가 사용할 프레임버퍼와 보조 버퍼를 생성한다.
     * 실제 렌더 단계는 render()에서 바닥/천장, 벽, 스프라이트, UI 메서드로 나누어 수행한다.
     */
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

    /**
     * 화면 출력에 사용할 최종 프레임버퍼 이미지를 반환한다.
     */
    public BufferedImage getFrameBuffer() {
        return frameBuffer;
    }

    /**
     * 현재 씬을 한 프레임으로 렌더링한다.
     * 구체적인 렌더링 단계는 clear(), renderFloorAndCeiling(), renderWalls(),
     * SpriteRenderer.render(), UI 합성으로 분리되어 있다.
     */
    public void render(Scene scene, UiManager uiManager) {

        clear();

        Entity player = scene.getPlayer();
        if (player == null || player.physics == null) return;

        // 각 화면 컬럼별 가장 가까운 벽 깊이를 초기화한다.
        // 이후 스프라이트 렌더링에서 벽 뒤 픽셀을 제외하는 데 사용한다.
        for (int x = 0; x < width; x++) zBuffer[x] = Double.MAX_VALUE;

        renderFloorAndCeiling(player);
        renderWalls(scene, player);
        spriteRenderer.render(scene, pixels, width, height, zBuffer);

        if (uiManager != null) {
            renderUiOverlay(scene, uiManager);
            uiManager.renderDrawCommands(frameBuffer);
        }
    }

    /**
     * 프레임버퍼를 기본 배경색으로 초기화한다.
     */
    private void clear() {
        Arrays.fill(pixels, 0xFF000000);
    }

    /**
     * 엔티티에 붙은 UiComponent 이미지를 프레임버퍼 픽셀 배열에 직접 합성한다.
     * Lua draw command 기반 UI는 UiManager.renderDrawCommands()에서 별도로 처리한다.
     */
    private void renderUiOverlay(Scene scene, UiManager uiManager) {
        for (Entity entity : scene.getEntities()) {
            if (entity.ui == null) continue;
            if (!entity.ui.visible) continue;

            String textureId = entity.ui.currentTextureId != null
                    ? entity.ui.currentTextureId
                    : entity.ui.uiId;

            int[] uiPixels = uiManager.getPixels(textureId);
            if (uiPixels == null) continue;

            int uiW = uiManager.getWidth(textureId);
            int uiH = uiManager.getHeight(textureId);

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

    /**
     * 플레이어 카메라 기준으로 바닥과 천장 텍스처를 투영한다.
     * 각 화면 행의 월드 좌표를 계산한 뒤 Texture의 전역 flat 텍스처를 샘플링한다.
     */
    private void renderFloorAndCeiling(Entity player) {

        PhysicsComponent phys = player.physics;
        CameraComponent cam = player.camera;

        // 화면 왼쪽/오른쪽 끝에서 나가는 카메라 ray 방향을 미리 계산한다.
        double rayDirX0 = cam.dirX - cam.planeX;
        double rayDirY0 = cam.dirY - cam.planeY;
        double rayDirX1 = cam.dirX + cam.planeX;
        double rayDirY1 = cam.dirY + cam.planeY;

        int halfHeight = height / 2;

        for (int y = halfHeight; y < height; y++) {

            // 화면 중앙선에서 멀어질수록 가까운 바닥/천장을 샘플링한다.
            double p = y - halfHeight;
            double posZ = 0.5 * height;
            double rowDistance = posZ / p;

            // 같은 화면 행에서 x가 1픽셀 이동할 때 월드 좌표가 얼마나 변하는지 계산한다.
            double stepX = rowDistance * (rayDirX1 - rayDirX0) / width;
            double stepY = rowDistance * (rayDirY1 - rayDirY0) / width;

            // 행의 첫 픽셀에 대응하는 월드 바닥 좌표다.
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
                // 같은 월드 좌표를 위쪽 대칭 행에 찍어 천장을 표현한다.
                pixels[x + (height - y) * width] = ceilingColor;

                floorX += stepX;
                floorY += stepY;
            }
        }
    }

    /**
     * DDA 레이캐스팅으로 벽을 컬럼 단위로 렌더링한다.
     * 타일 충돌 탐색, 텍스처 좌표 계산, zBuffer 기록, 안개/측면 음영 적용을 포함한다.
     */
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
            // cameraX는 화면 x좌표를 -1.0~1.0의 카메라 평면 좌표로 정규화한 값이다.
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

            // DDA: 다음 세로/가로 격자선 중 더 가까운 쪽으로 한 칸씩 전진한다.
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

            // fisheye 왜곡을 줄이기 위해 ray 길이가 아니라 카메라 평면에 수직인 거리를 사용한다.
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

            // 화면 벽 높이와 텍스처 v 좌표의 증가량을 계산한다.
            double step = 1.0 / lineHeight;
            double texPos = (drawStart - height / 2.0 + lineHeight / 2.0) * step;

            int texId = scene.getTile(mapX, mapY);

            for (int y = drawStart; y < drawEnd; y++) {
                double v = texPos;
                texPos += step;

                int color = textureManager.getWallPixel(texId, wallX, v);

                if (side == 1) {
                    // y축 방향 벽은 약간 어둡게 칠해 면 구분을 만든다.
                    color = shader.tint(color, 0x000000, 0.25);
                }

                color = shader.applyFog(color, perpWallDist);

                pixels[x + y * width] = color;
            }
        }
    }
}
