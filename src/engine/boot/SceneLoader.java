package engine.boot;

import engine.Scene;


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

    }
}