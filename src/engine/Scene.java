package engine;

import java.util.ArrayList;
import java.util.List;

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

    public void addEntity(Entity e) {
        entities.add(e);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setPlayer(Entity player) {
        this.player = player;
        addEntity(player);
    }

    public Entity getPlayer() {
        return player;
    }

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

    public String getName() {
        return name;
    }
}