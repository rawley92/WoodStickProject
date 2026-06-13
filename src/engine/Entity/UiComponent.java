package engine.Entity;

/**
 * 엔티티에 고정 부착되는 UI 이미지 상태를 보관한다.
 */
public class UiComponent {

    public String uiId;

    public boolean visible = true;

    public String currentTextureId;

    public int x = 0;
    public int y = 0;

    public int frame = 0;
    public double animTime = 0;
}
