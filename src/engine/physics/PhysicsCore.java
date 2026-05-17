package engine.physics;

import engine.Scene;
import engine.Entity;
import java.util.List;

public class PhysicsCore {

    private final double GRAVITY = -9.8; // 필요 시 중력 적용
    private final double FRICTION = 0.9;  // 마찰력

    /**
     * 메인 물리 업데이트 루프
     * @param scene 현재 활성화된 씬 데이터
     * @param dt    델타타임 (지난 프레임과의 시간 간격)
     */
    public void update(Scene scene, double dt) {
        if (scene == null) return;

        List<Entity> entities = scene.getEntities();

        for (Entity e : entities) {
            if (!e.isDynamic) continue;
            applyMovement(e, scene, dt);

            if (e.isActive) {
                updateActiveLogic(e, dt); 
            } else {
                updatePassiveLogic(e, dt); 
            }

            updateAnimationTick(e, dt);
        }
    }

    private void applyMovement(Entity e, Scene scene, double dt) {
        double nextX = e.x + e.velX; 
        double nextY = e.y + e.velY;

        if (!isWall(scene, nextX, e.y)) {
            e.x = nextX;
        } else {
            e.velX = 0; 
        }

        if (!isWall(scene, e.x, nextY)) {
            e.y = nextY;
        } else {
            e.velY = 0;
        }
        e.velX *= 0.5; 
        e.velY *= 0.5;
    }

    private boolean isWall(Scene scene, double x, double y) {

        double radius = 0.42;

        int left = (int)(x - radius);
        int right = (int)(x + radius);

        int top = (int)(y - radius);
        int bottom = (int)(y + radius);

        return
            scene.getTile(left, top) > 0 ||
            scene.getTile(right, top) > 0 ||
            scene.getTile(left, bottom) > 0 ||
            scene.getTile(right, bottom) > 0;
    }

    private void updateActiveLogic(Entity e, double dt) {
    }

    private void updatePassiveLogic(Entity e, double dt) {
    }

    private void updateAnimationTick(Entity e, double dt) {
        if (e.totalFrames <= 1) return;
        e.animTimer += dt;
        if (e.animTimer >= e.frameDuration) {
            e.animTimer = 0;
            e.currentFrame =
                (e.currentFrame + 1) % e.totalFrames;
        }
    }

    public void activatePhysics(Entity e) {
        e.isDynamic = true;
    }
}