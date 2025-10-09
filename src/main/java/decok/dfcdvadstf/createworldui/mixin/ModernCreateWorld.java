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
    private GuiButton field_146325_B; // 生成建筑按钮
    @Shadow
    private GuiButton field_146322_F; // 自定义（预设）
    @Shadow
    private int field_146331_K; // 世界类型的索引

    @Unique
    private int modernWorldCreatingUI$currentTab = 100; // 100: Game, 101: World, 102: More
    @Unique
    private final List<GuiButton> modernWorldCreatingUI$tabButtons = new ArrayList<>();
    @Unique
    private boolean modernWorldCreatingUI$isReorganizing = false;
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

    @Inject(method = "initGui", at = @At("HEAD"))
    private void onInitGuiHead(CallbackInfo ci) {
        // 在头部保存一些关键状态或执行预处理
        // 这里可以保存原版的某些状态，或者准备自定义初始化
        modernWorldCreatingUI$preInit();
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGuiTail(CallbackInfo ci) {
        // 在原版初始化完成后，重新组织界面为 Tab 布局
        modernWorldCreatingUI$reorganizeToTabLayout();
    }

    @Unique
    private void modernWorldCreatingUI$preInit() {

        // 2. 确保关键字段不为null
        modernWorldCreatingUI$ensureFieldsNotNull();

        // 3. 设置初始化标志
        modernWorldCreatingUI$isReorganizing = true;
    }

    @Unique
    private void modernWorldCreatingUI$ensureFieldsNotNull() {
        // 确保关键字段不为null，防止后续出现NPE
        if (this.field_146330_J == null) {
            this.field_146330_J = "New World";
        }
        if (this.field_146329_I == null) {
            this.field_146329_I = "";
        }
        if (this.field_146342_r == null) {
            this.field_146342_r = "survival";
        }
        if (WorldType.worldTypes == null || this.field_146331_K >= WorldType.worldTypes.length ||
                WorldType.worldTypes[this.field_146331_K] == null) {
            this.field_146331_K = 0;
        }
    }

    @Unique
    private void modernWorldCreatingUI$reorganizeToTabLayout() {
        // 保存必要的按钮
        List<GuiButton> essentialButtons = modernWorldCreatingUI$collectEssentialButtons();

        // 清空并重新构建界面
        this.buttonList.clear();
        this.modernWorldCreatingUI$tabButtons.clear();
        this.buttonList.addAll(essentialButtons);

        // 创建Tab界面
        modernWorldCreatingUI$createTabButtons();
        modernWorldCreatingUI$recreateFunctionalButtons();
        modernWorldCreatingUI$setupTextFields();
        modernWorldCreatingUI$updateButtonVisibility();
        modernWorldCreatingUI$repositionActionButtons();

        modernWorldCreatingUI$isReorganizing = false;
    }

    @Unique
    private List<GuiButton> modernWorldCreatingUI$collectEssentialButtons() {
        List<GuiButton> essentialButtons = new ArrayList<>();
        for (GuiButton button : (List<GuiButton>) this.buttonList) {
            if (button.id == 0 || button.id == 1) { // 创建和取消按钮
                essentialButtons.add(button);
            }
        }
        return essentialButtons;
    }

    @Unique
    private void modernWorldCreatingUI$recreateFunctionalButtons() {
        // 重新创建所有功能按钮，使用新的位置
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
    private void modernWorldCreatingUI$setupTextFields() {
        // 确保输入框使用正确的位置
        if (field_146333_g != null) {
            field_146333_g.xPosition = this.width / 2 - 100;
            field_146333_g.yPosition = 40;
        } else {
            field_146333_g = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 40, 200, 20);
            field_146333_g.setText(this.field_146330_J);
        }

        if (field_146335_h != null) {
            field_146335_h.xPosition = this.width / 2 - 100;
            field_146335_h.yPosition = 100;
        } else {
            field_146335_h = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 100, 200, 20);
            field_146335_h.setText(this.field_146329_I);
        }

        field_146333_g.setFocused(true);
    }

    @Unique
    private void modernWorldCreatingUI$repositionActionButtons() {
        // 确保创建和取消按钮在底部正确位置
        GuiButton createButton = modernWorldCreatingUI$getButtonById(0);
        GuiButton cancelButton = modernWorldCreatingUI$getButtonById(1);

        if (createButton != null) {
            createButton.xPosition = this.width / 2 - 155;
            createButton.yPosition = this.height - 28;
            createButton.width = 150;
            createButton.height = 20;
        }

        if (cancelButton != null) {
            cancelButton.xPosition = this.width / 2 + 5;
            cancelButton.yPosition = this.height - 28;
            cancelButton.width = 150;
            cancelButton.height = 20;
        }
    }

    @Unique
    private void modernWorldCreatingUI$createTabButtons() {
        // 清空现有的 Tab 按钮
        this.modernWorldCreatingUI$tabButtons.clear();

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
    private void modernWorldCreatingUI$updateButtonText() {
        // 确保字段不为空
        if (this.field_146342_r == null) {
            this.field_146342_r = "survival";
        }
        if (WorldType.worldTypes == null || this.field_146331_K >= WorldType.worldTypes.length || WorldType.worldTypes[this.field_146331_K] == null) {
            this.field_146331_K = 0; // 重置为默认世界类型
        }

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

    /**
     * <p>
     *    根据当前 Tab 显示/隐藏相应的按钮（标签页由ID判断。）<br>
     *    100 -> 游戏<br>
     *    101 -> 世界<br>
     *    102 -> 更多
     * </p>
     * <p>
     *     Show or hide the correlate buttons based on the current tabs. (Tabs judging by ID)<br>
     *     100 -> Game<br>
     *     101 -> World<br>
     *     102 -> More
     * </p>
     */
    @Unique
    private void modernWorldCreatingUI$updateButtonVisibility() {
        switch (modernWorldCreatingUI$currentTab) {
            case 100:
                this.field_146343_z.visible = true;
                this.field_146321_E.visible = true;
                this.field_146325_B.visible = false;
                this.field_146326_C.visible = false;
                this.field_146320_D.visible = false;
                this.field_146322_F.visible = false;
                modernWorldCreatingUI$getButtonById(200).visible = false;
                break;
            case 101: // 世界 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.field_146325_B.visible = true;
                this.field_146326_C.visible = true;
                this.field_146320_D.visible = true;
                this.field_146322_F.visible = WorldType.worldTypes[this.field_146331_K].isCustomizable();
                modernWorldCreatingUI$getButtonById(200).visible = false;
                break;
            case 102: // 更多 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.field_146325_B.visible = false;
                this.field_146326_C.visible = false;
                this.field_146320_D.visible = false;
                this.field_146322_F.visible = false;
                modernWorldCreatingUI$getButtonById(200).visible = true;
                break;
        }

        // 更新按钮文本
        modernWorldCreatingUI$updateButtonText();
    }

    /**
     * @param mouseX As vanilla done.
     * @param mouseY Same to mouseX
     * @param partialTicks Same to mouseX
     * @author
     * @reason
     */
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
            this.mc.displayGuiScreen(new GameRuleEditor(null));
            ci.cancel();
            return;
        }

        // 处理世界类型选择按钮
        if (button.id == 5) {
            modernWorldCreatingUI$handleWorldTypeSelection();
            ci.cancel();
            return;
        }

        // 对于其他原版按钮，更新文本显示
        if (button.id == 2 || button.id == 4 || button.id == 5 || button.id == 6 || button.id == 7) {
            modernWorldCreatingUI$scheduleButtonTextUpdate();
        }
    }

    @Unique
    private void modernWorldCreatingUI$handleWorldTypeSelection() {
        // 循环选择世界类型

        // 跳过空的世界类型
        do {
            this.field_146331_K = (this.field_146331_K + 1) % WorldType.worldTypes.length;
        } while (WorldType.worldTypes[this.field_146331_K] == null);

        // 更新按钮文本和可见性
        modernWorldCreatingUI$updateButtonText();
        modernWorldCreatingUI$updateButtonVisibility();
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

    /**
     * @author
     * @reason
     * @param typedChar
     * @param keyCode
     */
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

    /**
     * @author
     * @reason
     * @param mouseX
     * @param mouseY
     * @param mouseButton
     */
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
    private GuiButton modernWorldCreatingUI$getButtonById(int id) {
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
