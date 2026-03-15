package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiCyclableButton extends GuiButton {

    public interface CycleHandler {
        void onCycle(int direction);
    }

    public interface TextSupplier {
        String getText();
    }

    private final CycleHandler handler;
    private final TextSupplier textSupplier;

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

    public void mouseScrolled(int delta) {
        if (!enabled) return;
        int direction = Integer.signum(delta);
        if (direction == 0) return;
        if (handler != null) {
            handler.onCycle(direction);
        }
        updateText();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY){
        if(super.mousePressed(mc, mouseX, mouseY)){
            if (handler != null) handler.onCycle(1);
            updateText();
            return true;
        }
        return false;
    }

    public void updateText() {
        if (textSupplier != null) {
            this.displayString = textSupplier.getText();
        }
    }
}