package engine.boot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    private final String dataRootPath = "Data";
    private final String outputJsonPath = "Data/Data.json";

    private static class AssetInfo {
        String id;
        String path;

        AssetInfo(String id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    public DataLoader() {
    }

    public void scanAndGenerateJson() {
        System.out.println("[DATA] 디렉토리 스캔 및 데이터 기반 Data.json 빌드 시작...");

        File rootDir = new File(dataRootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("[DATA ERROR] 'Data/' 루트 폴더를 찾을 수 없습니다.");
            return;
        }

        List<AssetInfo> wallTextures = new ArrayList<>();
        List<AssetInfo> uiTextures = new ArrayList<>();
        List<AssetInfo> characters = new ArrayList<>();
        List<AssetInfo> levels = new ArrayList<>();

        File wallsDir = new File(rootDir, "textures/walls");
        if (wallsDir.exists() && wallsDir.isDirectory()) {
            File[] files = wallsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    String id = f.getName().substring(0, f.getName().lastIndexOf("."));
                    String path = "Data/textures/walls/" + f.getName();
                    wallTextures.add(new AssetInfo(id, path));
                }
            }
        }

        File uiDir = new File(rootDir, "UI");
        if (uiDir.exists() && uiDir.isDirectory()) {
            File[] files = uiDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    String id = f.getName().substring(0, f.getName().lastIndexOf("."));
                    String path = "Data/UI/" + f.getName();
                    uiTextures.add(new AssetInfo(id, path));
                    uiTextures.add(new AssetInfo(id + "1", path));
                }
            }
        }

        File charDir = new File(rootDir, "Char");
        if (charDir.exists() && charDir.isDirectory()) {
            File[] subDirs = charDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File d : subDirs) {
                    String id = d.getName();
                    String path = "Data/Char/" + id; 
                    characters.add(new AssetInfo(id, path));
                }
            }
        }
        File levelDir = new File(rootDir, "Level");
        if (levelDir.exists() && levelDir.isDirectory()) {
            File[] subDirs = levelDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File d : subDirs) {
                    String id = d.getName();
                    String path = "Data/Level/" + id + "/map.dat";
                    levels.add(new AssetInfo(id, path));
                }
            }
        }

        String jsonOutput = buildJsonString(wallTextures, uiTextures, characters, levels);

        try (FileWriter writer = new FileWriter(outputJsonPath)) {
            writer.write(jsonOutput);
            System.out.println("[DATA] Data.json 명세서 갱신 완료 -> " + outputJsonPath);
        } catch (IOException e) {
            System.err.println("[DATA ERROR] Data.json 명세서 파일 쓰기 실패!");
            e.printStackTrace();
        }
    }

    private String buildJsonString(List<AssetInfo> walls, List<AssetInfo> uis, List<AssetInfo> chars, List<AssetInfo> levels) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // 1. Wall Textures
        sb.append("  \"wall_textures\": [\n");
        appendAssetBlock(sb, walls);
        sb.append("  ],\n");

        // 2. UI Textures
        sb.append("  \"ui_textures\": [\n");
        appendAssetBlock(sb, uis);
        sb.append("  ],\n");

        // 3. Characters
        sb.append("  \"characters\": [\n");
        appendAssetBlock(sb, chars);
        sb.append("  ],\n");

        // 4. Levels
        sb.append("  \"levels\": [\n");
        appendAssetBlock(sb, levels);
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    private void appendAssetBlock(StringBuilder sb, List<AssetInfo> assets) {
        for (int i = 0; i < assets.size(); i++) {
            AssetInfo asset = assets.get(i);
            sb.append("    { \"id\": \"").append(asset.id).append("\", \"path\": \"").append(asset.path).append("\" }");
            if (i < assets.size() - 1) sb.append(",");
            sb.append("\n");
        }
    }
}