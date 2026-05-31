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

public class ScriptManager {

    private final Globals globals;
    private final Map<String, LuaTable> scriptEnvironments = new HashMap<>();
    private final Map<String, LuaValue> updateCache = new HashMap<>();
    private final Map<String, Boolean> forceReloadFlag = new HashMap<>();


    public ScriptManager(
            Boot boot,
            Texture textureCore,
            UiManager uiManager,
            SoundEngine soundEngine
    ) {
        this.globals = JsePlatform.standardGlobals();

        new ScriptAPI(
                boot,
                textureCore,
                uiManager,
                soundEngine
        ).register(globals);
    }

     private void loadScript(String scriptPath, boolean forceReload) {

        if (!forceReload && scriptEnvironments.containsKey(scriptPath)) {
            return;
        }

        try {
            LuaTable env = new LuaTable();

            LuaTable meta = new LuaTable();
            meta.set("__index", globals);
            env.setmetatable(meta);

            LuaValue chunk;

            try (FileInputStream stream = new FileInputStream(scriptPath)) {
                chunk = globals.load(stream, scriptPath, "bt", env);
            }

            chunk.call();

            scriptEnvironments.put(scriptPath, env);

            LuaValue updateFunc = env.get("update");
            if (!updateFunc.isnil()) {
                updateCache.put(scriptPath, updateFunc);
            }

            System.out.println("[SCRIPT] Loaded: " + scriptPath);

        } catch (Exception e) {
            System.err.println("[LUA ERROR] load failed: " + scriptPath);
            e.printStackTrace();
        }
    }
   
    public void updateEntity(
            Entity entity,
            double deltaTime,
            Entity player,
            Control control
    ) {

        if (!entity.isActive) return;
        if (entity.scriptPath == null) return;

        String path = entity.scriptPath;

        if (!scriptEnvironments.containsKey(path)) {
            loadScript(path, false);
        }

        LuaValue updateFunc = updateCache.get(path);
        if (updateFunc == null) return;

        try {
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

    public void resetAllScripts() {
            scriptEnvironments.clear();
            updateCache.clear();
            forceReloadFlag.clear();

            System.out.println("[SCRIPT] All script states reset");
        }


    public void runScript(String scriptId) {
        String path = AssetRegistry.getPath(scriptId);
        if (path == null) return;
        loadScript(path, true);
    }

    public void reloadScript(String scriptPath) {
        scriptEnvironments.remove(scriptPath);
        updateCache.remove(scriptPath);
        loadScript(scriptPath, true);
    }
}