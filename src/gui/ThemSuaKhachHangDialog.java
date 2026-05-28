package gui;

import dao.KhachHangDAO;
import model.entities.KhachHang;
import model.utils.ValidationUtils;
import model.utils.EventUtils;
import model.utils.DatePicker;
import model.utils.DimOverlay;
import model.utils.FieldValidationUtils;

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
import java.util.Objects;

public class ThemSuaKhachHangDialog extends Stage {

    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_RED = "#dc2626";

    private double xOffset = 0;
    private double yOffset = 0;

    private final Window owner;
    private final KhachHang khachHang;
    private final KhachHangDAO dao;
    private final Runnable onSuccess;
    private final boolean isEdit;

    private TextField txtTen, txtCCCD, txtSDT;
    private DatePicker dpNgaySinh;
    private Label errTen, errNS, errCCCD, errSDT;
    private Button btnSave;

    private Region overlay;

    public ThemSuaKhachHangDialog(Window owner, KhachHang kh, KhachHangDAO dao, Runnable onSuccess) {
        this.owner = owner;
        this.khachHang = kh;
        this.dao = dao;
        this.onSuccess = onSuccess;
        this.isEdit = (kh != null);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(buildRoot(), 520, 620);
        scene.setFill(Color.TRANSPARENT);
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

    private void initEvents() {
        // 1. Nhấn Enter để Thêm mới / Cập nhật luôn (Tab chuyển ô tự động của JavaFX)
        EventUtils.setupEnterToSave(() -> {
            if (btnSave != null && !btnSave.isDisabled()) {
                handleSave();
            }
        }, txtTen, txtCCCD, txtSDT, dpNgaySinh);

        // 2. Theo dõi form: Tự động khóa nút Cập nhật nếu chưa thay đổi gì (Chỉ áp dụng
        // chế độ Sửa)
        if (isEdit && btnSave != null) {
            EventUtils.setupDirtyTracking(btnSave, txtTen, txtCCCD, txtSDT);
            // DatePicker custom cần listen trực tiếp vào valueProperty
            LocalDate initialNS = khachHang.getNgaySinh();
            dpNgaySinh.valueProperty().addListener((obs, oldV, newV) -> {
                boolean textChanged = !Objects.equals(txtTen.getText().trim(),
                        khachHang.getTenKH() == null ? "" : khachHang.getTenKH())
                        || !Objects.equals(txtCCCD.getText().trim(),
                                khachHang.getSoCCCD() == null ? "" : khachHang.getSoCCCD())
                        || !Objects.equals(txtSDT.getText().trim(),
                                khachHang.getSoDT() == null ? "" : khachHang.getSoDT());
                boolean nsChanged = !Objects.equals(newV, initialNS);
                btnSave.setDisable(!textChanged && !nsChanged);
            });
        }
    }

    public void showDialog() {
        this.overlay = DimOverlay.show(owner);
        showAndWait();
        DimOverlay.hide(owner, this.overlay);
    }

    private VBox buildRoot() {
        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 6);");
        root.getChildren().addAll(buildHeader(), buildBody(), buildFooter());
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: " + C_SIDEBAR + "; -fx-background-radius: 16 16 0 0;");

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(isEdit ? "CẬP NHẬT KHÁCH HÀNG" : "THÊM KHÁCH HÀNG MỚI");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(
                isEdit ? "Chỉnh sửa thông tin khách hàng: " + khachHang.getMaKH() : "Điền đầy đủ thông tin bên dưới");
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web("#93c5fd"));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 10 4 10; -fx-background-radius: 6;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 10 4 10;"));
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

    private ScrollPane buildBody() {
        VBox formList = new VBox(16);
        formList.setPadding(new Insets(22, 32, 10, 32));
        formList.setStyle("-fx-background-color: white;");

        txtTen = makeField(isEdit ? nvl(khachHang.getTenKH()) : "", "Nhập họ và tên");
        errTen = errLabel();

        int curYear = LocalDate.now().getYear();
        dpNgaySinh = new DatePicker(curYear - 100, curYear + 25);
        dpNgaySinh.setMaxWidth(Double.MAX_VALUE);
        if (isEdit && khachHang.getNgaySinh() != null) {
            dpNgaySinh.setValue(khachHang.getNgaySinh());
        }
        errNS = errLabel();

        txtCCCD = makeField(isEdit ? nvl(khachHang.getSoCCCD()) : "", "Nhập CCCD (12 số)");
        ValidationUtils.applyNumericOnlyFilter(txtCCCD, 12);
        errCCCD = errLabel();

        txtSDT = makeField(isEdit ? nvl(khachHang.getSoDT()) : "", "Nhập SDT (10 số)");
        ValidationUtils.applyNumericOnlyFilter(txtSDT, 10);
        errSDT = errLabel();

        setupValidation();

        formList.getChildren().addAll(
                fieldBlock("Họ và tên *", txtTen, errTen, "Vui lòng nhập họ và tên"),
                fieldBlockDate("Ngày sinh *", dpNgaySinh, errNS, "Vui lòng chọn ngày sinh khách hàng (từ đủ 16 tuổi)"),
                fieldBlock("Số CCCD *", txtCCCD, errCCCD, "Vui lòng nhập số CCCD (12 số)"),
                fieldBlock("Số điện thoại *", txtSDT, errSDT, "Vui lòng nhập số điện thoại (10 số)"));

        ScrollPane scroll = new ScrollPane(formList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 32, 20, 32));
        footer.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER
                + " transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-background-radius: 0 0 16 16;");

        Button btnCancel = makeFooterBtn("Hủy", "white", "#374151", C_BORDER, "#f3f4f6");
        btnCancel.setOnAction(e -> close());

        btnSave = makeFooterBtn(isEdit ? "💾 Cập nhật" : "💾 Thêm mới", C_SIDEBAR, "white", "transparent", C_ACTIVE);
        btnSave.setOnAction(e -> handleSave());
        if (isEdit)
            btnSave.setDisable(true);

        footer.getChildren().addAll(btnCancel, btnSave);
        return footer;
    }

    private void setupValidation() {
        txtTen.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv)
                validateTen();
        });
        txtSDT.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv)
                validateSDT();
        });
        txtCCCD.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv)
                validateCCCD();
        });
        // Listen vào valueProperty thay vì focusedProperty
        // vì popup tự custom không fire focusedProperty của HBox khi đóng
        dpNgaySinh.valueProperty().addListener((o, ov, nv) -> validateNS());
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
            showErrorField(txtTen, errTen, "⚠ Họ và tên phải chứa ít nhất 1 ký tự");
            return false;
        }
        clearErrorField(txtTen, errTen);
        return true;
    }

    private boolean validateNS() {
        return FieldValidationUtils.validateNgaySinh(
        dpNgaySinh, errNS, 16,
        fieldStyle(),
        fieldStyle() + "-fx-border-color: " + C_RED + "; -fx-background-color: #fef2f2;"
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
        String currentId = khachHang != null ? khachHang.getMaKH() : "";
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
        String currentId = khachHang != null ? khachHang.getMaKH() : "";
        if (ValidationUtils.isDuplicateCCCD(cccd, currentId)) {
            showErrorField(txtCCCD, errCCCD, "⚠ CCCD này đã tồn tại trong hệ thống.");
            return false;
        }
        clearErrorField(txtCCCD, errCCCD);
        return true;
    }

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
        if (!ok) {
            EventUtils.focusFirstError(
                    new javafx.scene.Node[] { txtTen, dpNgaySinh, txtCCCD, txtSDT },
                    new Label[] { errTen, errNS, errCCCD, errSDT });
            return;
        }

        String ten = ValidationUtils.toTitleCase(txtTen.getText().trim().replaceAll("\\s+", " "));
        String cccd = txtCCCD.getText().trim();
        String sdt = txtSDT.getText().trim();
        LocalDate ns = dpNgaySinh.getValue();

        if (isEdit) {
            khachHang.setTenKH(ten);
            khachHang.setSoCCCD(cccd);
            khachHang.setSoDT(sdt);
            khachHang.setNgaySinh(ns);
            if (dao.update(khachHang)) {
                showInfo("Cập nhật thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else
                showError("Cập nhật thất bại");
        } else {
            String newMaKH = dao.getNextMaKH();
            KhachHang newKH = new KhachHang(newMaKH, ten, cccd, sdt, ns);
            if (dao.insert(newKH)) {
                showInfo("Thêm thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else
                showError("Thêm thất bại");
        }
    }

    private VBox fieldBlockDate(String label, javafx.scene.Node field, Label errLbl, String hint) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(0, 0, 2, 0));
        HBox lblBox = new HBox(4);
        if (label.endsWith("*")) {
            Label lblText = new Label(label.substring(0, label.length() - 1).trim());
            lblText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblText.setTextFill(Color.web("#374151"));
            Label lblStar = new Label("*");
            lblStar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblStar.setTextFill(Color.web(C_RED));
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

    private VBox fieldBlock(String label, Control field, Label errLbl, String hint) {
        return fieldBlockDate(label, field, errLbl, hint);
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-pref-height: 40; -fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: "
                + C_BORDER + "; -fx-padding: 8 12 8 12;";
    }

    private TextField makeField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private Label errLabel() {
        Label l = new Label("");
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web(C_RED));
        l.setWrapText(true);
        l.setMinHeight(14);
        return l;
    }

    private void showErrorField(javafx.scene.Node tf, Label errLabel, String msg) {
        if (errLabel != null)
            errLabel.setText(msg);
        tf.setStyle(fieldStyle() + "-fx-border-color: " + C_RED + "; -fx-background-color: #fef2f2;");
    }

    private void clearErrorField(javafx.scene.Node tf, Label errLabel) {
        if (errLabel != null)
            errLabel.setText("");
        tf.setStyle(fieldStyle());
    }

    private Button makeFooterBtn(String text, String bg, String textFill, String border, String hoverBg) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        b.setPrefHeight(40);
        b.setPrefWidth(text.contains("Thêm") || text.contains("Cập") ? 140 : 100);
        b.setCursor(javafx.scene.Cursor.HAND);
        String style = "-fx-background-color: " + bg + "; -fx-text-fill: " + textFill
                + "; -fx-background-radius: 8; -fx-border-color: " + border + "; -fx-border-radius: 8;";
        String hover = "-fx-background-color: " + hoverBg + "; -fx-text-fill: " + textFill
                + "; -fx-background-radius: 8; -fx-border-color: " + border + "; -fx-border-radius: 8;";
        b.setStyle(style);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(style));
        return b;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}