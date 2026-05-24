package engine.audio;

public class Listener {

    private final SoundEngine soundEngine;

    public Listener(SoundEngine soundEngine) {
        this.soundEngine = soundEngine;
    }

    // 예: 충돌 사운드
    public void onCollision(String sfxPath) {
        soundEngine.playSfx(sfxPath);
    }

    // 예: 씬 변경
    public void onSceneChange(String bgmPath) {
        soundEngine.playBgm(bgmPath, true);
    }
}