package engine.script;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;
import engine.Entity.Entity;
import engine.audio.SoundEngine;
import engine.boot.Boot;
import engine.render.Texture;
import engine.render.UiManager;
import engine.boot.AssetRegistry;
import engine.Control;
import java.util.HashMap;
import java.util.Map;

public class ScriptManager {
    private final Globals globals;
    private final Map<String, LuaValue> updateCache = new HashMap<>();

    public ScriptManager(Boot boot, Texture textureCore, UiManager uiManager, SoundEngine soundEngine) {
        this.globals = JsePlatform.standardGlobals();
        new ScriptAPI(boot, textureCore, uiManager, soundEngine).register(globals);
    }

    public void updateEntity(Entity entity, double deltaTime, Entity player, Control control) {
        if (!entity.isActive || entity.scriptPath == null) return;
        if (!updateCache.containsKey(entity.scriptPath)) {
            try {
                globals.loadfile(entity.scriptPath).call();
                LuaValue updateFunc = globals.get("update");
                if (!updateFunc.isnil()) {
                    updateCache.put(entity.scriptPath, updateFunc);
                }
            } catch (Exception e) {
                System.err.println("[LUA ERROR] 스크립트 로드 실패: " + entity.scriptPath);
            }
        }
        LuaValue updateFunc = updateCache.get(entity.scriptPath);
        if (updateFunc != null) {
            try {
                updateFunc.invoke(LuaValue.varargsOf(new LuaValue[] {
                                CoerceJavaToLua.coerce(entity),
                                LuaValue.valueOf(deltaTime),
                                CoerceJavaToLua.coerce(player),
                                CoerceJavaToLua.coerce(control)
                }));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runScript(String scriptId) {
        String path = AssetRegistry.getPath(scriptId);
        System.out.println("[SCRIPT] Loading script: " + scriptId + " at path: " + path);

        if (path == null) {
            System.err.println("[ERROR] Script not found in Registry: " + scriptId);
        return;
        }

        try { 
            globals.loadfile(path).call(); 
            System.out.println("[SCRIPT] Execution success: " + scriptId); // 성공 로그
            } catch (Exception e) { 
                e.printStackTrace(); 
            }

    
    }
}