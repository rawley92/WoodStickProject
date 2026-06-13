package engine.script;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;

import engine.Entity.Entity;
import engine.audio.SoundEngine;
import engine.boot.AssetRegistry;
import engine.boot.Boot;
import engine.render.Texture;
import engine.render.UiManager;
import engine.Control;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Lua 스크립트 파일을 Java 엔진 루프에 연결하는 실행 관리자다.
 * 스크립트별 환경과 update 함수 캐시를 관리해 매 프레임 호출 비용과 상태 범위를 제어한다.
 */
public class ScriptManager {

    private final Globals globals;
    private final Map<String, LuaTable> scriptEnvironments = new HashMap<>();
    private final Map<String, LuaValue> updateCache = new HashMap<>();
    private final Map<String, Boolean> forceReloadFlag = new HashMap<>();


    /**
     * Lua 실행 환경을 만들고 엔진 API를 등록한다.
     * 실제 API 함수 바인딩은 ScriptAPI.register()가 담당한다.
     */
    public ScriptManager(
            Boot boot,
            Texture textureCore,
            UiManager uiManager,
            SoundEngine soundEngine
    ) {
        // LuaJ의 표준 라이브러리를 포함한 전역 환경을 만든다.
        // 각 스크립트 env는 이 globals를 __index로 참조한다.
        this.globals = JsePlatform.standardGlobals();

        new ScriptAPI(
                boot,
                textureCore,
                uiManager,
                soundEngine
        ).register(globals);
    }

    /**
     * Lua 파일을 독립 환경으로 로드하고 update 함수를 캐싱한다.
     * 캐시 재사용 여부와 강제 재로드 정책은 forceReload 인자로 결정한다.
     */
    private void loadScript(String scriptPath, boolean forceReload) {

        // 이미 로드된 스크립트는 환경과 update 함수를 재사용한다.
        // forceReload는 title/main 같은 부트 스크립트를 새로 실행해야 할 때 사용한다.
        if (!forceReload && scriptEnvironments.containsKey(scriptPath)) {
            return;
        }

        try {
            // 스크립트마다 별도 env를 부여해 local/global 상태가 파일 단위로 유지되게 한다.
            LuaTable env = new LuaTable();

            // env에서 찾지 못한 함수/값은 globals에서 찾게 해 engine, math, dofile 등을 공유한다.
            LuaTable meta = new LuaTable();
            meta.set("__index", globals);
            env.setmetatable(meta);

            LuaValue chunk;

            try (FileInputStream stream = new FileInputStream(scriptPath)) {
                // "bt"는 LuaJ가 binary/text chunk를 허용하게 하는 모드다.
                chunk = globals.load(stream, scriptPath, "bt", env);
            }

            // 파일 최상위 코드를 실행한다.
            // 이 과정에서 function update(...)가 env에 등록되거나 씬 로드 코드가 즉시 실행된다.
            chunk.call();

            scriptEnvironments.put(scriptPath, env);

            LuaValue updateFunc = env.get("update");
            if (!updateFunc.isnil()) {
                // 매 프레임 env lookup을 반복하지 않도록 update 함수 참조를 캐싱한다.
                updateCache.put(scriptPath, updateFunc);
            }

            System.out.println("[SCRIPT] Loaded: " + scriptPath);

        } catch (Exception e) {
            System.err.println("[LUA ERROR] load failed: " + scriptPath);
            e.printStackTrace();
        }
    }
   
    /**
     * 엔티티에 연결된 Lua update 함수를 호출한다.
     * 스크립트 로드와 update 함수 캐싱은 loadScript()가 처리한다.
     */
    public void updateEntity(
            Entity entity,
            double deltaTime,
            Entity player,
            Control control
    ) {

        if (!entity.isActive) return;
        if (entity.scriptPath == null) return;

        String path = entity.scriptPath;

        // spawnEntity 시점에는 scriptPath만 연결되며, 실제 Lua 파일 로드는 첫 update에서 지연 수행된다.
        if (!scriptEnvironments.containsKey(path)) {
            loadScript(path, false);
        }

        LuaValue updateFunc = updateCache.get(path);
        if (updateFunc == null) return;

        try {
            // Java 객체는 Lua userdata로 전달된다.
            // Lua는 entity.physics.x처럼 public 필드에 직접 접근한다.
            updateFunc.invoke(LuaValue.varargsOf(new LuaValue[]{
                    entity.getLuaWrapper(),
                    LuaValue.valueOf(deltaTime),
                    player.getLuaWrapper(),
                    control.getLuaWrapper()
            }));
        } catch (Exception e) {
            System.err.println("[LUA ERROR] update failed: " + path);
            e.printStackTrace();
        }
    }

    /**
     * 모든 Lua 스크립트 환경과 update 캐시를 제거한다.
     * 씬 전환이나 디버그 리로드에서 전역 스크립트 상태를 초기화할 때 사용한다.
     */
    public void resetAllScripts() {
        scriptEnvironments.clear();
        updateCache.clear();
        forceReloadFlag.clear();

        System.out.println("[SCRIPT] All script states reset");
    }


    /**
     * AssetRegistry의 스크립트 ID를 실제 파일 경로로 변환해 실행한다.
     */
    public void runScript(String scriptId) {
        String path = AssetRegistry.getPath(scriptId);
        if (path == null) return;
        loadScript(path, true);
    }

    /**
     * 특정 Lua 파일의 캐시를 제거하고 다시 로드한다.
     */
    public void reloadScript(String scriptPath) {
        scriptEnvironments.remove(scriptPath);
        updateCache.remove(scriptPath);
        loadScript(scriptPath, true);
    }
}
