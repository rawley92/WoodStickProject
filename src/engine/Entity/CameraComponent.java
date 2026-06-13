package engine.Entity;

/**
 * 레이캐스팅 카메라의 전방 벡터와 투영 평면 벡터를 보관한다.
 */
public class CameraComponent {

    public double dirX = 1.0;
    public double dirY = 0.0;

    public double planeX = 0.0;
    public double planeY = 0.9;
}
