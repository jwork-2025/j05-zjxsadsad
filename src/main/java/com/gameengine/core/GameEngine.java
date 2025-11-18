package com.gameengine.core;

import javax.swing.Timer;

import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

/**
 * 游戏引擎
 */
public class GameEngine {
    private Renderer renderer;
    private InputManager inputManager;
    private Scene currentScene;
    private boolean running;
    private float targetFPS;
    private float deltaTime;
    private long lastTime;
    private Timer gameTimer;
    private RecordingService recordingService;
    private boolean enableRecording;
    private final int width;
    private final int height;
    
    public GameEngine(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.renderer = new Renderer(width, height, title);
        this.inputManager = InputManager.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();
        this.enableRecording = false;
        this.recordingService = null;
    }
    
    /**
     * 启用录制功能
     */
    public void enableRecording(RecordingConfig config) {
        this.enableRecording = true;
        this.recordingService = new RecordingService(config);
    }
    
    /**
     * 启用录制功能（使用默认配置）
     */
    public void enableRecording() {
        enableRecording(new RecordingConfig());
    }
    
    /**
     * 运行游戏引擎
     */
    public void run() {
        running = true;
        
        // 初始化当前场景
        if (currentScene != null) {
            currentScene.initialize();
        }
        
        // 不在这里开始录制，而是在切换到GameScene时才开始
        
        // 创建游戏循环定时器
        gameTimer = new Timer((int) (1000 / targetFPS), e -> {
            if (running) {
                update();
                render();
            }
        });
        
        gameTimer.start();
    }
    
    /**
     * 更新游戏逻辑
     */
    private void update() {
        // 计算时间间隔
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f; // 转换为秒
        lastTime = currentTime;
        
        // 处理事件（收集输入）
        renderer.pollEvents();
        
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
        
        // 更新录制
        if (enableRecording && recordingService != null && currentScene != null) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        // 在帧末更新输入（清空 justPressed 状态）
        inputManager.update();
        
        // 检查窗口是否关闭
        if (renderer.shouldClose()) {
            running = false;
            gameTimer.stop();
            if (enableRecording && recordingService != null) {
                recordingService.stop();
            }
        }
    }
    
    /**
     * 渲染游戏
     */
    private void render() {
        renderer.beginFrame();
        
        // 渲染场景
        if (currentScene != null) {
            currentScene.render();
        }
        
        renderer.endFrame();
    }
    
    /**
     * 设置当前场景
     */
    public void setScene(Scene scene) {
        // 如果是从游戏场景切换，停止录制
        if (currentScene != null && currentScene.getName().equals("GameScene")) {
            if (enableRecording && recordingService != null && recordingService.isRecording()) {
                recordingService.stop();
                System.out.println("录制已停止");
            }
        }
        
        this.currentScene = scene;
        if (scene != null && running) {
            scene.initialize();
            
            // 如果切换到游戏场景，开始新的录制
            if (scene.getName().equals("GameScene") && enableRecording && recordingService != null) {
                try {
                    recordingService = new RecordingService(new RecordingConfig());
                    recordingService.start(scene, width, height);
                    System.out.println("开始新的录制");
                } catch (Exception e) {
                    System.err.println("启动录制失败: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 获取当前场景
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * 停止游戏引擎
     */
    public void stop() {
        running = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
    
    /**
     * 获取渲染器
     */
    public Renderer getRenderer() {
        return renderer;
    }
    
    /**
     * 获取输入管理器
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * 获取时间间隔
     */
    public float getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * 设置目标帧率
     */
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
        if (gameTimer != null) {
            gameTimer.setDelay((int) (1000 / fps));
        }
    }
    
    /**
     * 获取目标帧率
     */
    public float getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * 检查引擎是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
