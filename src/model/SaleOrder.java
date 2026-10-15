package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SaleOrder - หัวบิล / ใบเสร็จ 1 ใบ (Domain Model)
 * ------------------------------------------------------------------
 * โครง: SaleOrder 1 ใบ ประกอบด้วย SaleOrderItem หลายแถว (Composition)
 * ฟิลด์:
 *   id          เช่น "Order 5" (เลขบิล ห้ามซ้ำ - ดู nextOrderId ใน PosPanel)
 *   orderDate   วันเวลาขาย
 *   items       รายการสินค้าในบิล
 *   subtotal    รวมก่อนหักส่วนลด (คำนวณจาก items)
 *   discount    ส่วนลด (บาท)
 *   totalAmount ยอดสุทธิ = subtotal - discount (ค่าที่เก็บลง sales.csv)
 *   status      "paid" หรือ "cancelled"
 *   createdBy   User คนขาย (เก็บ username ลง sales.csv ช่อง cashier)
 *
 * กฎสำคัญ (ห้ามลืม):
 * 1) Model นี้ "ห้าม" อ่าน/เขียนไฟล์เอง ให้เรียกผ่าน
 *    FileStorageService.getInstance().saveSaleOrder(order) จากฝั่ง GUI
 *    (เดิมมี save()/loadById() ในนี้ = ผิด layer ลบออกแล้ว)
 * 2) totalAmount ที่เซฟ = ยอดหลังหักส่วนลดเสมอ กันรายงานเพี้ยน
 *
 * จะแก้ครั้งหน้า:
 * - เปลี่ยนวิธีคำนวณยอด: แก้ calculateTotal()/calculateNetTotal() ตรงนี้
 * - ดูบิลย้อนหลังพร้อมรายการ: db.loadSaleOrderById(id)
 */
public class SaleOrder {

    private String id;
    private LocalDateTime orderDate;
    private List<SaleOrderItem> items;
    private double subtotal;
    private double discount;
    private double totalAmount;
    private String status;
    private User createdBy;

    /** สร้างเปล่า (FileStorageService ใช้ตอนโหลดจาก CSV) */
    public SaleOrder() {
        this.items = new ArrayList<>();
    }

    /** สร้างบิลใหม่พร้อมคนขาย (status เริ่ม = paid) */
    public SaleOrder(String id, User createdBy) {
        this.id = id;
        this.createdBy = createdBy;
        this.orderDate = LocalDateTime.now();
        this.status = "paid";
        this.items = new ArrayList<>();
    }

    // ==================================================================
    // จัดการรายการในบิล
    // ==================================================================

    /** เพิ่มสินค้า 1 แถวลงบิล (ถ่ายราคาปัจจุบันเก็บไว้) */
    public void addItem(Product product, int qty) {
        if (product == null || qty <= 0) return;
        String itemId = id + "-" + (items.size() + 1);
        items.add(new SaleOrderItem(itemId, product, qty, product.getPrice()));
        calculateTotal();
    }

    /** เพิ่มจากตะกร้า (ใช้ตอนชำระเงิน - แปลง CartItem เป็น SaleOrderItem) */
    public void addItem(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) return;
        addItem(cartItem.getProduct(), cartItem.getQuantity());
    }

    /** ลบแถวที่ productId ตรงกันออก (แล้วคำนวณยอดใหม่) */
    public void removeItem(String productId) {
        items.removeIf(it -> it.getProduct() != null && it.getProduct().getId().equals(productId));
        calculateTotal();
    }

    // ==================================================================
    // คำนวณยอด (แก้สูตรตรงนี้ที่เดียว)
    // ==================================================================

    /**
     * รวมก่อนหักส่วนลด = ผลรวม subtotal ทุกแถว
     * พร้อมอัปเดต totalAmount = subtotal - discount ให้ด้วย
     */
    public double calculateTotal() {
        double sum = 0;
        for (SaleOrderItem it : items) sum += it.getSubtotal();
        this.subtotal = sum;
        this.totalAmount = Math.max(0, subtotal - discount);
        return totalAmount;
    }

    /** ยอดสุทธิหลังหักส่วนลด (alias ของ calculateTotal อ่านง่ายขึ้น) */
    public double calculateNetTotal() {
        return calculateTotal();
    }

    public List<SaleOrderItem> getItems() {
        return items;
    }

    /** เป็นบิลที่ถูกยกเลิกแล้วไหม */
    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    /** รวมก่อนหักส่วนลด */
    public double getSubtotal() { return subtotal; }

    public double getDiscount() { return discount; }
    /** ตั้งส่วนลด (กันติดลบให้, แล้วคำนวณยอดใหม่) */
    public void setDiscount(double discount) {
        this.discount = Math.max(0, discount);
        calculateTotal();
    }

    /** ยอดสุทธิ (ค่าที่เก็บลง sales.csv) */
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
