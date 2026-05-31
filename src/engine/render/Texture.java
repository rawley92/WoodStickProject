package engine.render;

import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import engine.boot.AssetRegistry;

import java.awt.image.BufferedImage;
import java.io.File;

public class Texture {

    private Map<Integer, int[]> wallTextures = new HashMap<>();
    private Map<Integer, Integer> wallWidthMap = new HashMap<>();
    private Map<Integer, Integer> wallHeightMap = new HashMap<>();

    private Map<String, int[][][]> assetLibrary = new HashMap<>();
    private Map<String, Integer> widthMap = new HashMap<>();
    private Map<String, Integer> heightMap = new HashMap<>();

    private Map<Integer, int[]> wallAssetLibrary = new HashMap<>();
    private Map<Integer, Integer> wallAssetWidths = new HashMap<>();
    private Map<Integer, Integer> wallAssetHeights = new HashMap<>();

    private int globalFloorTextureIndex = -1;
    private int globalCeilingTextureIndex = -1;
    private Map<Integer, int[]> flatTextures = new HashMap<>(); // 바닥/천장용 이미지 저장소
    private Map<Integer, Integer> flatWidths = new HashMap<>();
    private Map<Integer, Integer> flatHeights = new HashMap<>();

    public void loadTextures(String[] textureIds) {
        for (String id : textureIds) {
            int assetIndex = AssetRegistry.getIndex(id);
            String path = AssetRegistry.getPath(assetIndex);
            
            if (path == null) {
                System.err.println("[SCENE LOADER] AssetRegistry에 등록되지 않은 ID: " + id);
                continue;
            }

            try {
                System.out.println("[SCENE LOADER] Loading texture -> " + id + " (" + path + ")");
                
                // 1. 이미지 파일 읽기
                File file = new File(path);
                if (!file.exists()) throw new Exception("파일 없음: " + path);
                
                BufferedImage img = ImageIO.read(file);
                int w = img.getWidth();
                int h = img.getHeight();
                int[] pixels = new int[w * h];
                img.getRGB(0, 0, w, h, pixels, 0, w);

                // 2. ID에 따라 알맞은 저장소에 등록
                if (id.contains("Wall")) {
                    // 벽 텍스처 등록
                    addWallTextureWithStringId(id, w, h, pixels);
                    System.out.println("[TEXTURE] Wall registered: " + id);
                } else {
                    // 그 외(바닥, 천장, UI 등)는 flat으로 등록
                    addFlatTexture(assetIndex, w, h, pixels);
                 
                    System.out.println("[TEXTURE] Flat registered: " + id);
                }
                
            } catch (Exception e) {
                System.err.println("[SCENE LOADER] 텍스처 로드 실패: " + id);
                e.printStackTrace();
            }
        }
    }

    public void addWallTexture(
            int id,
            int width,
            int height,
            int[] pixels) {

        wallTextures.put(id, pixels);

        wallWidthMap.put(id, width);
        wallHeightMap.put(id, height);
    }

    public void addFlatTexture(int index, int w, int h, int[] pixels) {
        flatTextures.put(index, pixels);
        flatWidths.put(index, w);
        flatHeights.put(index, h);
    }

    public void setGlobalFloorTexture(String id) {
        this.globalFloorTextureIndex = AssetRegistry.getIndex(id);
    }

    public void setGlobalCeilingTexture(String id) {
        this.globalCeilingTextureIndex = AssetRegistry.getIndex(id);
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
        int index = AssetRegistry.getIndex(stringId);
        if (index < 0) {
            System.err.println("[TEXTURE ERROR] 잘못된 wall asset id: " + stringId);
            return;
        }

        wallAssetLibrary.put(index, pixels);
        wallAssetWidths.put(index, width);
        wallAssetHeights.put(index, height);
    }

    public void bindIntIdToStringId(int intId, String stringId) {
        int assetIndex = AssetRegistry.getIndex(stringId);

        if (wallAssetLibrary.containsKey(assetIndex)) {
            addWallTexture(intId, wallAssetWidths.get(assetIndex), wallAssetHeights.get(assetIndex), wallAssetLibrary.get(assetIndex));
            System.out.println("[SCRIPT BIND] 맵 코드 " + intId + "번  [" + stringId + "] 텍스처 링크 완료.");
        } else {
            // [수정] 아래 디버그 로그 추가
            System.err.println("[SCRIPT ERROR] 명세서에 '" + stringId + "' 텍스처가 로드되지 않았습니다!");
            System.err.println("--- 현재 로드된 텍스처 목록 ---");
            for (Integer key : wallAssetLibrary.keySet()) {
                System.err.println("  - " + AssetRegistry.getIdByIndex(key));
            }
            System.err.println("------------------------------");
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

    public int getFloorPixel(double u, double v) {
        return getFlatPixel(globalFloorTextureIndex, u, v);
    }

    public int getCeilingPixel(double u, double v) {
        return getFlatPixel(globalCeilingTextureIndex, u, v);
    }

    private int getFlatPixel(int index, double u, double v) {
        int[] pixels = flatTextures.get(index);
        if (pixels == null) return 0xFFFF00FF; // 텍스처 없으면 핑크색

        int w = flatWidths.get(index);
        int h = flatHeights.get(index);

        // 0.0~1.0 범위를 텍스처 좌표로 변환
        int x = (int)((u - Math.floor(u)) * w);
        int y = (int)((v - Math.floor(v)) * h);

        return pixels[x + y * w];
    }
}
