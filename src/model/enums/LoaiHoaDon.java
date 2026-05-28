package model.enums;

public enum LoaiHoaDon {
    HOA_DON_PHONG("Hóa đơn phòng"),
    HOA_DON_HOAN_TIEN("Hóa đơn hoàn tiền");

    private final String displayName;

    LoaiHoaDon(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LoaiHoaDon fromName(String dbName) {
        for (LoaiHoaDon m : values()) {
            if (m.name().equals(dbName)) return m;
        }
        return HOA_DON_PHONG;
    }
}
