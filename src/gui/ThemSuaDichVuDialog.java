package gui;

import dao.DichVuDAO;
import dao.LoaiDichVuDAO;
import model.entities.DichVu;
import model.entities.LoaiDichVu;
import model.utils.ValidationUtils;
import model.utils.EventUtils;
import model.utils.DimOverlay;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

import java.util.Random;
import java.util.List;

/**
 * ThemSuaDichVuDialog – JavaFX
 * Quản lý thêm/sửa dịch vụ với thiết kế đồng bộ hệ thống.
 */
public class ThemSuaDichVuDialog extends Stage {

    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_RED = "#dc2626";

    private double xOffset = 0;
    private double yOffset = 0;

    private final Window owner;
    private final DichVu dichVu;
    private final Runnable onSuccess;
    private final boolean isEdit;

    private TextField txtMaDV, txtTenDV, txtDonVi;
    private ComboBox<LoaiDichVu> cbLoai;
    private Label errTen, errDonVi;
    private Button btnSave;

    private Region overlay;

    public ThemSuaDichVuDialog(Window owner, DichVu dv, Runnable onSuccess) {
        this.owner = owner;
        this.dichVu = dv;
        this.onSuccess = onSuccess;
        this.isEdit = (dv != null);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(buildRoot(), 500, 580);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        centerOnScreen();

        initEvents();

        Platform.runLater(() -> {
            if (txtTenDV != null) {
                txtTenDV.requestFocus();
            }
        });
    }

    private void initEvents() {
        EventUtils.setupEnterToSave(() -> {
            if (btnSave != null && !btnSave.isDisabled()) {
                handleSave();
            }
        }, txtTenDV, txtDonVi, cbLoai);
        if (isEdit && btnSave != null) {
            EventUtils.setupDirtyTracking(btnSave, txtTenDV, txtDonVi, cbLoai);
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
        Label lblTitle = new Label(isEdit ? "CẬP NHẬT DỊCH VỤ" : "THÊM DỊCH VỤ MỚI");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(isEdit ? "Mã dịch vụ: " + dichVu.getMaDV() : "Tạo dịch vụ mới với mã tự động");
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web("#93c5fd"));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;");
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
        VBox form = new VBox(15);
        form.setPadding(new Insets(22, 32, 10, 32));
        form.setStyle("-fx-background-color: white;");

        // 1. Mã dịch vụ (Auto-generate if new)
        DichVuDAO dvDAO = new DichVuDAO();
        txtMaDV = makeField(isEdit ? dichVu.getMaDV() : dvDAO.generateNextMaDV(), "");
        txtMaDV.setEditable(false);
        txtMaDV.setStyle(fieldStyle() + "-fx-background-color: #f3f4f6; -fx-text-fill: " + C_TEXT_GRAY + ";");

        // 2. Tên dịch vụ
        txtTenDV = makeField(isEdit ? dichVu.getTenDV() : "", "Ví dụ: Coca Cola, Massage...");
        errTen = errLabel();

        // 3. Loại dịch vụ
        cbLoai = new ComboBox<>(FXCollections.observableArrayList());
        LoaiDichVuDAO ldvDAO = new LoaiDichVuDAO();
        List<LoaiDichVu> listLoai = ldvDAO.getAll();
        cbLoai.getItems().addAll(listLoai);
        if (isEdit) {
            for (LoaiDichVu l : listLoai) {
                if (l.getMaLoaiDV().equals(dichVu.getLoaiDV())) {
                    cbLoai.setValue(l);
                    break;
                }
            }
        } else if (!listLoai.isEmpty()) {
            cbLoai.setValue(listLoai.get(0));
        }
        cbLoai.setMaxWidth(Double.MAX_VALUE);
        cbLoai.setStyle(fieldStyle());

        Button btnAddLoai = new Button("+");
        btnAddLoai.setStyle("-fx-background-color: " + C_SIDEBAR
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-pref-height: 42; -fx-pref-width: 42;");
        btnAddLoai.setOnAction(e -> showAddLoaiDialog());
        HBox loaiBox = new HBox(8, cbLoai, btnAddLoai);
        HBox.setHgrow(cbLoai, Priority.ALWAYS);

        // 4. Đơn vị tính
        txtDonVi = makeField(isEdit ? dichVu.getDonVi() : "Cái", "Ví dụ: Chai, Lượt, Cái...");
        errDonVi = errLabel();

        setupValidation();

        form.getChildren().addAll(
                fieldBlock("Mã dịch vụ", txtMaDV, null, "Mã định danh tự động"),
                fieldBlock("Tên dịch vụ *", txtTenDV, errTen, "Nhập tên sản phẩm hoặc dịch vụ"),
                fieldBlock("Loại dịch vụ", loaiBox, null, null),
                fieldBlock("Đơn vị tính *", txtDonVi, errDonVi, "Đơn vị định lượng (Lon, Bộ...)"));

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 32, 20, 32));
        footer.setStyle(
                "-fx-background-color: white; -fx-border-color: " + C_BORDER + " transparent transparent transparent;");

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
        txtTenDV.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv)
                validateTen();
        });
        txtDonVi.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv)
                validateDonVi();
        });
    }

    private boolean validateTen() {
        String s = txtTenDV.getText().trim();
        if (s.isEmpty()) {
            showErrorField(txtTenDV, errTen, "⚠ Tên dịch vụ không được để trống.");
            return false;
        }
        if (s.matches(ValidationUtils.REGEX_SPAM_CHAR)) {
            showErrorField(txtTenDV, errTen, "⚠ Tên có chứa ký tự lặp lại bất thường.");
            return false;
        }
        
        // Kiểm tra trùng lặp tên dịch vụ trong CSDL
        DichVuDAO dao = new DichVuDAO();
        String currentMaDV = isEdit ? dichVu.getMaDV() : null;
        if (dao.existsTenDV(s, currentMaDV)) {
            showErrorField(txtTenDV, errTen, "⚠ Tên dịch vụ đã tồn tại trong hệ thống.");
            return false;
        }

        clearErrorField(txtTenDV, errTen);
        return true;
    }

    private boolean validateDonVi() {
        if (txtDonVi.getText().trim().isEmpty()) {
            showErrorField(txtDonVi, errDonVi, "⚠ Đơn vị tính không được để trống.");
            return false;
        }
        clearErrorField(txtDonVi, errDonVi);
        return true;
    }

    private void handleSave() {
        boolean ok = validateTen() && validateDonVi();
        if (!ok)
            return;

        String ma = txtMaDV.getText().trim();
        String ten = ValidationUtils.toTitleCase(txtTenDV.getText().trim());
        Double gia = isEdit ? dichVu.getGia() : null;
        LoaiDichVu selectedLoai = cbLoai.getValue();
        if (selectedLoai == null) {
            showError("Vui lòng chọn hoặc thêm Loại dịch vụ.");
            return;
        }
        String loaiKey = selectedLoai.getMaLoaiDV();
        String donVi = txtDonVi.getText().trim();
        int trangThai = 0;

        DichVuDAO dao = new DichVuDAO();
        if (isEdit) {
            dichVu.setTenDV(ten);
            dichVu.setGia(gia);
            dichVu.setLoaiDV(loaiKey);
            dichVu.setDonVi(donVi);
            dichVu.setTrangThai(trangThai);
            if (dao.update(dichVu)) {
                showInfo("Cập nhật dịch vụ thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else {
                showError("Lỗi: Không thể cập nhật dịch vụ. Vui lòng kiểm tra lại kết nối.");
            }
        } else {
            // 1. Kiểm tra trùng mã
            if (dao.exists(ma)) {
                showError("Lỗi: Mã dịch vụ [" + ma + "] đã tồn tại trong hệ thống. Vui lòng thử lại.");
                return;
            }
            // 2. Thêm mới
            DichVu newDv = new DichVu(ma, ten, gia, loaiKey, "", donVi, trangThai);
            if (dao.insert(newDv)) {
                showInfo("Thêm dịch vụ mới thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else {
                showError("Lỗi khi thêm dịch vụ. Vui lòng kiểm tra lại dữ liệu.");
            }
        }
    }

    private void showAddLoaiDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm Loại Dịch Vụ Mới");
        dialog.setHeaderText("Nhập tên loại dịch vụ mới:");
        dialog.setContentText("Tên loại:");
        dialog.initOwner(this);
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                LoaiDichVuDAO dao = new LoaiDichVuDAO();
                String newId = dao.generateNextMaLoaiDV();
                LoaiDichVu newLoai = new LoaiDichVu(newId, name.trim());
                if (dao.insert(newLoai)) {
                    cbLoai.getItems().add(newLoai);
                    cbLoai.setValue(newLoai);
                } else {
                    showError("Thêm loại dịch vụ thất bại.");
                }
            }
        });
    }

    /* ── UI Helpers (Synced with other dialogs) ────────────────────────── */
    private VBox fieldBlock(String label, javafx.scene.Node field, Label errLbl, String hint) {
        VBox b = new VBox(4);
        Label l = new Label(label);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        l.setTextFill(Color.web("#374151"));
        b.getChildren().add(l);
        if (field instanceof Region r)
            r.setMaxWidth(Double.MAX_VALUE);
        b.getChildren().add(field);
        if (errLbl != null)
            b.getChildren().add(errLbl);
        if (hint != null) {
            Label h = new Label(hint);
            h.setFont(Font.font("Segoe UI", 11));
            h.setTextFill(Color.web(C_TEXT_GRAY));
            b.getChildren().add(h);
        }
        return b;
    }

    private TextField makeField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-pref-height: 40; -fx-background-color: white; " +
                "-fx-border-color: " + C_BORDER
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;";
    }

    private Label errLabel() {
        Label l = new Label("");
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web(C_RED));
        l.setMinHeight(14);
        return l;
    }

    private void showErrorField(javafx.scene.Node n, Label err, String msg) {
        if (err != null)
            err.setText(msg);
        n.setStyle(fieldStyle() + "-fx-border-color: " + C_RED + "; -fx-background-color: #fef2f2;");
    }

    private void clearErrorField(javafx.scene.Node n, Label err) {
        if (err != null)
            err.setText("");
        n.setStyle(fieldStyle());
    }

    private Button makeFooterBtn(String text, String bg, String fg, String border, String hover) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        b.setPrefHeight(40);
        b.setPrefWidth(120);
        b.setCursor(javafx.scene.Cursor.HAND);
        String s = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-background-radius: 8; -fx-border-color: " + border + "; -fx-border-radius: 8;";
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle(s.replace(bg, hover)));
        b.setOnMouseExited(e -> b.setStyle(s));
        return b;
    }

    private void showInfo(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}
