@echo off
chcp 65001 >nul
echo 编译游戏引擎...

REM 创建输出目录
if not exist build\classes mkdir build\classes

REM 编译所有Java文件
javac -encoding UTF-8 -d build/classes -cp . src/main/java/com/gameengine/math/Vector2.java src/main/java/com/gameengine/input/InputManager.java src/main/java/com/gameengine/core/Component.java src/main/java/com/gameengine/core/GameObject.java src/main/java/com/gameengine/components/TransformComponent.java src/main/java/com/gameengine/components/PhysicsComponent.java src/main/java/com/gameengine/components/RenderComponent.java src/main/java/com/gameengine/graphics/Renderer.java src/main/java/com/gameengine/recording/RecordingStorage.java src/main/java/com/gameengine/recording/FileRecordingStorage.java src/main/java/com/gameengine/recording/RecordingJson.java src/main/java/com/gameengine/recording/RecordingConfig.java src/main/java/com/gameengine/recording/RecordingService.java src/main/java/com/gameengine/core/GameEngine.java src/main/java/com/gameengine/core/GameLogic.java src/main/java/com/gameengine/scene/Scene.java src/main/java/com/gameengine/example/GameScene.java src/main/java/com/gameengine/example/MenuScene.java src/main/java/com/gameengine/example/ReplayScene.java src/main/java/com/gameengine/example/GameExample.java

if %errorlevel% equ 0 (
    echo 编译成功！
    echo 运行游戏...
    java -Dfile.encoding=UTF-8 -cp build/classes com.gameengine.example.GameExample
) else (
    echo 编译失败！
    pause
    exit /b 1
)

pause
