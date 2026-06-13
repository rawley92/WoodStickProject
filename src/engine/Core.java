package engine;

import engine.render.RenderCore;
import engine.render.Texture;
import engine.render.UiManager;
import engine.render.physics.PhysicsCore;
import engine.Entity.Entity;
import engine.Entity.PlayerController;
import engine.boot.Boot;
import engine.boot.AssetRegistry;
import engine.boot.AssetRegistryBuilder;
import engine.script.ScriptManager; 
import engine.audio.SoundEngine;

import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

/**
 * Java 런타임의 최상위 조립자다.
 * 이 클래스는 게임 규칙을 직접 소유하기보다 엔진 서비스와 Lua 컨텐츠 레이어를 연결한다.
 */
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
    private ScriptManager scriptManager; 
    private SoundEngine soundEngine;
    private String title = "Java Engine v1.0";

    /**
     * 엔진 실행에 필요한 런타임 서비스를 조립한다.
     * 세부 초기화 순서는 레지스트리 빌드, 에셋 로드, Lua 부트 스크립트 실행,
     * 입력/물리/렌더 서비스 생성, 윈도우 초기화 메서드로 분리되어 있다.
     */
    public Core() {
        this.boot = new Boot();
        boot.loadConfig();

        // Data 폴더를 실행 시점마다 다시 스캔해 최신 에셋 목록을 만든다.
        // 이후 AssetRegistry는 생성된 Data/Data.json을 기준으로 ID와 경로를 조회한다.
        AssetRegistryBuilder registryBuilder = new AssetRegistryBuilder();
        registryBuilder.buildRegistry();

        AssetRegistry.loadFromJson("Data/Data.json");

        // 렌더/UI/오디오는 Lua API에서도 참조되므로 ScriptManager 생성 전에 준비한다.
        this.textureCore = new Texture();
        this.uiManager = new UiManager(); 
        this.soundEngine = new SoundEngine();

        // UI 이미지는 Texture의 flat 저장소와 별개로 UiManager에 BufferedImage 형태로 캐싱한다.
        // Lua의 engine.uiImage 계열 호출은 이 캐시를 사용한다.
        for (String id : AssetRegistry.getAllIds()) {
            if (id.contains("Textures.UI.")) {
                uiManager.registerUi(id, AssetRegistry.getPath(id));
            }
        }

        boot.init(textureCore);

        // ScriptAPI는 boot/texture/ui/sound 서비스를 Lua 전역 engine 테이블로 노출한다.
        this.scriptManager = new ScriptManager(boot, textureCore, uiManager, soundEngine);

        // Script.main은 Lua 전역 상태를 만들고 최초 씬(title)을 로드하는 컨텐츠 진입점이다.
        this.scriptManager.runScript("Script.main");

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

    /**
     * 렌더링 대상 Canvas를 포함하는 고정 크기 Swing 윈도우를 구성한다.
     * 실제 해상도 값은 Boot가 보유한 Config에서 가져온다.
     */
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

    /**
     * 엔진 메인 루프 스레드를 시작한다.
     * 루프의 실제 시간 누적과 프레임 처리는 run()에서 수행한다.
     */
    public synchronized void start() {
        if (running) return;
        running = true;
        System.out.println("[CORE] Engine Bootstrap Success. Starting Loop...");
        thread = new Thread(this, "engine_Main_Loop");
        thread.start();
    }

    /**
     * 고정 업데이트 간격을 유지하는 메인 루프를 실행한다.
     * 프레임별 세부 작업은 update()와 render()에 위임한다.
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double nsPerUpdate = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();

            // 실제 경과 시간을 60Hz 논리 업데이트 횟수로 환산한다.
            // 렌더링이 늦어져도 update()는 필요한 만큼 따라잡는다.
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

    /**
     * 한 프레임의 게임 상태를 갱신한다.
     * 입력 스냅샷, 플레이어 제어, Lua 엔티티 스크립트, 사운드 요청,
     * 물리 갱신은 각각 전용 객체에 위임한다.
     */
    private void update(double deltaTime) {
        // AWT 이벤트 스레드에서 변경된 입력 플래그를 게임 루프 기준 값으로 고정한다.
        control.snapshot();

        Scene scene = boot.getCurrentScene();
        if (scene == null) return; 

        // 플레이어 이동은 Java에서 처리한다.
        // 적, 아이템, UI, 전투 같은 컨텐츠 로직은 아래 Lua 엔티티 update에서 처리된다.
        playerController.update(scene.getPlayer(), control, deltaTime);

        if (scene.getEntities() != null) {
            for (Entity entity : new ArrayList<>(scene.getEntities())) {
                if (entity.type == Entity.EntityType.PLAYER) continue;

                // 엔티티별 scriptPath에 연결된 Lua update(entity, dt, player, control)를 호출한다.
                scriptManager.updateEntity(entity, deltaTime, scene.getPlayer(), control);

                 if (entity.sound != null && entity.sound.requestPlay) {

                    String soundId = entity.sound.currentSound;
                    String path = AssetRegistry.getPath(soundId);

                    // Lua는 asset ID만 요청하고, Java가 실제 파일 경로로 해석해 재생한다.
                    if (path != null) {
                        soundEngine.playSfx(path);
                    }

                    entity.sound.clear(); 
                }
            }
        }
        // Lua에서 변경한 속도/활성 상태를 바탕으로 최종 위치와 벽 충돌을 처리한다.
        physicsCore.update(scene, deltaTime);
    }

    /**
     * 현재 씬을 프레임버퍼에 렌더링하고 Canvas BufferStrategy로 화면에 출력한다.
     * 씬 내부의 실제 레이캐스팅/스프라이트/UI 합성은 RenderCore가 담당한다.
     */
    private void render() {
        Scene scene = boot.getCurrentScene();
        if (scene == null) {
            System.out.println("[DEBUG] Scene is null! InitScene failed?");
            return;
        }

        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            // Canvas는 표시된 뒤에 BufferStrategy를 만들 수 있으므로 첫 프레임에서 초기화한다.
            createBufferStrategy(3);
            return;
        }

        // RenderCore는 내부 BufferedImage에 장면을 그리고, Core는 그 결과만 화면에 복사한다.
        renderCore.render(scene, this.uiManager);

        Graphics g = bs.getDrawGraphics();
        g.drawImage(renderCore.getFrameBuffer(), 0, 0, getWidth(), getHeight(), null);
        g.dispose();
        bs.show();
    }

    /**
     * 복구할 수 없는 엔진 오류를 보고하고 프로세스를 종료한다.
     */
    public static void halt(String message) {
        System.err.println("\n[ENGINE FATAL ERROR] System halted: " + message);
        System.exit(1);
    }

    /**
     * 메인 루프 종료를 요청하고 루프 스레드가 끝날 때까지 대기한다.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    /**
     * 데스크톱 애플리케이션 진입점이다.
     */
    public static void main(String[] args) {
        new Core().start();
    }
}
