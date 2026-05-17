package engine.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UiManager {

    private final Map<String, int[]> uiTextures = new HashMap<>();
    private final Map<String, Integer> uiWidths = new HashMap<>();
    private final Map<String, Integer> uiHeights = new HashMap<>();
    private final Map<String, Boolean> uiFlags = new HashMap<>();

    public UiManager() {
    }

    public void registerUi(String assetId, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("[UI ERROR] 명세서에 기재된 파일이 존재하지 않습니다: " + filePath);
                return;
            }

            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);

            uiTextures.put(assetId, pixels);
            uiWidths.put(assetId, w);
            uiHeights.put(assetId, h);
            uiFlags.put(assetId, false); 

            System.out.println("[DATA BIND] UI 에셋 등록 완료 -> ID: " + assetId + " (경로: " + filePath + ")");
        } catch (Exception e) {
            System.err.println("[UI ERROR] 에셋 바인딩 실패 -> ID: " + assetId + ", 원인: " + e.getMessage());
        }
    }

    public void setVisible(String name, boolean visible) {
        if (uiFlags.containsKey(name)) {
            uiFlags.put(name, visible);
        }
    }

    public Map<String, Boolean> getUiFlags() { return uiFlags; }
    public int[] getPixels(String name) { return uiTextures.get(name); }
    public int getWidth(String name) { return uiWidths.get(name); }
    public int getHeight(String name) { return uiHeights.get(name); }
}