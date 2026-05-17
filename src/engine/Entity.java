package engine;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * [Engine/Boot/Entity]
 * 게임 내 모든 객체(플레이어, NPC, 아이템 등)의 상태 데이터 구조체.
 */
public class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    public final int entityId;

    // =========================
    // Basic Info
    // =========================
    public String name;
    public String assetId;
    public EntityType type;

    /**
     * 루아 스크립트에서 이 NPC를 제어할 때 식별할 스크립트 키 (예: "guard", "slime")
     */
    public String scriptName = "default";

    // =========================
    // Transform
    // =========================
    public double x;
    public double y;
    public double velX;
    public double velY;
    public double rotation;

    // =========================
    // Direction / Camera
    // =========================
    public double dirX;
    public double dirY;
    public double planeX;
    public double planeY;

    // =========================
    // Physics / Logic Flags
    // =========================
    public boolean isDynamic = false;
    public boolean isActive = false;
    public boolean isDestroyed = false;

    // =========================
    // Animation (이제 Lua에서 상태별로 직접 커스텀 제어 가능)
    // =========================
    public int currentFrame = 0;
    public int totalFrames = 1;
    public double animTimer = 0;
    public double frameDuration = 0.2;

    // =========================
    // Render Temporary Data
    // =========================
    public double distSq;

    // =========================
    // Constructor
    // =========================
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

    // =========================
    // Physics Bridges (루아에서 편하게 호출할 수 있는 유틸 메서드들)
    // =========================
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