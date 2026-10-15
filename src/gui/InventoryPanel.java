package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Product;
import service.FileStorageService;

/**
 * InventoryPanel - หน้าคลังสินค้า (ตามรูปที่ 2)
 * stats + search + table + เพิ่ม/แก้ไข/ปรับสต็อก/ลบ/รีเฟรช
 * popup ทั้ง 4 แบบตามดีไซน์ + ตอนลบเลือกสินค้าได้ใน dialog
 * เก็บลง products.csv ใช้ร่วมกับ PosPanel
 */
public class InventoryPanel extends JPanel {

    private static final Color HEADER_BG = new Color(0x733D3D);
    private static final Color CONTENT_BG = new Color(0xE0D3D3);
    private static final Color CARD_BG = new Color(0x6B3434);
    private static final Color BTN_RED = new Color(0xA04040);

    // โทนสี popup ตามแบบ
    private static final Color POPUP_HEADER_BG = new Color(0xD9D9D9);
    private static final Color POPUP_BODY_BG = new Color(0xF5F5F5);
    private static final Color FIELD_BG = new Color(0xD8CDCD);
    private static final Color POPUP_BTN_BG = new Color(0xC9BCBE);

    private List<Product> products = new ArrayList<>();
    private FileStorageService storage = new FileStorageService();

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private String searchText = "";

    // stat labels
    private JLabel lblTotal;
    private JLabel lblLow;
    private JLabel lblOut;
    private JLabel lblValue;

    public static final String[] CATEGORIES = {PosPanel.MEAT, "ลูกชิ้น", "ผักต่างๆ"};

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(CONTENT_BG);
        buildUI();
        loadProducts();
    }

    private static Font font(int style, int size) { return AppFont.thai(style, size); }

    private void buildUI() {
        add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(CONTENT_BG);
        body.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        body.add(createStatsRow(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(createToolbar(), BorderLayout.NORTH);
        center.add(createTableScroll(), BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 75));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel t = new JLabel("คลังสินค้า");
        t.setFont(font(Font.BOLD, 26));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel("จัดการสินค้าและสต็อก");
        s.setFont(font(Font.PLAIN, 14));
        s.setForeground(Color.WHITE);
        left.add(t);
        left.add(s);

        StyledButton btnAdd = new StyledButton("+เพิ่มสินค้าใหม่", new Color(0x7A7A7A), 6);
        btnAdd.setPreferredSize(new Dimension(150, 40));
        btnAdd.addActionListener(e -> showAddDialog());

        header.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(btnAdd);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        lblTotal = new JLabel("0");
        lblLow = new JLabel("0");
        lblOut = new JLabel("0");
        lblValue = new JLabel("฿ 0");

        row.add(statCard("สินค้าทั้งหมด", lblTotal, Color.WHITE));
        row.add(statCard("สต็อกใกล้หมด", lblLow, Color.WHITE));
        row.add(statCard("สินค้าหมดสต็อก", lblOut, Color.RED));
        row.add(statCard("มูลค่าสินค้าในสต็อกรวม", lblValue, Color.WHITE));
        return row;
    }

    private JPanel statCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(12, 8, 12, 8));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 15));
        t.setForeground(Color.WHITE);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setFont(font(Font.BOLD, 28));
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(t);
        card.add(valueLabel);
        return card;
    }

    private JPanel createToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        txtSearch = new JTextField("ค้นหาชื่อหรือรหัสสินค้า.....");
        txtSearch.setFont(font(Font.PLAIN, 13));
        txtSearch.setForeground(Color.GRAY);
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().equals("ค้นหาชื่อหรือรหัสสินค้า.....")) {
                    txtSearch.setText("");
                    txtSearch.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setText("ค้นหาชื่อหรือรหัสสินค้า.....");
                    txtSearch.setForeground(Color.GRAY);
                }
            }
        });
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                String t = txtSearch.getText().trim();
                if (t.equals("ค้นหาชื่อหรือรหัสสินค้า.....")) t = "";
                searchText = t;
                refreshTable();
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        StyledButton bStock = new StyledButton("ปรับปรุงสต็อก", BTN_RED, 6);
        StyledButton bEdit = new StyledButton("แก้ไขสินค้า", BTN_RED, 6);
        StyledButton bRefresh = new StyledButton("รีเฟรช", BTN_RED, 6);
        StyledButton bDel = new StyledButton("ลบสินค้า", BTN_RED, 6);
        bStock.addActionListener(e -> restockSelected());
        bEdit.addActionListener(e -> editSelected());
        bRefresh.addActionListener(e -> loadProducts());
        bDel.addActionListener(e -> showDeleteDialog());
        btns.add(bStock);
        btns.add(bEdit);
        btns.add(bRefresh);
        btns.add(bDel);

        bar.add(txtSearch, BorderLayout.CENTER);
        bar.add(btns, BorderLayout.EAST);
        bar.setBorder(new EmptyBorder(4, 0, 4, 0));
        return bar;
    }

    private JScrollPane createTableScroll() {
        String[] cols = {"เมนู", "รหัสสินค้า", "หมวดหมู่", "ราคาขาย", "สต็อกคงเหลือ", "สถานะ"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(font(Font.PLAIN, 14));
        table.setRowHeight(30);
        table.getTableHeader().setFont(font(Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(0xD8B4B4));
        table.getTableHeader().setOpaque(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        // จัดกลาง + สีสถานะ
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(center);

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                String s = String.valueOf(v);
                if (s.contains("หมด")) setForeground(Color.RED);
                else if (s.contains("ใกล้")) setForeground(new Color(0xE08000));
                else setForeground(new Color(0x00AA00));
                setFont(font(Font.BOLD, 13));
                if (sel) { setBackground(t.getSelectionBackground()); }
                else setBackground(Color.WHITE);
                return comp;
            }
        });
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                try {
                    int stock = Integer.parseInt(String.valueOf(v));
                    setForeground(stock <= 5 ? Color.RED : Color.BLACK);
                } catch (Exception e) { setForeground(Color.BLACK); }
                setFont(font(Font.BOLD, 13));
                if (sel) setBackground(t.getSelectionBackground());
                else setBackground(Color.WHITE);
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // ---------------- data ----------------
    public void loadProducts() {
        products = storage.loadProducts();
        refreshTable();
        updateStats();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Product p : products) {
            String q = searchText.toLowerCase();
            if (!q.isEmpty()
                && !p.getName().toLowerCase().contains(q)
                && !p.getId().toLowerCase().contains(q)) continue;
            String status = p.getStock() <= 0 ? "หมดสต็อก" : (p.getStock() <= 5 ? "ใกล้หมด" : "พร้อมขาย");
            if (p.getStock() > 5) status = "พร้อมขาย";
            tableModel.addRow(new Object[]{
                p.getName(), p.getId(), p.getCategory(),
                fmtPrice(p.getPrice()), p.getStock(), status
            });
        }
    }

    private void updateStats() {
        int total = products.size();
        int low = 0, out = 0;
        double value = 0;
        for (Product p : products) {
            if (p.getStock() <= 0) out++;
            else if (p.getStock() <= 5) low++;
            value += p.getPrice() * p.getStock();
        }
        lblTotal.setText(String.valueOf(total));
        lblLow.setText(String.valueOf(low));
        lblOut.setText(String.valueOf(out));
        lblValue.setText("฿ " + String.format("%,.0f", value));
    }

    public void addProduct(Product product) {
        if (product == null) return;
        storage.saveProduct(product);
        loadProducts();
    }

    public void updateProduct(Product product) {
        if (product == null) return;
        storage.saveProduct(product);
        loadProducts();
    }

    public void deleteProduct(String productId) {
        storage.deleteProduct(productId);
        loadProducts();
    }

    private Product selectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        String id = String.valueOf(tableModel.getValueAt(row, 1));
        for (Product p : products) if (p.getId().equals(id)) return p;
        return null;
    }

    private int selectedIndex() {
        Product p = selectedProduct();
        if (p == null) return -1;
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId().equals(p.getId())) return i;
        }
        return -1;
    }

    private void editSelected() {
        Product p = selectedProduct();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "กรุณาเลือกสินค้าในตารางที่ต้องการแก้ไข");
            return;
        }
        showEditDialog(p);
    }

    private void restockSelected() {
        Product p = selectedProduct();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "กรุณาเลือกสินค้าในตารางที่ต้องการปรับสต็อก");
            return;
        }
        showRestockDialog(p);
    }

    // ================= popup style helpers (ตามแบบ) =================

    /** สร้าง dialog เปล่าพร้อมแถบหัวข้อสีเทาตามแบบ คืน dialog (body ต้อง add เองที่ CENTER) */
    private JDialog createPopup(String title, int w, int h, JLabel[] titleOut) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, title, true);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        header.setBackground(POPUP_HEADER_BG);
        header.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 20));
        header.add(t);
        dlg.add(header, BorderLayout.NORTH);
        if (titleOut != null && titleOut.length > 0) titleOut[0] = t;
        return dlg;
    }

    private JPanel popupBody() {
        JPanel body = new JPanel();
        body.setBackground(POPUP_BODY_BG);
        body.setBorder(new EmptyBorder(18, 28, 8, 28));
        return body;
    }

    private JLabel popupLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 14));
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private JTextField popupField() {
        return popupField("");
    }

    private JTextField popupField(String text) {
        JTextField f = new JTextField(text);
        f.setFont(font(Font.PLAIN, 14));
        f.setBackground(FIELD_BG);
        f.setBorder(BorderFactory.createLineBorder(new Color(0xB9A9A9)));
        return f;
    }

    private <T> JComboBox<T> popupCombo(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(font(Font.PLAIN, 14));
        cb.setBackground(FIELD_BG);
        return cb;
    }

    private JButton popupButton(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 13));
        b.setBackground(POPUP_BTN_BG);
        b.setPreferredSize(new Dimension(110, 30));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel popupButtons(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 0, 14, 0));
        for (JButton b : btns) p.add(b);
        return p;
    }

    private static String displayName(Product p) {
        return p.getName() + " (" + p.getId() + ")";
    }

    // ================= 1. เพิ่มสินค้า =================
    private void showAddDialog() {
        JDialog dlg = createPopup("เพิ่มสินค้า", 500, 400, null);
        JPanel body = popupBody();
        body.setLayout(new GridLayout(5, 2, 12, 14));

        // รหัสเมนูเป็น dropdown (พิมพ์รหัสเองได้) ตามแบบ
        JComboBox<String> cbId = popupCombo(freeIds(30));
        cbId.setEditable(true);
        cbId.setSelectedItem(nextId());

        JTextField txtName = popupField();
        JComboBox<String> cbCat = popupCombo(CATEGORIES);
        JTextField txtPrice = popupField();
        JTextField txtStock = popupField();

        body.add(popupLabel("รหัสเมนู :"));
        body.add(cbId);
        body.add(popupLabel("ชื่อเมนู :"));
        body.add(txtName);
        body.add(popupLabel("หมวดหมู่ :"));
        body.add(cbCat);
        body.add(popupLabel("ราคาขาย (บาท) :"));
        body.add(txtPrice);
        body.add(popupLabel("จำนวนสต็อก :"));
        body.add(txtStock);

        JButton btnCancel = popupButton("Cancel");
        JButton btnOk = popupButton("OK");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            String id = String.valueOf(cbId.getSelectedItem() == null ? "" : cbId.getSelectedItem()).trim();
            String name = txtName.getText().trim();
            String cat = String.valueOf(cbCat.getSelectedItem());
            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "กรุณากรอกรหัสเมนูและชื่อเมนู");
                return;
            }
            double price;
            int stock;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
                stock = Integer.parseInt(txtStock.getText().trim());
                if (Double.isNaN(price) || Double.isInfinite(price) || price < 0 || stock < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "ราคาขายหรือจำนวนสต็อกไม่ถูกต้อง");
                return;
            }
            for (Product p : products) {
                if (p.getId().equals(id)) {
                    JOptionPane.showMessageDialog(dlg, "รหัสเมนูซ้ำ กรุณาใช้รหัสอื่น");
                    return;
                }
            }
            addProduct(new Product(id, name, cat, price, stock));
            dlg.dispose();
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(POPUP_BODY_BG);
        wrap.add(body, BorderLayout.CENTER);
        wrap.add(popupButtons(btnCancel, btnOk), BorderLayout.SOUTH);

        dlg.add(wrap, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // ================= 2. แก้ไขข้อมูลสินค้า =================
    private void showEditDialog(Product p) {
        JLabel[] titleOut = new JLabel[1];
        JDialog dlg = createPopup("แก้ไขข้อมูลสินค้า " + p.getId(), 500, 320, titleOut);

        JPanel body = popupBody();
        body.setLayout(new GridLayout(3, 2, 12, 16));

        JTextField txtName = popupField(p.getName());
        JComboBox<String> cbCat = popupCombo(categoriesWith(p.getCategory()));
        cbCat.setSelectedItem(p.getCategory().isEmpty() ? CATEGORIES[0] : p.getCategory());
        JTextField txtPrice = popupField(fmtPrice(p.getPrice()));

        body.add(popupLabel("ชื่อเมนู :"));
        body.add(txtName);
        body.add(popupLabel("หมวดหมู่ :"));
        body.add(cbCat);
        body.add(popupLabel("ราคาขาย (บาท) :"));
        body.add(txtPrice);

        JButton btnCancel = popupButton("Cancel");
        JButton btnOk = popupButton("OK");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "กรุณากรอกชื่อเมนู");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
                if (Double.isNaN(price) || Double.isInfinite(price) || price < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "ราคาขายไม่ถูกต้อง");
                return;
            }
            p.setName(name);
            p.setCategory(String.valueOf(cbCat.getSelectedItem()));
            p.setPrice(price);
            updateProduct(p);
            dlg.dispose();
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(POPUP_BODY_BG);
        wrap.add(body, BorderLayout.CENTER);
        wrap.add(popupButtons(btnCancel, btnOk), BorderLayout.SOUTH);

        dlg.add(wrap, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // ================= 3. ยืนยันการลบสินค้า (เลือกได้ว่าลบอะไร) =================
    private void showDeleteDialog() {
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ยังไม่มีสินค้าให้ลบ");
            return;
        }
        JDialog dlg = createPopup("ยืนยันการลบสินค้า", 500, 300, null);
        dlg.setLayout(new BorderLayout());

        JPanel body = popupBody();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // ตัวเลือกว่าลบอะไร
        JPanel pickRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        pickRow.setOpaque(false);
        JLabel pickLbl = new JLabel("เลือกสินค้าที่ต้องการลบ :");
        pickLbl.setFont(font(Font.BOLD, 14));
        String[] items = new String[products.size()];
        for (int i = 0; i < products.size(); i++) items[i] = displayName(products.get(i));
        JComboBox<String> cbPick = popupCombo(items);
        int sel = selectedIndex();
        cbPick.setSelectedIndex(sel >= 0 ? sel : 0);
        pickRow.add(pickLbl);
        pickRow.add(cbPick);

        JLabel confirm = new JLabel("", SwingConstants.CENTER);
        confirm.setFont(font(Font.BOLD, 15));
        confirm.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirm.setBorder(new EmptyBorder(22, 0, 6, 0));

        Runnable refreshText = () -> {
            Product cur = products.get(cbPick.getSelectedIndex());
            confirm.setText("ต้องการลบสินค้า " + cur.getName() + " " + cur.getId() + " ใช่หรือไม่ ??");
        };
        cbPick.addActionListener(e -> refreshText.run());
        refreshText.run();

        body.add(pickRow);
        body.add(confirm);

        JButton btnYes = popupButton("Yes");
        JButton btnNo = popupButton("No");
        btnNo.addActionListener(e -> dlg.dispose());
        btnYes.addActionListener(e -> {
            Product cur = products.get(cbPick.getSelectedIndex());
            deleteProduct(cur.getId());
            dlg.dispose();
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(POPUP_BODY_BG);
        wrap.add(body, BorderLayout.CENTER);
        wrap.add(popupButtons(btnYes, btnNo), BorderLayout.SOUTH);

        // createPopup ใส่ header ไว้ที่ NORTH แล้ว ต้อง add wrap ที่ CENTER
        dlg.add(wrap, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // ================= 4. ปรับปรุงสต็อก (เพิ่ม / ลด) =================
    private static final String ADJ_ADD = "เพิ่มสต็อก (+)";
    private static final String ADJ_REDUCE = "ลดสต็อก (-)";

    private void showRestockDialog(Product p) {
        JDialog dlg = createPopup("ปรับปรุงสต็อก", 520, 400, null);

        JPanel body = popupBody();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel menuLbl = new JLabel("เมนู : " + p.getName() + " (" + p.getId() + ")");
        menuLbl.setFont(font(Font.BOLD, 18));
        menuLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stockLbl = new JLabel("สต็อกปัจจุบัน : " + p.getStock() + " ชิ้น");
        stockLbl.setFont(font(Font.BOLD, 18));
        stockLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // แถว dropdown เลือกประเภทการปรับ
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        typeRow.setOpaque(false);
        typeRow.setBorder(new EmptyBorder(18, 0, 0, 0));
        JLabel typeLbl = new JLabel("ประเภทการปรับ :");
        typeLbl.setFont(font(Font.BOLD, 14));
        JComboBox<String> cbType = popupCombo(new String[]{ADJ_ADD, ADJ_REDUCE});
        cbType.setPreferredSize(new Dimension(170, 28));
        typeRow.add(typeLbl);
        typeRow.add(cbType);

        // แถวกรอกจำนวน
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        inputRow.setOpaque(false);
        inputRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        JLabel inLbl = new JLabel("ระบุจำนวนที่ต้องการเพิ่ม (ชิ้น) :");
        inLbl.setFont(font(Font.BOLD, 14));
        JTextField txtQty = popupField();
        txtQty.setPreferredSize(new Dimension(150, 26));
        inputRow.add(inLbl);
        inputRow.add(txtQty);

        // แสดงสต็อกหลังปรับแบบสด
        JLabel previewLbl = new JLabel(" ");
        previewLbl.setFont(font(Font.BOLD, 15));
        previewLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewLbl.setBorder(new EmptyBorder(14, 0, 0, 0));

        Runnable refresh = () -> {
            boolean reduce = ADJ_REDUCE.equals(cbType.getSelectedItem());
            inLbl.setText(reduce ? "ระบุจำนวนที่ต้องการลด (ชิ้น) :" : "ระบุจำนวนที่ต้องการเพิ่ม (ชิ้น) :");
            try {
                int q = Integer.parseInt(txtQty.getText().trim());
                if (q <= 0) throw new NumberFormatException();
                long after = reduce ? (long) p.getStock() - q : (long) p.getStock() + q;
                if (after < 0) {
                    previewLbl.setForeground(new Color(0xC62828));
                    previewLbl.setText("ลดได้ไม่เกิน " + p.getStock() + " ชิ้น");
                } else {
                    previewLbl.setForeground(Color.BLACK);
                    previewLbl.setText("สต็อกหลังปรับ : " + after + " ชิ้น");
                }
            } catch (NumberFormatException ex) {
                previewLbl.setText(" ");
            }
        };
        cbType.addActionListener(e -> refresh.run());
        txtQty.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
        });

        body.add(menuLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(stockLbl);
        body.add(typeRow);
        body.add(inputRow);
        body.add(previewLbl);

        JButton btnCancel = popupButton("Cancel");
        JButton btnOk = popupButton("OK");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            boolean reduce = ADJ_REDUCE.equals(cbType.getSelectedItem());
            int qty;
            try {
                qty = Integer.parseInt(txtQty.getText().trim());
                if (qty <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "กรุณากรอกจำนวนเต็มมากกว่า 0");
                return;
            }
            if (reduce) {
                if (qty > p.getStock()) {
                    JOptionPane.showMessageDialog(dlg,
                        "สต็อกปัจจุบันมีเพียง " + p.getStock() + " ชิ้น ลดได้ไม่เกินจำนวนนี้");
                    return;
                }
                p.reduceStock(qty);
            } else {
                if ((long) p.getStock() + qty > Integer.MAX_VALUE) {
                    JOptionPane.showMessageDialog(dlg, "จำนวนสต็อกมากเกินไป");
                    return;
                }
                p.addStock(qty);
            }
            updateProduct(p);
            dlg.dispose();
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(POPUP_BODY_BG);
        wrap.add(body, BorderLayout.CENTER);
        wrap.add(popupButtons(btnCancel, btnOk), BorderLayout.SOUTH);

        dlg.add(wrap, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    // ================= helpers =================
    /** แสดงราคา: เป็นจำนวนเต็มไม่มีทศนิยม แต่ถ้ามีสตางค์ให้โชว์ 2 ตำแหน่ง (เดิมใช้ (int) ทำให้ 12.5 กลายเป็น 12) */
    private static String fmtPrice(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.2f", v);
    }

    /** รายการหมวดหมู่ใน dropdown: ถ้าสินค้ามีหมวดที่ไม่อยู่ในลิสต์ ให้เติมเข้าไป กันถูกเปลี่ยนหมวดเงียบๆ ตอนกด OK */
    private static String[] categoriesWith(String current) {
        if (current == null || current.isEmpty()) return CATEGORIES;
        for (String c : CATEGORIES) if (c.equals(current)) return CATEGORIES;
        String[] out = new String[CATEGORIES.length + 1];
        out[0] = current;
        System.arraycopy(CATEGORIES, 0, out, 1, CATEGORIES.length);
        return out;
    }

    private String nextId() {
        int max = 0;
        for (Product p : products) {
            try {
                String num = p.getId().replaceAll("[^0-9]", "");
                max = Math.max(max, Integer.parseInt(num));
            } catch (Exception ignored) {}
        }
        return String.format("M%03d", max + 1);
    }

    /** รหัสว่างถัดไป n ตัว สำหรับ dropdown รหัสเมนู */
    private String[] freeIds(int n) {
        List<String> out = new ArrayList<>();
        int i = 1;
        while (out.size() < n && i < 999) {
            String id = String.format("M%03d", i);
            boolean used = false;
            for (Product p : products) {
                if (p.getId().equals(id)) { used = true; break; }
            }
            if (!used) out.add(id);
            i++;
        }
        if (out.isEmpty()) out.add(nextId());
        return out.toArray(new String[0]);
    }

    // --- getters ---
    public List<Product> getProducts() { return products; }
}
