package model.enums;

public enum TrangThaiPhong {
    CONTRONG("Còn trống"),
    BAN("Bẩn"),
    DACOKHACH("Đã có khách"),
    BAOTRI("Bảo trì");

    private String label;

    TrangThaiPhong(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }

    // Thêm hàm này để lấy chuỗi tiếng Việt nếu cần
    public String getLabel() {
        return label;
    }
}