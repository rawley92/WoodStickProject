package engine.Entity;

import java.util.concurrent.atomic.AtomicInteger;

public class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    public final int entityId;

    public String name;
    public String assetId;
    public EntityType type;

    public String scriptName = "default";

    public PhysicsComponent physics;

    public CameraComponent camera;

    public RenderComponent render;

    public boolean isDynamic = false;
    public boolean isActive = false;
    public boolean isDestroyed = false;

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

        if (name.equalsIgnoreCase("Player")) {
            this.type = EntityType.PLAYER;
            this.camera = new CameraComponent(); 
        }
    }

    public enum EntityType {
        PLAYER, NPC, ITEM, PROP, PROJECTILE
    }

    @Override
    public String toString() {
        return "[Entity #" + entityId + "] " + name + " (" + physics.x + ", " + physics.y + ")";
    }
}