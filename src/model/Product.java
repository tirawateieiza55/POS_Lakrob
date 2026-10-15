package model;

/**
 * Product - สินค้า 1 ตัวในร้าน (Domain Model)
 * ------------------------------------------------------------------
 * ฟิลด์: id (เช่น M001), name (ชื่อ), category (หมวด), price, stock
 * - ไฟล์จริง: data/products.csv  อ่าน/เขียนผ่าน FileStorageService
 * - ใครใช้บ้าง: PosPanel (โชว์การ์ด), InventoryPanel (ตารางสต็อก),
 *   CartItem/SaleOrderItem (อ้างถึงสินค้า)
 *
 * จะแก้ครั้งหน้า:
 * - เกณฑ์ "ใกล้หมด" อยู่ที่ isLowStock()/getStatus() (ตอนนี้ <= 5)
 *   เปลี่ยนเลขตรงนั้นที่เดียว ทุกจอเปลี่ยนตาม
 * - ห้ามติดลบ: addStock/reduceStock/setStock กันค่าติดลบไว้แล้ว
 *   อย่า set stock ตรงๆ ข้ามเมธอดพวกนี้
 */
public class Product {

    /** เกณฑ์เตือนสต็อกใกล้หมด (แก้เลขนี้ที่เดียว) */
    public static final int LOW_STOCK_LIMIT = 5;

    private String id;
    private String name;
    private String category;
    private double price;
    private int stock;

    /** constructor เปล่า: มีไว้ให้ตอนโหลด CSV สร้าง object ก่อนแล้วค่อย set */
    public Product() {
    }

    /** สร้างแบบไม่ระบุหมวด (หมวด = "") */
    public Product(String id, String name, double price, int stock) {
        this(id, name, "", price, stock);
    }

    /** สร้างแบบครบ: รหัส, ชื่อ, หมวด, ราคา, สต็อก */
    public Product(String id, String name, String category, double price, int stock) {
        this.id = id;
        this.name = name;
        this.category = category == null ? "" : category;
        this.price = price;
        this.stock = Math.max(0, stock);
    }

    /** มีของเหลือขายไหม (stock > 0) */
    public boolean isInStock() {
        return stock > 0;
    }

    /** สต็อกใกล้หมดไหม (เหลือแต่ไม่เกิน LOW_STOCK_LIMIT) */
    public boolean isLowStock() {
        return stock > 0 && stock <= LOW_STOCK_LIMIT;
    }

    /** เติมสต็อก (qty <= 0 จะถูกเมิน ไม่ติดลบ) */
    public void addStock(int qty) {
        if (qty > 0) this.stock += qty;
    }

    /** ตัดสต็อกตอนขาย (กันติดลบ: ถ้าตัดเกินจะเหลือ 0) */
    public void reduceStock(int qty) {
        if (qty <= 0) return;
        this.stock -= qty;
        if (this.stock < 0) this.stock = 0;
    }

    /** สถานะภาษาไทยโชว์ในตาราง: หมดสต็อก / ใกล้หมด / พร้อมขาย */
    public String getStatus() {
        if (stock <= 0) return "หมดสต็อก";
        if (stock <= LOW_STOCK_LIMIT) return "ใกล้หมด";
        return "พร้อมขาย";
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category == null ? "" : category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    /** ตั้งสต็อกตรงๆ (กันติดลบให้แล้ว) */
    public void setStock(int stock) { this.stock = Math.max(0, stock); }
}
