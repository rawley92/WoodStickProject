package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

/**
 * 키보드 입력을 엔진과 Lua 스크립트가 읽을 수 있는 상태값으로 보관한다.
 * 이벤트 기반 입력을 프레임 기반 스냅샷으로 바꾸는 역할을 한다.
 */
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

    /**
     * Lua 스크립트가 입력 상태를 읽을 수 있도록 현재 Control 객체를 userdata로 노출한다.
     * 변환 결과는 반복 생성 비용을 줄이기 위해 캐싱한다.
     */
    public LuaValue getLuaWrapper() {
        if (this.luaWrapper == null) {
            this.luaWrapper = CoerceJavaToLua.coerce(this);
        }
        return this.luaWrapper;
    }

    /**
     * AWT 키 입력 이벤트를 엔진의 실시간 입력 플래그로 변환한다.
     * 프레임에서 안정적으로 사용할 상태 복사는 snapshot()이 담당한다.
     */
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

    /**
     * AWT 키 해제 이벤트를 엔진의 실시간 입력 플래그에 반영한다.
     */
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

    /**
     * 문자 입력 콜백이다.
     * 현재 게임 입력은 keyPressed/keyReleased의 키 코드 기반 처리만 사용한다.
     */
    @Override
    public void keyTyped(KeyEvent e) {}

    /**
     * 비동기 키 이벤트 상태를 한 프레임 동안 고정된 s_* 상태로 복사한다.
     * Lua와 플레이어 컨트롤러는 이 스냅샷 값을 기준으로 입력을 판정한다.
     */
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
