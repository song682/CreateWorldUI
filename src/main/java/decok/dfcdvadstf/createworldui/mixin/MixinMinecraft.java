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

    /**
     * 在resize方法调用后注入代码，以保持GuiCreateWorld中的当前Tab
     */
    @Inject(method = "resize", at = @At("TAIL"))
    private void onResize(int width, int height, CallbackInfo ci) {
        // 检查当前屏幕是否为GuiCreateWorld
        if (currentScreen instanceof GuiCreateWorld) {
            // 尝试获取该界面的TabManager并重新初始化Tab以保持当前选中的Tab
            try {
                // 使用反射获取TabManager字段
                java.lang.reflect.Field tabManagerField = currentScreen.getClass().getDeclaredField("modernWorldCreatingUI$tabManager");
                tabManagerField.setAccessible(true);
                TabManager tabManager = (TabManager) tabManagerField.get(currentScreen);
                
                if (tabManager != null) {
                    // 重新初始化标签页并保持当前选中的标签页
                    tabManager.reinitializeTabs(width, height);
                }
            } catch (Exception e) {
                // 如果反射失败，静默处理，不影响其他功能
            }
        }
    }
}