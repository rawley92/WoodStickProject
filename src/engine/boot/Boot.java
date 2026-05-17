package engine.boot;

import engine.Scene;
import engine.render.Texture;
import engine.render.UiManager;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Boot {
    private Scene currentScene;
    private Config config;

    public Boot() {
    }

    public boolean loadConfig() {
        this.config = new Config(); 
        System.out.println("[BOOT] 전역 하드웨어 설정 로드 완료 (해상도: " + config.baseWidth + "x" + config.baseHeight + ")");
        return true;
    }

    public void init(Texture textureCore) {
    }

    public Config getConfig() { return this.config; }
    public Scene getCurrentScene() { return this.currentScene; }

    public void setAndActivateScene(Scene scene) {
        this.currentScene = scene;
        System.out.println("[BOOT] Active Scene Switched to: " + scene.getName());
    }

    private static class AssetEntry {
        String id;
        String path;
        AssetEntry(String id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    public void loadAssetsFromManifest(Texture textureManager, UiManager uiManager) {
        String jsonPath = "Data/Data.json";
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            System.err.println("[REGISTRY ERROR] 명세서 파일(Data.json)이 없습니다. DataLoader 확인 필요.");
            return;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
            List<AssetEntry> walls = parseJsonObjectArray(content, "wall_textures");
            for (AssetEntry wall : walls) {
                File file = new File(wall.path);
                if (!file.exists()) {
                    System.err.println("[REGISTRY ERROR] 벽 텍스처 파일이 없습니다: " + wall.path);
                    continue;
                }

                BufferedImage img = ImageIO.read(file);
                int w = img.getWidth();
                int h = img.getHeight();
                int[] pixels = new int[w * h];
                img.getRGB(0, 0, w, h, pixels, 0, w);

                textureManager.addWallTextureWithStringId(wall.id, w, h, pixels);
                System.out.println("[REGISTRY] Wall Stored in Library: [" + wall.id + "]");
            }

            if (uiManager != null) {
                List<AssetEntry> uis = parseJsonObjectArray(content, "ui_textures");
                for (AssetEntry ui : uis) {
                    uiManager.registerUi(ui.id, ui.path);
                }
            }

            List<AssetEntry> characters = parseJsonObjectArray(content, "characters");
            for (AssetEntry charAsset : characters) {
                File dir = new File(charAsset.path);
                if (!dir.exists()) continue;

                registerCharacterSpriteSheet(textureManager, charAsset.id, dir);
            }

        } catch (Exception e) {
            System.err.println("[REGISTRY ERROR] 매니페스트 기반 로딩 중 치명적 예외 발생");
            e.printStackTrace();
        }
    }

    private void registerCharacterSpriteSheet(Texture textureManager, String name, File dir) throws Exception {
        int dirCount = 8;
        int frameCount = 4;
        boolean initialized = false;

        for (int d = 0; d < dirCount; d++) {
            for (int f = 0; f < frameCount; f++) {
                File imgFile = new File(dir, "walk_" + d + "_" + f + ".png");
                if (!imgFile.exists()) continue;

                BufferedImage img = ImageIO.read(imgFile);
                if (!initialized) {
                    textureManager.addAsset(name, dirCount, frameCount, img.getWidth(), img.getHeight());
                    initialized = true;
                }

                int w = img.getWidth();
                int h = img.getHeight();
                int[] pixels = new int[w * h];
                img.getRGB(0, 0, w, h, pixels, 0, w);
                textureManager.setPixels(name, d, f, pixels);
            }
        }
        System.out.println("[REGISTRY] Character Asset Texture Loaded: " + name);
    }

    private List<AssetEntry> parseJsonObjectArray(String json, String key) {
        List<AssetEntry> result = new ArrayList<>();
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return result;

        int startBracket = json.indexOf("[", keyIndex);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket == -1 || endBracket == -1) return result;

        String arrayContent = json.substring(startBracket + 1, endBracket);

        String[] objects = arrayContent.split("\\}");
        for (String obj : objects) {
            if (!obj.contains("{")) continue;

            int idIdx = obj.indexOf("\"id\":");
            int idStart = obj.indexOf("\"", idIdx + 5);
            int idEnd = obj.indexOf("\"", idStart + 1);
 
            int pathIdx = obj.indexOf("\"path\":");
            int pathStart = obj.indexOf("\"", pathIdx + 7);
            int pathEnd = obj.indexOf("\"", pathStart + 1);
            
            if (idStart != -1 && idEnd != -1 && pathStart != -1 && pathEnd != -1) {
                String id = obj.substring(idStart + 1, idEnd).trim();
                String path = obj.substring(pathStart + 1, pathEnd).trim();
                result.add(new AssetEntry(id, path));
            }
        }
        return result;
    }
}