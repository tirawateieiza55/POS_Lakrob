import gui.AppFont;
import gui.LoginFrame;
import service.FileStorageService;

import javax.swing.SwingUtilities;

/**
 * Main - จุดเริ่มต้นโปรแกรม POS Lakrob
 * ------------------------------------------------------------------
 * ลำดับตอนเปิดโปรแกรม (ห้ามสลับ):
 *   1) FileStorageService.getInstance() -> สร้างโฟลเดอร์ data/ +
 *      ไฟล์ users.csv/products.csv/sales.csv/sale_items.csv ถ้ายังไม่มี
 *   2) AppFont.applyGlobalDefault()     -> ตั้งฟอนต์ไทยก่อนสร้างจอ
 *      (กันตัวหนังสือเป็นสี่เหลี่ยม □□□)
 *   3) เปิด LoginFrame -> ล็อกอินผ่าน -> LoginFrame เปิด MainFrame ต่อเอง
 *
 * วิธีรัน (อ่านก่อนรันใน IDE/terminal):
 * - ไฟล์นี้อยู่ที่ src/Main.java (package เริ่มต้น) compile ด้วย:
 *     javac -encoding UTF-8 -d bin src/Main.java src/model/*.java src/service/*.java src/gui/*.java
 * - รันจาก "โฟลเดอร์รากโปรเจกต์" (ที่มี data/ อยู่) ด้วย:
 *     java -cp bin Main
 *   ห้ามรันจากใน src/ เพราะจะหา data/ ไม่เจอ
 *
 * จะแก้ครั้งหน้า:
 * - เปลี่ยนหน้าแรก: แก้บรรทัด new LoginFrame() ด้านล่าง
 * - เพิ่ม seed ข้อมูล: แก้ใน FileStorageService.ensureDataFiles()
 */
public class Main {

    public static void main(String[] args) {
        // 1) เตรียมไฟล์ข้อมูลก่อน (ใช้ singleton ตัวเดียวกับทั้งแอป)
        FileStorageService.getInstance();

        // 2) ตั้งฟอนต์ไทยเป็น default ก่อนสร้างหน้าจอทุกครั้ง
        AppFont.applyGlobalDefault();

        // 3) เปิดหน้า login ใน EDT (กฎของ Swing: สร้างจอใน invokeLater เสมอ)
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
