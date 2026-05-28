package model.entities;

import java.util.Objects;

/**
 * Thực thể đại diện cho bảng BangGiaDichVu_ChiTiet trong cơ sở dữ liệu.
 * Dùng để lưu trữ giá cụ thể của từng dịch vụ theo từng bảng giá.
 */
public class BangGiaDichVu_ChiTiet {
    private String maChiTietBangGia; // maChiTietBangGia (Primary Key)
    private BangGiaDichVu maBangGia;        // maBangGia (Foreign Key)
    private DichVu maDichVu;         // maDichVu (Foreign Key)
    private Double giaDichVu;        // giaDichVu
    private String donViTinh;        // donViTinh

    // Constructor không tham số
    public BangGiaDichVu_ChiTiet() {
    }

    // Constructor đầy đủ tham số
    public BangGiaDichVu_ChiTiet(String maChiTietBangGia, BangGiaDichVu maBangGia, DichVu maDichVu, 
                                Double giaDichVu, String donViTinh) {
        this.maChiTietBangGia = maChiTietBangGia;
        this.maBangGia = maBangGia;
        this.maDichVu = maDichVu;
        this.giaDichVu = giaDichVu;
        this.donViTinh = donViTinh;
    }

    // Getter và Setter
    public String getMaChiTietBangGia() {
        return maChiTietBangGia;
    }

    public void setMaChiTietBangGia(String maChiTietBangGia) {
        this.maChiTietBangGia = maChiTietBangGia;
    }

    public BangGiaDichVu getMaBangGia() {
        return maBangGia;
    }

    public void setMaBangGia(BangGiaDichVu maBangGia) {
        this.maBangGia = maBangGia;
    }

    public DichVu getMaDichVu() {
        return maDichVu;
    }

    public void setMaDichVu(DichVu maDichVu) {
        this.maDichVu = maDichVu;
    }

    public Double getGiaDichVu() {
        return giaDichVu;
    }

    public void setGiaDichVu(Double giaDichVu) {
        this.giaDichVu = giaDichVu;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        this.donViTinh = donViTinh;
    }



    // Phương thức hashCode và equals dựa trên maChiTietBangGia
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BangGiaDichVu_ChiTiet that = (BangGiaDichVu_ChiTiet) o;
        return Objects.equals(maChiTietBangGia, that.maChiTietBangGia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maChiTietBangGia);
    }

    // Phương thức toString hỗ trợ kiểm tra dữ liệu
    @Override
    public String toString() {
        return "BangGiaDichVu_ChiTiet{" +
                "maChiTietBangGia='" + maChiTietBangGia + '\'' +
                ", maDichVu='" + maDichVu + '\'' +
                ", giaDichVu=" + giaDichVu +
                ", donViTinh='" + donViTinh + '\'' +
                '}';
    }
}