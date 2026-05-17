package engine.boot;

public class Config {

    public int baseWidth;  
    public int baseHeight;  
    public int scale;     

    public int targetFps;  
    public boolean vsync;  

    public String initialLevel; 
    public boolean debugMode;   
    
    public double masterVolume;

    public Config() {
        this.baseWidth = 1280;
        this.baseHeight = 720;
        this.scale = 1; 
        this.targetFps = 60;
        this.initialLevel = "Level";
        this.debugMode = false;
        this.masterVolume = 0.5;
    }

    public int getDisplayWidth() {
        return baseWidth * scale;
    }

    public int getDisplayHeight() {
        return baseHeight * scale;
    }
    
}