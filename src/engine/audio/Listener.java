package engine.audio;

/**
 * 게임 이벤트를 오디오 재생 요청으로 변환하는 보조 리스너다.
 */
public class Listener {

    private final SoundEngine soundEngine;

    /**
     * 이벤트성 사운드 호출을 SoundEngine으로 전달하는 리스너를 생성한다.
     */
    public Listener(SoundEngine soundEngine) {
        this.soundEngine = soundEngine;
    }

    /**
     * 충돌 이벤트에 대응하는 효과음을 재생한다.
     */
    public void onCollision(String sfxPath) {
        soundEngine.playSfx(sfxPath);
    }

    /**
     * 씬 변경 이벤트에 대응하는 BGM을 반복 재생한다.
     */
    public void onSceneChange(String bgmPath) {
        soundEngine.playBgm(bgmPath, true);
    }
}
