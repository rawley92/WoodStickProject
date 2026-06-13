package engine.boot;

/**
 * 엔진 실행에 필요한 전역 설정값을 담는다.
 * 현재는 파일 파싱보다 코드 기본값을 제공하는 단순 설정 모델이다.
 */
public class Config {

    public int baseWidth;  
    public int baseHeight;  
    public int scale;     

    public int targetFps;  
    public boolean vsync;  

    public String initialLevel; 
    public boolean debugMode;   
    
    public double masterVolume;

    /**
     * 엔진 전역 설정의 기본값을 구성한다.
     * 파일 기반 설정 파싱이 추가되기 전까지 이 값들이 런타임 기본값으로 사용된다.
     */
    public Config() {
        this.baseWidth = 1280;
        this.baseHeight = 720;
        this.scale = 1; 
        this.targetFps = 60;
        this.initialLevel = "Level";
        this.debugMode = false;
        this.masterVolume = 0.5;
    }

    /**
     * 논리 해상도와 스케일을 반영한 출력 폭을 반환한다.
     */
    public int getDisplayWidth() {
        return baseWidth * scale;
    }

    /**
     * 논리 해상도와 스케일을 반영한 출력 높이를 반환한다.
     */
    public int getDisplayHeight() {
        return baseHeight * scale;
    }
    
}
