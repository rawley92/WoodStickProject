package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Control implements KeyListener {

    // 1. RAW INPUT (이건 계속 변함)
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    public boolean turnLeft;
    public boolean turnRight;

    // 2. SNAPSHOT (게임 로직이 보는 고정 값)
    public boolean s_up;
    public boolean s_down;
    public boolean s_left;
    public boolean s_right;

    public boolean s_turnLeft;
    public boolean s_turnRight;

    // ===== KEY EVENTS (RAW만 변경) =====
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W) up = true;
        if (key == KeyEvent.VK_S) down = true;
        if (key == KeyEvent.VK_A) turnLeft = true;
        if (key == KeyEvent.VK_D) turnRight = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_W) up = false;
        if (key == KeyEvent.VK_S) down = false;
        if (key == KeyEvent.VK_A) turnLeft = false;
        if (key == KeyEvent.VK_D) turnRight = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ===== SNAPSHOT 생성 (핵심) =====
    public void snapshot() {
        s_up = up;
        s_down = down;
        s_left = left;
        s_right = right;

        s_turnLeft = turnLeft;
        s_turnRight = turnRight;
    }
}