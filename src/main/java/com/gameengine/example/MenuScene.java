package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

/**
 * 菜单场景
 * 提供开始游戏和回放选项
 */
public class MenuScene extends Scene {
    private final GameEngine engine;
    private Renderer renderer;
    private InputManager input;
    private int selectedOption = 0;
    private final String[] options = {"开始游戏", "回放录制", "退出"};

    public MenuScene(GameEngine engine) {
        super("Menu");
        this.engine = engine;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        this.selectedOption = 0;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // 上下键选择选项
        if (input.isKeyJustPressed(38)) { // UP
            selectedOption = (selectedOption - 1 + options.length) % options.length;
        }
        if (input.isKeyJustPressed(40)) { // DOWN
            selectedOption = (selectedOption + 1) % options.length;
        }
        
        // 回车键确认选择
        if (input.isKeyJustPressed(10)) { // ENTER
            handleSelection();
        }
        
        // ESC 也可以退出
        if (input.isKeyPressed(27)) { // ESC
            engine.stop();
        }
    }

    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.15f, 0.15f, 0.25f, 1.0f);
        
        // 绘制标题
        renderer.drawText("葫芦娃游戏", 300, 100, 32, 1.0f, 0.8f, 0.2f, 1.0f);
        
        // 绘制选项
        for (int i = 0; i < options.length; i++) {
            float y = 250 + i * 60;
            float r = (i == selectedOption) ? 1.0f : 0.6f;
            float g = (i == selectedOption) ? 1.0f : 0.6f;
            float b = (i == selectedOption) ? 0.3f : 0.6f;
            
            String text = (i == selectedOption ? "> " : "  ") + options[i];
            renderer.drawText(text, 300, y, 20, r, g, b, 1.0f);
        }
        
        // 绘制提示
        renderer.drawText("使用 ↑↓ 选择, Enter 确认", 250, 500, 14, 0.5f, 0.5f, 0.5f, 1.0f);
    }

    private void handleSelection() {
        switch (selectedOption) {
            case 0: // 开始游戏
                engine.setScene(new GameScene(engine));
                break;
            case 1: // 回放录制
                engine.setScene(new ReplayScene(engine, null));
                break;
            case 2: // 退出
                engine.stop();
                break;
        }
    }
}
