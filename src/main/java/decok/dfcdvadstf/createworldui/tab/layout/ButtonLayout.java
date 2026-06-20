package decok.dfcdvadstf.createworldui.tab.layout;

import net.minecraft.client.gui.GuiButton;
import decok.dfcdvadstf.catframe.ui.layouts.ILayout;

import java.util.function.Consumer;

/**
 * A layout wrapper for {@link GuiButton} so it can participate in CatFrame layouts.
 * <p>{@link GuiButton} 的布局包装器，使其能够参与 CatFrame 布局系统。</p>
 *
 * <p>Usage / 用法:</p>
 * <pre>{@code
 * GridLayout layout = new GridLayout();
 * layout.addChild(new ButtonLayout(new GuiButton(1, 0, 0, 100, 20, "Click me")), 0, 0);
 * }</pre>
 */
public class ButtonLayout implements ILayout {

    private final GuiButton button;

    public ButtonLayout(GuiButton button) {
        this.button = button;
    }

    @Override
    public int getX() {
        return button.xPosition;
    }

    @Override
    public void setX(int x) {
        button.xPosition = x;
    }

    @Override
    public int getY() {
        return button.yPosition;
    }

    @Override
    public void setY(int y) {
        button.yPosition = y;
    }

    @Override
    public int getWidth() {
        return button.width;
    }

    @Override
    public int getHeight() {
        return button.height;
    }

    @Override
    public void visitWidgets(Consumer<Object> widgetVisitor) {
        widgetVisitor.accept(button);
    }

    /**
     * Get the wrapped button. / 获取被包装的按钮。
     */
    public GuiButton getButton() {
        return button;
    }
}
