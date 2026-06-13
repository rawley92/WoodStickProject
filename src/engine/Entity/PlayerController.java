package engine.Entity;

import engine.Control;

/**
 * 입력 스냅샷을 플레이어 이동과 카메라 회전으로 변환한다.
 * 충돌과 최종 위치 보정은 PhysicsCore가 담당한다.
 */
public class PlayerController {

    private static final double MOVE_SPEED = 2.5;
    private static final double ROT_SPEED = 2.5;

    /**
     * 입력 상태를 플레이어의 이동 속도와 카메라 방향 변화로 변환한다.
     * 회전 계산은 rotate()가 담당하고, 실제 벽 충돌 반영은 PhysicsCore가 처리한다.
     */
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

        // 전후 이동은 카메라 전방 벡터를 사용한다.
        if (input.s_up) {
            moveX += cam.dirX;
            moveY += cam.dirY;
        }

        if (input.s_down) {
            moveX -= cam.dirX;
            moveY -= cam.dirY;
        }

        // 좌우 이동은 전방 벡터에 수직인 벡터를 사용해 strafing을 만든다.
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
            // 대각선 이동이 더 빨라지지 않도록 입력 벡터를 정규화한 뒤 속도에 더한다.
            phys.velX += (moveX / length) * MOVE_SPEED * dt;
            phys.velY += (moveY / length) * MOVE_SPEED * dt;
        }
    }

    /**
     * 카메라 방향 벡터와 투영 평면을 같은 각도로 회전시킨다.
     * 레이캐스팅 렌더러는 이 두 벡터를 기준으로 시야를 계산한다.
     */
    private void rotate(CameraComponent c, double angle) {

        double oldDirX = c.dirX;

        // 2D 회전 행렬을 dir 벡터에 적용한다.
        c.dirX = c.dirX * Math.cos(angle) - c.dirY * Math.sin(angle);
        c.dirY = oldDirX * Math.sin(angle) + c.dirY * Math.cos(angle);

        double oldPlaneX = c.planeX;

        // plane 벡터도 같은 각도로 회전해야 시야 폭이 카메라 방향과 함께 유지된다.
        c.planeX = c.planeX * Math.cos(angle) - c.planeY * Math.sin(angle);
        c.planeY = oldPlaneX * Math.sin(angle) + c.planeY * Math.cos(angle);
    }
}
