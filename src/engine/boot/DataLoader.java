package engine.boot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * [Engine/Boot/DataLoader]
 * Data 폴더 내부를 전체 스캔하여 자원 맵핑용 'Data.json'을 자동으로 갱신/출력합니다.
 */
public class DataLoader {

    private final String dataRootPath = "Data";
    private final String outputJsonPath = "Data/Data.json";

    public DataLoader() {
        // 이제 이 단계에서는 Texture를 직접 채우지 않고 JSON 색인 파일만 구워냅니다.
    }

    /**
     * Data/ 폴더 내 파일 구조를 확인하고 Data.json 파일을 강제로 새로 생성합니다.
     */
    public void scanAndGenerateJson() {
        System.out.println("[DATA] 디렉토리 스캔 및 Data.json 빌드 시작...");

        File rootDir = new File(dataRootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("[DATA ERROR] 'Data/' 루트 폴더를 찾을 수 없습니다.");
            return;
        }

        // 각 카테고리별 데이터를 담을 리스트
        List<String> wallTextures = new ArrayList<>();
        List<String> characters = new ArrayList<>();
        List<String> levels = new ArrayList<>();

        // 1. 벽 텍스처 스캔 (Data/textures/walls)
        File wallsDir = new File(rootDir, "textures/walls");
        if (wallsDir.exists() && wallsDir.isDirectory()) {
            File[] files = wallsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".png")) {
                        wallTextures.add(f.getName());
                    }
                }
            }
        }

        // 2. 캐릭터 스프라이트 폴더 스캔 (Data/Char)
        File charDir = new File(rootDir, "Char");
        if (charDir.exists() && charDir.isDirectory()) {
            File[] subDirs = charDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File d : subDirs) {
                    characters.add(d.getName()); // 폴더명 자체를 에셋 ID로 처리
                }
            }
        }

        // 3. 레벨/스테이지 폴더 스캔 (Data/Level)
        File levelDir = new File(rootDir, "Level");
        if (levelDir.exists() && levelDir.isDirectory()) {
            File[] subDirs = levelDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File d : subDirs) {
                    levels.add(d.getName());
                }
            }
        }

        // 4. 수집된 컬렉션을 기반으로 정적 JSON 문자열 조립
        String jsonOutput = buildJsonString(wallTextures, characters, levels);

        // 5. 파일 디스크에 물리 쓰기(Write)
        try (FileWriter writer = new FileWriter(outputJsonPath)) {
            writer.write(jsonOutput);
            System.out.println("[DATA] Data.json 파일이 성공적으로 갱신되었습니다 -> " + outputJsonPath);
        } catch (IOException e) {
            System.err.println("[DATA ERROR] Data.json 파일 쓰기 실패!");
            e.printStackTrace();
        }
    }

    /**
     * 외부 라이브러리 없이 순수 문자열 연산으로 깔끔한 포맷의 JSON을 빌드합니다.
     */
    private String buildJsonString(List<String> walls, List<String> characters, List<String> levels) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // 벽 텍스처 배열 생성
        sb.append("  \"wall_textures\": [\n");
        for (int i = 0; i < walls.size(); i++) {
            sb.append("    \"").append(walls.get(i)).append("\"");
            if (i < walls.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // 캐릭터 에셋 배열 생성
        sb.append("  \"characters\": [\n");
        for (int i = 0; i < characters.size(); i++) {
            sb.append("    \"").append(characters.get(i)).append("\"");
            if (i < characters.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // 레벨 배열 생성
        sb.append("  \"levels\": [\n");
        for (int i = 0; i < levels.size(); i++) {
            sb.append("    \"").append(levels.get(i)).append("\"");
            if (i < levels.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("\n  ]\n"); // 마지막 요소는 콤마 없음

        sb.append("}");
        return sb.toString();
    }
}