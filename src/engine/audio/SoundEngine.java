package engine.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java Sound API를 사용해 BGM과 효과음을 재생한다.
 * Lua와 엔티티는 사운드 ID만 요청하고, 실제 파일 재생은 이 클래스가 담당한다.
 */
public class SoundEngine {

    private Clip bgmClip;

    private final List<Clip> activeSfx = new ArrayList<>();

    /**
     * Java Sound 기반 오디오 서비스를 생성한다.
     * 실제 BGM/SFX 재생은 playBgm(), playSfx()가 파일 경로를 받아 처리한다.
     */
    public SoundEngine() {
        System.out.println("[SOUND] SoundEngine initialized");
    }

    /**
     * 지정된 WAV 파일을 BGM Clip으로 재생한다.
     * 기존 BGM 정리와 Clip 생성/루프 설정을 내부에서 처리한다.
     */
    public void playBgm(String path, boolean loop) {
        System.out.println("[SOUND DEBUG] stop current bgm");
        // BGM은 한 번에 하나만 유지하므로 새 BGM 시작 전 기존 Clip을 닫는다.
        stopBgm();
        try {
            File file = new File(path);

            System.out.println("[SOUND DEBUG] loading: " + file.getAbsolutePath());

            // Java Sound는 파일을 AudioInputStream으로 읽은 뒤 Clip에 open해야 재생할 수 있다.
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);

            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            System.out.println("[SOUND DEBUG] clip created = " + (bgmClip != null));

            if (loop) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            bgmClip.start();

            System.out.println("[SOUND] Playing BGM: " + path);

        } catch (UnsupportedAudioFileException e) {
            System.err.println("[SOUND ERROR] Unsupported audio format");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[SOUND ERROR] File not found or IO error");
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.err.println("[SOUND ERROR] Audio device unavailable");
            e.printStackTrace();
        }
    }

    /**
     * 현재 재생 중인 BGM Clip을 중지하고 리소스를 해제한다.
     */
    public void stopBgm() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    /**
     * 지정된 WAV 파일을 일회성 효과음으로 재생한다.
     * 종료된 Clip은 LineListener에서 자동으로 close하고 activeSfx 목록에서 제거한다.
     */
    public void playSfx(String path) {
        try {
            File file = new File(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);

            // SFX는 동시에 여러 개 재생될 수 있으므로 매 호출마다 별도 Clip을 만든다.
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            activeSfx.add(clip);

            // Clip 재생이 끝나면 close해서 native 오디오 리소스를 해제한다.
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    activeSfx.remove(clip);
                }
            });

            clip.start();

        } catch (Exception e) {
            System.err.println("[SOUND ERROR] SFX failed: " + path);
        }
    }

    /**
     * 현재 활성화된 모든 효과음을 중지하고 리소스를 해제한다.
     */
    public void stopAllSfx() {
        for (Clip c : activeSfx) {
            c.stop();
            c.close();
        }
        activeSfx.clear();
    }
}
