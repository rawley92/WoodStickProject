package engine.boot;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Data/Data.json의 에셋 ID, 파일 경로, 8비트 인덱스를 런타임 조회 테이블로 제공한다.
 * Lua 컨텐츠와 Java 렌더/오디오 시스템이 공유하는 에셋 네임스페이스다.
 */
public class AssetRegistry {
    
    private static final Map<String, String> paths = new HashMap<>();
    private static final Map<String, Integer> indexes = new HashMap<>();
    private static final Map<Integer, String> indexPaths = new HashMap<>();
    private static final Map<Integer, String> indexIds = new HashMap<>();

    /**
     * Data/Data.json을 읽어 에셋 ID, 경로, 8비트 인덱스 조회 테이블을 구성한다.
     * 카테고리별 세부 파싱은 parseCategory()에 위임한다.
     */
    public static void loadFromJson(String jsonFilePath) {
        try {
            // 재로드 시 이전 실행의 에셋 정보가 남지 않도록 모든 조회 테이블을 비운다.
            paths.clear();
            indexes.clear();
            indexPaths.clear();
            indexIds.clear();

            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONObject root = new JSONObject(content);

            // Data.json의 최상위 키는 Audio, Char, Level, Script, Textures 같은 카테고리다.
            for (String category : root.keySet()) {
                parseCategory(root, category);
            }
            
            System.out.println("[ASSET REGISTRY] 총 " + paths.size() + "개 에셋 로드 완료.");
        } catch (IOException e) {
            System.err.println("[ASSET REGISTRY] JSON 파일을 찾을 수 없습니다.");
        }
    }

    /**
     * 런타임에서 직접 에셋 경로를 등록한다.
     * 자동 스캔 레지스트리 외의 임시 바인딩이 필요할 때 사용한다.
     */
    public static void registerPath(String id, String path) {
        paths.put(id, path);
    }

    /**
     * 등록된 전체 에셋 ID 집합을 반환한다.
     */
    public static java.util.Set<String> getAllIds() {
        return paths.keySet();
    }

    /**
     * JSON 루트의 한 카테고리 배열을 레지스트리 내부 맵으로 변환한다.
     * index 값이 존재하면 문자열 ID와 정수 인덱스 양방향 조회를 함께 구성한다.
     */
    private static void parseCategory(JSONObject root, String category) {
        JSONArray array = root.getJSONArray(category);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            String id = obj.getString("id");
            String path = obj.getString("path");
            String index = obj.optString("index", null);

            // 문자열 ID는 Lua와 Java 코드에서 사람이 읽는 논리 키로 사용된다.
            paths.put(id, path);

            if (index != null && !index.isEmpty()) {
                int parsedIndex = parseIndex(index);
                if (parsedIndex < 0) {
                    System.err.println("[ASSET REGISTRY] 잘못된 index 값입니다: " + index + " (" + id + ")");
                    continue;
                }

                // 정수 index는 map.dat 타일값, UI 캐시, 텍스처 캐시에서 빠른 조회 키로 사용된다.
                indexes.put(id, parsedIndex);
                indexPaths.put(parsedIndex, path);
                indexIds.put(parsedIndex, id);
            }
        }
    }

    /**
     * 에셋 ID 또는 index 문자열로 파일 경로를 조회한다.
     * 실제 index 문자열 판별은 getIndex()가 담당한다.
     */
    public static String getPath(String id) {
        int index = getIndex(id);

        if (index >= 0) {
            // ID와 index 문자열 모두 최종적으로 정수 index 경로 조회로 통합한다.
            return getPath(index);
        }

        System.err.println("[DEBUG] AssetRegistry에 ID가 존재하지 않습니다: " + id);
        System.out.println("[DEBUG] 현재 레지스트리 목록: " + paths.keySet());
        return null;
    }

    /**
     * 정수 index로 파일 경로를 조회한다.
     */
    public static String getPath(int index) {
        return indexPaths.get(index);
    }

    /**
     * 문자열 index를 에셋 ID로 변환한다.
     */
    public static String getIdByIndex(String index) {
        return getIdByIndex(parseIndex(index));
    }

    /**
     * 정수 index를 에셋 ID로 변환한다.
     */
    public static String getIdByIndex(int index) {
        return indexIds.get(index);
    }

    /**
     * 문자열 index로 파일 경로를 조회한다.
     */
    public static String getPathByIndex(String index) {
        return getPath(parseIndex(index));
    }

    /**
     * 에셋 ID 또는 문자열 index를 정수 index로 변환한다.
     * AssetRegistry는 이 메서드를 기준으로 ID 기반 호출과 index 기반 호출을 통합한다.
     */
    public static int getIndex(String idOrIndex) {
        if (idOrIndex == null || idOrIndex.isEmpty()) {
            return -1;
        }

        // 1차: 문자열 ID가 정확히 등록되어 있는지 확인한다.
        Integer index = indexes.get(idOrIndex);
        if (index != null) {
            return index;
        }

        // 2차: "0x2A" 또는 "42" 같은 index 문자열인지 확인한다.
        int parsedIndex = parseIndex(idOrIndex);
        if (indexPaths.containsKey(parsedIndex)) {
            return parsedIndex;
        }

        // index 없이 registerPath()만 된 ID는 경로는 존재하지만 index 기반 캐시는 사용할 수 없다.
        if (paths.containsKey(idOrIndex)) {
            return indexes.getOrDefault(idOrIndex, -1);
        }

        return -1;
    }

    /**
     * prefix로 시작하는 에셋 ID 목록을 반환한다.
     * 카테고리 단위 텍스처 로드에 사용된다.
     */
    public static String[] getIdsByCategory(String prefix) {
        return paths.keySet().stream()
                .filter(id -> id.startsWith(prefix))
                .toArray(String[]::new);
    }

    /**
     * 0xNN 또는 십진수 문자열을 0~255 범위의 에셋 index로 파싱한다.
     * 잘못된 값은 -1로 반환해 호출부가 실패를 감지하게 한다.
     */
    private static int parseIndex(String rawIndex) {
        if (rawIndex == null || rawIndex.isEmpty()) {
            return -1;
        }

        try {
            // Integer.decode는 "0x10", "#10", "16" 형식을 모두 처리한다.
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
