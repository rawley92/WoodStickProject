package engine.render;

import engine.boot.AssetRegistry;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lua가 요청한 2D UI 그리기 명령과 UI 이미지 캐시를 관리한다.
 * 3D 장면 렌더링 이후 Java2D를 사용해 최종 프레임버퍼 위에 합성한다.
 */
public class UiManager {

    private final Map<Integer, int[]> uiTextures = new HashMap<>();
    private final Map<Integer, BufferedImage> uiImages = new HashMap<>();
    private final Map<Integer, Integer> uiWidths = new HashMap<>();
    private final Map<Integer, Integer> uiHeights = new HashMap<>();
    private final List<DrawCommand> drawCommands = new ArrayList<>();

    /**
     * UI 텍스처 캐시와 draw command 목록을 가진 관리자를 생성한다.
     */
    public UiManager() {
    }

    /**
     * UI 에셋 이미지를 로드해 index 기반 캐시에 등록한다.
     * Lua의 uiImage 계열 API는 이 캐시를 통해 이미지를 참조한다.
     */
    public void registerUi(String assetId, String filePath) {
        try {
            // UI도 AssetRegistry index를 기준으로 저장해 Lua에서 문자열 ID만 넘기면 조회할 수 있게 한다.
            int assetIndex = AssetRegistry.getIndex(assetId);
            if (assetIndex < 0) {
                System.err.println("[UI ERROR] 잘못된 UI 에셋 ID입니다: " + assetId);
                return;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("[UI ERROR] 명세서에 기재된 파일이 존재하지 않습니다: " + filePath);
                return;
            }

            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);

            // 픽셀 배열은 Entity.ui 오버레이용, BufferedImage는 Graphics2D draw command용으로 사용된다.
            uiTextures.put(assetIndex, pixels);
            uiImages.put(assetIndex, img);
            uiWidths.put(assetIndex, w);
            uiHeights.put(assetIndex, h);

            System.out.println("[DATA BIND] UI 에셋 등록 완료 -> ID: " + assetId + " (경로: " + filePath + ")");
        } catch (Exception e) {
            System.err.println("[UI ERROR] 에셋 바인딩 실패 -> ID: " + assetId + ", 원인: " + e.getMessage());
        }
    }

    /**
     * UI 텍스처의 원본 픽셀 배열을 반환한다.
     */
    public int[] getPixels(String id) {
        return uiTextures.get(AssetRegistry.getIndex(id));
    }

    /**
     * UI 텍스처의 원본 폭을 반환한다.
     */
    public int getWidth(String id) {
        return uiWidths.getOrDefault(AssetRegistry.getIndex(id), 0);
    }

    /**
     * UI 텍스처의 원본 높이를 반환한다.
     */
    public int getHeight(String id) {
        return uiHeights.getOrDefault(AssetRegistry.getIndex(id), 0);
    }

    /**
     * 이번 프레임에 누적된 즉시 모드 UI 명령을 제거한다.
     */
    public void clearDrawCommands() {
        drawCommands.clear();
    }

    /**
     * 사각형 UI 명령을 큐에 추가한다.
     */
    public void drawRect(int x, int y, int width, int height, int color, double alpha) {
        drawCommands.add(DrawCommand.rect(x, y, width, height, color, alpha));
    }

    /**
     * 이미지 UI 명령을 큐에 추가한다.
     * 실제 Graphics2D 렌더링은 renderDrawCommands()에서 수행한다.
     */
    public void drawImage(String textureId, int x, int y, int width, int height, double alpha) {
        int textureIndex = AssetRegistry.getIndex(textureId);
        if (!uiImages.containsKey(textureIndex)) {
            System.err.println("[UI ERROR] 등록되지 않은 UI 이미지입니다: " + textureId);
            return;
        }

        // 즉시 그리지 않고 명령만 저장한다.
        // 실제 합성은 RenderCore가 3D 장면을 그린 뒤 renderDrawCommands()에서 수행한다.
        drawCommands.add(DrawCommand.image(textureIndex, x, y, width, height, alpha));
    }

    /**
     * 회전 이미지 UI 명령을 큐에 추가한다.
     */
    public void drawRotatedImage(String textureId, int x, int y, int width, int height, double angleDegrees, double alpha) {
        int textureIndex = AssetRegistry.getIndex(textureId);
        if (!uiImages.containsKey(textureIndex)) {
            System.err.println("[UI ERROR] 등록되지 않은 UI 이미지입니다: " + textureId);
            return;
        }

        drawCommands.add(DrawCommand.rotatedImage(textureIndex, x, y, width, height, angleDegrees, alpha));
    }

    /**
     * 좌표 기준 텍스트 UI 명령을 큐에 추가한다.
     */
    public void drawText(String text, int x, int y, int size, int color, double alpha) {
        drawCommands.add(DrawCommand.text(text, x, y, size, color, alpha, false));
    }

    /**
     * 중앙 정렬 텍스트 UI 명령을 큐에 추가한다.
     */
    public void drawTextCentered(String text, int x, int y, int size, int color, double alpha) {
        drawCommands.add(DrawCommand.text(text, x, y, size, color, alpha, true));
    }

    /**
     * 누적된 UI draw command를 프레임버퍼에 렌더링한다.
     * 명령의 종류별 세부 데이터는 DrawCommand 팩토리 메서드가 구성한다.
     */
    public void renderDrawCommands(BufferedImage frameBuffer) {
        if (drawCommands.isEmpty()) return;

        Graphics2D g = frameBuffer.createGraphics();

        // 텍스트와 회전 이미지가 거칠게 보이지 않도록 Java2D 렌더링 힌트를 켠다.
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (DrawCommand command : drawCommands) {
            // 명령별 alpha를 Graphics2D composite에 적용한다.
            float alpha = (float)Math.max(0.0, Math.min(1.0, command.alpha));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(toColor(command.color));

            if (command.type == DrawCommand.TYPE_RECT) {
                g.fillRect(command.x, command.y, command.width, command.height);
            } else if (command.type == DrawCommand.TYPE_IMAGE) {
                BufferedImage img = uiImages.get(command.textureIndex);
                if (img != null) {
                    if (Math.abs(command.angleDegrees) > 0.001) {
                        // 회전은 이미지 중심을 기준으로 적용하고, 이후 원래 transform을 복구한다.
                        AffineTransform oldTransform = g.getTransform();
                        g.rotate(
                                Math.toRadians(command.angleDegrees),
                                command.x + command.width / 2.0,
                                command.y + command.height / 2.0
                        );
                        g.drawImage(img, command.x, command.y, command.width, command.height, null);
                        g.setTransform(oldTransform);
                    } else {
                        g.drawImage(img, command.x, command.y, command.width, command.height, null);
                    }
                }
            } else if (command.type == DrawCommand.TYPE_TEXT) {
                g.setFont(new Font("SansSerif", Font.BOLD, command.size));

                int drawX = command.x;

                if (command.centered) {
                    // 중앙 정렬 텍스트는 실제 문자열 폭을 구해 시작 x좌표를 보정한다.
                    FontMetrics metrics = g.getFontMetrics();
                    drawX = command.x - metrics.stringWidth(command.text) / 2;
                }

                g.drawString(command.text, drawX, command.y);
            }
        }

        g.dispose();
    }

    /**
     * 0xRRGGBB 정수 색상을 AWT Color 객체로 변환한다.
     */
    private Color toColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new Color(r, g, b);
    }

    private static class DrawCommand {
        static final int TYPE_RECT = 1;
        static final int TYPE_TEXT = 2;
        static final int TYPE_IMAGE = 3;

        int type;
        int x;
        int y;
        int width;
        int height;
        int size;
        int color;
        double alpha;
        double angleDegrees;
        String text;
        int textureIndex;
        boolean centered;

        /**
         * 사각형 명령 데이터를 생성한다.
         */
        static DrawCommand rect(int x, int y, int width, int height, int color, double alpha) {
            DrawCommand command = new DrawCommand();
            command.type = TYPE_RECT;
            command.x = x;
            command.y = y;
            command.width = width;
            command.height = height;
            command.color = color;
            command.alpha = alpha;
            return command;
        }

        /**
         * 이미지 명령 데이터를 생성한다.
         */
        static DrawCommand image(int textureIndex, int x, int y, int width, int height, double alpha) {
            DrawCommand command = new DrawCommand();
            command.type = TYPE_IMAGE;
            command.textureIndex = textureIndex;
            command.x = x;
            command.y = y;
            command.width = width;
            command.height = height;
            command.alpha = alpha;
            command.angleDegrees = 0.0;
            return command;
        }

        /**
         * 이미지 명령에 회전 각도를 포함해 생성한다.
         */
        static DrawCommand rotatedImage(int textureIndex, int x, int y, int width, int height, double angleDegrees, double alpha) {
            DrawCommand command = image(textureIndex, x, y, width, height, alpha);
            command.angleDegrees = angleDegrees;
            return command;
        }

        /**
         * 텍스트 명령 데이터를 생성한다.
         */
        static DrawCommand text(String text, int x, int y, int size, int color, double alpha, boolean centered) {
            DrawCommand command = new DrawCommand();
            command.type = TYPE_TEXT;
            command.text = text;
            command.x = x;
            command.y = y;
            command.size = size;
            command.color = color;
            command.alpha = alpha;
            command.centered = centered;
            return command;
        }
    }
}
