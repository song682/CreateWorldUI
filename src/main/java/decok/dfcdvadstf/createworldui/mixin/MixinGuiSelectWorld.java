package decok.dfcdvadstf.createworldui.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveFormatComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <p>通过Mixin技术修改原版选择世界界面，当检测到没有存档时，直接跳转到创建世界界面</p>
 */
@Mixin(GuiSelectWorld.class)
public class MixinGuiSelectWorld extends GuiScreen {

    @Shadow
    protected GuiScreen field_146632_a;

    @Unique
    private static final Logger modernWorldCreatingUI$logger = LogManager.getLogger("MixinGuiSelectWorld");

    /**
     * 在初始化GUI时检测是否有存档，如果没有则直接跳转到创建世界界面
     */
    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void onInitGuiHead(CallbackInfo ci) {
        modernWorldCreatingUI$logger.info("Initializing GuiSelectWorld");

        // 检测是否有存档
        if (modernWorldCreatingUI$hasNoSaves()) {
            modernWorldCreatingUI$logger.info("No saves detected, redirecting to Create World screen");
            
            // 直接跳转到创建世界界面并取消原版initGui的执行
            this.mc.displayGuiScreen(new GuiCreateWorld(field_146632_a)); // 使用原版的父界面引用
            ci.cancel();
        }
    }

    /**
     * 检测是否有存档
     */
    @Unique
    private boolean modernWorldCreatingUI$hasNoSaves() {
        try {
            // 使用与原版代码相同的方法获取存档列表
            ISaveFormat saveFormat = this.mc.getSaveLoader();
            List saveList = saveFormat.getSaveList();
            
            if (saveList == null) {
                modernWorldCreatingUI$logger.warn("Could not get save list");
                return true; // 无法获取存档列表，视为没有存档
            }
            
            modernWorldCreatingUI$logger.info("Found {} save entries", saveList.size());
            
            return saveList.isEmpty();
        } catch (Exception e) {
            modernWorldCreatingUI$logger.error("Error checking for saves: ", e);
            return true; // 发生错误时，视为没有存档以确保用户能够创建新世界
        }
    }
}