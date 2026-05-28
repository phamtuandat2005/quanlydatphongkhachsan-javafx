package model.enums;

public enum DonViTinh {
    CAI("Cái"),
    NGAY("Ngày"),
    LUOT("Lượt"),
    CHAI("Chai"),   // Thêm cho đồ uống Minibar
    LON("Lon"),     // Thêm cho đồ uống Minibar
    DIA("Dĩa");     // Thêm cho Room Dining

    private final String label;

    DonViTinh(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    // Hàm hỗ trợ chuyển đổi từ String (DB) sang Enum an toàn
    public static DonViTinh fromString(String text) {
        for (DonViTinh b : DonViTinh.values()) {
            if (b.label.equalsIgnoreCase(text) || b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return CAI; // Mặc định nếu không tìm thấy
    }
}