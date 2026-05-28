package gui;

import dao.LoaiPhongDAO;
import dao.PhongDAO;
import model.entities.LoaiPhong;
import model.entities.Phong;
import model.enums.TrangThaiPhong;
import model.utils.DimOverlay;
import model.utils.EventUtils;
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
import javafx.collections.FXCollections;

import java.util.List;

public class ThemSuaPhongDialog extends Stage {

    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_RED = "#dc2626";

    private double xOffset = 0;
    private double yOffset = 0;

    private final Window owner;
    private final Phong phong;
    private final PhongDAO dao;
    private final Runnable onSuccess;
    private final boolean isEdit;

    private TextField txtMaPhong;
    private ComboBox<Integer> cbSoTang;
    private TextField txtSoTangEdit;
    private ComboBox<LoaiPhong> cbLoaiPhong;
    private TextField txtSucChua;
    private TextField txtGia;
    private ComboBox<String> cbTrangThai;
    private Button btnSave;

    private final LoaiPhongDAO lpDAO = new LoaiPhongDAO();
    private List<LoaiPhong> listLoaiPhong;

    private Region overlay;

    public ThemSuaPhongDialog(Window owner, Phong phong, PhongDAO dao, Runnable onSuccess) {
        this.owner = owner;
        this.phong = phong;
        this.dao = dao;
        this.onSuccess = onSuccess;
        this.isEdit = (phong != null);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(buildRoot(), 520, 600);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        centerOnScreen();
        
        EventUtils.setupEnterToSave(() -> {
            if (btnSave != null && !btnSave.isDisabled()) {
                handleSave();
            }
        }, txtSoTangEdit != null ? txtSoTangEdit : cbSoTang, cbLoaiPhong, cbTrangThai);
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
        Label lblTitle = new Label(isEdit ? "CẬP NHẬT PHÒNG" : "THÊM PHÒNG MỚI");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(isEdit ? "Mã phòng: " + phong.getMaPhong() : "Điền đầy đủ thông tin bên dưới");
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

        listLoaiPhong = lpDAO.getAll();

        txtMaPhong = new TextField(isEdit ? phong.getMaPhong() : "");
        txtMaPhong.setEditable(false);
        txtMaPhong.setStyle(
                fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold;");

        Control tangControl;
        if (isEdit) {
            txtSoTangEdit = new TextField(String.valueOf(phong.getSoTang()));
            txtSoTangEdit.setEditable(false);
            txtSoTangEdit.setStyle(fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280;");
            tangControl = txtSoTangEdit;
        } else {
            cbSoTang = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
            cbSoTang.setPromptText("Chọn tầng");
            cbSoTang.setMaxWidth(Double.MAX_VALUE);
            cbSoTang.setStyle(fieldStyle());
            cbSoTang.setOnAction(e -> generateMaPhong());
            cbSoTang.setValue(1);
            tangControl = cbSoTang;
        }

        cbLoaiPhong = new ComboBox<>(FXCollections.observableArrayList(listLoaiPhong));
        cbLoaiPhong.setMaxWidth(Double.MAX_VALUE);
        cbLoaiPhong.setStyle(fieldStyle());
        if (isEdit && phong.getLoaiPhong() != null) {
            String lpId = phong.getLoaiPhong().getMaLoaiPhong();
            cbLoaiPhong.getItems().stream().filter(lp -> lp.getMaLoaiPhong().equals(lpId)).findFirst()
                    .ifPresent(cbLoaiPhong::setValue);
        } else if (!listLoaiPhong.isEmpty()) {
            cbLoaiPhong.setValue(listLoaiPhong.get(0));
        }

        txtSucChua = new TextField();
        txtSucChua.setEditable(false);
        txtSucChua.setStyle(
                fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-font-weight: bold;");

        txtGia = new TextField();
        txtGia.setEditable(false);
        txtGia.setStyle(fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-font-weight: bold;");

        cbLoaiPhong.setOnAction(e -> {
            updateThongTinLoaiPhong();
            checkChanges();
        });
        updateThongTinLoaiPhong();

        Control trangThaiControl;
        if (isEdit) {
            // Phòng "Đã có khách" → trạng thái chỉ đọc (do nghiệp vụ đặt/nhận/trả phòng tự
            // quản lý)
            boolean isOccupied = phong.getTrangThai() == TrangThaiPhong.DACOKHACH;
            if (isOccupied) {
                cbTrangThai = new ComboBox<>();
                cbTrangThai.setValue(TrangThaiPhong.DACOKHACH.getLabel());
                TextField txtTTOccupied = new TextField(TrangThaiPhong.DACOKHACH.getLabel() + " (tự động)");
                txtTTOccupied.setEditable(false);
                txtTTOccupied.setStyle(
                        fieldStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold;");
                trangThaiControl = txtTTOccupied;
            } else {
                // Chỉ cho phép chọn Còn trống / Đang bảo trì
                cbTrangThai = new ComboBox<>(FXCollections.observableArrayList(TrangThaiPhong.CONTRONG.getLabel(),
                        TrangThaiPhong.BAN.getLabel(),TrangThaiPhong.BAOTRI.getLabel()));
                cbTrangThai.setMaxWidth(Double.MAX_VALUE);
                cbTrangThai.setStyle(fieldStyle());
                if (phong.getTrangThai() == TrangThaiPhong.CONTRONG)
                    cbTrangThai.setValue(TrangThaiPhong.CONTRONG.getLabel());
                else if(phong.getTrangThai() == TrangThaiPhong.BAN)
                    cbTrangThai.setValue(TrangThaiPhong.BAN.getLabel());
                else 
                    cbTrangThai.setValue(TrangThaiPhong.BAOTRI.getLabel());
                cbTrangThai.setOnAction(e -> checkChanges());
                trangThaiControl = cbTrangThai;
            }
        } else {
            cbTrangThai = new ComboBox<>();
            cbTrangThai.setValue(TrangThaiPhong.CONTRONG.getLabel()); // Lưu logic cho handleSave()

            TextField txtTT = new TextField(TrangThaiPhong.CONTRONG.getLabel());
            txtTT.setEditable(false);
            txtTT.setStyle(fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280;");
            trangThaiControl = txtTT;
        }

        if (!isEdit)
            generateMaPhong();

        formList.getChildren().addAll(
                fieldBlock("Số tầng *", tangControl, isEdit ? "Không thể thay đổi" : "Chọn tầng từ 1 đến 10"),
                fieldBlock("Mã phòng", txtMaPhong, "Tự động phát sinh"),
                fieldBlock("Loại phòng *", cbLoaiPhong, null),
                fieldBlock("Sức chứa", txtSucChua, "Tự động lấy theo loại"),
                fieldBlock("Giá phòng", txtGia, "Lấy động theo loại"),
                fieldBlock("Trạng thái", trangThaiControl, null));

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
        if (!isEdit || btnSave == null)
            return;
        boolean changed = false;
        String oldLpId = phong.getLoaiPhong() != null ? phong.getLoaiPhong().getMaLoaiPhong() : "";
        String curLpId = cbLoaiPhong.getValue() != null ? cbLoaiPhong.getValue().getMaLoaiPhong() : "";
        if (!java.util.Objects.equals(curLpId, oldLpId))
            changed = true;

        String currentTT = "";
        if (phong.getTrangThai() != null) {
            currentTT = phong.getTrangThai().getLabel();
        }

        if (!java.util.Objects.equals(cbTrangThai.getValue(), currentTT))
            changed = true;
        btnSave.setDisable(!changed);
    }

    private void updateThongTinLoaiPhong() {
        LoaiPhong lp = cbLoaiPhong.getValue();
        if (lp != null) {
            // Hiển thị tên sang tiếng Việt dựa trên TenLoaiPhong enum
            String vnType = lp.getMaLoaiPhong();
            if ("SINGLE".equalsIgnoreCase(vnType))
                vnType = "Phòng Đơn";
            else if ("DOUBLE".equalsIgnoreCase(vnType))
                vnType = "Phòng Đôi";
            else if ("TRIPLE".equalsIgnoreCase(vnType))
                vnType = "Phòng Ba";
            else if ("TWIN".equalsIgnoreCase(vnType))
                vnType = "Phòng Twin";

            txtSucChua.setText(lp.getSucChua() + " người (" + vnType + ")");
            txtGia.setText(String.format("%,.0f đ / ngày", lp.getGia()));
        }
    }

    private void generateMaPhong() {
        if (isEdit)
            return;
        Integer floor = cbSoTang.getValue();
        if (floor != null) {
            String maMoi = Phong.phatSinhMaPhong(floor, dao.getAll());
            if (maMoi == null) {
                txtMaPhong.setText("Đầy phòng tầng " + floor);
                txtMaPhong.setStyle(
                        fieldStyle() + "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                txtMaPhong.setText(maMoi);
                txtMaPhong.setStyle(
                        fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold;");
            }
        }
    }

    private void handleSave() {
        String ma = txtMaPhong.getText().trim();
        if (ma.isEmpty() || ma.contains("Đầy")) {
            showError("Mã phòng không hợp lệ!");
            return;
        }

        int tang = isEdit ? phong.getSoTang() : cbSoTang.getValue();
        LoaiPhong lp = cbLoaiPhong.getValue();
        String ttStr = cbTrangThai.getValue();
        TrangThaiPhong tt = null;
        if (TrangThaiPhong.DACOKHACH.getLabel().equals(ttStr))
            tt = TrangThaiPhong.DACOKHACH; // Giữ nguyên trạng thái gốc (phòng đang có khách)
        else if (TrangThaiPhong.CONTRONG.getLabel().equals(ttStr))
            tt = TrangThaiPhong.CONTRONG;
        else if (TrangThaiPhong.BAN.getLabel().equals(ttStr))
            tt = TrangThaiPhong.BAN;
        else if (TrangThaiPhong.BAOTRI.getLabel().equals(ttStr))
            tt = TrangThaiPhong.BAOTRI;

        Phong p;
        if (isEdit) {
            // Cập nhật: Giữ nguyên các giá trị không đổi từ đối tượng cũ
            p = new Phong(phong.getMaPhong());
            p.setTenPhong(phong.getTenPhong()); // Giữ tên phòng cũ
            p.setSoTang(phong.getSoTang());
            p.setSoPhong(phong.getSoPhong());
            p.setLoaiPhong(lp);
            p.setTrangThai(tt);
        } else {
            // Thêm mới: maPhong, tenPhong (giống mã), lp, tt, soPhong, tang
            int soPhong = 0;
            try {
                soPhong = Integer.parseInt(ma.substring(2));
            } catch (Exception ignored) {
            }
            p = new Phong(ma, ma, lp, tt, soPhong, tang);
        }

        if (isEdit) {
            if (dao.update(p)) {
                showInfo("Cập nhật thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else
                showError("Cập nhật thất bại");
        } else {
            if (dao.insert(p)) {
                showInfo("Thêm phòng thành công!");
                if (onSuccess != null)
                    onSuccess.run();
                close();
            } else
                showError("Thêm phòng thất bại");
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
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-pref-height: 40; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: "
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
