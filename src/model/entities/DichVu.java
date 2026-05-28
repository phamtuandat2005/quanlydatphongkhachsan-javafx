package model.entities;

import java.util.Objects;

/**
 * Class thực thể đại diện cho bảng DV trong Database LuciaHT
 */
public class DichVu {
    public static final int DANG_PHUC_VU = 0;
    public static final int TAM_NGUNG = 1;

    private String maDV;
    private String tenDV;
    private Double gia;
    private String loaiDV; // Lưu maLoaiDV
    private String tenLoaiDV;
    private String mieuTa;
    private String donVi;
    private int trangThai; // 0: DANG_PHUC_VU, 1: TAM_NGUNG

    public String getTrangThaiLabel() {
        return trangThai == DANG_PHUC_VU ? "Đang phục vụ" : "Tạm ngưng phục vụ";
    }

    public DichVu() {
    }

    public DichVu(String maDV) {
        this.maDV = maDV;
    }

    public DichVu(String maDV, String tenDV, Double gia, String loaiDV, String mieuTa, String donVi, int trangThai) {
        this.maDV = maDV;
        this.tenDV = tenDV;
        this.gia = gia;
        this.loaiDV = loaiDV;
        this.mieuTa = mieuTa;
        this.donVi = donVi;
        this.trangThai = trangThai;
    }

    public String getMaDV() {
        return maDV;
    }

    public void setMaDV(String maDV) {
        this.maDV = maDV;
    }

    public String getTenDV() {
        return tenDV;
    }

    public void setTenDV(String tenDV) {
        this.tenDV = tenDV;
    }

    public Double getGia() {
        return gia;
    }

    public void setGia(Double gia) {
        this.gia = gia;
    }

    public String getLoaiDV() {
        return loaiDV;
    }

    /**
     * Trả về tên hiển thị (Tiếng Việt) của loại dịch vụ
     */
    public String getTenLoaiDV() {
        return tenLoaiDV != null ? tenLoaiDV : "Chưa phân loại";
    }

    public void setTenLoaiDV(String tenLoaiDV) {
        this.tenLoaiDV = tenLoaiDV;
    }

    public void setLoaiDV(String loaiDV) {
        this.loaiDV = loaiDV;
    }

    public String getMieuTa() {
        return mieuTa;
    }

    public void setMieuTa(String mieuTa) {
        this.mieuTa = mieuTa;
    }

    public String getDonVi() {
        return donVi;
    }

    public void setDonVi(String donVi) {
        this.donVi = donVi;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DichVu dichVu = (DichVu) o;
        return Objects.equals(maDV, dichVu.maDV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDV);
    }

    @Override
    public String toString() {
        return tenDV != null ? tenDV : maDV;
    }

    // --- Legacy compatibility ---
    public String getMaDichVu() { return maDV; }
    public String getTenDichVu() { return tenDV; }
    public Double getDonGia() { return gia; }
    public Double getGiaDichVu() { return gia; }
    public String getLoaiDichVu() { return loaiDV; }
    public String getDonViTinh() { return donVi; }
}