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

/**
 * Java 엔진 서비스를 Lua의 engine 테이블 함수로 노출한다.
 * Data/Script의 컨텐츠 코드는 이 API를 통해 씬, 엔티티, UI, 오디오를 조작한다.
 */
public class ScriptAPI {
    private final Boot boot;
    private final Texture textureCore;
    private final UiManager uiManager;
    private final SoundEngine soundEngine;

    /**
     * Lua에서 호출할 엔진 서비스 참조를 보관한다.
     * 각 서비스의 실제 동작은 register()가 등록하는 engine.* 함수에서 호출된다.
     */
    public ScriptAPI(Boot boot, Texture textureCore, UiManager uiManager, SoundEngine soundEngine) {
        this.boot = boot;
        this.textureCore = textureCore;
        this.uiManager = uiManager;
        this.soundEngine = soundEngine;
    }

    /**
     * Lua 전역 환경에 engine 테이블과 엔진 API 함수를 등록한다.
     * 각 API는 씬, 텍스처, 엔티티, UI, 사운드 같은 Java 서비스를 Lua 스크립트에 노출한다.
     */
    public void register(Globals globals) {
        LuaValue engineLib = LuaValue.tableOf();
        globals.set("engine", engineLib);

        // Lua가 map.dat를 기반으로 Scene과 기본 Player를 만들 수 있게 한다.
        engineLib.set("initScene", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                // 인자가 2개면 sceneName/mapId, 1개면 기본 sceneName과 mapId로 해석한다.
                String sceneName = (args.narg() >= 2) ? args.checkjstring(1) : "TitleScene";
                String mapId = (args.narg() >= 2) ? args.checkjstring(2) : args.checkjstring(1);

                // Lua는 "Level.maze.map" 같은 논리 ID를 넘기고, Java가 실제 경로를 조회한다.
                String mapPath = AssetRegistry.getPath(mapId);
                if (mapPath == null) {
                    System.err.println("경로 찾기 실패: " + mapId);
                    return LuaValue.NIL;
                    } try {
                    // map.dat는 공백으로 분리된 정수 그리드다.
                    // 파일의 y행/x열 구조를 Scene이 사용하는 map[x][y] 구조로 전치해 저장한다.
                    List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(mapPath));
                    int height = lines.size();
                    int width = lines.get(0).trim().split("\\s+").length;
                    int[][] map = new int[width][height];
                    for (int y = 0; y < height; y++) {
                        String[] tokens = lines.get(y).trim().split("\\s+");
                        for (int x = 0; x < width; x++) map[x][y] = Integer.parseInt(tokens[x]);
                    }

                    // 모든 씬은 Java 기본 player를 하나 가진다.
                    // 세부 위치와 카메라 방향은 Lua가 setupPlayer()로 후속 설정한다.
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

        // 맵 타일 번호와 벽 텍스처 에셋을 연결한다.
        engineLib.set("assignWallTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (args.arg(1).isint()) textureCore.bindIntIdToStringId(args.checkint(1), resolveAssetId(args.checkjstring(2)));
                else textureCore.bindIntIdToStringId(1, resolveAssetId(args.checkjstring(1)));
                return LuaValue.NIL;
            }
        });

        // 전역 바닥 텍스처를 설정한다.
        engineLib.set("setFloorTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalFloorTexture(resolveAssetId(args.checkjstring(1))); return LuaValue.NIL; }
        });

        // 전역 천장 텍스처를 설정한다.
        engineLib.set("setCeilingTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) { textureCore.setGlobalCeilingTexture(resolveAssetId(args.checkjstring(1))); return LuaValue.NIL; }
        });

        // Lua 컨텐츠 스크립트가 런타임 엔티티를 스폰하고 스크립트를 연결할 수 있게 한다.
        engineLib.set("spawnEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                // type은 컨텐츠 분류명, assetId는 초기 렌더 이미지, scriptId는 동작 Lua 파일이다.
                String type = args.checkjstring(1);
                String assetId = resolveAssetId(args.checkjstring(2));
                double x = args.checkdouble(3);
                double y = args.checkdouble(4);
                String scriptId = args.checkjstring(5); 
                Entity entity = new Entity(type, assetId, x, y);
                entity.render.scale = args.optdouble(6, 1.0);

                // 엔티티는 asset registry 경로를 보관한다.
                // ScriptManager가 첫 update 시 이 경로를 실제 Lua 파일로 로드한다.
                entity.scriptPath = AssetRegistry.getPath(scriptId); 
                
                entity.isActive = true;
                entity.isDynamic = true;
                boot.getCurrentScene().addEntity(entity);
                return LuaValue.valueOf(entity.entityId);
            }
        });

        // entityId로 Java 엔티티를 Lua userdata 형태로 조회한다.
        engineLib.set("getEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                Scene scene = boot.getCurrentScene();
                if (scene == null) return LuaValue.NIL;

                Entity entity = scene.getEntityById(args.checkint(1));
                if (entity == null) return LuaValue.NIL;

                return entity.getLuaWrapper();
            }
        });

        // 엔티티 렌더 스케일을 Lua에서 변경할 수 있게 한다.
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

        // 플레이어 엔티티에 고정 UI 컴포넌트를 부착한다.
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

        // 엔티티 UI 컴포넌트의 현재 텍스처를 갱신한다.
        engineLib.set("updateUiTexture", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                // 인자가 하나면 플레이어 UI, 두 개면 entityId로 찾은 엔티티 UI를 갱신한다.
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

        // 이번 프레임의 즉시 모드 UI 명령을 초기화한다.
        engineLib.set("uiClear", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                uiManager.clearDrawCommands();
                return LuaValue.NIL;
            }
        });

        // UI 사각형 draw command를 추가한다.
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

        // UI 이미지 draw command를 추가한다.
        engineLib.set("uiImage", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String textureId = resolveAssetId(args.checkjstring(1));
                // width/height를 생략하면 UiManager가 등록한 원본 이미지 크기를 사용한다.
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

        // 회전된 UI 이미지 draw command를 추가한다.
        engineLib.set("uiImageRotated", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String textureId = resolveAssetId(args.checkjstring(1));
                // 기본 크기 호출과 명시 크기 호출을 모두 지원하기 위해 인자 개수로 해석 방식을 나눈다.
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

        // 좌표 기준 텍스트 draw command를 추가한다.
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

        // 중앙 정렬 텍스트 draw command를 추가한다.
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

        // 플레이어 위치와 카메라 방향/평면 벡터를 설정한다.
        engineLib.set("setupPlayer", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                Entity player = boot.getCurrentScene().getPlayer();
                // physics는 월드 좌표, camera.dir은 전방 벡터, camera.plane은 시야 폭 벡터다.
                player.physics.x = args.checkdouble(1); player.physics.y = args.checkdouble(2);
                player.camera.dirX = args.checkdouble(3); player.camera.dirY = args.checkdouble(4);
                player.camera.planeX = args.checkdouble(5); player.camera.planeY = args.checkdouble(6);
                return LuaValue.NIL;
            }
        });

        // 엔티티 단위 효과음 요청을 기록한다.
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

        // 레지스트리에 등록된 오디오 에셋을 BGM으로 재생한다.
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

        // 현재 BGM을 정지한다.
        engineLib.set("stopBgm", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                soundEngine.stopBgm();
                return NIL;
            }
        });

        // Lua 스크립트에서 애플리케이션 종료를 요청한다.
        engineLib.set("exit", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                System.exit(0);
                return NIL;
            }
        });

        // 두 지점 사이에 벽 타일이 있는지 검사한다.
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

        // 두 엔티티 사이의 유클리드 거리를 계산한다.
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

    /**
     * 두 월드 좌표 사이를 일정 간격으로 샘플링해 벽 존재 여부를 확인한다.
     * Lua의 시야 판정과 공격 판정에서 공통으로 사용된다.
     */
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
            // 선분을 일정 간격으로 샘플링해 중간 타일이 벽인지 확인한다.
            // 시작점/끝점은 엔티티가 있는 칸일 수 있으므로 내부 샘플만 검사한다.
            double t = i / (double)steps;
            int tileX = (int)(x1 + dx * t);
            int tileY = (int)(y1 + dy * t);

            if (scene.getTile(tileX, tileY) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Lua에서 전달된 에셋 ID 또는 index 문자열을 정규 에셋 ID로 변환한다.
     */
    private String resolveAssetId(String idOrIndex) {
        String id = AssetRegistry.getIdByIndex(idOrIndex);

        if (id != null) {
            return id;
        }

        return idOrIndex;
    }
}
