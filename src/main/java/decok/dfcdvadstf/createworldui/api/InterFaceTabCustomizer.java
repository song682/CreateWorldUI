package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;

import java.util.List;

 /**
  * @author dfdvdsf
  * @version 0.0.1.6
  *
  * @deprecated - This InterFace is deprecated, integrated into CreateWorldAPI.ITabCustomizer
  * @see CreateWorldAPI.ITabCustomizer
  */
 @Deprecated
public interface InterFaceTabCustomizer {
    void customize(GuiSlot slot, List<GuiButton> buttonList);
}