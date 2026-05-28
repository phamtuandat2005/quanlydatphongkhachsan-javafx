package model.enums;

public enum TrangThaiDatPhong {
    DA_XACNHAN("Đã xác nhận"),
    DA_CHECKIN("Đã nhận phòng"),
    DA_CHECKOUT("Đã trả phòng"),
    DA_HUY("Đã hủy"),

    HUY_HOAN_COC("Hủy - Hoàn cọc"),
    HUY_MAT_COC("Hủy - Mất cọc");

    private final String thongTinTrangThai;

    TrangThaiDatPhong(String thongTinTrangThai) {
        this.thongTinTrangThai = thongTinTrangThai;
    }

    public String getThongTinTrangThai() {
        return thongTinTrangThai;
    }
}
