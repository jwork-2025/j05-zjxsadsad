package com.gameengine.graphics;

import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 渲染器
 */
public class Renderer extends JFrame {
    private int width;
    private int height;
    private String title;
    private GamePanel gamePanel;
    private InputManager inputManager;
    
    public Renderer(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.inputManager = InputManager.getInstance();
        
        initialize();
    }
    
    private void initialize() {
        setTitle(title);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        setupInput();
        
        setVisible(true);
    }
    
    private void setupInput() {
        // 键盘输入
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                inputManager.onKeyPressed(e.getKeyCode());
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                inputManager.onKeyReleased(e.getKeyCode());
            }
        });
        
        // 鼠标输入 - 将监听器添加到GamePanel而不是JFrame
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 将 Swing 的鼠标按钮映射到我们的索引：
                // MouseEvent.BUTTON1 (左键) = 1 -> 映射到 0
                // MouseEvent.BUTTON2 (中键) = 2 -> 映射到 1  
                // MouseEvent.BUTTON3 (右键) = 3 -> 映射到 2
                int button = e.getButton() - 1;
                inputManager.onMousePressed(button);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                int button = e.getButton() - 1;
                inputManager.onMouseReleased(button);
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                inputManager.onMouseMoved(e.getX(), e.getY());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                inputManager.onMouseMoved(e.getX(), e.getY());
            }
        };
        
        // 将鼠标监听器添加到 GamePanel
        gamePanel.addMouseListener(mouseAdapter);
        gamePanel.addMouseMotionListener(mouseAdapter);
        
        setFocusable(true);
        requestFocus();
    }
    
    /**
     * 开始渲染帧
     */
    public void beginFrame() {
        gamePanel.clear();
    }
    
    /**
     * 结束渲染帧
     */
    public void endFrame() {
        gamePanel.repaint();
    }
    
    /**
     * 绘制矩形
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        gamePanel.addDrawable(new RectDrawable(x, y, width, height, r, g, b, a));
    }
    
    /**
     * 绘制圆形
     */
    public void drawCircle(float x, float y, float radius, int segments, float r, float g, float b, float a) {
        gamePanel.addDrawable(new CircleDrawable(x, y, radius, r, g, b, a));
    }
    
    /**
     * 绘制线条
     */
    public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        gamePanel.addDrawable(new LineDrawable(x1, y1, x2, y2, r, g, b, a));
    }
    
    /**
     * 绘制文字
     */
    public void drawText(String text, float x, float y, int size, float r, float g, float b, float a) {
        gamePanel.addDrawable(new TextDrawable(text, x, y, size, r, g, b, a));
    }
    
    /**
     * 检查窗口是否应该关闭
     */
    public boolean shouldClose() {
        return !isVisible();
    }
    
    /**
     * 处理事件
     */
    public void pollEvents() {
        // Swing自动处理事件
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        dispose();
    }
    
    // Getters
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public String getTitle() {
        return title;
    }
    
    /**
     * 游戏面板类
     */
    private class GamePanel extends JPanel {
        private List<Drawable> drawables = new ArrayList<>();
        
        public GamePanel() {
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.BLACK);
        }
        
        public void clear() {
            drawables.clear();
        }
        
        public void addDrawable(Drawable drawable) {
            drawables.add(drawable);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (Drawable drawable : drawables) {
                drawable.draw(g2d);
            }
        }
    }
    
    /**
     * 可绘制对象接口
     */
    private interface Drawable {
        void draw(Graphics2D g);
    }
    
    /**
     * 矩形绘制类
     */
    private static class RectDrawable implements Drawable {
        private float x, y, width, height;
        private Color color;
        
        public RectDrawable(float x, float y, float width, float height, float r, float g, float b, float a) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = new Color(r, g, b, a);
        }
        
        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect((int) x, (int) y, (int) width, (int) height);
        }
    }
    
    /**
     * 圆形绘制类
     */
    private static class CircleDrawable implements Drawable {
        private float x, y, radius;
        private Color color;
        
        public CircleDrawable(float x, float y, float radius, float r, float g, float b, float a) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = new Color(r, g, b, a);
        }
        
        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        }
    }
    
    /**
     * 线条绘制类
     */
    private static class LineDrawable implements Drawable {
        private float x1, y1, x2, y2;
        private Color color;
        
        public LineDrawable(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = new Color(r, g, b, a);
        }
        
        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
    }
    
    /**
     * 文字绘制类
     */
    private static class TextDrawable implements Drawable {
        private String text;
        private float x, y;
        private int size;
        private Color color;
        
        public TextDrawable(String text, float x, float y, int size, float r, float g, float b, float a) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = new Color(r, g, b, a);
        }
        
        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.setFont(new Font("Microsoft YaHei", Font.BOLD, size));
            g.drawString(text, (int) x, (int) y);
        }
    }
}
