package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import model.CartItem;
import model.Product;
import model.SaleOrder;
import model.User;
import service.FileStorageService;

/**
 * PosPanel - หน้าขายหน้าร้าน (ตามรูปที่ 1)
 * ซ้าย: header + หมวดหมู่ + การ์ดสินค้า (โหลดจาก products.csv ว่างตอนแรก เพิ่มผ่านหน้าสต็อก)
 * ขวา: ตะกร้า + ส่วนลด + ชำระเงิน -> popup ชำระ -> ใบเสร็จ
 */
public class PosPanel extends JPanel {

    public static final String MEAT = "เนื้อ,หมู,ไก่,ทะเล";

    private static final Color HEADER_BG = new Color(0x733D3D);
    private static final Color CONTENT_BG = new Color(0xE0D3D3);
    private static final Color CART_BG = new Color(0xB09E9E);
    private static final Color CAT_BG = new Color(0x5E2B2B);
    private static final Color CAT_ACTIVE = new Color(0x8B1E1E);
    private static final Color BLUE = new Color(0x2D8CEB);
    private static final Color PAY_BG = new Color(0xA04040);

    private List<Product> products = new ArrayList<>();
    private List<Product> filtered = new ArrayList<>();
    private List<CartItem> cart = new ArrayList<>();
    private double totalAmount;
    private double discount;

    private User currentUser;
    private FileStorageService storage = new FileStorageService();

    private String activeCategory = "ทั้งหมด";
    private String searchText = "";

    // UI refs
    private JPanel gridPanel;
    private JPanel cartListPanel;
    private JLabel lblCartCount;
    private JLabel lblSubtotal;
    private JLabel lblNetTotal;
    private JTextField txtDiscount;
    private JTextField txtSearch;
    private List<StyledButton> catButtons = new ArrayList<>();

    public PosPanel() {
        this(null);
    }

    public PosPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBackground(CONTENT_BG);
        buildUI();
        refreshProducts();
    }

    public void setUser(User user) { this.currentUser = user; }

    /**
     * เลขบิลถัดไป = เลขสูงสุดที่มีอยู่ + 1
     * (เดิมใช้จำนวนบิล + 1 ซึ่งพอลบบิลกลางๆ แล้วเปิดโปรแกรมใหม่ เลขจะซ้ำกับบิลที่มีอยู่ -> บิลไม่ถูกบันทึกแต่สต็อกถูกตัด)
     */
    private String nextOrderId() {
        int max = 0;
        try {
            for (SaleOrder o : storage.loadSaleOrders()) {
                String num = o.getId() == null ? "" : o.getId().replaceAll("[^0-9]", "");
                if (num.isEmpty() || num.length() > 9) continue;
                max = Math.max(max, Integer.parseInt(num));
            }
        } catch (Exception ignored) {}
        return "Order " + (max + 1);
    }

    // ---------------- UI ----------------
    private void buildUI() {
        // center หลัก (header + menu)
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(CONTENT_BG);
        main.add(createHeader(), BorderLayout.NORTH);
        main.add(createMenuArea(), BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
        add(createCartPanel(), BorderLayout.EAST);
    }

    private static Font font(int style, int size) { return AppFont.thai(style, size); }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, 75));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("เมนูทั้งหมดในร้าน");
        title.setFont(font(Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        txtSearch = new JTextField(16);
        txtSearch.setFont(font(Font.PLAIN, 16));
        txtSearch.addActionListener(e -> {
            searchText = txtSearch.getText().trim();
            applyFilter();
        });
        // ค้นหาแบบพิมพ์แล้วกรองเลย
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() { searchText = txtSearch.getText().trim(); applyFilter(); }
        });

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        JLabel s = label("ค้นหาเมนู:", Font.BOLD, 18, Color.WHITE);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        search.add(s);
        search.add(txtSearch);

        header.add(title, BorderLayout.WEST);
        header.add(search, BorderLayout.EAST);
        return header;
    }

    private JPanel createMenuArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(CONTENT_BG);
        area.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel cats = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        cats.setOpaque(false);
        catButtons.clear();
        for (String c : new String[]{"ทั้งหมด", MEAT, "ลูกชิ้น", "ผักต่างๆ"}) {
            StyledButton b = new StyledButton(c, c.equals(activeCategory) ? CAT_ACTIVE : CAT_BG);
            b.setPreferredSize(new Dimension(140, 38));
            b.addActionListener(e -> {
                activeCategory = c;
                for (StyledButton x : catButtons) x.setBaseColor(x == b ? CAT_ACTIVE : CAT_BG);
                applyFilter();
            });
            catButtons.add(b);
            cats.add(b);
        }

        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        gridPanel.setOpaque(false);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        wrap.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        area.add(cats, BorderLayout.NORTH);
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private JPanel createProductCard(Product p) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

        StyledButton btnAdd = new StyledButton("+ เพิ่มลงในตะกร้า", BLUE, 6);
        btnAdd.setFont(font(Font.PLAIN, 12));
        btnAdd.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAdd.setMaximumSize(new Dimension(170, 26));
        boolean outOfStock = p.getStock() <= 0;
        btnAdd.setEnabled(!outOfStock);
        btnAdd.addActionListener(e -> addProduct(p, 1));

        card.add(label(p.getName(), Font.BOLD, 18, Color.BLACK));
        String cat = p.getCategory().isEmpty() ? "-" : p.getCategory();
        card.add(label(cat + " - รหัส " + p.getId(), Font.PLAIN, 11, Color.DARK_GRAY));
        card.add(Box.createVerticalStrut(4));
        card.add(label("฿" + fmtPrice(p.getPrice()), Font.BOLD, 20, Color.BLACK));
        card.add(Box.createVerticalStrut(4));
        card.add(btnAdd);
        card.add(Box.createVerticalStrut(2));
        Color stockColor = outOfStock ? Color.RED
                : (p.getStock() <= 5 ? new Color(0xE08000) : new Color(0x1B7F2B));
        String stockTxt = outOfStock ? "หมดสต็อก" : ("คงเหลือ " + p.getStock() + " ชิ้น");
        card.add(label(stockTxt, Font.PLAIN, 10, stockColor));
        return card;
    }

    private static String fmtPrice(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.2f", v);
    }

    // ---------------- Cart panel ----------------
    private JPanel createCartPanel() {
        JPanel cartPanel = new JPanel(new BorderLayout(0, 10));
        cartPanel.setBackground(CART_BG);
        cartPanel.setPreferredSize(new Dimension(300, 0));
        cartPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel t1 = label("รายการสั่งซื้อ", Font.BOLD, 15, Color.WHITE);
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCartCount = label("0 รายการ", Font.PLAIN, 11, Color.WHITE);
        lblCartCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(t1);
        left.add(lblCartCount);

        JButton btnClear = new JButton("ล้างตะกร้า");
        btnClear.setFont(font(Font.BOLD, 11));
        btnClear.setFocusPainted(false);
        btnClear.addActionListener(e -> clearCart());

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(left, BorderLayout.WEST);
        head.add(btnClear, BorderLayout.EAST);

        cartListPanel = new JPanel();
        cartListPanel.setLayout(new BoxLayout(cartListPanel, BoxLayout.Y_AXIS));
        cartListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(cartListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        // summary
        JPanel sum = new JPanel(new GridLayout(3, 2, 5, 8));
        sum.setBackground(new Color(0xF0EAEA));
        sum.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        txtDiscount = new JTextField("0");
        txtDiscount.setHorizontalAlignment(JTextField.RIGHT);
        txtDiscount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
        });

        lblSubtotal = summaryLabel("฿0.00", true);
        lblNetTotal = summaryLabel("฿0.00", true);

        sum.add(summaryLabel("รวมเป็นเงิน :", false));
        sum.add(lblSubtotal);
        sum.add(summaryLabel("ส่วนลด :", false));
        sum.add(txtDiscount);
        sum.add(summaryLabel("ยอดสุทธิ :", false));
        sum.add(lblNetTotal);

        StyledButton btnPay = new StyledButton("ชำระเงิน", PAY_BG, 10);
        btnPay.setFont(font(Font.BOLD, 22));
        btnPay.setPreferredSize(new Dimension(0, 55));
        btnPay.addActionListener(e -> checkout());

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(sum, BorderLayout.CENTER);
        bottom.add(btnPay, BorderLayout.SOUTH);

        cartPanel.add(head, BorderLayout.NORTH);
        cartPanel.add(scroll, BorderLayout.CENTER);
        cartPanel.add(bottom, BorderLayout.SOUTH);
        return cartPanel;
    }

    private JLabel summaryLabel(String text, boolean right) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 13));
        if (right) l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    // ---------------- Data / logic ----------------
    /** โหลดสินค้าจาก CSV ใหม่ (เรียกเมื่อเปิดหน้านี้ หรือหลังเพิ่มสินค้าจากหน้าสต็อก) */
    public void refreshProducts() {
        products = storage.loadProducts();
        syncCartWithProducts();
        applyFilter();
    }

    /**
     * ตะกร้าเก็บ Product ตัวเก่าไว้ ถ้าแอดมินแก้ราคา/สต็อก/ลบสินค้าไประหว่างนั้น ตะกร้าจะค้างค่าเก่า
     * -> ผูกกลับกับข้อมูลล่าสุดทุกครั้งที่รีเฟรช, ตัดจำนวนให้ไม่เกินสต็อก, เอาสินค้าที่ถูกลบ/หมดออก
     */
    private void syncCartWithProducts() {
        if (cart.isEmpty()) return;
        boolean changed = false;
        for (Iterator<CartItem> it = cart.iterator(); it.hasNext();) {
            CartItem c = it.next();
            Product fresh = null;
            for (Product p : products) {
                if (p.getId().equals(c.getProduct().getId())) { fresh = p; break; }
            }
            if (fresh == null) { it.remove(); changed = true; continue; }
            if (c.getUnitPrice() != fresh.getPrice()) changed = true;
            c.setProduct(fresh);
            c.setUnitPrice(fresh.getPrice());
            if (c.getQuantity() > fresh.getStock()) {
                c.setQuantity(fresh.getStock());
                changed = true;
            }
            if (c.getQuantity() <= 0) { it.remove(); changed = true; }
        }
        renderCart();
        if (changed) {
            JOptionPane.showMessageDialog(this,
                "มีการเปลี่ยนแปลงราคา/สต็อกสินค้า ตะกร้าถูกปรับให้ตรงกับข้อมูลล่าสุดแล้ว",
                "ปรับตะกร้า", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** เช็คสต็อกจริงในไฟล์ก่อนขาย: คืน true ถ้าพอสำหรับทุกรายการในตะกร้า */
    private boolean verifyStock() {
        List<Product> latest = storage.loadProducts();
        for (CartItem c : cart) {
            Product found = null;
            for (Product p : latest) {
                if (p.getId().equals(c.getProduct().getId())) { found = p; break; }
            }
            String name = c.getProduct().getName();
            if (found == null) {
                JOptionPane.showMessageDialog(this, "สินค้า \"" + name + "\" ถูกลบออกจากระบบแล้ว",
                    "ไม่สามารถชำระเงินได้", JOptionPane.WARNING_MESSAGE);
                refreshProducts();
                return false;
            }
            if (found.getStock() < c.getQuantity()) {
                JOptionPane.showMessageDialog(this,
                    "สต็อกของ \"" + name + "\" ไม่พอ (คงเหลือ " + found.getStock()
                        + " ชิ้น แต่ในตะกร้ามี " + c.getQuantity() + " ชิ้น)",
                    "สต็อกไม่พอ", JOptionPane.WARNING_MESSAGE);
                refreshProducts();
                return false;
            }
        }
        return true;
    }

    public void loadProducts() { refreshProducts(); }

    private void applyFilter() {
        filtered = new ArrayList<>();
        for (Product p : products) {
            boolean catOk = activeCategory.equals("ทั้งหมด")
                || p.getCategory().equals(activeCategory);
            String q = searchText.toLowerCase();
            boolean searchOk = q.isEmpty()
                || p.getName().toLowerCase().contains(q)
                || p.getId().toLowerCase().contains(q);
            if (catOk && searchOk) filtered.add(p);
        }
        renderGrid();
    }

    private void renderGrid() {
        gridPanel.removeAll();
        if (filtered.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            empty.setPreferredSize(new Dimension(500, 200));
            JLabel msg = new JLabel("<html><div style='text-align:center'>"
                + "ยังไม่มีสินค้าในร้าน<br>ไปที่หน้า \"การจัดการสินค้าและสต็อก\""
                + "<br>แล้วกด \"+เพิ่มสินค้าใหม่\" เพื่อเพิ่มสินค้า</div></html>");
            msg.setFont(font(Font.PLAIN, 15));
            msg.setForeground(Color.DARK_GRAY);
            empty.add(msg);
            gridPanel.setLayout(new BorderLayout());
            gridPanel.add(empty, BorderLayout.CENTER);
            // กลับเป็น grid layout ครั้งหน้า
        } else {
            gridPanel.setLayout(new GridLayout(0, 3, 15, 15));
            for (Product p : filtered) gridPanel.add(createProductCard(p));
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    public void addProduct(Product product, int qty) {
        if (product == null || qty <= 0) return;
        // เช็คสต็อก
        int inCart = 0;
        for (CartItem c : cart) {
            if (c.getProduct().getId().equals(product.getId())) inCart = c.getQuantity();
        }
        if (inCart + qty > product.getStock()) {
            JOptionPane.showMessageDialog(this,
                "สต็อกไม่พอ (คงเหลือ " + product.getStock() + " ชิ้น)",
                "สต็อกไม่พอ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (CartItem c : cart) {
            if (c.getProduct().getId().equals(product.getId())) {
                c.increase(qty);
                recalc();
                renderCart();
                return;
            }
        }
        cart.add(new CartItem(product, qty));
        recalc();
        renderCart();
    }

    public void removeItem(String productId) {
        cart.removeIf(c -> c.getProduct().getId().equals(productId));
        recalc();
        renderCart();
    }

    public void updateQuantity(String productId, int qty) {
        for (CartItem c : cart) {
            if (c.getProduct().getId().equals(productId)) {
                if (qty <= 0) { removeItem(productId); return; }
                if (qty > c.getProduct().getStock()) {
                    JOptionPane.showMessageDialog(this, "สต็อกไม่พอ", "เตือน", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                c.setQuantity(qty);
            }
        }
        recalc();
        renderCart();
    }

    public double calculateTotal() {
        double sum = 0;
        for (CartItem c : cart) sum += c.getSubtotal();
        totalAmount = sum;
        return totalAmount;
    }

    private double parseDiscount() {
        try {
            discount = Double.parseDouble(txtDiscount.getText().trim().replace(",", ""));
            if (Double.isNaN(discount) || Double.isInfinite(discount) || discount < 0) discount = 0;
        } catch (Exception e) {
            discount = 0;
        }
        return discount;
    }

    private void recalc() {
        double sub = calculateTotal();
        double d = 0;
        if (txtDiscount != null) d = parseDiscount();
        if (d > sub) { d = sub; }
        double net = sub - d;
        if (lblSubtotal != null) lblSubtotal.setText("฿" + String.format("%.2f", sub));
        if (lblNetTotal != null) lblNetTotal.setText("฿" + String.format("%.2f", net));
        if (lblCartCount != null) {
            int n = cart.stream().mapToInt(CartItem::getQuantity).sum();
            lblCartCount.setText(n + " รายการ");
        }
    }

    public void clearCart() {
        cart.clear();
        if (txtDiscount != null) txtDiscount.setText("0");
        recalc();
        renderCart();
    }

    private void renderCart() {
        cartListPanel.removeAll();
        if (cart.isEmpty()) {
            JLabel msg = new JLabel("<html><div style='text-align:center'>ยังไม่มีรายการสั่งซื้อ<br>"
                + "คลิกเลือกเมนูรายการด้านซ้าย<br>เพื่อดำเนินการสั่งซื้อ</div></html>");
            msg.setFont(font(Font.PLAIN, 11));
            msg.setForeground(Color.WHITE);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setOpaque(false);
            wrap.add(msg);
            cartListPanel.add(wrap);
        } else {
            for (CartItem c : cart) {
                cartListPanel.add(cartRow(c));
                cartListPanel.add(Box.createVerticalStrut(6));
            }
        }
        recalc();
        cartListPanel.revalidate();
        cartListPanel.repaint();
    }

    private JPanel cartRow(CartItem c) {
        JPanel row = new JPanel(new BorderLayout(4, 2));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 86));

        JLabel name = new JLabel(c.getProduct().getName());
        name.setFont(font(Font.BOLD, 13));
        JLabel price = new JLabel("฿" + fmtPrice(c.getUnitPrice()) + " x " + c.getQuantity()
            + " = ฿" + String.format("%.2f", c.getSubtotal()));
        price.setFont(font(Font.PLAIN, 11));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        JButton del = new JButton("x");
        del.setMargin(new Insets(0, 6, 0, 6));
        del.setFocusPainted(false);
        del.addActionListener(e -> removeItem(c.getProduct().getId()));
        top.add(del, BorderLayout.EAST);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        qtyPanel.setOpaque(false);
        JButton minus = new JButton("-");
        minus.setMargin(new Insets(0, 8, 0, 8));
        minus.addActionListener(e -> updateQuantity(c.getProduct().getId(), c.getQuantity() - 1));
        JButton plus = new JButton("+");
        plus.setMargin(new Insets(0, 8, 0, 8));
        plus.addActionListener(e -> updateQuantity(c.getProduct().getId(), c.getQuantity() + 1));
        JLabel q = new JLabel(String.valueOf(c.getQuantity()));
        q.setFont(font(Font.BOLD, 13));
        qtyPanel.add(minus);
        qtyPanel.add(q);
        qtyPanel.add(plus);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(qtyPanel, BorderLayout.WEST);
        bottom.add(price, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(top);
        center.add(bottom);

        row.add(center, BorderLayout.CENTER);
        return row;
    }

    // ---------------- Checkout / Payment / Receipt ----------------
    public void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "กรุณาเลือกสินค้าก่อนชำระเงิน",
                "ตะกร้าว่าง", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!verifyStock()) return;
        double sub = calculateTotal();
        double d = parseDiscount();
        if (d > sub) d = sub;
        double net = sub - d;
        showPaymentDialog(nextOrderId(), net);
    }

    private void showPaymentDialog(String orderId, double netTotal) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "ชำระเงิน", true);
        dlg.setSize(560, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(new Color(0xDDDDDD));
        header.setBorder(new EmptyBorder(12, 0, 12, 0));
        JLabel title = new JLabel("ชำระเงิน");
        title.setFont(font(Font.BOLD, 28));
        header.add(title);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(15, 30, 15, 30));

        JLabel orderLbl = new JLabel("ยอดชำระทั้งสิ้น : " + orderId);
        orderLbl.setFont(font(Font.BOLD, 20));
        orderLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel totalLbl = new JLabel("฿" + String.format("%.2f", netTotal));
        totalLbl.setFont(font(Font.BOLD, 30));
        totalLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel receiveRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        receiveRow.setOpaque(false);
        JLabel rLbl = new JLabel("จำนวนเงินที่รับมา:");
        rLbl.setFont(font(Font.BOLD, 18));
        JTextField txtReceived = new JTextField(15);
        txtReceived.setFont(font(Font.PLAIN, 18));
        receiveRow.add(rLbl);
        receiveRow.add(txtReceived);

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        quick.setOpaque(false);
        JButton btnExact = new JButton("พอดี");
        JButton b100 = new JButton("100");
        JButton b500 = new JButton("500");
        JButton b1000 = new JButton("1000");
        for (JButton b : new JButton[]{btnExact, b100, b500, b1000}) {
            b.setFont(AppFont.thai(Font.BOLD, 13));
            b.setBackground(new Color(0xCBBDC0));
            b.setFocusPainted(false);
            quick.add(b);
        }
        btnExact.addActionListener(e -> txtReceived.setText(String.format(Locale.US, "%.2f", netTotal)));
        b100.addActionListener(e -> txtReceived.setText("100"));
        b500.addActionListener(e -> txtReceived.setText("500"));
        b1000.addActionListener(e -> txtReceived.setText("1000"));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btns.setOpaque(false);
        JButton btnCancel = new JButton("Cancel");
        JButton btnOk = new JButton("OK");
        btnCancel.setFont(AppFont.thai(Font.BOLD, 13));
        btnOk.setFont(AppFont.thai(Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(120, 36));
        btnOk.setPreferredSize(new Dimension(120, 36));
        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            double received;
            try {
                received = Double.parseDouble(txtReceived.getText().trim().replace(",", ""));
                if (Double.isNaN(received) || Double.isInfinite(received)) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "กรุณากรอกจำนวนเงินให้ถูกต้อง",
                    "ข้อมูลไม่ถูกต้อง", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (received < netTotal) {
                JOptionPane.showMessageDialog(dlg,
                    "จำนวนเงินไม่พอ (ขาดอีก ฿" + String.format("%.2f", netTotal - received) + ")",
                    "เงินไม่พอ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double change = received - netTotal;
            dlg.dispose();
            completeSale(orderId, netTotal, received, change);
        });
        btns.add(btnCancel);
        btns.add(btnOk);

        body.add(orderLbl);
        body.add(Box.createVerticalStrut(4));
        body.add(totalLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(receiveRow);
        body.add(quick);
        body.add(btns);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(body, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void completeSale(String orderId, double netTotal, double received, double change) {
        // เช็คสต็อกจริงอีกรอบก่อนตัด (กันสต็อกเปลี่ยนระหว่างเปิดหน้าชำระเงิน)
        if (!verifyStock()) return;

        double sub = calculateTotal();
        double d = parseDiscount();
        if (d > sub) d = sub;

        // 1) บันทึกบิลก่อน ถ้าบันทึกไม่ได้ (เช่น เลขบิลซ้ำ/เขียนไฟล์ไม่ได้) จะไม่ตัดสต็อก
        boolean saved = false;
        try {
            SaleOrder order = new SaleOrder(orderId, currentUser);
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("paid");
            for (CartItem c : cart) order.addItem(c.getProduct(), c.getQuantity());
            order.setTotalAmount(netTotal);
            saved = storage.saveSaleOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!saved) {
            JOptionPane.showMessageDialog(this,
                "บันทึกบิลไม่สำเร็จ (เลขบิลซ้ำหรือเขียนไฟล์ไม่ได้) ยังไม่มีการตัดสต็อก กรุณาลองใหม่อีกครั้ง",
                "ผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2) ตัดสต็อก + เซฟ
        List<Product> all = storage.loadProducts();
        for (CartItem c : cart) {
            for (Product p : all) {
                if (p.getId().equals(c.getProduct().getId())) {
                    p.reduceStock(c.getQuantity());
                    break;
                }
            }
        }
        storage.saveAllProducts(all);

        showReceiptDialog(orderId, new ArrayList<>(cart), sub, d, netTotal, received, change);

        clearCart();
        refreshProducts(); // อัปเดตเลขสต็อกบนการ์ด
    }

    private void showReceiptDialog(String orderId, List<CartItem> items,
                                   double sub, double discount, double net,
                                   double received, double change) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "ใบเสร็จรับเงิน", true);
        dlg.setSize(480, 620);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(new Color(0xDDDDDD));
        header.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel title = new JLabel("ใบเสร็จรับเงิน");
        title.setFont(font(Font.BOLD, 24));
        header.add(title);

        JTextArea area = new JTextArea(buildReceiptText(orderId, items, sub, discount, net, received, change));
        // ห้ามใช้ Monospaced ตรงๆ เพราะไม่มี glyph ไทย -> เป็น □□□ ใช้ตัวช่วยเลือกฟอนต์ที่แสดงไทยได้
        area.setFont(AppFont.monoThai(13));
        area.setEditable(false);
        area.setBackground(new Color(0xF7F7F7));
        area.setBorder(new EmptyBorder(10, 14, 10, 14));
        JScrollPane scroll = new JScrollPane(area);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        JButton btnPrint = new JButton("พิมพ์ใบเสร็จ");
        btnPrint.setFont(AppFont.thai(Font.BOLD, 13));
        btnPrint.setBackground(new Color(0x2D6CEB));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "พิมพ์ไม่สำเร็จ: " + ex.getMessage());
            }
        });
        JButton btnClose = new JButton("ปิดหน้าต่าง");
        btnClose.setFont(AppFont.thai(Font.BOLD, 13));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnPrint);
        btnPanel.add(btnClose);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(scroll, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private String buildReceiptText(String orderId, List<CartItem> items,
                                    double sub, double discount, double net,
                                    double received, double change) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String date = LocalDateTime.now().format(fmt);
        String cashier = currentUser == null ? "01" : currentUser.getUsername();
        String line = "=".repeat(46);
        String dash = "-".repeat(46);
        StringBuilder sb = new StringBuilder();
        sb.append(line).append("\n");
        sb.append(orderId).append("\n");
        sb.append("ใบเสร็จรับเงิน\n");
        sb.append("โทร : 011-1111-234\n");
        sb.append(line).append("\n");
        sb.append("บิล : ").append(orderId).append("\n");
        sb.append("เลขที่บิล : MMM-").append(date.replace("-", "").replace(" ", "").replace(":", "").substring(0, 12)).append("-111111\n");
        sb.append("วันที่ :").append(date).append("\n");
        sb.append("แคชเชียร์ : ").append(cashier).append("\n");
        sb.append(dash).append("\n");
        sb.append(String.format("%-14s %6s %8s %10s\n", "รายการ", "จำนวน", "ราคา", "รวม"));
        for (CartItem c : items) {
            sb.append(String.format("%-14s %6d %8.0f %10.2f\n",
                truncate(c.getProduct().getName(), 14),
                c.getQuantity(), c.getUnitPrice(), c.getSubtotal()));
        }
        sb.append(dash).append("\n");
        sb.append(String.format("รวมเงิน :     %.2f\n", sub));
        sb.append(String.format("ส่วนลด :     %.2f\n", discount));
        sb.append(line).append("\n");
        sb.append(String.format("ยอดสุทธิ :    %.2f\n", net));
        sb.append(line).append("\n");
        sb.append(String.format("วิธีชำระเงิน:         เงินสด\n"));
        sb.append(String.format("รับเงินมา:           %.2f\n", received));
        sb.append(String.format("เงินทอน:             %.2f\n", change));
        sb.append(line).append("\n");
        sb.append("      Thank you for Order !!!!\n");
        return sb.toString();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }

    // --- getters (คงตาม diagram) ---
    public List<CartItem> getCart() { return cart; }
    public double getTotalAmount() { return totalAmount; }
    public List<Product> getProducts() { return products; }
}
