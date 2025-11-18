package com.gameengine.example;

import com.gameengine.core.GameEngine;

/**
 * 游戏主入口
 * 负责创建游戏引擎并启动主菜单
 */
public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");
        
        try {
            // 创建游戏引擎
            GameEngine engine = new GameEngine(800, 600, "葫芦娃游戏");
            
            // 启用录制功能（仅在开始游戏时才会真正开始录制）
            engine.enableRecording();
            
            // 创建主菜单场景并启动
            engine.setScene(new MenuScene(engine));
            
            // 运行游戏
            engine.run();
            
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("游戏结束");
    }
}
