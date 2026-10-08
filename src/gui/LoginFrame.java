package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("ระบบออเดอร์หน้าร้าน");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        Color headerBg = new Color(0x4A0707);
        Color bodyBg = new Color(0xE4DEDE);
        Color fieldBorder = new Color(0xE8172E);
        Color buttonBg = new Color(0x94282B);

        // หัวบนสุด
        JPanel header = new JPanel();
        header.setBackground(headerBg);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(52, 0, 62, 0));

        JLabel title = new JLabel("ระบบออเดอร์หน้าร้าน");
        title.setFont(AppFont.thai(Font.BOLD, 52));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("โปรดลงชื่อเข้าสู่ระบบเพื่อเริ่มต้นใช้งาน");
        subtitle.setFont(AppFont.thai(Font.PLAIN, 22));
        subtitle.setForeground(Color.WHITE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.setPreferredSize(new Dimension(200,200));
        header.add(title);
        header.add(subtitle);

        // Fields ทำให้ว่างตอนพิม
        usernameField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) paintHint(g, this, "username");
            }
        };
        passwordField = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0) paintHint(g, this, "password");
            }
        };

        for (JTextField f : new JTextField[]{usernameField, passwordField}) {
            f.setFont(AppFont.thai(Font.PLAIN, 24));
            f.setBackground(Color.WHITE);
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            f.setPreferredSize(new Dimension(600, 62));
            f.setMaximumSize(new Dimension(600, 62));
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(fieldBorder, 5),
                    new EmptyBorder(0, 20, 0, 20)));
        }

        // text เฉยๆ
        Font labelFont = AppFont.thai(Font.BOLD, 24);
        JLabel userLabel = new JLabel("ชื่อผู้ใช้งาน");
        userLabel.setFont(labelFont);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel passLabel = new JLabel("รหัสผ่าน");
        passLabel.setFont(labelFont);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ติ๊กเพื่อแสดง/ซ่อนรหัสผ่าน
    JCheckBox showPass = new JCheckBox("แสดงรหัสผ่าน");
    showPass.setFont(AppFont.thai(Font.PLAIN, 18));
    showPass.setOpaque(false);
    showPass.setFocusPainted(false);
    showPass.setAlignmentX(Component.LEFT_ALIGNMENT);
    showPass.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    final char defaultEcho = passwordField.getEchoChar();
    showPass.addActionListener(e ->
    passwordField.setEchoChar(showPass.isSelected() ? (char) 0 : defaultEcho));

        // ปุ่มเข้าสู่ระบบ
        JButton loginButton = new JButton("เข้าสู่ระบบ");
        loginButton.setFont(AppFont.thai(Font.BOLD, 26));
        loginButton.setForeground(Color.white);
        loginButton.setBackground(buttonBg);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(500, 65));
        loginButton.addActionListener(e -> login());
        getRootPane().setDefaultButton(loginButton); // Enter key logs in

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(600, 65));
        buttonPanel.add(loginButton);

        // panel ของ username password เข้าสู่ระบบ
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        Dimension formSize = new Dimension(600, 420);
        form.setPreferredSize(formSize);
        form.setMinimumSize(formSize);
        form.setMaximumSize(formSize);

        form.add(userLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(25));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(10)); //เว้นระยะของช่องใต้รหัสผ่าน
        form.add(showPass);   //เช็คบ้อค                          
        form.add(Box.createVerticalStrut(30)); // ลดขนาดให้พอดีกับความสูงของฟอร์ม
        form.add(buttonPanel);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(bodyBg);
        body.add(form);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    /**
     * กดปุ่มเข้าสู่ระบบ: เช็ค users.csv (มีแค่ admin/user 2 คน)
     * ถูก -> เปิด MainFrame แล้วปิดหน้านี้, ผิด -> แจ้งเตือน
     */
    public boolean login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "กรุณากรอกชื่อผู้ใช้งานและรหัสผ่าน",
                "ข้อมูลไม่ครบ", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        model.User user = authenticate(username, password);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "ชื่อผู้ใช้งานหรือรหัสผ่านไม่ถูกต้อง",
                "เข้าสู่ระบบไม่สำเร็จ", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // สำเร็จ -> ไป MainFrame
        MainFrame main = new MainFrame(user);
        main.setVisible(true);
        dispose();
        return true;
    }

    /** เช็คกับไฟล์ data/users.csv คืน User ถ้าตรง, ไม่ตรงคืน null */
    public model.User authenticate(String username, String password) {
        try {
            service.FileStorageService fs = new service.FileStorageService();
            return fs.authenticate(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean authenticate() {
        return login();
    }

    // hint ทำให้ข้อความจาง
    private static void paintHint(Graphics g, JTextField field, String hint) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.setFont(field.getFont());
        FontMetrics fm = g2.getFontMetrics();
        Insets in = field.getInsets();
        int y = (field.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(hint, in.left, y);
        g2.dispose();
    }

    public static void main(String[] args) {
        // ตั้งฟอนต์ไทยเป็น default ก่อนสร้างจอ กันตัวหนังสือเป็น □□□
        AppFont.applyGlobalDefault();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}