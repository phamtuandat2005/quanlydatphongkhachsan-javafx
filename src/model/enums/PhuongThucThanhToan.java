package model.enums;

public enum PhuongThucThanhToan {
    TIEN_MAT("Tiền mặt"),
    THE_TIN_DUNG("Thẻ tín dụng"),
    CHUYEN_KHOAN("Chuyển khoản"),
    VI_DIEN_TU("Ví điện tử");

    private final String displayName;

    PhuongThucThanhToan(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PhuongThucThanhToan fromName(String dbName) {
        for (PhuongThucThanhToan m : values()) {
            if (m.name().equals(dbName)) return m;
        }
        return TIEN_MAT;
    }
}
