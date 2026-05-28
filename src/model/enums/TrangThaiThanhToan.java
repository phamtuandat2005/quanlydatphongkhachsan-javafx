package model.enums;

public enum TrangThaiThanhToan {
    CHUA_THANH_TOAN("Chưa thanh toán"),
    DA_THANH_TOAN_COC("Đã thanh toán cọc"),
    DA_THANH_TOAN("Đã thanh toán xong"),
    DA_HOAN_COC("Đã hủy - hoàn cọc"),
    DA_MAT_COC("Đã hủy - mất cọc");

    private final String displayName;

    TrangThaiThanhToan(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TrangThaiThanhToan fromName(String dbName) {
        for (TrangThaiThanhToan m : values()) {
            if (m.name().equals(dbName))
                return m;
        }
        return CHUA_THANH_TOAN;
    }
}
