package engine.physics;

import engine.Scene;
import engine.Entity.Entity;
import engine.Entity.PhysicsComponent;

import java.util.List;

public class PhysicsCore {

    private final double GRAVITY = -9.8;
    private final double FRICTION = 0.9;

    public void update(Scene scene, double dt) {
        if (scene == null) return;

        List<Entity> entities = scene.getEntities();

        for (Entity e : entities) {

            if (!e.isDynamic || e.physics == null) continue;

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

        PhysicsComponent p = e.physics;

        p.velX += p.accX * dt;
        p.velY += p.accY * dt;

        double nextX = p.x + p.velX;
        double nextY = p.y + p.velY;

        if (!isWall(scene, nextX, p.y)) {
            p.x = nextX;
        } else {
            p.velX = 0;
        }

        if (!isWall(scene, p.x, nextY)) {
            p.y = nextY;
        } else {
            p.velY = 0;
        }

        p.velX *= 0.05;
        p.velY *= 0.05;
    }

    private boolean isWall(Scene scene, double x, double y) {
        double radius = 0.42;

        int left = (int)(x - radius);
        int right = (int)(x + radius);
        int top = (int)(y - radius);
        int bottom = (int)(y + radius);

        return scene.getTile(left, top) > 0 ||
            scene.getTile(right, top) > 0 ||
            scene.getTile(left, bottom) > 0 ||
            scene.getTile(right, bottom) > 0;
    }

    private void updateActiveLogic(Entity e, double dt) {
    }

    private void updatePassiveLogic(Entity e, double dt) {
    }

    private void updateAnimationTick(Entity e, double dt) {

        if (e.render == null) return;
        if (e.render.currentFrame < 0) return;

        // (기존 로직 유지 가능)
    }

    public void activatePhysics(Entity e) {
        e.isDynamic = true;
    }
}
