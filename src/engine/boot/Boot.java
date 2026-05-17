package engine.boot;

import engine.Scene;
import engine.render.Texture;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * [Engine/Boot/Boot]
 * 매니페스트 생성/파싱 및 게임 전역 컨텍스트(Scene 관리)를 담당하는 부트스트랩 클래스입니다.
 */
public class Boot {
    private Scene currentScene;
    private Config config; // 기존 설정 오브젝트 유지 (가정)

    // 빈 생성자로 변경하여 Core에서 순서 제약을 없앱니다.
    public Boot() {
    }

    public boolean loadConfig() {
    // 기본 생성자를 호출하여 Config 클래스에 정의된 기본값(1280, 720 등)들을 주입합니다.
        this.config = new Config(); 
        
        // 나중에 진짜 외부 파일(config.json 같은)에서 읽어올 로직이 필요하다면 여기에 추가하면 됩니다.
        System.out.println("[BOOT] 전역 하드웨어 설정 로드 완료 (해상도: " + config.baseWidth + "x" + config.baseHeight + ")");
        return true;
    }

    public void init(Texture textureCore) {
        // 기존 초기화 로직 유지
    }

    public Config getConfig() {
        return this.config;
    }

    public Scene getCurrentScene() {
        return this.currentScene;
    }

    public void setAndActivateScene(Scene scene) {
        this.currentScene = scene;
        System.out.println("[BOOT] Active Scene Switched to: " + scene.getName());
    }

    /**
     * Data.json 파일을 읽어서 매니페스트에 등록된 에셋들을 메모리(textureManager)에 로드합니다.
     */
    public void loadAssetsFromManifest(Texture textureManager) {
        String jsonPath = "Data/Data.json";
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            System.err.println("[REGISTRY ERROR] 명세서 파일(Data.json)이 없습니다. DataLoader 확인 필요.");
            return;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)));

            // 1. 벽 텍스처 파싱 및 로드
            List<String> walls = parseJsonArray(content, "wall_textures");
            int wallId = 1;
            for (String wallFile : walls) {
                File file = new File("Data/textures/walls/" + wallFile);
                if (!file.exists()) continue;

                BufferedImage img = ImageIO.read(file);
                int w = img.getWidth();
                int h = img.getHeight();
                int[] pixels = new int[w * h];
                img.getRGB(0, 0, w, h, pixels, 0, w);

                textureManager.addWallTexture(wallId, w, h, pixels);
                System.out.println("[REGISTRY] Wall Registered: ID " + wallId + " -> " + wallFile);
                wallId++;
            }

            // 2. 캐릭터 에셋 파싱 및 로드
            List<String> characters = parseJsonArray(content, "characters");
            for (String charName : characters) {
                File dir = new File("Data/Char/" + charName);
                if (!dir.exists()) continue;

                registerCharacterSpriteSheet(textureManager, charName, dir);
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

    private List<String> parseJsonArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return result;

        int startBracket = json.indexOf("[", keyIndex);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket == -1 || endBracket == -1) return result;

        String arrayContent = json.substring(startBracket + 1, endBracket);
        String[] tokens = arrayContent.split(",");
        for (String token : tokens) {
            String clean = token.replace("\"", "").replace("\n", "").trim();
            if (!clean.isEmpty()) {
                result.add(clean);
            }
        }
        return result;
    }
}