package model.entities;

import java.time.LocalDate;

public class KhachHang {
    private String maKH;
    private String tenKH;
    private String soCCCD;
    private boolean daXoa;
    private String soDT;
    private LocalDate ngaySinh;

    // ─── Constructors ────────────────────────────────────────────────────────
    public KhachHang() {}

    public KhachHang(String maKH) {
        this.maKH = maKH;
    }

    public KhachHang(String maKH, String tenKH, String soCCCD, String soDT) {
        this.maKH = maKH;
        this.tenKH  = tenKH;
        this.soCCCD   = soCCCD;
        this.soDT = soDT;
    }

    public KhachHang(String maKH, String tenKH, String soCCCD,
                     String soDT, LocalDate ngaySinh) {
        this(maKH, tenKH, soCCCD, soDT);
        this.ngaySinh = ngaySinh;
    }

    public KhachHang(String tenKH, String soCCCD, String soDT, LocalDate ngaySinh) {
        this.tenKH = tenKH;
        this.soCCCD = soCCCD;
        this.soDT = soDT;
        this.ngaySinh = ngaySinh;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    /** Hôm nay có phải sinh nhật không */
    public boolean isBirthdayToday() {
        if (ngaySinh == null) return false;
        LocalDate today = LocalDate.now();
        return ngaySinh.getDayOfMonth() == today.getDayOfMonth()
            && ngaySinh.getMonthValue()  == today.getMonthValue();
    }

    /** Sinh nhật có trong tháng hiện tại không */
    public boolean isBirthdayThisMonth() {
        if (ngaySinh == null) return false;
        return ngaySinh.getMonthValue() == LocalDate.now().getMonthValue();
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────
    public String getMaKH()             { return maKH; }
    public void   setMaKH(String v)     { this.maKH = v; }

    public String getTenKH()            { return tenKH; }
    public void   setTenKH(String v)    { this.tenKH = v; }

    public String getSoCCCD()           { return soCCCD; }
    public void setSoCCCD(String soCCCD) {
        this.soCCCD = soCCCD;
    }

    public boolean isDaXoa() {
        return daXoa;
    }

    public void setDaXoa(boolean daXoa) {
        this.daXoa = daXoa;
    }

    public String getSoDT()             { return soDT; }
    public void   setSoDT(String v)     { this.soDT = v; }

    public LocalDate getNgaySinh()      { return ngaySinh; }
    public void       setNgaySinh(LocalDate v) { this.ngaySinh = v; }
}