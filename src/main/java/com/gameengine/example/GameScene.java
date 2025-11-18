package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.Random;

/**
 * 游戏场景
 */
public class GameScene extends Scene {
    private final GameEngine engine;
    private Renderer renderer;
    private InputManager input;
    private Random random;
    private float time;
    private GameLogic gameLogic;
    private boolean gameEnded = false;
    
    public GameScene(GameEngine engine) {
        super("GameScene");
        this.engine = engine;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        this.random = new Random();
        this.time = 0;
        this.gameLogic = new GameLogic(this, renderer);
        
        // 创建游戏对象
        createPlayer();
        createEnemies();
        createDecorations();
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // ESC 返回菜单
        if (input.isKeyJustPressed(27)) {
            gameEnded = true;
        }
        
        // 游戏结束后按 R 返回主菜单
        if (gameEnded) {
            if (input.isKeyJustPressed(82)) { // R 键
                engine.setScene(new MenuScene(engine));
            }
            return;
        }
        
        time += deltaTime;
        
        // 使用游戏逻辑类处理游戏规则
        gameLogic.handlePlayerInput();
        gameLogic.updatePhysics(deltaTime);
        gameLogic.checkCollisions();
        
        // 生成新敌人
        if (time > 2.0f) {
            createEnemy();
            time = 0;
        }
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 渲染所有对象
        super.render();
        
        // 渲染UI
        gameLogic.renderUI();
        
        if (gameEnded) {
            // 游戏结束提示
            renderer.drawRect(200, 250, 400, 100, 0.0f, 0.0f, 0.0f, 0.8f);
            renderer.drawText("游戏结束！", 320, 280, 24, 1.0f, 1.0f, 0.3f, 1.0f);
            renderer.drawText("按 R 键返回主菜单", 280, 310, 16, 0.8f, 0.8f, 0.8f, 1.0f);
        } else {
            // 提示
            renderer.drawText("ESC 结束游戏", 10, 20, 14, 0.6f, 0.6f, 0.6f, 1.0f);
        }
    }
    
    private void createPlayer() {
        // 创建葫芦娃 - 所有部位都在一个GameObject中
        GameObject player = new GameObject("Player") {
            private Vector2 basePosition;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                
                // 更新所有部位的位置
                updateBodyParts();
            }
            
            @Override
            public void render() {
                // 渲染所有部位
                renderBodyParts();
            }
            
            private void updateBodyParts() {
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform != null) {
                    basePosition = transform.getPosition();
                }
            }
            
            private void renderBodyParts() {
                if (basePosition == null) return;
                
                // 渲染身体
                renderer.drawRect(
                    basePosition.x - 8, basePosition.y - 10, 16, 20,
                    1.0f, 0.0f, 0.0f, 1.0f  // 红色
                );
                
                // 渲染头部
                renderer.drawRect(
                    basePosition.x - 6, basePosition.y - 22, 12, 12,
                    1.0f, 0.5f, 0.0f, 1.0f  // 橙色
                );
                
                // 渲染左臂
                renderer.drawRect(
                    basePosition.x - 13, basePosition.y - 5, 6, 12,
                    1.0f, 0.8f, 0.0f, 1.0f  // 黄色
                );
                
                // 渲染右臂
                renderer.drawRect(
                    basePosition.x + 7, basePosition.y - 5, 6, 12,
                    0.0f, 1.0f, 0.0f, 1.0f  // 绿色
                );
            }
        };
        
        // 添加变换组件
        player.addComponent(new TransformComponent(new Vector2(400, 300)));
        
        // 添加物理组件
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);
        
        addGameObject(player);
    }
    
    private void createEnemies() {
        for (int i = 0; i < 3; i++) {
            createEnemy();
        }
    }
    
    private void createEnemy() {
        GameObject enemy = new GameObject("Enemy") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        // 随机位置
        Vector2 position = new Vector2(
            random.nextFloat() * 800,
            random.nextFloat() * 600
        );
        
        // 添加变换组件
        enemy.addComponent(new TransformComponent(position));
        
        // 添加渲染组件 - 改为矩形，使用橙色
        RenderComponent render = enemy.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(20, 20),
            new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)  // 橙色
        ));
        render.setRenderer(renderer);
        
        // 添加物理组件
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2(
            (random.nextFloat() - 0.5f) * 150,
            (random.nextFloat() - 0.5f) * 150
        ));
        physics.setFriction(0.98f);
        
        // 设置敌人血量
        enemy.setUserData("health", 10);
        
        addGameObject(enemy);
    }
    
    private void createDecorations() {
        for (int i = 0; i < 5; i++) {
            createDecoration();
        }
    }
    
    private void createDecoration() {
        GameObject decoration = new GameObject("Decoration") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        // 随机位置
        Vector2 position = new Vector2(
            random.nextFloat() * 800,
            random.nextFloat() * 600
        );
        
        // 添加变换组件
        decoration.addComponent(new TransformComponent(position));
        
        // 添加渲染组件
        RenderComponent render = decoration.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(5, 5),
            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);
        
        addGameObject(decoration);
    }
}
