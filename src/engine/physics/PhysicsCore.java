package engine.physics;

import engine.Scene;
import engine.Entity; // 엔티티 구조체 가정
import java.util.List;

/**
 * [Engine/Physics/Core]
 * 객체의 운동 법칙, 충돌 감지, 애니메이션 상태 갱신 담당.
 * 모든 연산은 속성(Static/Dynamic, Active/Passive)에 따라 분산 처리됨.
 */
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
            // 1. 상태 체크: 정적(Static) 오브젝트는 물리 연산을 건너뜀
            if (!e.isDynamic) continue;

            // 2. 동적 연산: 위치 및 속도 갱신
            applyMovement(e, scene, dt);

            // 3. 로직 분류: 능동(Active) 객체와 비능동(Passive) 객체 분리 처리
            if (e.isActive) {
                updateActiveLogic(e, dt); // AI, 자가 이동 등
            } else {
                updatePassiveLogic(e, dt); // 들기, 밀기, 단순 물리 반응
            }

            // 4. 애니메이션 상태 업데이트
            updateAnimationTick(e, dt);
        }
    }

    /**
     * 기본 운동 법칙 적용 (가속도 -> 속도 -> 위치)
     */
    private void applyMovement(Entity e, Scene scene, double dt) {
        // 1. 다음 예상 위치 계산 (PlayerController가 이미 dt를 곱한 moveX를 주었다면 중복 곱셈 주의)
        // 현재 PlayerController가 velX = moveX * dt 형태라면 여기서는 바로 더해줍니다.
        double nextX = e.x + e.velX; 
        double nextY = e.y + e.velY;

        // 2. X축 충돌 체크 및 이동
        if (!isWall(scene, nextX, e.y)) {
            e.x = nextX;
        } else {
            e.velX = 0; // 벽에 막히면 속도 초기화
        }

        // 3. Y축 충돌 체크 및 이동
        if (!isWall(scene, e.x, nextY)) {
            e.y = nextY;
        } else {
            e.velY = 0;
        }

        // [중요] 위치에 반영된 속도는 매 프레임 초기화하거나 감쇠시켜 
        // 키를 뗐을 때 바로 멈추게 하거나 관성을 조절합니다.
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
    /**
     * 능동(Active) 객체: 사람, 차량 등 스스로 로직을 가짐
     */
    private void updateActiveLogic(Entity e, double dt) {
        // e.script (Data/Char/npc1/script.txt에서 로드된 내용) 실행 유도
        // 예: 플레이어를 향해 가속도를 주는 간단한 추적 로직
        // e.velX += Math.cos(e.rotation) * e.speed * dt;
    }

    /**
     * 비능동(Passive) 객체: 비누, 상자 등 외부 힘에 의해서만 움직임
     */
    private void updatePassiveLogic(Entity e, double dt) {
        // 외부 트리거(플레이어가 밟음, 근처 폭발 등)가 있을 때만 동적으로 전환되거나 튕김
        // 현재는 별도 자가 로직 없음
    }

    /**
     * 애니메이션 프레임 갱신
     */
    private void updateAnimationTick(Entity e, double dt) {

        if (e.totalFrames <= 1) return;

        e.animTimer += dt;

        if (e.animTimer >= e.frameDuration) {

            e.animTimer = 0;

            e.currentFrame =
                (e.currentFrame + 1) % e.totalFrames;
        }
    }
    /**
     * 속성 전환 기능: 정적 -> 동적 (예: 상자를 때렸을 때 활성화)
     */
    public void activatePhysics(Entity e) {
        e.isDynamic = true;
        // 필요 시 여기서 초기 충격량(Impulse) 부여 가능
    }
}