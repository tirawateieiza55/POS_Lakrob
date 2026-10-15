package service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * CsvUtil - ตัวช่วยอ่าน/เขียน CSV กลางของโปรเจกต์
 * ------------------------------------------------------------------
 * ทำไมต้องแยกไฟล์นี้ออกมา?
 * - เดิมโค้ดตัด comma / ครอบ quote / แกะ quote อยู่ใน FileStorageService
 *   ทำให้ไฟล์นั้นยาว 400+ บรรทัด อ่านยาก
 * - แยกออกมาเพื่อให้ FileStorageService เหลือแค่ "จะเก็บอะไร ไฟล์ไหน"
 *   ส่วน "ตัดบรรทัดยังไงให้ไม่พัง" มาอยู่ที่นี่ที่เดียว
 *
 * วิธีแก้ครั้งหน้า (อ่านตรงนี้ก่อน):
 * - ชื่อสินค้ามี comma (เช่น "เนื้อ,หมู,ไก่,ทะเล") -> ใช้ esc() ตอนเขียน
 *   และ splitCsv() ตอนอ่าน อย่าใช้ String.split(",") ตรงๆ เด็ดขาด
 * - ไฟล์ CSV ต้องเป็น UTF-8 เสมอ (อ่าน/เขียนด้วย StandardCharsets.UTF_8)
 */
public final class CsvUtil {

    private CsvUtil() {
        // ห้าม new - เรียกแบบ CsvUtil.splitCsv(...) อย่างเดียว
    }

    // ==================================================================
    // 1) ตัดบรรทัด CSV (รองรับ quote)
    // ==================================================================
    /**
     * ตัด 1 บรรทัด CSV เป็นช่องๆ
     * ตัวอย่าง:  M001,"เนื้อ,หมู",15.0  ->  ["M001", "\"เนื้อ,หมู\"", "15.0"]
     * หมายเหตุ: ช่องที่ครอบ quote จะยังติด " ไว้ ให้ส่งต่อให้ unesc() แกะอีกที
     */
    public static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                cur.append(c);
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

    // ==================================================================
    // 2) ตอนเขียน: ครอบข้อความให้ปลอดภัย
    // ==================================================================
    /**
     * ครอบข้อความก่อนเขียนลง CSV
     * - ถ้ามี comma หรือ " ให้ครอบด้วย " และเบิ้ล " เป็น ""
     * - เช่น  เนื้อ,หมู  ->  "เนื้อ,หมู"
     */
    public static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ==================================================================
    // 3) ตอนอ่าน: แกะข้อความกลับ
    // ==================================================================
    /**
     * แกะข้อความที่อ่านจาก CSV กลับเป็นค่าปกติ
     * เช่น  "เนื้อ,หมู"  ->  เนื้อ,หมู
     */
    public static String unesc(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    // ==================================================================
    // 4) ต่อท้ายไฟล์ทีละหลายบรรทัด (กันบรรทัดติดกัน)
    // ==================================================================
    /**
     * ต่อท้ายไฟล์ ถ้าบรรทัดสุดท้ายเดิมไม่ขึ้นบรรทัดใหม่ จะเติม newline ให้ก่อน
     * กันปัญหา "Order 1Order 2" ติดกันเป็นบรรทัดเดียว
     */
    public static void appendLines(Path p, List<String> lines) throws IOException {
        if (lines == null || lines.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        if (Files.exists(p) && Files.size(p) > 0) {
            try (RandomAccessFile raf = new RandomAccessFile(p.toFile(), "r")) {
                raf.seek(raf.length() - 1);
                if (raf.read() != '\n') sb.append(System.lineSeparator());
            }
        }
        for (String l : lines) sb.append(l).append(System.lineSeparator());
        Files.write(p, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }
}
