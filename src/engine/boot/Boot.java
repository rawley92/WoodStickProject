package engine.boot;

import engine.Scene;
import engine.render.Texture;
import engine.render.UiManager;

/**
 * 엔진 부팅 과정에서 공유되는 설정과 활성 씬을 관리한다.
 * 실제 컨텐츠 로드는 Lua API와 AssetRegistry를 통해 단계적으로 이루어진다.
 */
public class Boot {

    private Scene currentScene;

    private Config config;

    /**
     * 부트스트랩 상태를 담는 객체를 생성한다.
     * 실제 설정/에셋/씬 초기화는 별도 메서드에서 단계적으로 수행한다.
     */
    public Boot() {
    }

    /**
     * 에셋 레지스트리를 로드하는 부트 단계다.
     */
    public void loadAssets(Texture textureManager, UiManager uiManager) {

        AssetRegistry.loadFromJson("Data/Data.json");
        
        System.out.println("[BOOT] 모든 에셋 로드 완료.");
    }

    /**
     * 전역 설정 객체를 준비한다.
     * 현재 구현은 config.json 파싱이 아니라 Config 기본값 생성을 사용한다.
     */
    public boolean loadConfig() {
        this.config = new Config(); 

        System.out.println("[BOOT] 전역 하드웨어 설정 로드 완료 (해상도: " + config.baseWidth + "x" + config.baseHeight + ")");

        return true;
    }

    /**
     * 레지스트리에 등록된 텍스처 에셋을 Texture 서비스로 로드한다.
     * 구체적인 이미지 파일 판독과 벽/flat 분류는 Texture.loadTextures()가 담당한다.
     */
    public void init(Texture textureCore) {
        System.out.println("[BOOT] Initializing assets and textures...");

        String[] textureIds = AssetRegistry.getIdsByCategory("Textures"); 

        if (textureIds != null && textureIds.length > 0) {

            textureCore.loadTextures(textureIds);
            
            System.out.println("[BOOT] All textures loaded successfully.");
        } else {
            System.err.println("[BOOT] Warning: No textures found to load!");
        }
    }

    /**
     * 현재 전역 설정을 반환한다.
     */
    public Config getConfig() { 
        return this.config; 
    }

    /**
     * 현재 활성 씬을 반환한다.
     */
    public Scene getCurrentScene() { 
        return this.currentScene; 
    }

    /**
     * 런타임에서 사용할 활성 씬을 교체한다.
     * 씬 생성 자체는 Lua API 또는 별도 로더가 담당한다.
     */
    public void setAndActivateScene(Scene scene) {
        
        this.currentScene = scene;

        System.out.println("[BOOT] Active Scene Switched to: " + scene.getName());
    }
}
