package decok.dfcdvadstf.createworldui.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * <p>
 *     在{@link Minecraft}类中处理窗口resize事件，以保持{@link net.minecraft.client.gui.GuiCreateWorld}中的当前Tab<br>
 *     Handle the window resize event in the {@link Minecraft} class
 * </p>
 * <p>
 *     Resize handling has been moved to MixinModernCreateWorld.onInitGuiTail
 *     because Minecraft calls GuiScreen.initGui() again when resizing <br>
 *     resize处理已移至MixinModernCreateWorld.onInitGuiTail，
 *     因为Minecraft在resize时会重新调用GuiScreen.initGui方法
 * </p>
 * @see MixinModernCreateWorld
 * @see Minecraft#func_147120_f()
 * @see GuiScreen#initGui()
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow public net.minecraft.client.gui.GuiScreen currentScreen;

}