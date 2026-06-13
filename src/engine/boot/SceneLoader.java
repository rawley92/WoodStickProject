package engine.boot;

import engine.Scene;


/**
 * 향후 선언형 씬 로딩을 확장하기 위한 보조 로더다.
 * 현재 실제 map.dat 파싱은 Lua API의 initScene 구현이 수행한다.
 */
public class SceneLoader {

    private Scene currentScene;

    /**
     * 특정 씬을 대상으로 동작하는 로더를 생성한다.
     * 현재 구현에서는 향후 확장을 위한 보조 클래스 성격이 강하다.
     */
    public SceneLoader(Scene scene) {
        this.currentScene = scene;
    }

    /**
     * 레벨 ID를 에셋 경로로 해석한다.
     * 실제 map.dat 파싱과 Scene 교체는 현재 ScriptAPI.initScene()에서 수행한다.
     */
    public void loadMap(String levelId) {
        String path = AssetRegistry.getPath(levelId);
        
        if (path == null) {
            System.err.println("[SCENE LOADER] Map ID not found: " + levelId);
            return;
        }

        System.out.println("[SCENE LOADER] Loading map file from -> " + path);

    }
}
