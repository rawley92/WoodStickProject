package engine;

import engine.render.RenderCore;
import engine.render.Texture;
import engine.physics.PhysicsCore;
import engine.boot.Boot;
import engine.boot.DataLoader;
import engine.PlayerController;

import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.image.BufferStrategy;

public class Core extends Canvas implements Runnable {

    private boolean running = false;
    private Thread thread;
    private JFrame frame;

    private Boot boot;
    private Control control;
    private RenderCore renderCore;
    private PhysicsCore physicsCore;
    private Texture textureCore;
    private PlayerController playerController;
    private ScriptEngine scriptEngine;

    private double targetFps;
    private String title = "Java Data-Driven Engine v0.1";

    public Core() {
        // 1. 빈 부트 환경을 먼저 올리고 설정을 로드합니다.
        this.boot = new Boot();
        boot.loadConfig();

        // 2. 물리 폴더를 스캔하여 Data.json을 실시간으로 발행/갱신합니다.
        DataLoader dataLoader = new DataLoader();
        dataLoader.scanAndGenerateJson();

        // 3. 엔진 가상 캔버스 텍스처 코어를 생성하고 부트에 인젝션합니다.
        this.textureCore = new Texture();
        boot.init(textureCore);

        // 4. 별도의 매니저 클래스 없이, Boot가 방금 구워진 Data.json을 긁어 textureCore 메모리를 채웁니다.
        boot.loadAssetsFromManifest(textureCore);

        // 5. 스크립트 가상머신 구동 및 연산 모듈 조립
        this.scriptEngine = new ScriptEngine(boot, textureCore);
        this.control = new Control();
        this.physicsCore = new PhysicsCore();
        this.playerController = new PlayerController();

        this.renderCore = new RenderCore(
                boot.getConfig().baseWidth,
                boot.getConfig().baseHeight,
                boot.getConfig().scale,
                textureCore
        );

        initWindow();
        this.addKeyListener(control);
        this.setFocusable(true);
    }

    private void initWindow() {
        frame = new JFrame(title);
        Dimension size = new Dimension(1280, 720);
        this.setPreferredSize(size);
        this.setMinimumSize(size);
        this.setMaximumSize(size);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    public synchronized void start() {
        if (running) return;
        running = true;

        System.out.println("[CORE] Bootstrapping Script Engine...");
        scriptEngine.runScript("main"); 

        thread = new Thread(this, "engine_Main_Loop");
        thread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double nsPerUpdate = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerUpdate;
            lastTime = now;

            try {
                while (delta >= 1) {
                    update(1.0 / 60.0);
                    delta--;
                }
                render();
                Thread.sleep(1);
            } catch (Exception e) {
                halt("Runtime Panic: " + e.getMessage());
                e.printStackTrace();
            }
        }
        stop();
    }

    private void update(double deltaTime) {
        control.snapshot();

        Scene scene = boot.getCurrentScene();
        if (scene == null) return; 

        playerController.update(scene.getPlayer(), control, deltaTime);

        if (scene.getEntities() != null) {
            for (Entity entity : scene.getEntities()) {
                if (entity.type == Entity.EntityType.PLAYER) continue;
                scriptEngine.updateEntity(entity, deltaTime, scene.getPlayer());
            }
        }

        physicsCore.update(scene, deltaTime);
    }

    private void render() {
        Scene scene = boot.getCurrentScene();
        if (scene == null) return;

        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        renderCore.render(scene);

        Graphics g = bs.getDrawGraphics();
        g.drawImage(renderCore.getFrameBuffer(), 0, 0, getWidth(), getHeight(), null);
        g.dispose();
        bs.show();
    }

    public static void halt(String message) {
        System.err.println("\n[ENGINE FATAL ERROR] System halted: " + message);
        System.exit(1);
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        new Core().start();
    }
}