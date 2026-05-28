package model.entities;

public class LoaiPhong {
    private String maLoaiPhong;
    private double gia;
    private int sucChua;

    public LoaiPhong() {
    }

    public LoaiPhong(String maLoaiPhong, double gia, int sucChua) {
        this.maLoaiPhong = maLoaiPhong;
        this.gia = gia;
        this.sucChua = sucChua;
    }

    public LoaiPhong(String maLoaiPhong) {
        this.maLoaiPhong = maLoaiPhong;
    }

    public String getMaLoaiPhong() {
        return maLoaiPhong;
    }

    public void setMaLoaiPhong(String maLoaiPhong) {
        this.maLoaiPhong = maLoaiPhong;
    }

    public double getGia() {
        return gia;
    }

    public void setGia(double gia) {
        this.gia = gia;
    }

    public int getSucChua() {
        return sucChua;
    }

    public void setSucChua(int sucChua) {
        this.sucChua = sucChua;
    }

    @Override
    public String toString() {
        try {
            return model.enums.TenLoaiPhong.valueOf(maLoaiPhong).getDisplayName();
        } catch (Exception e) {
            return maLoaiPhong;
        }
    }

    // --- Legacy compatibility ---
    public String getMaLoai() { return maLoaiPhong; }
    public String getTenLoai() { return maLoaiPhong; }
    public double getGiaPerNgay() { return gia; }
}