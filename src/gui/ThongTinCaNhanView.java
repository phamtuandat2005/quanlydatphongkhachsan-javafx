package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

import model.entities.NhanVien;
import model.enums.ChucVu;
import dao.NhanVienDAO;

/**
 * ThongTinCaNhanView – Dialog xem chi tiết thông tin cá nhân nhân viên.
 *
 * Có thể được mở từ:
 * ─ NhanVienView (context menu "Xem chi tiết") → hiện thông tin nhân viên được
 * chọn.
 * ─ Header hoặc sidebar → hiện thông tin của chính người đang đăng nhập.
 *
 * Chỉ hiển thị (readonly), không có chức năng chỉnh sửa.
 */
public class ThongTinCaNhanView extends Stage {

        /* ── Bảng màu ─────────────────────────────────────────────────── */
        private static final String C_SIDEBAR = "#1e3a8a";
        private static final String C_SIDEBAR_DARK = "#172554";
        private static final String C_ACTIVE = "#1d4ed8";
        private static final String C_CONTENT_BG = "#f8f9fa";
        private static final String C_BORDER = "#e9ecef";
        private static final String C_GOLD = "#d4af37";
        private static final String C_TEXT_MUTED = "#6b7280";

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private final NhanVienDAO dao = new dao.NhanVienDAO();
        private final NhanVien nv;

        /* ── Constructor ──────────────────────────────────────────────── */
        public ThongTinCaNhanView(Window owner, NhanVien nv) {
                this.nv = nv;

                initOwner(owner);
                initModality(Modality.APPLICATION_MODAL);
                initStyle(StageStyle.TRANSPARENT);

                Scene scene = new Scene(buildRoot(), 520, 600);
                scene.setFill(Color.TRANSPARENT);
                setScene(scene);
                centerOnScreen();
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
                                buildProfileHeader(),
                                buildInfoBody(),
                                buildFooter());
                return root;
        }

        /*
         * ════════════════════════════════════════════════════════════════
         * PROFILE HEADER (Avatar + Tên + Chức vụ)
         * ════════════════════════════════════════════════════════════════
         */
        private VBox buildProfileHeader() {
                VBox header = new VBox(8);
                header.setAlignment(Pos.CENTER);
                header.setPadding(new Insets(28, 24, 22, 24));
                header.setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, " + C_SIDEBAR + ", "
                                                + C_SIDEBAR_DARK + ");" +
                                                "-fx-background-radius: 16 16 0 0;");

                // Close button (top-right)
                HBox topBar = new HBox();
                topBar.setAlignment(Pos.CENTER_RIGHT);
                Button btnClose = new Button("✕");
                btnClose.setStyle(
                                "-fx-background-color: transparent;" +
                                                "-fx-text-fill: rgba(255,255,255,0.7);" +
                                                "-fx-font-size: 16px;" +
                                                "-fx-cursor: hand;" +
                                                "-fx-padding: 2 8 2 8;");
                btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.15);" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-size: 16px;" +
                                                "-fx-cursor: hand;" +
                                                "-fx-padding: 2 8 2 8;" +
                                                "-fx-background-radius: 6;"));
                btnClose.setOnMouseExited(e -> btnClose.setStyle(
                                "-fx-background-color: transparent;" +
                                                "-fx-text-fill: rgba(255,255,255,0.7);" +
                                                "-fx-font-size: 16px;" +
                                                "-fx-cursor: hand;" +
                                                "-fx-padding: 2 8 2 8;"));
                btnClose.setOnAction(e -> close());
                topBar.getChildren().add(btnClose);

                // Avatar circle
                StackPane avatarPane = new StackPane();
                Circle circle = new Circle(40);
                boolean isManager = nv.getRole() == ChucVu.QUAN_LY;
                boolean isAdminRole = nv.getRole() == ChucVu.ADMIN;
                Color avatarColor = isAdminRole ? Color.web("#7c3aed")
                                : isManager ? Color.web(C_GOLD)
                                                : Color.web(C_ACTIVE);
                circle.setFill(avatarColor);
                circle.setStroke(Color.web("rgba(255,255,255,0.3)"));
                circle.setStrokeWidth(3);

                // Lấy chữ cái đầu
                String initials = getInitials(nv.getHoTen());
                Label lblInitials = new Label(initials);
                lblInitials.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
                lblInitials.setTextFill(Color.WHITE);
                avatarPane.getChildren().addAll(circle, lblInitials);

                // Tên
                Label lblName = new Label(nv.getHoTen());
                lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
                lblName.setTextFill(Color.WHITE);

                // Badge chức vụ
                String roleTxt = switch (nv.getRole()) {
                        case ADMIN -> "☆  Admin";
                        case QUAN_LY -> "⚙  Quản lý";
                        default -> "👤  Nhân viên";
                };
                Label badge = new Label(roleTxt);
                badge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
                if (isAdminRole) {
                        badge.setStyle(
                                        "-fx-background-color: rgba(124,58,237,0.2);" +
                                                        "-fx-text-fill: #c4b5fd;" +
                                                        "-fx-padding: 4 14 4 14;" +
                                                        "-fx-background-radius: 14;");
                } else if (isManager) {
                        badge.setStyle(
                                        "-fx-background-color: rgba(212,175,55,0.2);" +
                                                        "-fx-text-fill: " + C_GOLD + ";" +
                                                        "-fx-padding: 4 14 4 14;" +
                                                        "-fx-background-radius: 14;");
                } else {
                        badge.setStyle(
                                        "-fx-background-color: rgba(255,255,255,0.15);" +
                                                        "-fx-text-fill: #bfdbfe;" +
                                                        "-fx-padding: 4 14 4 14;" +
                                                        "-fx-background-radius: 14;");
                }

                // Mã nhân viên
                Label lblMa = new Label(nv.getMaNV());
                lblMa.setFont(Font.font("Segoe UI", 12));
                lblMa.setTextFill(Color.web("#93c5fd"));

                header.getChildren().addAll(topBar, avatarPane, lblName, badge, lblMa);
                return header;
        }

        /*
         * ════════════════════════════════════════════════════════════════
         * INFO BODY
         * ════════════════════════════════════════════════════════════════
         */
        private ScrollPane buildInfoBody() {
                VBox body = new VBox(0);
                body.setPadding(new Insets(8, 0, 8, 0));
                body.setStyle("-fx-background-color: white;");

                // Section: Thông tin cơ bản
                body.getChildren().add(sectionTitle("📋  Thông tin cơ bản"));
                body.getChildren().add(infoRow("Mã nhân viên", nv.getMaNV()));
                body.getChildren().add(infoRow("Họ và tên", nv.getHoTen()));
                body.getChildren().add(infoRow("Số điện thoại",
                                nv.getSoDT() != null ? nv.getSoDT() : "Chưa cập nhật"));
                body.getChildren().add(infoRow("Địa chỉ",
                                nv.getDiaChi() != null && !nv.getDiaChi().isBlank() ? nv.getDiaChi()
                                                : "Chưa cập nhật"));

                body.getChildren().add(infoRow("CCCD/CMND",
                                nv.getCccd() != null && !nv.getCccd().isBlank() ? nv.getCccd() : "Chưa cập nhật"));

                // Section: Thông tin cá nhân
                body.getChildren().add(sectionTitle("🎂  Sinh nhật"));
                body.getChildren().add(infoRow("Ngày sinh",
                                nv.getNgaySinh() != null ? nv.getNgaySinh().format(FMT) : "Chưa cập nhật"));

                // Section: Công việc
                body.getChildren().add(sectionTitle("💼  Thông tin công việc"));
                body.getChildren().add(infoRow("Chức vụ",
                                switch (nv.getRole()) {
                                        case ADMIN -> "Admin";
                                        case QUAN_LY -> "Quản lý";
                                        default -> "Nhân viên";
                                }));
                body.getChildren().add(infoRow("Trình độ",
                                nv.getTrinhDo() != null ? nv.getTrinhDo().toString() : "Chưa cập nhật"));
                body.getChildren().add(infoRow("Ngày vào làm",
                                nv.getNgayVaoLamDate() != null ? nv.getNgayVaoLamDate().format(FMT) : "Chưa cập nhật"));

                if (nv.getRole() == ChucVu.NHAN_VIEN && nv.getQuanLy() != null && nv.getQuanLy().getMaNV() != null) {
                        String qlId = nv.getQuanLy().getMaNV();
                        String qlVal = qlId;
                        try {
                                NhanVien ql = dao.getById(qlId);
                                if (ql != null) {
                                        qlVal = ql.getHoTen() + " (" + qlId + ")";
                                }
                        } catch (Exception ignored) {
                        }
                        body.getChildren().add(infoRow("Người quản lý", qlVal));
                }

                ScrollPane scroll = new ScrollPane(body);
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
                HBox footer = new HBox();
                footer.setAlignment(Pos.CENTER);
                footer.setPadding(new Insets(14, 32, 20, 32));
                footer.setStyle(
                                "-fx-background-color: white;" +
                                                "-fx-border-color: " + C_BORDER
                                                + " transparent transparent transparent;" +
                                                "-fx-border-width: 1 0 0 0;" +
                                                "-fx-background-radius: 0 0 16 16;");

                Button btnClose = new Button("Đóng");
                btnClose.setPrefWidth(140);
                btnClose.setPrefHeight(40);
                btnClose.setStyle(
                                "-fx-background-color: " + C_SIDEBAR + ";" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-cursor: hand;");
                btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                                "-fx-background-color: " + C_ACTIVE + ";" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-cursor: hand;"));
                btnClose.setOnMouseExited(e -> btnClose.setStyle(
                                "-fx-background-color: " + C_SIDEBAR + ";" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-cursor: hand;"));
                btnClose.setOnAction(e -> close());

                footer.getChildren().add(btnClose);
                return footer;
        }

        /*
         * ════════════════════════════════════════════════════════════════
         * UI HELPERS
         * ════════════════════════════════════════════════════════════════
         */
        private Label sectionTitle(String text) {
                Label lbl = new Label(text);
                lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                lbl.setTextFill(Color.web("#1f2937"));
                lbl.setPadding(new Insets(12, 28, 6, 28));
                return lbl;
        }

        private HBox infoRow(String label, String value) {
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 28, 8, 28));
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f8fafc;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));

                Label lblKey = new Label(label);
                lblKey.setFont(Font.font("Segoe UI", 13));
                lblKey.setTextFill(Color.web(C_TEXT_MUTED));
                lblKey.setMinWidth(140);
                lblKey.setPrefWidth(140);

                Label lblVal = new Label(value);
                lblVal.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                lblVal.setTextFill(Color.web("#1f2937"));
                lblVal.setWrapText(true);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                row.getChildren().addAll(lblKey, spacer, lblVal);
                return row;
        }

        private HBox infoRowHighlight(String label, String value) {
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 28, 10, 28));
                row.setStyle("-fx-background-color: #eff6ff;");

                Label lblKey = new Label(label);
                lblKey.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                lblKey.setTextFill(Color.web(C_ACTIVE));
                lblKey.setMinWidth(140);
                lblKey.setPrefWidth(140);

                Label lblVal = new Label(value);
                lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                lblVal.setTextFill(Color.web(C_ACTIVE));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                row.getChildren().addAll(lblKey, spacer, lblVal);
                return row;
        }

        private Region divider() {
                Region div = new Region();
                div.setPrefHeight(1);
                div.setMaxHeight(1);
                div.setStyle("-fx-background-color: " + C_BORDER + ";");
                VBox.setMargin(div, new Insets(6, 28, 6, 28));
                return div;
        }

        private String getInitials(String fullName) {
                if (fullName == null || fullName.isBlank())
                        return "?";
                String[] parts = fullName.trim().split("\\s+");
                if (parts.length >= 2) {
                        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
                }
                return parts[0].substring(0, 1).toUpperCase();
        }
}
