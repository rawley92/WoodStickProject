package engine.boot;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AssetRegistry {
    
    private static final Map<String, String> paths = new HashMap<>();
    private static final Map<String, Integer> indexes = new HashMap<>();
    private static final Map<Integer, String> indexPaths = new HashMap<>();
    private static final Map<Integer, String> indexIds = new HashMap<>();

    public static void loadFromJson(String jsonFilePath) {
        try {
            paths.clear();
            indexes.clear();
            indexPaths.clear();
            indexIds.clear();

            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONObject root = new JSONObject(content);

            for (String category : root.keySet()) {
                parseCategory(root, category);
            }
            
            System.out.println("[ASSET REGISTRY] 총 " + paths.size() + "개 에셋 로드 완료.");
        } catch (IOException e) {
            System.err.println("[ASSET REGISTRY] JSON 파일을 찾을 수 없습니다.");
        }
    }

    public static void registerPath(String id, String path) {
        paths.put(id, path);
    }

    public static java.util.Set<String> getAllIds() {
        return paths.keySet();
    }

    private static void parseCategory(JSONObject root, String category) {
        JSONArray array = root.getJSONArray(category);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            String id = obj.getString("id");
            String path = obj.getString("path");
            String index = obj.optString("index", null);

            paths.put(id, path);

            if (index != null && !index.isEmpty()) {
                int parsedIndex = parseIndex(index);
                if (parsedIndex < 0) {
                    System.err.println("[ASSET REGISTRY] 잘못된 index 값입니다: " + index + " (" + id + ")");
                    continue;
                }

                indexes.put(id, parsedIndex);
                indexPaths.put(parsedIndex, path);
                indexIds.put(parsedIndex, id);
            }
        }
    }

    public static String getPath(String id) {
        int index = getIndex(id);

        if (index >= 0) {
            return getPath(index);
        }

        System.err.println("[DEBUG] AssetRegistry에 ID가 존재하지 않습니다: " + id);
        System.out.println("[DEBUG] 현재 레지스트리 목록: " + paths.keySet());
        return null;
    }

    public static String getPath(int index) {
        return indexPaths.get(index);
    }

    public static String getIdByIndex(String index) {
        return getIdByIndex(parseIndex(index));
    }

    public static String getIdByIndex(int index) {
        return indexIds.get(index);
    }

    public static String getPathByIndex(String index) {
        return getPath(parseIndex(index));
    }

    public static int getIndex(String idOrIndex) {
        if (idOrIndex == null || idOrIndex.isEmpty()) {
            return -1;
        }

        Integer index = indexes.get(idOrIndex);
        if (index != null) {
            return index;
        }

        int parsedIndex = parseIndex(idOrIndex);
        if (indexPaths.containsKey(parsedIndex)) {
            return parsedIndex;
        }

        if (paths.containsKey(idOrIndex)) {
            return indexes.getOrDefault(idOrIndex, -1);
        }

        return -1;
    }

    public static String[] getIdsByCategory(String prefix) {
        return paths.keySet().stream()
                .filter(id -> id.startsWith(prefix))
                .toArray(String[]::new);
    }

    private static int parseIndex(String rawIndex) {
        if (rawIndex == null || rawIndex.isEmpty()) {
            return -1;
        }

        try {
            int index = Integer.decode(rawIndex);
            if (index < 0 || index > 0xFF) {
                return -1;
            }
            return index;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
