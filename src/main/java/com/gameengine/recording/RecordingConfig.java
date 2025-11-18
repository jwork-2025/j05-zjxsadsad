package com.gameengine.recording;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 录制配置类
 * 定义关键帧间隔、输出路径等参数
 */
public class RecordingConfig {
    /** 关键帧记录间隔（秒） */
    public final double keyframeIntervalSec;
    
    /** 输出文件路径 */
    public final String outputPath;
    
    /** 队列容量 */
    public final int queueCapacity;
    
    /** 数值量化精度（小数位数） */
    public final int quantizeDecimals;

    /**
     * 创建默认配置
     */
    public RecordingConfig() {
        this(0.5, generateDefaultPath(), 1000, 2);
    }

    /**
     * 创建自定义配置
     * @param keyframeIntervalSec 关键帧间隔（秒）
     * @param outputPath 输出路径
     * @param queueCapacity 队列容量
     * @param quantizeDecimals 数值精度
     */
    public RecordingConfig(double keyframeIntervalSec, String outputPath, 
                          int queueCapacity, int quantizeDecimals) {
        this.keyframeIntervalSec = keyframeIntervalSec;
        this.outputPath = outputPath;
        this.queueCapacity = queueCapacity;
        this.quantizeDecimals = quantizeDecimals;
    }

    /**
     * 生成默认的输出路径（带时间戳）
     */
    private static String generateDefaultPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        return "recordings/game_" + timestamp + ".jsonl";
    }
}
