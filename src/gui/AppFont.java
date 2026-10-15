package gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * AppFont - ตัวช่วยฟอนต์กลางของแอป (กันภาษาไทยเป็น □□□)
 * ------------------------------------------------------------------
 * ปัญหา: ฟอนต์บางตัว (เช่น Monospaced / default บางเครื่อง) ไม่มี glyph ไทย
 * วิธีแก้: thai() จะไล่หา Tahoma -> Leelawadee -> Segoe UI ... ตัวไหนแสดงไทยได้ใช้อันนั้น
 *
 * จะแก้ครั้งหน้า:
 * - เปลี่ยนฟอนต์หลัก: แก้ลิสต์ prefer ใน thai()
 * - ฟอนต์ใบเสร็จ: ใช้ monoThai() (อยากได้ monospace แต่ต้องแสดงไทยได้)
 * - เรียก applyGlobalDefault() ครั้งเดียวตอนเริ่มโปรแกรม (ใน Main)
 */
public final class AppFont {

    private AppFont() {}

    /** ข้อความตัวอย่างสำหรับเช็คว่าฟอนต์แสดงไทยได้ */
    private static final String THAI_SAMPLE =
        "กขคงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮะาิีึืุูเแโำไใๅๆ็่้๊๋์0123456789";

    private static boolean canThai(Font f) {
        try {
            return f != null && f.canDisplayUpTo(THAI_SAMPLE) == -1;
        } catch (Exception e) {
            return false;
        }
    }

    /** ฟอนต์ทั่วไปที่แสดงไทยได้ (优先 Tahoma ตามธีมเดิม) */
    public static Font thai(int style, int size) {
        String[] prefer = {"Tahoma", "Leelawadee UI", "Segoe UI", "Cordia New", "Angsana New"};
        for (String name : prefer) {
            Font f = new Font(name, style, size);
            if (canThai(f)) return f;
        }
        Font f = new Font(Font.SANS_SERIF, style, size);
        if (canThai(f)) return f;
        // ทางสุดท้าย: ไล่หาทุกฟอนต์ในเครื่อง
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()) {
                Font c = new Font(name, style, size);
                if (canThai(c)) return c;
            }
        } catch (Exception ignored) {}
        return f;
    }

    /**
     * ฟอนต์สำหรับใบเสร็จ: อยากได้แบบความกว้างคงที่ (monospace) เพื่อให้คอลัมน์
     * ตรงกัน แต่ต้องแสดงไทยได้ด้วย ถ้าเครื่องไหนไม่มี monospace ไทยก็ fallback
     * เป็นฟอนต์ไทยปกติ (เห็นไทยชัวร์ แถวอาจเยื้องนิดหน่อย)
     */
    public static Font monoThai(int size) {
        String[] prefer = {"Courier New", "Consolas", "Lucida Console",
            "DejaVu Sans Mono", Font.MONOSPACED};
        for (String name : prefer) {
            Font f = new Font(name, Font.PLAIN, size);
            if (canThai(f)) return f;
        }
        return thai(Font.PLAIN, size);
    }

    /**
     * ตั้งฟอนต์ไทยเป็นค่า default ของ Swing ทั้งแอป เพื่อกันปุ่ม/dialog/ตาราง
     * จุดไหนที่ลืม setFont ไว้แล้วไปโดนฟอนต์เครื่องที่ไม่มีไทย
     * ต้องเรียกก่อนสร้างหน้าจอ (ต้น main)
     */
    public static void applyGlobalDefault() {
        try {
            FontUIResource f = new FontUIResource(thai(Font.PLAIN, 14));
            String[] keys = {
                "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
                "Label.font", "TextField.font", "PasswordField.font", "TextArea.font",
                "TextPane.font", "EditorPane.font", "ComboBox.font", "List.font",
                "Table.font", "TableHeader.font", "Menu.font", "MenuItem.font",
                "OptionPane.messageFont", "OptionPane.buttonFont",
                "ToolTip.font", "TabbedPane.font", "Tree.font"
            };
            for (String k : keys) UIManager.put(k, f);
        } catch (Exception ignored) {}
    }
}
