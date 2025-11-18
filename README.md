# j03 - 葫芦娃游戏引擎

一个基于Java Swing的2D游戏引擎，实现了葫芦娃与妖精的对战游戏，支持游戏录制与回放功能。

## 功能特性

- **主菜单系统**: 提供开始游戏、回放录制、退出游戏三个选项
- **游戏录制**: 自动录制游戏过程，保存为JSONL格式
- **游戏回放**: 可回放已录制的游戏，完整重现游戏过程
- **核心引擎**: 游戏循环、场景管理、对象生命周期管理
- **渲染系统**: 基于Swing的2D渲染，支持矩形、圆形、线条、文字绘制
- **输入处理**: 键盘输入管理
- **物理系统**: 速度、摩擦力、边界检测
- **碰撞检测**: 玩家与敌人、子弹与敌人的碰撞检测
- **战斗系统**: 玩家血量、敌人血量、子弹、炸弹系统
- **组件系统**: 基于ECS架构的组件-实体系统

## 快速开始

### 环境要求

- **Java 11** 或更高版本

### 运行游戏

#### Windows系统
```batch
run.bat
```

#### Linux/Mac系统
```bash
chmod +x run.sh
./run.sh
```

### 游戏说明

#### 主菜单操作
- **↑/↓ 方向键**: 选择菜单选项
- **Enter**: 确认选择
- **ESC**: 退出游戏

#### 游戏操作
- **WASD** 或 **方向键**: 移动葫芦娃
- **H键** 或 **空格**: 发射子弹
- **J键**: 发射炸弹（CD: 20秒）
- **ESC**: 结束游戏返回主菜单
- **R键**: 游戏结束后返回主菜单

#### 回放操作
- **↑/↓ 方向键**: 选择录制文件
- **Enter**: 开始回放
- **ESC**: 结束回放返回主菜单
- **R键**: 回放结束后返回主菜单

### 游戏机制

- **玩家血量**: 初始20点，被敌人碰撞减少1点
- **敌人血量**: 初始10点，被子弹击中减少1点，被炸弹炸到减少8点
- **子弹特性**: 可反弹墙壁，最大飞行距离为5个屏幕宽度
- **炸弹特性**: 爆炸范围200像素，冷却时间20秒
- **敌人生成**: 每2秒自动生成一个新敌人
- **自动录制**: 开始游戏时自动录制，退出游戏时自动保存

## 项目结构

```
j03-zjxsadsad/
├── src/main/java/com/gameengine/
│   ├── core/              # 核心引擎
│   │   ├── GameEngine.java       # 游戏引擎主类
│   │   ├── GameObject.java       # 游戏对象基类
│   │   ├── Component.java        # 组件基类
│   │   └── GameLogic.java        # 游戏逻辑处理
│   ├── components/        # 组件系统
│   │   ├── TransformComponent.java  # 位置变换组件
│   │   ├── PhysicsComponent.java    # 物理组件
│   │   └── RenderComponent.java     # 渲染组件
│   ├── graphics/          # 渲染系统
│   │   └── Renderer.java            # 渲染器
│   ├── input/             # 输入系统
│   │   └── InputManager.java        # 输入管理器
│   ├── math/              # 数学工具
│   │   └── Vector2.java             # 2D向量
│   ├── scene/             # 场景管理
│   │   └── Scene.java               # 场景基类
│   ├── recording/         # 录制系统
│   │   ├── RecordingService.java    # 录制服务
│   │   ├── RecordingStorage.java    # 存储接口
│   │   ├── FileRecordingStorage.java # 文件存储实现
│   │   ├── RecordingConfig.java     # 录制配置
│   │   └── RecordingJson.java       # JSON解析工具
│   └── example/           # 游戏实现
│       ├── GameExample.java         # 主入口
│       ├── MenuScene.java           # 主菜单场景
│       ├── GameScene.java           # 游戏场景
│       └── ReplayScene.java         # 回放场景
├── recordings/            # 游戏录制文件目录
├── build/                 # 编译输出目录
├── run.bat               # Windows启动脚本
├── run.sh                # Linux/Mac启动脚本
└── README.md             # 项目说明文档
```

## 录制文件说明

游戏录制文件保存在 `recordings/` 目录下，采用JSONL格式（每行一个JSON对象）：

- **文件命名**: `game_YYYYMMDD_HHMMSS.jsonl`
- **文件格式**: 
  - `header`: 录制文件头，包含版本和分辨率信息
  - `keyframe`: 关键帧，记录所有游戏对象的位置和状态
  - `input`: 输入事件，记录玩家的按键操作

录制文件可以完整重现游戏过程，包括：
- 玩家移动轨迹
- 敌人生成和移动
- 子弹发射和飞行轨迹
- 炸弹爆炸效果
- 所有游戏对象的动态变化

## 技术架构

### 组件系统(ECS)

引擎使用组件-实体系统(ECS)设计模式：

```java
// 创建游戏对象
GameObject player = new GameObject("Player");

// 添加变换组件
TransformComponent transform = player.addComponent(
    new TransformComponent(new Vector2(400, 300))
);

// 添加物理组件
PhysicsComponent physics = player.addComponent(
    new PhysicsComponent(1.0f)
);
physics.setFriction(0.95f);

// 添加渲染组件
RenderComponent render = player.addComponent(
    new RenderComponent(
        RenderComponent.RenderType.RECTANGLE,
        new Vector2(20, 20),
        new RenderComponent.Color(1.0f, 0.0f, 0.0f, 1.0f)
    )
);
```

### 场景管理

游戏包含三个主要场景：

1. **MenuScene（主菜单）**: 游戏启动界面
2. **GameScene（游戏场景）**: 实际游戏进行场景
3. **ReplayScene（回放场景）**: 游戏录像回放场景

### 录制与回放系统

录制系统在游戏进行时自动工作：
- 进入GameScene时自动开始录制
- 退出GameScene时自动停止并保存录制
- 回放时动态创建和销毁游戏对象，完整重现游戏过程

## 开发指南

### 添加新组件

```java
public class MyComponent extends Component<MyComponent> {
    // 组件数据
    private int value;
    
    public MyComponent(int value) {
        this.value = value;
    }
    
    @Override
    public void update(float deltaTime) {
        // 更新逻辑
    }
}
```

### 创建自定义游戏对象

```java
GameObject customObject = new GameObject("Custom") {
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // 自定义更新逻辑
    }
    
    @Override
    public void render() {
        // 自定义渲染逻辑
    }
};
```

## 技术特点

- **零外部依赖**: 仅使用Java标准库
- **简单构建**: Shell脚本一键编译运行
- **跨平台**: 支持Windows、Linux、Mac系统
- **组件化设计**: 基于ECS架构，易于扩展
- **场景系统**: 清晰的场景切换逻辑
- **录制回放**: 完整的游戏过程记录与重现
- **代码结构**: 职责分离，易于维护

## 许可证

本项目仅供学习和参考使用。

