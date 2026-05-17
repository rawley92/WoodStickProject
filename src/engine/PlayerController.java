package engine;

public class PlayerController {

    private static final double MOVE_SPEED = 3.5;
    private static final double ROT_SPEED = 2.5;

    public void update(
            Entity player,
            Control input,
            double dt
    ) {

        if (player == null) {
            return;
        }

        // 회전
        if (input.s_turnLeft) {
            rotate(player, ROT_SPEED * dt);
        }

        if (input.s_turnRight) {
            rotate(player, -ROT_SPEED * dt);
        }

        // 이동
        double moveX = 0;
        double moveY = 0;

        if (input.s_up) {
            moveX += player.dirX * MOVE_SPEED * dt;
            moveY += player.dirY * MOVE_SPEED * dt;
        }

        if (input.s_down) {
            moveX -= player.dirX * MOVE_SPEED * dt;
            moveY -= player.dirY * MOVE_SPEED * dt;
        }

        player.velX = moveX;
        player.velY = moveY;
    }

    private void rotate(Entity e, double angle) {

        double oldDirX = e.dirX;

        e.dirX =
                e.dirX * Math.cos(angle)
                - e.dirY * Math.sin(angle);

        e.dirY =
                oldDirX * Math.sin(angle)
                + e.dirY * Math.cos(angle);

        double oldPlaneX = e.planeX;

        e.planeX =
                e.planeX * Math.cos(angle)
                - e.planeY * Math.sin(angle);

        e.planeY =
                oldPlaneX * Math.sin(angle)
                + e.planeY * Math.cos(angle);
    }
}