package engine.Entity;

/**
 * 엔티티 렌더링에 필요한 에셋 ID와 표시 상태를 보관하는 데이터 컴포넌트다.
 */
public class RenderComponent {

    public String assetId;

    public int currentFrame = 0;
    public boolean visible = true;
    public double rotation = 0;
    public double scale = 1.0;
    public double animationTimer = 0;
}
