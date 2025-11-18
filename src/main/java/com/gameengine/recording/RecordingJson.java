package com.gameengine.recording;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的 JSON 解析工具类
 * 用于解析录制文件中的 JSONL 数据
 */
public final class RecordingJson {
    private RecordingJson() {}

    /**
     * 从 JSON 字符串中提取指定字段的值
     * @param json JSON 字符串
     * @param key 字段名
     * @return 字段值（未去除引号）
     */
    public static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = json.indexOf(':', i);
        if (c < 0) return null;
        int end = c + 1;
        int comma = json.indexOf(',', end);
        int brace = json.indexOf('}', end);
        int j = (comma < 0) ? brace : (brace < 0 ? comma : Math.min(comma, brace));
        if (j < 0) j = json.length();
        return json.substring(end, j).trim();
    }

    /**
     * 去除字符串两端的引号
     * @param s 字符串
     * @return 去除引号后的字符串
     */
    public static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * 解析字符串为 double
     * @param s 字符串
     * @return double 值
     */
    public static double parseDouble(String s) {
        if (s == null) return 0.0;
        try { 
            return Double.parseDouble(stripQuotes(s)); 
        } catch (Exception e) { 
            return 0.0; 
        }
    }

    /**
     * 分割顶层数组元素
     * @param arr 数组字符串（不含外层方括号）
     * @return 分割后的元素数组
     */
    public static String[] splitTopLevel(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < arr.length(); i++) {
            char ch = arr.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            } else if (ch == ',' && depth == 0) {
                out.add(arr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < arr.length()) {
            out.add(arr.substring(start));
        }
        return out.stream()
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .toArray(String[]::new);
    }

    /**
     * 提取数组内容（不含方括号）
     * @param json JSON 字符串
     * @param startIdx 数组开始位置的索引
     * @return 数组内容
     */
    public static String extractArray(String json, int startIdx) {
        int i = startIdx;
        if (i >= json.length() || json.charAt(i) != '[') {
            return "";
        }
        int depth = 1;
        int begin = i + 1;
        i++;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(begin, i);
                }
            }
        }
        return "";
    }
}
