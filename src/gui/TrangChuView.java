package gui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.*;
import javafx.stage.Window;
import javafx.util.Duration;

import dao.PhongDAO;
import model.entities.Phong;
import model.enums.TrangThaiPhong;
import model.entities.LoaiPhong;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import dao.HoaDonDAO;
import model.entities.HoaDon;

/**
 * TrangChuView – JavaFX Dashboard
 * Chứa Banner ảnh phòng (hỗ trợ kéo thả/nút bấm) và sơ đồ phòng chia theo tầng.
 */
public class TrangChuView extends BorderPane {

    /* ── Màu giao diện ───────────────────────────────────────────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";

    /*
     * Màu phòng khớp với label thực trong enum TrangThaiPhong:
     */
    private static final Color COLOR_CONTRONG = Color.web("#22c55e");
    private static final Color COLOR_DACOKHACH = Color.web("#f59e0b");
    private static final Color COLOR_BAN = Color.web("#ef4444");
    private static final Color COLOR_DEFAULT = Color.web("#9ca3af");

    private final PhongDAO phongDAO = new PhongDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();

    private ComboBox<String> floorFilterCombo;
    private ComboBox<String> roomTypeFilterCombo;
    private Label fromDateLabel;
    private Label toDateLabel;
    private ScrollPane timelineScrollPane;
    private YearMonth timelineMonth = YearMonth.now();
    private static final DateTimeFormatter FILTER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* ── Banner carousel ─────────────────────────────────────────────── */
    private final List<BannerItem> bannerItems = new ArrayList<>();
    private ImageView bannerImageView;
    private Label bannerTitle;
    private Label bannerSubtitle;
    private HBox bannerDots;
    private Timeline bannerTimeline;
    private int currentBannerIndex = 0;
    
    // Biến lưu vị trí chuột để vuốt (swipe) và click vùng trái/phải
    private static final double SIDE_CLICK_ZONE = 130;
    private static final double SWIPE_THRESHOLD = 50;
    private double dragStartX;
    private double dragStartLocalX;

    /* ── Constructor ─────────────────────────────────────────────────── */
    public TrangChuView() {
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(26, 32, 32, 32));

        // Banner trên cùng
        StackPane heroBanner = buildHeroBanner();
        setTop(heroBanner);
        BorderPane.setMargin(heroBanner, new Insets(0, 0, 24, 0));

        VBox pageContent = new VBox(22);
        pageContent.setPadding(new Insets(0, 0, 16, 0));
        pageContent.getChildren().addAll(buildAnalyticsSection(), buildTimelineSection());

        ScrollPane pageScroll = new ScrollPane(pageContent);
        pageScroll.setFitToWidth(true);
        pageScroll.setBorder(Border.EMPTY);
        pageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.getContent().setStyle("-fx-background-color: transparent;");

        setCenter(pageScroll);
        startBannerAutoPlay();
    }

    /*
     * ════════════════════════════════════════════════════════════════════
     * BANNER ẢNH PHÒNG
     * ════════════════════════════════════════════════════════════════════
     */
    private StackPane buildHeroBanner() {
        loadBannerItems();

        StackPane banner = new StackPane();
        banner.setMinHeight(380);
        banner.setPrefHeight(380);
        banner.setStyle(
                "-fx-background-color: linear-gradient(to right, #111827, #1e3a8a);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-radius: 18;");
        banner.setEffect(new DropShadow(12, 0, 4, Color.web("#00000020")));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(banner.widthProperty());
        clip.heightProperty().bind(banner.heightProperty());
        banner.setClip(clip);

        bannerImageView = new ImageView();
        bannerImageView.setPreserveRatio(false);
        bannerImageView.setSmooth(true);
        bannerImageView.setMouseTransparent(true);
        bannerImageView.fitWidthProperty().bind(banner.widthProperty());
        bannerImageView.fitHeightProperty().bind(banner.heightProperty());

        Region overlay = new Region();
        overlay.setStyle(
                "-fx-background-color: linear-gradient(to right, " +
                        "rgba(15, 23, 42, 0.88), " +
                        "rgba(30, 58, 138, 0.48), " +
                        "rgba(15, 23, 42, 0.10));");
        overlay.setMouseTransparent(true);
        overlay.prefWidthProperty().bind(banner.widthProperty());
        overlay.prefHeightProperty().bind(banner.heightProperty());

        VBox textBox = new VBox(10);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setMaxWidth(680);
        textBox.setMouseTransparent(true);

        Label pill = new Label("LUCIA HOTEL · ROOM GALLERY");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pill.setTextFill(Color.WHITE);
        pill.setPadding(new Insets(6, 12, 6, 12));
        pill.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.18);" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.26);" +
                        "-fx-border-radius: 999;");

        bannerTitle = new Label("Lucia Hotel");
        bannerTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 31));
        bannerTitle.setTextFill(Color.WHITE);

        bannerSubtitle = new Label("Không gian nghỉ dưỡng hiện đại, sạch sẽ và phù hợp cho từng nhu cầu đặt phòng.");
        bannerSubtitle.setFont(Font.font("Segoe UI", 14));
        bannerSubtitle.setTextFill(Color.rgb(255, 255, 255, 0.90));
        bannerSubtitle.setWrapText(true);

        textBox.getChildren().addAll(pill, bannerTitle, bannerSubtitle);

        HBox content = new HBox();
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(34, 80, 34, 80));
        content.setMouseTransparent(true);
        content.getChildren().add(textBox);

        // Nút trái/phải chỉ để hiển thị.
        // Sự kiện click được xử lý trực tiếp trên banner theo tọa độ X,
        // nên không còn phụ thuộc vào việc JavaFX có pick được node nút hay không.
        StackPane btnPrev = createBannerClickZone("‹");
        StackPane btnNext = createBannerClickZone("›");

        StackPane.setAlignment(btnPrev, Pos.CENTER_LEFT);
        StackPane.setMargin(btnPrev, new Insets(0, 0, 0, 18));
        
        StackPane.setAlignment(btnNext, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnNext, new Insets(0, 18, 0, 0));

        bannerDots = new HBox(8);
        bannerDots.setAlignment(Pos.CENTER);
        StackPane.setAlignment(bannerDots, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bannerDots, new Insets(0, 0, 18, 0));

        banner.getChildren().addAll(bannerImageView, overlay, content, btnPrev, btnNext, bannerDots);
        
        btnPrev.toFront();
        btnNext.toFront();
        bannerDots.toFront();

        banner.setOnMouseEntered(e -> {
            if (bannerTimeline != null) bannerTimeline.pause();
        });
        banner.setOnMouseExited(e -> {
            if (bannerTimeline != null) bannerTimeline.play();
        });

        // LOGIC CHẮC CHẮN CHẠY:
        // 1) Kéo qua trái/phải: đổi banner.
        // 2) Click vùng 130px bên trái/phải của banner: đổi banner.
        // Vì bắt sự kiện trực tiếp trên banner cha, click vào icon tròn 2 bên cũng sẽ chạy.
        banner.setOnMouseMoved(e -> {
            double x = e.getX();
            if (x <= SIDE_CLICK_ZONE || x >= banner.getWidth() - SIDE_CLICK_ZONE) {
                banner.setCursor(Cursor.HAND);
            } else {
                banner.setCursor(Cursor.DEFAULT);
            }
        });

        banner.setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            dragStartLocalX = e.getX();
            banner.setCursor(Cursor.CLOSED_HAND);
        });

        banner.setOnMouseReleased(e -> {
            double deltaX = e.getSceneX() - dragStartX;
            double releaseX = e.getX();

            if (Math.abs(deltaX) >= SWIPE_THRESHOLD) {
                if (deltaX > 0) {
                    showPreviousBanner(true);
                } else {
                    showNextBanner(true);
                }
                restartBannerAutoPlay();
            } else {
                // Không kéo đủ xa thì coi là click.
                // Nếu click vào vùng trái/phải, chuyển banner luôn.
                double clickX = (dragStartLocalX + releaseX) / 2.0;
                if (clickX <= SIDE_CLICK_ZONE) {
                    showPreviousBanner(true);
                    restartBannerAutoPlay();
                } else if (clickX >= banner.getWidth() - SIDE_CLICK_ZONE) {
                    showNextBanner(true);
                    restartBannerAutoPlay();
                }
            }

            if (releaseX <= SIDE_CLICK_ZONE || releaseX >= banner.getWidth() - SIDE_CLICK_ZONE) {
                banner.setCursor(Cursor.HAND);
            } else {
                banner.setCursor(Cursor.DEFAULT);
            }
        });

        showBanner(0, false);
        return banner;
    }

    private StackPane createBannerClickZone(String text) {
        StackPane zone = new StackPane();
        zone.setMaxSize(44, 44);
        zone.setPrefSize(44, 44);

        // Quan trọng: để mouseTransparent = true.
        // Như vậy node nút không tự bắt chuột nữa, toàn bộ click sẽ đi về banner cha.
        // Banner cha sẽ dựa vào tọa độ X để biết người dùng bấm nút trái hay nút phải.
        zone.setMouseTransparent(true);

        Circle background = new Circle(22);
        background.setFill(Color.rgb(255, 255, 255, 0.24));
        background.setStroke(Color.rgb(255, 255, 255, 0.48));
        background.setStrokeWidth(1.1);
        background.setMouseTransparent(true);

        Label icon = new Label(text);
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        icon.setTextFill(Color.WHITE);
        icon.setTranslateY(-2);
        icon.setMouseTransparent(true);

        zone.getChildren().addAll(background, icon);
        return zone;
    }

    private void loadBannerItems() {
        if (!bannerItems.isEmpty()) return;

        addBannerItem(
                "Single Room",
                "Phòng đơn gọn gàng, đầy đủ tiện nghi cho khách đi công tác hoặc nghỉ ngắn ngày.",
                "/icon/Single_room.jpg");
        addBannerItem(
                "Double Room",
                "Không gian rộng rãi, hiện đại, phù hợp cho cặp đôi hoặc khách muốn nghỉ dưỡng thoải mái.",
                "/icon/Double_room.jpg");
        addBannerItem(
                "Double Bed Room",
                "Thiết kế hai giường linh hoạt, phù hợp cho bạn bè, đồng nghiệp hoặc nhóm khách nhỏ.",
                "/icon/Double_bed_room.jpg");
        addBannerItem(
                "Family Room",
                "Không gian lớn với khu sinh hoạt riêng, phù hợp cho gia đình và nhóm khách dài ngày.",
                "/icon/Family_room.jpg");
        addBannerItem(
                "Triple Room",
                "Phòng ba giường thoải mái, tối ưu cho nhóm khách cần nhiều chỗ nghỉ.",
                "/icon/Tripple_room.jpg");
    }

    private void addBannerItem(String title, String subtitle, String imagePath) {
        Image image = loadImage(imagePath);
        if (image != null && !image.isError()) {
            bannerItems.add(new BannerItem(title, subtitle, image));
        }
    }

    private Image loadImage(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) return null;
            return new Image(stream);
        } catch (Exception e) {
            return null;
        }
    }

    private void showPreviousBanner(boolean animate) {
        if (bannerItems.isEmpty()) return;
        int nextIndex = (currentBannerIndex - 1 + bannerItems.size()) % bannerItems.size();
        showBanner(nextIndex, animate);
    }

    private void showNextBanner(boolean animate) {
        if (bannerItems.isEmpty()) return;
        int nextIndex = (currentBannerIndex + 1) % bannerItems.size();
        showBanner(nextIndex, animate);
    }

    private void showBanner(int index, boolean animate) {
        if (bannerItems.isEmpty()) {
            bannerTitle.setText("Lucia Hotel");
            bannerSubtitle.setText("Chưa tìm thấy ảnh trong thư mục /icon. Hãy kiểm tra lại tên file ảnh phòng.");
            return;
        }

        if (index < 0 || index >= bannerItems.size()) index = 0;

        currentBannerIndex = index;
        BannerItem item = bannerItems.get(currentBannerIndex);

        if (!animate) {
            bannerImageView.setImage(item.getImage());
            bannerTitle.setText(item.getTitle());
            bannerSubtitle.setText(item.getSubtitle());
            updateBannerDots();
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), bannerImageView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.35);
        fadeOut.setOnFinished(e -> {
            bannerImageView.setImage(item.getImage());
            bannerTitle.setText(item.getTitle());
            bannerSubtitle.setText(item.getSubtitle());
            updateBannerDots();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(260), bannerImageView);
            fadeIn.setFromValue(0.35);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void updateBannerDots() {
        if (bannerDots == null) return;

        bannerDots.getChildren().clear();
        for (int i = 0; i < bannerItems.size(); i++) {
            final int targetIndex = i;
            Circle dot = new Circle(i == currentBannerIndex ? 5 : 4);
            dot.setCursor(Cursor.HAND);
            dot.setFill(i == currentBannerIndex ? Color.WHITE : Color.rgb(255, 255, 255, 0.42));
            dot.setStroke(Color.rgb(255, 255, 255, 0.65));
            dot.setStrokeWidth(i == currentBannerIndex ? 1.2 : 0);
            dot.setOnMouseClicked(e -> {
                e.consume();
                showBanner(targetIndex, true);
                restartBannerAutoPlay();
            });
            bannerDots.getChildren().add(dot);
        }
    }

    private void startBannerAutoPlay() {
        if (bannerItems.size() <= 1) return;
        bannerTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> showNextBanner(true)));
        bannerTimeline.setCycleCount(Animation.INDEFINITE);
        bannerTimeline.play();
    }

    private void restartBannerAutoPlay() {
        if (bannerTimeline != null) {
            bannerTimeline.stop();
            bannerTimeline.playFromStart();
        }
    }

    private HBox buildAnalyticsSection() {
        HBox section = new HBox(20);
        section.setAlignment(Pos.TOP_LEFT);
        section.setMaxWidth(Double.MAX_VALUE);
        section.setPadding(new Insets(0, 0, 0, 0));

        VBox revenueCard = buildRevenueCard();
        VBox statusCard = buildRoomStatusCard();

        HBox.setHgrow(revenueCard, Priority.ALWAYS);
        statusCard.setPrefWidth(340);

        section.getChildren().addAll(revenueCard, statusCard);
        return section;
    }

    private VBox buildRevenueCard() {
        VBox card = createCardContainer();
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Xu hướng doanh thu tuần");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT_DARK));

        Label subtitle = new Label("So với tuần trước (+12.4%)");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Pane chart = buildRevenueTrendChart(toWeeklyChartData(loadWeeklyRevenue()));

        ToggleButton weekButton = createSegmentButton("7 ngày qua", true);
        ToggleButton monthButton = createSegmentButton("Tháng này", false);
        ToggleGroup periodGroup = new ToggleGroup();
        weekButton.setToggleGroup(periodGroup);
        monthButton.setToggleGroup(periodGroup);
        periodGroup.selectToggle(weekButton);

        weekButton.setOnAction(e -> {
            periodGroup.selectToggle(weekButton);
            updateSegmentButtonStyle(weekButton, true);
            updateSegmentButtonStyle(monthButton, false);
            updateRevenueChart(chart, toWeeklyChartData(loadWeeklyRevenue()));
        });
        monthButton.setOnAction(e -> {
            periodGroup.selectToggle(monthButton);
            updateSegmentButtonStyle(weekButton, false);
            updateSegmentButtonStyle(monthButton, true);
            updateRevenueChart(chart, loadMonthlyRevenue());
        });

        HBox periodSwitch = new HBox(0);
        periodSwitch.getChildren().addAll(weekButton, monthButton);

        header.getChildren().addAll(titleBox, headerSpacer, periodSwitch);

        HBox legend = new HBox(18);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                buildLegendItem("Tuần này", "#2563eb"),
                buildLegendItem("Tuần trước", "#93c5fd")
        );

        card.getChildren().addAll(header, chart, legend);
        return card;
    }

    private ToggleButton createSegmentButton(String text, boolean active) {
        ToggleButton button = new ToggleButton(text);
        button.setMinHeight(28);
        button.setPadding(new Insets(0, 14, 0, 14));
        button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        button.setCursor(Cursor.HAND);
        updateSegmentButtonStyle(button, active);
        return button;
    }

    private void updateSegmentButtonStyle(ToggleButton button, boolean active) {
        button.setStyle(
                "-fx-background-color: " + (active ? "#ffffff" : "#f8fafc") + ";" +
                        "-fx-text-fill: " + C_TEXT_DARK + ";" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-radius: 0;");
    }

    private Pane buildRevenueTrendChart(Map<String, Double> revenue) {
        Pane chart = new Pane();
        chart.setPrefHeight(180);
        chart.setMinHeight(180);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getProperties().put("revenueData", revenue);
        chart.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Double> currentRevenue = (Map<String, Double>) chart.getProperties().get("revenueData");
                drawRevenueTrendChart(chart, currentRevenue, newValue.doubleValue());
            }
        });
        drawRevenueTrendChart(chart, revenue, 780);
        return chart;
    }

    private void updateRevenueChart(Pane chart, Map<String, Double> revenue) {
        chart.getProperties().put("revenueData", revenue);
        drawRevenueTrendChart(chart, revenue, chart.getWidth() > 0 ? chart.getWidth() : 780);
    }

    private void drawRevenueTrendChart(Pane chart, Map<String, Double> revenue, double availableWidth) {
        chart.getChildren().clear();

        double width = Math.max(560, availableWidth - 8);
        double top = 28;
        double left = 36;
        double bottom = 34;
        double plotWidth = width - left - 28;
        double plotHeight = 104;
        double maxAmount = revenue.values().stream().max(Double::compare).orElse(1.0);
        if (maxAmount <= 0) maxAmount = 1;

        for (int i = 0; i < 4; i++) {
            double y = top + i * (plotHeight / 3.0);
            Line grid = new Line(left, y, width - 20, y);
            grid.setStroke(Color.web("#eef2f7"));
            chart.getChildren().add(grid);
        }

        Polyline previousLine = new Polyline();
        previousLine.setStroke(Color.web("#93c5fd"));
        previousLine.setStrokeWidth(2);
        previousLine.getStrokeDashArray().addAll(6.0, 6.0);

        Polyline currentLine = new Polyline();
        currentLine.setStroke(Color.web("#2563eb"));
        currentLine.setStrokeWidth(2.4);

        int index = 0;
        double bestValue = -1;
        double bestX = left;
        double bestY = top + plotHeight;

        for (Map.Entry<String, Double> entry : revenue.entrySet()) {
            double x = left + index * (plotWidth / Math.max(1, revenue.size() - 1));
            double value = entry.getValue();
            double y = top + plotHeight - (value / maxAmount) * plotHeight;
            double previousY = top + plotHeight - ((value * 0.72) / maxAmount) * plotHeight;

            currentLine.getPoints().addAll(x, y);
            previousLine.getPoints().addAll(x, previousY);

            Label dayLabel = new Label(entry.getKey());
            dayLabel.setFont(Font.font("Segoe UI", 11));
            dayLabel.setTextFill(Color.web(C_TEXT_GRAY));
            dayLabel.setLayoutX(x - 18);
            dayLabel.setLayoutY(top + plotHeight + 14);
            dayLabel.setPrefWidth(38);
            dayLabel.setAlignment(Pos.CENTER);
            chart.getChildren().add(dayLabel);

            if (value > bestValue) {
                bestValue = value;
                bestX = x;
                bestY = y;
            }
            index++;
        }

        chart.getChildren().addAll(previousLine, currentLine);
        if (bestValue > 0) {
            Circle marker = new Circle(bestX, bestY, 4, Color.web("#2563eb"));
            Label valueLabel = new Label(formatShortCurrency(bestValue));
            valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            valueLabel.setTextFill(Color.web(C_TEXT_DARK));
            valueLabel.setLayoutX(bestX - 20);
            valueLabel.setLayoutY(bestY - 24);
            chart.getChildren().addAll(marker, valueLabel);
        }

        Line bottomLine = new Line(0, top + plotHeight + bottom, width, top + plotHeight + bottom);
        bottomLine.setStroke(Color.web("#e5e7eb"));
        chart.getChildren().add(bottomLine);
    }

    private HBox buildLegendItem(String text, String color) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4, Color.web(color));
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", 11));
        label.setTextFill(Color.web(C_TEXT_GRAY));
        item.getChildren().addAll(dot, label);
        return item;
    }

    private String formatShortCurrency(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", amount / 1_000_000);
        }
        if (amount >= 1_000) {
            return String.format(Locale.US, "%.0fK", amount / 1_000);
        }
        return String.format(Locale.US, "%.0f", amount);
    }

    private Map<LocalDate, Double> loadWeeklyRevenue() {
        Map<LocalDate, Double> revenue = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            revenue.put(monday.plusDays(i), 0.0);
        }

        try {
            List<HoaDon> invoices = hoaDonDAO.getAllWithKhachHang();
            for (HoaDon hd : invoices) {
                if (hd.getNgayTaoHD() == null) continue;
                LocalDate date = hd.getNgayTaoHD().toLocalDate();
                if (!revenue.containsKey(date)) continue;

                double amount = hoaDonDAO.tinhDoanhThu(hd);
                revenue.put(date, revenue.get(date) + amount);
            }
        } catch (Exception ignored) {
            // Nếu truy vấn lỗi thì vẫn hiện layout mặc định.
        }

        return revenue;
    }

    private Map<String, Double> toWeeklyChartData(Map<LocalDate, Double> weeklyRevenue) {
        Map<String, Double> chartData = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("E", Locale.forLanguageTag("vi"));
        for (Map.Entry<LocalDate, Double> entry : weeklyRevenue.entrySet()) {
            chartData.put(entry.getKey().format(dayFormatter), entry.getValue());
        }
        return chartData;
    }

    private Map<String, Double> loadMonthlyRevenue() {
        Map<String, Double> revenue = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 1; i <= 5; i++) {
            revenue.put("Tuần " + i, 0.0);
        }

        try {
            List<HoaDon> invoices = hoaDonDAO.getAllWithKhachHang();
            for (HoaDon hd : invoices) {
                if (hd.getNgayTaoHD() == null) continue;
                LocalDate date = hd.getNgayTaoHD().toLocalDate();
                if (!YearMonth.from(date).equals(currentMonth)) continue;

                int week = Math.min(5, ((date.getDayOfMonth() - 1) / 7) + 1);
                String key = "Tuần " + week;
                double amount = hoaDonDAO.tinhDoanhThu(hd);
                revenue.put(key, revenue.get(key) + amount);
            }
        } catch (Exception ignored) {
            // Nếu truy vấn lỗi thì vẫn hiện layout mặc định.
        }

        return revenue;
    }

    private VBox buildRoomStatusCard() {
        VBox card = createCardContainer();
        card.setSpacing(16);

        Label title = new Label("Trạng thái phòng");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT_DARK));

        List<Phong> rooms = phongDAO.getAll();
        long total = rooms.size();
        long available = rooms.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.CONTRONG).count();
        long occupied = rooms.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.DACOKHACH).count();
        long dirty = rooms.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.BAN).count();

        int readyRate = total > 0 ? (int) Math.round(available * 100.0 / total) : 0;

        HBox statusBody = new HBox(20);
        statusBody.setAlignment(Pos.CENTER_LEFT);

        VBox roomTypeStats = new VBox(10);
        roomTypeStats.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(roomTypeStats, Priority.ALWAYS);
        buildRoomTypeRows(rooms).forEach(roomTypeStats.getChildren()::add);

        statusBody.getChildren().addAll(buildDonut(readyRate), roomTypeStats);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setStyle("-fx-background-color: #e5e7eb;");

        Label taskTitle = new Label("TÁC VỤ CẦN XỬ LÝ");
        taskTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        taskTitle.setTextFill(Color.web(C_TEXT_DARK));

        VBox tasks = new VBox(8);
        tasks.getChildren().addAll(
                buildTaskRow("⌂", dirty + " phòng cần dọn dẹp", "#fff7ed", "#b45309"),
                buildTaskRow("↪", occupied + " phòng chờ check-out", "#eff6ff", "#1d4ed8")
        );

        card.getChildren().addAll(title, statusBody, divider, taskTitle, tasks);
        return card;
    }

    private StackPane buildDonut(int percentValue) {
        StackPane donut = new StackPane();
        donut.setMinSize(86, 86);
        donut.setPrefSize(86, 86);

        Circle base = new Circle(34);
        base.setFill(Color.TRANSPARENT);
        base.setStroke(Color.web("#e5e7eb"));
        base.setStrokeWidth(12);

        Arc arc = new Arc(43, 43, 34, 34, 90, -percentValue * 3.6);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#10b981"));
        arc.setStrokeWidth(12);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);

        Label percent = new Label(percentValue + "%");
        percent.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        percent.setTextFill(Color.web(C_TEXT_DARK));

        donut.getChildren().addAll(base, arc, percent);
        return donut;
    }

    private List<HBox> buildRoomTypeRows(List<Phong> rooms) {
        Map<String, long[]> statsByType = new LinkedHashMap<>();
        for (Phong room : rooms) {
            String roomType = room.getLoaiPhong() != null ? room.getLoaiPhong().toString() : "Khác";
            long[] stats = statsByType.computeIfAbsent(roomType, key -> new long[2]);
            if (room.getTrangThai() == TrangThaiPhong.CONTRONG) {
                stats[0]++;
            }
            stats[1]++;
        }

        List<HBox> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : statsByType.entrySet()) {
            rows.add(buildRoomTypeRow(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        if (rows.isEmpty()) {
            rows.add(buildRoomTypeRow("Chưa có phòng", 0, 0));
        }
        return rows;
    }

    private HBox buildRoomTypeRow(String name, long available, long total) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(name);
        label.setFont(Font.font("Segoe UI", 11));
        label.setTextFill(Color.web(C_TEXT_DARK));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label(available + "/" + total);
        value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        value.setTextFill(Color.web(C_TEXT_DARK));

        row.getChildren().addAll(label, spacer, value);
        return row;
    }

    private HBox buildTaskRow(String iconText, String text, String background, String color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-border-color: " + color + "22;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-radius: 4;");

        Label icon = new Label(iconText);
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        icon.setTextFill(Color.web(color));

        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        label.setTextFill(Color.web(C_TEXT_DARK));

        row.getChildren().addAll(icon, label);
        return row;
    }

    private HBox buildStatusRow(String name, long count, String color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Region badge = new Region();
        badge.setPrefSize(10, 10);
        badge.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 999;");

        Label label = new Label(name);
        label.setFont(Font.font("Segoe UI", 12));
        label.setTextFill(Color.web(C_TEXT_GRAY));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label(String.valueOf(count));
        value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        value.setTextFill(Color.web(C_TEXT_DARK));

        row.getChildren().addAll(badge, label, spacer, value);
        return row;
    }

    private VBox buildTimelineSection() {
        VBox card = createCardContainer();
        card.getChildren().add(buildSectionHeader("Lịch phòng", "Theo dõi trạng thái và lịch đặt phòng trong vòng 1 tháng."));

        floorFilterCombo = new ComboBox<>();
        floorFilterCombo.getItems().add("Tất cả tầng");
        floorFilterCombo.getItems().addAll(getAvailableFloors());
        floorFilterCombo.setValue(floorFilterCombo.getItems().contains("Tầng 1") ? "Tầng 1" : "Tất cả tầng");
        floorFilterCombo.setPrefWidth(120);

        roomTypeFilterCombo = new ComboBox<>();
        roomTypeFilterCombo.getItems().add("Tất cả loại");
        roomTypeFilterCombo.getItems().addAll(getAvailableRoomTypes());
        roomTypeFilterCombo.setValue("Tất cả loại");
        roomTypeFilterCombo.setPrefWidth(160);

        fromDateLabel = createDateRangeLabel();
        toDateLabel = createDateRangeLabel();
        updateTimelineDateLabels();

        Button previousMonthButton = createMonthButton("Trở lại");
        previousMonthButton.setOnAction(e -> changeTimelineMonth(-1));

        Button nextMonthButton = createMonthButton("Tiếp");
        nextMonthButton.setOnAction(e -> changeTimelineMonth(1));

        HBox filterRow = new HBox(12,
                createFilterGroup("Tầng", floorFilterCombo),
                createFilterGroup("Loại phòng", roomTypeFilterCombo),
                createFilterGroup("Từ", fromDateLabel),
                createFilterGroup("Đến", toDateLabel),
                createButtonGroup(previousMonthButton),
                createButtonGroup(nextMonthButton)
        );
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(0, 0, 12, 0));
        filterRow.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().add(filterRow);

        timelineScrollPane = new ScrollPane(buildTimelineGrid());
        timelineScrollPane.setBorder(Border.EMPTY);
        timelineScrollPane.setFitToWidth(true);
        timelineScrollPane.setPrefHeight(380);
        timelineScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        timelineScrollPane.getContent().setStyle("-fx-background-color: transparent;");

        setupTimelineAutoFilter();

        card.getChildren().add(timelineScrollPane);
        return card;
    }

    private Label createDateRangeLabel() {
        Label label = new Label();
        label.setMinWidth(140);
        label.setPrefWidth(140);
        label.setMinHeight(36);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPadding(new Insets(0, 12, 0, 12));
        label.setFont(Font.font("Segoe UI", 13));
        label.setTextFill(Color.web(C_TEXT_DARK));
        label.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-background-radius: 4;");
        return label;
    }

    private Button createMonthButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(36);
        button.setPadding(new Insets(0, 16, 0, 16));
        button.setCursor(Cursor.HAND);
        button.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;");
        return button;
    }

    private void setupTimelineAutoFilter() {
        floorFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshTimelineGrid());
        roomTypeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshTimelineGrid());
    }

    private void changeTimelineMonth(int amount) {
        timelineMonth = timelineMonth.plusMonths(amount);
        updateTimelineDateLabels();
        refreshTimelineGrid();
    }

    private void updateTimelineDateLabels() {
        if (fromDateLabel != null) {
            fromDateLabel.setText(timelineMonth.atDay(1).format(FILTER_DATE_FORMAT));
        }
        if (toDateLabel != null) {
            toDateLabel.setText(timelineMonth.atEndOfMonth().format(FILTER_DATE_FORMAT));
        }
    }

    private VBox buildTimelineGrid() {
        List<LocalDate> dates = buildDateRange();

        List<Phong> rooms = phongDAO.getAll();
        List<Phong> filteredRooms = filterRooms(rooms);

        VBox table = new VBox(14);
        table.setPadding(new Insets(8, 0, 0, 0));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        Label roomLabel = new Label("Phòng");
        roomLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        roomLabel.setTextFill(Color.web(C_TEXT_DARK));
        roomLabel.setPrefWidth(96);

        header.getChildren().add(roomLabel);
        for (LocalDate date : dates) {
            Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("EEE\ndd/MM", Locale.forLanguageTag("vi"))));
            dateLabel.setFont(Font.font("Segoe UI", 11));
            dateLabel.setTextFill(Color.web(C_TEXT_GRAY));
            dateLabel.setTextAlignment(TextAlignment.CENTER);
            dateLabel.setPrefWidth(60);
            dateLabel.setWrapText(true);
            header.getChildren().add(dateLabel);
        }
        table.getChildren().add(header);

        if (filteredRooms.isEmpty()) {
            Label emptyMessage = new Label("Không có phòng phù hợp với bộ lọc hiện tại.");
            emptyMessage.setFont(Font.font("Segoe UI", 12));
            emptyMessage.setTextFill(Color.web(C_TEXT_GRAY));
            table.getChildren().add(emptyMessage);
            return table;
        }

        for (Phong room : filteredRooms) {
            table.getChildren().add(buildTimelineRow(room, dates));
        }

        return table;
    }

    private VBox createFilterGroup(String title, Control control) {
        VBox group = new VBox(4);
        Label label = new Label(title);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        label.setTextFill(Color.web(C_TEXT_DARK));
        group.getChildren().addAll(label, control);
        return group;
    }

    private VBox createButtonGroup(Button button) {
        VBox group = new VBox(4);
        Label spacer = new Label("");
        spacer.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        group.getChildren().addAll(spacer, button);
        return group;
    }

    private List<String> getAvailableFloors() {
        List<Integer> floors = new ArrayList<>();
        for (Phong room : phongDAO.getAll()) {
            int floor = room.getSoTang();
            if (!floors.contains(floor)) {
                floors.add(floor);
            }
        }
        Collections.sort(floors);
        List<String> result = new ArrayList<>();
        for (Integer floor : floors) {
            result.add("Tầng " + floor);
        }
        return result;
    }

    private List<String> getAvailableRoomTypes() {
        List<String> types = new ArrayList<>();
        for (Phong room : phongDAO.getAll()) {
            String type = room.getLoaiPhong() != null ? room.getLoaiPhong().toString() : "Không xác định";
            if (!types.contains(type)) {
                types.add(type);
            }
        }
        Collections.sort(types);
        return types;
    }

    private List<Phong> filterRooms(List<Phong> rooms) {
        String selectedFloor = floorFilterCombo != null ? floorFilterCombo.getValue() : "Tất cả tầng";
        String selectedType = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : "Tất cả loại";

        List<Phong> filtered = new ArrayList<>();
        for (Phong room : rooms) {
            boolean matchesFloor = selectedFloor == null || selectedFloor.equals("Tất cả tầng")
                    || selectedFloor.equals("Tầng " + room.getSoTang());
            boolean matchesType = selectedType == null || selectedType.equals("Tất cả loại")
                    || selectedType.equals(room.getLoaiPhong() != null ? room.getLoaiPhong().toString() : "Không xác định");
            if (matchesFloor && matchesType) {
                filtered.add(room);
            }
        }
        return filtered;
    }

    private List<LocalDate> buildDateRange() {
        LocalDate fromDate = timelineMonth.atDay(1);
        LocalDate toDate = timelineMonth.atEndOfMonth();
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private void refreshTimelineGrid() {
        if (timelineScrollPane != null) {
            timelineScrollPane.setContent(buildTimelineGrid());
        }
    }

    private HBox buildTimelineRow(Phong room, List<LocalDate> dates) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label roomLabel = new Label(room.getMaPhong());
        roomLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        roomLabel.setTextFill(Color.web(C_TEXT_DARK));
        roomLabel.setPrefWidth(96);

        row.getChildren().add(roomLabel);
        for (LocalDate date : dates) {
            StackPane cell = createTimelineCell(room, date);
            row.getChildren().add(cell);
        }

        return row;
    }

    private StackPane createTimelineCell(Phong room, LocalDate date) {
        Color fill;
        String label = "";
        if (room.getTrangThai() == TrangThaiPhong.DACOKHACH) {
            fill = Color.web("#fde68a");
            label = "Đã có khách";
        } else if (room.getTrangThai() == TrangThaiPhong.BAN) {
            fill = Color.web("#fecaca");
            label = "Bảo trì";
        } else {
            fill = Color.web("#dcfce7");
        }

        Region box = new Region();
        box.setPrefSize(58, 28);
        box.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 12;",
                toHex(fill)));

        StackPane cell = new StackPane(box);
        cell.setPrefWidth(58);
        cell.setPrefHeight(28);
        cell.setAlignment(Pos.CENTER);
        cell.setStyle(date.equals(LocalDate.now())
                ? "-fx-border-color: #3b82f6; -fx-border-radius: 14;"
                : "");

        if (!label.isEmpty() && date.equals(LocalDate.now())) {
            Label status = new Label(label);
            status.setFont(Font.font("Segoe UI", 9));
            status.setTextFill(Color.web(C_TEXT_DARK));
            cell.getChildren().add(status);
        }

        return cell;
    }

    private VBox createCardContainer() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;");
        card.setEffect(new DropShadow(10, 0, 3, Color.web("#00000012")));
        return card;
    }

    private HBox buildSectionHeader(String titleText, String subtitleText) {
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 10, 0));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4);
        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(C_TEXT_DARK));

        Label subtitle = new Label(subtitleText);
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(C_TEXT_GRAY));

        text.getChildren().addAll(title, subtitle);
        header.getChildren().add(text);
        return header;
    }

    private static class BannerItem {
        private final String title;
        private final String subtitle;
        private final Image image;

        BannerItem(String title, String subtitle, Image image) {
            this.title = title;
            this.subtitle = subtitle;
            this.image = image;
        }

        String getTitle() { return title; }
        String getSubtitle() { return subtitle; }
        Image getImage() { return image; }
    }

    /*
     * ════════════════════════════════════════════════════════════════════
     * SƠ ĐỒ PHÒNG
     * ════════════════════════════════════════════════════════════════════
     */
    private HBox buildSectionHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(18, 22, 14, 22));
        h.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Sơ đồ phòng");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(C_TEXT_DARK));
        
        // TẠO LÒ XO (SPACER) ĐỂ ĐẨY CHÚ THÍCH SANG PHẢI, TÁCH RỜI KHỎI TITLE
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox legend = new HBox(36); 
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
                legendDot(COLOR_CONTRONG, TrangThaiPhong.CONTRONG.getLabel()),
                legendDot(COLOR_DACOKHACH, TrangThaiPhong.DACOKHACH.getLabel()),
                legendDot(COLOR_BAN, TrangThaiPhong.BAN.getLabel())
        );

        h.getChildren().addAll(title, spacer, legend);
        return h;
    }

    private HBox legendDot(Color color, String label) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER);
        
        Rectangle dot = new Rectangle(14, 14); 
        dot.setArcWidth(4);
        dot.setArcHeight(4);
        dot.setFill(color);
        
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web(C_TEXT_GRAY));
        
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private VBox buildRoomGrid() {
        VBox grid = new VBox(0);
        grid.setStyle("-fx-background-color: " + C_CARD_BG + ";");

        try {
            List<Phong> all = phongDAO.getAll();
            Map<Integer, List<Phong>> byFloor = new TreeMap<>();
            for (Phong p : all) {
                int floor = 0;
                try {
                    floor = p.getSoTang();
                } catch (Exception e) {}
                byFloor.computeIfAbsent(floor, k -> new ArrayList<>()).add(p);
            }

            for (Map.Entry<Integer, List<Phong>> entry : byFloor.entrySet()) {
                grid.getChildren().add(buildFloorSection(entry.getKey(), entry.getValue()));
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: " + C_BORDER + ";");
                grid.getChildren().add(sep);
            }
        } catch (Exception ignored) {
            Label err = new Label("Không thể tải dữ liệu phòng.");
            err.setPadding(new Insets(20));
            err.setTextFill(Color.web(C_TEXT_GRAY));
            grid.getChildren().add(err);
        }

        return grid;
    }

    private VBox buildFloorSection(int floorNum, List<Phong> rooms) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16, 22, 16, 22));

        Label floorLbl = new Label("TẦNG " + floorNum);
        floorLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        floorLbl.setTextFill(Color.web(C_TEXT_GRAY));

        FlowPane flow = new FlowPane(14, 14);
        flow.setPrefWrapLength(Double.MAX_VALUE);
        for (Phong p : rooms) {
            flow.getChildren().add(buildRoomCard(p));
        }

        section.getChildren().addAll(floorLbl, flow);
        return section;
    }

    private StackPane buildRoomCard(Phong phong) {
        TrangThaiPhong trangThai = phong.getTrangThai();
        String status = (trangThai != null) ? trangThai.toString() : "Không rõ";
        String maPhong = phong.getMaPhong();
        LoaiPhong loaiPhong = phong.getLoaiPhong();

        String loaiStr = (loaiPhong != null && loaiPhong.getTenLoai() != null)
                ? loaiPhong.getTenLoai() : "--";
        String priceStr = (loaiPhong != null && loaiPhong.getGiaPerNgay() > 0)
                ? String.format("%,.0f đ", loaiPhong.getGiaPerNgay()) : "";

        Color colorTop, colorBottom;
        if (trangThai == TrangThaiPhong.CONTRONG) {
            colorTop = Color.web("#22c55e");
            colorBottom = Color.web("#16a34a");
        } else if (trangThai == TrangThaiPhong.DACOKHACH) {
            colorTop = Color.web("#f59e0b");
            colorBottom = Color.web("#d97706");
        } else if (trangThai == TrangThaiPhong.BAN) {
            colorTop = Color.web("#ef4444");
            colorBottom = Color.web("#dc2626");
        } else {
            colorTop = COLOR_DEFAULT;
            colorBottom = Color.web("#6b7280");
        }

        StackPane card = new StackPane();
        card.setPrefSize(150, 96);
        card.setMinSize(150, 96);
        card.setMaxSize(150, 96);
        card.setCursor(Cursor.HAND);

        Region bg = new Region();
        bg.setPrefSize(147, 93);
        bg.setStyle(String.format(
                "-fx-background-color: linear-gradient(to bottom, %s, %s);" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 6, 0, 2, 2);",
                toHex(colorTop), toHex(colorBottom)));

        VBox content = new VBox(3);
        content.setPadding(new Insets(10, 14, 10, 14));
        content.setAlignment(Pos.TOP_LEFT);
        content.setPickOnBounds(false);

        Label lblName = new Label(maPhong);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblName.setTextFill(Color.WHITE);

        Label lblType = new Label(loaiStr);
        lblType.setFont(Font.font("Segoe UI", 10));
        lblType.setTextFill(Color.rgb(255, 255, 255, 0.80));

        Label lblStatus = new Label(status);
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblStatus.setTextFill(Color.WHITE);

        Label lblPrice = new Label(priceStr);
        lblPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblPrice.setTextFill(Color.WHITE);

        content.getChildren().addAll(lblName, lblType, lblPrice, lblStatus);
        card.getChildren().addAll(bg, content);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.04);
            card.setScaleY(1.04);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Window owner = getScene() != null ? getScene().getWindow() : null;
                new ChiTietPhongDialog(
                        owner, maPhong, loaiStr, priceStr, "Tầng " + phong.getSoTang(), status).show();
            }
        });

        return card;
    }

    /* ── Utility ─────────────────────────────────────────────────────── */
    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}
