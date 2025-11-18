package com.gameengine.core;

import java.util.List;

import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

/**
 * 游戏逻辑类，处理具体的游戏规则
 */
public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    private Renderer renderer;
    private boolean anyKeyPressed = false;
    private int playerHealth = 20;
    private boolean gameOver = false;
    private Vector2 lastMovementDirection = new Vector2(1, 0); // 默认朝右
    
    // 炸弹系统
    private float bombCooldown = 0f; // 当前CD时间
    private static final float BOMB_CD_TIME = 20.0f; // CD总时间20秒
    private static final float BOMB_RADIUS = 200.0f; // 炸弹爆炸半径
    private static final int BOMB_DAMAGE = 8; // 炸弹伤害
    private boolean bombKeyPressed = false; // J键防抖
    
    public GameLogic(Scene scene, Renderer renderer) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        this.renderer = renderer;
    }
    
    /**
     * 处理玩家输入
     */
    public void handlePlayerInput() {
        
        if (gameOver) return; // 游戏结束时禁用输入
        
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;
        
        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
        
        if (transform == null || physics == null) return;
        
        Vector2 movement = new Vector2();
        
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }
        
        if (movement.magnitude() > 0) {
            movement = movement.normalize();
            // 更新最后的移动方向（支持斜向）
            lastMovementDirection = new Vector2(movement.x, movement.y);
            physics.setVelocity(movement.multiply(200));
        }
        
        // 开火：H键或空格键
        if ((inputManager.isKeyPressed(72) || inputManager.isKeyPressed(32)) && !anyKeyPressed) { // H键或空格
            spawnBullet(transform.getPosition(), lastMovementDirection);
            anyKeyPressed = true;
        } else if (!inputManager.isKeyPressed(72) && !inputManager.isKeyPressed(32)) {
            anyKeyPressed = false;
        }
        
        // J键：发射炸弹（有CD限制）
        if (inputManager.isKeyPressed(74)) { // J键
            if (!bombKeyPressed && bombCooldown <= 0) {
                spawnBomb(transform.getPosition());
                bombCooldown = BOMB_CD_TIME; // 重置CD
                bombKeyPressed = true;
            }
        } else {
            bombKeyPressed = false; // J键释放后重置
        }
        
        // 重新开始游戏：R键
        if (gameOver && inputManager.isKeyPressed(82)) { // R键
            restartGame();
        }
        
        // 边界检查
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 20) pos.x = 800 - 20;
        if (pos.y > 600 - 20) pos.y = 600 - 20;
        transform.setPosition(pos);
    }
    
    /**
     * 更新物理系统
     */
    public void updatePhysics(float deltaTime) {
        // 更新炸弹CD
        if (bombCooldown > 0) {
            bombCooldown -= deltaTime;
            if (bombCooldown < 0) bombCooldown = 0;
        }
        
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        for (PhysicsComponent physics : physicsComponents) {
            GameObject owner = physics.getOwner();
            TransformComponent transform = owner.getComponent(TransformComponent.class);
            if (transform != null) {
                Vector2 pos = transform.getPosition();
                Vector2 velocity = physics.getVelocity();
                
                // 敌人随机改变方向
                if ("Enemy".equals(owner.getName()) && Math.random() < 0.02) { // 2%概率改变方向
                    velocity.x = (float)((Math.random() - 0.5) * 200); // 增加速度范围
                    velocity.y = (float)((Math.random() - 0.5) * 200);
                    physics.setVelocity(velocity);
                }
                
                // 边界反弹 - 敌人可以在更大范围内移动
                if ("Enemy".equals(owner.getName())) {
                    if (pos.x <= -50 || pos.x >= 850) {
                        velocity.x = -velocity.x;
                        physics.setVelocity(velocity);
                    }
                    if (pos.y <= -50 || pos.y >= 650) {
                        velocity.y = -velocity.y;
                        physics.setVelocity(velocity);
                    }
                    
                    // 确保在扩展边界内
                    if (pos.x < -50) pos.x = -50;
                    if (pos.y < -50) pos.y = -50;
                    if (pos.x > 850) pos.x = 850;
                    if (pos.y > 650) pos.y = 650;
                    transform.setPosition(pos);
                } else {
                    // 玩家边界检查
                    if (pos.x <= 0 || pos.x >= 800 - 15) {
                        velocity.x = -velocity.x;
                        physics.setVelocity(velocity);
                    }
                    if (pos.y <= 0 || pos.y >= 600 - 15) {
                        velocity.y = -velocity.y;
                        physics.setVelocity(velocity);
                    }
                    
                    // 确保在边界内
                    if (pos.x < 0) pos.x = 0;
                    if (pos.y < 0) pos.y = 0;
                    if (pos.x > 800 - 15) pos.x = 800 - 15;
                    if (pos.y > 600 - 15) pos.y = 600 - 15;
                    transform.setPosition(pos);
                }
            }
        }
    }
    
    /**
     * 检查碰撞
     */
    public void checkCollisions() {
        if (gameOver) return; // 游戏结束时不检查碰撞
        
        // 玩家与敌人碰撞：减少血量并弹开
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (!players.isEmpty()) {
            GameObject player = players.get(0);
            TransformComponent playerTransform = player.getComponent(TransformComponent.class);
            PhysicsComponent playerPhysics = player.getComponent(PhysicsComponent.class);
            if (playerTransform != null && playerPhysics != null) {
                for (GameObject obj : scene.getGameObjects()) {
                    if ("Enemy".equals(obj.getName())) {
                        TransformComponent enemyTransform = obj.getComponent(TransformComponent.class);
                        PhysicsComponent enemyPhysics = obj.getComponent(PhysicsComponent.class);
                        if (enemyTransform != null && enemyPhysics != null) {
                            float distance = playerTransform.getPosition().distance(enemyTransform.getPosition());
                            if (distance < 25) {
                                // 减少玩家血量
                                playerHealth--;
                                System.out.println("血量: " + playerHealth + "/20");
                                if (playerHealth <= 0) {
                                    gameOver = true;
                                    System.out.println("游戏结束！按R键重新开始");
                                }
                                
                                // 计算碰撞方向
                                Vector2 playerPos = playerTransform.getPosition();
                                Vector2 enemyPos = enemyTransform.getPosition();
                                Vector2 collisionDirection = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y);
                                if (collisionDirection.magnitude() > 0) {
                                    collisionDirection = collisionDirection.normalize();
                                    
                                    // 弹开玩家
                                    Vector2 playerVelocity = playerPhysics.getVelocity();
                                    playerVelocity = playerVelocity.add(collisionDirection.multiply(300));
                                    playerPhysics.setVelocity(playerVelocity);
                                    
                                    // 弹开敌人
                                    Vector2 enemyVelocity = enemyPhysics.getVelocity();
                                    enemyVelocity = enemyVelocity.add(collisionDirection.multiply(-200));
                                    enemyPhysics.setVelocity(enemyVelocity);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 子弹与敌人碰撞：减少敌人血量
        List<GameObject> allObjects = scene.getGameObjects();
        for (GameObject objBullet : allObjects) {
            if (!"Bullet".equals(objBullet.getName())) continue;
            TransformComponent bulletT = objBullet.getComponent(TransformComponent.class);
            if (bulletT == null) continue;
            for (GameObject objEnemy : allObjects) {
                if (!"Enemy".equals(objEnemy.getName())) continue;
                TransformComponent enemyT = objEnemy.getComponent(TransformComponent.class);
                if (enemyT == null) continue;
                float dist = bulletT.getPosition().distance(enemyT.getPosition());
                if (dist < 15) {
                    // 减少敌人血量
                    int enemyHealth = getEnemyHealth(objEnemy);
                    enemyHealth--;
                    setEnemyHealth(objEnemy, enemyHealth);
                    
                    // 移除子弹
                    objBullet.destroy();
                    
                    // 如果敌人血量归零，移除敌人
                    if (enemyHealth <= 0) {
                        objEnemy.destroy();
                    }
                    break;
                }
            }
        }
    }

    private void spawnBullet(Vector2 playerPos, Vector2 direction) {
        if (renderer == null) return;
        
        // 使用传入的方向，如果方向为零则不发射
        if (direction.magnitude() == 0) return;
        
        // 确保方向已归一化
        Vector2 normalizedDirection = direction.normalize();

        GameObject bullet = new GameObject("Bullet") {
            private Vector2 startPosition;
            private final float maxDistance = 5 * 800; // 5个屏幕宽度的距离
            private boolean initialized = false;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                
                TransformComponent transform = getComponent(TransformComponent.class);
                PhysicsComponent physics = getComponent(PhysicsComponent.class);
                if (transform != null && physics != null) {
                    if (!initialized) {
                        startPosition = new Vector2(transform.getPosition());
                        initialized = true;
                    }
                    
                    // 检查飞行距离
                    Vector2 currentPos = transform.getPosition();
                    float distance = startPosition.distance(currentPos);
                    if (distance >= maxDistance) {
                        destroy();
                        return;
                    }
                    
                    // 边界反弹
                    Vector2 pos = transform.getPosition();
                    Vector2 velocity = physics.getVelocity();
                    
                    // 检查左右边界反弹
                    if (pos.x <= 4 || pos.x >= 796) {
                        velocity.x = -velocity.x;
                        physics.setVelocity(velocity);
                    }
                    
                    // 检查上下边界反弹
                    if (pos.y <= 4 || pos.y >= 596) {
                        velocity.y = -velocity.y;
                        physics.setVelocity(velocity);
                    }
                    
                    // 确保子弹在边界内
                    if (pos.x < 4) pos.x = 4;
                    if (pos.y < 4) pos.y = 4;
                    if (pos.x > 796) pos.x = 796;
                    if (pos.y > 596) pos.y = 596;
                    transform.setPosition(pos);
                }
            }
        };

        bullet.addComponent(new TransformComponent(new Vector2(playerPos)));
        PhysicsComponent p = bullet.addComponent(new PhysicsComponent(0.1f));
        p.setFriction(1.0f);
        p.setVelocity(normalizedDirection.multiply(400));
        RenderComponent r = bullet.addComponent(new RenderComponent(
                RenderComponent.RenderType.CIRCLE,
                new Vector2(8, 8),
                new RenderComponent.Color(1.0f, 0.0f, 0.0f, 1.0f) // 红色
        ));
        r.setRenderer(renderer);

        scene.addGameObject(bullet);
    }
    
    /**
     * 生成炸弹
     */
    private void spawnBomb(Vector2 playerPos) {
        if (renderer == null) return;

        GameObject bomb = new GameObject("Bomb") {
            private float lifetime = 0f;
            private static final float EXPLODE_TIME = 1.5f; // 1.5秒后爆炸
            private boolean exploded = false;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                
                lifetime += deltaTime;
                
                // 到达爆炸时间
                if (lifetime >= EXPLODE_TIME && !exploded) {
                    explode();
                    exploded = true;
                }
                
                // 爆炸后0.5秒消失
                if (exploded && lifetime >= EXPLODE_TIME + 0.5f) {
                    destroy();
                }
            }
            
            @Override
            public void render() {
                renderComponents();
            }
            
            private void explode() {
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform == null) return;
                
                Vector2 bombPos = transform.getPosition();
                
                // 对范围内的所有敌人造成伤害
                List<GameObject> allObjects = scene.getGameObjects();
                for (GameObject obj : allObjects) {
                    if ("Enemy".equals(obj.getName())) {
                        TransformComponent enemyTransform = obj.getComponent(TransformComponent.class);
                        if (enemyTransform != null) {
                            float distance = bombPos.distance(enemyTransform.getPosition());
                            if (distance <= BOMB_RADIUS) {
                                // 造成5点伤害
                                int enemyHealth = getEnemyHealth(obj);
                                enemyHealth -= BOMB_DAMAGE;
                                setEnemyHealth(obj, enemyHealth);
                                
                                // 如果血量归零则摧毁
                                if (enemyHealth <= 0) {
                                    obj.destroy();
                                }
                            }
                        }
                    }
                }
                
                // 创建爆炸视觉效果
                createExplosionEffect(bombPos);
            }
            
            private void createExplosionEffect(Vector2 pos) {
                // 创建多个扩散的粒子效果
                for (int i = 0; i < 12; i++) {
                    float angle = (float) (i * Math.PI * 2 / 12);
                    Vector2 direction = new Vector2(
                        (float) Math.cos(angle),
                        (float) Math.sin(angle)
                    );
                    createExplosionParticle(pos, direction);
                }
            }
            
            private void createExplosionParticle(Vector2 pos, Vector2 direction) {
                GameObject particle = new GameObject("ExplosionParticle") {
                    private float lifetime = 0f;
                    
                    @Override
                    public void update(float deltaTime) {
                        super.update(deltaTime);
                        updateComponents(deltaTime);
                        
                        lifetime += deltaTime;
                        if (lifetime >= 0.5f) {
                            destroy();
                        }
                    }
                };
                
                particle.addComponent(new TransformComponent(new Vector2(pos)));
                PhysicsComponent p = particle.addComponent(new PhysicsComponent(0.1f));
                p.setVelocity(direction.multiply(200));
                p.setFriction(0.9f);
                
                RenderComponent r = particle.addComponent(new RenderComponent(
                    RenderComponent.RenderType.CIRCLE,
                    new Vector2(6, 6),
                    new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f) // 橙色爆炸效果
                ));
                r.setRenderer(renderer);
                
                scene.addGameObject(particle);
            }
        };

        bomb.addComponent(new TransformComponent(new Vector2(playerPos)));
        
        // 炸弹有轻微的物理效果
        PhysicsComponent p = bomb.addComponent(new PhysicsComponent(0.5f));
        p.setFriction(0.95f);
        
        // 炸弹渲染 - 金黄色大圆球，闪烁效果
        RenderComponent r = bomb.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(15, 15),
            new RenderComponent.Color(1.0f, 0.84f, 0.0f, 1.0f) // 金黄色
        ));
        r.setRenderer(renderer);

        scene.addGameObject(bomb);
    }
    
    /**
     * 重新开始游戏
     */
    private void restartGame() {
        playerHealth = 20;
        gameOver = false;
        bombCooldown = 0; // 重置炸弹CD
        System.out.println("游戏重新开始！");
        
        // 重置玩家位置
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (!players.isEmpty()) {
            GameObject player = players.get(0);
            TransformComponent playerTransform = player.getComponent(TransformComponent.class);
            if (playerTransform != null) {
                playerTransform.setPosition(new Vector2(400, 300));
            }
        }
        
        // 清除所有子弹
        List<GameObject> allObjects = scene.getGameObjects();
        for (GameObject obj : allObjects) {
            if ("Bullet".equals(obj.getName())) {
                obj.destroy();
            }
        }
    }
    
    /**
     * 渲染游戏UI
     */
    public void renderUI() {
        if (renderer == null) return;
        
        // 渲染玩家血量条
        float barWidth = 200;
        float barHeight = 20;
        float barX = 10;
        float barY = 10;
        
        // 玩家血量条背景
        renderer.drawRect(barX, barY, barWidth, barHeight, 0.3f, 0.3f, 0.3f, 1.0f);
        
        // 玩家血量条
        float healthPercent = (float)playerHealth / 20.0f;
        float healthWidth = barWidth * healthPercent;
        float healthColor = healthPercent > 0.5f ? 0.0f : (1.0f - healthPercent * 2.0f);
        renderer.drawRect(barX, barY, healthWidth, barHeight, healthColor, healthPercent, 0.0f, 1.0f);
        
        // 血量文字
        renderer.drawText("血量: " + playerHealth + "/20", barX + 5, barY + 15, 12, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // 渲染敌人血量条
        List<GameObject> allObjects = scene.getGameObjects();
        for (GameObject obj : allObjects) {
            if ("Enemy".equals(obj.getName())) {
                TransformComponent enemyTransform = obj.getComponent(TransformComponent.class);
                if (enemyTransform != null) {
                    Vector2 enemyPos = enemyTransform.getPosition();
                    
                    // 敌人血量条参数
                    float enemyBarWidth = 30;
                    float enemyBarHeight = 4;
                    float enemyBarX = enemyPos.x - enemyBarWidth / 2;
                    float enemyBarY = enemyPos.y - 25; // 在敌人头上
                    
                    // 敌人血量条背景
                    renderer.drawRect(enemyBarX, enemyBarY, enemyBarWidth, enemyBarHeight, 0.2f, 0.2f, 0.2f, 0.8f);
                    
                    // 敌人血量条
                    int enemyHealth = getEnemyHealth(obj);
                    float enemyHealthPercent = (float)enemyHealth / 10.0f;
                    float enemyHealthWidth = enemyBarWidth * enemyHealthPercent;
                    float enemyHealthColor = enemyHealthPercent > 0.5f ? 0.0f : (1.0f - enemyHealthPercent * 2.0f);
                    renderer.drawRect(enemyBarX, enemyBarY, enemyHealthWidth, enemyBarHeight, enemyHealthColor, enemyHealthPercent, 0.0f, 1.0f);
                }
            }
        }
        
        // 游戏结束提示
        if (gameOver) {
            renderer.drawRect(200, 250, 400, 100, 0.0f, 0.0f, 0.0f, 0.9f);
            renderer.drawText("游戏结束！", 330, 290, 32, 1.0f, 0.0f, 0.0f, 1.0f);
            renderer.drawText("按 R 键重新开始", 310, 325, 20, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // 炸弹CD条
        float bombBarWidth = 150;
        float bombBarHeight = 20;
        float bombBarX = 10;
        float bombBarY = 40;
        
        // 炸弹CD背景
        renderer.drawRect(bombBarX, bombBarY, bombBarWidth, bombBarHeight, 0.3f, 0.3f, 0.3f, 1.0f);
        
        // 炸弹CD进度
        float bombProgress = 1.0f - (bombCooldown / BOMB_CD_TIME);
        float bombWidth = bombBarWidth * bombProgress;
        if (bombProgress >= 1.0f) {
            // 炸弹可用 - 显示金色
            renderer.drawRect(bombBarX, bombBarY, bombWidth, bombBarHeight, 1.0f, 0.84f, 0.0f, 1.0f);
            renderer.drawText("炸弹: 就绪", bombBarX + 5, bombBarY + 15, 12, 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            // 炸弹冷却中 - 显示灰色
            renderer.drawRect(bombBarX, bombBarY, bombWidth, bombBarHeight, 0.5f, 0.5f, 0.5f, 1.0f);
            int cdSeconds = (int) Math.ceil(bombCooldown);
            renderer.drawText("炸弹: " + cdSeconds + "秒", bombBarX + 5, bombBarY + 15, 12, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // 操作提示
        if (!gameOver) {
            // 绘制操作提示背景 - 移到更靠上的位置
            renderer.drawRect(10, 420, 350, 80, 0.0f, 0.0f, 0.0f, 0.8f);
            
            // 标题
            renderer.drawText("操作说明", 20, 440, 16, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // WASD移动
            renderer.drawRect(20, 450, 15, 15, 0.0f, 1.0f, 1.0f, 1.0f);
            renderer.drawText("WASD - 移动", 45, 462, 14, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // H键普通攻击
            renderer.drawCircle(27, 478, 6, 8, 1.0f, 0.0f, 0.0f, 1.0f);
            renderer.drawText("H键 - 普通攻击", 45, 482, 14, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // J键炸弹
            renderer.drawCircle(27, 493, 9, 8, 1.0f, 0.84f, 0.0f, 1.0f);
            if (bombCooldown > 0) {
                int cdSeconds = (int) Math.ceil(bombCooldown);
                renderer.drawText("J键 - 炸弹 (CD: " + cdSeconds + "秒)", 45, 497, 14, 1.0f, 0.5f, 0.0f, 1.0f);
            } else {
                renderer.drawText("J键 - 炸弹 (可用)", 45, 497, 14, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        }
    }
    
    /**
     * 获取游戏状态
     */
    public boolean isGameOver() {
        return gameOver;
    }
    
    /**
     * 获取玩家血量
     */
    public int getPlayerHealth() {
        return playerHealth;
    }
    
    /**
     * 获取敌人血量
     */
    private int getEnemyHealth(GameObject enemy) {
        Object health = enemy.getUserData("health");
        return health != null ? (Integer)health : 10; // 默认10点血量
    }
    
    /**
     * 设置敌人血量
     */
    private void setEnemyHealth(GameObject enemy, int health) {
        enemy.setUserData("health", health);
    }
}
