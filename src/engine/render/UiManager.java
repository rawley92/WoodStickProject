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

public class UiManager {

    private final Map<Integer, int[]> uiTextures = new HashMap<>();
    private final Map<Integer, BufferedImage> uiImages = new HashMap<>();
    private final Map<Integer, Integer> uiWidths = new HashMap<>();
    private final Map<Integer, Integer> uiHeights = new HashMap<>();
    private final List<DrawCommand> drawCommands = new ArrayList<>();

    public UiManager() {
    }

    public void registerUi(String assetId, String filePath) {
        try {
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

            uiTextures.put(assetIndex, pixels);
            uiImages.put(assetIndex, img);
            uiWidths.put(assetIndex, w);
            uiHeights.put(assetIndex, h);

            System.out.println("[DATA BIND] UI 에셋 등록 완료 -> ID: " + assetId + " (경로: " + filePath + ")");
        } catch (Exception e) {
            System.err.println("[UI ERROR] 에셋 바인딩 실패 -> ID: " + assetId + ", 원인: " + e.getMessage());
        }
    }

    public int[] getPixels(String id) {
        return uiTextures.get(AssetRegistry.getIndex(id));
    }

    public int getWidth(String id) {
        return uiWidths.getOrDefault(AssetRegistry.getIndex(id), 0);
    }

    public int getHeight(String id) {
        return uiHeights.getOrDefault(AssetRegistry.getIndex(id), 0);
    }

    public void clearDrawCommands() {
        drawCommands.clear();
    }

    public void drawRect(int x, int y, int width, int height, int color, double alpha) {
        drawCommands.add(DrawCommand.rect(x, y, width, height, color, alpha));
    }

    public void drawImage(String textureId, int x, int y, int width, int height, double alpha) {
        int textureIndex = AssetRegistry.getIndex(textureId);
        if (!uiImages.containsKey(textureIndex)) {
            System.err.println("[UI ERROR] 등록되지 않은 UI 이미지입니다: " + textureId);
            return;
        }

        drawCommands.add(DrawCommand.image(textureIndex, x, y, width, height, alpha));
    }

    public void drawRotatedImage(String textureId, int x, int y, int width, int height, double angleDegrees, double alpha) {
        int textureIndex = AssetRegistry.getIndex(textureId);
        if (!uiImages.containsKey(textureIndex)) {
            System.err.println("[UI ERROR] 등록되지 않은 UI 이미지입니다: " + textureId);
            return;
        }

        drawCommands.add(DrawCommand.rotatedImage(textureIndex, x, y, width, height, angleDegrees, alpha));
    }

    public void drawText(String text, int x, int y, int size, int color, double alpha) {
        drawCommands.add(DrawCommand.text(text, x, y, size, color, alpha, false));
    }

    public void drawTextCentered(String text, int x, int y, int size, int color, double alpha) {
        drawCommands.add(DrawCommand.text(text, x, y, size, color, alpha, true));
    }

    public void renderDrawCommands(BufferedImage frameBuffer) {
        if (drawCommands.isEmpty()) return;

        Graphics2D g = frameBuffer.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (DrawCommand command : drawCommands) {
            float alpha = (float)Math.max(0.0, Math.min(1.0, command.alpha));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(toColor(command.color));

            if (command.type == DrawCommand.TYPE_RECT) {
                g.fillRect(command.x, command.y, command.width, command.height);
            } else if (command.type == DrawCommand.TYPE_IMAGE) {
                BufferedImage img = uiImages.get(command.textureIndex);
                if (img != null) {
                    if (Math.abs(command.angleDegrees) > 0.001) {
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
                    FontMetrics metrics = g.getFontMetrics();
                    drawX = command.x - metrics.stringWidth(command.text) / 2;
                }

                g.drawString(command.text, drawX, command.y);
            }
        }

        g.dispose();
    }

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

        static DrawCommand rotatedImage(int textureIndex, int x, int y, int width, int height, double angleDegrees, double alpha) {
            DrawCommand command = image(textureIndex, x, y, width, height, alpha);
            command.angleDegrees = angleDegrees;
            return command;
        }

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
