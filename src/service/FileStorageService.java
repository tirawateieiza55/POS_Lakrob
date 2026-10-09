package service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.Product;
import model.SaleOrder;
import model.SaleOrderItem;
import model.User;

/**
 * FileStorageService - Service & Persistence Layer
 * อ่าน-เขียน CSV ในโฟลเดอร์ data/
 *  - users.csv      : id,username,password,role
 *  - products.csv   : id,name,category,price,stock
 *  - sales.csv      : id,orderDate,totalAmount,status,cashier
 *  - sale_items.csv : orderId,productId,qty,unitPrice
 */
public class FileStorageService extends Service {

    private String basePath;

    public FileStorageService() {
        this.basePath = "data/";
        ensureDataFiles();
    }

    public FileStorageService(String basePath) {
        if (basePath == null || basePath.isBlank()) basePath = "data/";
        if (!basePath.endsWith("/") && !basePath.endsWith("\\")) basePath += "/";
        this.basePath = basePath;
        ensureDataFiles();
    }

    private String usersFile() { return basePath + "users.csv"; }
    private String productsFile() { return basePath + "products.csv"; }
    private String salesFile() { return basePath + "sales.csv"; }
    private String saleItemsFile() { return basePath + "sale_items.csv"; }

    private void ensureDataFiles() {
        try {
            Path dir = Paths.get(basePath);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            Path uf = Paths.get(usersFile());
            if (!Files.exists(uf)) {
                List<String> lines = new ArrayList<>();
                lines.add("id,username,password,role");
                lines.add("01,admin,admin123,Admin");
                lines.add("02,user,user123,พนักงานขาย");
                Files.write(uf, lines, StandardCharsets.UTF_8);
            }
            Path pf = Paths.get(productsFile());
            if (!Files.exists(pf)) {
                List<String> lines = new ArrayList<>();
                lines.add("id,name,category,price,stock");
                Files.write(pf, lines, StandardCharsets.UTF_8);
            }
            Path sf = Paths.get(salesFile());
            if (!Files.exists(sf)) {
                Files.write(sf,
                    List.of("id,orderDate,totalAmount,status,cashier"),
                    StandardCharsets.UTF_8);
            }
            Path sif = Paths.get(saleItemsFile());
            if (!Files.exists(sif)) {
                Files.write(sif, List.of("orderId,productId,qty,unitPrice"), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- CSV helpers ----------
    private static String[] splitCsv(String line) {
        // แบบง่าย (ชื่อสินค้าไม่มี comma นอกจากหมวดหมู่ที่ใช้ comma จริง)
        // หมวดหมู่ "เนื้อ,หมู,ไก่,ทะเล" มี comma -> ต้องรองรับ quote
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String unesc(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    // ---------- User ----------
    public List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        try {
            Path p = Paths.get(usersFile());
            if (!Files.exists(p)) { ensureDataFiles(); }
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] c = splitCsv(line);
                if (c.length < 4) continue;
                list.add(new User(unesc(c[0]), unesc(c[1]), unesc(c[2]), unesc(c[3])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** คืน User ถ้า username/password ตรง, ไม่ตรงคืน null */
    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        for (User u : loadUsers()) {
            if (u.getUsername().equals(username.trim()) && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    public void saveUser(User user) {
        if (user == null) return;
        List<User> all = loadUsers();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(user.getId())) { all.set(i, user); found = true; break; }
        }
        if (!found) all.add(user);
        try {
            List<String> lines = new ArrayList<>();
            lines.add("id,username,password,role");
            for (User u : all) {
                lines.add(esc(u.getId()) + "," + esc(u.getUsername()) + "," + esc(u.getPassword()) + "," + esc(u.getRole()));
            }
            Files.write(Paths.get(usersFile()), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User loadUser() {
        List<User> all = loadUsers();
        return all.isEmpty() ? null : all.get(0);
    }

    // ---------- Product ----------
    public List<Product> loadProducts() {
        List<Product> list = new ArrayList<>();
        try {
            Path p = Paths.get(productsFile());
            if (!Files.exists(p)) { ensureDataFiles(); return list; }
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] c = splitCsv(line);
                if (c.length < 5) continue;
                try {
                    String id = unesc(c[0]);
                    String name = unesc(c[1]);
                    String cat = unesc(c[2]);
                    double price = Double.parseDouble(unesc(c[3]));
                    int stock = Integer.parseInt(unesc(c[4]));
                    list.add(new Product(id, name, cat, price, stock));
                } catch (NumberFormatException ex) {
                    // ข้ามแถวเสีย
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveAllProducts(List<Product> products) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("id,name,category,price,stock");
            if (products != null) {
                for (Product p : products) {
                    lines.add(esc(p.getId()) + "," + esc(p.getName()) + "," + esc(p.getCategory())
                        + "," + p.getPrice() + "," + p.getStock());
                }
            }
            Files.write(Paths.get(productsFile()), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveProduct(Product product) {
        if (product == null) return;
        List<Product> all = loadProducts();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(product.getId())) { all.set(i, product); found = true; break; }
        }
        if (!found) all.add(product);
        saveAllProducts(all);
    }

    public void deleteProduct(String productId) {
        List<Product> all = loadProducts();
        all.removeIf(p -> p.getId().equals(productId));
        saveAllProducts(all);
    }

    // ---------- SaleOrder (หัวบิล + รายการสินค้าในบิล) ----------
    public void saveSaleOrder(SaleOrder order) {
        if (order == null) return;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String date = order.getOrderDate() == null
                ? LocalDateTime.now().format(fmt) : order.getOrderDate().format(fmt);
            String cashier = order.getCreatedBy() == null ? "" : order.getCreatedBy().getUsername();
            String line = esc(order.getId()) + "," + esc(date) + "," + order.getTotalAmount()
                + "," + esc(order.getStatus() == null ? "paid" : order.getStatus()) + "," + esc(cashier);
            Path p = Paths.get(salesFile());
            if (!Files.exists(p)) ensureDataFiles();
            // กันเขียน id ซ้ำ: ถ้ามีแล้วให้ข้าม
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String l : lines) {
                if (l.startsWith(order.getId() + ",")) return;
            }
            Files.write(p, (System.lineSeparator() + line).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND);

            // บันทึกรายการสินค้าในบิล (ใช้คืนสต็อกตอนลบบิล)
            Path ip = Paths.get(saleItemsFile());
            if (!Files.exists(ip)) ensureDataFiles();
            StringBuilder sb = new StringBuilder();
            for (SaleOrderItem it : order.getItems()) {
                if (it.getProduct() == null) continue;
                sb.append(System.lineSeparator())
                  .append(esc(order.getId())).append(",")
                  .append(esc(it.getProduct().getId())).append(",")
                  .append(it.getQuantity()).append(",")
                  .append(it.getUnitPrice());
            }
            if (sb.length() > 0) {
                Files.write(ip, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SaleOrder> loadSaleOrders() {
        List<SaleOrder> list = new ArrayList<>();
        try {
            Path p = Paths.get(salesFile());
            if (!Files.exists(p)) { ensureDataFiles(); return list; }
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] c = splitCsv(line);
                if (c.length < 5) continue;
                try {
                    SaleOrder o = new SaleOrder();
                    o.setId(unesc(c[0]));
                    o.setOrderDate(LocalDateTime.parse(unesc(c[1]), fmt));
                    o.setTotalAmount(Double.parseDouble(unesc(c[2])));
                    o.setStatus(unesc(c[3]));
                    list.add(o);
                } catch (Exception ex) {
                    // ข้ามแถวเสีย
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * ลบบิลและคืนสต็อกตามรายการในบิล
     * คืน -1 ถ้าไม่พบบิล/เขียนไฟล์ไม่ได้, นอกนั้นคืนจำนวนรายการสินค้าที่คืนสต็อกได้
     */
    public int deleteSaleOrder(String orderId) {
        if (orderId == null) return -1;
        try {
            Path sp = Paths.get(salesFile());
            if (!Files.exists(sp)) return -1;
            List<String> salesLines = Files.readAllLines(sp, StandardCharsets.UTF_8);
            List<String> keepSales = new ArrayList<>();
            boolean found = false;
            for (int i = 0; i < salesLines.size(); i++) {
                String line = salesLines.get(i);
                if (i > 0 && !line.trim().isEmpty()
                        && unesc(splitCsv(line.trim())[0]).equals(orderId)) {
                    found = true;
                    continue;
                }
                keepSales.add(line);
            }
            if (!found) return -1;

            int restored = 0;
            Path ip = Paths.get(saleItemsFile());
            if (Files.exists(ip)) {
                List<String> items = Files.readAllLines(ip, StandardCharsets.UTF_8);
                List<String> keepItems = new ArrayList<>();
                List<Product> products = loadProducts();
                for (int i = 0; i < items.size(); i++) {
                    String line = items.get(i).trim();
                    if (i == 0 || line.isEmpty()) { keepItems.add(items.get(i)); continue; }
                    String[] c = splitCsv(line);
                    if (c.length < 3 || !unesc(c[0]).equals(orderId)) {
                        keepItems.add(items.get(i));
                        continue;
                    }
                    try {
                        String pid = unesc(c[1]);
                        int qty = Integer.parseInt(unesc(c[2]));
                        for (Product p : products) {
                            if (p.getId().equals(pid)) {
                                p.addStock(qty);
                                restored++;
                                break;
                            }
                        }
                    } catch (NumberFormatException ex) {
                        // ข้ามแถวเสีย
                    }
                }
                if (restored > 0) saveAllProducts(products);
                Files.write(ip, keepItems, StandardCharsets.UTF_8);
            }
            Files.write(sp, keepSales, StandardCharsets.UTF_8);
            return restored;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // --- override จาก Service (abstract) ---
    @Override
    public void save(Object data) {
        if (data instanceof Product) saveProduct((Product) data);
        else if (data instanceof SaleOrder) saveSaleOrder((SaleOrder) data);
        else if (data instanceof User) saveUser((User) data);
    }

    @Override
    public Object load() {
        return loadProducts();
    }

    @Override
    public void delete(String id) {
        deleteProduct(id);
    }

    // --- getter/setter ---
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
}