package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * A cyclable button that supports both click and scroll-wheel cycling through values.
 * Left-click or scroll down cycles forward (+1); scroll up cycles backward (-1).
 * The controller is handled by {@link CycleHandler}
 * The displayed text is provided dynamically via a {@link TextSupplier}.
 * <p>
 * 支持单击和滚轮切换値的循环按钮。
 * 左键点击或向下滚动为正向(+1)；向上滚动为反向(-1)。
 * 处理方式由{@link CycleHandler}处理。
 * 显示的文本通过 {@link TextSupplier} 动态提供。
 */
public class GuiCyclableButton extends GuiButton {

    /**
     * Callback interface invoked when the button cycles.
     * {@code direction} is +1 for forward, -1 for backward.
     * <p>
     * 按钮切换时调用的回调接口。
     * {@code direction} 为 +1 表示正向，-1 表示反向。
     */
    public interface CycleHandler {
        void onCycle(int direction);
    }

    /**
     * Supplies the text to display on the button.
     * <p>
     * 提供按钮上显示文本的接口。
     */
    public interface TextSupplier {
        String getText();
    }

    private final CycleHandler handler;
    private final TextSupplier textSupplier;

    /**
     * Creates a new cyclable button.
     * <p>
     * 创建一个新的循环按钮。
     *
     * @param id           Button ID / 按钮 ID
     * @param x            X position / X 坐标
     * @param y            Y position / Y 坐标
     * @param width        Width / 宽度
     * @param height       Height / 高度
     * @param textSupplier Supplier for the display text / 提供显示文本的供应器
     * @param handler      Callback when cycling / 切换时的回调
     */
    public GuiCyclableButton(int id,
                             int x,
                             int y,
                             int width,
                             int height,
                             TextSupplier textSupplier,
                             CycleHandler handler) {

        super(id, x, y, width, height, "");
        this.handler = handler;
        this.textSupplier = textSupplier;

        updateText();
    }

    /**
     * Handles mouse scroll input to cycle through values.
     * Positive {@code delta} scrolls up (backward, -1); negative scrolls down (forward, +1).
     * <p>
     * 处理鼠标滚轮输入以切换値。
     * {@code delta} 为正向上滚动（反向，-1）；负向下滚动（正向，+1）。
     */
    public void mouseScrolled(int delta) {
        if (!enabled) return;
        int direction = Integer.signum(delta);
        if (direction == 0) return;
        if (handler != null) {
            handler.onCycle(direction);
        }
        updateText();
    }

    /**
     * Cycles forward (+1) on left-click.
     * <p>
     * 左键点击时正向切换(+1)。
     */
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY){
        if(super.mousePressed(mc, mouseX, mouseY)){
            if (handler != null) handler.onCycle(1);
            updateText();
            return true;
        }
        return false;
    }

    /**
     * Updates the button's display text by querying the {@link TextSupplier}.
     * <p>
     * 通过查询 {@link TextSupplier} 更新按钮显示文本。
     */
    public void updateText() {
        if (textSupplier != null) {
            this.displayString = textSupplier.getText();
        }
    }
}