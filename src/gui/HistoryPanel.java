package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import model.SaleOrder;
import model.User;
import service.FileStorageService;

/**
 * HistoryPanel - ประวัติการขาย + รายงานยอด (อ่านจาก sales.csv + sale_items.csv)
 * ------------------------------------------------------------------
 * ตาราง: เลขบิล / วันเวลา / ยอดรวม / สถานะ / ปุ่มยกเลิก (กากบาท)
 * แถบล่าง: จำนวนบิล + ยอดรวม (ไม่นับบิล cancelled)
 * ยกเลิกบิล: cancelOrder() -> เปลี่ยนสถานะเป็น cancelled + คืนสต็อก (บิลยังอยู่)
 *
 * จะแก้ครั้งหน้า:
 * - เพิ่มกรองตามวัน/ค้นหาบิล: แก้ loadHistory() (ตอนนี้โชว์ทั้งหมด)
 * - ดูรายละเอียดบิล: ใช้ viewDetail() (ตอนนี้โหลด items ครบแล้วจาก sale_items.csv)
 * - เปลี่ยนสี/คอลัมน์: แก้ cols + renderer ใน constructor
 */
public class HistoryPanel extends JPanel {

    private static final Color HEADER_BG = new Color(0x733D3D);
    private static final Color CONTENT_BG = new Color(0xE0D3D3);

    private static final int COL_ID = 0;
    private static final int COL_STATUS = 3;
    private static final int COL_DELETE = 4;
    private static final String DELETE_MARK = "✕";

    private List<SaleOrder> sales;
    private User currentUser;
    // ใช้ singleton ตัวเดียวทั้งแอป (ห้าม new เอง)
    private FileStorageService storage = FileStorageService.getInstance();
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;
    private JLabel totalLabel;

    public HistoryPanel() {
        this(null);
    }

    public HistoryPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBackground(CONTENT_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 75));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel t = new JLabel("ประวัติการขายและรายงาน");
        t.setFont(AppFont.thai(Font.BOLD, 26));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel("ร่างไว้ก่อน - ดูประวัติย้อนหลังแบบง่าย");
        s.setFont(AppFont.thai(Font.PLAIN, 13));
        s.setForeground(Color.WHITE);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(t);
        left.add(s);
        header.add(left, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"เลขที่บิล", "วันเวลา", "ยอดรวม", "สถานะ", "ยกเลิก"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(AppFont.thai(Font.PLAIN, 14));
        table.setRowHeight(28);

        // บิลที่ยกเลิกแล้วแสดงเป็นสีเทา (ส่วนคอลัมน์ปุ่มมี renderer ของตัวเองด้านล่าง)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(tb, value, isSelected, hasFocus, row, column);
                boolean cancelled = FileStorageService.STATUS_CANCELLED
                        .equalsIgnoreCase(String.valueOf(tb.getModel().getValueAt(row, COL_STATUS)));
                if (!isSelected) setForeground(cancelled ? Color.GRAY : Color.BLACK);
                return this;
            }
        });

        // คอลัมน์ปุ่มกากบาท
        table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(70);
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(tb, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(AppFont.thai(Font.BOLD, 18));
                setForeground(new Color(0xC62828));
                return this;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != COL_DELETE) return;
                if (!DELETE_MARK.equals(tableModel.getValueAt(row, COL_DELETE))) return;
                cancelOrder(tableModel.getValueAt(row, COL_ID).toString());
            }
        });
        // เปลี่ยนเมาส์เป็นรูปมือเมื่อชี้ที่ปุ่ม
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                boolean onButton = row >= 0 && col == COL_DELETE
                        && DELETE_MARK.equals(tableModel.getValueAt(row, COL_DELETE));
                table.setCursor(onButton ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                         : Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // ===== แถบสรุปยอดรวมด้านล่าง =====
        JPanel summary = new JPanel(new BorderLayout());
        summary.setBackground(HEADER_BG);
        summary.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        countLabel = new JLabel("จำนวนบิลทั้งหมด: 0 บิล");
        countLabel.setFont(AppFont.thai(Font.PLAIN, 16));
        countLabel.setForeground(Color.WHITE);

        totalLabel = new JLabel("ยอดรวมทั้งหมด: 0.00 บาท");
        totalLabel.setFont(AppFont.thai(Font.BOLD, 20));
        totalLabel.setForeground(Color.WHITE);

        summary.add(countLabel, BorderLayout.WEST);
        summary.add(totalLabel, BorderLayout.EAST);
        add(summary, BorderLayout.SOUTH);

        loadHistory();
    }

    public void loadHistory() {
        sales = storage.loadSaleOrders();
        tableModel.setRowCount(0);

        double grandTotal = 0;
        int cancelledCount = 0;
        for (SaleOrder o : sales) {
            boolean cancelled = FileStorageService.STATUS_CANCELLED.equalsIgnoreCase(o.getStatus());
            tableModel.addRow(new Object[]{
                o.getId(),
                o.getOrderDate() == null ? "" : o.getOrderDate().toString().replace("T", " "),
                String.format("%.2f", o.getTotalAmount()),
                o.getStatus(),
                cancelled ? "" : DELETE_MARK   // ยกเลิกไปแล้วไม่ต้องมีปุ่ม
            });
            if (cancelled) cancelledCount++;
            else grandTotal += o.getTotalAmount(); // ยอดรวมไม่นับบิลที่ยกเลิก
        }
        if (sales.isEmpty()) {
            tableModel.addRow(new Object[]{"-", "ยังไม่มีข้อมูลการขาย", "-", "-", ""});
        }

        countLabel.setText("จำนวนบิลทั้งหมด: " + sales.size() + " บิล"
                + (cancelledCount > 0 ? " (ยกเลิก " + cancelledCount + " บิล)" : ""));
        totalLabel.setText(String.format("ยอดรวมทั้งหมด: %,.2f บาท", grandTotal));
    }

    /** ยกเลิกบิล: เปลี่ยนสถานะเป็น cancelled + คืนสต็อก (บิลยังอยู่ในประวัติ) */
    private void cancelOrder(String orderId) {
        int ok = JOptionPane.showConfirmDialog(this,
                "ต้องการยกเลิกบิล " + orderId + " และคืนสินค้าเข้าสต็อกใช่หรือไม่?\n"
                + "บิลจะยังอยู่ในประวัติ โดยแสดงสถานะ " + FileStorageService.STATUS_CANCELLED,
                "ยืนยันการยกเลิกบิล",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        int restored = storage.cancelSaleOrder(orderId);
        if (restored == -2) {
            JOptionPane.showMessageDialog(this, "บิลนี้ถูกยกเลิกไปแล้ว",
                    "แจ้งเตือน", JOptionPane.INFORMATION_MESSAGE);
            loadHistory();
            return;
        }
        if (restored < 0) {
            JOptionPane.showMessageDialog(this, "ยกเลิกบิลไม่สำเร็จ ไม่พบบิลหรือบันทึกไฟล์ไม่ได้",
                    "ผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (restored == 0) {
            JOptionPane.showMessageDialog(this,
                    "ยกเลิกบิลแล้ว แต่ไม่พบรายการสินค้าของบิลนี้ จึงไม่ได้คืนสต็อก\n"
                    + "(บิลที่ขายก่อนเพิ่มระบบเก็บรายการสินค้า)",
                    "แจ้งเตือน", JOptionPane.INFORMATION_MESSAGE);
        }
        loadHistory();
    }

    /** ดูบิลใบเดียวพร้อมรายการสินค้า (โหลดจาก sale_items.csv มาครบแล้ว) */
    public SaleOrder viewDetail(String orderId) {
        if (sales != null) {
            for (SaleOrder o : sales) if (o.getId().equals(orderId)) return o;
        }
        // fallback: โหลดจากไฟล์ตรง (SaleOrder ไม่มีเมธอดอ่านไฟล์เองแล้ว - ต้องผ่าน storage)
        return storage.loadSaleOrderById(orderId);
    }
    public List<SaleOrder> getSales() { return sales; }
}