package engine.boot;

import engine.Entity;
import engine.Entity.EntityType;
import engine.Scene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SceneLoader {

    public Scene load(String levelName) {
        String levelDir = "Data/Level/" + levelName;
        File dir = new File(levelDir);
        
        if (!dir.exists()) {
            System.err.println("[SCENE ERROR] 레벨 폴더를 찾을 수 없습니다: " + levelDir);
            return null;
        }

        try {
            int[][] map = loadMapArray(levelDir + "/map.dat");
            Scene scene = new Scene(levelName, map);

            String jsonPath = levelDir + "/scene.json";
            String jsonContent = "";
            if (new File(jsonPath).exists()) {
                jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
            }

            double playerX = 3.5;
            double playerY = 3.5;
            if (!jsonContent.isEmpty()) {
                playerX = parseJsonDouble(jsonContent, "spawnX", 3.5);
                playerY = parseJsonDouble(jsonContent, "spawnY", 3.5);
            }
            
            Entity player = new Entity("player", "player", playerX, playerY);
            player.type = EntityType.PLAYER;
            player.isDynamic = true;
            player.isActive = true;
            scene.setPlayer(player);

            if (!jsonContent.isEmpty()) {
                loadLevelEntitiesFromJson(scene, jsonContent);
            }

            System.out.println("[SCENE] 레벨 로드 완료: " + levelName + " (" + map.length + "x" + map[0].length + ")");
            return scene;

        } catch (Exception e) {
            System.err.println("[SCENE ERROR] '" + levelName + "' 로드 중 치명적 실패");
            e.printStackTrace();
            return null;
        }
    }

    private void loadLevelEntitiesFromJson(Scene scene, String json) {
        int entitiesIdx = json.indexOf("\"entities\"");
        if (entitiesIdx == -1) return;

        int startBracket = json.indexOf("[", entitiesIdx);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket == -1 || endBracket == -1) return;

        String entitiesArray = json.substring(startBracket + 1, endBracket);
        String[] entityTokens = entitiesArray.split("\\}");

        for (String token : entityTokens) {
            if (!token.contains("{")) continue;
            
            String name = parseJsonString(token, "name", "npc_generic");
            String assetId = parseJsonString(token, "assetId", name);
            double x = parseJsonDouble(token, "x", 5.0);
            double y = parseJsonDouble(token, "y", 5.0);

            Entity npc = new Entity(name, assetId, x, y);
            npc.type = EntityType.NPC;
            npc.isDynamic = true;
            npc.isActive = true;

            scene.addEntity(npc);
            System.out.println("[SCENE] NPC 배치 완료 -> " + name + " (" + x + ", " + y + ")");
        }
    }

    private int[][] loadMapArray(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        int height = lines.size();
        int width = lines.get(0).trim().split("\\s+").length;

        int[][] map = new int[width][height];
        for (int y = 0; y < height; y++) {
            String[] tokens = lines.get(y).trim().split("\\s+");
            for (int x = 0; x < width; x++) {
                map[x][y] = Integer.parseInt(tokens[x]);
            }
        }
        return map;
    }

    private String parseJsonString(String json, String key, String defaultVal) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return defaultVal;
        int colonIdx = json.indexOf(":", keyIdx);
        int startQuote = json.indexOf("\"", colonIdx);
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (startQuote == -1 || endQuote == -1) return defaultVal;
        return json.substring(startQuote + 1, endQuote);
    }

    private double parseJsonDouble(String json, String key, double defaultVal) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return defaultVal;
        int colonIdx = json.indexOf(":", keyIdx);
        int commaIdx = json.indexOf(",", colonIdx);
        if (commaIdx == -1) commaIdx = json.indexOf("\n", colonIdx);
        if (commaIdx == -1) commaIdx = json.length();
        
        try {
            String valStr = json.substring(colonIdx + 1, commaIdx).replaceAll("[}\\]\\s\"]", "");
            return Double.parseDouble(valStr);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}