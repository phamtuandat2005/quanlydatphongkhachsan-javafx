package model.entities;

public class ChiTietHoaDon {
    private String maCTHD;
    private HoaDon hoaDon;
    private ChiTietDatPhong chiTietDatPhong;
    private String maHD;
    private String maCTDP;
    private double thoiGianLuuTru;
    private int soLuongPhong;
    private double thanhTien;
    private double phuThu;
    private double phuPhiTraMuon;

    public ChiTietHoaDon() {
    }

    public ChiTietHoaDon(String maCTHD, HoaDon hoaDon, ChiTietDatPhong chiTietDatPhong, String maHD, String maCTDP, double thoiGianLuuTru, int soLuongPhong, double thanhTien) {
        this.maCTHD = maCTHD;
        this.hoaDon = hoaDon;
        this.chiTietDatPhong = chiTietDatPhong;
        this.maHD = maHD;
        this.maCTDP = maCTDP;
        this.thoiGianLuuTru = thoiGianLuuTru;
        this.soLuongPhong = soLuongPhong;
        this.thanhTien = thanhTien;
    }

    public String getMaCTHD() { return maCTHD; }
    public void setMaCTHD(String maCTHD) { this.maCTHD = maCTHD; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public ChiTietDatPhong getChiTietDatPhong() { return chiTietDatPhong; }
    public void setChiTietDatPhong(ChiTietDatPhong chiTietDatPhong) { this.chiTietDatPhong = chiTietDatPhong; }

    public String getMaHD() { return maHD; }
    public void setMaHD(String maHD) { this.maHD = maHD; }

    public String getMaCTDP() { return maCTDP; }
    public void setMaCTDP(String maCTDP) { this.maCTDP = maCTDP; }

    public double getThoiGianLuuTru() { return thoiGianLuuTru; }
    public void setThoiGianLuuTru(double thoiGianLuuTru) { this.thoiGianLuuTru = thoiGianLuuTru; }

    public int getSoLuongPhong() { return soLuongPhong; }
    public void setSoLuongPhong(int soLuongPhong) { this.soLuongPhong = soLuongPhong; }

    public double getThanhTien() { return thanhTien; }
    public void setThanhTien(double thanhTien) { this.thanhTien = thanhTien; }

    public double getPhuThu() { return phuThu; }
    public void setPhuThu(double phuThu) { this.phuThu = phuThu; }

    public double getPhuPhiTraMuon() { return phuPhiTraMuon; }
    public void setPhuPhiTraMuon(double phuPhiTraMuon) { this.phuPhiTraMuon = phuPhiTraMuon; }

    @Override
    public String toString() {
        return maCTHD;
    }
}
// Force rebuild
