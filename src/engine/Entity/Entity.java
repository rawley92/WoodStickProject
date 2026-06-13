package engine.Entity;

import java.util.concurrent.atomic.AtomicInteger;
import engine.audio.SoundEngine;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

/**
 * 월드에 존재하는 플레이어, 적, 아이템, 프롭의 공통 데이터 모델이다.
 * 동작은 주로 Lua 스크립트가 정의하고, Java는 컴포넌트 상태를 보관한다.
 */
public class Entity {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    public final int entityId;

    public String name;
    public String assetId;
    public EntityType type;
    public String scriptPath;
    public String scriptName = "default";

    public PhysicsComponent physics;
    public CameraComponent camera;
    public RenderComponent render;
    public SoundComponent sound;
    public SoundEngine soundEngine;
    public UiComponent ui;

    public boolean isDynamic = false;
    public boolean isActive = false;
    public boolean isDestroyed = false;
    public boolean soundTriggered = false;

    private transient LuaValue luaWrapper = null;

    /**
     * 게임 월드에 배치될 기본 엔티티를 생성한다.
     * 세부 상태는 Physics/Render/Sound/UI 같은 컴포넌트 필드에 나뉘어 저장된다.
     */
    public Entity(String name, String assetId, double x, double y) {
        this.entityId = NEXT_ID.getAndIncrement();
        this.name = name;
        this.assetId = assetId;

        this.physics = new PhysicsComponent();
        this.physics.x = x;
        this.physics.y = y;

        this.render = new RenderComponent();
        this.render.assetId = assetId;

        this.type = EntityType.PROP;
        this.sound = new SoundComponent();
        this.ui = null;

        if (name.equalsIgnoreCase("Player")) {
            this.type = EntityType.PLAYER;
            this.camera = new CameraComponent(); 
        }
    }

    /**
     * Lua 스크립트에서 이 엔티티의 public 필드와 컴포넌트에 접근할 수 있도록 변환한다.
     * 같은 엔티티는 동일 wrapper를 재사용한다.
     */
    public LuaValue getLuaWrapper() {
        if (this.luaWrapper == null) {
            this.luaWrapper = CoerceJavaToLua.coerce(this);
        }
        return this.luaWrapper;
    }

    public enum EntityType {
        PLAYER, NPC, ITEM, PROP, PROJECTILE
    }

    /**
     * 물리 컴포넌트를 반환한다.
     */
    public PhysicsComponent getPhysics() {
        return physics;
    }

    /**
     * 디버그 출력용 엔티티 식별 문자열을 구성한다.
     */
    @Override
    public String toString() {
        return "[Entity #" + entityId + "] " + name + " (" + physics.x + ", " + physics.y + ")";
    }
}
