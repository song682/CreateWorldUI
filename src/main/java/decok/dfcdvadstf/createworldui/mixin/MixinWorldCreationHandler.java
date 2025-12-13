package decok.dfcdvadstf.createworldui.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiCreateWorld.class)
public abstract class MixinWorldCreationHandler {

    /**
     * 保留原版世界名处理流程，
     * 只在“读取文本框内容”这一刻做兜底。
     */
    @WrapOperation(
            method = "func_146314_g",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiTextField;getText()Ljava/lang/String;"
            )
    )
    private String modernWorldCreatingUI$wrapNHandleWorldName(
            GuiTextField textField,
            Operation<String> original
    ) {
        // 原版文本
        String internalRealWorldName = original.call(textField).trim();

        // UI 可以空，但内部世界名不允许
        if (internalRealWorldName.isEmpty()) {
            return "New World"; // 与 vanilla 行为一致
            // 如果你想用 "New World" 或 I18n，也可以在这里换
        }

        return internalRealWorldName;
    }
}