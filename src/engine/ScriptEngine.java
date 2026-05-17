package engine;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.*;
import engine.boot.Boot;
import engine.render.Texture;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ScriptEngine {

    private Globals globals;
    private Boot boot;
    private Texture textureCore;
    private LuaValue cachedUpdateFunc = LuaValue.NIL;

    public ScriptEngine(Boot boot, Texture textureCore) {
        this.boot = boot;
        this.textureCore = textureCore;
        this.globals = JsePlatform.standardGlobals();

        exposeEngineAPI();
    }

    private void exposeEngineAPI() {
        LuaValue engineLib = LuaValue.tableOf();
        globals.set("engine", engineLib);

        // ==========================================
        // 1. SCENE / MAP INITIALIZATION API
        // ==========================================
        
        // engine.initScene("Level1", "Data/Level/Level1/map.dat")
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
                    
                    // 기본 플레이어 레이아웃 기본값 배치
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

        // ==========================================
        // 2. TEXTURE TO WALL ALLOCATION API (핵심 수정 변경점)
        // ==========================================
        
        // engine.assignWallTexture(int mapCode, String textureAssetName)
        // 예: engine.assignWallTexture(1, "brick_red") -> map.dat의 1번 벽은 프리로드된 brick_red 텍스처를 맵핑한다.
        engineLib.set("assignWallTexture", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int mapCode = args.checkint(1);
                String textureName = args.checkjstring(2);

                // DataLoader에 의해 TextureCore에 이미 로드된 에셋 텍스처를 검색합니다.
                // 텍스처 코어 설계에 따라 오버로딩 메서드나 이름 매핑 바인딩 테이블을 거치게 처리합니다.
                // 예시: textureCore.bindMapCodeToAsset(mapCode, textureName);
                
                System.out.println("[Lua Link] 맵 타일 정의: [값 " + mapCode + "] ➡️ [텍스처: " + textureName + "]");
                return LuaValue.NIL;
            }
        });

        // ==========================================
        // 3. ENTITY SPAWN API
        // ==========================================
        
        // engine.spawnEntity("NPC", "npc_guard", 5.5, 5.5, "guard_patrol")
        engineLib.set("spawnEntity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String typeStr = args.checkjstring(1);
                String assetName = args.checkjstring(2); // 파일 경로가 아닌 프리로드된 캐릭터 자산 이름
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

                if (entity.type == Entity.EntityType.PLAYER) {
                    currentScene.setPlayer(entity);
                } else {
                    currentScene.addEntity(entity);
                }
                return LuaValue.NIL;
            }
        });
    }

    public void runScript(String scriptName) {
        String path = "Data/Script/" + scriptName + ".lua";
        try {
            globals.loadfile(path).call();
            cachedUpdateFunc = globals.get("onEntityUpdate");
        } catch (Exception e) {
            System.err.println("[Lua Error] 스크립트 실행 실패: " + path);
        }
    }

    public void updateEntity(Entity entity, double deltaTime, Entity player) {
        if (!entity.isActive || cachedUpdateFunc.isnil()) return;
        try {
            cachedUpdateFunc.call(
                CoerceJavaToLua.coerce(entity),
                LuaValue.valueOf(deltaTime),
                CoerceJavaToLua.coerce(player)
            );
        } catch (Exception e) {
            System.err.println("[Lua Runtime Error] Update Fail: " + e.getMessage());
        }
    }
}