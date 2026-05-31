package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class Control implements KeyListener {

    public volatile boolean up;
    public volatile boolean down;
    public volatile boolean left;
    public volatile boolean right;

    public volatile boolean turnLeft;
    public volatile boolean turnRight;
    public volatile boolean menuUp;
    public volatile boolean menuDown;
    public volatile boolean enter;
    public volatile boolean space;
    public volatile boolean m_key;
    public volatile boolean o_key;
    public volatile boolean p_key;
    public volatile boolean u_key;

    public boolean s_up;
    public boolean s_down;
    public boolean s_left;
    public boolean s_right;
    public boolean s_turnLeft;
    public boolean s_turnRight;
    public boolean s_menuUp;
    public boolean s_menuDown;
    public boolean s_enter;
    public boolean s_space;
    public boolean s_m_key;
    public boolean s_o_key;
    public boolean s_p_key;
    public boolean s_u_key;

    private transient LuaValue luaWrapper = null;

    public LuaValue getLuaWrapper() {
        if (this.luaWrapper == null) {
            this.luaWrapper = CoerceJavaToLua.coerce(this);
        }
        return this.luaWrapper;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) up = true;
        if (key == KeyEvent.VK_S) down = true;
        if (key == KeyEvent.VK_A) left = true;
        if (key == KeyEvent.VK_D) right = true;
        if (key == KeyEvent.VK_LEFT) turnLeft = true;
        if (key == KeyEvent.VK_RIGHT) turnRight = true;
        if (key == KeyEvent.VK_UP) menuUp = true;
        if (key == KeyEvent.VK_DOWN) menuDown = true;
        if (key == KeyEvent.VK_P) p_key = true;
        if (key == KeyEvent.VK_ENTER) enter = true;
        if (key == KeyEvent.VK_SPACE) space = true;
        if (key == KeyEvent.VK_M) m_key = true;
        if (key == KeyEvent.VK_O) o_key = true;
        if (key == KeyEvent.VK_U) u_key = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) up = false;
        if (key == KeyEvent.VK_S) down = false;
        if (key == KeyEvent.VK_A) left = false;
        if (key == KeyEvent.VK_D) right = false;
        if (key == KeyEvent.VK_LEFT) turnLeft = false;
        if (key == KeyEvent.VK_RIGHT) turnRight = false;
        if (key == KeyEvent.VK_UP) menuUp = false;
        if (key == KeyEvent.VK_DOWN) menuDown = false;
        if (key == KeyEvent.VK_P) p_key = false;
        if (key == KeyEvent.VK_ENTER) enter = false;
        if (key == KeyEvent.VK_SPACE) space = false;
        if (key == KeyEvent.VK_M) m_key = false;
        if (key == KeyEvent.VK_O) o_key = false;
        if (key == KeyEvent.VK_U) u_key = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public void snapshot() {
        s_up = up;
        s_down = down;
        s_left = left;
        s_right = right;
        s_turnLeft = turnLeft;
        s_turnRight = turnRight;
        s_menuUp = menuUp;
        s_menuDown = menuDown;
        s_p_key = p_key;
        s_enter = enter;
        s_space = space;
        s_m_key = m_key;
        s_o_key = o_key;
        s_u_key = u_key;
    }
}
