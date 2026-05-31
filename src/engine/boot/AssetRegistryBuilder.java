package engine.boot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

    public AssetRegistryBuilder() {
    }

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
            scanRecursively(rootDir, categorizedAssets, counter);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return;
        }

        String jsonOutput = buildJsonString(categorizedAssets);
        writeJson(jsonOutput);
    }

    private int scanRecursively(File dir, Map<String, List<AssetInfo>> map, int counter) {
        File[] files = dir.listFiles();
        if (files == null) return counter;

        for (File file : files) {
            if (file.isDirectory()) {
                counter = scanRecursively(file, map, counter);
            } else if (!file.getName().equals("Data.json") && isRuntimeAsset(file)) {
                Path rootPath = Paths.get(dataRootPath);
                Path filePath = file.toPath();
                Path relativeToRoot = rootPath.relativize(filePath);

                if (relativeToRoot.getParent() == null) {
                    continue;
                }
                
                String category = relativeToRoot.getName(0).toString(); 

                String fileName = file.getName();
                String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));

                String pathStr = relativeToRoot.getParent().toString().replace(File.separator, ".");
                String id = pathStr + "." + nameWithoutExt;

                if (counter > 0xFF) {
                    throw new IllegalStateException("[ASSET BUILD] 8-bit index 범위를 초과했습니다. 런타임 에셋은 최대 256개까지 등록할 수 있습니다.");
                }

                String path = file.getPath().replace("\\", "/");
                String hexIndex = String.format("0x%02X", counter++);
                
                map.putIfAbsent(category, new ArrayList<>());
                map.get(category).add(new AssetInfo(hexIndex, id, path));
            }
        }
        return counter;
    }

    private boolean isRuntimeAsset(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png")
                || name.endsWith(".wav")
                || name.endsWith(".dat")
                || name.endsWith(".json")
                || name.endsWith(".lua");
    }

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

    private void writeJson(String json) {
        try (FileWriter writer = new FileWriter(outputJsonPath)) {
            writer.write(json);
            System.out.println("[ASSET BUILD] Registry generated: " + outputJsonPath);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
