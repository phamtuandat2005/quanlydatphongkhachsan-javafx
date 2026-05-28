package model.entities;

public class LoaiPhong_TienNghi {
    private String maLoaiPhong;
    private String maTienNghi;
    private int soLuong;

    public LoaiPhong_TienNghi(String maLoaiPhong, String maTienNghi, int soLuong) {
        this.maLoaiPhong = maLoaiPhong;
        this.maTienNghi = maTienNghi;
        this.soLuong = soLuong;
    }

    public LoaiPhong_TienNghi(String maLoaiPhong, String maTienNghi) {
        this.maLoaiPhong = maLoaiPhong;
        this.maTienNghi = maTienNghi;
        this.soLuong = 1;
    }

    public String getMaLoaiPhong() {
        return maLoaiPhong;
    }

    public void setMaLoaiPhong(String maLoaiPhong) {
        this.maLoaiPhong = maLoaiPhong;
    }

    public String getMaTienNghi() {
        return maTienNghi;
    }

    public void setMaTienNghi(String maTienNghi) {
        this.maTienNghi = maTienNghi;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

}
