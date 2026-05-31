package engine.render;

public class Shader {

    private double fogDensity = 0.15;
    private int fogColor = 0x000000;      
    private int maxVisibleDistance = 15;  

    /**
     * [핵심 연산] 거리에 따른 픽셀 색상 감쇠 (임의의 안개 색상 지원 및 부드러운 전이)
     * @param color    Texture에서 가져온 원본 색상 (0xAARRGGBB)
     * @param distance 카메라로부터의 수직 거리 (Z-Buffer 값)
     * @return         변조된 최종 색상
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