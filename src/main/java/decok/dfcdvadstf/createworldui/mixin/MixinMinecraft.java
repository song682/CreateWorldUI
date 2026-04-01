package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通过Mixin技术在Minecraft类中处理窗口resize事件，以保持GuiCreateWorld中的当前Tab
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow public net.minecraft.client.gui.GuiScreen currentScreen;

    // resize处理已移至MixinModernCreateWorld.onInitGuiTail
    // 因为Minecraft在resize时会重新调用GuiScreen.initGui方法
}