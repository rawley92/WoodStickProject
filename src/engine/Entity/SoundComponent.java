package engine.Entity;

/**
 * 엔티티가 요청한 사운드 재생 상태를 보관하는 데이터 컴포넌트다.
 */
public class SoundComponent {

    public String currentSound;
    public boolean loop;
    public boolean requestPlay;
    public boolean playing;

    public double volume = 1.0;

    /**
     * 엔티티 단위 사운드 요청 상태를 초기화한다.
     * 실제 재생은 Core가 requestPlay를 감지한 뒤 SoundEngine에 위임한다.
     */
    public SoundComponent() {
        this.currentSound = null;
        this.loop = false;
        this.requestPlay = false;
    }

    /**
     * 이 엔티티에서 사운드를 재생해야 함을 표시한다.
     * 경로 해석과 Clip 재생은 SoundEngine이 담당한다.
     */
    public void play(String soundId, boolean loop) {
        this.currentSound = soundId;
        this.loop = loop;
        this.requestPlay = true;
    }

    /**
     * 현재 사운드 요청을 제거한다.
     */
    public void stop() {
        this.currentSound = null;
        this.requestPlay = false;
    }

    /**
     * 재생 요청 플래그만 정리한다.
     * currentSound는 유지되므로 마지막 요청 정보가 필요할 때 참조할 수 있다.
     */
    public void clear() {
        this.requestPlay = false;
    }
}
