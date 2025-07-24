package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;

import java.util.List;

public interface InterFaceTabCustomizer {
    void customize(GuiSlot slot, List<GuiButton> buttonList);
}