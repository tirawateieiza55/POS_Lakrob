import gui.AppFont;
import gui.LoginFrame;
import service.FileStorageService;
 
import javax.swing.SwingUtilities;
 
/**
 * จุดเริ่มต้นของโปรแกรม POS Lakrob
 * ลำดับ: เตรียมไฟล์ข้อมูล data/*.csv -> ตั้งฟอนต์ไทย -> เปิดหน้า Login
 * (LoginFrame จะเปิด MainFrame ให้เองเมื่อ login สำเร็จ)
 *
 * วางไฟล์นี้ที่ src/Main.java แล้วรันจากโฟลเดอร์รากของโปรเจกต์
 * เพื่อให้โฟลเดอร์ data/ ถูกพบ
 */
public class Main {
 
    public static void main(String[] args) {
        // สร้างโฟลเดอร์/ไฟล์ users.csv, products.csv, sales.csv ถ้ายังไม่มี
        new FileStorageService();
 
        // ตั้งฟอนต์ไทยเป็น default ก่อนสร้างหน้าจอ กันตัวหนังสือเป็น □□□
        AppFont.applyGlobalDefault();
 
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
 