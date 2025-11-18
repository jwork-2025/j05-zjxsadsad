package com.gameengine.example;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingJson;

import java.io.File;
import java.util.*;

/**
 * 回放场景
 * 读取录制文件并播放
 */
public class ReplayScene extends Scene {
    private final GameEngine engine;
    private String recordingPath;
    private Renderer renderer;
    private InputManager input;
    private float time;

    // 关键帧数据结构
    private static class Keyframe {
        static class EntityInfo {
            String id;      // 唯一ID（如 Enemy_0, Enemy_1）
            String name;    // 对象名称（如 Enemy）
            Vector2 pos;
            String rt; // 渲染类型
            float w, h;
            float r = 0.9f, g = 0.9f, b = 0.2f, a = 1.0f; // 默认颜色
        }
        double t;
        List<EntityInfo> entities = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final List<GameObject> objectList = new ArrayList<>();
    
    // 文件选择模式
    private List<File> recordingFiles;
    private int selectedIndex = 0;
    private boolean replayEnded = false;

    public ReplayScene(GameEngine engine, String path) {
        super("Replay");
        this.engine = engine;
        this.recordingPath = path;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        this.time = 0f;
        this.keyframes.clear();
        this.objectList.clear();
        
        if (recordingPath != null) {
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
        } else {
            // 列出所有录制文件
            RecordingStorage storage = new FileRecordingStorage();
            this.recordingFiles = storage.listRecordings();
            this.selectedIndex = 0;
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // 回放结束后按 R 返回主菜单
        if (replayEnded) {
            if (input.isKeyJustPressed(82)) { // R 键
                engine.setScene(new MenuScene(engine));
            }
            return;
        }
        
        // ESC 结束回放
        if (input.isKeyJustPressed(27)) {
            replayEnded = true;
            return;
        }
        
        // 文件选择模式
        if (recordingPath == null) {
            handleFileSelection();
            return;
        }

        if (keyframes.size() < 1) return;
        
        time += deltaTime;
        
        // 限制在最后关键帧处停止
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time >= lastT) {
            time = (float) lastT;
            replayEnded = true;
        }

        // 查找插值区间
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) {
                a = k1;
                b = k2;
                break;
            }
        }
        
        double span = Math.max(1e-6, b.t - a.t);
        double u = Math.min(1.0, Math.max(0.0, (time - a.t) / span));
        
        updateInterpolatedPositions(a, b, (float) u);
    }

    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        if (recordingPath == null) {
            renderFileSelection();
        } else {
            // 渲染回放对象
            super.render();
            
            if (replayEnded) {
                // 回放结束提示
                renderer.drawRect(200, 250, 400, 100, 0.0f, 0.0f, 0.0f, 0.8f);
                renderer.drawText("回放结束！", 320, 280, 24, 1.0f, 1.0f, 0.3f, 1.0f);
                renderer.drawText("按 R 键返回主菜单", 280, 310, 16, 0.8f, 0.8f, 0.8f, 1.0f);
            } else if (keyframes.size() > 0) {
                // 显示回放进度
                double lastT = keyframes.get(keyframes.size() - 1).t;
                String info = String.format("回放中... %.1f / %.1f 秒 (ESC 结束)", time, lastT);
                renderer.drawText(info, 10, 20, 14, 0.8f, 0.8f, 0.8f, 1.0f);
            }
        }
    }

    private void handleFileSelection() {
        if (recordingFiles == null || recordingFiles.isEmpty()) {
            return;
        }
        
        // 上下键选择文件
        if (input.isKeyJustPressed(38)) { // UP
            selectedIndex = (selectedIndex - 1 + recordingFiles.size()) % recordingFiles.size();
        }
        if (input.isKeyJustPressed(40)) { // DOWN
            selectedIndex = (selectedIndex + 1) % recordingFiles.size();
        }
        
        // 回车键确认选择
        if (input.isKeyJustPressed(10)) { // ENTER
            File selected = recordingFiles.get(selectedIndex);
            recordingPath = selected.getPath();
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
        }
    }

    private void renderFileSelection() {
        renderer.drawText("选择录制文件 (↑↓ 选择, Enter 确认, ESC 返回)", 10, 20, 14, 1.0f, 1.0f, 1.0f, 1.0f);
        
        if (recordingFiles == null || recordingFiles.isEmpty()) {
            renderer.drawText("没有找到录制文件", 10, 60, 14, 0.8f, 0.3f, 0.3f, 1.0f);
            return;
        }
        
        for (int i = 0; i < recordingFiles.size(); i++) {
            File f = recordingFiles.get(i);
            float y = 60 + i * 30;
            float r = (i == selectedIndex) ? 1.0f : 0.6f;
            float g = (i == selectedIndex) ? 1.0f : 0.6f;
            float b = (i == selectedIndex) ? 0.2f : 0.6f;
            
            String text = (i == selectedIndex ? "> " : "  ") + f.getName();
            renderer.drawText(text, 10, y, 14, r, g, b, 1.0f);
        }
    }

    private void loadRecording(String path) {
        try {
            RecordingStorage storage = new FileRecordingStorage();
            Iterable<String> lines = storage.readLines(path);
            
            for (String line : lines) {
                String type = RecordingJson.stripQuotes(RecordingJson.field(line, "type"));
                
                if ("keyframe".equals(type)) {
                    Keyframe kf = new Keyframe();
                    kf.t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                    
                    String entitiesField = RecordingJson.field(line, "entities");
                    if (entitiesField != null) {
                        int idx = line.indexOf("\"entities\"");
                        if (idx >= 0) {
                            int arrStart = line.indexOf('[', idx);
                            if (arrStart >= 0) {
                                String arr = RecordingJson.extractArray(line, arrStart);
                                String[] parts = RecordingJson.splitTopLevel(arr);
                                
                                for (String p : parts) {
                                    Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                                    ei.id = RecordingJson.stripQuotes(RecordingJson.field(p, "id"));
                                    ei.name = RecordingJson.stripQuotes(RecordingJson.field(p, "name"));
                                    // 兼容旧格式：如果没有name字段，使用id
                                    if (ei.name == null || ei.name.isEmpty()) {
                                        ei.name = ei.id;
                                    }
                                    ei.pos = new Vector2(
                                        (float) RecordingJson.parseDouble(RecordingJson.field(p, "x")),
                                        (float) RecordingJson.parseDouble(RecordingJson.field(p, "y"))
                                    );
                                    ei.rt = RecordingJson.stripQuotes(RecordingJson.field(p, "rt"));
                                    ei.w = (float) RecordingJson.parseDouble(RecordingJson.field(p, "w"));
                                    ei.h = (float) RecordingJson.parseDouble(RecordingJson.field(p, "h"));
                                    
                                    String colorField = RecordingJson.field(p, "color");
                                    if (colorField != null) {
                                        int colorIdx = p.indexOf("\"color\"");
                                        if (colorIdx >= 0) {
                                            int colorArrStart = p.indexOf('[', colorIdx);
                                            if (colorArrStart >= 0) {
                                                String colorArr = RecordingJson.extractArray(p, colorArrStart);
                                                String[] rgba = colorArr.split(",");
                                                if (rgba.length >= 4) {
                                                    ei.r = (float) Double.parseDouble(rgba[0].trim());
                                                    ei.g = (float) Double.parseDouble(rgba[1].trim());
                                                    ei.b = (float) Double.parseDouble(rgba[2].trim());
                                                    ei.a = (float) Double.parseDouble(rgba[3].trim());
                                                }
                                            }
                                        }
                                    }
                                    
                                    kf.entities.add(ei);
                                }
                            }
                        }
                    }
                    
                    keyframes.add(kf);
                }
            }
            
            System.out.println("加载了 " + keyframes.size() + " 个关键帧");
        } catch (Exception e) {
            System.err.println("加载录制文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildObjectsFromFirstKeyframe() {
        if (keyframes.isEmpty()) return;
        
        Keyframe first = keyframes.get(0);
        for (Keyframe.EntityInfo ei : first.entities) {
            // 使用唯一ID作为GameObject名称
            GameObject obj = new GameObject(ei.id);
            obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
            
            // 根据渲染类型创建对应组件
            if (ei.rt != null && !"CUSTOM".equals(ei.rt)) {
                RenderComponent.RenderType rt = RenderComponent.RenderType.RECTANGLE;
                if ("CIRCLE".equals(ei.rt)) {
                    rt = RenderComponent.RenderType.CIRCLE;
                } else if ("LINE".equals(ei.rt)) {
                    rt = RenderComponent.RenderType.LINE;
                }
                
                RenderComponent rc = new RenderComponent(
                    rt,
                    new Vector2(ei.w, ei.h),
                    new RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                );
                rc.setRenderer(renderer);
                obj.addComponent(rc);
            } else if ("CUSTOM".equals(ei.rt) || "Player".equals(ei.name)) {
                // 自定义渲染（如 Player）
                final String objId = ei.id;
                final Vector2 initPos = new Vector2(ei.pos);
                obj = new GameObject(objId) {
                    private Vector2 basePosition;
                    
                    @Override
                    public void update(float deltaTime) {
                        super.update(deltaTime);
                        TransformComponent transform = getComponent(TransformComponent.class);
                        if (transform != null) {
                            basePosition = transform.getPosition();
                        }
                    }
                    
                    @Override
                    public void render() {
                        if (basePosition == null) return;
                        
                        // 渲染葫芦娃（简化版）
                        renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
                        renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
                        renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
                        renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
                    }
                };
                obj.addComponent(new TransformComponent(initPos));
            }
            
            objectList.add(obj);
            addGameObject(obj);
        }
    }

    private void updateInterpolatedPositions(Keyframe a, Keyframe b, float u) {
        // 收集当前关键帧中应该存在的所有实体ID
        java.util.Set<String> currentEntityIds = new java.util.HashSet<>();
        for (Keyframe.EntityInfo ei : b.entities) {
            currentEntityIds.add(ei.id);
        }
        
        // 移除不再存在的对象（被击杀的敌人、消失的子弹等）
        java.util.Iterator<GameObject> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (!currentEntityIds.contains(obj.getName())) {
                obj.destroy();  // 标记为非活跃状态
                iterator.remove();
            }
        }
        
        // 更新现有对象的位置，或添加新对象
        for (Keyframe.EntityInfo eb : b.entities) {
            GameObject obj = findGameObjectById(eb.id);
            
            if (obj == null) {
                // 新对象，需要创建
                obj = createGameObjectFromEntity(eb);
                objectList.add(obj);
                addGameObject(obj);
            }
            
            // 更新位置（插值）
            Keyframe.EntityInfo ea = findEntity(a, eb.id);
            if (ea != null) {
                TransformComponent tc = obj.getComponent(TransformComponent.class);
                if (tc != null) {
                    float x = ea.pos.x + (eb.pos.x - ea.pos.x) * u;
                    float y = ea.pos.y + (eb.pos.y - ea.pos.y) * u;
                    tc.setPosition(new Vector2(x, y));
                }
            } else {
                // 如果在a中不存在（刚生成的对象），直接使用b的位置
                TransformComponent tc = obj.getComponent(TransformComponent.class);
                if (tc != null) {
                    tc.setPosition(new Vector2(eb.pos.x, eb.pos.y));
                }
            }
        }
    }
    
    private GameObject findGameObjectById(String id) {
        for (GameObject obj : objectList) {
            if (id.equals(obj.getName())) {
                return obj;
            }
        }
        return null;
    }
    
    private GameObject createGameObjectFromEntity(Keyframe.EntityInfo ei) {
        GameObject obj = new GameObject(ei.id);
        obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
        
        // 根据渲染类型创建对应组件
        if (ei.rt != null && !"CUSTOM".equals(ei.rt)) {
            RenderComponent.RenderType rt = RenderComponent.RenderType.RECTANGLE;
            if ("CIRCLE".equals(ei.rt)) {
                rt = RenderComponent.RenderType.CIRCLE;
            } else if ("LINE".equals(ei.rt)) {
                rt = RenderComponent.RenderType.LINE;
            }
            
            RenderComponent rc = new RenderComponent(
                rt,
                new Vector2(ei.w, ei.h),
                new RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
            );
            rc.setRenderer(renderer);
            obj.addComponent(rc);
        } else if ("CUSTOM".equals(ei.rt) || "Player".equals(ei.name)) {
            // 自定义渲染（如 Player）
            final String objId = ei.id;
            final Vector2 initPos = new Vector2(ei.pos);
            obj = new GameObject(objId) {
                private Vector2 basePosition;
                
                @Override
                public void update(float deltaTime) {
                    super.update(deltaTime);
                    TransformComponent transform = getComponent(TransformComponent.class);
                    if (transform != null) {
                        basePosition = transform.getPosition();
                    }
                }
                
                @Override
                public void render() {
                    if (basePosition == null) return;
                    
                    // 渲染葫芦娃（简化版）
                    renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
                    renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
                    renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
                    renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
                }
            };
            obj.addComponent(new TransformComponent(initPos));
        }
        
        return obj;
    }

    private Keyframe.EntityInfo findEntity(Keyframe kf, String id) {
        for (Keyframe.EntityInfo ei : kf.entities) {
            if (id.equals(ei.id)) {
                return ei;
            }
        }
        return null;
    }
}
