package decok.dfcdvadstf.createworldui.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.EnumDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiOptions.class)
public class ModernOptions extends GuiScreen {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        // 更新难度按钮文本以反映当前设置
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.id == 108) { // 难度按钮的ID，可能需要根据实际调整
                    modernWorldCreatingUI$updateDifficultyButtonText(button);
                }
            }
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 108) { // 难度按钮
            // 延迟更新，确保游戏设置已更改
            net.minecraft.client.Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override
                public void run() {
                    modernWorldCreatingUI$updateDifficultyButtonText(button);
                }
            });
        }
    }

    @Unique
    private void modernWorldCreatingUI$updateDifficultyButtonText(GuiButton button) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.gameSettings != null) {
            EnumDifficulty difficulty = mc.gameSettings.difficulty;
            String difficultyName = "";
            switch (difficulty) {
                case PEACEFUL:
                    difficultyName = net.minecraft.client.resources.I18n.format("options.difficulty.peaceful");
                    break;
                case EASY:
                    difficultyName = net.minecraft.client.resources.I18n.format("options.difficulty.easy");
                    break;
                case NORMAL:
                    difficultyName = net.minecraft.client.resources.I18n.format("options.difficulty.normal");
                    break;
                case HARD:
                    difficultyName = net.minecraft.client.resources.I18n.format("options.difficulty.hard");
                    break;
            }
            button.displayString = net.minecraft.client.resources.I18n.format("options.difficulty") + ": " + difficultyName;
        }
    }
}