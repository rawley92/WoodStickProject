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

    public static void loadFromJson(String jsonFilePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONObject root = new JSONObject(content);

            // root의 모든 키(예: Level, Script, Textures)를 순회하며 파싱
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
            // ID를 "카테고리_이름" 형태로 저장하여 중복 방지 (선택 사항)
            // 혹은 그냥 obj.getString("id")를 사용하여 유니크하게 관리
            paths.put(obj.getString("id"), obj.getString("path"));
        }
    }

    public static String getPath(String id) {
        if (!paths.containsKey(id)) 
            try {
                System.err.println("[DEBUG] AssetRegistry에 ID가 존재하지 않습니다: " + id);
                System.out.println("[DEBUG] 현재 레지스트리 목록: " + paths.keySet());
            }catch (Exception e) {
        System.err.println("텍스처 로드 실패: " + id);
        e.printStackTrace(); 
        }
        return paths.get(id);
    }

    public static String[] getIdsByCategory(String prefix) {
        return paths.keySet().stream()
                .filter(id -> id.startsWith(prefix))
                .toArray(String[]::new);
}
}
