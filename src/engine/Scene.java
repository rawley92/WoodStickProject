package engine;

import java.util.ArrayList;
import java.util.List;

import engine.Entity.Entity;

/**
 * 현재 플레이 중인 맵, 플레이어, 엔티티 목록을 묶는 런타임 씬 모델이다.
 * 씬 생성 데이터는 Lua와 map.dat에서 들어오고, Java는 조회와 보관을 담당한다.
 */
public class Scene {

    private String name;
    private int[][] map;
    private List<Entity> entities;
    private Entity player;

    /**
     * 이름, 타일맵, 엔티티 목록을 가진 런타임 씬을 생성한다.
     * 맵 데이터의 의미와 생성 방식은 ScriptAPI.initScene 또는 Lua 미로 생성기가 결정한다.
     */
    public Scene(String name, int[][] map) {
        this.name = name;
        this.map = map;

        this.entities = new ArrayList<>();
    }

    /**
     * 씬 업데이트와 렌더링 대상 목록에 엔티티를 추가한다.
     */
    public void addEntity(Entity e) {
        entities.add(e);
    }

    /**
     * 현재 씬에 등록된 엔티티 목록을 반환한다.
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * 씬의 플레이어 참조를 설정하고 일반 엔티티 목록에도 등록한다.
     */
    public void setPlayer(Entity player) {
        this.player = player;
        addEntity(player);
    }

    /**
     * 플레이어 엔티티를 반환한다.
     */
    public Entity getPlayer() {
        return player;
    }

    /**
     * 타일 좌표의 맵 값을 반환한다.
     * 맵 바깥은 충돌 가능한 벽으로 취급한다.
     */
    public int getTile(int x, int y) {
        if (x < 0 || y < 0) {
            return 1;
        }

        if (x >= getWidth()
        || y >= getHeight()) {
            return 1;
        }
        return map[x][y];
    }

    /**
     * entityId로 씬 내부 엔티티를 검색한다.
     * 현재 엔티티 수가 작다는 전제에서 선형 탐색을 사용한다.
     */
    public Entity getEntityById(int id) {
        for (Entity e : entities) {
            if (e.entityId == id) return e;
        }
        return null;
    }

    /**
     * 내부 타일맵 배열을 반환한다.
     */
    public int[][] getMap() {
        return map;
    }

    /**
     * 맵의 가로 타일 수를 반환한다.
     */
    public int getWidth() {
        return map.length;
    }

    /**
     * 맵의 세로 타일 수를 반환한다.
     */
    public int getHeight() {
        return map[0].length;
    }

    /**
     * 씬 이름을 반환한다.
     */
    public String getName() {
        return name;
    }
}
