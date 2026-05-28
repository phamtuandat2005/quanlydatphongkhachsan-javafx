package model.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * FieldValidationUtils — Tiện ích kiểm tra dữ liệu nhập trên form (JavaFX).
 *
 * <p>Hỗ trợ kiểm tra "rỗng / chưa chọn" và hiển thị lỗi cho:
 * <ul>
 *   <li>{@link TextInputControl}  (TextField, TextArea, PasswordField)</li>
 *   <li>{@link ComboBox}, {@link ChoiceBox}</li>
 *   <li>{@link javafx.scene.control.DatePicker} (chuẩn JavaFX)</li>
 *   <li>{@link model.utils.DatePicker} (DatePicker custom của project)</li>
 *   <li>Vùng chứa nhiều {@link CheckBox} / {@link RadioButton} (VBox, FlowPane, HBox…)</li>
 *   <li>Map ID → CheckBox (multi-select pattern)</li>
 * </ul>
 *
 * <p>Mỗi validator trả về {@code true} nếu hợp lệ, {@code false} nếu lỗi
 * (và đã tự hiển thị message lên Label + đổi border đỏ cho field).
 *
 * <h2>Cách dùng nhanh trong handleSave():</h2>
 * <pre>{@code
 * boolean ok = true;
 * if (!FieldValidationUtils.validateNgaySinh(dpNgaySinh, errNS, 16,
 *         fieldStyle(), errorStyle())) ok = false;
 * if (!FieldValidationUtils.validateRequired(cbLoaiPhong, errLoai,
 *         "Chưa chọn loại phòng.", fieldStyle(), errorStyle())) ok = false;
 * if (!FieldValidationUtils.validateAnySelected(loaiPhongCheckBoxes,
 *         loaiPhongCheckBoxArea, errLoaiPhong, "Chưa chọn loại phòng.")) ok = false;
 * if (!ok) { EventUtils.focusFirstError(...); return; }
 * }</pre>
 */
public final class FieldValidationUtils {

    private FieldValidationUtils() {
    }

    /* ══════════════════════════════════════════════════════════════════
     * BẢNG MÀU MẶC ĐỊNH (có thể override khi gọi)
     * ══════════════════════════════════════════════════════════════════ */

    public static final String DEFAULT_ERROR_COLOR  = "#dc2626";
    public static final String DEFAULT_BORDER_COLOR = "#e9ecef";
    public static final String DEFAULT_ERROR_BG     = "#fef2f2";

    /* ══════════════════════════════════════════════════════════════════
     * 1. KIỂM TRA "RỖNG / CHƯA CHỌN" — LOGIC THUẦN, KHÔNG ĐỘNG UI
     * ══════════════════════════════════════════════════════════════════ */

    /** TextField/TextArea/PasswordField: null hoặc trim rỗng → empty. */
    public static boolean isEmpty(TextInputControl tf) {
        return tf == null || tf.getText() == null || tf.getText().trim().isEmpty();
    }

    /** ComboBox: chưa chọn hoặc value là String rỗng → empty. */
    public static boolean isEmpty(ComboBox<?> cb) {
        if (cb == null || cb.getValue() == null) return true;
        Object v = cb.getValue();
        return (v instanceof String) && ((String) v).trim().isEmpty();
    }

    /** ChoiceBox: tương tự ComboBox. */
    public static boolean isEmpty(ChoiceBox<?> cb) {
        if (cb == null || cb.getValue() == null) return true;
        Object v = cb.getValue();
        return (v instanceof String) && ((String) v).trim().isEmpty();
    }

    /** DatePicker chuẩn JavaFX. */
    public static boolean isEmpty(javafx.scene.control.DatePicker dp) {
        return dp == null || dp.getValue() == null;
    }

    /** DatePicker custom của project. */
    public static boolean isEmpty(model.utils.DatePicker dp) {
        return dp == null || dp.getValue() == null;
    }

    /** Có ít nhất 1 CheckBox tick trong Map (multi-select). */
    public static boolean anySelected(Map<?, CheckBox> map) {
        if (map == null || map.isEmpty()) return false;
        return map.values().stream().anyMatch(c -> c != null && c.isSelected());
    }

    /** Có ít nhất 1 CheckBox tick trong Collection. */
    public static boolean anySelected(Collection<CheckBox> list) {
        if (list == null) return false;
        return list.stream().anyMatch(c -> c != null && c.isSelected());
    }

    /**
     * Quét đệ quy 1 vùng chứa (Pane bất kỳ — VBox, FlowPane, HBox…):
     * trả về true nếu có ít nhất 1 CheckBox hoặc RadioButton đang được chọn.
     */
    public static boolean anySelectedIn(Pane container) {
        if (container == null) return false;
        for (Node n : container.getChildren()) {
            if (n instanceof CheckBox && ((CheckBox) n).isSelected()) return true;
            if (n instanceof RadioButton && ((RadioButton) n).isSelected()) return true;
            if (n instanceof Pane && anySelectedIn((Pane) n)) return true;
        }
        return false;
    }

    /* ══════════════════════════════════════════════════════════════════
     * 2. HIỆN / XÓA STYLE LỖI
     * ══════════════════════════════════════════════════════════════════ */

    /**
     * Hiện lỗi cho field: set message + đổi style.
     *
     * @param field       node UI (có thể null)
     * @param errLabel    label hiển thị thông báo (có thể null)
     * @param msg         thông báo lỗi
     * @param errorStyle  chuỗi CSS đầy đủ áp lên field (nên có border đỏ + bg đỏ nhạt)
     */
    public static void showFieldError(Node field, Label errLabel, String msg, String errorStyle) {
        if (errLabel != null) errLabel.setText(msg);
        if (field != null && errorStyle != null) field.setStyle(errorStyle);
    }

    /** Xóa lỗi cho field: clear message + restore style. */
    public static void clearFieldError(Node field, Label errLabel, String normalStyle) {
        if (errLabel != null) errLabel.setText("");
        if (field != null && normalStyle != null) field.setStyle(normalStyle);
    }

    /**
     * Hiện lỗi cho 1 vùng chứa (Region) — chỉ thay border-color, giữ nguyên các style khác.
     * Dùng cho VBox/FlowPane chứa danh sách CheckBox như "loại phòng", "phòng cụ thể".
     */
    public static void showAreaError(Region area, Label errLabel, String msg) {
        showAreaError(area, errLabel, msg, DEFAULT_ERROR_COLOR);
    }

    public static void showAreaError(Region area, Label errLabel, String msg, String errorBorderColor) {
        if (errLabel != null) errLabel.setText(msg);
        if (area != null) {
            String base = (area.getStyle() == null ? "" : area.getStyle())
                    .replaceAll("-fx-border-color:[^;]*;", "");
            area.setStyle(base + " -fx-border-color: " + errorBorderColor + ";");
        }
    }

    public static void clearAreaError(Region area, Label errLabel) {
        clearAreaError(area, errLabel, DEFAULT_BORDER_COLOR);
    }

    public static void clearAreaError(Region area, Label errLabel, String normalBorderColor) {
        if (errLabel != null) errLabel.setText("");
        if (area != null) {
            String base = (area.getStyle() == null ? "" : area.getStyle())
                    .replaceAll("-fx-border-color:[^;]*;", "");
            area.setStyle(base + " -fx-border-color: " + normalBorderColor + ";");
        }
    }

    /* ══════════════════════════════════════════════════════════════════
     * 3. VALIDATOR KẾT HỢP (LOGIC + UI)
     *    Trả về TRUE nếu hợp lệ, FALSE nếu lỗi (đã tự hiển thị message)
     * ══════════════════════════════════════════════════════════════════ */

    /** Bắt buộc TextField/TextArea phải có dữ liệu. */
    public static boolean validateRequired(TextInputControl tf, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(tf)) {
            showFieldError(tf, err, msg, errorStyle);
            return false;
        }
        clearFieldError(tf, err, normalStyle);
        return true;
    }

    /** Bắt buộc ComboBox phải chọn 1 giá trị. */
    public static boolean validateRequired(ComboBox<?> cb, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(cb)) {
            showFieldError(cb, err, msg, errorStyle);
            return false;
        }
        clearFieldError(cb, err, normalStyle);
        return true;
    }

    /** Bắt buộc ChoiceBox phải chọn 1 giá trị. */
    public static boolean validateRequired(ChoiceBox<?> cb, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(cb)) {
            showFieldError(cb, err, msg, errorStyle);
            return false;
        }
        clearFieldError(cb, err, normalStyle);
        return true;
    }

    /** Bắt buộc DatePicker chuẩn JavaFX phải có giá trị. */
    public static boolean validateRequired(javafx.scene.control.DatePicker dp, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(dp)) {
            showFieldError(dp, err, msg, errorStyle);
            return false;
        }
        clearFieldError(dp, err, normalStyle);
        return true;
    }

    /** Bắt buộc DatePicker custom của project phải có giá trị. */
    public static boolean validateRequired(model.utils.DatePicker dp, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(dp)) {
            showFieldError(dp, err, msg, errorStyle);
            return false;
        }
        clearFieldError(dp, err, normalStyle);
        return true;
    }

    /* -- Vùng (Region) chứa danh sách CheckBox/RadioButton ---------- */

    /**
     * Bắt buộc ít nhất 1 phần tử trong vùng được tick.
     * Quét cả Pane lồng nhau (FlowPane chứa VBox chứa CheckBox).
     */
    public static boolean validateAnySelected(Region area, Label err, String msg) {
        return validateAnySelected(area, err, msg, DEFAULT_BORDER_COLOR);
    }

    public static boolean validateAnySelected(Region area, Label err, String msg, String normalBorderColor) {
        boolean hasAny = (area instanceof Pane) && anySelectedIn((Pane) area);
        if (!hasAny) {
            showAreaError(area, err, msg);
            return false;
        }
        clearAreaError(area, err, normalBorderColor);
        return true;
    }

    /**
     * Bắt buộc ít nhất 1 entry trong Map &lt;ID, CheckBox&gt; được tick.
     * Pattern này dùng cho danh sách "loại phòng" (loaiPhongCheckBoxes) hoặc
     * "phòng cụ thể" (phongCheckBoxes) trong ThemSuaDatPhongDialog.
     */
    public static boolean validateAnySelected(Map<?, CheckBox> map, Region area, Label err, String msg) {
        return validateAnySelected(map, area, err, msg, DEFAULT_BORDER_COLOR);
    }

    public static boolean validateAnySelected(Map<?, CheckBox> map, Region area, Label err, String msg,
                                              String normalBorderColor) {
        if (!anySelected(map)) {
            showAreaError(area, err, msg);
            return false;
        }
        clearAreaError(area, err, normalBorderColor);
        return true;
    }

    /* ══════════════════════════════════════════════════════════════════
     * 4. VALIDATE NGÀY SINH / KHOẢNG NGÀY
     * ══════════════════════════════════════════════════════════════════ */

    /**
     * Validate ngày sinh trên DatePicker custom: bắt buộc + đủ tuổi tối thiểu + không tương lai.
     *
     * @param minAge tuổi tối thiểu (VD: 16 cho khách hàng / nhân viên)
     */
    public static boolean validateNgaySinh(model.utils.DatePicker dp, Label err, int minAge,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(dp)) {
            showFieldError(dp, err, "Chưa chọn ngày sinh.", errorStyle);
            return false;
        }
        LocalDate ns = dp.getValue();
        if (ns.isAfter(LocalDate.now())) {
            showFieldError(dp, err, "Ngày sinh không thể ở tương lai.", errorStyle);
            return false;
        }
        if (LocalDate.now().minusYears(minAge).isBefore(ns)) {
            showFieldError(dp, err, "Phải từ đủ " + minAge + " tuổi.", errorStyle);
            return false;
        }
        clearFieldError(dp, err, normalStyle);
        return true;
    }

    /** Validate ngày sinh trên DatePicker chuẩn JavaFX. */
    public static boolean validateNgaySinh(javafx.scene.control.DatePicker dp, Label err, int minAge,
                                           String normalStyle, String errorStyle) {
        if (isEmpty(dp)) {
            showFieldError(dp, err, "Chưa chọn ngày sinh.", errorStyle);
            return false;
        }
        LocalDate ns = dp.getValue();
        if (ns.isAfter(LocalDate.now())) {
            showFieldError(dp, err, "Ngày sinh không thể ở tương lai.", errorStyle);
            return false;
        }
        if (LocalDate.now().minusYears(minAge).isBefore(ns)) {
            showFieldError(dp, err, "Phải từ đủ " + minAge + " tuổi.", errorStyle);
            return false;
        }
        clearFieldError(dp, err, normalStyle);
        return true;
    }

    /**
     * Validate khoảng ngày (2 DatePicker custom): cả 2 phải có giá trị + to phải sau from.
     * Dùng cho check-in / check-out trong ThemSuaDatPhongDialog.
     *
     * @param labelFrom tên hiển thị field bắt đầu (VD: "ngày nhận phòng")
     * @param labelTo   tên hiển thị field kết thúc (VD: "ngày trả phòng")
     */
    public static boolean validateDateRange(model.utils.DatePicker from, model.utils.DatePicker to,
                                            Label errFrom, Label errTo,
                                            String labelFrom, String labelTo,
                                            String normalStyle, String errorStyle) {
        boolean ok = true;
        if (isEmpty(from)) {
            showFieldError(from, errFrom, "Chưa chọn " + labelFrom + ".", errorStyle);
            ok = false;
        } else {
            clearFieldError(from, errFrom, normalStyle);
        }
        if (isEmpty(to)) {
            showFieldError(to, errTo, "Chưa chọn " + labelTo + ".", errorStyle);
            ok = false;
        } else if (!isEmpty(from) && !to.getValue().isAfter(from.getValue())) {
            showFieldError(to, errTo, labelTo + " phải sau " + labelFrom + ".", errorStyle);
            ok = false;
        } else {
            clearFieldError(to, errTo, normalStyle);
        }
        return ok;
    }


    /* ══════════════════════════════════════════════════════════════════
     * 5. LIVE VALIDATION / VALIDATE KHI RỜI FIELD
     *    Dùng để Tab qua field là tự hiện lỗi + bôi đỏ field.
     *    Validator truyền vào nên là method đã tự show/clear lỗi, ví dụ:
     *    () -> validateTen()
     * ══════════════════════════════════════════════════════════════════ */

    /**
     * Tạo style lỗi từ style thường.
     * Dùng chung để field lỗi có viền đỏ + nền đỏ nhạt giống nhau ở mọi dialog.
     */
    public static String errorStyle(String normalStyle) {
        return errorStyle(normalStyle, DEFAULT_ERROR_COLOR, DEFAULT_ERROR_BG);
    }

    /**
     * Tạo style lỗi từ style thường, có thể truyền màu riêng.
     */
    public static String errorStyle(String normalStyle, String errorBorderColor, String errorBgColor) {
        String base = normalStyle == null ? "" : normalStyle;
        return base
                + "-fx-border-color: " + (errorBorderColor == null ? DEFAULT_ERROR_COLOR : errorBorderColor) + ";"
                + "-fx-background-color: " + (errorBgColor == null ? DEFAULT_ERROR_BG : errorBgColor) + ";";
    }

    /**
     * Gọi validator khi field mất focus.
     *
     * <p>Dùng được cho TextField, ComboBox, DatePicker chuẩn và cả control custom.
     * Với control custom có field con bên trong, hàm này sẽ gắn listener đệ quy vào
     * các node con để trường hợp bấm Tab khỏi ô nhập bên trong vẫn validate được.
     */
    public static void validateOnFocusLost(Node field, BooleanSupplier validator) {
        if (field == null || validator == null) return;
        attachFocusLostRecursive(field, validator);
    }

    private static void attachFocusLostRecursive(Node node, BooleanSupplier validator) {
        if (node == null || validator == null) return;

        node.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (Boolean.TRUE.equals(wasFocused) && Boolean.FALSE.equals(isFocused)) {
                validator.getAsBoolean();
            }
        });

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                attachFocusLostRecursive(child, validator);
            }
        }
    }

    /**
     * Gắn live validation đầy đủ:
     * - Khi user nhập/chọn lại: xóa lỗi tạm thời.
     * - Khi user Tab/click rời field: validate và hiện lỗi nếu sai.
     *
     * <p>Validator nên là method trả boolean và tự gọi showFieldError/clearFieldError,
     * ví dụ: {@code () -> validateSDT()}.
     */
    public static void setupLiveValidation(Node field, Label err, BooleanSupplier validator, Runnable clearStyle) {
        if (field == null) return;

        validateOnFocusLost(field, validator);

        if (field instanceof TextInputControl tf) {
            autoClearOnChange(tf, err, clearStyle);
        } else if (field instanceof ComboBox<?> cb) {
            autoClearOnChange(cb, err, clearStyle);
        } else if (field instanceof ChoiceBox<?> cb) {
            autoClearOnChange(cb, err, clearStyle);
        } else if (field instanceof javafx.scene.control.DatePicker dp) {
            autoClearOnChange(dp, err, clearStyle);
        } else if (field instanceof model.utils.DatePicker dp) {
            autoClearOnChange(dp, err, clearStyle);
        }
    }

    /**
     * Gắn validate bắt buộc cho TextField/TextArea:
     * Tab khỏi field rỗng → hiện lỗi + bôi đỏ field.
     */
    public static void setupRequiredOnBlur(TextInputControl tf, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                tf, err,
                () -> validateRequired(tf, err, msg, normalStyle, errorStyle),
                () -> clearFieldError(tf, err, normalStyle)
        );
    }

    /**
     * Gắn validate bắt buộc cho ComboBox:
     * Tab khỏi field chưa chọn → hiện lỗi + bôi đỏ field.
     */
    public static void setupRequiredOnBlur(ComboBox<?> cb, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                cb, err,
                () -> validateRequired(cb, err, msg, normalStyle, errorStyle),
                () -> clearFieldError(cb, err, normalStyle)
        );
    }

    /**
     * Gắn validate bắt buộc cho ChoiceBox:
     * Tab khỏi field chưa chọn → hiện lỗi + bôi đỏ field.
     */
    public static void setupRequiredOnBlur(ChoiceBox<?> cb, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                cb, err,
                () -> validateRequired(cb, err, msg, normalStyle, errorStyle),
                () -> clearFieldError(cb, err, normalStyle)
        );
    }

    /**
     * Gắn validate bắt buộc cho DatePicker chuẩn JavaFX.
     */
    public static void setupRequiredOnBlur(javafx.scene.control.DatePicker dp, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                dp, err,
                () -> validateRequired(dp, err, msg, normalStyle, errorStyle),
                () -> clearFieldError(dp, err, normalStyle)
        );
    }

    /**
     * Gắn validate bắt buộc cho DatePicker custom của project.
     */
    public static void setupRequiredOnBlur(model.utils.DatePicker dp, Label err, String msg,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                dp, err,
                () -> validateRequired(dp, err, msg, normalStyle, errorStyle),
                () -> clearFieldError(dp, err, normalStyle)
        );
    }

    /**
     * Gắn validate ngày sinh cho DatePicker custom:
     * Tab khỏi field chưa chọn / sai tuổi / ngày tương lai → hiện lỗi + bôi đỏ.
     */
    public static void setupNgaySinhOnBlur(model.utils.DatePicker dp, Label err, int minAge,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                dp, err,
                () -> validateNgaySinh(dp, err, minAge, normalStyle, errorStyle),
                () -> clearFieldError(dp, err, normalStyle)
        );
    }

    /**
     * Gắn validate ngày sinh cho DatePicker chuẩn JavaFX.
     */
    public static void setupNgaySinhOnBlur(javafx.scene.control.DatePicker dp, Label err, int minAge,
                                           String normalStyle, String errorStyle) {
        setupLiveValidation(
                dp, err,
                () -> validateNgaySinh(dp, err, minAge, normalStyle, errorStyle),
                () -> clearFieldError(dp, err, normalStyle)
        );
    }


    /* ══════════════════════════════════════════════════════════════════
     * 6. AUTO-CLEAR ERROR — gắn listener tự xóa lỗi khi user bắt đầu nhập/chọn
     *    (giảm noise trên form, UX mượt hơn)
     * ══════════════════════════════════════════════════════════════════ */

    /** Tự động xóa lỗi khi user gõ vào TextField. */
    public static void autoClearOnChange(TextInputControl tf, Label err, Runnable clearStyle) {
        if (tf == null) return;
        tf.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.trim().isEmpty()) {
                if (err != null) err.setText("");
                if (clearStyle != null) clearStyle.run();
            }
        });
    }

    /** Tự động xóa lỗi khi user chọn ComboBox. */
    public static void autoClearOnChange(ComboBox<?> cb, Label err, Runnable clearStyle) {
        if (cb == null) return;
        cb.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                if (err != null) err.setText("");
                if (clearStyle != null) clearStyle.run();
            }
        });
    }

    /** Tự động xóa lỗi khi user chọn ChoiceBox. */
    public static void autoClearOnChange(ChoiceBox<?> cb, Label err, Runnable clearStyle) {
        if (cb == null) return;
        cb.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                if (err != null) err.setText("");
                if (clearStyle != null) clearStyle.run();
            }
        });
    }

    /** Tự động xóa lỗi khi user chọn DatePicker chuẩn JavaFX. */
    public static void autoClearOnChange(javafx.scene.control.DatePicker dp, Label err, Runnable clearStyle) {
        if (dp == null) return;
        dp.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                if (err != null) err.setText("");
                if (clearStyle != null) clearStyle.run();
            }
        });
    }

    /** Tự động xóa lỗi khi user chọn DatePicker custom. */
    public static void autoClearOnChange(model.utils.DatePicker dp, Label err, Runnable clearStyle) {
        if (dp == null) return;
        dp.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                if (err != null) err.setText("");
                if (clearStyle != null) clearStyle.run();
            }
        });
    }

    /** Tự động xóa lỗi vùng khi user tick bất kỳ CheckBox nào trong Map. */
    public static void autoClearOnAnyCheck(Map<?, CheckBox> map, Region area, Label err,
                                           String normalBorderColor) {
        if (map == null) return;
        for (CheckBox cb : map.values()) {
            if (cb == null) continue;
            cb.selectedProperty().addListener((o, ov, nv) -> {
                if (anySelected(map)) clearAreaError(area, err, normalBorderColor);
            });
        }
    }
}
