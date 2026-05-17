package engine.boot;

import engine.render.Texture;
import engine.Entity;
import engine.Entity.EntityType;
import engine.Scene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * [Engine/Boot/SceneLoader]
 * 특정 레벨 폴더(Data/Level/레벨명) 내부의 map.dat와 scene.json을 결합하여
 * 인게임 런타임 씬(Scene) 객체를 생성합니다.
 */
public class SceneLoader {

    private final Texture textureManager;

    public SceneLoader(Texture textureManager) {
        this.textureManager = textureManager;
    }

    public Scene load(String levelName) {
        String levelDir = "Data/Level/" + levelName;
        File dir = new File(levelDir);
        
        if (!dir.exists()) {
            System.err.println("[SCENE ERROR] 레벨 폴더를 찾을 수 없습니다: " + levelDir);
            return null;
        }

        try {
            // 1. map.dat로부터 2차원 벽 데이터 구조 파싱
            int[][] map = loadMapArray(levelDir + "/map.dat");
            Scene scene = new Scene(levelName, map);

            // 2. scene.json 파일 로드 및 메타데이터 파싱
            String jsonPath = levelDir + "/scene.json";
            String jsonContent = "";
            if (new File(jsonPath).exists()) {
                jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
            }

            // 3. 플레이어 스폰 위치 추출 및 생성 (기본값 설정 후 JSON이 있으면 덮어씀)
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

            // 4. 엔티티 데이터 배치 (scene.json의 배치 명세를 기반으로 소환)
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

    /**
     * scene.json 명세서에 기록된 복수 개의 엔티티 정보를 파싱하여 씬에 동적 배치합니다.
     */
    private void loadLevelEntitiesFromJson(Scene scene, String json) {
        // "entities": [ ... ] 구조 안에서 개별 엔티티 블록 분리 매칭
        int entitiesIdx = json.indexOf("\"entities\"");
        if (entitiesIdx == -1) return;

        int startBracket = json.indexOf("[", entitiesIdx);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket == -1 || endBracket == -1) return;

        String entitiesArray = json.substring(startBracket + 1, endBracket);
        // 각 객체 단위 { } 로 분할
        String[] entityTokens = entitiesArray.split("\\}");

        for (String token : entityTokens) {
            if (!token.contains("{")) continue;
            
            String name = parseJsonString(token, "name", "npc_generic");
            String assetId = parseJsonString(token, "assetId", name);
            double x = parseJsonDouble(token, "x", 5.0);
            double y = parseJsonDouble(token, "y", 5.0);

            // 텍스처 로딩은 Boot에서 이미 끝났으므로, 여기서는 인스턴스 배치만 수행합니다!
            Entity npc = new Entity(name, assetId, x, y);
            npc.type = EntityType.NPC;
            npc.isDynamic = true;
            npc.isActive = true;

            scene.addEntity(npc);
            System.out.println("[SCENE] NPC 배치 완료 -> " + name + " (" + x + ", " + y + ")");
        }
    }

    // ==========================================
    // 유틸리티 데이터 파서 (외부 라이브러리 프리)
    // ==========================================

    private int[][] loadMapArray(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        int height = lines.size();
        int width = lines.get(0).trim().split("\\s+").length;

        int[][] map = new int[width][height];
        for (int y = 0; y < height; y++) {
            String[] tokens = lines.get(y).trim().split("\\s+");
            for (String token : tokens) {
                // ... 기존 파싱 로직과 동일
            }
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