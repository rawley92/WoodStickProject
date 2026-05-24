package engine.boot;

import engine.Scene;

/**
 * [SceneLoader]
 * Lua 스크립트 요청에 따라 에셋의 실제 로딩 처리를 담당한다.
 */
public class SceneLoader {

    private Scene currentScene;

    public SceneLoader(Scene scene) {
        this.currentScene = scene;
    }

    public void loadMap(String levelId) {
        String path = AssetRegistry.getPath(levelId);
        
        if (path == null) {
            System.err.println("[SCENE LOADER] Map ID not found: " + levelId);
            return;
        }

        System.out.println("[SCENE LOADER] Loading map file from -> " + path);
        // TODO: 실제 맵 파일 바이트 단위 읽기 및 Scene 객체에 데이터 주입
    }
}