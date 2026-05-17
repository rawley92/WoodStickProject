package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 현재 게임 월드 상태 저장
 */
public class Scene {

    private String name;

    private int[][] map;

    private List<Entity> entities;

    private Entity player;

    public Scene(String name, int[][] map) {

        this.name = name;
        this.map = map;

        this.entities = new ArrayList<>();
    }

    // =========================
    // Entity
    // =========================

    public void addEntity(Entity e) {

        entities.add(e);
    }

    public List<Entity> getEntities() {

        return entities;
    }

    // =========================
    // Player
    // =========================

    public void setPlayer(Entity player) {

        this.player = player;

        addEntity(player);
    }

    public Entity getPlayer() {

        return player;
    }

    // =========================
    // Map
    // =========================

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

    public int[][] getMap() {

        return map;
    }

    public int getWidth() {

        return map.length;
    }

    public int getHeight() {

        return map[0].length;
    }

    // =========================
    // Scene Info
    // =========================

    public String getName() {

        return name;
    }
}