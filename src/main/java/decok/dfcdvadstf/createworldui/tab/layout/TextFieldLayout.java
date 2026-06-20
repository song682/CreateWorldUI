package decok.dfcdvadstf.createworldui.tab.layout;

import net.minecraft.client.gui.GuiTextField;
import decok.dfcdvadstf.catframe.ui.layouts.ILayout;

import java.util.function.Consumer;

/**
 * A layout wrapper for {@link GuiTextField} so it can participate in CatFrame layouts.
 * <p>{@link GuiTextField} 的布局包装器，使其能够参与 CatFrame 布局系统。</p>
 *
 * <p>Usage / 用法:</p>
 * <pre>{@code
 * GridLayout layout = new GridLayout();
 * TextFieldLayout textFieldLayout = new TextFieldLayout(new GuiTextField(...));
 * layout.addChild(textFieldLayout, 0, 0);
 * }</pre>
 */
public class TextFieldLayout implements ILayout {

    private final GuiTextField textField;

    public TextFieldLayout(GuiTextField textField) {
        this.textField = textField;
    }

    @Override
    public int getX() {
        return textField.xPosition;
    }

    @Override
    public void setX(int x) {
        textField.xPosition = x;
    }

    @Override
    public int getY() {
        return textField.yPosition;
    }

    @Override
    public void setY(int y) {
        textField.yPosition = y;
    }

    @Override
    public int getWidth() {
        return textField.width;
    }

    @Override
    public int getHeight() {
        return textField.height;
    }

    @Override
    public void visitWidgets(Consumer<Object> widgetVisitor) {
        widgetVisitor.accept(textField);
    }

    /**
     * Get the wrapped text field. / 获取被包装的文本框。
     */
    public GuiTextField getTextField() {
        return textField;
    }
}
