package model.entities;

public class ChiTietDatPhong {
    private String maCTDP;
    private Phong phong;
    private DatPhong datPhong;
    private double giaCoc;
    private int soNguoi;
    private String ghiChu;

    public ChiTietDatPhong() {
    }

    public ChiTietDatPhong(String maCTDP, Phong phong, DatPhong datPhong, double giaCoc, int soNguoi, String ghiChu) {
        this.maCTDP = maCTDP;
        this.phong = phong;
        this.datPhong = datPhong;
        this.giaCoc = giaCoc;
        this.soNguoi = soNguoi;
        this.ghiChu = ghiChu;
    }

    public String getMaCTDP() {
        return maCTDP;
    }

    public void setMaCTDP(String maCTDP) {
        this.maCTDP = maCTDP;
    }

    public Phong getPhong() {
        return phong;
    }

    public void setPhong(Phong phong) {
        this.phong = phong;
    }

    public DatPhong getDatPhong() {
        return datPhong;
    }

    public void setDatPhong(DatPhong datPhong) {
        this.datPhong = datPhong;
    }

    public double getGiaCoc() {
        return giaCoc;
    }

    public void setGiaCoc(double giaCoc) {
        this.giaCoc = giaCoc;
    }

    public int getSoNguoi() {
        return soNguoi;
    }

    public void setSoNguoi(int soNguoi) {
        this.soNguoi = soNguoi;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    @Override
    public String toString() {
        return maCTDP;
    }
}
