package com.gameengine.recording;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 录制存储抽象接口
 * 定义录制数据的读写和列举方法
 */
public interface RecordingStorage {
    /**
     * 打开写入器
     * @param path 文件路径
     * @throws IOException 如果打开失败
     */
    void openWriter(String path) throws IOException;
    
    /**
     * 写入一行数据
     * @param line 要写入的行
     * @throws IOException 如果写入失败
     */
    void writeLine(String line) throws IOException;
    
    /**
     * 关闭写入器
     */
    void closeWriter();

    /**
     * 读取文件的所有行
     * @param path 文件路径
     * @return 行的可迭代对象
     * @throws IOException 如果读取失败
     */
    Iterable<String> readLines(String path) throws IOException;
    
    /**
     * 列出所有录制文件
     * @return 录制文件列表
     */
    List<File> listRecordings();
}
