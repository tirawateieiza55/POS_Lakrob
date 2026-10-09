package gui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.User;

/**
 * MainFrame - เฟรมหลักหลัง login
 * ซ้าย: sidebar เลือก 3 หน้า (สลับ tab ด้วย CardLayout ได้ตลอด)
 * ขวา: PosPanel / InventoryPanel / HistoryPanel
 */
public class MainFrame extends JFrame {

    public static final int DESIGN_WIDTH = 1200, DESIGN_HEIGHT = 800;

    private static final Color SIDEBAR_BG = new Color(0x8F3E3E);
    private static final Color SIDEBAR_BTN = new Color(0x5E2B2B);
    private static final Color SIDEBAR_ACTIVE = new Color(0x4A1F1F);

    private static final String PAGE_POS = "POS";
    private static final String PAGE_STOCK = "STOCK";
    private static final String PAGE_HISTORY = "HISTORY";

    private User currentUser;
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private PosPanel posPanel;
    private InventoryPanel inventoryPanel;
    private HistoryPanel historyPanel;

    private List<StyledButton> navButtons = new ArrayList<>();
    private JLabel lblUserInfo;

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("ระบบออเดอร์หน้าร้าน - " + (user == null ? "" : user.getUsername()));
        setSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        posPanel = new PosPanel(user);
        inventoryPanel = new InventoryPanel();
        historyPanel = new HistoryPanel();

        cardPanel.add(posPanel, PAGE_POS);
        cardPanel.add(inventoryPanel, PAGE_STOCK);
        cardPanel.add(historyPanel, PAGE_HISTORY);

        add(createSidebar(), BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);

        showPage(PAGE_POS);
    }

    private static Font font(int style, int size) { return AppFont.thai(style, size); }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, DESIGN_HEIGHT));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JLabel t1 = new JLabel("ระบบออเดอร์หน้าร้าน");
        t1.setFont(font(Font.BOLD, 17));
        t1.setForeground(Color.WHITE);
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t2 = new JLabel("ระบบจัดการเมนูและสต็อกสินค้า");
        t2.setFont(font(Font.PLAIN, 11));
        t2.setForeground(Color.WHITE);
        t2.setAlignmentX(Component.CENTER_ALIGNMENT);

        top.add(t1);
        top.add(t2);
        top.add(Box.createVerticalStrut(20));

        String[] items = {"เมนูหน้าร้าน", "การจัดการสินค้าและสต็อก", "ประวัติการขายและรายงาน"};
        String[] pages = {PAGE_POS, PAGE_STOCK, PAGE_HISTORY};
        navButtons.clear();
        for (int i = 0; i < items.length; i++) {
            final String page = pages[i];
            StyledButton b = new StyledButton(items[i], SIDEBAR_BTN, 5);
            b.setFont(font(Font.BOLD, 13));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setPreferredSize(new Dimension(200, 46));
            b.setMaximumSize(new Dimension(200, 46));
            b.addActionListener(e -> showPage(page));
            navButtons.add(b);
            top.add(b);
            top.add(Box.createVerticalStrut(8));
        }

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);

        String username = currentUser == null ? "user" : currentUser.getUsername();
        String role = currentUser == null ? "พนักงานขาย" : currentUser.getDisplayRole();
        String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        lblUserInfo = new JLabel("<html>ผู้ใช้งาน : <b>" + username + "</b><br>สิทธิ์ : " + role + "<br>" + datetime + "</html>");
        lblUserInfo.setFont(font(Font.PLAIN, 12));
        lblUserInfo.setForeground(Color.WHITE);
        lblUserInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        bottom.add(lblUserInfo);
        bottom.add(Box.createVerticalStrut(12));
        StyledButton logout = new StyledButton("ออกจากระบบ", new Color(0xB46A6A), 5);
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setPreferredSize(new Dimension(200, 40));
        logout.setMaximumSize(new Dimension(200, 40));
        logout.addActionListener(e -> logout());
        bottom.add(logout);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    /** สลับหน้า + รีโหลดข้อมูลให้สด (เลือกอะไรไปก็เปลี่ยน tab ได้ตลอด) */
    public void showPage(String page) {
        cardLayout.show(cardPanel, page);
        for (int i = 0; i < navButtons.size(); i++) {
            boolean active =
                (page.equals(PAGE_POS) && i == 0)
                || (page.equals(PAGE_STOCK) && i == 1)
                || (page.equals(PAGE_HISTORY) && i == 2);
            navButtons.get(i).setBaseColor(active ? SIDEBAR_ACTIVE : SIDEBAR_BTN);
        }
        // refresh ข้อมูลทุกครั้งที่สลับหน้า
        if (page.equals(PAGE_POS)) posPanel.refreshProducts();
        if (page.equals(PAGE_STOCK)) inventoryPanel.loadProducts();
        if (page.equals(PAGE_HISTORY)) historyPanel.loadHistory();
    }

    private void logout() {
        int ok = JOptionPane.showConfirmDialog(this, "ออกจากระบบ?", "ยืนยัน", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    public User getCurrentUser() { return currentUser; }

    public static void main(String[] args) {
        AppFont.applyGlobalDefault();
        SwingUtilities.invokeLater(() -> {
            // สำหรับเทสลวดลายโดยไม่ต้อง login
            User test = new User("02", "user", "user123", "พนักงานขาย");
            new MainFrame(test).setVisible(true);
        });
    }
}
