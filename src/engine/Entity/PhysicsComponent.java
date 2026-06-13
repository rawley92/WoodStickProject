package engine.Entity;

/**
 * 엔티티의 월드 위치와 이동량을 보관하는 물리 데이터 컴포넌트다.
 */
public class PhysicsComponent {

    public double x;
    public double y;

    public double velX;
    public double velY;
    
    public double accX = 0;
    public double accY = 0;


    public double radius = 0.3;

    /**
     * 물리 이동을 즉시 멈춘다.
     * 위치나 가속도는 유지하고 현재 속도만 초기화한다.
     */
    public void stop() {
        velX = 0;
        velY = 0;
    }
}
