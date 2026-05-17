package engine;

import engine.render.RenderCore;
import engine.render.Texture;
import engine.render.UiManager;
import engine.physics.PhysicsCore;
import engine.boot.Boot;
import engine.boot.DataLoader;

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
    private UiManager uiManager;
    private PlayerController playerController;
    private ScriptEngine scriptEngine;

    private String title = "Java Data-Driven Engine v0.3";

    public Core() {
        this.boot = new Boot();
        boot.loadConfig();

        DataLoader dataLoader = new DataLoader();
        dataLoader.scanAndGenerateJson();
        this.textureCore = new Texture();
        this.uiManager = new UiManager(); 
        
        boot.init(textureCore);

        boot.loadAssetsFromManifest(textureCore, this.uiManager);

        this.control = new Control();
        this.physicsCore = new PhysicsCore();
        this.playerController = new PlayerController();
        this.scriptEngine = new ScriptEngine(boot, textureCore, this.uiManager);

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
        Dimension size = new Dimension(boot.getConfig().baseWidth, boot.getConfig().baseHeight);
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

        renderCore.render(scene, this.uiManager);

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