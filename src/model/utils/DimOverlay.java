package model.utils;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * DimOverlay – Tiện ích làm tối nền khi mở dialog.
 *
 * Cách dùng:
 *   Region overlay = DimOverlay.show(ownerWindow);
 *   dialog.showAndWait();
 *   DimOverlay.hide(ownerWindow, overlay);
 */
public final class DimOverlay {

    private static final String WRAPPER_ID = "__dim_overlay_wrapper__";
    private static final String OVERLAY_ID = "__dim_overlay__";

    private DimOverlay() {}

    /**
     * Thêm lớp phủ tối lên cửa sổ chủ.
     */
    public static Region show(Window owner) {
        if (owner == null || owner.getScene() == null) {
            return null;
        }

        Parent root = owner.getScene().getRoot();
        StackPane wrapper = ensureWrapper(owner, root);

        // Dọn overlay cũ nếu lần trước bị kẹt, tránh tạo lớp trong suốt chặn chuột.
        wrapper.getChildren().removeIf(node -> OVERLAY_ID.equals(node.getId()));

        Region overlay = new Region();
        overlay.setId(OVERLAY_ID);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlay.setMouseTransparent(false); // Khi dialog đang mở thì overlay chặn click nền là đúng.
        overlay.setOpacity(0);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(overlay, javafx.geometry.Pos.CENTER);

        wrapper.getChildren().add(overlay);

        FadeTransition ft = new FadeTransition(Duration.millis(250), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return overlay;
    }

    /**
     * Gỡ lớp phủ tối khỏi cửa sổ chủ.
     */
    public static void hide(Window owner, Region overlay) {
        if (owner == null || owner.getScene() == null) {
            return;
        }

        if (overlay == null) {
            cleanupLeftoverOverlays(owner);
            return;
        }

        // Quan trọng: trong lúc fade out, overlay không được chặn chuột nữa.
        overlay.setMouseTransparent(true);

        Parent root = owner.getScene().getRoot();
        if (root instanceof StackPane wrapper && WRAPPER_ID.equals(wrapper.getId())) {
            FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
            ft.setFromValue(overlay.getOpacity());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                wrapper.getChildren().remove(overlay);
                unwrapIfPossible(owner, wrapper);
            });
            ft.play();
            return;
        }

        // Fallback: nếu root đã bị đổi hoặc wrapper không còn đúng id,
        // vẫn cố gỡ overlay khỏi parent để tránh một Region trong suốt chặn click.
        if (overlay.getParent() instanceof Pane pane) {
            pane.getChildren().remove(overlay);
        }
        cleanupLeftoverOverlays(owner);
    }

    private static StackPane ensureWrapper(Window owner, Parent root) {
        if (root instanceof StackPane sp && WRAPPER_ID.equals(sp.getId())) {
            return sp;
        }

        StackPane wrapper = new StackPane();
        wrapper.setId(WRAPPER_ID);
        owner.getScene().setRoot(wrapper);
        wrapper.getChildren().add(root);
        return wrapper;
    }

    private static void unwrapIfPossible(Window owner, StackPane wrapper) {
        cleanupWrapperOverlays(wrapper);

        if (wrapper.getChildren().size() == 1 && wrapper.getChildren().get(0) instanceof Parent original) {
            wrapper.getChildren().clear();
            owner.getScene().setRoot(original);
        }
    }

    private static void cleanupLeftoverOverlays(Window owner) {
        Parent root = owner.getScene().getRoot();
        if (root instanceof StackPane wrapper && WRAPPER_ID.equals(wrapper.getId())) {
            cleanupWrapperOverlays(wrapper);
            unwrapIfPossible(owner, wrapper);
        }
    }

    private static void cleanupWrapperOverlays(StackPane wrapper) {
        for (Node node : wrapper.getChildren()) {
            if (OVERLAY_ID.equals(node.getId())) {
                node.setMouseTransparent(true);
            }
        }
        wrapper.getChildren().removeIf(node -> OVERLAY_ID.equals(node.getId()));
    }
}
