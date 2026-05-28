package gui;

import dao.TienNghiDAO;
import model.entities.TienNghi;
import model.utils.DimOverlay;
import model.utils.EventUtils;

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

public class ThemSuaTienNghiDialog extends Stage {

    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_RED = "#dc2626";

    private double xOffset = 0;
    private double yOffset = 0;

    private final Window owner;
    private final TienNghi tienNghi;
    private final TienNghiDAO dao;
    private final Runnable onSuccess;
    private final boolean isEdit;

    private TextField txtMaTN;
    private TextField txtTenTN;
    private TextArea txtMoTa;
    private ComboBox<String> cbTrangThai;
    private Button btnSave;

    private Region overlay;

    public ThemSuaTienNghiDialog(Window owner, TienNghi tienNghi, TienNghiDAO dao, Runnable onSuccess) {
        this.owner = owner;
        this.tienNghi = tienNghi;
        this.dao = dao;
        this.onSuccess = onSuccess;
        this.isEdit = (tienNghi != null);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(buildRoot(), 480, 500);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        centerOnScreen();
        
        EventUtils.setupEnterToSave(() -> {
            if (btnSave != null && !btnSave.isDisabled()) {
                handleSave();
            }
        }, txtTenTN, cbTrangThai);
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
        Label lblTitle = new Label(isEdit ? "CẬP NHẬT TIỆN NGHI" : "THÊM TIỆN NGHI MỚI");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(isEdit ? "Mã: " + tienNghi.getMaTienNghi() : "Nhập thông tin tiện nghi");
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

        txtMaTN = new TextField();
        txtMaTN.setEditable(false);
        txtMaTN.setStyle(fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold;");
        if (isEdit) {
            txtMaTN.setText(tienNghi.getMaTienNghi());
        } else {
            txtMaTN.setText(dao.generateNewMaTN());
        }

        txtTenTN = new TextField(isEdit ? tienNghi.getTenTienNghi() : "");
        txtTenTN.setStyle(fieldStyle());
        txtTenTN.setPromptText("VD: Tivi màn hình phẳng");
        txtTenTN.textProperty().addListener((obs, oldV, newV) -> checkChanges());

        txtMoTa = new TextArea(isEdit ? tienNghi.getMoTa() : "");
        txtMoTa.setStyle(fieldStyle() + "-fx-pref-height: 80;");
        txtMoTa.setPromptText("Mô tả chi tiết tiện nghi...");
        txtMoTa.setWrapText(true);
        txtMoTa.textProperty().addListener((obs, oldV, newV) -> checkChanges());

        cbTrangThai = new ComboBox<>(FXCollections.observableArrayList("Đang sử dụng", "Ngưng sử dụng"));
        cbTrangThai.setMaxWidth(Double.MAX_VALUE);
        cbTrangThai.setStyle(fieldStyle());
        cbTrangThai.setValue(isEdit && !tienNghi.isTrangThai() ? "Ngưng sử dụng" : "Đang sử dụng");
        cbTrangThai.setOnAction(e -> checkChanges());

        if (isEdit) {
            formList.getChildren().addAll(
                    fieldBlock("Mã Tiện Nghi", txtMaTN, "Tự động phát sinh"),
                    fieldBlock("Tên Tiện Nghi *", txtTenTN, null),
                    fieldBlock("Mô tả", txtMoTa, null),
                    fieldBlock("Trạng thái", cbTrangThai, null)
            );
        } else {
            formList.getChildren().addAll(
                    fieldBlock("Mã Tiện Nghi", txtMaTN, "Tự động phát sinh"),
                    fieldBlock("Tên Tiện Nghi *", txtTenTN, null),
                    fieldBlock("Mô tả", txtMoTa, null)
            );
        }

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
        if (isEdit) btnSave.setDisable(true);

        footer.getChildren().addAll(btnCancel, btnSave);
        return footer;
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

    private void checkChanges() {
        if (!isEdit || btnSave == null) return;
        boolean changed = false;
        
        if (!txtTenTN.getText().trim().equals(tienNghi.getTenTienNghi() == null ? "" : tienNghi.getTenTienNghi())) changed = true;
        if (!txtMoTa.getText().trim().equals(tienNghi.getMoTa() == null ? "" : tienNghi.getMoTa())) changed = true;
        boolean tt = "Đang sử dụng".equals(cbTrangThai.getValue());
        if (tt != tienNghi.isTrangThai()) changed = true;
        
        btnSave.setDisable(!changed);
    }

    private void handleSave() {
        String ten = txtTenTN.getText().trim();
        if (ten.isEmpty()) {
            showError("Tên tiện nghi không được để trống!");
            return;
        }

        String ma = txtMaTN.getText().trim();
        String moTa = txtMoTa.getText().trim();
        boolean trangThai = "Đang sử dụng".equals(cbTrangThai.getValue());

        TienNghi tn = new TienNghi(ma, ten, moTa, trangThai);

        if (isEdit) {
            if (dao.update(tn)) {
                showInfo("Cập nhật thành công!");
                if (onSuccess != null) onSuccess.run();
                close();
            } else {
                showError("Cập nhật thất bại");
            }
        } else {
            if (dao.insert(tn)) {
                showInfo("Thêm tiện nghi thành công!");
                if (onSuccess != null) onSuccess.run();
                close();
            } else {
                showError("Thêm tiện nghi thất bại");
            }
        }
    }

    private VBox fieldBlock(String label, Control field, String hint) {
        VBox b = new VBox(4);
        b.setPadding(new Insets(0, 0, 2, 0));
        HBox lblBox = new HBox(4);
        if (label.endsWith("*")) {
            Label lblText = new Label(label.substring(0, label.length() - 1).trim());
            lblText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblText.setTextFill(Color.web("#374151"));
            Label lblStar = new Label("*");
            lblStar.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblStar.setTextFill(Color.web(C_RED));
            lblBox.getChildren().addAll(lblText, lblStar);
        } else {
            Label lblText = new Label(label);
            lblText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblText.setTextFill(Color.web("#374151"));
            lblBox.getChildren().add(lblText);
        }
        b.getChildren().add(lblBox);
        field.setMaxWidth(Double.MAX_VALUE);
        b.getChildren().add(field);
        if (hint != null) {
            Label h = new Label(hint);
            h.setFont(Font.font("Segoe UI", 11));
            h.setTextFill(Color.web(C_TEXT_GRAY));
            b.getChildren().add(h);
        }
        return b;
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: "
                + C_BORDER + "; -fx-padding: 8 12 8 12; -fx-background-color: white;";
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
}
