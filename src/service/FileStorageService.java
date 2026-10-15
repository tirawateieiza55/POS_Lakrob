package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Product;
import model.SaleOrder;
import model.SaleOrderItem;
import model.User;

import static service.CsvUtil.esc;
import static service.CsvUtil.unesc;
import static service.CsvUtil.splitCsv;
import static service.CsvUtil.appendLines;

/**
 * FileStorageService - ชั้นเก็บไฟล์ CSV ทั้งหมดของระบบ (Persistence)
 * ------------------------------------------------------------------
 * หน้าที่: อ่าน/เขียนไฟล์ในโฟลเดอร์ data/ เท่านั้น ไม่มี logic ขายของ
 *
 * ไฟล์ที่ดูแล (4 ไฟล์):
 *   data/users.csv      : id,username,password,role
 *   data/products.csv   : id,name,category,price,stock
 *   data/sales.csv      : id,orderDate,totalAmount,status,cashier
 *   data/sale_items.csv : orderId,productId,qty,unitPrice
 *
 * วิธีใช้ที่ถูกต้อง (อ่านก่อนแก้):
 *   FileStorageService db = FileStorageService.getInstance();
 *   List<Product> ps = db.loadProducts();
 * ห้าม new FileStorageService() กระจายหลายที่แบบเดิม เพราะจะได้คนละ
 * instance กัน เสี่ยงอ่าน/เขียนชนกัน ให้เรียก getInstance() ตัวเดียว
 *
 * ถ้าจะเพิ่มตารางใหม่:
 * 1) เพิ่มเมธอด xxxFile() + เพิ่มใน ensureDataFiles()
 * 2) เพิ่ม loadXxx()/saveXxx() ในโซนของตัวเอง (ดูโซนด้านล่าง)
 * 3) อย่าใส่ logic ธุรกิจ (เช่น คำนวณยอด) ที่นี่ ให้ไปไว้ใน model/gui
 *
 * เรื่อง encoding: ไฟล์ทั้งหมดเป็น UTF-8 ชื่อสินค้าไทยมี comma ได้
 * การตัด/ครอบ quote ย้ายไปอยู่ใน CsvUtil หมดแล้ว
 */
public class FileStorageService {

    /** สถานะบิลที่ถูกยกเลิก (เก็บใน sales.csv ช่อง status) */
    public static final String STATUS_CANCELLED = "cancelled";
    /** สถานะบิลที่จ่ายแล้ว */
    public static final String STATUS_PAID = "paid";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================================================================
    // Singleton: ใช้ตัวเดียวทั้งแอป
    // ==================================================================
    private static volatile FileStorageService instance;

    /** ใช้ตัวนี้เป็นหลัก: ได้ instance เดียวกันทั้งแอป */
    public static FileStorageService getInstance() {
        if (instance == null) {
            synchronized (FileStorageService.class) {
                if (instance == null) instance = new FileStorageService();
            }
        }
        return instance;
    }

    private String basePath;

    /** constructor หลัก: ใช้ data/ ข้างโฟลเดอร์ที่รันโปรแกรม */
    public FileStorageService() {
        this.basePath = "data/";
        ensureDataFiles();
    }

    /**
     * constructor สำหรับเทส: ระบุโฟลเดอร์เก็บไฟล์เองได้
     * ตัวอย่าง: new FileStorageService("data_test/")
     */
    public FileStorageService(String basePath) {
        if (basePath == null || basePath.isBlank()) basePath = "data/";
        if (!basePath.endsWith("/") && !basePath.endsWith("\\")) basePath += "/";
        this.basePath = basePath;
        ensureDataFiles();
    }

    // ==================================================================
    // ชื่อไฟล์ (แก้ path รวมที่นี่ที่เดียว)
    // ==================================================================
    private String usersFile()     { return basePath + "users.csv"; }
    private String productsFile()  { return basePath + "products.csv"; }
    private String salesFile()     { return basePath + "sales.csv"; }
    private String saleItemsFile() { return basePath + "sale_items.csv"; }

    // ==================================================================
    // สร้างโฟลเดอร์/ไฟล์เปล่าพร้อม header ถ้ายังไม่มี
    // ==================================================================
    private void ensureDataFiles() {
        try {
            Path dir = Paths.get(basePath);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // users.csv - แถม admin/user เริ่มต้นให้ 2 คน (ล็อกอินครั้งแรกได้เลย)
            Path uf = Paths.get(usersFile());
            if (!Files.exists(uf)) {
                List<String> lines = new ArrayList<>();
                lines.add("id,username,password,role");
                lines.add("01,admin,admin123,Admin");
                // ระวัง: ห้ามมี comma ในชื่อ role ถ้าไม่ครอบ quote
                lines.add("02,user,user123,พนักงานขาย");
                Files.write(uf, lines, StandardCharsets.UTF_8);
            }
            // products.csv - เริ่มว่าง เพิ่มผ่านหน้า "คลังสินค้า"
            Path pf = Paths.get(productsFile());
            if (!Files.exists(pf)) {
                Files.write(pf, List.of("id,name,category,price,stock"), StandardCharsets.UTF_8);
            }
            // sales.csv - หัวบิล
            Path sf = Paths.get(salesFile());
            if (!Files.exists(sf)) {
                Files.write(sf, List.of("id,orderDate,totalAmount,status,cashier"),
                        StandardCharsets.UTF_8);
            }
            // sale_items.csv - รายการสินค้าในแต่ละบิล (เอาไว้คืนสต็อกตอนยกเลิกบิล)
            Path sif = Paths.get(saleItemsFile());
            if (!Files.exists(sif)) {
                Files.write(sif, List.of("orderId,productId,qty,unitPrice"), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================================================================
    // โซนที่ 1: User (ล็อกอิน)
    // ==================================================================

    /** โหลด user ทั้งหมด (ข้ามบรรทัดเสีย ไม่ throw) */
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

    /**
     * เช็คล็อกอิน: คืน User ถ้า username/password ตรง, ไม่ตรงคืน null
     * หมายเหตุ: เทียบ username แบบ trim ช่องว่างหัวท้ายให้แล้ว
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        for (User u : loadUsers()) {
            if (u.getUsername().equals(username.trim()) && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    /** เก็บ user (id ซ้ำ = อัปเดต, id ใหม่ = เพิ่ม) แล้วเขียนไฟล์ใหม่ทั้งไฟล์ */
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
                lines.add(esc(u.getId()) + "," + esc(u.getUsername()) + ","
                        + esc(u.getPassword()) + "," + esc(u.getRole()));
            }
            Files.write(Paths.get(usersFile()), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** คืน user คนแรก (ใช้ตอนเทสเร็วๆ ถ้าไม่มีคืน null) */
    public User loadUser() {
        List<User> all = loadUsers();
        return all.isEmpty() ? null : all.get(0);
    }

    // ==================================================================
    // โซนที่ 2: Product (สินค้า)
    // ==================================================================

    /** โหลดสินค้าทั้งหมดจาก products.csv */
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
                    // ข้ามแถวที่ราค/สต็อกเสีย ไม่ให้โปรแกรมล่ม
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** เขียนสินค้าทั้ง list ลงไฟล์ใหม่ทั้งไฟล์ (ใช้หลังตัดสต็อก/คืนสต็อก) */
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

    /** เก็บสินค้า 1 ตัว (id ซ้ำ = อัปเดต) */
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

    /** ลบสินค้าด้วย id (ไม่เจอ = เงียบไว้) */
    public void deleteProduct(String productId) {
        List<Product> all = loadProducts();
        all.removeIf(p -> p.getId().equals(productId));
        saveAllProducts(all);
    }

    // ==================================================================
    // โซนที่ 3: SaleOrder (หัวบิล + รายการในบิล)
    // ==================================================================

    /**
     * บันทึกบิล + รายการสินค้าในบิล
     * ลำดับ: เช็คว่าเลขบิลซ้ำไหม -> ต่อท้าย sales.csv -> ต่อท้าย sale_items.csv
     * คืน true = สำเร็จ, false = บิล null / เลขบิลซ้ำ / เขียนไฟล์ไม่ได้
     * สำคัญ: ถ้าคืน false ห้ามตัดสต็อก (caller ต้องเช็คก่อนตัดเสมอ)
     */
    public boolean saveSaleOrder(SaleOrder order) {
        if (order == null || order.getId() == null) return false;
        try {
            String date = order.getOrderDate() == null
                    ? LocalDateTime.now().format(DATE_FMT) : order.getOrderDate().format(DATE_FMT);
            String cashier = order.getCreatedBy() == null ? "" : order.getCreatedBy().getUsername();
            String line = esc(order.getId()) + "," + esc(date) + "," + order.getTotalAmount()
                    + "," + esc(order.getStatus() == null ? STATUS_PAID : order.getStatus())
                    + "," + esc(cashier);
            Path p = Paths.get(salesFile());
            if (!Files.exists(p)) ensureDataFiles();
            // กันเขียนเลขบิลซ้ำ: ถ้ามีแล้วให้ไม่บันทึก (caller จะได้รู้ว่าต้องลองเลขใหม่)
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String l = lines.get(i).trim();
                if (l.isEmpty()) continue;
                if (unesc(splitCsv(l)[0]).equals(order.getId())) return false;
            }
            appendLines(p, List.of(line));

            // บันทึกรายการสินค้าในบิล (เอาไว้คืนสต็อกตอนยกเลิกบิล + ดูรายละเอียดย้อนหลัง)
            Path ip = Paths.get(saleItemsFile());
            if (!Files.exists(ip)) ensureDataFiles();
            List<String> itemLines = new ArrayList<>();
            for (SaleOrderItem it : order.getItems()) {
                if (it.getProduct() == null) continue;
                itemLines.add(esc(order.getId()) + "," + esc(it.getProduct().getId()) + ","
                        + it.getQuantity() + "," + it.getUnitPrice());
            }
            appendLines(ip, itemLines);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * โหลดบิลทั้งหมด "พร้อมรายการสินค้า + คนขาย"
     * - อ่าน sales.csv เป็นหัวบิลก่อน
     * - แล้ว join sale_items.csv + products.csv เติม items และ createdBy ให้
     * - บิลเก่าที่ขายก่อนมีระบบ sale_items จะได้ items ว่าง (ไม่ล่ม)
     */
    public List<SaleOrder> loadSaleOrders() {
        List<SaleOrder> list = new ArrayList<>();
        try {
            Path p = Paths.get(salesFile());
            if (!Files.exists(p)) { ensureDataFiles(); return list; }

            // เตรียม map สินค้า (id -> Product) ไว้ join กับ sale_items
            Map<String, Product> productMap = new HashMap<>();
            for (Product pr : loadProducts()) productMap.put(pr.getId(), pr);

            // เตรียม map รายการสินค้าในแต่ละบิล (orderId -> items)
            Map<String, List<SaleOrderItem>> itemsMap = loadSaleItemsGrouped(productMap);

            // เตรียม map user (username -> User) ไว้เติม createdBy
            Map<String, User> userMap = new HashMap<>();
            for (User u : loadUsers()) userMap.put(u.getUsername(), u);

            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] c = splitCsv(line);
                if (c.length < 5) continue;
                try {
                    SaleOrder o = new SaleOrder();
                    o.setId(unesc(c[0]));
                    o.setOrderDate(LocalDateTime.parse(unesc(c[1]), DATE_FMT));
                    o.setTotalAmount(Double.parseDouble(unesc(c[2])));
                    o.setStatus(unesc(c[3]));
                    String cashierName = unesc(c[4]);
                    if (!cashierName.isEmpty()) o.setCreatedBy(userMap.get(cashierName));

                    // เติมรายการสินค้า (ถ้าไม่มี = บิลเก่า ได้ list ว่าง)
                    List<SaleOrderItem> items = itemsMap.get(o.getId());
                    if (items != null) {
                        for (SaleOrderItem it : items) o.getItems().add(it);
                    }
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
     * โหลดบิลใบเดียวพร้อมรายการสินค้า (ใช้ในหน้า History ปุ่มดูรายละเอียด)
     * คืน null ถ้าไม่เจอเลขบิล
     */
    public SaleOrder loadSaleOrderById(String orderId) {
        if (orderId == null) return null;
        for (SaleOrder o : loadSaleOrders()) {
            if (orderId.equals(o.getId())) return o;
        }
        return null;
    }

    /** อ่าน sale_items.csv แล้วจัดกลุ่มตาม orderId (helper ของ loadSaleOrders) */
    private Map<String, List<SaleOrderItem>> loadSaleItemsGrouped(Map<String, Product> productMap) {
        Map<String, List<SaleOrderItem>> map = new HashMap<>();
        try {
            Path ip = Paths.get(saleItemsFile());
            if (!Files.exists(ip)) return map;
            List<String> lines = Files.readAllLines(ip, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] c = splitCsv(line);
                if (c.length < 4) continue;
                try {
                    String orderId = unesc(c[0]);
                    String pid = unesc(c[1]);
                    int qty = Integer.parseInt(unesc(c[2]));
                    double unitPrice = Double.parseDouble(unesc(c[3]));
                    // ผูกกับ Product ตัวจริงถ้ายังมีอยู่ (ถ้าถูกลบไปแล้วก็สร้างตัวแทนไว้ดูชื่อไม่ได้ -> ข้าม)
                    Product pr = productMap.get(pid);
                    if (pr == null) pr = new Product(pid, "(สินค้าถูกลบ)", "", unitPrice, 0);
                    SaleOrderItem it = new SaleOrderItem(orderId + "-" + (map.getOrDefault(orderId, List.of()).size() + 1),
                            pr, qty, unitPrice);
                    map.computeIfAbsent(orderId, k -> new ArrayList<>()).add(it);
                } catch (NumberFormatException ex) {
                    // ข้ามแถวเสีย
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * ลบบิลถาวร + คืนสต็อกตามรายการในบิล
     * คืน -1 = ไม่พบบิล/เขียนไฟล์ไม่ได้, นอกนั้น = จำนวนรายการที่คืนสต็อกได้
     * หมายเหตุ: ปกติให้ใช้ cancelSaleOrder() แทน (เก็บบิลไว้ดูย้อนหลัง)
     * เมธอดนี้เก็บไว้เผื่อต้องลบจริงๆ
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

    /**
     * ยกเลิกบิล: เปลี่ยนสถานะเป็น cancelled (บิลยังอยู่ในประวัติ) + คืนสต็อก
     * คืน -1 = ไม่พบบิล/เขียนไฟล์ไม่ได้,
     *     -2 = บิลนี้ถูกยกเลิกไปแล้ว (กันคืนสต็อกซ้ำ),
     *     นอกนั้น = จำนวนรายการที่คืนสต็อกได้
     */
    public int cancelSaleOrder(String orderId) {
        if (orderId == null) return -1;
        try {
            Path sp = Paths.get(salesFile());
            if (!Files.exists(sp)) return -1;
            List<String> lines = Files.readAllLines(sp, StandardCharsets.UTF_8);
            int idx = -1;
            String[] cols = null;
            for (int i = 1; i < lines.size(); i++) {
                String l = lines.get(i).trim();
                if (l.isEmpty()) continue;
                String[] c = splitCsv(l);
                if (c.length >= 4 && unesc(c[0]).equals(orderId)) { idx = i; cols = c; break; }
            }
            if (idx < 0) return -1;
            if (STATUS_CANCELLED.equalsIgnoreCase(unesc(cols[3]))) return -2;

            // คืนสต็อก (รายการใน sale_items.csv เก็บไว้เหมือนเดิม ไม่ลบ)
            int restored = 0;
            Path ip = Paths.get(saleItemsFile());
            if (Files.exists(ip)) {
                List<String> items = Files.readAllLines(ip, StandardCharsets.UTF_8);
                List<Product> products = loadProducts();
                for (int i = 1; i < items.size(); i++) {
                    String line = items.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] c = splitCsv(line);
                    if (c.length < 3 || !unesc(c[0]).equals(orderId)) continue;
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
            }

            // เปลี่ยนสถานะ (ฟิลด์อื่นเก็บตามเดิม)
            cols[3] = esc(STATUS_CANCELLED);
            lines.set(idx, String.join(",", cols));
            Files.write(sp, lines, StandardCharsets.UTF_8);
            return restored;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // --- getter/setter ---
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
}
