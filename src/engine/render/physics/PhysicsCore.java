package engine.render.physics;

import engine.Scene;
import engine.Entity.Entity;
import engine.Entity.PhysicsComponent;

import java.util.List;

/**
 * 씬의 동적 엔티티 위치를 갱신하고 타일맵 벽 충돌을 적용한다.
 * 게임 규칙은 Lua가 주도하고, 이 클래스는 최종 이동 제약을 담당한다.
 */
public class PhysicsCore {

    private final double GRAVITY = -9.8;
    private final double FRICTION = 0.9;

    /**
     * 씬의 동적 엔티티에 물리 갱신을 적용한다.
     * 실제 이동/충돌은 applyMovement(), 확장 훅은 updateActiveLogic()/updatePassiveLogic()이 담당한다.
     */
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

    /**
     * 엔티티의 속도를 위치에 반영하고 벽 충돌을 처리한다.
     * 충돌 판정의 구체적인 타일 검사는 isWall()에 위임한다.
     */
    private void applyMovement(Entity e, Scene scene, double dt) {

        PhysicsComponent p = e.physics;

        // 현재 가속도를 속도에 누적한다.
        // Lua AI가 velX/velY를 직접 설정하는 경우도 있으므로 기존 속도를 보존한다.
        p.velX += p.accX * dt;
        p.velY += p.accY * dt;

        double nextX = p.x + p.velX;
        double nextY = p.y + p.velY;

        if (p.noClip) {
            p.x = nextX;
            p.y = nextY;
            p.velX *= 0.05;
            p.velY *= 0.05;
            return;
        }

        // X축과 Y축을 분리해서 검사하면 벽에 비스듬히 닿았을 때 한 축 이동은 유지된다.
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

        // 현재 구현의 vel 값은 프레임 이동량에 가깝기 때문에 강한 감쇠로 다음 프레임 잔여 이동을 줄인다.
        p.velX *= 0.05;
        p.velY *= 0.05;
    }

    /**
     * 엔티티 반경을 기준으로 다음 위치가 벽과 겹치는지 검사한다.
     * Scene.getTile()이 맵 바깥을 벽으로 처리하므로 경계 충돌도 함께 해결된다.
     */
    private boolean isWall(Scene scene, double x, double y) {
        double radius = 0.42;

        // 원형 충돌체를 간단한 네 모서리 샘플로 근사한다.
        int left = (int)(x - radius);
        int right = (int)(x + radius);
        int top = (int)(y - radius);
        int bottom = (int)(y + radius);

        // 네 지점 중 하나라도 벽이면 해당 위치로 이동할 수 없다.
        return scene.getTile(left, top) > 0 ||
            scene.getTile(right, top) > 0 ||
            scene.getTile(left, bottom) > 0 ||
            scene.getTile(right, bottom) > 0;
    }

    /**
     * 활성 엔티티의 물리 후처리를 위한 확장 지점이다.
     * 현재 게임 규칙은 Lua update가 주도하므로 기본 구현은 비어 있다.
     */
    private void updateActiveLogic(Entity e, double dt) {
    }

    /**
     * 비활성 엔티티의 물리 후처리를 위한 확장 지점이다.
     */
    private void updatePassiveLogic(Entity e, double dt) {
    }

    /**
     * 렌더 애니메이션 프레임 진행을 위한 확장 지점이다.
     * 현재 스프라이트 선택은 주로 Lua 스크립트가 entity.assetId를 바꾸는 방식으로 처리한다.
     */
    private void updateAnimationTick(Entity e, double dt) {

        if (e.render == null) return;
        if (e.render.currentFrame < 0) return;

        // (기존 로직 유지 가능)
    }

    /**
     * 엔티티를 물리 갱신 대상으로 활성화한다.
     */
    public void activatePhysics(Entity e) {
        e.isDynamic = true;
    }
}
