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

        engineLib.set("initScene", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String sceneName = (args.narg() >= 2) ? args.checkjstring(1) : "TitleScene";
                String mapId = (args.narg() >= 2) ? args.checkjstring(2) : args.checkjstring(1);
                String mapPath = AssetRegistry.getPath(mapId);
                if (mapPath == null) {
                    System.err.println("경로 찾기 실패: " + mapId);
                    return LuaValue.NIL;
                    } try {
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

        engineLib.set("assignWallTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (args.arg(1).isint()) textureCore.bindIntIdToStringId(args.checkint(1), resolveAssetId(args.checkjstring(2)));
                else textureCore.bindIntIdToStringId(1, resolveAssetId(args.checkjstring(1)));
                return LuaValue.NIL;
            }
        });
        engineLib.set("setFloorTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalFloorTexture(resolveAssetId(args.checkjstring(1))); return LuaValue.NIL; }
        });
        engineLib.set("setCeilingTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalCeilingTexture(resolveAssetId(args.checkjstring(1))); return LuaValue.NIL; }
        });

        engineLib.set("spawnEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String type = args.checkjstring(1);
                String assetId = resolveAssetId(args.checkjstring(2));
                double x = args.checkdouble(3);
                double y = args.checkdouble(4);
                String scriptId = args.checkjstring(5); 
                Entity entity = new Entity(type, assetId, x, y);
                entity.render.scale = args.optdouble(6, 1.0);
                entity.scriptPath = AssetRegistry.getPath(scriptId); 
                
                entity.isActive = true;
                entity.isDynamic = true;
                boot.getCurrentScene().addEntity(entity);
                return LuaValue.valueOf(entity.entityId);
            }
        });

        engineLib.set("getEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                Scene scene = boot.getCurrentScene();
                if (scene == null) return LuaValue.NIL;

                Entity entity = scene.getEntityById(args.checkint(1));
                if (entity == null) return LuaValue.NIL;

                return entity.getLuaWrapper();
            }
        });

        engineLib.set("setEntityScale", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int entityId = args.checkint(1);
                double scale = args.checkdouble(2);
                Entity entity = boot.getCurrentScene().getEntityById(entityId);

                if (entity != null && entity.render != null) {
                    entity.render.scale = scale;
                }
                return LuaValue.NIL;
            }
        });

        engineLib.set("attachUi", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = boot.getCurrentScene().getPlayer();
                UiComponent ui = new UiComponent();
                ui.uiId = resolveAssetId(args.checkjstring(1));
                ui.visible = args.optboolean(2, true);
                
                if (args.narg() >= 3) {
                    ui.currentTextureId = resolveAssetId(args.checkjstring(3));
                }

                entity.ui = ui;
                return LuaValue.NIL;
            }
        });

        engineLib.set("updateUiTexture", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = boot.getCurrentScene().getPlayer();
                if (args.narg() >= 2) {
                    int entityId = args.checkint(1);
                    entity = boot.getCurrentScene().getEntityById(entityId);
                }
                if (entity == null || entity.ui == null) return LuaValue.NIL;
                String textureId = args.narg() >= 2
                        ? resolveAssetId(args.checkjstring(2))
                        : resolveAssetId(args.checkjstring(1));
                entity.ui.currentTextureId = textureId;
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiClear", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                uiManager.clearDrawCommands();
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiRect", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                uiManager.drawRect(
                        args.checkint(1),
                        args.checkint(2),
                        args.checkint(3),
                        args.checkint(4),
                        args.checkint(5),
                        args.optdouble(6, 1.0)
                );
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiImage", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String textureId = resolveAssetId(args.checkjstring(1));
                int width = uiManager.getWidth(textureId);
                int height = uiManager.getHeight(textureId);
                double alpha = 1.0;

                if (args.narg() == 4) {
                    alpha = args.optdouble(4, 1.0);
                } else if (args.narg() >= 5) {
                    width = args.checkint(4);
                    height = args.checkint(5);
                    alpha = args.optdouble(6, 1.0);
                }

                if (width <= 0 || height <= 0) {
                    System.err.println("[UI ERROR] 이미지 크기를 확인할 수 없습니다: " + textureId);
                    return LuaValue.NIL;
                }

                uiManager.drawImage(
                        textureId,
                        args.checkint(2),
                        args.checkint(3),
                        width,
                        height,
                        alpha
                );
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiImageRotated", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String textureId = resolveAssetId(args.checkjstring(1));
                int width = uiManager.getWidth(textureId);
                int height = uiManager.getHeight(textureId);
                double angleDegrees;
                double alpha = 1.0;

                if (args.narg() >= 6) {
                    width = args.checkint(4);
                    height = args.checkint(5);
                    angleDegrees = args.checkdouble(6);
                    alpha = args.optdouble(7, 1.0);
                } else {
                    angleDegrees = args.checkdouble(4);
                    alpha = args.optdouble(5, 1.0);
                }

                if (width <= 0 || height <= 0) {
                    System.err.println("[UI ERROR] 이미지 크기를 확인할 수 없습니다: " + textureId);
                    return LuaValue.NIL;
                }

                uiManager.drawRotatedImage(
                        textureId,
                        args.checkint(2),
                        args.checkint(3),
                        width,
                        height,
                        angleDegrees,
                        alpha
                );
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiText", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                uiManager.drawText(
                        args.checkjstring(1),
                        args.checkint(2),
                        args.checkint(3),
                        args.checkint(4),
                        args.checkint(5),
                        args.optdouble(6, 1.0)
                );
                return LuaValue.NIL;
            }
        });

        engineLib.set("uiTextCenter", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                uiManager.drawTextCentered(
                        args.checkjstring(1),
                        args.checkint(2),
                        args.checkint(3),
                        args.checkint(4),
                        args.checkint(5),
                        args.optdouble(6, 1.0)
                );
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

        engineLib.set("exit", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                System.exit(0);
                return NIL;
            }
        });

        engineLib.set("hasWallBetween", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int offset = args.arg(1).istable() ? 1 : 0;
                double x1 = args.checkdouble(1 + offset);
                double y1 = args.checkdouble(2 + offset);
                double x2 = args.checkdouble(3 + offset);
                double y2 = args.checkdouble(4 + offset);
                return LuaValue.valueOf(hasWallBetween(x1, y1, x2, y2));
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

    private boolean hasWallBetween(double x1, double y1, double x2, double y2) {
        Scene scene = boot.getCurrentScene();
        if (scene == null) {
            return false;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= 0.0) {
            return false;
        }

        int steps = Math.max(1, (int)(distance * 10.0));

        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            int tileX = (int)(x1 + dx * t);
            int tileY = (int)(y1 + dy * t);

            if (scene.getTile(tileX, tileY) > 0) {
                return true;
            }
        }

        return false;
    }

    private String resolveAssetId(String idOrIndex) {
        String id = AssetRegistry.getIdByIndex(idOrIndex);

        if (id != null) {
            return id;
        }

        return idOrIndex;
    }
}
