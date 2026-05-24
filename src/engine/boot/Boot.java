package engine.boot;

import engine.Scene;
import engine.render.Texture;
import engine.render.UiManager;

public class Boot {
    private Scene currentScene;
    private Config config;

    public Boot() {
    }

    public void loadAssets(Texture textureManager, UiManager uiManager) {
        // 1. 레지스트리 로드 (경로 맵핑 생성)
        AssetRegistry.loadFromJson("Data/Data.json");
        
        System.out.println("[BOOT] 모든 에셋 로드 완료.");
    }

    public boolean loadConfig() {
        this.config = new Config(); 
        System.out.println("[BOOT] 전역 하드웨어 설정 로드 완료 (해상도: " + config.baseWidth + "x" + config.baseHeight + ")");
        return true;
    }

    public void init(Texture textureCore) {
    System.out.println("[BOOT] Initializing assets and textures...");

        // 1. Data.json에 있는 Textures 카테고리의 모든 ID 가져오기
        // 만약 AssetRegistry에 getCategoryIds가 없다면 직접 배열을 넣으셔도 됩니다.
        String[] textureIds = AssetRegistry.getIdsByCategory("Textures"); 

        if (textureIds != null && textureIds.length > 0) {
            // 2. 텍스처 로드 실행
            textureCore.loadTextures(textureIds);
            System.out.println("[BOOT] All textures loaded successfully.");
        } else {
            System.err.println("[BOOT] Warning: No textures found to load!");
        }
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
}