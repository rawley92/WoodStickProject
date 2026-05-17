package engine.boot;

/**
 * [Engine/Boot/Config]
 * 엔진의 전역 설정 데이터 구조체.
 * Boot 과정에서 외부 JSON/파일로부터 데이터를 주입받음.
 */
public class Config {

    // 1. 디스플레이 설정 (해상도 및 스케일링)
    public int baseWidth;   // 내부 렌더링 가로 해상도 (예: 640)
    public int baseHeight;  // 내부 렌더링 세로 해상도 (예: 360)
    public int scale;       // 화면 출력 배율 (예: 2 -> 1280x720)
    
    // 2. 성능 설정
    public int targetFps;   // 목표 프레임 (예: 60)
    public boolean vsync;   // 수직 동기화 여부

    // 3. 엔진 동작 설정
    public String initialLevel; // 시작 시 로드할 맵 이름
    public boolean debugMode;   // 디버그 정보(FPS, 위치 등) 표시 여부
    
    // 4. 사운드 설정
    public double masterVolume; // 0.0 ~ 1.0

    /**
     * 기본 생성자: 기본값 설정
     */
    public Config() {
        this.baseWidth = 1280;
        this.baseHeight = 720;
        this.scale = 1; // 기본 1280x720 출력
        this.targetFps = 60;
        this.initialLevel = "Level";
        this.debugMode = false;
        this.masterVolume = 0.5;
    }

    /**
     * 실제 출력되는 윈도우 가로 크기 반환
     */
    public int getDisplayWidth() {
        return baseWidth * scale;
    }

    /**
     * 실제 출력되는 윈도우 세로 크기 반환
     */
    public int getDisplayHeight() {
        return baseHeight * scale;
    }
    
}