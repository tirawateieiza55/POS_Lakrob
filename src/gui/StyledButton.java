package gui;
 
import javax.swing.*;
import java.awt.*;
 
/**
 * StyledButton - ปุ่มวาดสีเอง (สีเหมือนกันทุก OS/Look and Feel)
 * ------------------------------------------------------------------
 * ทำไมต้องมี: JButton ปกติบน macOS ไม่ยอมแสดงสีพื้นหลัง เลยวาด fillRoundRect เอง
 * เอฟเฟกต์: hover = สว่างขึ้น 30, กด = เข้มลง, disable = เทา + เคอร์เซอร์รูปมือ
 *
 * จะแก้ครั้งหน้า:
 * - เปลี่ยนทรงปุ่ม: แก้ค่า arc ตอน new (เช่น new StyledButton("...", สี, 10))
 * - เปลี่ยนสีปุ่มที่ active (เช่น เมนูที่เลือก): เรียก setBaseColor(สีใหม่)
 */
public class StyledButton extends JButton {
 
    private Color baseColor;
    private final int arc;
 
    public StyledButton(String text, Color baseColor) {
        this(text, baseColor, 8);
    }
 
    public StyledButton(String text, Color baseColor, int arc) {
        super(text);
        this.baseColor = baseColor;
        this.arc = arc;
        setForeground(Color.WHITE);
        setFont(AppFont.thai(Font.BOLD, 13));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
 
    /** เปลี่ยนสีหลักของปุ่ม (ใช้ตอนไฮไลต์ปุ่มที่ถูกเลือก) */
    public void setBaseColor(Color c) {
        this.baseColor = c;
        repaint();
    }
 
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 
        Color c = baseColor;
        if (!isEnabled()) {
            c = Color.GRAY;
        } else if (getModel().isPressed()) {
            c = baseColor.darker();
        } else if (getModel().isRollover()) {
            c = brighten(baseColor);
        }
        g2.setColor(c);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
 
        super.paintComponent(g);   // วาดข้อความทับ
    }
 
    private static Color brighten(Color c) {
        int r = Math.min(255, c.getRed() + 30);
        int gr = Math.min(255, c.getGreen() + 30);
        int b = Math.min(255, c.getBlue() + 30);
        return new Color(r, gr, b);
    }
}
