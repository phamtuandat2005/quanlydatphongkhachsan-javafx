package gui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import model.enums.TrangThaiPhong;
import model.utils.DimOverlay;

/**
 * ChiTietPhongDialog – JavaFX
 * Thay thế hoàn toàn ChiTietPhongFrame (Swing JDialog).
 *
 * Hiển thị thông tin chi tiết một phòng: mã phòng, loại phòng, tầng, trạng
 * thái.
 * Dùng Stage modal, bo góc, không có thanh tiêu đề hệ thống (UNDECORATED).
 */
public class ChiTietPhongDialog extends Stage {

    /* ── Màu ─────────────────────────────────────────────────────────── */
    private static final String C_BG = "white";
    private static final String C_TEXT_DARK = "#1f2937";
    private static final String C_TEXT_GRAY = "#9ca3af";
    private static final String C_BORDER = "#f3f4f6";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_NAVY_HOVER = "#1e40af";

    // * ── Overlay ─────────────────────────────────────────────────────────── */
    private javafx.scene.layout.Region activeOverlay;

    // * ── Constructor ───────────────────────────────────────────────────────────
    // */
    public ChiTietPhongDialog(Window owner, String maPhong,
            String loaiPhong, String priceStr, String tang, String trangThai) {
        // Thiết lập Stage
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        initStyle(StageStyle.TRANSPARENT); // Bo góc bằng clip + transparent stage

        // Màu header theo trạng thái
        String[] headerColors = HeaderColors(trangThai);
        String headerBg = headerColors[0];
        String headerDark = headerColors[1];

        // Tính chiều cao: Thêm hàng giá nếu có
        boolean hasPrice = priceStr != null && !priceStr.isBlank();
        double height = hasPrice ? 330 : 295;

        // 1. TẠO CARD
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:" + C_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 30;" +
                        "-fx-border-radius: 30;");

        card.getChildren().addAll(
                buildHeader(maPhong, tang, trangThai, headerBg, headerDark),
                buildBody(loaiPhong, priceStr, trangThai),
                buildFooter());

        // 2. DÙNG CON DAO 'CLIP' ĐỂ CẮT GỌT NỘI DUNG THẬT TRÒN TRỊA
        Rectangle clip = new Rectangle();
        clip.setArcWidth(60); // Độ cong = Radius 30 * 2
        clip.setArcHeight(60);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);

        // 3. TẠO LỚP NỀN RIÊNG BIỆT CHỈ LÀM NHIỆM VỤ ĐỔ BÓNG
        Rectangle bgShadow = new Rectangle();
        bgShadow.widthProperty().bind(card.widthProperty());
        bgShadow.heightProperty().bind(card.heightProperty());
        bgShadow.setArcWidth(60);
        bgShadow.setArcHeight(60);
        bgShadow.setFill(Color.WHITE);
        // Chuyển hiệu ứng DropShadow qua lớp nền này
        bgShadow.setEffect(new DropShadow(35, 0, 8, Color.rgb(0, 0, 0, 0.35)));

        Button btnX = buildCloseButton();

        // 4. XẾP CHỒNG LÊN NHAU: Bóng đổ (dưới) -> Card (giữa) -> Nút X (trên)
        StackPane root = new StackPane(bgShadow, card, btnX);
        root.setBackground(Background.EMPTY);

        // [CỰC KỲ QUAN TRỌNG] Ép toàn bộ khung lùi vào 40px để cho BÓNG ĐỔ có không
        // gian lan tỏa
        root.setPadding(new Insets(40));

        // Định vị nút X
        StackPane.setAlignment(btnX, Pos.TOP_RIGHT);
        // Lề của nút X lúc này được tính từ góc của 'card'
        StackPane.setMargin(btnX, new Insets(14, 14, 0, 0));

        // 5. TĂNG KÍCH THƯỚC CỬA SỔ LÊN ĐỂ BÙ CHO KHÔNG GIAN ĐỔ BÓNG
        // Card (560x460) + Padding bóng đổ (40x2) = Scene (640x540)
        Scene scene = new Scene(root, 640, 540);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        setResizable(false);

        // Khi đóng form xóa overlay
        setOnHidden(e -> {
            if (activeOverlay != null) {
                DimOverlay.hide(owner, activeOverlay);
            }
        });

        // Thêm overlay + fade-in khi show
        setOnShown(e -> {
            activeOverlay = DimOverlay.show(owner);

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
    }

    // Nút x trên góc phải
    private Button buildCloseButton() {
        Button btn = new Button();
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setTextFill(Color.WHITE);
        btn.setPrefSize(36, 36);
        btn.setMinSize(36, 36);
        btn.setMaxSize(36, 36);
        btn.setCursor(javafx.scene.Cursor.HAND);

        Label xLabel = new Label("x");
        xLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        xLabel.setTextFill(Color.WHITE);
        xLabel.setMouseTransparent(true); // Click xuyên qua label xuống button
        btn.setGraphic(xLabel);
        btn.setText("");

        setRedBg(btn, false);

        btn.setBorder(new Border(new BorderStroke(
                Color.rgb(255, 255, 255, 0.35),
                BorderStrokeStyle.SOLID,
                new CornerRadii(10),
                new BorderWidths(1.5))));

        btn.setEffect(new DropShadow(8, 0, 2, Color.rgb(0, 0, 0, 0.28)));
        btn.setOnMouseEntered(e -> setRedBg(btn, true));
        btn.setOnMouseExited(e -> setRedBg(btn, false));
        btn.setOnAction(e -> close());
        return btn;
    }

    private void setRedBg(Button btn, boolean hover) {
        btn.setBackground(new Background(new BackgroundFill(
                hover ? Color.web("#b91c1c") : Color.web("#dc2626"),
                new CornerRadii(10),
                Insets.EMPTY)));
    }

    /*
     * ── HEADER - gradient màu theo trạng thái
     * ─────────────────────────────────────────────────────
     */
    private VBox buildHeader(String maPhong, String tang, String trangThai, String bgColor, String darkColor) {
        VBox header = new VBox(0);
        header.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + bgColor + ", " +
                        darkColor + ");" + "-fx-background-radius: 29 29 0 0;");
        header.setPadding(new Insets(28, 70, 24, 32));

        // Badge trạng thái nhỏ
        Label badge = new Label("●  " + (trangThai != null ? trangThai : "—"));
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        badge.setTextFill(Color.WHITE);
        badge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.22);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: rgba(255,255,255,0.38);" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 4 14 4 12;");

        // Số phòng lớn
        Label lblRoom = new Label(maPhong);
        lblRoom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        lblRoom.setTextFill(Color.WHITE);
        lblRoom.setStyle("-fx-effect: dropshadow(gaussian,  rgba(0, 0, 0, 0.25), 4, 0, 0, 2);");
        VBox.setMargin(lblRoom, new Insets(10, 0, 2, 0));

        // Tầng
        Label lblTang = new Label("🏢  " + tang);
        lblTang.setFont(Font.font("Segoe UI", 14));
        lblTang.setTextFill(Color.rgb(255, 255, 255, 0.88));

        // Thêm tất cả vào header
        header.getChildren().addAll(badge, lblRoom, lblTang);
        return header;
    }

    /* ── Body: các hàng thông tin ────────────────────────────────────── */
    private VBox buildBody(String loaiPhong, String priceStr, String trangThai) {
        VBox body = new VBox(0);
        body.setPadding(new Insets(6, 32, 6, 32));
        body.setStyle("-fx-background-color: white ;");

        body.getChildren().add(infoRow("🛏", "Loại phòng", loaiPhong, null));

        if (priceStr != null && !priceStr.isBlank()) {
            body.getChildren().add(infoRow("💰", "Giá phòng", priceStr, null));
        }
        body.getChildren().add(infoRow("📍", "Trạng thái", null, trangThai));

        return body;
    }

    /** Hàng thông tin có trạng thái: icon - label – giá trị - trạng thái */
    private HBox infoRow(String icon, String label, String value, String trangThai) {
        HBox row = new HBox(14);
        row.setPadding(new Insets(16, 0, 16, 0));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: transparent transparent " + C_BORDER + " transparent;" +
                "-fx-border-width: 0 0 1 0;");

        // Icon vòng tròn
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(38, 38);
        iconBox.setPrefSize(38, 38);
        Rectangle iconBg = new Rectangle(38, 38);
        iconBg.setArcWidth(10);
        iconBg.setArcHeight(10);
        iconBg.setFill(Color.web("#f3f4f6"));
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(16));
        iconBox.getChildren().addAll(iconBg, iconLbl);

        // Label key
        Label lblKey = new Label(label);
        lblKey.setFont(Font.font("Segoe UI", 13));
        lblKey.setTextFill(Color.web(C_TEXT_GRAY));
        lblKey.setPrefWidth(120);

        HBox.setHgrow(lblKey, Priority.NEVER);

        // Giá trị và badge
        if (trangThai != null) {
            // Badge màu cho trạng thái
            String[] bc = bodyBadgeColors(trangThai);
            Label badge = new Label("●  " + trangThai);
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            badge.setTextFill(Color.web(bc[1]));
            badge.setStyle(
                    "-fx-background-color: " + bc[0] + ";" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 5 16 5 14;");
            row.getChildren().addAll(iconBox, lblKey, badge);
        } else {
            Label lblVal = new Label(value != null && !value.isBlank() ? value : "--");
            lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            lblVal.setTextFill(Color.web(C_TEXT_DARK));
            row.getChildren().addAll(iconBox, lblKey, lblVal);
        }
        return row;
    }

    /* ── Footer: nút đóng ────────────────────────────────────────────── */
    private VBox buildFooter() {
        // Phân cách mảnh
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: " + C_BORDER + ";");

        // Hàng nút đóng
        HBox btnRow = new HBox();
        btnRow.setPadding(new Insets(20, 32, 32, 32));
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // Nút đóng chính
        String btnBase = "-fx-background-color: " + C_NAVY + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11 36 11 36;" +
                "-fx-cursor: hand;";
        String btnHover = btnBase.replace(C_NAVY, C_NAVY_HOVER);
        Button btnClose = new Button("Đóng");
        btnClose.setStyle(btnBase);
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnHover));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(btnBase));
        btnClose.setOnAction(e -> close());

        btnRow.getChildren().add(btnClose);

        // Bọc trong VBox để có separator trên footer
        VBox wrapper = new VBox(0, sep, btnRow);
        wrapper.setStyle(
                "-fx-background-color: #f9fafb;" +
                        "-fx-background-radius: 0 0 29 29;");
        return wrapper;
    }

    private Region buildThinLine() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: " + C_BORDER + ";");
        return r;
    }

    /*
     * ── COLOR STATE: màu sắc theo trạng thái
     * ──────────────────────────────────────────────
     */

    // Trả về [headerBg, headerDark] cho gradient header
    private String[] HeaderColors(String trangThai) {
        if (TrangThaiPhong.CONTRONG.getLabel().equals(trangThai))
            return new String[] { "#22c55e", "#15803d" };
        if (TrangThaiPhong.DACOKHACH.getLabel().equals(trangThai))
            return new String[] { "#f59e0b", "#b45309" };
        if (TrangThaiPhong.BAN.getLabel().equals(trangThai))
            return new String[] { "#ef4444", "#b91c1c" };
        if (TrangThaiPhong.BAOTRI.getLabel().equals(trangThai))
            return new String[] { "#ef4444", "#b91c1c" };
        return new String[] { "#6b7280", "#374151" };
    }

    // Trả về [badgeBg, badgeFg] cho badge nhỏ
    private String[] bodyBadgeColors(String trangThai) {
        if (TrangThaiPhong.CONTRONG.getLabel().equals(trangThai))
            return new String[] { "#dcfce7", "#15803d" };
        if (TrangThaiPhong.DACOKHACH.getLabel().equals(trangThai))
            return new String[] { "#fef9c3", "#b45309" };
        if (TrangThaiPhong.BAN.getLabel().equals(trangThai))
            return new String[] { "#fee2e2", "#b91c1c" };
        if (TrangThaiPhong.BAN.getLabel().equals(trangThai))
            return new String[] { "#fee2e2", "#b91c1c" };
        return new String[] { "#f3f4f6", "#6b7280" };
    }
}