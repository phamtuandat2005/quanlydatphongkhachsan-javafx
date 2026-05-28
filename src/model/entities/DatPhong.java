package model.entities;

import java.time.LocalDateTime;

public class DatPhong {
    private String maDat;
    private LocalDateTime ngayDat;
    private KhachHang khachHang;
    private LocalDateTime ngayCheckIn;
    private LocalDateTime ngayCheckOut;
    private int soNguoi;
    private LocalDateTime ngayThanhToan;
    private String trangThai; // CHO_XACNHAN, DA_XACNHAN, DA_CHECKIN, DA_CHECKOUT, DA_HUY

    public DatPhong() {
    }

    public DatPhong(String maDat) {
        this.maDat = maDat;
    }

    public DatPhong(String maDat, LocalDateTime ngayDat, KhachHang khachHang, LocalDateTime ngayCheckIn, LocalDateTime ngayCheckOut) {
        this.maDat = maDat;
        this.ngayDat = ngayDat;
        this.khachHang = khachHang;
        this.ngayCheckIn = ngayCheckIn;
        this.ngayCheckOut = ngayCheckOut;
        this.trangThai = "CHO_XACNHAN";
    }

    public String getMaDat() {
        return maDat;
    }

    public void setMaDat(String maDat) {
        this.maDat = maDat;
    }

    public LocalDateTime getNgayDat() {
        return ngayDat;
    }

    public void setNgayDat(LocalDateTime ngayDat) {
        this.ngayDat = ngayDat;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public LocalDateTime getNgayCheckIn() {
        return ngayCheckIn;
    }

    public void setNgayCheckIn(LocalDateTime ngayCheckIn) {
        this.ngayCheckIn = ngayCheckIn;
    }

    public LocalDateTime getNgayCheckOut() {
        return ngayCheckOut;
    }

    public void setNgayCheckOut(LocalDateTime ngayCheckOut) {
        this.ngayCheckOut = ngayCheckOut;
    }

    public int getSoNguoi() {
        return soNguoi;
    }

    public void setSoNguoi(int soNguoi) {
        this.soNguoi = soNguoi;
    }

    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    public String toString() {
        return maDat;
    }

    // --- Legacy compatibility getters ---
    public String getMaDatPhong() { return maDat; }
}
