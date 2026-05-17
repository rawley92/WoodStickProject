package engine.render;

import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Texture {

    private Map<Integer, int[]> wallTextures = new HashMap<>();
    private Map<Integer, Integer> wallWidthMap = new HashMap<>();
    private Map<Integer, Integer> wallHeightMap = new HashMap<>();

    private Map<String, int[][][]> assetLibrary = new HashMap<>();
    private Map<String, Integer> widthMap = new HashMap<>();
    private Map<String, Integer> heightMap = new HashMap<>();

    private Map<String, int[]> wallAssetLibrary = new HashMap<>();
    private Map<String, Integer> wallAssetWidths = new HashMap<>();
    private Map<String, Integer> wallAssetHeights = new HashMap<>();

    public void addWallTexture(
            int id,
            int width,
            int height,
            int[] pixels) {

        wallTextures.put(id, pixels);

        wallWidthMap.put(id, width);
        wallHeightMap.put(id, height);
    }

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

    public int calculateDirIndex(double npcRotation, double relativeAngle) {
    
        double angle = relativeAngle - npcRotation;

        angle = angle % (Math.PI * 2);
        if (angle < 0) {
            angle += Math.PI * 2;
        }

        int dirIndex = (int) Math.floor((angle + Math.PI / 8.0) / (Math.PI / 4.0));
        
        return dirIndex % 8;
    }

    public boolean hasMultipleDirections(String assetId) {
        if (assetId.startsWith("item_") || assetId.equals("key") || assetId.equals("barrel")) {
            return false;
        }
        return true; 
    }

    public void addWallTextureWithStringId(String stringId, int width, int height, int[] pixels) {
        wallAssetLibrary.put(stringId, pixels);
        wallAssetWidths.put(stringId, width);
        wallAssetHeights.put(stringId, height);
    }

    public void bindIntIdToStringId(int intId, String stringId) {
        if (wallAssetLibrary.containsKey(stringId)) {
            addWallTexture(intId, wallAssetWidths.get(stringId), wallAssetHeights.get(stringId), wallAssetLibrary.get(stringId));
            System.out.println("[SCRIPT BIND] 맵 코드 " + intId + "번  [" + stringId + "] 텍스처 링크 완료.");
        } else {
            System.err.println("[SCRIPT ERROR] 명세서에 '" + stringId + "' 텍스처가 로드되지 않았습니다!");
        }
    }

    public void loadWallTexture(int id, String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];

            img.getRGB(0, 0, w, h, pixels, 0, w);
            addWallTexture(id, w, h, pixels);

            System.out.println("[TEXTURE] Loaded wall texture: " + path);
        } catch (Exception e) {
            System.err.println("[TEXTURE ERROR] Failed: " + path);
            e.printStackTrace();
        }
    }
}