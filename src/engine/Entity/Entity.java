package engine.Entity;

import java.util.concurrent.atomic.AtomicInteger;
import engine.audio.SoundEngine;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    public final int entityId;

    public String name;
    public String assetId;
    public EntityType type;
    public String scriptPath;
    public String scriptName = "default";

    public PhysicsComponent physics;
    public CameraComponent camera;
    public RenderComponent render;
    public SoundComponent sound;
    public SoundEngine soundEngine;
    public UiComponent ui;

    public boolean isDynamic = false;
    public boolean isActive = false;
    public boolean isDestroyed = false;
    public boolean soundTriggered = false;

    private transient LuaValue luaWrapper = null;

    public Entity(String name, String assetId, double x, double y) {
        this.entityId = NEXT_ID.getAndIncrement();
        this.name = name;
        this.assetId = assetId;

        this.physics = new PhysicsComponent();
        this.physics.x = x;
        this.physics.y = y;

        this.render = new RenderComponent();
        this.render.assetId = assetId;

        this.type = EntityType.PROP;
        this.sound = new SoundComponent();
        this.ui = null;

        if (name.equalsIgnoreCase("Player")) {
            this.type = EntityType.PLAYER;
            this.camera = new CameraComponent(); 
        }
    }

    public LuaValue getLuaWrapper() {
        if (this.luaWrapper == null) {
            this.luaWrapper = CoerceJavaToLua.coerce(this);
        }
        return this.luaWrapper;
    }

    public enum EntityType {
        PLAYER, NPC, ITEM, PROP, PROJECTILE
    }

    public PhysicsComponent getPhysics() {
        return physics;
    }

    @Override
    public String toString() {
        return "[Entity #" + entityId + "] " + name + " (" + physics.x + ", " + physics.y + ")";
    }
}
