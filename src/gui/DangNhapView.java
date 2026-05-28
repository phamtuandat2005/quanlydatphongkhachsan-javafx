package gui;

import atlantafx.base.theme.PrimerLight;
import dao.NhanVienDAO;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;
import model.entities.NhanVien;

public class DangNhapView extends Application {

    // ===== FIELDS =====
    private TextField txtUsername;
    private PasswordField txtPassword;
    private TextField txtPasswordVisible;
    private CheckBox chkShowPassword;
    private Button btnLogin;
    private Label lblError;
    private Stage primaryStage; // Lưu stage để dùng trong các method
    private NhanVienDAO nvDAO = new NhanVienDAO();

    // ===== MÀU SẮC =====
    private static final String BLUE_600 = "#2563EB";
    private static final String BLUE_700 = "#1D4ED8";
    private static final String RED_500 = "#EF4444";

    // =========================================================
    // START
    // =========================================================
    @Override
    public void start(Stage stage) {
        javafx.application.Platform.setImplicitExit(false); // Ngăn javaFX tắt cửa sổ ẩn
        this.primaryStage = stage; // Gán stage vào field để các method khác dùng
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // ROOT
        StackPane root = new StackPane();
        root.setPrefSize(850, 600);

        // BACKGROUND gradient fallback
        Pane gradientBg = new Pane();
        gradientBg.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0F172A")),
                        new Stop(1, Color.web("#1E3A5F"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        gradientBg.prefWidthProperty().bind(root.widthProperty());
        gradientBg.prefHeightProperty().bind(root.heightProperty());

        // BACKGROUND ảnh
        ImageView bgImg = new ImageView();
        try {
            bgImg.setImage(new Image("file:src/background/background.png"));
        } catch (Exception ignored) {
        }
        bgImg.fitWidthProperty().bind(root.widthProperty());
        bgImg.fitHeightProperty().bind(root.heightProperty());
        bgImg.setPreserveRatio(false);

        // DIM overlay
        Rectangle dimOverlay = new Rectangle();
        dimOverlay.widthProperty().bind(root.widthProperty());
        dimOverlay.heightProperty().bind(root.heightProperty());
        dimOverlay.setFill(Color.rgb(0, 0, 0, 0.45));

        // ===== CARD =====
        VBox card = new VBox(0);
        card.setPrefWidth(380);
        card.setMinWidth(380);
        card.setMaxWidth(380);
        card.setPrefHeight(500);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(40, 40, 40, 40));
        card.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.12), new CornerRadii(16), Insets.EMPTY)));
        card.setStyle(
                "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.4));
        shadow.setRadius(40);
        shadow.setOffsetY(16);
        card.setEffect(shadow);

        // ===== ICON =====
        StackPane iconCircle = new StackPane();
        try {
            ImageView logoView = new ImageView(new Image("file:src/icon/logo.png"));
            logoView.setFitWidth(96);
            logoView.setFitHeight(96);
            logoView.setPreserveRatio(true);
            // Bo tròn logo (clip thành hình tròn)
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(48, 48, 48);
            logoView.setClip(clip);

            DropShadow logoShadow = new DropShadow(12, Color.web(BLUE_700, 0.5));
            logoShadow.setOffsetY(4);
            logoView.setEffect(logoShadow);

            iconCircle.getChildren().add(logoView);
        } catch (Exception ex) {
            // Fallback: dùng circle + chữ H như cũ nếu không tìm thấy file logo
            Circle circle = new Circle(36);
            circle.setFill(Color.web(BLUE_600, 0.75));
            DropShadow iconShadow = new DropShadow(12, Color.web(BLUE_700, 0.5));
            iconShadow.setOffsetY(4);
            circle.setEffect(iconShadow);
            Label lblIcon = new Label("H");
            lblIcon.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
            lblIcon.setTextFill(Color.WHITE);
            iconCircle.getChildren().addAll(circle, lblIcon);
        }
        VBox.setMargin(iconCircle, new Insets(0, 0, 20, 0));

        // ===== TIÊU ĐỀ =====
        Label lblTitle = new Label("LUCIA HOTEL");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.WHITE);
        lblTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 4, 0, 0, 2);");
        lblTitle.setTextAlignment(TextAlignment.CENTER);
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblTitle, new Insets(0, 0, 6, 0));

        // ===== SUBTITLE =====
        Label lblSub = new Label("Vui lòng đăng nhập để tiếp tục");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.rgb(255, 255, 255, 0.85));
        lblSub.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 3, 0, 0, 1);");
        lblSub.setTextAlignment(TextAlignment.CENTER);
        lblSub.setAlignment(Pos.CENTER);
        lblSub.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblSub, new Insets(0, 0, 28, 0));

        // ===== FIELD: Mã nhân viên =====
        String placeholder = "Ví dụ: LUCIA001";
        VBox fieldUser = buildGlassField("Mã nhân viên", placeholder, "👤");
        txtUsername = (TextField) ((StackPane) fieldUser.getChildren().get(1)).getChildren().get(0);

        // Buộc in hoa khi nhập
        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                txtUsername.setText(newVal.toUpperCase());
            }
        });

        VBox.setMargin(fieldUser, new Insets(0, 0, 16, 0));

        // ===== FIELD: Mật khẩu =====
        VBox fieldPass = buildGlassPasswordField();
        VBox.setMargin(fieldPass, new Insets(0, 0, 10, 0));

        // ===== CHECKBOX: Hiện mật khẩu =====
        chkShowPassword = new CheckBox("Hiện mật khẩu");
        chkShowPassword.setFont(Font.font("Segoe UI", 12));
        chkShowPassword.setTextFill(Color.rgb(255, 255, 255, 0.85));
        chkShowPassword.setStyle("-fx-mark-color: white;");
        chkShowPassword.setOnAction(e -> togglePasswordVisibility());
        HBox chkRow = new HBox(chkShowPassword);
        chkRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(chkRow, new Insets(0, 0, 20, 0));

        // ===== ERROR LABEL — bọc trong StackPane cố định chiều cao =====
        lblError = new Label("");
        lblError.setFont(Font.font("Segoe UI", 12));
        lblError.setTextFill(Color.WHITE);
        lblError.setWrapText(true);
        lblError.setMaxWidth(Double.MAX_VALUE);
        lblError.setPadding(new Insets(10, 14, 10, 14));
        lblError.setBackground(new Background(new BackgroundFill(
                Color.web(RED_500, 0.65), new CornerRadii(8), Insets.EMPTY)));
        lblError.setVisible(false);

        // Bọc trong StackPane cố định 44px — không bao giờ thay đổi chiều cao
        StackPane errorWrapper = new StackPane(lblError);
        errorWrapper.setPrefHeight(44);
        errorWrapper.setMinHeight(44);
        errorWrapper.setMaxHeight(44);
        VBox.setMargin(errorWrapper, new Insets(0, 0, 8, 0));

        // ===== BUTTON ĐĂNG NHẬP =====
        btnLogin = new Button("Đăng Nhập");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(44);
        btnLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnLogin.setTextFill(Color.WHITE);
        btnLogin.setCursor(Cursor.HAND);
        styleBtnLogin(false);
        btnLogin.setOnMouseEntered(e -> styleBtnLogin(true));
        btnLogin.setOnMouseExited(e -> styleBtnLogin(false));
        btnLogin.setOnAction(e -> handleLogin());
        VBox.setMargin(btnLogin, new Insets(0, 0, 0, 0));

        // ===== ENTER NAVIGATION =====
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        txtPassword.setOnAction(e -> handleLogin());
        // txtPasswordVisible dùng primaryStage qua handleLogin()

        // ===== ASSEMBLE CARD =====
        card.getChildren().addAll(
                iconCircle, lblTitle, lblSub,
                fieldUser, fieldPass, chkRow,
                errorWrapper, btnLogin);

        // CENTER BOX — giữ card không bị stretch
        HBox centerBox = new HBox(card);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(30, 0, 30, 0));
        centerBox.prefWidthProperty().bind(root.widthProperty());
        centerBox.prefHeightProperty().bind(root.heightProperty());

        root.getChildren().addAll(gradientBg, bgImg, dimOverlay, centerBox);

        // ===== ANIMATION: Fade + Slide up =====
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        tt.setFromY(20);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();

        // ===== SCENE & STAGE =====
        Scene scene = new Scene(root, 850, 600);
        scene.setUserData(this);
        stage.setTitle("Lucia Star Hotel - Đăng nhập");
        // Set icon cửa sổ
        Image logoImg = new Image("file:src/icon/logo.png");
        System.out.println("Logo loaded? error=" + logoImg.isError() + " w=" + logoImg.getWidth());
        stage.getIcons().add(logoImg);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
        stage.setResizable(false);
        stage.show();

        txtUsername.requestFocus(); // Mặc định là trỏ field tên đăng nhập
    }

    // =========================================================
    // BUILD FIELD thông thường
    // =========================================================
    private VBox buildGlassField(String labelText, String placeholder, String icon) {
        VBox box = new VBox(6);

        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lbl.setTextFill(Color.rgb(255, 255, 255, 0.9));
        lbl.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 2, 0, 0, 1);");

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(14));
        iconLbl.setPadding(new Insets(0, 0, 0, 10));

        TextField field = new TextField();
        setupPromptTextBehavior(field, placeholder);
        field.setFont(Font.font("Segoe UI", 14));
        field.setPrefHeight(42);
        field.setPadding(new Insets(0, 12, 0, 36));
        applyGlassFieldStyle(field, false);
        field.focusedProperty().addListener((obs, o, f) -> applyGlassFieldStyle(field, f));

        StackPane stack = new StackPane();
        StackPane.setAlignment(iconLbl, Pos.CENTER_LEFT);
        stack.getChildren().addAll(field, iconLbl);

        box.getChildren().addAll(lbl, stack);
        return box;
    }

    // =========================================================
    // BUILD FIELD mật khẩu (có toggle hiện/ẩn)
    // =========================================================
    private VBox buildGlassPasswordField() {
        VBox box = new VBox(6);

        Label lbl = new Label("Mật khẩu");
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lbl.setTextFill(Color.rgb(255, 255, 255, 0.9));
        lbl.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 2, 0, 0, 1);");

        Label iconLbl = new Label("🔒");
        iconLbl.setFont(Font.font(14));
        iconLbl.setPadding(new Insets(0, 0, 0, 10));

        // PasswordField (ẩn ký tự)
        txtPassword = new PasswordField();
        setupPromptTextBehavior(txtPassword, "Nhập mật khẩu...");
        txtPassword.setFont(Font.font("Segoe UI", 14));
        txtPassword.setPrefHeight(42);
        txtPassword.setPadding(new Insets(0, 12, 0, 36));
        applyGlassFieldStyle(txtPassword, false);
        txtPassword.focusedProperty().addListener((obs, o, f) -> applyGlassFieldStyle(txtPassword, f));

        // TextField (hiện ký tự — khi bấm "Hiện mật khẩu")
        txtPasswordVisible = new TextField();
        setupPromptTextBehavior(txtPassword, "Nhập mật khẩu...");
        txtPasswordVisible.setFont(Font.font("Segoe UI", 14));
        txtPasswordVisible.setPrefHeight(42);
        txtPasswordVisible.setPadding(new Insets(0, 12, 0, 36));
        applyGlassFieldStyle(txtPasswordVisible, false);
        txtPasswordVisible.focusedProperty().addListener((obs, o, f) -> applyGlassFieldStyle(txtPasswordVisible, f));
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
        txtPasswordVisible.setOnAction(e -> handleLogin()); // dùng primaryStage qua handleLogin()

        // Sync nội dung 2 field
        txtPassword.textProperty().addListener((obs, o, n) -> {
            if (!txtPasswordVisible.getText().equals(n))
                txtPasswordVisible.setText(n);
        });
        txtPasswordVisible.textProperty().addListener((obs, o, n) -> {
            if (!txtPassword.getText().equals(n))
                txtPassword.setText(n);
        });

        StackPane stack = new StackPane();
        StackPane.setAlignment(iconLbl, Pos.CENTER_LEFT);
        stack.getChildren().addAll(txtPassword, txtPasswordVisible, iconLbl);

        box.getChildren().addAll(lbl, stack);
        return box;
    }

    // =========================================================
    // STYLE FIELD (glass effect)
    // =========================================================
    private void applyGlassFieldStyle(TextField f, boolean focused) {
        String border = focused ? "rgba(255,255,255,0.6)" : "rgba(255,255,255,0.3)";
        f.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, focused ? 0.28 : 0.18),
                new CornerRadii(8), Insets.EMPTY)));
        f.setStyle(String.format(
                "-fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1.5;" +
                        "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.6);",
                border));
    }

    // =========================================================
    // STYLE BUTTON
    // =========================================================
    private void styleBtnLogin(boolean hover) {
        Color c = hover ? Color.web(BLUE_700, 0.85) : Color.web(BLUE_600, 0.82);
        btnLogin.setBackground(new Background(new BackgroundFill(c, new CornerRadii(10), Insets.EMPTY)));
        DropShadow ds = new DropShadow(hover ? 16 : 10, Color.web(BLUE_600, hover ? 0.6 : 0.4));
        ds.setOffsetY(hover ? 5 : 3);
        btnLogin.setEffect(ds);
    }

    // =========================================================
    // TOGGLE hiện/ẩn mật khẩu
    // =========================================================
    private void togglePasswordVisibility() {
        boolean show = chkShowPassword.isSelected();
        txtPassword.setVisible(!show);
        txtPassword.setManaged(!show);
        txtPasswordVisible.setVisible(show);
        txtPasswordVisible.setManaged(show);
        if (show)
            txtPasswordVisible.requestFocus();
        else
            txtPassword.requestFocus();
    }

    // =========================================================
    // HANDLE LOGIN — validation đầy đủ
    // =========================================================
    private void handleLogin() {
        String user = txtUsername.getText().trim();
        String pass = chkShowPassword.isSelected()
                ? txtPasswordVisible.getText()
                : txtPassword.getText();

        // 1. Không nhập cả 2
        if (user.isEmpty() && pass.isEmpty()) {
            showError("Vui lòng nhập mã nhân viên và mật khẩu!");
            txtUsername.requestFocus();
            return;
        }

        // 2. Chưa nhập mã nhân viên
        if (user.isEmpty()) {
            showError("Chưa nhập mã nhân viên!");
            txtUsername.requestFocus();
            return;
        }

        // 3. Chưa nhập mật khẩu
        if (pass.isEmpty()) {
            showError("Chưa nhập mật khẩu!");
            if (chkShowPassword.isSelected())
                txtPasswordVisible.requestFocus();
            else
                txtPassword.requestFocus();
            return;
        }

        // 4. Kiểm tra mã nhân viên tồn tại không
        NhanVien staff = nvDAO.getById(user);
        if (staff == null) {
            showError("Mã nhân viên không tồn tại!");
            txtUsername.requestFocus();
            txtUsername.selectAll();
            return;
        }

        // 5. Kiểm tra mật khẩu
        if (!nvDAO.authenticate(user, pass)) {
            showError("Mật khẩu không chính xác!");
            txtPassword.clear();
            txtPasswordVisible.clear();
            if (chkShowPassword.isSelected())
                txtPasswordVisible.requestFocus();
            else
                txtPassword.requestFocus();
            shakeError();
            return;
        }

        // 6. ĐĂNG NHẬP THÀNH CÔNG
        lblError.setVisible(false);
        primaryStage.hide(); // Ẩn màn hình login

        // MainFrameView -> chạy trên JavaFx Application Thread
        Stage loadingStage = showLoadingStage();

        PauseTransition delay = new PauseTransition(Duration.millis(900));
        delay.setOnFinished(e -> {
            Stage mainStage = new Stage();
            new MainFrameView(mainStage, staff, primaryStage);
            loadingStage.close();
        });
        delay.play();
    }

    private Stage showLoadingStage() {
        Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
        loadingStage.initOwner(primaryStage);
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setResizable(false);

        StackPane root = new StackPane();
        root.setPrefSize(360, 220);
        root.setPadding(new Insets(28));
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(15, 23, 42, 0.92), new CornerRadii(18), Insets.EMPTY)));
        root.setStyle(
                "-fx-border-color: rgba(255,255,255,0.22);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.35));
        shadow.setRadius(28);
        shadow.setOffsetY(10);
        root.setEffect(shadow);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);

        StackPane logoBadge = new StackPane();
        logoBadge.setPrefSize(76, 76);
        logoBadge.setMaxSize(76, 76);
        logoBadge.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        logoBadge.setEffect(new DropShadow(14, Color.rgb(37, 99, 235, 0.35)));

        ImageView logoView = new ImageView(loadLogoImage());
        logoView.setFitWidth(64);
        logoView.setFitHeight(64);
        logoView.setPreserveRatio(true);
        Rectangle logoClip = new Rectangle(64, 64);
        logoClip.setArcWidth(14);
        logoClip.setArcHeight(14);
        logoView.setClip(logoClip);
        logoBadge.getChildren().add(logoView);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(46, 46);
        progress.setStyle("-fx-progress-color: " + BLUE_600 + ";");

        Label title = new Label("Đang tải dữ liệu");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Vui lòng chờ trong giây lát...");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.rgb(255, 255, 255, 0.78));

        content.getChildren().addAll(logoBadge, progress, title, subtitle);
        root.getChildren().add(content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        loadingStage.setScene(scene);
        loadingStage.show();
        return loadingStage;
    }

    private Image loadLogoImage() {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream("/icon/logo.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception ignored) {
        }
        return new Image("file:src/icon/logo.png");
    }

    // =========================================================
    // HIỂN THỊ LỖI
    // =========================================================
    private void showError(String msg) {
        lblError.setText("⚠  " + msg);
        lblError.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), lblError);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // =========================================================
    // SHAKE ANIMATION khi sai mật khẩu
    // =========================================================
    private void shakeError() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), btnLogin);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> btnLogin.setTranslateX(0));
        shake.play();
    }

    // =========================================================
    // RESET LẠI FORM KHI ĐĂNG XUẤT TỪ MÀN HÌNH CHÍNH
    // =========================================================
    public void resetForm() {
        txtUsername.clear();
        txtPassword.clear();
        txtPasswordVisible.clear();
        lblError.setVisible(false);
        txtUsername.requestFocus(); // Đưa con trỏ chuột về ô tài khoản
    }

    // =========================================================
    // XỬ LÝ ẨN/HIỆN CHỮ GỢI Ý KHI CLICK (FOCUS)
    // =========================================================
    private void setupPromptTextBehavior(TextField textField, String originalPrompt) {
        // Đặt chữ gợi ý ban đầu
        textField.setPromptText(originalPrompt);
    }
}
