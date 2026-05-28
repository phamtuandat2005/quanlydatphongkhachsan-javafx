package gui;

import dao.NhanVienDAO;
import model.entities.NhanVien;
import model.enums.ChucVu;
import model.enums.trinhDo;
import model.enums.TrangThaiNV;
import model.utils.DatePicker;
import model.utils.DimOverlay;
import model.utils.EventUtils;
import model.utils.FieldValidationUtils;
import model.utils.ValidationUtils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * ThemSuaNhanVienDialog – JavaFX.
 *
 * Quy tắc phân quyền:
 * ─ ADMIN : Có thể thêm cả Quản lý lẫn Nhân viên. Sửa/xóa tất cả.
 * ─ QUAN_LY : Khi THÊM → chức vụ gán cứng "Nhân viên" (không ComboBox).
 * Khi SỬA → chức vụ readonly.
 * ─ Khi chọn QUAN_LY làm chức vụ mới: trình độ phải là DAIHOC hoặc TREN_DAIHOC.
 */
public class ThemSuaNhanVienDialog extends Stage {

    /* ── Bảng màu ─────────────────────────────────────────────────── */
    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_ERROR = "#dc2626";
    private static final String C_ERROR_BG = "#fef2f2";
    private static final String C_SUCCESS = "#16a34a";

    /* ── State ────────────────────────────────────────────────────── */
    private double xOffset = 0;
    private double yOffset = 0;

    private final Window owner;
    private final NhanVienDAO dao = new NhanVienDAO();
    private final NhanVien nvEdit; // null = thêm mới
    private final NhanVien currentUser; // người đang đăng nhập
    private final gui.QuanLyNhanVienView parentView; // để reload
    private final boolean isAdmin;
    /** Có phải ADMIN không (toàn quyền) */

    private Region overlay;

    /* ── Form fields ──────────────────────────────────────────────── */
    private TextField txtTen, txtSDT, txtCCCD, txtDiaChi;
    private ComboBox<String> cbChucVu;
    private ComboBox<trinhDo> cbTrinhDo;
    private ComboBox<TrangThaiNV> cbTrangThai;
    private ComboBox<NhanVien> cbNguoiQuanLy;
    private Button btnSave;

    private DatePicker dpNgaySinh;
    private Label errTen, errSDT, errCCCD, errDiaChi, errNS;
    private Label errTrinhDo, errChucVu, errTrangThai, errNguoiQuanLy;
    private VBox nguoiQuanLyBox;

    /* ── Constructor ──────────────────────────────────────────────── */
    public ThemSuaNhanVienDialog(Window owner, NhanVien nvEdit, NhanVien currentUser,
            gui.QuanLyNhanVienView parentView) {
        this.owner = owner;
        this.nvEdit = nvEdit;
        this.currentUser = currentUser;
        this.parentView = parentView;
        this.isAdmin = (currentUser != null && currentUser.getRole() == ChucVu.ADMIN);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(buildRoot(), 580, 700);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        setScene(scene);
        centerOnScreen();

        initEvents();

        Platform.runLater(() -> {
            if (txtTen != null) {
                txtTen.requestFocus();
                txtTen.positionCaret(txtTen.getText().length());
            }
        });
    }

    public void showDialog() {
        this.overlay = DimOverlay.show(owner);
        showAndWait();
        DimOverlay.hide(owner, this.overlay);
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * SỰ KIỆN LOGIC (EventUtils)
     * ════════════════════════════════════════════════════════════════
     */
    private void initEvents() {
        // 1. Nhấn Enter để Thêm mới / Cập nhật luôn (Tab chuyển ô tự động của JavaFX)
        EventUtils.setupEnterToSave(() -> {
            if (btnSave != null && !btnSave.isDisabled()) {
                handleSave();
            }
        }, txtTen, txtDiaChi, txtSDT, txtCCCD, dpNgaySinh, cbTrinhDo, cbChucVu, cbTrangThai, cbNguoiQuanLy);

        // 2. Theo dõi form (Dirty Tracking): Tự động khóa/mở nút Cập nhật
        if (nvEdit != null && btnSave != null) {
            if (isAdmin) {
                EventUtils.setupDirtyTracking(btnSave, txtTen, txtDiaChi, txtSDT, txtCCCD, dpNgaySinh, cbTrinhDo,
                        cbChucVu, cbTrangThai, cbNguoiQuanLy);
            } else {
                // Nếu là Quản lý thì Chức vụ và Trạng thái bị Readonly, không cần track
                EventUtils.setupDirtyTracking(btnSave, txtTen, txtDiaChi, txtSDT, txtCCCD, dpNgaySinh, cbTrinhDo,
                        cbNguoiQuanLy);
            }
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * ROOT
     * ════════════════════════════════════════════════════════════════
     */
    private VBox buildRoot() {
        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 6);");
        root.getChildren().addAll(
                buildHeader(),
                buildFormBody(),
                buildFooter());
        return root;
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * HEADER
     * ════════════════════════════════════════════════════════════════
     */
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle(
                "-fx-background-color: " + C_SIDEBAR + ";" +
                        "-fx-background-radius: 16 16 0 0;");

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(nvEdit == null ? "THÊM NHÂN VIÊN MỚI" : "CẬP NHẬT NHÂN VIÊN");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(nvEdit == null
                ? "Điền đầy đủ thông tin bên dưới"
                : "Chỉnh sửa thông tin nhân viên " + nvEdit.getMaNV());
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web("#93c5fd"));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 10 4 10;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 10 4 10;" +
                        "-fx-background-radius: 6;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 10 4 10;"));
        btnClose.setOnAction(e -> close());

        header.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        header.setOnMouseDragged(event -> {
            setX(event.getScreenX() - xOffset);
            setY(event.getScreenY() - yOffset);
        });

        header.getChildren().addAll(titleBox, spacer, btnClose);
        return header;
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * FORM BODY
     * ════════════════════════════════════════════════════════════════
     */
    private ScrollPane buildFormBody() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(24, 34, 14, 34));
        form.setStyle("-fx-background-color: white;");

        /* ── Họ và tên ─────────────────────────────────────────────── */
        txtTen = makeField(nvEdit != null ? nvl(nvEdit.getHoTen()) : "", "Nhập họ và tên");
        errTen = errLabel();
        form.getChildren().add(fieldBlock("Họ và tên *", txtTen, errTen, "Vui lòng nhập họ và tên"));

        /* ── Địa chỉ ────────────────────────────────────────────── */
        txtDiaChi = makeField(nvEdit != null ? nvl(nvEdit.getDiaChi()) : "", "Nhập địa chỉ cư trú");
        errDiaChi = errLabel();
        form.getChildren().add(fieldBlock("Địa chỉ", txtDiaChi, errDiaChi, null));

        /* ── Ngày sinh ──────────────────────────────────────────── */
        int curYear = LocalDate.now().getYear();
        dpNgaySinh = new DatePicker(curYear - 100, curYear);
        dpNgaySinh.setMaxWidth(Double.MAX_VALUE);
        if (nvEdit != null && nvEdit.getNgaySinh() != null) {
            dpNgaySinh.setValue(nvEdit.getNgaySinh());
        }
        errNS = errLabel();
        form.getChildren().add(fieldBlock("Ngày sinh *", dpNgaySinh, errNS, "Vui lòng chọn ngày sinh (từ đủ 18 tuổi)"));

        /* ── CCCD ───────────────────────────────────────────────── */
        txtCCCD = makeField(nvEdit != null ? nvl(nvEdit.getCccd()) : "", "Nhập CCCD (12 số)");
        ValidationUtils.applyNumericOnlyFilter(txtCCCD, 12);
        errCCCD = errLabel();
        form.getChildren().add(fieldBlock("Số CCCD *", txtCCCD, errCCCD, "Vui lòng nhập số CCCD (12 số)"));

        /* ── SĐT ────────────────────────────────────────────────── */
        txtSDT = makeField(nvEdit != null ? nvl(nvEdit.getSoDT()) : "", "Nhập số điện thoại");
        ValidationUtils.applyNumericOnlyFilter(txtSDT, 10);
        errSDT = errLabel();
        form.getChildren().add(fieldBlock("Số điện thoại *", txtSDT, errSDT, "Vui lòng nhập số điện thoại (10 số)"));

        /* ── Trình độ ───────────────────────────────────────────── */
        cbTrinhDo = new ComboBox<>();
        cbTrinhDo.getItems().addAll(trinhDo.values());
        cbTrinhDo.setPromptText("Chọn trình độ");
        styleCombo(cbTrinhDo);
        if (nvEdit != null && nvEdit.getTrinhDo() != null) {
            cbTrinhDo.getSelectionModel().select(nvEdit.getTrinhDo());
        }
        errTrinhDo = errLabel();
        form.getChildren().add(fieldBlock("Trình độ *", cbTrinhDo, errTrinhDo, "Bắt buộc chọn trình độ chuyên môn"));

        /* ── Chức vụ ────────────────────────────────────────────── */
        cbChucVu = new ComboBox<>();
        cbChucVu.getItems().addAll("Quản lý", "Nhân viên");
        cbChucVu.setPromptText("Chọn chức vụ");
        styleCombo(cbChucVu);
        errChucVu = errLabel();

        if (nvEdit != null) {
            if (isAdmin) {
                cbChucVu.getSelectionModel().select(getRoleLabel(nvEdit.getRole()));
                form.getChildren().add(fieldBlock("Chức vụ *", cbChucVu, errChucVu, "Admin có thể chọn Quản lý hoặc Nhân viên"));
            } else {
                TextField txtCV = makeReadonlyField(getRoleLabel(nvEdit.getRole()));
                form.getChildren().add(fieldBlock("Chức vụ", txtCV, null, null));
            }
        } else if (isAdmin) {
            cbChucVu.getSelectionModel().select("Nhân viên");
            form.getChildren().add(fieldBlock("Chức vụ *", cbChucVu, errChucVu, "Admin có thể chọn Quản lý hoặc Nhân viên"));
        } else {
            TextField txtCV = makeReadonlyField("Nhân viên");
            form.getChildren().add(fieldBlock("Chức vụ", txtCV, null, null));
        }

        /* ── Trạng thái (Chỉ hiện khi Sửa) ──────────────────────── */
        if (nvEdit != null) {
            cbTrangThai = new ComboBox<>();
            cbTrangThai.getItems().addAll(TrangThaiNV.values());
            cbTrangThai.setPromptText("Chọn trạng thái");
            errTrangThai = errLabel();
            cbTrangThai.setConverter(new javafx.util.StringConverter<TrangThaiNV>() {
                @Override
                public String toString(TrangThaiNV object) {
                    if (object == null) return "";
                    return object == TrangThaiNV.CON_LAM ? "Còn làm" : "Đã nghỉ";
                }
                @Override
                public TrangThaiNV fromString(String string) {
                    return "Còn làm".equals(string) ? TrangThaiNV.CON_LAM : TrangThaiNV.DA_NGHI;
                }
            });
            styleCombo(cbTrangThai);

            if (isAdmin) {
                cbTrangThai.getSelectionModel().select(nvEdit.getTrangThai());
                form.getChildren().add(fieldBlock("Trạng thái *", cbTrangThai, errTrangThai, null));
            } else {
                TextField txtTT = makeReadonlyField(
                        nvEdit.getTrangThai() == TrangThaiNV.CON_LAM ? "Còn làm" : "Đã nghỉ");
                form.getChildren().add(fieldBlock("Trạng thái", txtTT, null, "Hệ thống tự động cập nhật"));
            }
        }

        /* ── Người quản lý ──────────────────────────────────────── */
        cbNguoiQuanLy = new ComboBox<>();
        cbNguoiQuanLy.setPromptText("Chọn người quản lý trực tiếp");
        styleCombo(cbNguoiQuanLy);
        loadManagersToCombo();
        cbNguoiQuanLy.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NhanVien item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getHoTen() + " (" + item.getMaNV() + ")");
            }
        });
        cbNguoiQuanLy.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(NhanVien item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getHoTen() + " (" + item.getMaNV() + ")");
            }
        });

        errNguoiQuanLy = errLabel();
        nguoiQuanLyBox = fieldBlock("Người quản lý *", cbNguoiQuanLy, errNguoiQuanLy, "Nhân viên cần có người quản lý trực tiếp");

        // Mặc định chọn chính mình nếu đang là QUAN_LY tự tạo nhân viên
        if (!isAdmin && currentUser != null && currentUser.getRole() == ChucVu.QUAN_LY) {
            for (int i = 0; i < cbNguoiQuanLy.getItems().size(); i++) {
                if (Objects.equals(cbNguoiQuanLy.getItems().get(i).getMaNV(), currentUser.getMaNV())) {
                    cbNguoiQuanLy.getSelectionModel().select(i);
                    break;
                }
            }
            cbNguoiQuanLy.setDisable(true); // Khóa không cho chuyển QL khác
        }

        updateNguoiQuanLyVisibility();
        form.getChildren().add(nguoiQuanLyBox);

        if (isAdmin) {
            cbChucVu.setOnAction(e -> {
                updateNguoiQuanLyVisibility();
                validateChucVu();
                validateNguoiQuanLy();
            });
        }

        // Load NQL khi sửa
        if (nvEdit != null && nvEdit.getRole() == ChucVu.NHAN_VIEN && nvEdit.getQuanLy() != null && nvEdit.getQuanLy().getMaNV() != null) {
            nguoiQuanLyBox.setVisible(true);
            nguoiQuanLyBox.setManaged(true);
            for (int i = 0; i < cbNguoiQuanLy.getItems().size(); i++) {
                if (Objects.equals(cbNguoiQuanLy.getItems().get(i).getMaNV(), nvEdit.getQuanLy().getMaNV())) {
                    cbNguoiQuanLy.getSelectionModel().select(i);
                    break;
                }
            }
        }

        setupValidation();

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * FOOTER
     * ════════════════════════════════════════════════════════════════
     */
    private HBox buildFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 32, 20, 32));
        footer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + " transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;" +
                        "-fx-background-radius: 0 0 16 16;");

        Button btnCancel = makeFooterBtn("Hủy", "white", "#374151", C_BORDER, "#f3f4f6");
        btnCancel.setOnAction(e -> close());

        btnSave = makeFooterBtn(nvEdit == null ? "💾  Thêm mới" : "💾  Cập nhật",
                C_SIDEBAR, "white", "transparent", C_ACTIVE);
        btnSave.setOnAction(e -> handleSave());

        footer.getChildren().addAll(btnCancel, btnSave);
        return footer;
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * VALIDATION
     * ════════════════════════════════════════════════════════════════
     */
    private void setupValidation() {
        /*
         * Dùng FieldValidationUtils.setupLiveValidation(...)
         * để form tự validate khi người dùng Tab/click rời field.
         * Validator vẫn giữ trong dialog vì mỗi field có rule riêng.
         */

        FieldValidationUtils.setupLiveValidation(
                txtTen, errTen,
                () -> validateTen(),
                () -> txtTen.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                txtSDT, errSDT,
                () -> validateSDT(),
                () -> txtSDT.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                txtCCCD, errCCCD,
                () -> validateCCCD(),
                () -> txtCCCD.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                dpNgaySinh, errNS,
                () -> {
                    boolean ok = validateNS();
                    if (ok && txtCCCD != null && !txtCCCD.getText().trim().isEmpty()) {
                        validateCCCD();
                    }
                    return ok;
                },
                () -> dpNgaySinh.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                cbTrinhDo, errTrinhDo,
                () -> validateTrinhDo(),
                () -> cbTrinhDo.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                cbChucVu, errChucVu,
                () -> validateChucVu(),
                () -> cbChucVu.setStyle(fieldStyle())
        );

        FieldValidationUtils.setupLiveValidation(
                cbNguoiQuanLy, errNguoiQuanLy,
                () -> validateNguoiQuanLy(),
                () -> cbNguoiQuanLy.setStyle(fieldStyle())
        );

        if (cbTrangThai != null) {
            FieldValidationUtils.setupLiveValidation(
                    cbTrangThai, errTrangThai,
                    () -> validateTrangThai(),
                    () -> cbTrangThai.setStyle(fieldStyle())
            );
        }

        /*
         * Riêng DatePicker custom: khi chọn ngày từ popup thì valueProperty thay đổi,
         * nên validate ngay để lỗi biến mất/mới xuất hiện không cần chờ Tab.
         */
        dpNgaySinh.valueProperty().addListener((o, ov, nv) -> {
            validateNS();
            if (txtCCCD != null && !txtCCCD.getText().trim().isEmpty()) {
                validateCCCD();
            }
        });
    }

    private boolean validateTen() {
        String ten = txtTen.getText().trim().replaceAll("\\s+", " ");
        if (ten.isEmpty()) {
            showErrorField(txtTen, errTen, "⚠ Vui lòng nhập họ và tên.");
            return false;
        }
        if (!ten.matches(ValidationUtils.REGEX_NAME)) {
            showErrorField(txtTen, errTen, "⚠ Chỉ được chứa chữ cái và khoảng trắng.");
            return false;
        }
        if (ten.matches(ValidationUtils.REGEX_SPAM_CHAR)) {
            showErrorField(txtTen, errTen, "⚠ Tên có chứa ký tự lặp lại bất thường.");
            return false;
        }
        if (!ValidationUtils.isValidNameLength(ten)) {
            showErrorField(txtTen, errTen, "⚠ Họ phải chứa ít nhất 1 ký tự, Tên chứa ít nhất 2 ký tự.");
            return false;
        }
        clearErrorField(txtTen, errTen);
        return true;
    }

    private boolean validateNS() {
        return FieldValidationUtils.validateNgaySinh(
                dpNgaySinh, errNS, 18,
                fieldStyle(),
                errorFieldStyle()
        );
    }

    private boolean validateTrinhDo() {
        return FieldValidationUtils.validateRequired(
                cbTrinhDo, errTrinhDo,
                "⚠ Chưa chọn trình độ.",
                fieldStyle(), errorFieldStyle()
        );
    }

    private boolean validateChucVu() {
        if (!isAdmin || cbChucVu == null || !cbChucVu.isVisible()) {
            return true;
        }
        return FieldValidationUtils.validateRequired(
                cbChucVu, errChucVu,
                "⚠ Chưa chọn chức vụ.",
                fieldStyle(), errorFieldStyle()
        );
    }

    private boolean validateTrangThai() {
        if (nvEdit == null || !isAdmin || cbTrangThai == null || !cbTrangThai.isVisible()) {
            return true;
        }
        return FieldValidationUtils.validateRequired(
                cbTrangThai, errTrangThai,
                "⚠ Chưa chọn trạng thái.",
                fieldStyle(), errorFieldStyle()
        );
    }

    private boolean validateNguoiQuanLy() {
        if (nguoiQuanLyBox == null || !nguoiQuanLyBox.isVisible() || !nguoiQuanLyBox.isManaged()) {
            FieldValidationUtils.clearFieldError(cbNguoiQuanLy, errNguoiQuanLy, fieldStyle());
            return true;
        }

        if (cbNguoiQuanLy.getItems().isEmpty()) {
            FieldValidationUtils.showFieldError(
                    cbNguoiQuanLy, errNguoiQuanLy,
                    "⚠ Chưa có Quản lý đang làm để gán cho nhân viên.",
                    errorFieldStyle()
            );
            return false;
        }

        return FieldValidationUtils.validateRequired(
                cbNguoiQuanLy, errNguoiQuanLy,
                "⚠ Chưa chọn người quản lý.",
                fieldStyle(), errorFieldStyle()
        );
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        if (sdt.isEmpty()) {
            showErrorField(txtSDT, errSDT, "⚠ Vui lòng nhập số điện thoại (10 số).");
            return false;
        }
        if (!sdt.matches(ValidationUtils.REGEX_PHONE_VN)) {
            showErrorField(txtSDT, errSDT, "⚠ Sai đầu số nhà mạng Việt Nam (03x, 05x, 07x, 08x, 09x).");
            return false;
        }
        String currentId = nvEdit != null ? nvEdit.getMaNV() : "";
        if (ValidationUtils.isDuplicateSDT(sdt, currentId)) {
            showErrorField(txtSDT, errSDT, "⚠ SĐT này đã tồn tại trong hệ thống.");
            return false;
        }
        clearErrorField(txtSDT, errSDT);
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        LocalDate ns = dpNgaySinh.getValue();
        if (cccd.isEmpty()) {
            showErrorField(txtCCCD, errCCCD, "⚠ Vui lòng nhập số CCCD.");
            return false;
        }
        if (!cccd.matches(ValidationUtils.REGEX_CCCD_FORMAT)) {
            showErrorField(txtCCCD, errCCCD, "⚠ Phải gồm đúng 12 chữ số.");
            return false;
        }
        if (!ValidationUtils.isValidProvinceCode(cccd)) {
            showErrorField(txtCCCD, errCCCD, "⚠ Mã tỉnh/thành phố không hợp lệ.");
            return false;
        }
        if (ns != null) {
            int namSinh = ns.getYear();
            if (!ValidationUtils.isValidCCCDCenturyAndGender(cccd, namSinh)) {
                showErrorField(txtCCCD, errCCCD,
                        "⚠ số thứ 4 không khớp (dưới năm 2000 0:nam 1:nữ, từ năm 2000 2:nam, 3:nữ ).");
                return false;
            }
            if (!ValidationUtils.isValidCCCDBirthYear(cccd, namSinh)) {
                showErrorField(txtCCCD, errCCCD, "⚠ 2 số năm sinh trên CCCD bị sai (số 5,6).");
                return false;
            }
        }
        String currentId = nvEdit != null ? nvEdit.getMaNV() : "";
        if (ValidationUtils.isDuplicateCCCD(cccd, currentId)) {
            showErrorField(txtCCCD, errCCCD, "⚠ CCCD này đã tồn tại trong hệ thống.");
            return false;
        }
        clearErrorField(txtCCCD, errCCCD);
        return true;
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * SAVE
     * ════════════════════════════════════════════════════════════════
     */
    private void handleSave() {
        boolean ok = true;
        if (!validateTen())
            ok = false;
        if (!validateNS())
            ok = false;
        if (!validateSDT())
            ok = false;
        if (!validateCCCD())
            ok = false;
        if (!validateTrinhDo())
            ok = false;
        if (!validateChucVu())
            ok = false;
        if (!validateTrangThai())
            ok = false;
        if (!validateNguoiQuanLy())
            ok = false;

        // Nếu có lỗi -> Tự động focus ô lỗi đầu tiên.
        // Dùng list động để tránh truyền null khi cbTrangThai không xuất hiện ở màn Thêm mới.
        if (!ok) {
            java.util.List<javafx.scene.Node> errorNodes = new java.util.ArrayList<>();
            java.util.List<Label> errorLabels = new java.util.ArrayList<>();

            addFocusTarget(errorNodes, errorLabels, txtTen, errTen);
            addFocusTarget(errorNodes, errorLabels, dpNgaySinh, errNS);
            addFocusTarget(errorNodes, errorLabels, txtCCCD, errCCCD);
            addFocusTarget(errorNodes, errorLabels, txtSDT, errSDT);
            addFocusTarget(errorNodes, errorLabels, cbTrinhDo, errTrinhDo);
            addFocusTarget(errorNodes, errorLabels, cbChucVu, errChucVu);
            addFocusTarget(errorNodes, errorLabels, cbTrangThai, errTrangThai);
            addFocusTarget(errorNodes, errorLabels, cbNguoiQuanLy, errNguoiQuanLy);

            EventUtils.focusFirstError(
                    errorNodes.toArray(new javafx.scene.Node[0]),
                    errorLabels.toArray(new Label[0])
            );
            return;
        }

        try {
            NhanVien n = (nvEdit != null) ? nvEdit : new NhanVien();

            // ── Xác định chức vụ target ──────────────────────────────
            ChucVu targetRole;
            if (isAdmin) {
                targetRole = "Quản lý".equals(cbChucVu.getValue()) ? ChucVu.QUAN_LY : ChucVu.NHAN_VIEN;
            } else if (nvEdit != null) {
                targetRole = nvEdit.getRole();
            } else {
                targetRole = ChucVu.NHAN_VIEN;
            }

            // ── Validate trình độ khi gán QUAN_LY ────────────────────
            trinhDo selectedTrinhDo = cbTrinhDo.getValue();
            if (targetRole == ChucVu.QUAN_LY) {
                if (selectedTrinhDo != trinhDo.DAIHOC && selectedTrinhDo != trinhDo.SAU_DAIHOC) {
                    FieldValidationUtils.showFieldError(
                            cbTrinhDo, errTrinhDo,
                            "⚠ Quản lý phải có trình độ Đại học hoặc Sau đại học.",
                            errorFieldStyle()
                    );
                    cbTrinhDo.requestFocus();
                    return;
                }

                if (nvEdit == null || nvEdit.getRole() != ChucVu.QUAN_LY) {
                    long managerCount = dao.getAll().stream().filter(nv -> nv.getRole() == ChucVu.QUAN_LY).count();
                    if (managerCount >= 3) {
                        showError("Hệ thống chỉ cho phép tối đa 3 Quản lý!");
                        return;
                    }
                }
            }

            // ── Tự động sinh mã ───────────────────────
            if (nvEdit == null) {
                List<NhanVien> all = dao.getAll();
                int maxId = 0;
                for (NhanVien item : all) {
                    try {
                        int id = Integer.parseInt(item.getMaNV().replace("LUCIA", ""));
                        if (id > maxId)
                            maxId = id;
                    } catch (NumberFormatException ignored) {
                    }
                }
                n.setMaNV(String.format("LUCIA%03d", maxId + 1));
            }

            // ── Gán dữ liệu ────────────────────────────────────────
            n.setHoTen(ValidationUtils.toTitleCase(txtTen.getText().trim().replaceAll("\\s+", " ")));
            n.setDiaChi(txtDiaChi.getText().trim());
            n.setSoDT(txtSDT.getText().trim());
            n.setNgaySinh(dpNgaySinh.getValue());
            n.setCccd(txtCCCD.getText().trim());
            n.setRole(targetRole);
            n.setTrinhDo(selectedTrinhDo);
            n.setTrangThai((nvEdit != null && cbTrangThai != null) ? cbTrangThai.getValue()
                    : (nvEdit != null ? nvEdit.getTrangThai() : TrangThaiNV.CON_LAM));

            if (nvEdit != null) {
                n.setNgayVaoLamDate(nvEdit.getNgayVaoLamDate());
            } else {
                n.setNgayVaoLamDate(LocalDate.now());
            }

            if (n.getRole() == ChucVu.NHAN_VIEN && cbNguoiQuanLy.getValue() != null) {
                n.setQuanLy(new NhanVien(cbNguoiQuanLy.getValue().getMaNV()));
            } else {
                n.setQuanLy(null);
            }

            if (nvEdit != null && (n.getTrangThai() == TrangThaiNV.DA_NGHI || n.getRole() != ChucVu.QUAN_LY)) {
                java.util.List<NhanVien> subordinates = dao.getAll().stream()
                        .filter(nv -> nv.getQuanLy() != null && nvEdit.getMaNV().equals(nv.getQuanLy().getMaNV())
                                && nv.getTrangThai() != TrangThaiNV.DA_NGHI)
                        .toList();

                if (!subordinates.isEmpty()) {
                    java.util.List<NhanVien> otherManagers = dao.getAll().stream()
                            .filter(nv -> nv.getRole() == ChucVu.QUAN_LY
                                    && nv.getTrangThai() != TrangThaiNV.DA_NGHI
                                    && !nv.getMaNV().equals(nvEdit.getMaNV()))
                            .toList();

                    if (otherManagers.isEmpty()) {
                        showError("Nhân sự này đang quản lý " + subordinates.size()
                                + " cá nhân khác!\nBạn cần thêm ít nhất 1 Quản lý khác để bàn giao trước khi cho người này nghỉ hoặc giáng chức.");
                        return;
                    }

                    Dialog<NhanVien> dialog = new Dialog<>();
                    dialog.initOwner(this);
                    dialog.setTitle("Bàn giao quyền quản lý");
                    
                    VBox header = new VBox(8);
                    header.setPadding(new Insets(24));
                    header.setStyle("-fx-background-color: " + C_SIDEBAR + ";");
                    Label title = new Label("Bàn giao nhân sự");
                    title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
                    title.setTextFill(Color.WHITE);
                    Label subtitle = new Label("Quản lý [" + n.getHoTen() + "] đang trực tiếp quản lý " + subordinates.size() + " nhân viên.");
                    subtitle.setFont(Font.font("Segoe UI", 13));
                    subtitle.setTextFill(Color.web("#bfdbfe"));
                    header.getChildren().addAll(title, subtitle);
            
                    VBox content = new VBox(16);
                    content.setPadding(new Insets(24));
                    Label prompt = new Label("Vui lòng chọn Quản lý thay thế để tiếp nhận bàn giao công việc:");
                    prompt.setFont(Font.font("Segoe UI", 14));
                    prompt.setTextFill(Color.web("#111827"));
            
                    ComboBox<NhanVien> cb = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(otherManagers));
                    cb.setMaxWidth(Double.MAX_VALUE);
                    cb.setPrefHeight(42);
                    cb.setPromptText("Chọn quản lý thay thế...");
                    cb.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(NhanVien item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item.getHoTen() + " (" + item.getMaNV() + ")");
                        }
                    });
                    cb.setButtonCell(cb.getCellFactory().call(null));
                    if (!otherManagers.isEmpty()) cb.getSelectionModel().select(0);
            
                    content.getChildren().addAll(prompt, cb);
            
                    VBox layout = new VBox(0, header, content);
                    layout.setPrefWidth(480);
                    dialog.getDialogPane().setContent(layout);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
                    Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
                    btnOk.setText("Xác nhận bàn giao");
            
                    Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                    btnCancel.setText("Hủy bỏ");
            
                    dialog.setResultConverter(bt -> bt == ButtonType.OK ? cb.getValue() : null);

                    Region dim = model.utils.DimOverlay.show(this);
                    java.util.Optional<NhanVien> result = dialog.showAndWait();
                    model.utils.DimOverlay.hide(this, dim);

                    if (result.isPresent()) {
                        NhanVien newManager = result.get();
                        for (NhanVien sub : subordinates) {
                            sub.setQuanLy(new NhanVien(newManager.getMaNV()));
                            dao.update(sub);
                        }
                    } else {
                        return; // Hủy lưu nếu không bàn giao
                    }
                }
            }

            boolean isSuccess = (nvEdit == null) ? dao.insert(n) : dao.update(n);

            if (isSuccess) {
                showInfo("Thành công!", nvEdit == null
                        ? "Đã thêm nhân viên " + n.getMaNV() + " thành công."
                        : "Đã cập nhật thông tin nhân viên " + n.getMaNV() + ".");
                if (parentView != null)
                    parentView.loadData();
                close();
            } else {
                showError("Thao tác thất bại. Vui lòng kiểm tra lại kết nối.");
            }

        } catch (Exception ex) {
            showError("Lỗi: " + ex.getMessage());
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════
     * HELPERS
     * ════════════════════════════════════════════════════════════════
     */

    /**
     * Xác định xem "Người quản lý" combobox có nên hiển thị không.
     * Ẩn khi: đang thêm và chức vụ = Quản lý, hoặc nvEdit là Quản lý/Admin.
     */
    private void updateNguoiQuanLyVisibility() {
        boolean showNQL;
        if (isAdmin) {
            showNQL = "Nhân viên".equals(cbChucVu.getValue());
        } else if (nvEdit != null) {
            showNQL = nvEdit.getRole() == ChucVu.NHAN_VIEN;
        } else {
            // QUAN_LY thêm → luôn hiển thị (vì target = NHAN_VIEN)
            showNQL = true;
        }
        nguoiQuanLyBox.setVisible(showNQL);
        nguoiQuanLyBox.setManaged(showNQL);

        if (!showNQL) {
            cbNguoiQuanLy.getSelectionModel().clearSelection();
            FieldValidationUtils.clearFieldError(cbNguoiQuanLy, errNguoiQuanLy, fieldStyle());
        }
    }

    private void loadManagersToCombo() {
        cbNguoiQuanLy.getItems().clear();
        List<NhanVien> all = dao.getAll();
        if (all != null) {
            all.stream()
                    .filter(n -> n.getRole() == ChucVu.QUAN_LY)
                    .filter(n -> n.getTrangThai() != TrangThaiNV.DA_NGHI)
                    .filter(n -> nvEdit == null || !n.getMaNV().equals(nvEdit.getMaNV()))
                    .forEach(cbNguoiQuanLy.getItems()::add);
        }
    }

    private String getRoleLabel(ChucVu role) {
        if (role == null)
            return "Nhân viên";
        return switch (role) {
            case QUAN_LY -> "Quản lý";
            case ADMIN -> "Admin";
            default -> "Nhân viên";
        };
    }

    private VBox fieldBlock(String label, javafx.scene.Node field, Label errLbl, String hint) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(0, 0, 2, 0));
        HBox lblBox = new HBox(4);

        if (label.endsWith("*")) {
            Label lblText = new Label(label.substring(0, label.length() - 1).trim());
            lblText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblText.setTextFill(Color.web("#374151"));
            Label lblStar = new Label("*");
            lblStar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblStar.setTextFill(Color.web(C_ERROR));
            lblBox.getChildren().addAll(lblText, lblStar);
        } else {
            Label lblText = new Label(label);
            lblText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblText.setTextFill(Color.web("#374151"));
            lblBox.getChildren().add(lblText);
        }
        b.getChildren().add(lblBox);

        if (field instanceof Region r)
            r.setMaxWidth(Double.MAX_VALUE);
        b.getChildren().add(field);

        if (errLbl != null) {
            errLbl.setMaxWidth(Double.MAX_VALUE);
            b.getChildren().add(errLbl);
        }
        if (hint != null) {
            Label h = new Label(hint);
            h.setFont(Font.font("Segoe UI", 11));
            h.setTextFill(Color.web(C_TEXT_GRAY));
            b.getChildren().add(h);
        }
        return b;
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI';"
                + "-fx-font-size: 13px;"
                + "-fx-pref-height: 42;"
                + "-fx-background-color: white;"
                + "-fx-background-radius: 10;"
                + "-fx-border-radius: 10;"
                + "-fx-border-color: " + C_BORDER + ";"
                + "-fx-border-width: 1;"
                + "-fx-padding: 9 12 9 12;";
    }

    private String errorFieldStyle() {
        return FieldValidationUtils.errorStyle(fieldStyle(), C_ERROR, C_ERROR_BG);
    }

    private TextField makeField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private TextField makeReadonlyField(String value) {
        TextField tf = new TextField(value);
        tf.setEditable(false);
        tf.setFocusTraversable(false);
        tf.setStyle(fieldStyle() + "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;");
        return tf;
    }

    private void styleCombo(ComboBox<?> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setPrefHeight(40);
        cb.setStyle(fieldStyle());
    }

    private Label errLabel() {
        Label l = new Label("");
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web(C_ERROR));
        l.setWrapText(true);
        l.setMinHeight(16);
        l.setStyle("-fx-padding: 1 0 0 2;");
        return l;
    }

    private void showErrorField(javafx.scene.Node tf, Label errLabel, String msg) {
        FieldValidationUtils.showFieldError(tf, errLabel, msg, errorFieldStyle());
    }

    private void clearErrorField(javafx.scene.Node tf, Label errLabel) {
        FieldValidationUtils.clearFieldError(tf, errLabel, fieldStyle());
    }

    private void addFocusTarget(java.util.List<javafx.scene.Node> nodes,
                                java.util.List<Label> labels,
                                javafx.scene.Node node,
                                Label label) {
        if (node != null && label != null) {
            nodes.add(node);
            labels.add(label);
        }
    }

    private Button makeFooterBtn(String text, String bg, String fg, String border, String bgHover) {
        Button btn = new Button(text);
        btn.setPrefHeight(40);
        btn.setPrefWidth(text.contains("Thêm") || text.contains("Cập") ? 140 : 100);
        String baseStyle = "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand;" +
                (border.equals("transparent") ? "" : "-fx-border-color: " + border + "; -fx-border-radius: 8;");
        String hoverStyle = "-fx-background-color: " + bgHover + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand;" +
                (border.equals("transparent") ? "" : "-fx-border-color: " + border + "; -fx-border-radius: 8;");
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}