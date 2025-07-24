package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.CreateWorldAPI;
import decok.dfcdvadstf.createworldui.api.CreateWorldAPI.IWorldTab;
import decok.dfcdvadstf.createworldui.api.CreateWorldAPI.ITabCustomizer;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorld extends GuiScreen {

    @Shadow private WorldSettings worldSettings;
    @Shadow private String field_146332_f; // 世界名称
    @Shadow private String field_146333_g; // 种子文本
    @Shadow private boolean field_146334_i; // 生成结构
    @Shadow private boolean field_146335_r; // 奖励箱

    private List<IWorldTab> activeTabs = new ArrayList<>();

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        // 加载注册的标签
        activeTabs = CreateWorldAPI.getTabs()
                .stream()
                .sorted(Comparator.comparingInt(IWorldTab::getTabOrder))
                .collect(Collectors.toList());

        // 添加标签按钮
        int xPos = 10;
        for (int i = 0; i < activeTabs.size(); i++) {
            IWorldTab tab = activeTabs.get(i);
            this.buttonList.add(new GuiButton(1000 + i, xPos, 5, 100, 20, tab.getTabName()));
            xPos += 105;
        }

        // 应用自定义器
        ITabCustomizer customizer = CreateWorldAPI.getCustomizer(GuiCreateWorld.class);
        if (customizer != null) {
            customizer.customize(this.buttonList);
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id >= 1000 && button.id < 1000 + activeTabs.size()) {
            IWorldTab tab = activeTabs.get(button.id - 1000);
            tab.initGui((GuiScreen)(Object)this, worldSettings);
            ci.cancel();
        }
    }
}