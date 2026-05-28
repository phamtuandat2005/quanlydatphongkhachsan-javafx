package model.utils;

import java.util.Set;

/**
 * Lớp tiện ích chứa toàn bộ Regex và logic kiểm tra dữ liệu đầu vào.
 * Tất cả đều là static để có thể gọi trực tiếp từ mọi class khác.
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * 1. CÁC HẰNG SỐ REGEX
     * ══════════════════════════════════════════════════════════════════
     */

    /**
     * HỌ VÀ TÊN (ĐÃ SỬA):
     * - Cho phép chữ cái (Unicode — hỗ trợ tiếng Việt, tên quốc tế).
     * - Cho phép khoảng trắng.
     * - Cho phép dấu nháy đơn ' (O'Brien).
     * - Cho phép dấu gạch nối - (Jean-Paul — tên Pháp).
     * - Cho phép dấu chấm . (Mr. John, tên nước ngoài).
     * - KHÔNG cho phép 2 ký tự đặc biệt liên tiếp (--, .., .-, -., ''..)
     *   → Dùng isValidNameStructure() để kiểm tra bổ sung sau khi matches.
     */
    public static final String REGEX_NAME = "^[\\p{L}\\s\\.\\'-]+$";

    /**
     * Regex bắt 2 ký tự đặc biệt liên tiếp (dùng cho validate tên).
     * Ký tự đặc biệt: . - ' (dấu chấm, gạch nối, nháy đơn)
     */
    public static final String REGEX_CONSECUTIVE_SPECIAL_CHARS = ".*[\\.\\-'][\\.\\-'].*";

    /**
     * CHỐNG LẶP KÝ TỰ BẤT THƯỜNG:
     * - Chặn chuỗi có 3 ký tự giống nhau liên tiếp (VD: "Vănnn", "Hooo").
     */
    public static final String REGEX_SPAM_CHAR = ".*(.)\\1{2,}.*";

    /**
     * SỐ ĐIỆN THOẠI DI ĐỘNG (Chuẩn Việt Nam):
     * - Di động (10 số): 03x, 05x, 07x, 08x, 09x
     */
    public static final String REGEX_PHONE_VN = "^(03|05|07|08|09)\\d{8}$";

    /**
     * CĂN CƯỚC CÔNG DÂN (CCCD): Đúng 12 chữ số.
     */
    public static final String REGEX_CCCD_FORMAT = "^\\d{12}$";

    /*
     * ══════════════════════════════════════════════════════════════════
     * 2. CÁC HÀM KIỂM TRA LOGIC
     * ══════════════════════════════════════════════════════════════════
     */

    /**
     * Kiểm tra độ dài Họ và Tên (Tối thiểu 2 từ).
     */
    public static boolean isValidNameLength(String ten) {
        if (ten == null || ten.trim().isEmpty())
            return false;
        String[] parts = ten.trim().split("\\s+");
        if (parts.length < 2)
            return false;
        return parts[0].length() >= 1 && parts[parts.length - 1].length() >= 1;
    }

    /**
     * [MỚI] Kiểm tra cấu trúc tên hợp lệ — không có 2 ký tự đặc biệt liên tiếp,
     * không bắt đầu/kết thúc bằng ký tự đặc biệt.
     *
     * Các trường hợp CHẶN:
     *   - "Jean--Paul"   (2 gạch nối liền)
     *   - "Mr..John"     (2 chấm liền)
     *   - "A.-B"         ( chấm rồi gạch)
     *   - "-Nam"         (bắt đầu bằng gạch)
     *   - "Nam."         (kết thúc bằng chấm)
     *
     * Các trường hợp CHẤP NHẬN:
     *   - "Nguyễn Văn A"
     *   - "Jean-Paul Sartre"
     *   - "Mr. John"
     *   - "O'Brien"
     *   - "Ho Chi Minh"
     *   - "Jean-Pierre O'Neil"
     */
    public static boolean isValidNameStructure(String ten) {
        if (ten == null) return false;
        String t = ten.trim();
        if (t.isEmpty()) return false;

        // Không cho bắt đầu/kết thúc bằng ký tự đặc biệt
        char first = t.charAt(0);
        char last = t.charAt(t.length() - 1);
        if (first == '.' || first == '-' || first == '\'') return false;
        if (last == '-' || last == '\'') return false;
        // Ghi chú: dấu chấm cuối tên có thể chấp nhận cho viết tắt (VD: "John Jr.")
        // => Mình cho phép kết thúc bằng dấu chấm

        // Không cho 2 ký tự đặc biệt liên tiếp (cân nhắc cả khoảng trắng ở giữa:
        // "Jean - Paul" với khoảng trắng 2 bên gạch vẫn OK vì không liên tiếp)
        if (t.matches(REGEX_CONSECUTIVE_SPECIAL_CHARS)) return false;

        // Không cho ký tự đặc biệt sát khoảng trắng (VD: "A .B" hoặc "A- B")
        // Tuy nhiên chấp nhận "Mr. John" → dấu chấm đứng trước khoảng trắng.
        // Không chặn "- " hoặc " -" vì đây là ý đồ viết thường gặp,
        // chỉ chặn các cặp đặc biệt-đặc biệt liền nhau như "--", "..", ".-"...

        return true;
    }

    /**
     * Kiểm tra mã tỉnh trên CCCD có hợp lệ hay không.
     */
    private static final Set<String> PROVINCE_CODES = Set.of(
            "001", "002", "004", "006", "008", "010", "011", "012", "014", "015",
            "017", "019", "020", "022", "024", "025", "026", "027", "030", "031",
            "033", "034", "035", "036", "037", "038", "040", "042", "044", "045",
            "046", "048", "049", "051", "052", "054", "056", "058", "060", "062",
            "064", "066", "067", "068", "070", "072", "074", "075", "077", "079",
            "080", "082", "083", "084", "086", "087", "089", "091", "092", "093",
            "094", "095", "096");

    public static boolean isValidProvinceCode(String cccd) {
        if (cccd == null || !cccd.matches(REGEX_CCCD_FORMAT)) {
            return false;
        }
        return PROVINCE_CODES.contains(cccd.substring(0, 3));
    }

    public static boolean isValidCCCDCenturyAndGender(String cccd, int namSinh) {
        if (cccd == null || cccd.length() < 4)
            return false;

        int theKyGioiTinh = cccd.charAt(3) - '0';

        if (namSinh >= 1900 && namSinh <= 1999)
            return (theKyGioiTinh == 0 || theKyGioiTinh == 1);
        if (namSinh >= 2000 && namSinh <= 2099)
            return (theKyGioiTinh == 2 || theKyGioiTinh == 3);
        return false;
    }

    public static boolean isValidCCCDBirthYear(String cccd, int namSinh) {
        if (cccd == null || cccd.length() < 6)
            return false;

        String maNamSinh = cccd.substring(4, 6);
        String maNamSinhThucTe = String.format("%02d", namSinh % 100);

        return maNamSinh.equals(maNamSinhThucTe);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * 3. HÀM CHUẨN HÓA DỮ LIỆU
     * ══════════════════════════════════════════════════════════════════
     */

    public static String toTitleCase(String input) {
        if (input == null || input.trim().isEmpty())
            return "";

        input = input.trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;

        for (char ch : input.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                nextUpper = true;
                sb.append(ch);
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(ch));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * 4. HÀM TIỆN ÍCH GIAO DIỆN
     * ══════════════════════════════════════════════════════════════════
     */

    public static void applyNumericOnlyFilter(javafx.scene.control.TextField textField, int maxLength) {
        if (textField == null)
            return;

        javafx.scene.control.TextFormatter<String> formatter = new javafx.scene.control.TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String newText = change.getControlNewText();
            if (newText.isEmpty() || (newText.matches("\\d+") && newText.length() <= maxLength)) {
                return change;
            }

            return null;
        });

        textField.setTextFormatter(formatter);
    }

    public static boolean isCCCDInBlacklist(String cccd) {
        if (cccd == null || cccd.length() != 12)
            return false;

        if (cccd.matches("^(\\d)\\1{11}$"))
            return true;

        String ascending = "01234567890123456789";
        String descending = "98765432109876543210";
        if (ascending.contains(cccd) || descending.contains(cccd))
            return true;

        return false;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * 5. HÀM KIỂM TRA TRÙNG LẶP SĐT / CCCD TRONG DB
     * ══════════════════════════════════════════════════════════════════
     */

    public static boolean isDuplicateSDT(String sdt, String excludeId) {
        if (sdt == null || sdt.isBlank()) return false;
        String sql = "SELECT maNV as id FROM NV WHERE soDT = ? UNION ALL " +
                     "SELECT maKH as id FROM KH WHERE soDT = ?";
        try (java.sql.Connection con = connectDatabase.ConnectDatabase.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sdt);
            ps.setString(2, sdt);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String existingId = rs.getString("id");
                    if (existingId != null && !existingId.trim().equals(excludeId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isDuplicateCCCD(String cccd, String excludeId) {
        if (cccd == null || cccd.isBlank()) return false;
        String sql = "SELECT maNV as id FROM NV WHERE soCCCD = ? UNION ALL " +
                     "SELECT maKH as id FROM KH WHERE soCCCD = ?";
        try (java.sql.Connection con = connectDatabase.ConnectDatabase.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cccd);
            ps.setString(2, cccd);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String existingId = rs.getString("id");
                    if (existingId != null && !existingId.trim().equals(excludeId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}