package engine.render;

import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * [Engine/Render/Texture]
 * 모든 에셋의 픽셀 데이터를 관리.
 * 벽 / 스프라이트 텍스처 샘플링 담당.
 */
public class Texture {

    // =========================
    // Wall Texture
    // =========================

    private Map<Integer, int[]> wallTextures = new HashMap<>();

    private Map<Integer, Integer> wallWidthMap = new HashMap<>();
    private Map<Integer, Integer> wallHeightMap = new HashMap<>();

    // =========================
    // Sprite Texture
    // =========================

    // 구조:
    // <AssetName, [Direction][Frame][Pixels]>
    private Map<String, int[][][]> assetLibrary = new HashMap<>();

    private Map<String, Integer> widthMap = new HashMap<>();
    private Map<String, Integer> heightMap = new HashMap<>();

    /**
     * 벽 텍스처 등록
     */
    public void addWallTexture(
            int id,
            int width,
            int height,
            int[] pixels) {

        wallTextures.put(id, pixels);

        wallWidthMap.put(id, width);
        wallHeightMap.put(id, height);
    }

    /**
     * 스프라이트 에셋 등록
     */
    public void addAsset(
            String name,
            int dirCount,
            int frameCount,
            int width,
            int height) {

        assetLibrary.put(
                name,
                new int[dirCount][frameCount][]
        );

        widthMap.put(name, width);
        heightMap.put(name, height);
    }

    /**
     * 프레임 픽셀 데이터 주입
     */
    public void setPixels(
            String name,
            int dir,
            int frame,
            int[] pixels) {

        int[][][] asset = assetLibrary.get(name);

        if (asset == null) {
            return;
        }

        asset[dir][frame] = pixels;
    }

    /**
     * 스프라이트 픽셀 샘플링
     */
    public int getPixel(
            String name,
            int dir,
            int frame,
            double u,
            double v) {

        int[][][] asset = assetLibrary.get(name);

        if (asset == null) {
            return 0xFFFF00FF;
        }

        dir = Math.max(
                0,
                Math.min(dir, asset.length - 1)
        );

        frame = Math.max(
                0,
                Math.min(frame, asset[dir].length - 1)
        );

        int[] pixels = asset[dir][frame];

        if (pixels == null) {
            return 0x00000000;
        }

        int texW = widthMap.get(name);
        int texH = heightMap.get(name);

        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int x = (int)(u * (texW - 1));
        int y = (int)(v * (texH - 1));

        return pixels[x + y * texW];
    }

    /**
     * 벽 텍스처 샘플링
     */
    public int getWallPixel(
            int id,
            double u,
            double v) {

        int[] pixels = wallTextures.get(id);

        if (pixels == null) {
            return 0xFFFF00FF;
        }

        int texW = wallWidthMap.get(id);
        int texH = wallHeightMap.get(id);

        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int x = (int)(u * (texW - 1));
        int y = (int)(v * (texH - 1));

        return pixels[x + y * texW];
    }

    /**
     * NPC 방향 인덱스 계산
     */
    public int calculateDirIndex(
            double npcRotation,
            double relativeAngle) {

                double angle =
                        (npcRotation - relativeAngle + Math.PI * 2)
                        % (Math.PI * 2);

                return (int)Math.round(
                        angle / (Math.PI / 4)
                ) % 8;
            }
            public void loadWallTexture(
                int id,
                String path
        ) {

            try {

                BufferedImage img =
                        ImageIO.read(new File(path));

                int w = img.getWidth();
                int h = img.getHeight();

                int[] pixels = new int[w * h];

                img.getRGB(
                        0,
                        0,
                        w,
                        h,
                        pixels,
                        0,
                        w
                );

                addWallTexture(
                        id,
                        w,
                        h,
                        pixels
                );

                System.out.println(
                        "[TEXTURE] Loaded wall texture: " + path
                );

            } catch (Exception e) {

                System.err.println(
                        "[TEXTURE ERROR] Failed: " + path
                );

            e.printStackTrace();
        }
    }
}