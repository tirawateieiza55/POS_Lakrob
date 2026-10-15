package model;

/**
 * SaleOrderItem - สินค้า 1 แถวในบิล (ลูกของ SaleOrder แบบ Composition)
 * ------------------------------------------------------------------
 * ต่างจาก CartItem ยังไง (จำแค่นี้):
 * - CartItem      = ของใน "ตะกร้า" ตอนกำลังเลือก ยังไม่จ่าย ลบ/แก้ได้อิสระ
 * - SaleOrderItem = ของใน "บิลที่จ่ายแล้ว" ถูกบันทึก sale_items.csv ห้ามแก้
 *
 * ฟิลด์: id (เช่น "Order 3-1"), product (อ้าง Product), quantity,
 *        unitPrice (ราคาตอนขาย ถ่ายไว้กันราคาขึ้นลงย้อนหลังเพี้ยน),
 *        subtotal (unitPrice * quantity คำนวณสดทุกครั้ง)
 */
public class SaleOrderItem {

    private String id;
    private Product product;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    /** constructor เปล่า: มีไว้ให้ตอนโหลดไฟล์ */
    public SaleOrderItem() {
    }

    public SaleOrderItem(String id, Product product, int quantity, double unitPrice) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** ยอดแถวนี้ = ราคาต่อชิ้น x จำนวน (คำนวณใหม่ทุกครั้งที่เรียก) */
    public double getSubtotal() {
        this.subtotal = unitPrice * quantity;
        return subtotal;
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
