package engine;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.*;

import engine.Entity.Entity;
import engine.boot.Boot;
import engine.render.Texture;
import engine.render.UiManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ScriptEngine {

    private Globals globals;
    private Boot boot;
    private Texture textureCore;
    private UiManager uiManager; 

    private final Map<String, LuaValue> scriptCache = new HashMap<>();

    public ScriptEngine(Boot boot, Texture textureCore, UiManager uiManager) {
        this.boot = boot;
        this.textureCore = textureCore;
        this.uiManager = uiManager;
        this.globals = JsePlatform.standardGlobals();

        exposeEngineAPI();
    }

    private void exposeEngineAPI() {
        LuaValue engineLib = LuaValue.tableOf();
        globals.set("engine", engineLib);

        engineLib.set("initScene", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String sceneName = args.checkjstring(1);
                String mapPath = args.checkjstring(2);

                try {
                    List<String> lines = Files.readAllLines(Paths.get(mapPath));
                    int height = lines.size();
                    int width = lines.get(0).trim().split("\\s+").length;

                    int[][] map = new int[width][height];
                    for (int y = 0; y < height; y++) {
                        String[] tokens = lines.get(y).trim().split("\\s+");
                        for (int x = 0; x < width; x++) {
                            map[x][y] = Integer.parseInt(tokens[x]);
                        }
                    }

                    Scene scene = new Scene(sceneName, map);
                    
                    Entity player = new Entity("player", "player", 3.5, 3.5);
                    player.type = Entity.EntityType.PLAYER;
                    player.isDynamic = true;
                    player.isActive = true;
                    scene.setPlayer(player);

                    boot.setAndActivateScene(scene);
                    System.out.println("[Lua Link] Scene '" + sceneName + "' 생성 완료.");
                } catch (Exception e) {
                    System.err.println("[Lua Error] Map load failed: " + mapPath);
                }
                return LuaValue.NIL;
            }
        });

        engineLib.set("assignWallTexture", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {

                int mapCode = args.checkint(1);
                String textureName = args.checkjstring(2);

                textureCore.bindIntIdToStringId(mapCode, textureName);

                System.out.println(
                    "[Lua Link] 맵 타일 정의: [값 "
                    + mapCode
                    + "] [텍스처: "
                    + textureName
                    + "]"
                );
                return LuaValue.NIL;
            }
        });

        engineLib.set("spawnEntity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {

                String typeStr = args.checkjstring(1);
                String assetName = args.checkjstring(2);
                double x = args.checkdouble(3);
                double y = args.checkdouble(4);
                String scriptName = args.optjstring(5, "default");

                Scene currentScene = boot.getCurrentScene();
                if (currentScene == null) return LuaValue.NIL;

                Entity entity = new Entity(assetName, assetName, x, y);
                entity.type = Entity.EntityType.valueOf(typeStr.toUpperCase());
                entity.scriptName = scriptName;
                entity.isDynamic = true;
                entity.isActive = true;

                if (!scriptName.equals("default")) {
                    preloadScript(scriptName);
                }

                currentScene.addEntity(entity);

                System.out.println("[ENTITY SPAWN] " + assetName + " @ " + x + ", " + y);

                return LuaValue.NIL;
            }
        });

        engineLib.set("setUiVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String uiName = args.checkjstring(1);
                boolean visible = args.checkboolean(2);

                if (uiManager != null) {
                    uiManager.setVisible(uiName, visible);
                }
                return LuaValue.NIL;
            }
        });

        engineLib.set("setupPlayer", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double x = args.checkdouble(1);
                double y = args.checkdouble(2);
                double dirX = args.checkdouble(3);
                double dirY = args.checkdouble(4);
                double planeX = args.checkdouble(5);
                double planeY = args.checkdouble(6);

                Scene currentScene = boot.getCurrentScene();
                if (currentScene != null && currentScene.getPlayer() != null) {
                    Entity player = currentScene.getPlayer();
                    player.physics.x = x;
                    player.physics.y = y;

                    player.camera.dirX = dirX;
                    player.camera.dirY = dirY;
                    player.camera.planeX = planeX;
                    player.camera.planeY = planeY;
                    System.out.println("[Script Bridge] 플레이어 물리 및 카메라 벡터 초기화 완료.");
                }
                return LuaValue.NIL;
            }
        });
    }

    public void preloadScript(String scriptName) {
        if (scriptCache.containsKey(scriptName)) return;

        String path = "Data/Script/" + scriptName + ".lua";
        try {
            LuaValue chunk = globals.loadfile(path);
            chunk.call(); 
            scriptCache.put(scriptName, chunk);
        } catch (Exception e) {
            System.err.println("[Lua Error] 스크립트 프리로드 실패: " + path);
        }
    }

    public void runScript(String scriptName) {
        String path = "Data/Script/" + scriptName + ".lua";
        try {
            globals.loadfile(path).call();
        } catch (Exception e) {
            System.err.println("[Lua Error] 전역 스크립트 실행 실패: " + path);
        }
    }

    public void updateEntity(Entity entity, double deltaTime, Entity player) {
        if (!entity.isActive || entity.scriptName == null || entity.scriptName.equals("default")) return;

        String funcName = "update_" + entity.scriptName;
        LuaValue updateFunc = globals.get(funcName);

        if (updateFunc.isnil()) return;

        try {
            updateFunc.call(
                CoerceJavaToLua.coerce(entity),
                LuaValue.valueOf(deltaTime),
                CoerceJavaToLua.coerce(player)
            );
        } catch (Exception e) {
            System.err.println("[Lua Runtime Error] " + entity.scriptName + " 틱 연산 실패: " + e.getMessage());
        }
    }
}