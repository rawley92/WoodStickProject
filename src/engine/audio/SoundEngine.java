package engine.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundEngine {

    private Clip bgmClip;

    private final List<Clip> activeSfx = new ArrayList<>();

    public SoundEngine() {
        System.out.println("[SOUND] SoundEngine initialized");
    }

    // BGM 재생
    public void playBgm(String path, boolean loop) {
        System.out.println("[SOUND DEBUG] stop current bgm");
        stopBgm();
        try {
            File file = new File(path);

            System.out.println("[SOUND DEBUG] loading: " + file.getAbsolutePath());

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

    // BGM 정지
    public void stopBgm() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    // 효과음 (간단 버전)
    public void playSfx(String path) {
        try {
            File file = new File(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            activeSfx.add(clip);

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
    public void stopAllSfx() {
        for (Clip c : activeSfx) {
            c.stop();
            c.close();
        }
        activeSfx.clear();
    }
}