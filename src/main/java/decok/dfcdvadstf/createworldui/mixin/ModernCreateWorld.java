package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.TabState;
import decok.dfcdvadstf.createworldui.gamerule.GameRuleEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static decok.dfcdvadstf.createworldui.api.TextureHelper.drawModalRectWithCustomSizedTexture;


@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class ModernCreateWorld extends GuiScreen {

    /***
     * 
     */

    @Shadow
    private GuiScreen field_146332_f; //parentScreen
    @Shadow
    private boolean field_146337_w; // 硬核模式
    @Shadow
    private String field_146330_J; // 世界显示名称
    @Shadow
    private String field_146342_r; // 游戏模式的文本标识
    @Shadow
    private String field_146329_I; // 种子
    @Shadow
    private boolean field_146341_s; // 生成建筑
    @Shadow
    private boolean field_146338_v; // 奖励箱
    @Shadow
    private boolean field_146340_t; // 允许作弊
    @Shadow
    private GuiTextField field_146335_h; // 种子输入框
    @Shadow
    private GuiTextField field_146333_g; // 世界名称输入框
    @Shadow
    private GuiButton field_146321_E; // 允许作弊按钮
    @Shadow
    private GuiButton field_146343_z; // 游戏模式按钮
    @Shadow
    private GuiButton field_146326_C; // 奖励箱按钮
    @Shadow
    private GuiButton field_146320_D; // 世界类型选择按钮
    @Shadow
    private GuiButton field_146325_B; // 生成建筑
    @Shadow
    private GuiButton field_146322_F; // 自定义（预设）
    @Shadow
    private int field_146331_K; // 世界类型的索引

    @Unique
    private int modernWorldCreatingUI$currentTab = 100; // 100: Game, 101: World, 102: More
    @Unique
    private final List<GuiButton> modernWorldCreatingUI$tabButtons = new ArrayList<>();
    @Unique
    private final Map<Integer, GuiButton> modernWorldCreatingUI$tabControls = new HashMap<>();
    @Unique
    private static final ResourceLocation OPTIONS_BG_LIGHT = new ResourceLocation("createworldui:textures/gui/options_background.png");
    @Unique
    private static final ResourceLocation OPTIONS_BG_DARK = new ResourceLocation("createworldui:textures/gui/options_background_dark.png");
    @Unique
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("createworldui:textures/gui/tabs.png");
    @Unique
    private static final int TAB_WIDTH = 130;
    @Unique
    private static final int TAB_HEIGHT = 24;

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void initModernGui(CallbackInfo ci) {
        ci.cancel();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.modernWorldCreatingUI$tabButtons.clear();

        // 初始化输入框
        field_146333_g = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 40, 200, 20);
        field_146333_g.setFocused(true);
        field_146333_g.setText(this.field_146330_J);

        field_146335_h = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 100, 200, 20);
        field_146335_h.setText(this.field_146329_I);

        // 创建 Tab 按钮
        modernWorldCreatingUI$createTabButtons();

        // 添加原版的功能按钮（但根据当前 Tab 设置可见性）
        modernWorldCreatingUI$addOriginalButtons();

        // 更新按钮可见性
        modernWorldCreatingUI$updateButtonVisibility();
    }

    @Unique
    private void modernWorldCreatingUI$createTabButtons() {
        int xPos = this.width / 2 - 125;
        int yPos = 5;
        String[] tabNames = {
                I18n.format("createworldui.tab.game"),
                I18n.format("createworldui.tab.world"),
                I18n.format("createworldui.tab.more")
        };

        for (int i = 0; i < 3; i++) {
            GuiButton tabButton = new GuiButton(100 + i, xPos, yPos, TAB_WIDTH, TAB_HEIGHT, tabNames[i]) {
                @Override
                public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                    if (this.visible) {
                        mc.getTextureManager().bindTexture(TABS_TEXTURE);
                        boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                                mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                        boolean isSelected = modernWorldCreatingUI$currentTab == this.id;

                        TabState state = isSelected ?
                                (isHovered ? TabState.SELECTED_HOVER : TabState.SELECTED) :
                                (isHovered ? TabState.HOVER : TabState.NORMAL);

                        drawTexturedModalRect(this.xPosition, this.yPosition, state.u, state.v, TAB_WIDTH, TAB_HEIGHT);
                        drawCenteredString(mc.fontRenderer, this.displayString,
                                this.xPosition + this.width / 2,
                                this.yPosition + (this.height - 8) / 2, state.textColor);
                    }
                }
            };
            modernWorldCreatingUI$tabButtons.add(tabButton);
            this.buttonList.add(tabButton);
            xPos += TAB_WIDTH + 5;
        }
    }

    @Unique
    private void modernWorldCreatingUI$addOriginalButtons() {
        // 创建和取消按钮（始终显示在底部）
        this.buttonList.add(new GuiButton(0, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("selectWorld.create")));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("gui.cancel")));

        // 原版的功能按钮（位置调整到合适的位置）
        this.buttonList.add(this.field_146343_z = new GuiButton(2, this.width / 2 - 100, 140, 200, 20, ""));
        this.buttonList.add(this.field_146325_B = new GuiButton(4, this.width / 2 - 100, 165, 200, 20, ""));
        this.buttonList.add(this.field_146326_C = new GuiButton(7, this.width / 2 - 100, 190, 200, 20, ""));
        this.buttonList.add(this.field_146320_D = new GuiButton(5, this.width / 2 - 100, 215, 200, 20, ""));
        this.buttonList.add(this.field_146321_E = new GuiButton(6, this.width / 2 - 100, 240, 200, 20, ""));
        this.buttonList.add(this.field_146322_F = new GuiButton(8, this.width / 2 - 100, 265, 200, 20, I18n.format("selectWorld.customizeType")));
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, 140, 200, 20, I18n.format("createworldui.button.gameRuleEditor")));

        // 更新按钮文本
        modernWorldCreatingUI$updateButtonText();
    }

    @Unique
    private void modernWorldCreatingUI$updateButtonText() {
        // 更新游戏模式按钮文本
        this.field_146343_z.displayString = I18n.format("selectWorld.gameMode") + " " +
                I18n.format("selectWorld.gameMode." + this.field_146342_r);

        // 更新生成建筑按钮文本
        this.field_146325_B.displayString = I18n.format("selectWorld.mapFeatures") +
                (this.field_146341_s ? I18n.format("options.on") : I18n.format("options.off"));

        // 更新奖励箱按钮文本
        this.field_146326_C.displayString = I18n.format("selectWorld.bonusItems") +
                (this.field_146338_v && !this.field_146337_w ? I18n.format("options.on") : I18n.format("options.off"));

        // 更新世界类型按钮文本
        this.field_146320_D.displayString = I18n.format("selectWorld.mapType") +
                I18n.format(WorldType.worldTypes[this.field_146331_K].getTranslateName());

        // 更新允许作弊按钮文本
        this.field_146321_E.displayString = I18n.format("selectWorld.allowCommands") +
                (this.field_146340_t && !this.field_146337_w ? I18n.format("options.on") : I18n.format("options.off"));
    }

    @Unique
    private void modernWorldCreatingUI$updateButtonVisibility() {
        // 根据当前 Tab 显示/隐藏相应的按钮
        switch (modernWorldCreatingUI$currentTab) {
            case 100: // 游戏 Tab
                this.field_146343_z.visible = true;
                this.field_146321_E.visible = true;
                this.field_146325_B.visible = false;
                this.field_146326_C.visible = false;
                this.field_146320_D.visible = false;
                this.field_146322_F.visible = false;
                getButtonById(200).visible = false;
                break;
            case 101: // 世界 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.field_146325_B.visible = true;
                this.field_146326_C.visible = true;
                this.field_146320_D.visible = true;
                this.field_146322_F.visible = WorldType.worldTypes[this.field_146331_K].isCustomizable();
                getButtonById(200).visible = false;
                break;
            case 102: // 更多 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.field_146325_B.visible = false;
                this.field_146326_C.visible = false;
                this.field_146320_D.visible = false;
                this.field_146322_F.visible = false;
                getButtonById(200).visible = true;
                break;
        }

        // 更新按钮文本
        modernWorldCreatingUI$updateButtonText();
    }

    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawModalRectWithCustomSizedTexture(OPTIONS_BG_LIGHT, 0, 0, 16, 16,this.width, this.height, 16, 16);

        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);

        this.drawCenteredString(this.fontRendererObj, I18n.format("selectWorld.create"), this.width / 2, 15, 0xFFFFFF);

        // 根据当前 Tab 显示不同的输入框
        if (modernWorldCreatingUI$currentTab == 100) {
            // 游戏 Tab 显示世界名称
            this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), this.width / 2 - 100, 27, 0xA0A0A0);
            field_146333_g.drawTextBox();
        } else {
            // 世界和更多 Tab 显示种子
            this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterSeed"), this.width / 2 - 100, 87, 0xA0A0A0);
            field_146335_h.drawTextBox();
        }

        // 显示当前 Tab 的标题
        String tabTitle;
        switch (modernWorldCreatingUI$currentTab) {
            case 100:
                tabTitle = I18n.format("createworldui.tab.game");
                break;
            case 101:
                tabTitle = I18n.format("createworldui.tab.world");
                break;
            case 102:
                tabTitle = I18n.format("createworldui.tab.more");
                break;
            default:
                tabTitle = "";
                break;
        }
        this.drawCenteredString(this.fontRendererObj, tabTitle, this.width / 2, 120, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        // 处理 Tab 切换
        if (button.id >= 100 && button.id <= 102) {
            modernWorldCreatingUI$currentTab = button.id;
            modernWorldCreatingUI$updateButtonVisibility();
            ci.cancel();
            return;
        }

        // 处理游戏规则编辑器按钮
        if (button.id == 200) {
            // 打开游戏规则编辑器
            // 注意：这里传入null作为world参数，因为创建世界界面还没有world对象
            // 你可能需要修改GameRuleEditor来支持创建新世界时的游戏规则设置
            this.mc.displayGuiScreen(new GameRuleEditor(null));
            ci.cancel();
            return;
        }

        // 对于原版按钮，更新文本显示
        if (button.id == 2 || button.id == 4 || button.id == 5 || button.id == 6 || button.id == 7) {
            // 创建一个延迟更新任务
            modernWorldCreatingUI$scheduleButtonTextUpdate();
        }
    }

    @Unique
    private void modernWorldCreatingUI$scheduleButtonTextUpdate() {
        // 创建一个简单的 Runnable 来更新按钮文本
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                modernWorldCreatingUI$updateButtonText();
            }
        };

        // 使用 Minecraft 的任务调度系统
        Minecraft.getMinecraft().func_152344_a(updateTask);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // 根据当前 Tab 处理不同的输入框
        if (modernWorldCreatingUI$currentTab == 100) {
            field_146333_g.textboxKeyTyped(typedChar, keyCode);
            this.field_146330_J = this.field_146333_g.getText();
        } else {
            field_146335_h.textboxKeyTyped(typedChar, keyCode);
            this.field_146329_I = this.field_146335_h.getText();
        }

        if (keyCode == 1) {
            this.mc.displayGuiScreen(field_146332_f);
        }

        // 更新创建按钮状态
        ((GuiButton)this.buttonList.get(2)).enabled = this.field_146333_g.getText().length() > 0;

        // 处理世界名称
        this.func_146314_g();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 根据当前 Tab 处理不同的输入框点击
        if (modernWorldCreatingUI$currentTab == 100) {
            field_146333_g.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            field_146335_h.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Unique
    private GuiButton getButtonById(int id) {
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.id == id) {
                    return button;
                }
            }
        }
        return null;
    }

    // 保留原版的世界名称处理方法
    @Shadow
    private void func_146314_g() {}

    @Shadow
    private void func_146319_h() {}
}
