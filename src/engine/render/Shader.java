package engine.render;

/**
 * [Engine/Render/Shader]
 * 픽셀의 최종 색상을 결정하는 소프트웨어 셰이더.
 * 거리(Depth)에 따른 명암 감쇠 및 색상 제한 효과 담당.
 */
public class Shader {

    // 안개 및 조명 설정
    private double fogDensity = 0.15;
    private int fogColor = 0x000000;      // 안개 색상 (0xRRGGBB)
    private int maxVisibleDistance = 15;  // 이 거리 이상은 완전히 안개 색상으로 고정

    /**
     * [핵심 연산] 거리에 따른 픽셀 색상 감쇠 (임의의 안개 색상 지원 및 부드러운 전이)
     * @param color    Texture에서 가져온 원본 색상 (0xAARRGGBB)
     * @param distance 카메라로부터의 수직 거리 (Z-Buffer 값)
     * @return         변조된 최종 색상
     */
    public int applyFog(int color, double distance) {
        int a = (color >> 24) & 0xFF;

        // 완전 투명한 픽셀은 계산 생략
        if (a == 0) {
            return 0x00000000;
        }

        // 안개 색상의 R, G, B 분리
        int fogR = (fogColor >> 16) & 0xFF;
        int fogG = (fogColor >> 8) & 0xFF;
        int fogB = fogColor & 0xFF;

        // 최대 가시거리를 넘으면 완전히 안개 색상 반환 (알파 유지)
        if (distance > maxVisibleDistance) {
            return (a << 24) | (fogR << 16) | (fogG << 8) | fogB;
        }

        // 거리 1.0 이전까지는 100% 밝기를 유지하다가, 그 이후부터 부드럽게 감쇠 시작 (밝기 툭 떨어짐 방지)
        double effectiveDist = Math.max(0.0, distance - 1.0);
        double factor = 1.0 / (1.0 + (effectiveDist * fogDensity));

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // 임의의 안개 색상과 부드럽게 섞이도록 선형 보간 연산 (Linear Interpolation)
        r = (int) (r * factor + fogR * (1.0 - factor));
        g = (int) (g * factor + fogG * (1.0 - factor));
        b = (int) (b * factor + fogB * (1.0 - factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * [레트로 효과] 색상 수 제한 (Posterization)
     * 픽셀 아트 느낌을 강하게 주기 위해 색상 단계를 의도적으로 줄임.
     */
    public int applyPosterize(int color, int steps) {
        int a = (color >> 24) & 0xFF; // 알파 채널 추출
        
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (r / steps) * steps;
        g = (g / steps) * steps;
        b = (b / steps) * steps;

        // 반드시 알파 채널(a)을 다시 합성해서 반환해야 투명화 버그가 안 생깁니다.
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * [특수 효과] 특정 색상 강조 (예: 피격 시 붉은색 오버레이, 벽면 어두운 쪽 음영 처리)
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