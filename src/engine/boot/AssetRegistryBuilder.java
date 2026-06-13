package engine.boot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Data 폴더의 런타임 에셋을 스캔해 Data/Data.json 레지스트리를 생성한다.
 * 컨텐츠 제작자가 파일을 추가하면 실행 시점에 논리 ID와 index가 자동 부여된다.
 */
public class AssetRegistryBuilder {

    private final String dataRootPath = "Data";
    private final String outputJsonPath = "Data/Data.json";

    private static class AssetInfo {
        String index;
        String id;
        String path;

        AssetInfo(String index, String id, String path) {
            this.index = index;
            this.id = id;
            this.path = path;
        }
    }

    /**
     * 에셋 레지스트리 빌더를 생성한다.
     * 루트 경로와 출력 경로는 클래스 상수 필드로 고정되어 있다.
     */
    public AssetRegistryBuilder() {
    }

    /**
     * Data 폴더를 스캔해 런타임 에셋 레지스트리 JSON을 생성한다.
     * 세부 스캔, 런타임 에셋 판별, JSON 직렬화, 파일 저장은 하위 메서드로 분리한다.
     */
    public void buildRegistry() {
        System.out.println("[ASSET BUILD] Registry build started. Scanning all files in Data/");

        File rootDir = new File(dataRootPath);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("[ASSET WARNING] Data directory missing : " + dataRootPath);
            return;
        }
        Map<String, List<AssetInfo>> categorizedAssets = new LinkedHashMap<>();
        int counter = 0;

        try {
            // counter는 재귀 호출을 거치며 0x00부터 증가하는 에셋 index다.
            scanRecursively(rootDir, categorizedAssets, counter);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return;
        }

        String jsonOutput = buildJsonString(categorizedAssets);
        writeJson(jsonOutput);
    }

    /**
     * 디렉터리를 재귀 순회하며 런타임 에셋을 카테고리별 목록에 추가한다.
     * 파일 종류 판별은 isRuntimeAsset(), JSON 문자열 생성은 buildJsonString()이 담당한다.
     */
    private int scanRecursively(File dir, Map<String, List<AssetInfo>> map, int counter) {
        File[] files = dir.listFiles();
        if (files == null) return counter;

        for (File file : files) {
            if (file.isDirectory()) {
                counter = scanRecursively(file, map, counter);
            } else if (!file.getName().equals("Data.json") && isRuntimeAsset(file)) {
                // Data 기준 상대 경로를 사용해야 실행 위치가 바뀌어도 ID 생성 규칙이 유지된다.
                Path rootPath = Paths.get(dataRootPath);
                Path filePath = file.toPath();
                Path relativeToRoot = rootPath.relativize(filePath);

                if (relativeToRoot.getParent() == null) {
                    continue;
                }
                
                // 첫 경로 조각(Audio, Textures, Script 등)을 카테고리로 사용한다.
                String category = relativeToRoot.getName(0).toString(); 

                String fileName = file.getName();
                String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));

                // 폴더 구분자를 점으로 바꿔 "Textures.Level.Wall_1" 같은 논리 ID를 만든다.
                String pathStr = relativeToRoot.getParent().toString().replace(File.separator, ".");
                String id = pathStr + "." + nameWithoutExt;

                if (counter > 0xFF) {
                    throw new IllegalStateException("[ASSET BUILD] 8-bit index 범위를 초과했습니다. 런타임 에셋은 최대 256개까지 등록할 수 있습니다.");
                }

                String path = file.getPath().replace("\\", "/");
                // map.dat와 Lua API에서 쓰기 쉬운 8비트 16진 문자열로 index를 저장한다.
                String hexIndex = String.format("0x%02X", counter++);
                
                map.putIfAbsent(category, new ArrayList<>());
                map.get(category).add(new AssetInfo(hexIndex, id, path));
            }
        }
        return counter;
    }

    /**
     * 레지스트리에 포함할 런타임 에셋 파일인지 판정한다.
     */
    private boolean isRuntimeAsset(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png")
                || name.endsWith(".wav")
                || name.endsWith(".dat")
                || name.endsWith(".json")
                || name.endsWith(".lua");
    }

    /**
     * 카테고리별 에셋 목록을 Data/Data.json 포맷의 문자열로 직렬화한다.
     */
    private String buildJsonString(Map<String, List<AssetInfo>> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        Iterator<Map.Entry<String, List<AssetInfo>>> it = map.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<String, List<AssetInfo>> entry = it.next();
            sb.append("  \"").append(entry.getKey()).append("\": [\n");
            List<AssetInfo> list = entry.getValue();
            
            for (int i = 0; i < list.size(); i++) {
                AssetInfo a = list.get(i);
                sb.append("    { \"index\": \"").append(a.index).append("\", ")
                  .append("\"id\": \"").append(a.id).append("\", ")
                  .append("\"path\": \"").append(a.path).append("\" }");
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]");
            if (it.hasNext()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 생성된 레지스트리 JSON을 출력 경로에 저장한다.
     */
    private void writeJson(String json) {
        try (FileWriter writer = new FileWriter(outputJsonPath)) {
            writer.write(json);
            System.out.println("[ASSET BUILD] Registry generated: " + outputJsonPath);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
