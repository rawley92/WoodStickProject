package engine.render;

/**
 * 렌더링된 픽셀에 안개, 틴트, 포스터라이즈 같은 단순 색상 후처리를 적용한다.
 */
public class Shader {

    private double fogDensity = 0.15;
    private int fogColor = 0x000000;      
    private int maxVisibleDistance = 15;  

    /**
     * 거리에 따라 픽셀 색상을 안개 색상으로 보간한다.
     * 색상 채널 분해와 보간 계산은 이 메서드 내부에서 처리한다.
     */
    public int applyFog(int color, double distance) {
        int a = (color >> 24) & 0xFF;

        if (a == 0) {
            return 0x00000000;
        }

        int fogR = (fogColor >> 16) & 0xFF;
        int fogG = (fogColor >> 8) & 0xFF;
        int fogB = fogColor & 0xFF;

        if (distance > maxVisibleDistance) {
            return (a << 24) | (fogR << 16) | (fogG << 8) | fogB;
        }

        double effectiveDist = Math.max(0.0, distance - 1.0);
        double factor = 1.0 / (1.0 + (effectiveDist * fogDensity));

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int) (r * factor + fogR * (1.0 - factor));
        g = (int) (g * factor + fogG * (1.0 - factor));
        b = (int) (b * factor + fogB * (1.0 - factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 색상 채널을 지정 단계 단위로 계단화한다.
     * 현재 기본 렌더 파이프라인에서는 보조 후처리 기능으로 남아 있다.
     */
    public int applyPosterize(int color, int steps) {
        int a = (color >> 24) & 0xFF; 
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (r / steps) * steps;
        g = (g / steps) * steps;
        b = (b / steps) * steps;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 원본 색상과 tintColor를 intensity 비율로 보간한다.
     * 벽 방향 음영처럼 간단한 색상 변조가 필요할 때 사용한다.
     */
    public int tint(int color, int tintColor, double intensity) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) return 0x00000000;

        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;

        int r2 = (tintColor >> 16) & 0xFF;
        int g2 = (tintColor >> 8) & 0xFF;
        int b2 = tintColor & 0xFF;

        int r = (int) (r1 * (1 - intensity) + r2 * intensity);
        int g = (int) (g1 * (1 - intensity) + g2 * intensity);
        int b = (int) (b1 * (1 - intensity) + b2 * intensity);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
