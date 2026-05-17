package engine;

import java.util.concurrent.atomic.AtomicInteger;

public class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    public final int entityId;

    public String name;
    public String assetId;
    public EntityType type;

    public String scriptName = "default";

    public double x;
    public double y;
    public double velX;
    public double velY;
    public double rotation;

    public double dirX;
    public double dirY;
    public double planeX;
    public double planeY;

    public boolean isDynamic = false;
    public boolean isActive = false;
    public boolean isDestroyed = false;


    public int currentFrame = 0;
    public int totalFrames = 1;
    public double animTimer = 0;
    public double frameDuration = 0.2;
    public boolean skipAnimUpdate = false;
    public double distSq;

    public Entity(String name, String assetId, double x, double y) {
        this.entityId = NEXT_ID.getAndIncrement();
        this.name = name;
        this.assetId = assetId;
        this.x = x;
        this.y = y;

        this.dirX = -1.0;
        this.dirY = 0.0;
        this.planeX = 0.0;
        this.planeY = 0.9;
        this.type = EntityType.PROP;
    }
    public void applyForce(double fx, double fy) {
        if (!isDynamic) return;
        this.velX += fx;
        this.velY += fy;
    }

    public void setVelocity(double vx, double vy) {
        this.velX = vx;
        this.velY = vy;
    }

    public void stop() {
        this.velX = 0;
        this.velY = 0;
    }

    public double distanceSq(double tx, double ty) {
        double dx = tx - this.x;
        double dy = ty - this.y;
        return dx * dx + dy * dy;
    }

    public enum EntityType {
        PLAYER, NPC, ITEM, PROP, PROJECTILE
    }

    @Override
    public String toString() {
        return "[Entity #" + entityId + "] " + name + " (" + x + ", " + y + ")";
    }
}