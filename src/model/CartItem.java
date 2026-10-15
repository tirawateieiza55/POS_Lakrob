package model;

/**
 * CartItem - สินค้า 1 แถวใน "ตะกร้า" ฝั่งขวาของหน้า POS
 * ------------------------------------------------------------------
 * ความสัมพันธ์: อ้าง Product แบบ 1:1 (เก็บ pointer ไป Product ตัวจริง)
 * ฟิลด์: product, quantity, unitPrice (ถ่ายราคาตอนหยิบ), subtotal
 *
 * จะแก้ครั้งหน้า:
 * - เพิ่มสินค้าลงตะกร้า: ดู PosPanel.addProduct() (มีเช็คสต็อกก่อนเพิ่ม)
 * - แปลงตะกร้าเป็นบิลตอนจ่ายเงิน: ใช้ toSaleOrderItem(orderId, index)
 *   อย่าสร้าง SaleOrderItem มือเองที่อื่น (กัน id/ราคาไม่ตรงกัน)
 */
public class CartItem {

    private Product product;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    /** constructor เปล่า: มีไว้เผื่อ framework / เทส */
    public CartItem() {
    }

    /** สร้างจากสินค้า + จำนวน (จำนวน < 1 จะปัดเป็น 1 ให้) */
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);
        this.unitPrice = product != null ? product.getPrice() : 0;
        this.subtotal = this.unitPrice * this.quantity;
    }

    /** ยอดแถวนี้ (คำนวณสดทุกครั้ง) */
    public double getSubtotal() {
        this.subtotal = unitPrice * quantity;
        return subtotal;
    }

    /** ตั้งจำนวน (<= 0 ได้ = เอาไว้ลบออก, caller ต้อง remove เอง) */
    public void setQuantity(int qty) {
        if (qty < 0) qty = 0;
        this.quantity = qty;
        this.subtotal = this.unitPrice * this.quantity;
    }

    /** บวกจำนวนเพิ่ม (ปุ่ม + ในตะกร้า) */
    public void increase(int delta) {
        setQuantity(this.quantity + delta);
    }

    /**
     * แปลงตะกร้าแถวนี้เป็นแถวในบิลจริง (เรียกตอนกดชำระเงิน)
     * id รูปแบบ: "Order 5-1", "Order 5-2", ...
     */
    public SaleOrderItem toSaleOrderItem(String orderId, int index) {
        String itemId = orderId + "-" + index;
        return new SaleOrderItem(itemId, product, quantity, unitPrice);
    }

    // --- getters / setters ---
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
