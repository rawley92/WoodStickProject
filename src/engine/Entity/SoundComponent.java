package engine.Entity;

public class SoundComponent {

    public String currentSound;
    public boolean loop;
    public boolean requestPlay;
    public boolean playing;

    public double volume = 1.0;

    public SoundComponent() {
        this.currentSound = null;
        this.loop = false;
        this.requestPlay = false;
    }

    public void play(String soundId, boolean loop) {
        this.currentSound = soundId;
        this.loop = loop;
        this.requestPlay = true;
    }

    public void stop() {
        this.currentSound = null;
        this.requestPlay = false;

    }
        public void clear() {
        this.requestPlay = false;
        }
    
}