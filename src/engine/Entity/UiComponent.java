package engine.Entity;

public class UiComponent {

    public String uiId;

    public boolean visible = true;

    public String currentTextureId;

    public int x = 0;
    public int y = 0;

    // 미래 확장용 (애니메이션 대비)
    public int frame = 0;
    public double animTime = 0;
}