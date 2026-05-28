package model.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Popup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * SmartDatePicker – DatePicker custom với ComboBox chọn nhanh tháng/năm.
 *
 * Trông như một ô nhập ngày duy nhất (giống DatePicker gốc).
 * Hỗ trợ nhập tay theo dạng dd/MM/yyyy với tự động thêm dấu "/".
 * Khi click nút 📅 → mở popup:
 * ┌─────────────────────────────────────┐
 * │ [< ] [Tháng ▾] [Năm ▾] [ >] │
 * ├─────────────────────────────────────┤
 * │ T2 T3 T4 T5 T6 T7 CN │
 * │ 1 2 3 4 5 6 7 │
 * │ 8 9 10 ... │
 * └─────────────────────────────────────┘
 *
 * Sử dụng:
 * SmartDatePicker dp = new SmartDatePicker();
 * dp.setValue(LocalDate.of(2000, 5, 15));
 * LocalDate date = dp.getValue();
 */
public class DatePicker extends HBox {

    /* ── Bảng màu ─────────────────────────────────────────────────── */
    private static final String C_BORDER = "#e9ecef";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_TODAY_BG = "#dbeafe";
    private static final String C_SELECT_BG = "#1d4ed8";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] WEEK_DAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
    private static final String[] MONTH_NAMES = {
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    };

    /* ── State ────────────────────────────────────────────────────── */
    private final ObjectProperty<LocalDate> value = new SimpleObjectProperty<>(this, "value");
    private YearMonth viewMonth; // tháng đang hiển thị trên popup
    private final TextField txtDisplay;
    private Popup popup;
    private GridPane calendarGrid;
    private ComboBox<String> cbMonth;
    private ComboBox<Integer> cbYear;

    private final int minYear;
    private final int maxYear;
    private LocalDate minDate;

    // Cờ để tránh vòng lặp khi setValue cập nhật text
    private boolean updatingFromCode = false;

    /*
     * ══════════════════════════════════════════════════════════════════
     * CONSTRUCTOR
     * ══════════════════════════════════════════════════════════════════
     */
    public DatePicker() {
        this(LocalDate.now().getYear() - 100, LocalDate.now().getYear());
    }

    /**
     * @param minYear năm nhỏ nhất trong danh sách
     * @param maxYear năm lớn nhất trong danh sách
     */
    public DatePicker(int minYear, int maxYear) {
        super(0);
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.viewMonth = YearMonth.now();
        this.minDate = null;

        setAlignment(Pos.CENTER_LEFT);
        setCursor(Cursor.HAND);

        /* ── Text field hiển thị – cho phép nhập tay ─────────────── */
        txtDisplay = new TextField();
        txtDisplay.setPromptText("dd/MM/yyyy");
        txtDisplay.setEditable(true);
        txtDisplay.setCursor(Cursor.TEXT);
        txtDisplay.setPrefHeight(40);
        HBox.setHgrow(txtDisplay, Priority.ALWAYS);
        txtDisplay.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 8 0 0 8;" +
                        "-fx-background-radius: 8 0 0 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 12 8 12;");

        // TextFormatter để chỉ cho nhập số và tự thêm dấu "/"
        setupDateTextFormatter();

        txtDisplay.focusedProperty().addListener((obs, o, focused) -> {
            if (focused) {
                txtDisplay.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: " + C_BLUE + ";" +
                                "-fx-border-radius: 8 0 0 8;" +
                                "-fx-background-radius: 8 0 0 8;" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 8 12 8 12;" +
                                "-fx-border-width: 2;");
            } else {
                txtDisplay.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: " + C_BORDER + ";" +
                                "-fx-border-radius: 8 0 0 8;" +
                                "-fx-background-radius: 8 0 0 8;" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 8 12 8 12;");
                // Khi mất focus, thử parse ngày nhập tay
                if (!updatingFromCode) {
                    parseManualInput();
                }
            }
        });

        /* ── Nút mở popup ────────────────────────────────────────── */
        Button btnOpen = new Button("📅");
        btnOpen.setPrefSize(42, 40);
        btnOpen.setMinWidth(42);
        btnOpen.setCursor(Cursor.HAND);
        btnOpen.setStyle(
                "-fx-background-color: " + C_NAVY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 0 8 8 0;" +
                        "-fx-border-radius: 0 8 8 0;" +
                        "-fx-cursor: hand;");
        btnOpen.setOnMouseEntered(e -> btnOpen.setStyle(
                "-fx-background-color: " + C_BLUE + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 0 8 8 0;" +
                        "-fx-border-radius: 0 8 8 0;" +
                        "-fx-cursor: hand;"));
        btnOpen.setOnMouseExited(e -> btnOpen.setStyle(
                "-fx-background-color: " + C_NAVY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 0 8 8 0;" +
                        "-fx-border-radius: 0 8 8 0;" +
                        "-fx-cursor: hand;"));

        getChildren().addAll(txtDisplay, btnOpen);

        /* ── Click nút → toggle popup ────────────────────────────── */
        btnOpen.setOnAction(e -> togglePopup());
    }

    /**
     * TextFormatter + listener: chỉ cho nhập số/"/", tự pad "0", tự thêm "/" và clamp ngày/tháng.
     */
    private void setupDateTextFormatter() {
        txtDisplay.setTextFormatter(new TextFormatter<>(change -> {
            if (updatingFromCode) return change;
            if (!change.isContentChange()) return change;
            String newText = change.getControlNewText();
            // Cho phép xóa
            if (newText.length() < change.getControlText().length()) return change;
            // Chỉ nhập số và "/"
            String addedText = change.getText();
            if (!addedText.isEmpty() && !addedText.matches("[0-9/]*")) return null;
            // Tối đa 10 ký tự
            if (newText.length() > 10) return null;
            return change;
        }));

        txtDisplay.textProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFromCode) return;
            if (newVal == null) return;
            // Chỉ xử lý khi đang thêm ký tự
            if (oldVal != null && newVal.length() <= oldVal.length()) return;

            // ── CASE 1: user gõ 1 chữ số rồi bấm "/" cho NGÀY (vd "1/") ─────
            // → pad thành "01/"
            if (newVal.length() == 2 && newVal.charAt(1) == '/'
                    && Character.isDigit(newVal.charAt(0))) {
                int day = safeParseInt(String.valueOf(newVal.charAt(0)), 1);
                if (day < 1) day = 1;
                updatingFromCode = true;
                txtDisplay.setText(String.format("%02d/", day));
                txtDisplay.positionCaret(3);
                updatingFromCode = false;
                return;
            }

            // ── CASE 2: user gõ đủ 2 chữ số ngày (vd "13") → thêm "/" ───────
            if (newVal.length() == 2 && !newVal.contains("/")) {
                int day = safeParseInt(newVal, 1);
                if (day < 1) day = 1;
                if (day > 31) day = 31;
                updatingFromCode = true;
                txtDisplay.setText(String.format("%02d/", day));
                txtDisplay.positionCaret(3);
                updatingFromCode = false;
                return;
            }

            // ── CASE 3: user gõ 1 chữ số rồi bấm "/" cho THÁNG (vd "01/3/") ─
            // pattern: \d{2}/\d/ (length 5)
            if (newVal.length() == 5 && newVal.charAt(4) == '/'
                    && newVal.matches("\\d{2}/\\d/")) {
                int month = safeParseInt(String.valueOf(newVal.charAt(3)), 1);
                if (month < 1) month = 1;
                if (month > 12) month = 12;
                String dayPart = newVal.substring(0, 2);
                // Clamp ngày theo tháng (tạm dùng năm hiện tại)
                int day = safeParseInt(dayPart, 1);
                int maxDay = maxDaysInMonth(month, LocalDate.now().getYear());
                if (day > maxDay) day = maxDay;
                updatingFromCode = true;
                txtDisplay.setText(String.format("%02d/%02d/", day, month));
                txtDisplay.positionCaret(6);
                updatingFromCode = false;
                return;
            }

            // ── CASE 4: user gõ đủ 2 chữ số tháng (vd "01/05") → thêm "/" ───
            if (newVal.length() == 5 && newVal.charAt(2) == '/'
                    && !newVal.substring(3).contains("/")) {
                String mmPart = newVal.substring(3);
                if (mmPart.length() == 2 && mmPart.matches("\\d{2}")) {
                    int month = safeParseInt(mmPart, 1);
                    if (month < 1) month = 1;
                    if (month > 12) month = 12;
                    String dayPart = newVal.substring(0, 2);
                    int day = safeParseInt(dayPart, 1);
                    int maxDay = maxDaysInMonth(month, LocalDate.now().getYear());
                    if (day > maxDay) day = maxDay;
                    updatingFromCode = true;
                    txtDisplay.setText(String.format("%02d/%02d/", day, month));
                    txtDisplay.positionCaret(6);
                    updatingFromCode = false;
                }
                return;
            }

            // ── CASE 5: đủ 10 ký tự dd/MM/yyyy → validate + clamp đầy đủ ────
            if (newVal.length() == 10) {
                validateAndClampDate(newVal);
            }
        });
    }

    /**
     * Validate và tự động clamp ngày khi nhập đủ dd/MM/yyyy.
     * VD: "31/04/2024" → "30/04/2024" vì tháng 4 chỉ có 30 ngày.
     */
    private void validateAndClampDate(String text) {
        if (text == null || text.length() != 10) return;
        String[] parts = text.split("/");
        if (parts.length != 3) return;
        try {
            int day   = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year  = Integer.parseInt(parts[2]);

            if (month < 1)  month = 1;
            if (month > 12) month = 12;
            int maxDay = maxDaysInMonth(month, year);
            if (day < 1)      day = 1;
            if (day > maxDay) day = maxDay;

            String corrected = String.format("%02d/%02d/%04d", day, month, year);
            if (!corrected.equals(text)) {
                updatingFromCode = true;
                txtDisplay.setText(corrected);
                txtDisplay.positionCaret(10);
                updatingFromCode = false;
            }

            if (year < minYear || year > maxYear) return;
            LocalDate parsed = LocalDate.of(year, month, day);
            if (minDate != null && parsed.isBefore(minDate)) return;

            updatingFromCode = true;
            this.value.set(parsed);
            viewMonth = YearMonth.from(parsed);
            updatingFromCode = false;
        } catch (Exception ignored) {}
    }

    /** Số ngày tối đa trong tháng, có xét năm nhuận. */
    private int maxDaysInMonth(int month, int year) {
        return switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11            -> 30;
            case 2                      -> isLeapYear(year) ? 29 : 28;
            default                     -> 31;
        };
    }

    /** Kiểm tra năm nhuận. */
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /** Parse int an toàn. */
    private int safeParseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s); } catch (Exception e) { return defaultVal; }
    }

    /**
     * Parse ngày từ text nhập tay khi mất focus — delegate sang validateAndClampDate.
     */
    private void parseManualInput() {
        String text = txtDisplay.getText();
        if (text == null || text.isEmpty()) return;
        if (text.length() == 10) {
            validateAndClampDate(text);
        } else {
            try {
                LocalDate parsed = LocalDate.parse(text, FMT);
                if (minDate != null && parsed.isBefore(minDate)) return;
                if (parsed.getYear() < minYear || parsed.getYear() > maxYear) return;
                updatingFromCode = true;
                this.value.set(parsed);
                viewMonth = YearMonth.from(parsed);
                updatingFromCode = false;
            } catch (DateTimeParseException ignored) {}
        }
    }

    /**
     * Override requestFocus: trỏ focus vào ô text bên trong (dùng cho focusFirstError).
     */
    @Override
    public void requestFocus() {
        if (txtDisplay != null) txtDisplay.requestFocus();
        else super.requestFocus();
    }

    /**
     * Gắn handler khi user nhấn Enter bên trong DatePicker.
     * Dùng thay cho EventUtils.setupEnterToSave vì key event nằm trong txtDisplay.
     */
    public void setOnEnterPressed(Runnable action) {
        if (txtDisplay != null && action != null) {
            txtDisplay.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    e.consume();
                    action.run();
                }
            });
        }
    }

    /**
     * Constructor mới nhận vào ngày mặc định ban đầu.
     */
    public DatePicker(LocalDate initialDate) {
        this();
        setValue(initialDate);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * GET / SET VALUE
     * ══════════════════════════════════════════════════════════════════
     */
    public LocalDate getValue() {
        return value.get();
    }

    public void setValue(LocalDate date) {
        updatingFromCode = true;
        this.value.set(date);
        if (date != null) {
            txtDisplay.setText(date.format(FMT));
            viewMonth = YearMonth.from(date);
        } else {
            txtDisplay.setText("");
        }
        updatingFromCode = false;
    }

    public ObjectProperty<LocalDate> valueProperty() {
        return value;
    }

    public void setMinDate(LocalDate minDate) {
        this.minDate = minDate;
        refreshCalendar();
    }

    public void setPromptText(String prompt) {
        txtDisplay.setPromptText(prompt);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * POPUP
     * ══════════════════════════════════════════════════════════════════
     */
    private void togglePopup() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
            return;
        }
        showPopup();
    }

    private void showPopup() {
        if (popup == null)
            popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().clear();
        popup.getContent().add(buildPopupContent());

        // Vị trí ngay dưới text field
        var bounds = localToScreen(getBoundsInLocal());
        if (bounds != null) {
            popup.show(getScene().getWindow(),
                    bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    private VBox buildPopupContent() {
        VBox box = new VBox(0);
        box.setPrefWidth(320);
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);");

        box.getChildren().addAll(buildNavBar(), buildWeekHeader(), buildCalendarGrid());
        refreshCalendar();
        return box;
    }

    /* ── Nav bar: [<] [Tháng ▾] [Năm ▾] [>] ──────────────────────── */
    private HBox buildNavBar() {
        HBox nav = new HBox(8);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(14, 14, 10, 14));
        nav.setStyle("-fx-background-color: " + C_NAVY + ";" +
                "-fx-background-radius: 12 12 0 0;");

        Button btnPrev = navArrow("◂");
        btnPrev.setOnAction(e -> {
            viewMonth = viewMonth.minusMonths(1);
            syncCombosFromView();
            refreshCalendar();
        });

        // ── ComboBox Tháng ──────────────────────────────────────
        cbMonth = new ComboBox<>();
        cbMonth.getItems().addAll(MONTH_NAMES);
        cbMonth.getSelectionModel().select(viewMonth.getMonthValue() - 1);
        cbMonth.setPrefHeight(32);
        cbMonth.setPrefWidth(105);
        cbMonth.setStyle(
                "-fx-font-size: 12px; -fx-font-family: 'Segoe UI';" +
                        "-fx-background-radius: 6; -fx-border-radius: 6;" +
                        "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-text-fill: white; -fx-prompt-text-fill: white;" +
                        "-fx-border-color: rgba(255,255,255,0.3);");
        // FIX: Đảm bảo text hiển thị bên trong ComboBox cũng màu trắng
        cbMonth.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.WHITE);
                    setFont(Font.font("Segoe UI", 12));
                }
            }
        });
        cbMonth.setOnAction(e -> {
            int m = cbMonth.getSelectionModel().getSelectedIndex() + 1;
            if (m > 0) {
                viewMonth = viewMonth.withMonth(m);
                refreshCalendar();
            }
        });

        // ── ComboBox Năm ────────────────────────────────────────
        cbYear = new ComboBox<>();
        for (int y = maxYear; y >= minYear; y--)
            cbYear.getItems().add(y);
        cbYear.setValue(viewMonth.getYear());
        cbYear.setPrefHeight(32);
        cbYear.setPrefWidth(85);
        cbYear.setStyle(
                "-fx-font-size: 12px; -fx-font-family: 'Segoe UI';" +
                        "-fx-background-radius: 6; -fx-border-radius: 6;" +
                        "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-text-fill: white; -fx-prompt-text-fill: white;" +
                        "-fx-border-color: rgba(255,255,255,0.3);");
        // FIX: Đảm bảo text hiển thị bên trong ComboBox cũng màu trắng
        cbYear.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setTextFill(Color.WHITE);
                    setFont(Font.font("Segoe UI", 12));
                }
            }
        });
        cbYear.setOnAction(e -> {
            Integer y = cbYear.getValue();
            if (y != null) {
                viewMonth = viewMonth.withYear(y);
                refreshCalendar();
            }
        });

        Button btnNext = navArrow("▸");
        btnNext.setOnAction(e -> {
            viewMonth = viewMonth.plusMonths(1);
            syncCombosFromView();
            refreshCalendar();
        });

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        nav.getChildren().addAll(btnPrev, sp1, cbMonth, cbYear, sp2, btnNext);
        return nav;
    }

    /* ── Header thứ: T2 T3 T4 T5 T6 T7 CN ─────────────────── */
    private GridPane buildWeekHeader() {
        GridPane g = new GridPane();
        g.setPadding(new Insets(8, 14, 4, 14));
        g.setHgap(0);
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(WEEK_DAYS[i]);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            lbl.setTextFill(Color.web(i >= 5 ? "#dc2626" : "#6b7280"));
            lbl.setAlignment(Pos.CENTER);
            lbl.setPrefSize(320.0 / 7 - 4, 24);
            lbl.setMaxWidth(Double.MAX_VALUE);
            g.add(lbl, i, 0);
            GridPane.setHgrow(lbl, Priority.ALWAYS);
        }
        return g;
    }

    /* ── Lưới ngày 7×6 ───────────────────────────────────────────── */
    private GridPane buildCalendarGrid() {
        calendarGrid = new GridPane();
        calendarGrid.setPadding(new Insets(2, 14, 14, 14));
        calendarGrid.setHgap(0);
        calendarGrid.setVgap(2);
        return calendarGrid;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * REFRESH CALENDAR GRID
     * ══════════════════════════════════════════════════════════════════
     */
    private void refreshCalendar() {
        if (calendarGrid == null)
            return;
        calendarGrid.getChildren().clear();

        LocalDate first = viewMonth.atDay(1);
        int dow = first.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        int offset = dow - 1; // số ô trống đầu
        int daysInMonth = viewMonth.lengthOfMonth();

        LocalDate today = LocalDate.now();
        LocalDate val = getValue();

        int row = 0;
        for (int i = 0; i < offset; i++) {
            calendarGrid.add(emptyCell(), i, row);
        }

        int col = offset;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = viewMonth.atDay(day);
            Button btn = dayButton(day, date, today, val);
            calendarGrid.add(btn, col, row);
            GridPane.setHgrow(btn, Priority.ALWAYS);
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
        // Ô trống cuối
        while (col > 0 && col < 7) {
            calendarGrid.add(emptyCell(), col, row);
            col++;
        }
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * HELPERS
     * ══════════════════════════════════════════════════════════════════
     */
    private void syncCombosFromView() {
        cbMonth.getSelectionModel().select(viewMonth.getMonthValue() - 1);
        cbYear.setValue(viewMonth.getYear());
    }

    private Button dayButton(int day, LocalDate date, LocalDate today, LocalDate val) {
        Button btn = new Button(String.valueOf(day));
        btn.setFont(Font.font("Segoe UI", 12));
        btn.setPrefSize(320.0 / 7 - 4, 34);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setCursor(Cursor.HAND);

        boolean isToday = date.equals(today);
        boolean isSelected = date.equals(val);
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isDisabled = minDate != null && date.isBefore(minDate);

        String bg = "transparent";
        String fg = isWeekend ? "#dc2626" : "#1f2937";
        String radius = "50";

        if (isSelected) {
            bg = C_SELECT_BG;
            fg = "white";
        } else if (isToday) {
            bg = C_TODAY_BG;
            fg = C_BLUE;
        }

        if (isDisabled) {
            fg = "#d1d5db"; // màu xám nhạt
            btn.setDisable(true);
        }

        String style = "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: " + radius + ";" +
                "-fx-border-radius: " + radius + ";" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: " + (isDisabled ? "default" : "hand") + ";";
        btn.setStyle(style);

        if (!isDisabled) {
            String hoverStyle = style.replace(
                    "-fx-background-color: " + bg,
                    "-fx-background-color: " + (isSelected ? C_SELECT_BG : "#e0e7ff"));

            btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(style));

            btn.setOnAction(e -> {
                setValue(date);
                if (popup != null)
                    popup.hide();
            });
        }

        return btn;
    }

    private Region emptyCell() {
        Region r = new Region();
        r.setPrefSize(320.0 / 7 - 4, 34);
        return r;
    }

    private Button navArrow(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setPrefSize(32, 32);
        btn.setCursor(Cursor.HAND);
        String normal = "-fx-background-color: rgba(255,255,255,0.1);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 50;" +
                "-fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(255,255,255,0.25);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 50;" +
                "-fx-cursor: hand;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }
}