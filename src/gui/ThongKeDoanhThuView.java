package gui;

import dao.HoaDonDAO;

import model.entities.HoaDon;
import model.entities.NhanVien;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ThongKeDoanhThuView extends BorderPane {

    private static final String C_BG = "#f1f5f9";
    private static final String C_CARD_BG = "linear-gradient(to bottom right, #ffffff, #f8fafc)";
    private static final String C_BORDER = "#e2e8f0";
    private static final String C_NAVY = "#1e3a8a";

    private HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private List<HoaDon> allHoaDon = new ArrayList<>();

    private ComboBox<String> cbGroupBy;
    private DatePicker dpFrom, dpTo;
    private Button btnExport;

    private Label lblFrom, lblTo;
    private ComboBox<Integer> cbMonth, cbQuarter, cbYear, cbToYear;
    private Label lblMonth, lblQuarter, lblYear, lblToYear;

    private Label lblTotalRev, lblTotalInv, lblAvgRev;
    private BarChart<String, Number> barChart;
    private LineChart<String, Number> lineChart;

    private ToggleButton btnThoiGian, btnPhong;
    private CheckBox cbHideBar;
    private VBox viewThoiGian, viewPhong;
    private BarChart<String, Number> barChartPhong;
    private PieChart pieChartPhong;

    private boolean isAdjustingDate = false;

    // Lưu lại dữ liệu hiện tại để xuất báo cáo
    private Map<String, Double> currentExportData = new LinkedHashMap<>();

    private NhanVien currentStaff;

    public ThongKeDoanhThuView(NhanVien currentStaff) {
        this.currentStaff = currentStaff;
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildContent());

        setupListeners();
        loadData();
    }

    private VBox buildHeader() {
        VBox topBox = new VBox(20);

        Label title = new Label("THỐNG KÊ DOANH THU");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(C_NAVY));

        HBox filterBox = new HBox(16);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(16));
        filterBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 12;");
        filterBox.setEffect(new DropShadow(10, 0, 4, Color.web("#0000000A")));

        Label lblFilter = new Label("Xem theo:");
        lblFilter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblFilter.setTextFill(Color.web("#475569"));

        cbGroupBy = new ComboBox<>(FXCollections.observableArrayList(
                "Ngày", "Tháng", "Quý", "Năm"));
        cbGroupBy.setValue("Tháng");
        cbGroupBy.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        lblFrom = new Label("Từ ngày:");
        lblFrom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblFrom.setTextFill(Color.web("#475569"));

        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                }
                return null;
            }
        };

        dpFrom = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpFrom.setConverter(converter);
        dpFrom.setPrefWidth(140);
        dpFrom.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        lblTo = new Label("Đến ngày:");
        lblTo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTo.setTextFill(Color.web("#475569"));

        dpTo = new DatePicker(LocalDate.now());
        dpTo.setConverter(converter);
        dpTo.setPrefWidth(140);
        dpTo.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        // Limit dpTo selectable dates to dpFrom -> dpFrom + 30 days
        dpTo.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate fromDate = dpFrom.getValue();
                if (fromDate != null) {
                    LocalDate maxDate = fromDate.plusDays(30);
                    if (item.isBefore(fromDate) || item.isAfter(maxDate)) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;"); // light pink for disabled
                    }
                }
            }
        });

        lblMonth = new Label("Tháng:");
        lblMonth.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblMonth.setTextFill(Color.web("#475569"));
        cbMonth = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        cbMonth.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        lblQuarter = new Label("Quý:");
        lblQuarter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblQuarter.setTextFill(Color.web("#475569"));
        cbQuarter = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        cbQuarter.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        lblYear = new Label("Năm:");
        lblYear.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblYear.setTextFill(Color.web("#475569"));
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear; i >= currentYear - 10; i--)
            years.add(i);
        cbYear = new ComboBox<>(years);
        cbYear.setEditable(true);
        cbYear.setPrefWidth(100);
        cbYear.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        lblToYear = new Label("Đến năm:");
        lblToYear.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblToYear.setTextFill(Color.web("#475569"));
        cbToYear = new ComboBox<>(years);
        cbToYear.setEditable(true);
        cbToYear.setPrefWidth(100);
        cbToYear.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 38; -fx-background-radius: 6;");

        StringConverter<Integer> intConverter = new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return LocalDate.now().getYear();
                }
            }
        };
        cbYear.setConverter(intConverter);
        cbToYear.setConverter(intConverter);

        cbMonth.setValue(LocalDate.now().getMonthValue());
        cbQuarter.setValue((LocalDate.now().getMonthValue() - 1) / 3 + 1);
        cbYear.setValue(currentYear);
        cbToYear.setValue(currentYear);

        // Spacer đẩy nút Export sang góc phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Các nút chuyển trang
        btnThoiGian = new ToggleButton("Thời gian");
        btnPhong = new ToggleButton("Loại phòng");
        ToggleGroup tg = new ToggleGroup();
        btnThoiGian.setToggleGroup(tg);
        btnPhong.setToggleGroup(tg);
        btnThoiGian.setSelected(true);

        cbHideBar = new CheckBox("Ẩn cột");
        cbHideBar.setFont(Font.font("Segoe UI", 13));
        cbHideBar.setTextFill(Color.web(C_NAVY));
        cbHideBar.setOnAction(e -> {
            if (!isAdjustingDate)
                updateDashboard();
        });

        String tbStyleActive = "-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-background-color: "
                + C_NAVY + "; -fx-text-fill: white;";
        String tbStyleInactive = "-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-background-color: transparent; -fx-text-fill: "
                + C_NAVY + "; -fx-border-color: " + C_NAVY + "; -fx-border-radius: 6;";

        btnThoiGian.setStyle(tbStyleActive);
        btnPhong.setStyle(tbStyleInactive);

        tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
                return;
            }
            if (newVal == btnThoiGian) {
                btnThoiGian.setStyle(tbStyleActive);
                btnPhong.setStyle(tbStyleInactive);
                viewThoiGian.setVisible(true);
                viewThoiGian.setManaged(true);
                viewPhong.setVisible(false);
                viewPhong.setManaged(false);
            } else {
                btnPhong.setStyle(tbStyleActive);
                btnThoiGian.setStyle(tbStyleInactive);
                viewThoiGian.setVisible(false);
                viewThoiGian.setManaged(false);
                viewPhong.setVisible(true);
                viewPhong.setManaged(true);
            }
            if (!isAdjustingDate)
                updateDashboard();
        });

        HBox toggleBox = new HBox(8, btnThoiGian, btnPhong);
        toggleBox.setAlignment(Pos.CENTER_LEFT);

        // Nút xuất báo cáo
        btnExport = new Button("📥 Xuất báo cáo");
        btnExport.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnExport.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnExport.setOnAction(e -> exportReport());

        filterBox.getChildren().addAll(lblFilter, cbGroupBy, lblFrom, dpFrom, lblTo, dpTo, lblMonth, cbMonth,
                lblQuarter, cbQuarter, lblYear, cbYear, lblToYear, cbToYear, spacer, toggleBox, btnExport);
        topBox.getChildren().addAll(title, filterBox);
        BorderPane.setMargin(topBox, new Insets(0, 0, 24, 0));

        updateFilterUI(cbGroupBy.getValue());

        return topBox;
    }

    private void setupListeners() {
        cbGroupBy.valueProperty().addListener((obs, oldV, newV) -> {
            updateFilterUI(newV);
            adjustEndDate();
            if (!isAdjustingDate)
                updateDashboard();
        });

        dpFrom.valueProperty().addListener((obs, oldV, newV) -> {
            adjustEndDate();
            if (!isAdjustingDate)
                updateDashboard();
        });

        dpTo.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isAdjustingDate)
                updateDashboard();
        });

        cbMonth.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isAdjustingDate)
                updateDashboard();
        });

        cbQuarter.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isAdjustingDate)
                updateDashboard();
        });

        cbYear.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isAdjustingDate)
                updateDashboard();
        });

        cbToYear.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isAdjustingDate)
                updateDashboard();
        });
    }

    private void updateFilterUI(String groupBy) {
        boolean isM = "Tháng".equals(groupBy);
        boolean isQ = "Quý".equals(groupBy);
        boolean isY = "Năm".equals(groupBy);
        boolean isNormal = "Ngày".equals(groupBy);

        lblFrom.setVisible(isNormal);
        lblFrom.setManaged(isNormal);
        dpFrom.setVisible(isNormal);
        dpFrom.setManaged(isNormal);
        lblTo.setVisible(isNormal);
        lblTo.setManaged(isNormal);
        dpTo.setVisible(isNormal);
        dpTo.setManaged(isNormal);

        lblMonth.setVisible(false);
        lblMonth.setManaged(false);
        cbMonth.setVisible(false);
        cbMonth.setManaged(false);

        lblQuarter.setVisible(isQ);
        lblQuarter.setManaged(isQ);
        cbQuarter.setVisible(isQ);
        cbQuarter.setManaged(isQ);

        lblYear.setVisible(isM || isQ || isY);
        lblYear.setManaged(isM || isQ || isY);
        lblYear.setText(isY ? "Từ năm:" : "Năm:");
        cbYear.setVisible(isM || isQ || isY);
        cbYear.setManaged(isM || isQ || isY);

        lblToYear.setVisible(isY);
        lblToYear.setManaged(isY);
        cbToYear.setVisible(isY);
        cbToYear.setManaged(isY);
    }

    private void adjustEndDate() {
        if (dpFrom.getValue() == null)
            return;
        LocalDate start = dpFrom.getValue();
        String groupBy = cbGroupBy.getValue();
        if (groupBy == null)
            return;

        LocalDate newEnd = dpTo.getValue();
        if ("Ngày".equals(groupBy)) {
            newEnd = start.plusDays(30);
            isAdjustingDate = true;
            dpTo.setValue(newEnd);
            isAdjustingDate = false;
        }
    }

    private VBox buildContent() {
        VBox content = new VBox(24);

        // 1. Stat Cards (Dùng chung cho cả 2 view)
        HBox cardsBox = new HBox(24);

        Object[] revCard = createStatCard("💰", "TỔNG DOANH THU", "0 đ", "#f59e0b", "#d97706");
        Object[] invCard = createStatCard("🧾", "TỔNG ĐƠN ĐẶT", "0", "#22c55e", "#16a34a");
        Object[] avgCard = createStatCard("📈", "TRUNG BÌNH / ĐƠN", "0 đ", "#3b82f6", "#2563eb");

        lblTotalRev = (Label) revCard[1];
        lblTotalInv = (Label) invCard[1];
        lblAvgRev = (Label) avgCard[1];

        VBox card1 = (VBox) revCard[0];
        VBox card2 = (VBox) invCard[0];
        VBox card3 = (VBox) avgCard[0];

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        cardsBox.getChildren().addAll(card1, card2, card3);

        // --- View 1: Theo Thời Gian ---
        viewThoiGian = new VBox(24);
        CategoryAxis xAxisBar = new CategoryAxis();
        xAxisBar.setLabel("Thời gian");
        xAxisBar.setTickLabelFont(Font.font("Segoe UI", 12));

        NumberAxis yAxisBar = new NumberAxis();
        yAxisBar.setLabel("Doanh thu (VND)");
        yAxisBar.setTickLabelFont(Font.font("Segoe UI", 12));
        yAxisBar.setMinorTickVisible(false);
        yAxisBar.setTickLabelGap(15);

        barChart = new BarChart<>(xAxisBar, yAxisBar);
        barChart.setAnimated(false);
        barChart.setLegendVisible(false);
        barChart.setPadding(new Insets(5, 10, 5, 30));

        CategoryAxis xAxisLine = new CategoryAxis();
        xAxisLine.setLabel("Thời gian"); // Thêm label để chiều cao khớp với BarChart
        xAxisLine.setTickLabelFont(Font.font("Segoe UI", 12));
        NumberAxis yAxisLine = new NumberAxis();
        yAxisLine.setLabel("Doanh thu (VND)"); // Thêm label để chiều rộng khớp với BarChart
        yAxisLine.setTickLabelFont(Font.font("Segoe UI", 12));
        yAxisLine.setMinorTickVisible(false);
        yAxisLine.setTickLabelGap(15); // Đồng bộ gap giống hệt BarChart

        lineChart = new LineChart<>(xAxisLine, yAxisLine);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(true);

        // CỰC KỲ QUAN TRỌNG: Đồng bộ Padding y hệt BarChart
        lineChart.setPadding(new Insets(5, 10, 5, 30));

        lineChart.getStylesheets().add("data:text/css," +
                ".chart-plot-background { -fx-background-color: transparent; }" +
                ".axis { -fx-tick-label-fill: transparent; -fx-tick-mark-visible: false; -fx-minor-tick-visible: false; }"
                +
                ".axis-label { -fx-text-fill: transparent; }" +
                ".chart-vertical-grid-lines { -fx-stroke: transparent; }" +
                ".chart-horizontal-grid-lines { -fx-stroke: transparent; }" +
                ".chart-series-line { -fx-stroke: #ef4444; -fx-stroke-width: 3px; }" +
                ".chart-line-symbol { -fx-background-color: #ef4444, white; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 4px; }");

        StackPane chartContainer1 = new StackPane(barChart, lineChart, cbHideBar);
        StackPane.setAlignment(cbHideBar, Pos.TOP_RIGHT);
        cbHideBar.setPadding(new Insets(10, 10, 0, 0));
        chartContainer1.setStyle(
                "-fx-background-color: white; -fx-padding: 24 24 24 40; -fx-background-radius: 12; -fx-border-color: "
                        + C_BORDER + "; -fx-border-radius: 12;");
        chartContainer1.setEffect(new DropShadow(10, 0, 4, Color.web("#0000000A")));
        VBox.setVgrow(chartContainer1, Priority.ALWAYS);
        viewThoiGian.getChildren().add(chartContainer1);

        // --- View 2: Theo Loại Phòng ---
        viewPhong = new VBox(24);
        HBox phongChartsBox = new HBox(24);

        CategoryAxis xAxisPhong = new CategoryAxis();
        xAxisPhong.setLabel("Loại phòng");
        xAxisPhong.setTickLabelFont(Font.font("Segoe UI", 12));

        NumberAxis yAxisPhong = new NumberAxis();
        yAxisPhong.setLabel("Số đêm");
        yAxisPhong.setTickLabelFont(Font.font("Segoe UI", 12));
        yAxisPhong.setMinorTickVisible(false);
        yAxisPhong.setTickLabelGap(10);

        barChartPhong = new BarChart<>(xAxisPhong, yAxisPhong);
        barChartPhong.setAnimated(false);
        barChartPhong.setLegendVisible(false);
        HBox.setHgrow(barChartPhong, Priority.ALWAYS);

        pieChartPhong = new PieChart();
        pieChartPhong.setAnimated(false);
        pieChartPhong.setLegendVisible(true);
        pieChartPhong.setLabelsVisible(false); // Tắt label bên trong để tránh đè chữ
        pieChartPhong.setLegendSide(javafx.geometry.Side.BOTTOM);
        HBox.setHgrow(pieChartPhong, Priority.ALWAYS);

        phongChartsBox.getChildren().addAll(barChartPhong, pieChartPhong);
        phongChartsBox.setStyle(
                "-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 12; -fx-border-color: " + C_BORDER
                        + "; -fx-border-radius: 12;");
        phongChartsBox.setEffect(new DropShadow(10, 0, 4, Color.web("#0000000A")));
        VBox.setVgrow(phongChartsBox, Priority.ALWAYS);
        viewPhong.getChildren().add(phongChartsBox);

        // Container chung
        viewThoiGian.setVisible(true);
        viewThoiGian.setManaged(true);
        viewPhong.setVisible(false);
        viewPhong.setManaged(false);

        StackPane chartsStack = new StackPane(viewThoiGian, viewPhong);
        VBox.setVgrow(chartsStack, Priority.ALWAYS);

        content.getChildren().addAll(cardsBox, chartsStack);
        return content;
    }

    private Object[] createStatCard(String icon, String title, String value, String colorLight, String colorDark) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;");
        card.setEffect(new DropShadow(10, 0, 4, Color.web("#0000000A")));

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(6);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTitle.setTextFill(Color.web("#64748b"));

        Label lblVal = new Label(value);
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        lblVal.setTextFill(Color.web("#0f172a"));

        textBox.getChildren().addAll(lblTitle, lblVal);

        StackPane badge = new StackPane();
        badge.setMinSize(56, 56);
        badge.setPrefSize(56, 56);
        Rectangle badgeBg = new Rectangle(56, 56);
        badgeBg.setArcWidth(16);
        badgeBg.setArcHeight(16);
        badgeBg.setFill(Color.web(colorLight, 0.15));
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI Emoji", 26));
        badge.getChildren().addAll(badgeBg, iconLbl);

        topRow.getChildren().addAll(textBox, badge);
        card.getChildren().add(topRow);

        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color: linear-gradient(to right, " + colorLight + ", " + colorDark
                + "); -fx-background-radius: 2;");
        card.getChildren().add(bar);

        return new Object[] { card, lblVal };
    }

    private void loadData() {
        new Thread(() -> {
            allHoaDon = hoaDonDAO.getAllWithKhachHang();

            dao.DichVuSuDungDAO dvsdDAO = new dao.DichVuSuDungDAO();
            for (HoaDon hd : allHoaDon) {
                double currentSumPhong = hoaDonDAO.getTongTienPhongCurrent(hd.getMaHD());
                java.util.List<model.entities.DichVuSuDung> listDV = dvsdDAO.findByMaHD(hd.getMaHD());
                double totalTienDV = listDV.stream().mapToDouble(model.entities.DichVuSuDung::getThanhTien).sum();

                hd.setTienPhong(currentSumPhong);
                hd.setTienDV(totalTienDV);
                hoaDonDAO.tinhDoanhThu(hd);
            }

            javafx.application.Platform.runLater(this::updateDashboard);
        }).start();
    }

    private void updateDashboard() {
        if (allHoaDon == null)
            return;

        LocalDate startDate, endDate;
        String groupBy = cbGroupBy.getValue();

        if ("Tháng".equals(groupBy)) {
            int y = cbYear.getValue();
            startDate = LocalDate.of(y, 1, 1);
            endDate = LocalDate.of(y, 12, 31);
        } else if ("Quý".equals(groupBy)) {
            int q = cbQuarter.getValue();
            int y = cbYear.getValue();
            startDate = LocalDate.of(y, (q - 1) * 3 + 1, 1);
            endDate = startDate.plusMonths(3).minusDays(1);
        } else if ("Năm".equals(groupBy)) {
            int y1 = cbYear.getValue();
            int y2 = cbToYear.getValue();
            if (y1 > y2) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            startDate = LocalDate.of(y1, 1, 1);
            endDate = LocalDate.of(y2, 12, 31);
        } else {
            startDate = dpFrom.getValue();
            endDate = dpTo.getValue();
            if (startDate == null)
                startDate = LocalDate.of(2000, 1, 1);
            if (endDate == null)
                endDate = LocalDate.now();
        }

        final LocalDate fStart = startDate;
        final LocalDate fEnd = endDate;

        List<HoaDon> filtered = allHoaDon.stream().filter(hd -> {
            LocalDate date = hd.getNgayTaoHD() != null ? hd.getNgayTaoHD().toLocalDate() : null;
            if (date == null)
                return false;
            return !date.isBefore(fStart) && !date.isAfter(fEnd);
        }).collect(Collectors.toList());

        // Tính stats
        double totalRevenue = filtered.stream()
                .mapToDouble(model.utils.RevenueCalculator::calculateActualRevenue)
                .sum();
        int totalInvoices = filtered.size();
        double avgRevenue = totalInvoices > 0 ? totalRevenue / totalInvoices : 0;

        lblTotalRev.setText(String.format("%,.0f đ", totalRevenue));
        lblTotalInv.setText(String.valueOf(totalInvoices));
        lblAvgRev.setText(String.format("%,.0f đ", avgRevenue));

        // Nhóm dữ liệu (sử dụng biến groupBy đã khai báo ở đầu hàm)
        Map<LocalDate, Double> chartData = new TreeMap<>();

        // Khởi tạo các cột rỗng để biểu đồ hiển thị đầy đủ
        if ("Tháng".equals(groupBy)) {
            int y = cbYear.getValue();
            for (int i = 1; i <= 12; i++) {
                chartData.put(LocalDate.of(y, i, 1), 0.0);
            }
        } else if ("Quý".equals(groupBy)) {
            int q = cbQuarter.getValue();
            int y = cbYear.getValue();
            int startMonth = (q - 1) * 3 + 1;
            for (int i = 0; i < 3; i++) {
                chartData.put(LocalDate.of(y, startMonth + i, 1), 0.0);
            }
        } else if ("Năm".equals(groupBy)) {
            int y1 = cbYear.getValue();
            int y2 = cbToYear.getValue();
            if (y1 > y2) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            for (int i = y1; i <= y2; i++) {
                chartData.put(LocalDate.of(i, 1, 1), 0.0);
            }
        }

        for (HoaDon hd : filtered) {
            LocalDate date = hd.getNgayTaoHD().toLocalDate();
            LocalDate sortKey;
            switch (groupBy) {
                case "Tuần":
                    sortKey = date
                            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    break;
                case "Tháng":
                case "Quý":
                    sortKey = date.withDayOfMonth(1);
                    break;
                case "Năm":
                    sortKey = LocalDate.of(date.getYear(), 1, 1);
                    break;
                case "Ngày":
                default:
                    sortKey = date;
                    break;
            }
            double dt = model.utils.RevenueCalculator.calculateActualRevenue(hd);
            chartData.put(sortKey, chartData.getOrDefault(sortKey, 0.0) + dt);
        }

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();

        currentExportData.clear(); // Reset dữ liệu xuất

        double maxValue = 0; // Lưu lại mức cao nhất để đồng bộ 2 trục Y

        for (Map.Entry<LocalDate, Double> entry : chartData.entrySet()) {
            LocalDate d = entry.getKey();
            Double val = entry.getValue();
            if (val > maxValue)
                maxValue = val;

            String label;
            switch (groupBy) {
                case "Tuần":
                    int week = d.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    label = "Tuần " + week + "/" + d.getYear();
                    break;
                case "Tháng":
                case "Quý":
                    label = d.format(monthFmt);
                    break;
                case "Năm":
                    label = d.format(yearFmt);
                    break;
                case "Ngày":
                default:
                    label = d.format(dayFmt);
                    break;
            }

            barSeries.getData().add(new XYChart.Data<>(label, val));
            lineSeries.getData().add(new XYChart.Data<>(label, val));
            currentExportData.put(label, val);
        }

        barChart.getData().clear();
        barChart.getData().add(barSeries);

        lineChart.getData().clear();
        lineChart.getData().add(lineSeries);

        // --- ĐỒNG BỘ TRỤC Y CHO BAR VÀ LINE ---
        double upperBound = maxValue + (maxValue * 0.2); // Tăng 20% để không bị che số
        if (upperBound == 0)
            upperBound = 1000;

        // Cố định tickUnit là số nguyên tròn trịa
        double tickUnit = Math.ceil(upperBound / 5.0);
        if (tickUnit == 0)
            tickUnit = 100;
        upperBound = tickUnit * 5;

        ((NumberAxis) barChart.getYAxis()).setAutoRanging(false);
        ((NumberAxis) barChart.getYAxis()).setLowerBound(0);
        ((NumberAxis) barChart.getYAxis()).setUpperBound(upperBound);
        ((NumberAxis) barChart.getYAxis()).setTickUnit(tickUnit);

        ((NumberAxis) lineChart.getYAxis()).setAutoRanging(false);
        ((NumberAxis) lineChart.getYAxis()).setLowerBound(0);
        ((NumberAxis) lineChart.getYAxis()).setUpperBound(upperBound);
        ((NumberAxis) lineChart.getYAxis()).setTickUnit(tickUnit);

        // Thêm Tooltip, Data Label & Style cho Bar
        // Hide bars if requested
        if (cbHideBar.isSelected()) {
            for (XYChart.Data<String, Number> data : barSeries.getData()) {
                if (data.getNode() != null && data.getNode() instanceof StackPane) {
                    StackPane sp = (StackPane) data.getNode();
                    sp.setStyle("-fx-background-color: transparent;");
                    sp.getChildren().clear(); // hide labels
                }
            }
        } else {
            for (XYChart.Data<String, Number> data : barSeries.getData()) {
                if (data.getNode() != null && data.getNode() instanceof StackPane) {
                    StackPane sp = (StackPane) data.getNode();
                    sp.setStyle(
                            "-fx-background-color: linear-gradient(to top, #1e3a8a, #3b82f6); -fx-background-radius: 4 4 0 0;");
                    Label dataLabel = new Label(String.format("%,.0f", data.getYValue()));
                    dataLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    dataLabel.setTextFill(Color.web("#1e3a8a"));
                    dataLabel.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
                    dataLabel.setTranslateY(-20);
                    StackPane.setAlignment(dataLabel, Pos.TOP_CENTER);
                    sp.getChildren().removeIf(n -> n instanceof javafx.scene.text.Text || n instanceof Label);
                    sp.getChildren().add(dataLabel);

                    Tooltip tooltip = new Tooltip(String.format("%,.0f VND", data.getYValue()));
                    tooltip.setFont(Font.font("Segoe UI", 14));
                    Tooltip.install(sp, tooltip);

                    sp.setOnMouseEntered(e -> sp.setStyle(
                            "-fx-background-color: linear-gradient(to top, #1e40af, #60a5fa); -fx-background-radius: 4 4 0 0;"));
                    sp.setOnMouseExited(e -> sp.setStyle(
                            "-fx-background-color: linear-gradient(to top, #1e3a8a, #3b82f6); -fx-background-radius: 4 4 0 0;"));
                }
            }
        }

        // --- CẬP NHẬT VIEW THEO LOẠI PHÒNG (SỐ ĐÊM SỬ DỤNG) ---
        Map<String, Double> soNgaySuDungPhong = hoaDonDAO.getSoNgaySuDungTheoLoaiPhong(fStart, fEnd);

        // Sắp xếp theo số đêm tăng dần
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(soNgaySuDungPhong.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue());

        // Tính tổng số đêm để tính %
        double tongSoDem = sortedEntries.stream().mapToDouble(Map.Entry::getValue).sum();

        XYChart.Series<String, Number> barPhongSeries = new XYChart.Series<>();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        ObservableList<String> sortedCategories = FXCollections.observableArrayList();

        double maxPhongValue = 0;
        for (Map.Entry<String, Double> entry : sortedEntries) {
            // Làm tròn số đêm để tránh bị lẻ (do CSDL lưu thời gian lưu trú là số thực do
            // phụ thu checkin/out)
            double val = Math.round(entry.getValue());
            if (val > maxPhongValue)
                maxPhongValue = val;

            sortedCategories.add(entry.getKey());
            barPhongSeries.getData().add(new XYChart.Data<>(entry.getKey(), val));

            // Tính %
            double percent = (tongSoDem > 0) ? (val / tongSoDem * 100) : 0;
            String pieLabel = String.format("%s (%.1f%%)", entry.getKey(), percent);
            pieData.add(new PieChart.Data(pieLabel, val));
        }

        CategoryAxis xAxisPhong = (CategoryAxis) barChartPhong.getXAxis();
        xAxisPhong.setCategories(sortedCategories);

        barChartPhong.getData().clear();
        barChartPhong.getData().add(barPhongSeries);

        pieChartPhong.setData(pieData);

        double upperBoundPhong = Math.ceil(maxPhongValue + (maxPhongValue * 0.2));
        if (upperBoundPhong == 0)
            upperBoundPhong = 10;
        double tickUnitPhong = Math.ceil(upperBoundPhong / 5.0);
        if (tickUnitPhong == 0)
            tickUnitPhong = 1;
        upperBoundPhong = tickUnitPhong * 5;

        ((NumberAxis) barChartPhong.getYAxis()).setAutoRanging(false);
        ((NumberAxis) barChartPhong.getYAxis()).setLowerBound(0);
        ((NumberAxis) barChartPhong.getYAxis()).setUpperBound(upperBoundPhong);
        ((NumberAxis) barChartPhong.getYAxis()).setTickUnit(tickUnitPhong);

        for (XYChart.Data<String, Number> data : barPhongSeries.getData()) {
            if (data.getNode() != null && data.getNode() instanceof StackPane) {
                StackPane sp = (StackPane) data.getNode();
                sp.setStyle(
                        "-fx-background-color: linear-gradient(to top, #047857, #34d399); -fx-background-radius: 4 4 0 0;");
                Label dataLabel = new Label(String.format("%,.0f", data.getYValue()));
                dataLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                dataLabel.setTextFill(Color.web("#047857"));
                dataLabel.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
                dataLabel.setTranslateY(-20);
                StackPane.setAlignment(dataLabel, Pos.TOP_CENTER);
                sp.getChildren().removeIf(n -> n instanceof javafx.scene.text.Text || n instanceof Label);
                sp.getChildren().add(dataLabel);

                Tooltip tooltip = new Tooltip(String.format("%,.0f đêm", data.getYValue()));
                tooltip.setFont(Font.font("Segoe UI", 14));
                Tooltip.install(sp, tooltip);

                sp.setOnMouseEntered(e -> sp.setStyle(
                        "-fx-background-color: linear-gradient(to top, #065f46, #6ee7b7); -fx-background-radius: 4 4 0 0;"));
                sp.setOnMouseExited(e -> sp.setStyle(
                        "-fx-background-color: linear-gradient(to top, #047857, #34d399); -fx-background-radius: 4 4 0 0;"));
            }
        }

        // Tooltip for PieChart
        for (PieChart.Data data : pieChartPhong.getData()) {
            Tooltip tooltip = new Tooltip(data.getName() + "\n" + String.format("%,.0f đêm", data.getPieValue()));
            tooltip.setFont(Font.font("Segoe UI", 14));
            Tooltip.install(data.getNode(), tooltip);
        }
    }

    // --- LOGIC XUẤT BÁO CÁO CSV ---
    // --- LOGIC XUẤT BÁO CÁO CSV (DANH SÁCH HÓA ĐƠN) ---
    private void exportReport() {
        try {
            if (allHoaDon == null || allHoaDon.isEmpty()) {
                hienThongBao(Alert.AlertType.WARNING, "Thông báo", "Danh sách hóa đơn trống!");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("BaoCaoDoanhThu_" + LocalDate.now().toString() + ".csv");
            File file = fileChooser.showSaveDialog(this.getScene().getWindow());

            if (file == null)
                return;

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,###", symbols);

            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.write('\ufeff');

                String tuNgay = dpFrom.getValue() != null ? dpFrom.getValue().toString() : "Đầu kỳ";
                String denNgay = dpTo.getValue() != null ? dpTo.getValue().toString() : "Hiện tại";

                writer.println("BÁO CÁO CHI TIẾT DOANH THU");
                writer.println("Từ ngày: " + tuNgay + " đến ngày: " + denNgay);

                // CHỈ SỬA DÒNG BỊ LỖI LocalDateTime
                writer.println("Ngày xuất: " + java.time.LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

                writer.println();

                writer.println(
                        "Mã Hóa Đơn,Mã Đặt Phòng,Khách Hàng,Nhân Viên,Thời Gian,Tiền Phòng (VND),Tiền Dịch Vụ (VND),Thuế VAT (VND),Tiền Cọc (VND),Doanh Thu (VND)");

                double tongDoanhThu = 0;
                double tongTienPhong = 0;
                double tongTienDV = 0;
                double tongTienThue = 0;
                double tongTienCoc = 0;

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                for (HoaDon hd : allHoaDon) {

                    LocalDate ngayHD = hd.getNgayTaoHD().toLocalDate();

                    if (dpFrom.getValue() != null && ngayHD.isBefore(dpFrom.getValue()))
                        continue;
                    if (dpTo.getValue() != null && ngayHD.isAfter(dpTo.getValue()))
                        continue;

                    String trangThai = hd.getTrangThaiThanhToan();

                    String maHD = String.valueOf(hd.getMaHD());
                    String maDat = (hd.getDatPhong() != null)
                            ? String.valueOf(hd.getDatPhong().getMaDat())
                            : "N/A";

                    String tenKH = "Vãng lai";

                    if (hd.getDatPhong() != null
                            && hd.getDatPhong().getKhachHang() != null
                            && hd.getDatPhong().getKhachHang().getTenKH() != null) {

                        tenKH = hd.getDatPhong().getKhachHang().getTenKH();
                    }

                    String tenNV = (hd.getNhanVien() != null
                            && hd.getNhanVien().getHoTen() != null)
                                    ? hd.getNhanVien().getHoTen()
                                    : "N/A";

                    String thoiGian = (hd.getNgayTaoHD() != null)
                            ? ("=\"" + hd.getNgayTaoHD().format(dtf) + "\"")
                            : "N/A";

                    double giaTriDoanhThu = model.utils.RevenueCalculator.calculateActualRevenue(hd);

                    if (giaTriDoanhThu <= 0)
                        continue;

                    double tienPhong = hd.getTienPhong();
                    double tienDV = hd.getTienDV();

                    double thueVATPercent = (hd.getThueVAT() > 0)
                            ? hd.getThueVAT()
                            : 0.10;

                    double tienThue = (tienPhong + tienDV) * thueVATPercent;
                    double tienCoc = hd.getTienCoc();

                    tongTienPhong += tienPhong;
                    tongTienDV += tienDV;
                    tongTienThue += tienThue;
                    tongTienCoc += tienCoc;
                    tongDoanhThu += giaTriDoanhThu;

                    String tpStr = "=\"" + df.format(tienPhong) + "\"";
                    String tdvStr = "=\"" + df.format(tienDV) + "\"";
                    String thueStr = "=\"" + df.format(tienThue) + "\"";
                    String tcStr = "=\"" + df.format(tienCoc) + "\"";
                    String doanhThuStr = "=\"" + df.format(giaTriDoanhThu) + "\"";

                    writer.println(
                            maHD + "," +
                                    maDat + "," +
                                    tenKH + "," +
                                    tenNV + "," +
                                    thoiGian + "," +
                                    tpStr + "," +
                                    tdvStr + "," +
                                    thueStr + "," +
                                    tcStr + "," +
                                    doanhThuStr);
                }

                writer.println();

                writer.println(
                        "TỔNG CỘNG,,,,," +
                                "=\"" + df.format(tongTienPhong) + "\"," +
                                "=\"" + df.format(tongTienDV) + "\"," +
                                "=\"" + df.format(tongTienThue) + "\"," +
                                "=\"" + df.format(tongTienCoc) + "\"," +
                                "=\"" + df.format(tongDoanhThu) + "\"");

                hienThongBao(
                        Alert.AlertType.INFORMATION,
                        "Thành công",
                        "Xuất file báo cáo thành công!");
            }

        } catch (Exception e) {
            e.printStackTrace();

            hienThongBao(
                    Alert.AlertType.ERROR,
                    "Lỗi",
                    "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Hàm hiển thị thông báo
    private void hienThongBao(Alert.AlertType type,
            String title,
            String content) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.showAndWait();
    }
}