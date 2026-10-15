package service;

import java.util.List;

/**
 * Service<T> - คลาสแม่แบบของ "งานธุรกิจ" ทุกตัวในระบบ
 * ------------------------------------------------------------------
 * อ่านยังไง:
 * - <T> คือชนิดข้อมูลที่ service นั้นดูแล เช่น Service<Product>,
 *   Service<User>, Service<SaleOrder>
 * - เดิมเป็น save(Object)/load() ที่คืน Object ต้อง cast เอง เสี่ยงพัง
 *   ตอนนี้เป็น generic แล้ว IDE จะเตือนชนิดให้ตั้งแต่ตอนพิมพ์
 *
 * จะเพิ่ม service ใหม่ต้องทำยังไง:
 * 1) สร้างคลาสใหม่ extends Service<ชนิดข้อมูล> เช่น
 *      public class ProductService extends Service<Product> { ... }
 * 2) implement 3 เมธอด save / loadAll / delete ให้ครบ
 * 3) ข้างในเรียก FileStorageService.getInstance() เพื่ออ่านเขียนไฟล์จริง
 *    (FileStorageService = ชั้นเก็บไฟล์, Service<T> = ชั้นธุรกิจ)
 *
 * หมายเหตุ: FileStorageService ไม่ได้ extends คลาสนี้โดยตรง เพราะมัน
 * ดูแลหลายตาราง (users/products/sales) ในตัวเดียว เลยเป็น singleton
 * ให้ service ย่อยเรียกใช้แทน
 */
public abstract class Service<T> {

    /** เก็บหรืออัปเดตข้อมูล 1 ตัว (ถ้า id ซ้ำ = อัปเดต, ถ้าใหม่ = เพิ่ม) */
    public abstract void save(T data);

    /** โหลดทั้งหมด (คืน list ว่างถ้ายังไม่มีข้อมูล ไม่คืน null) */
    public abstract List<T> loadAll();

    /** ลบด้วย id (ถ้าไม่เจอให้เงียบไว้ ไม่ต้อง throw) */
    public abstract void delete(String id);
}
