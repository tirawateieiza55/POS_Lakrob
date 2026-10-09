package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import model.SaleOrder;
import service.FileStorageService;

/**
 * HistoryPanel - ร่างไว้ก่อน (แสดงประวัติจาก sales.csv)
 * เต็มๆ จะทำรอบถัดไป (กรองตามวัน + ดูรายละเอียด + รายงานยอด)
 */
public class HistoryPanel extends JPanel {

    private static final Color HEADER_BG = new Color(0x733D3D);
    private static final Color CONTENT_BG = new Color(0xE0D3D3);

    private static final int COL_ID = 0;
    private static final int COL_DELETE = 4;
    private static final String DELETE_MARK = "✕";

    private List<SaleOrder> sales;
    private FileStorageService storage = new FileStorageService();
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;
    private JLabel totalLabel;

    public HistoryPanel() {
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

        String[] cols = {"เลขที่บิล", "วันเวลา", "ยอดรวม", "สถานะ", "ลบ"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(AppFont.thai(Font.PLAIN, 14));
        table.setRowHeight(28);

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
                deleteOrder(tableModel.getValueAt(row, COL_ID).toString());
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
        for (SaleOrder o : sales) {
            tableModel.addRow(new Object[]{
                o.getId(),
                o.getOrderDate() == null ? "" : o.getOrderDate().toString().replace("T", " "),
                String.format("%.2f", o.getTotalAmount()),
                o.getStatus(),
                DELETE_MARK
            });
            grandTotal += o.getTotalAmount();
        }
        if (sales.isEmpty()) {
            tableModel.addRow(new Object[]{"-", "ยังไม่มีข้อมูลการขาย", "-", "-", ""});
        }

        countLabel.setText("จำนวนบิลทั้งหมด: " + sales.size() + " บิล");
        totalLabel.setText(String.format("ยอดรวมทั้งหมด: %,.2f บาท", grandTotal));
    }

    private void deleteOrder(String orderId) {
        int ok = JOptionPane.showConfirmDialog(this,
                "ต้องการลบบิล " + orderId + " และคืนสินค้าเข้าสต็อกใช่หรือไม่?\nการลบไม่สามารถย้อนกลับได้",
                "ยืนยันการลบบิล",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        int restored = storage.deleteSaleOrder(orderId);
        if (restored < 0) {
            JOptionPane.showMessageDialog(this, "ลบบิลไม่สำเร็จ ไม่พบบิลหรือบันทึกไฟล์ไม่ได้",
                    "ผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (restored == 0) {
            JOptionPane.showMessageDialog(this,
                    "ลบบิลแล้ว แต่ไม่พบรายการสินค้าของบิลนี้ จึงไม่ได้คืนสต็อก\n"
                    + "(บิลที่ขายก่อนเพิ่มระบบเก็บรายการสินค้า)",
                    "แจ้งเตือน", JOptionPane.INFORMATION_MESSAGE);
        }
        loadHistory();
    }

    public SaleOrder viewDetail(String orderId) {
        if (sales != null) {
            for (SaleOrder o : sales) if (o.getId().equals(orderId)) return o;
        }
        return SaleOrder.loadById(orderId);
    }
    public List<SaleOrder> getSales() { return sales; }
}