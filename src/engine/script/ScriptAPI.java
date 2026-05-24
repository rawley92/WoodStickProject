package engine.script;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import engine.boot.Boot;
import engine.boot.AssetRegistry;
import engine.render.Texture;
import engine.render.UiManager;
import engine.Entity.Entity;
import engine.Entity.UiComponent;
import engine.Scene;
import engine.audio.SoundEngine;
import java.util.List;

public class ScriptAPI {
    private final Boot boot;
    private final Texture textureCore;
    private final UiManager uiManager;
    private final SoundEngine soundEngine;

    public ScriptAPI(Boot boot, Texture textureCore, UiManager uiManager, SoundEngine soundEngine) {
        this.boot = boot;
        this.textureCore = textureCore;
        this.uiManager = uiManager;
        this.soundEngine = soundEngine;
    }

    public void register(Globals globals) {
        LuaValue engineLib = LuaValue.tableOf();
        globals.set("engine", engineLib);

        // 1. initScene
        engineLib.set("initScene", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String sceneName = (args.narg() >= 2) ? args.checkjstring(1) : "TitleScene";
                String mapId = (args.narg() >= 2) ? args.checkjstring(2) : args.checkjstring(1);
                System.out.println("[DEBUG] Attempting to load map: " + mapId); // 로그 추가
                String mapPath = AssetRegistry.getPath(mapId);
                if (mapPath == null) return LuaValue.NIL;

                try {
                    List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(mapPath));
                    int height = lines.size();
                    int width = lines.get(0).trim().split("\\s+").length;
                    int[][] map = new int[width][height];
                    for (int y = 0; y < height; y++) {
                        String[] tokens = lines.get(y).trim().split("\\s+");
                        for (int x = 0; x < width; x++) map[x][y] = Integer.parseInt(tokens[x]);
                    }
                    Scene scene = new Scene(sceneName, map);
                    Entity player = new Entity("player", "player", 3.5, 3.5);
                    player.isDynamic = true;
                    player.type = Entity.EntityType.PLAYER;
                    scene.setPlayer(player);
                    boot.setAndActivateScene(scene);
                    System.out.println("[SUCCESS] Scene initialized: " + sceneName);
                } catch (Exception e) { 
                    System.err.println("[CRITICAL ERROR] initScene failed: " + e.getMessage());
                    System.err.println("[CRITICAL] initScene failed for map: " + mapPath);
                     e.printStackTrace();
                 }
                return LuaValue.NIL;
            }
        });

        // 2. Texture 관련
        engineLib.set("assignWallTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (args.arg(1).isint()) textureCore.bindIntIdToStringId(args.checkint(1), args.checkjstring(2));
                else textureCore.bindIntIdToStringId(1, args.checkjstring(1));
                return LuaValue.NIL;
            }
        });
        engineLib.set("setFloorTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalFloorTexture(args.checkjstring(1)); return LuaValue.NIL; }
        });
        engineLib.set("setCeilingTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalCeilingTexture(args.checkjstring(1)); return LuaValue.NIL; }
        });

        // 3. spawnEntity
        engineLib.set("spawnEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String type = args.checkjstring(1);
                String assetId = args.checkjstring(2);
                double x = args.checkdouble(3);
                double y = args.checkdouble(4);
                String scriptId = args.checkjstring(5); // 예: "Script.NPC1"

                Entity entity = new Entity(type, assetId, x, y);
                
                // [중요] ID를 통해 실제 파일 경로를 가져와 저장
                entity.scriptPath = AssetRegistry.getPath(scriptId); 
                
                entity.isActive = true;
                entity.isDynamic = true;
                boot.getCurrentScene().addEntity(entity);
                return LuaValue.NIL;
            }
        });

        // 4. UI 및 Player
        engineLib.set("attachUi", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = boot.getCurrentScene().getPlayer();

                UiComponent ui = new UiComponent();
                ui.uiId = args.checkjstring(1);
                ui.visible = args.optboolean(2, true);
                
                if (args.narg() >= 3) {
                    ui.currentTextureId = args.checkjstring(3);
                }

                entity.ui = ui;
                return LuaValue.NIL;
            }
        });

        engineLib.set("setupPlayer", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                Entity player = boot.getCurrentScene().getPlayer();
                player.physics.x = args.checkdouble(1); player.physics.y = args.checkdouble(2);
                player.camera.dirX = args.checkdouble(3); player.camera.dirY = args.checkdouble(4);
                player.camera.planeX = args.checkdouble(5); player.camera.planeY = args.checkdouble(6);
                return LuaValue.NIL;
            }
        });

        engineLib.set("playSound", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {

                int entityId = args.checkint(1);
                String soundId = args.checkjstring(2);
                boolean loop = args.optboolean(3, false);

                Entity entity = boot.getCurrentScene().getEntityById(entityId);
                if (entity == null || entity.sound == null) return NIL;

                entity.sound.play(soundId, loop);
                return NIL;
            }
        });

        engineLib.set("playBgm", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {

                String soundId = args.checkjstring(1);
                boolean loop = args.optboolean(2, false);

                String path = AssetRegistry.getPath(soundId);
                if (path == null) return NIL;

                soundEngine.playBgm(path, loop);

                return NIL;
            }
        });

        engineLib.set("stopBgm", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                soundEngine.stopBgm();
                return NIL;
            }
        });

        engineLib.set("distance", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue a, LuaValue b) {
                Entity e1 = (Entity) CoerceLuaToJava.coerce(a, Entity.class);
                Entity e2 = (Entity) CoerceLuaToJava.coerce(b, Entity.class);

                double dx = e1.physics.x - e2.physics.x;
                double dy = e1.physics.y - e2.physics.y;

                return LuaValue.valueOf(Math.sqrt(dx * dx + dy * dy));
            }
        });
    }
}