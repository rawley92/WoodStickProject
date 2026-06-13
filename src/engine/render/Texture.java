package engine.render;

import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import engine.boot.AssetRegistry;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 벽, 바닥, 천장, 보조 스프라이트 텍스처의 픽셀 데이터를 관리한다.
 * AssetRegistry의 ID/index 체계를 실제 렌더링용 픽셀 배열로 바꾸는 계층이다.
 */
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

    /**
     * 에셋 ID 목록을 실제 이미지 픽셀 캐시로 로드한다.
     * 벽 텍스처와 flat 텍스처 분류는 파일 로드 후 ID 규칙에 따라 하위 등록 메서드로 위임한다.
     */
    public void loadTextures(String[] textureIds) {
        for (String id : textureIds) {
            // 레지스트리는 문자열 ID와 8비트 index를 함께 제공한다.
            // 픽셀 저장소는 대부분 index를 키로 사용한다.
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

    /**
     * 맵 타일 숫자와 벽 텍스처 픽셀 데이터를 직접 연결한다.
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
     * 바닥, 천장, UI 계열처럼 반복 샘플링 가능한 flat 텍스처를 index로 등록한다.
     */
    public void addFlatTexture(int index, int w, int h, int[] pixels) {
        flatTextures.put(index, pixels);
        flatWidths.put(index, w);
        flatHeights.put(index, h);
    }

    /**
     * 바닥 렌더링에 사용할 전역 텍스처 ID를 설정한다.
     */
    public void setGlobalFloorTexture(String id) {
        this.globalFloorTextureIndex = AssetRegistry.getIndex(id);
    }

    /**
     * 천장 렌더링에 사용할 전역 텍스처 ID를 설정한다.
     */
    public void setGlobalCeilingTexture(String id) {
        this.globalCeilingTextureIndex = AssetRegistry.getIndex(id);
    }

    /**
     * 방향과 프레임을 가진 스프라이트 에셋 슬롯을 만든다.
     * 현재 주 렌더 경로는 SpriteRenderer의 lazy-load 방식을 더 많이 사용한다.
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
     * addAsset()으로 만든 슬롯의 특정 방향/프레임 픽셀 데이터를 채운다.
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
     * 방향/프레임 기반 에셋에서 정규화된 UV 좌표의 픽셀을 반환한다.
     */
    public int getPixel(
            String name,
            int dir,
            int frame,
            double u,
            double v) {

        int[][][] asset = assetLibrary.get(name);

        if (asset == null) {
            // 누락 에셋은 디버깅이 쉽도록 마젠타색으로 표시한다.
            return 0xFFFF00FF;
        }

        // Lua/렌더러에서 범위를 벗어난 방향이나 프레임을 넘겨도 안전하게 가장 가까운 값으로 고정한다.
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

        // UV 좌표는 텍스처 영역 안으로 제한한다.
        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int x = (int)(u * (texW - 1));
        int y = (int)(v * (texH - 1));

        return pixels[x + y * texW];
    }

    /**
     * 벽 타일 ID와 정규화된 UV 좌표에 해당하는 벽 텍스처 픽셀을 반환한다.
     */
    public int getWallPixel(
            int id,
            double u,
            double v) {

        int[] pixels = wallTextures.get(id);

        if (pixels == null) {
            // 벽 텍스처 바인딩 실패를 화면에서 즉시 볼 수 있게 한다.
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
     * NPC 회전과 상대 각도를 8방향 스프라이트 인덱스로 변환한다.
     */
    public int calculateDirIndex(double npcRotation, double relativeAngle) {
    
        double angle = relativeAngle - npcRotation;

        // 음수 각도를 포함한 모든 각도를 0~2PI 범위로 정규화한다.
        angle = angle % (Math.PI * 2);
        if (angle < 0) {
            angle += Math.PI * 2;
        }

        // 45도 단위의 8방향 인덱스로 양자화한다.
        int dirIndex = (int) Math.floor((angle + Math.PI / 8.0) / (Math.PI / 4.0));
        
        return dirIndex % 8;
    }

    /**
     * 에셋이 방향별 스프라이트를 가진 것으로 처리할지 판단한다.
     */
    public boolean hasMultipleDirections(String assetId) {
        if (assetId.startsWith("item_") || assetId.equals("key") || assetId.equals("barrel")) {
            return false;
        }
        return true; 
    }

    /**
     * 문자열 에셋 ID로 로드된 벽 텍스처를 index 기반 임시 저장소에 등록한다.
     * 맵 타일 숫자와의 실제 연결은 bindIntIdToStringId()가 수행한다.
     */
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

    /**
     * 맵의 정수 타일 ID를 문자열 에셋 ID의 벽 텍스처에 연결한다.
     */
    public void bindIntIdToStringId(int intId, String stringId) {
        int assetIndex = AssetRegistry.getIndex(stringId);

        if (wallAssetLibrary.containsKey(assetIndex)) {
            // map.dat의 정수 타일값을 실제 벽 텍스처 픽셀 배열에 연결한다.
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

    /**
     * 지정 경로에서 벽 텍스처를 직접 로드해 타일 ID에 등록한다.
     * 자동 레지스트리 경로를 우회해야 할 때 사용할 수 있는 보조 메서드다.
     */
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

    /**
     * 현재 전역 바닥 텍스처에서 월드 좌표 기반 반복 샘플링 픽셀을 반환한다.
     */
    public int getFloorPixel(double u, double v) {
        return getFlatPixel(globalFloorTextureIndex, u, v);
    }

    /**
     * 현재 전역 천장 텍스처에서 월드 좌표 기반 반복 샘플링 픽셀을 반환한다.
     */
    public int getCeilingPixel(double u, double v) {
        return getFlatPixel(globalCeilingTextureIndex, u, v);
    }

    /**
     * flat 텍스처를 반복 UV 방식으로 샘플링한다.
     */
    private int getFlatPixel(int index, double u, double v) {
        int[] pixels = flatTextures.get(index);
        if (pixels == null) return 0xFFFF00FF; // 텍스처 없으면 핑크색

        int w = flatWidths.get(index);
        int h = flatHeights.get(index);

        // 0.0~1.0 범위를 텍스처 좌표로 변환
        // floor()를 빼서 월드 좌표가 커져도 텍스처가 반복되게 만든다.
        int x = (int)((u - Math.floor(u)) * w);
        int y = (int)((v - Math.floor(v)) * h);

        return pixels[x + y * w];
    }
}
