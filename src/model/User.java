package model;

/**
 * User - ผู้ใช้ระบบ (Domain Model)
 * ------------------------------------------------------------------
 * ฟิลด์: id, username, password, role (เช่น Admin / พนักงานขาย)
 * - ไฟล์จริง: data/users.csv
 * - ใช้ตอน: LoginFrame.authenticate() -> ได้ User -> ส่งต่อให้ MainFrame
 *   เพื่อเช็คสิทธิ์ (isAdmin) และบันทึกว่าใครขาย (SaleOrder.createdBy)
 *
 * จะแก้ครั้งหน้า:
 * - เพิ่ม role ใหม่: แก้แค่ isAdmin() + getDisplayRole() ด้านล่าง
 * - อย่าเก็บ password แบบ plain text ในงานจริง (ตอนนี้เป็น CSV งานเรียน
 *   เลยเก็บตรงๆ ไว้ก่อน)
 */
public class User {

    private String id;
    private String username;
    private String password;
    private String role;

    /** constructor เปล่า: มีไว้ให้ตอนโหลด CSV */
    public User() {
    }

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /** มี role ระบุไว้ไหม (กัน user ผีที่ role ว่าง) */
    public boolean checkPermission() {
        return role != null && !role.isBlank();
    }

    /**
     * เป็นแอดมินไหม (เข้าได้เฉพาะหน้าสต็อก)
     * เทียบแบบตรงตัวเท่านั้น กัน role ที่มีคำว่า admin ปนอยู่ได้สิทธิ์โดยไม่ตั้งใจ
     * จะเพิ่มชื่อ role แอดมินแบบใหม่ ให้เพิ่มใน list ตรงนี้
     */
    public boolean isAdmin() {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return r.equals("admin") || r.equals("ผู้จัดการ") || r.equals("ผู้จัดการร้าน")
                || r.equals("เจ้าของ") || r.equals("เจ้าของร้าน");
    }

    /** เช็ครหัสผ่าน (เทียบตรงตัว) */
    public boolean checkPassword(String password) {
        if (password == null) return false;
        return password.equals(this.password);
    }

    public String getRole() {
        return role;
    }

    /** ชื่อ role ภาษาไทยโชว์ใน sidebar (admin -> ผู้จัดการร้าน) */
    public String getDisplayRole() {
        if (isAdmin()) return "ผู้จัดการร้าน";
        if (role == null || role.isBlank()) return "พนักงานขาย";
        return role;
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public void setRole(String role) { this.role = role; }
}
