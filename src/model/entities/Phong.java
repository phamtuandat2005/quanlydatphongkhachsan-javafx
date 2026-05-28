package model.entities;

import java.util.List;
import model.enums.TrangThaiPhong;
import model.utils.IdGenerator;

public class Phong {
    private String maPhong;
    private String tenPhong;
    private LoaiPhong loaiPhong;
    private TrangThaiPhong tinhTrang;
    private int soPhong;
    private int soTang;
    private boolean daXoa;

    public Phong() {
    }

    public Phong(String maPhong, String tenPhong, LoaiPhong loaiPhong, TrangThaiPhong tinhTrang, int soPhong, int soTang) {
        this.maPhong = maPhong;
        this.tenPhong = tenPhong;
        this.loaiPhong = loaiPhong;
        this.tinhTrang = tinhTrang;
        this.soPhong = soPhong;
        this.soTang = soTang;
    }

    public Phong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getTenPhong() {
        return tenPhong;
    }

    public void setTenPhong(String tenPhong) {
        this.tenPhong = tenPhong;
    }

    public LoaiPhong getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(LoaiPhong loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public TrangThaiPhong getTinhTrang() {
        return tinhTrang;
    }

    public void setTinhTrang(TrangThaiPhong tinhTrang) {
        this.tinhTrang = tinhTrang;
    }

    public int getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(int soPhong) {
        this.soPhong = soPhong;
    }

    public int getSoTang() {
        return soTang;
    }

    public void setSoTang(int soTang) {
        this.soTang = soTang;
    }

    public boolean isDaXoa() {
        return daXoa;
    }

    public void setDaXoa(boolean daXoa) {
        this.daXoa = daXoa;
    }

    @Override
    public String toString() {
        return tenPhong != null ? tenPhong : maPhong;
    }

    public TrangThaiPhong getTrangThai() { return tinhTrang; }
    public void setTrangThai(TrangThaiPhong tt) { this.tinhTrang = tt; }

    /**
     * Tự động phát sinh mã phòng theo tầng (Ví dụ: Tầng 2 -> P201, P202..., P2100)
     */
    public static String phatSinhMaPhong(int floor, List<Phong> existingRooms) {
        for (int i = 0; i < 1000; i++) {
            String candidate = IdGenerator.randomId("P", 6);
            boolean exists = false;
            for (Phong p : existingRooms) {
                if (p.getMaPhong().equals(candidate)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) return candidate;
        }
        return null;
    }
}