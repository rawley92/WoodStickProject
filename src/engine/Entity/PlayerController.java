package engine.Entity;

import engine.Control;

public class PlayerController {

    private static final double MOVE_SPEED = 2.5;
    private static final double ROT_SPEED = 2.5;

    public void update(Entity player, Control input, double dt) {

        if (player == null) return;

        PhysicsComponent phys = player.physics;
        CameraComponent cam = player.camera;

        if (input.s_turnLeft) {
            rotate(cam, ROT_SPEED * dt);
        }

        if (input.s_turnRight) {
            rotate(cam, -ROT_SPEED * dt);
        }

        double moveX = 0.0;
        double moveY = 0.0;

        if (input.s_up) {
            moveX += cam.dirX;
            moveY += cam.dirY;
        }

        if (input.s_down) {
            moveX -= cam.dirX;
            moveY -= cam.dirY;
        }

        if (input.s_right) {
            moveX += cam.dirY;
            moveY -= cam.dirX;
        }

        if (input.s_left) {
            moveX -= cam.dirY;
            moveY += cam.dirX;
        }

        double length = Math.sqrt(moveX * moveX + moveY * moveY);

        if (length > 0.0) {
            phys.velX += (moveX / length) * MOVE_SPEED * dt;
            phys.velY += (moveY / length) * MOVE_SPEED * dt;
        }
    }

    private void rotate(CameraComponent c, double angle) {

        double oldDirX = c.dirX;

        c.dirX = c.dirX * Math.cos(angle) - c.dirY * Math.sin(angle);
        c.dirY = oldDirX * Math.sin(angle) + c.dirY * Math.cos(angle);

        double oldPlaneX = c.planeX;

        c.planeX = c.planeX * Math.cos(angle) - c.planeY * Math.sin(angle);
        c.planeY = oldPlaneX * Math.sin(angle) + c.planeY * Math.cos(angle);
    }
}
