package model.entities;

public class LoaiDichVu {
    private String maLoaiDV;
    private String tenLoaiDV;

    public LoaiDichVu() {
    }

    public LoaiDichVu(String maLoaiDV, String tenLoaiDV) {
        this.maLoaiDV = maLoaiDV;
        this.tenLoaiDV = tenLoaiDV;
    }

    public String getMaLoaiDV() {
        return maLoaiDV;
    }

    public void setMaLoaiDV(String maLoaiDV) {
        this.maLoaiDV = maLoaiDV;
    }

    public String getTenLoaiDV() {
        return tenLoaiDV;
    }

    public void setTenLoaiDV(String tenLoaiDV) {
        this.tenLoaiDV = tenLoaiDV;
    }

    @Override
    public String toString() {
        return tenLoaiDV;
    }
}
