package com.gameengine.recording;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import com.gameengine.math.Vector2;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * 录制服务
 * 负责录制游戏过程，包括关键帧和输入事件
 */
public class RecordingService {
    private final RecordingConfig config;
    private volatile boolean recording;
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();
    private double elapsed;
    private double keyframeElapsed;
    private final double warmupSec = 0.1; // 等待一帧让场景对象完成初始化
    private final DecimalFormat qfmt;
    private Scene lastScene;
    
    // 用于异步写入的缓冲区
    private java.util.concurrent.BlockingQueue<String> lineQueue;

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new java.util.concurrent.ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.qfmt = new DecimalFormat();
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
    }

    public boolean isRecording() {
        return recording;
    }
    
    public RecordingConfig getConfig() {
        return config;
    }

    /**
     * 开始录制
     * @param scene 要录制的场景
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        
        storage.openWriter(config.outputPath);
        
        // 启动异步写入线程
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { 
                            Thread.sleep(2); 
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { 
                    storage.closeWriter(); 
                } catch (Exception ignored) {}
            }
        }, "record-writer");
        
        recording = true;
        writerThread.start();

        // 写入头部信息
        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
    }

    /**
     * 停止录制
     */
    public void stop() {
        if (!recording) return;
        
        try {
            if (lastScene != null) {
                writeKeyframe(lastScene);
            }
        } catch (Exception ignored) {}
        
        recording = false;
        
        try { 
            writerThread.join(500); 
        } catch (InterruptedException ignored) {}
    }

    /**
     * 更新录制（每帧调用）
     * @param deltaTime 时间增量
     * @param scene 当前场景
     * @param input 输入管理器
     */
    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;
        lastScene = scene;

        // 记录输入事件（只记录刚按下的按键）
        Set<Integer> just = input.getJustPressedKeysSnapshot();
        if (!just.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"input\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
            boolean first = true;
            for (Integer k : just) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]}");
            enqueue(sb.toString());
        }

        // 定期写入关键帧（跳过开头暖机，避免空关键帧）
        if (elapsed >= warmupSec && keyframeElapsed >= config.keyframeIntervalSec) {
            if (writeKeyframe(scene)) {
                keyframeElapsed = 0.0;
            }
        }
    }

    /**
     * 写入关键帧
     * @param scene 当前场景
     * @return 是否成功写入
     */
    private boolean writeKeyframe(Scene scene) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"keyframe\",\"t\":").append(qfmt.format(elapsed)).append(",\"entities\":[");
        
        List<GameObject> objs = scene.getGameObjects();
        boolean first = true;
        int count = 0;
        
        // 为同名对象计数，生成唯一ID
        java.util.Map<String, Integer> nameCounters = new java.util.HashMap<>();
        
        for (GameObject obj : objs) {
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc == null) continue;
            
            float x = tc.getPosition().x;
            float y = tc.getPosition().y;
            
            // 生成唯一ID：对象名_序号
            String baseName = obj.getName();
            int index = nameCounters.getOrDefault(baseName, 0);
            nameCounters.put(baseName, index + 1);
            String uniqueId = baseName + "_" + index;
            
            if (!first) sb.append(',');
            sb.append('{')
              .append("\"id\":\"").append(uniqueId).append("\",")
              .append("\"name\":\"").append(baseName).append("\",")
              .append("\"x\":").append(qfmt.format(x)).append(',')
              .append("\"y\":").append(qfmt.format(y));

            // 可选渲染信息（若对象带有 RenderComponent，则记录形状、尺寸、颜色）
            RenderComponent rc = obj.getComponent(RenderComponent.class);
            if (rc != null) {
                RenderComponent.RenderType rt = rc.getRenderType();
                Vector2 sz = rc.getSize();
                RenderComponent.Color col = rc.getColor();
                sb.append(',')
                  .append("\"rt\":\"").append(rt.name()).append("\",")
                  .append("\"w\":").append(qfmt.format(sz.x)).append(',')
                  .append("\"h\":").append(qfmt.format(sz.y)).append(',')
                  .append("\"color\":[")
                  .append(qfmt.format(col.r)).append(',')
                  .append(qfmt.format(col.g)).append(',')
                  .append(qfmt.format(col.b)).append(',')
                  .append(qfmt.format(col.a)).append(']');
            } else {
                // 标记自定义渲染（如 Player），方便回放做近似还原
                sb.append(',').append("\"rt\":\"CUSTOM\"");
            }

            sb.append('}');
            first = false;
            count++;
        }
        
        sb.append("]}");
        
        if (count > 0) {
            enqueue(sb.toString());
            return true;
        }
        return false;
    }

    /**
     * 将数据加入写入队列
     */
    private void enqueue(String line) {
        try {
            lineQueue.put(line);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
