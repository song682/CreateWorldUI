package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.TabState;
import decok.dfcdvadstf.createworldui.gamerule.GameRuleEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class ModernCreateWorld extends GuiScreen {

    /**
     * Manage all the button and string.
     */
    @Shadow
    private GuiScreen field_146332_f; // parentScreen
    @Shadow
    private boolean field_146337_w; // hardcore
    @Shadow
    private String field_146330_J; // World displayed name
    @Shadow
    private String field_146342_r; // Gamemode description Text
    @Shadow
    private String field_146329_I; // Seed
    @Shadow
    private boolean field_146341_s; // Generate Structures
    @Shadow
    private boolean field_146338_v; // Bonus Chest
    @Shadow
    private boolean field_146340_t; // Allow Cheats
    @Shadow
    private GuiTextField field_146335_h; // Seed text box
    @Shadow
    private GuiTextField field_146333_g; // World Name Input Box
    @Shadow
    private GuiButton field_146321_E; // Allow Cheats
    @Shadow
    private GuiButton field_146343_z; // GameMode
    @Shadow
    private GuiButton field_146326_C; // Bonus Chest
    @Shadow
    private GuiButton field_146320_D; // World Type selections
    @Shadow
    private GuiButton field_146325_B; // Generate Structure
    @Shadow
    private GuiButton field_146322_F; // 自定义（预设）
    @Shadow
    private int field_146331_K; // Index of world type

    @Unique
    private int modernWorldCreatingUI$currentTab = 100; // 100: Game, 101: World, 102: More
    @Unique
    private final List<GuiButton> modernWorldCreatingUI$tabButtons = new ArrayList<>();
    @Unique
    private boolean modernWorldCreatingUI$isReorganizing = false;
    @Unique
    private static final ResourceLocation OPTIONS_BG_DARK = new ResourceLocation("createworldui:textures/gui/options_background_dark.png");
    @Unique
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("createworldui:textures/gui/tabs.png");
    @Unique
    private static final int TAB_WIDTH = 130;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private final Map<Integer, String> modernWorldCreatingUI$hoverTexts = new HashMap<>();
    @Unique
    private GuiButton modernWorldCreatingUI$difficultyButton;
    @Unique
    private EnumDifficulty modernWorldCreatingUI$difficulty = EnumDifficulty.NORMAL;


    /**
     * <p>
     *   在头部保存一些关键状态或执行预处理，
     *   这里可以保存原版的某些状态，或者准备自定义初始化，
     *   这么做的原因就是防止点生成世界时，有NPE出现<br>
     *   1. 确保关键字段不为null<br>
     *   2. 设置初始化标志
     * </p>
     */
    @Inject(method = "initGui", at = @At("HEAD"))
    private void onInitGuiHead(CallbackInfo ci) {
        modernWorldCreatingUI$ensureFieldsNotNull();
        modernWorldCreatingUI$isReorganizing = true;
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGuiTail(CallbackInfo ci) {
        // 在原版初始化完成后，重新组织界面为 Tab 布局

        // 保存必要的按钮
        List<GuiButton> essentialButtons = modernWorldCreatingUI$collectEssentialButtons();

        // 清空并重新构建界面
        this.buttonList.clear();
        this.modernWorldCreatingUI$tabButtons.clear();
        this.modernWorldCreatingUI$hoverTexts.clear();
        this.buttonList.addAll(essentialButtons);

        // 创建Tab界面
        modernWorldCreatingUI$createTabButtons();
        modernWorldCreatingUI$recreateFunctionalButtons();
        modernWorldCreatingUI$setupTextFields();
        modernWorldCreatingUI$updateButtonVisibilityNAbility();
        modernWorldCreatingUI$repositionActionButtons();

        // 初始化悬停文本
        modernWorldCreatingUI$initHoverTexts();

        modernWorldCreatingUI$isReorganizing = false;
    }

    @Unique
    private void modernWorldCreatingUI$initHoverTexts() {
        // 只保留功能按钮的悬停文本
        modernWorldCreatingUI$hoverTexts.put(4, I18n.format("createworldui.hover.generateStructures"));
        modernWorldCreatingUI$hoverTexts.put(5, I18n.format("createworldui.hover.worldType"));
        modernWorldCreatingUI$hoverTexts.put(6, I18n.format("createworldui.hover.allowCheats"));
        modernWorldCreatingUI$hoverTexts.put(7, I18n.format("createworldui.hover.bonusChest"));
        modernWorldCreatingUI$hoverTexts.put(8, I18n.format("createworldui.hover.customize"));
        modernWorldCreatingUI$hoverTexts.put(200, I18n.format("createworldui.hover.gameRuleEditor"));
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
        this.buttonList.add(this.field_146343_z = new GuiButton(2, this.width / 2 - 104, this.height / 2, 208, 20, ""));
        this.buttonList.add(this.field_146325_B = new GuiButton(4, this.width / 2 + 154 - 44, this.height / 2 + 15, 44, 20, ""));
        this.buttonList.add(this.field_146326_C = new GuiButton(7, this.width / 2 + 154 - 44, this.height / 2 - 15 , 44, 20, ""));
        this.buttonList.add(this.field_146320_D = new GuiButton(5, this.width / 2 - 154, this.height / 8 + 10, 150, 20, ""));
        this.buttonList.add(this.field_146321_E = new GuiButton(6, this.width / 2 - 104, this.height / 2 + 50, 208, 20, ""));
        this.buttonList.add(this.field_146322_F = new GuiButton(8, this.width / 2 + 4 , this.height / 8 + 10, 150, 20, I18n.format("selectWorld.customizeType")));
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 2, 200, 20, I18n.format("createworldui.button.gameRuleEditor")));
        this.buttonList.add(this.modernWorldCreatingUI$difficultyButton =
                new GuiButton(9, this.width / 2 - 104, this.height / 2 + 25, 208, 20, modernWorldCreatingUI$getDifficultyText()));

        // 更新按钮文本
        modernWorldCreatingUI$updateButtonText();
    }

    @Unique
    private void modernWorldCreatingUI$setupTextFields() {
        // 确保输入框使用正确的位置
        if (field_146333_g != null) {
            field_146333_g.xPosition = this.width / 2 - 104;
            field_146333_g.yPosition = this.height / 5;
            field_146333_g.width = 208;
        } else {
            field_146333_g = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, TAB_HEIGHT + 10, this.width / 2, 20);
            field_146333_g.setText(this.field_146330_J);
        }

        if (field_146335_h != null) {
            field_146335_h.xPosition = this.width / 2 - 154;
            field_146335_h.yPosition = this.height / 3 - 1;
            field_146335_h.width = 308;
        } else {
            field_146335_h = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, TAB_HEIGHT + 20, this.width / 2, 20);
            field_146335_h.setText(this.field_146329_I);
        }

        field_146333_g.setFocused(true);
    }

    /**
     * <p>
     *     对“创建世界”和"取消"做特殊处理。
     * </p>
     * <p>
     *     Do some treats towards "Create World" and "Cancel"
     * </p>
     */
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

    /**
     * <p>
     *     创建三个标签页。<br>
     *     {@code 100} -> 游戏<br>
     *      {@code 101} -> 世界<br>
     *      {@code 102} -> 更多
     * </p>
     * <p>
     *     Create 3 Tabs.<br>
     *      {@code 100} -> Game<br>
     *      {@code 101} -> World<br>
     *      {@code 102} -> More
     * </p>
     */
    @Unique
    private void modernWorldCreatingUI$createTabButtons() {
        // 清空现有的 Tab 按钮
        this.modernWorldCreatingUI$tabButtons.clear();

        // 计算总宽度：3个tab + 2个间隔
        int totalWidth = TAB_WIDTH * 3 + 2;
        // 计算起始位置，让整个tab组居中
        int startX = this.width / 2 - totalWidth / 2;
        String[] tabNames = {
                I18n.format("createworldui.tab.game"),
                I18n.format("createworldui.tab.world"),
                I18n.format("createworldui.tab.more")
        };

        for (int i = 0; i < 3; i++) {
            int xPos = startX + i * (TAB_WIDTH + 1);
            GuiButton tabButton = new GuiButton(100 + i, xPos, 0, TAB_WIDTH, TAB_HEIGHT, tabNames[i]) {
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
        }
    }

    /**
     * <p>
     *     管理并更新按钮上的文字。
     * </p>
     * <p>
     *     Manage and update the text on the button.
     * </p>
     */
    @Unique
    private void modernWorldCreatingUI$updateButtonText() {
       // 确保字段不为空
        if (this.field_146342_r == null) {
            this.field_146342_r = "survival";
        }
        if (WorldType.worldTypes == null || this.field_146331_K >= WorldType.worldTypes.length || WorldType.worldTypes[this.field_146331_K] == null) {
            this.field_146331_K = 0; // 重置为默认世界类型
        }
        // 更新游戏模式难度文本
        if (this.modernWorldCreatingUI$difficultyButton != null) {
            this.modernWorldCreatingUI$difficultyButton.displayString = modernWorldCreatingUI$getDifficultyText();
        }


        // 更新游戏模式按钮文本
        this.field_146343_z.displayString = I18n.format("selectWorld.gameMode") + " " +
                I18n.format("selectWorld.gameMode." + this.field_146342_r);

        // 更新生成建筑按钮文本
        this.field_146325_B.displayString = this.field_146341_s ? I18n.format("options.on") : I18n.format("options.off");

        // 更新奖励箱按钮文本
        this.field_146326_C.displayString = this.field_146338_v && !this.field_146337_w ? I18n.format("options.on") : I18n.format("options.off");

        // 更新世界类型按钮文本
        this.field_146320_D.displayString = I18n.format("selectWorld.mapType") + " " +
                I18n.format(WorldType.worldTypes[this.field_146331_K].getTranslateName());

        // 更新允许作弊按钮文本
        this.field_146321_E.displayString = I18n.format("selectWorld.allowCommands") + " " +
                (this.field_146340_t && !this.field_146337_w ? I18n.format("options.on") : I18n.format("options.off"));
    }

    @Unique
    private String modernWorldCreatingUI$getDifficultyText() {
        return I18n.format("options.difficulty") + ": " +
                I18n.format(modernWorldCreatingUI$difficulty.getDifficultyResourceKey());
    }

    /**
     * <p>
     *    根据当前 Tab 显示/隐藏相应的按钮（标签页由ID判断。）
     * </p>
     * <p>
     *     Show or hide the correlate buttons based on the current tabs. (Tab's judging by ID)
     * </p>
     */
    @Unique
    private void modernWorldCreatingUI$updateButtonVisibilityNAbility() {
        switch (modernWorldCreatingUI$currentTab) {
            case 100:
                this.field_146343_z.visible = true;
                this.field_146321_E.visible = true;
                this.modernWorldCreatingUI$difficultyButton.visible = true;
                this.field_146325_B.visible = false;
                this.field_146326_C.visible = false;
                this.field_146320_D.visible = false;
                this.field_146322_F.visible = false;
                modernWorldCreatingUI$getButtonById(200).visible = false;
                break;
            case 101: // 世界 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.modernWorldCreatingUI$difficultyButton.visible = false;
                this.field_146325_B.visible = true;
                this.field_146326_C.visible = true;
                this.field_146320_D.visible = true;
                this.field_146322_F.enabled = WorldType.worldTypes[this.field_146331_K].isCustomizable();
                this.field_146322_F.visible = true;
                modernWorldCreatingUI$getButtonById(200).visible = false;
                break;
            case 102: // 更多 Tab
                this.field_146343_z.visible = false;
                this.field_146321_E.visible = false;
                this.modernWorldCreatingUI$difficultyButton.visible = false;
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
     * @author dfdvdsf
     * @reason Enhance the vanilla
     */
    @Inject(method = {"drawScreen"}, at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ci.cancel();
        // 绘制主背景
        this.drawBackground(0);

        // 在顶部绘制暗黑色背景，从(0,0)到(width, tabs底部)
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);

        // 计算Tab背景区域：从左上角(0,0)开始，宽度为整个窗口宽度，高度到Tabs底部
        // Tabs的y位置 + Tabs高度
        // 绘制暗黑色背景区域
        this.modernWorldCreatingUI$drawTiledTexture(0, 0, this.width, TAB_HEIGHT - 2, 16, 16);

        // 绘制两条横线
        modernWorldCreatingUI$drawColoredLine(0, TAB_HEIGHT - 3, this.width, 0x00FFFFFF, 0x40FFFFFF); // 上透明下白，25%透明度
        modernWorldCreatingUI$drawColoredLine(0, this.height - 35, this.width, 0x40000000, 0x40FFFFFF); // 上白下黑，25%透明度

        // 根据当前 Tab 显示不同的输入框
        if (modernWorldCreatingUI$currentTab == 100) {
            // 游戏 Tab 显示世界名称
            this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), this.width / 2 - 104, this.height / 5 - 13, 0xA0A0A0);
            field_146333_g.drawTextBox();
        }

        if (modernWorldCreatingUI$currentTab == 101) {
            // 世界 Tab 显示种子
            this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterSeed"), this.width / 2 - 154, this.height / 3 - 2 - 13, 0xA0A0A0);
            field_146335_h.drawTextBox();

            // 添加提示文字：如果种子输入框为空且没有焦点，则绘制提示文字
            if (field_146335_h.getText().isEmpty() && !field_146335_h.isFocused()) {
                String placeholder = I18n.format("selectWorld.seedInfo");
                int x = field_146335_h.xPosition + 4;
                int y = field_146335_h.yPosition + (field_146335_h.height - 8) / 2;
                this.fontRendererObj.drawStringWithShadow(placeholder, x, y, 0x808080);
            }

            // 修改：在按钮左侧绘制标签文本
            // 生成建筑标签
            this.drawString(this.fontRendererObj, I18n.format("createworldui.selectWorld.mapFeatures"), this.width / 2 - 154, this.height / 2 + 15 + 6, 0xFFFFFF);
            // 奖励箱标签
            this.drawString(this.fontRendererObj, I18n.format("createworldui.selectWorld.bonusItems"), this.width / 2 - 154, this.height / 2 - 15 + 6, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制悬停提示
        modernWorldCreatingUI$drawHoverText(mouseX, mouseY);
    }

    @Unique
    private void modernWorldCreatingUI$drawHoverText(int mouseX, int mouseY) {
        // 只检查功能按钮的悬停
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.visible && mouseX >= button.xPosition && mouseY >= button.yPosition &&
                        mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height) {

                    // 跳过标签页按钮、创建和取消按钮
                    if (button.id >= 100 && button.id <= 102) continue;
                    if (button.id == 0 || button.id == 1) continue;

                    // 特殊处理游戏模式按钮
                    if (button.id == 2) {
                        String hoverText = modernWorldCreatingUI$getGameModeHoverText();
                        if (hoverText != null && !hoverText.isEmpty()) {
                            this.drawHoveringText(Arrays.asList(hoverText), mouseX, mouseY, this.fontRendererObj);
                            return;
                        }
                    }

                    // 其他按钮从Map中获取悬停文本
                    String hoverText = modernWorldCreatingUI$hoverTexts.get(button.id);
                    if (hoverText != null && !hoverText.isEmpty()) {
                        this.drawHoveringText(Arrays.asList(hoverText), mouseX, mouseY, this.fontRendererObj);
                        return;
                    }
                }
            }
        }

        // 检查文本框悬停
        if (modernWorldCreatingUI$currentTab == 100 && field_146333_g != null) {
            if (mouseX >= field_146333_g.xPosition && mouseY >= field_146333_g.yPosition &&
                    mouseX < field_146333_g.xPosition + field_146333_g.width && mouseY < field_146333_g.yPosition + field_146333_g.height) {

                String worldName = field_146333_g.getText();
                String hoverText;
                if (worldName.isEmpty()) {
                    hoverText = I18n.format("createworldui.hover.worldName.empty");
                } else {
                    hoverText = I18n.format("createworldui.hover.worldName.filled", worldName);
                }

                this.drawHoveringText(Arrays.asList(hoverText), mouseX, mouseY, this.fontRendererObj);
                return;
            }
        }
    }

    @Unique
    private String modernWorldCreatingUI$getGameModeHoverText() {
        if (this.field_146342_r == null) {
            return I18n.format("createworldui.hover.gameMode");
        }

        switch (this.field_146342_r) {
            case "survival":
                return I18n.format("createworldui.hover.gameMode.survival");
            case "creative":
                return I18n.format("createworldui.hover.gameMode.creative");
            case "hardcore":
                return I18n.format("createworldui.hover.gameMode.hardcore");
            case "adventure":
                return I18n.format("createworldui.hover.gameMode.adventure");
            default:
                return I18n.format("createworldui.hover.gameMode");
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        // 处理 Tab 切换
        if (button.id >= 100 && button.id <= 102) {
            modernWorldCreatingUI$currentTab = button.id;
            modernWorldCreatingUI$updateButtonVisibilityNAbility();
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

        // 处理生成建筑按钮
        if (button.id == 4) {
            this.field_146341_s = !this.field_146341_s;
            modernWorldCreatingUI$updateButtonText();
            ci.cancel();
            return;
        }

        // 处理允许作弊按钮
        if (button.id == 6) {
            // 硬核模式下不允许作弊
            if (this.field_146337_w) {
                this.field_146340_t = false;
            } else {
                this.field_146340_t = !this.field_146340_t;
            }
            modernWorldCreatingUI$updateButtonText();
            ci.cancel();
            return;
        }

        // 处理奖励箱按钮
        if (button.id == 7) {
            // 硬核模式下不允许奖励箱
            if (this.field_146337_w) {
                this.field_146338_v = false;
            } else {
                this.field_146338_v = !this.field_146338_v;
            }
            modernWorldCreatingUI$updateButtonText();
            ci.cancel();
            return;
        }

        // 处理难度按钮
        if (button.id == 110) {
            int next = (this.modernWorldCreatingUI$difficulty.getDifficultyId() + 1) % EnumDifficulty.values().length;
            this.modernWorldCreatingUI$difficulty = EnumDifficulty.getDifficultyEnum(next);
            if (this.modernWorldCreatingUI$difficultyButton != null) {
                this.modernWorldCreatingUI$difficultyButton.displayString = modernWorldCreatingUI$getDifficultyText();
            }
            ci.cancel();
            return;
        }

        // 对于其他原版按钮，更新文本显示
        if (button.id == 2 || button.id == 4 || button.id == 5 || button.id == 6 || button.id == 7 || button.id == 110) {
            modernWorldCreatingUI$scheduleButtonTextUpdate();
        }
    }

    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;launchIntegratedServer(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void modernWorldCreatingUI$afterLaunchWorld(GuiButton button, CallbackInfo ci) {
        try {
            Object integrated = this.mc.getIntegratedServer();
            if (integrated == null) {
                // fallback: theIntegratedServer field (in case getIntegratedServer is not yet assigned)
                try {
                    java.lang.reflect.Field fserv = Minecraft.class.getDeclaredField("theIntegratedServer");
                    fserv.setAccessible(true);
                    integrated = fserv.get(this.mc);
                } catch (Throwable ignored) {}
            }

            if (integrated != null) {
                Class<?> serverClass = integrated.getClass();
                try {
                    java.lang.reflect.Field worldsField = serverClass.getDeclaredField("worldServers");
                    worldsField.setAccessible(true);
                    Object[] worlds = (Object[]) worldsField.get(integrated);
                    if (worlds != null && worlds.length > 0 && worlds[0] != null) {
                        Object overworld = worlds[0];
                        try {
                            java.lang.reflect.Field diffField = overworld.getClass().getDeclaredField("difficultySetting");
                            diffField.setAccessible(true);
                            diffField.set(overworld, this.modernWorldCreatingUI$difficulty);
                            this.mc.gameSettings.difficulty = this.modernWorldCreatingUI$difficulty;
                            this.mc.gameSettings.saveOptions();
                        } catch (NoSuchFieldException nsf) {
                            try {
                                java.lang.reflect.Field diffField = overworld.getClass().getDeclaredField("field_73013_u");
                                diffField.setAccessible(true);
                                diffField.set(overworld, this.modernWorldCreatingUI$difficulty);
                                this.mc.gameSettings.difficulty = this.modernWorldCreatingUI$difficulty;
                                this.mc.gameSettings.saveOptions();
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable t) {
                    try {
                        java.lang.reflect.Field tw = Minecraft.class.getDeclaredField("theWorld");
                        tw.setAccessible(true);
                        Object clientWorld = tw.get(this.mc);
                        if (clientWorld != null) {
                            try {
                                java.lang.reflect.Field diffField = clientWorld.getClass().getDeclaredField("difficultySetting");
                                diffField.setAccessible(true);
                                diffField.set(clientWorld, this.modernWorldCreatingUI$difficulty);
                            } catch (NoSuchFieldException nsf) {
                                try {
                                    java.lang.reflect.Field diffField = clientWorld.getClass().getDeclaredField("field_73013_u");
                                    diffField.setAccessible(true);
                                    diffField.set(clientWorld, this.modernWorldCreatingUI$difficulty);
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    @Unique
    private void modernWorldCreatingUI$handleWorldTypeSelection() {
        // 跳过空的世界类型
        do {
            this.field_146331_K = (this.field_146331_K + 1) % WorldType.worldTypes.length;
        } while (WorldType.worldTypes[this.field_146331_K] == null);

        // 更新按钮文本和可见性
        modernWorldCreatingUI$updateButtonText();
        modernWorldCreatingUI$updateButtonVisibilityNAbility();
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
     * @author dfdvdsf
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

    /**
     * 绘制平铺纹理
     * 使用原版的drawTexturedModalRect方法将纹理平铺到指定区域
     *
     * @param x 绘制区域的起始X坐标
     * @param y 绘制区域的起始Y坐标
     * @param width 绘制区域的总宽度
     * @param height 绘制区域的总高度
     * @param textureWidth 单个纹理块的宽度
     * @param textureHeight 单个纹理块的高度
     */
    @Unique
    private void modernWorldCreatingUI$drawTiledTexture(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        for (int tileX = 0; tileX < width; tileX += textureWidth) {
            for (int tileY = 0; tileY < height; tileY += textureHeight) {
                int tileW = Math.min(textureWidth, width - tileX);
                int tileH = Math.min(textureHeight, height - tileY);

                double u1 = 0.0;
                double u2 = (double)tileW / (double)textureWidth;
                double v1 = 0.0;
                double v2 = (double)tileH / (double)textureHeight;

                tessellator.addVertexWithUV(x + tileX, y + tileY + tileH, 0.0D, u1, v2);
                tessellator.addVertexWithUV(x + tileX + tileW, y + tileY + tileH, 0.0D, u2, v2);
                tessellator.addVertexWithUV(x + tileX + tileW, y + tileY, 0.0D, u2, v1);
                tessellator.addVertexWithUV(x + tileX, y + tileY, 0.0D, u1, v1);
            }
        }

        tessellator.draw();
    }

    /**
     * 绘制彩色线条
     * 绘制一个2像素高的线条，上半像素为指定颜色1，下半像素为指定颜色2
     *
     * @param x 线条起始X坐标
     * @param y 线条起始Y坐标
     * @param width 线条宽度
     * @param topColor 上半像素颜色（ARGB格式）
     * @param bottomColor 下半像素颜色（ARGB格式）
     */
    @Unique
    private void modernWorldCreatingUI$drawColoredLine(int x, int y, int width, int topColor, int bottomColor){
        // 绘制上半像素（黑色，50%透明度）
        drawRect(x, y, x + width, y + 1, topColor);

        // 绘制下半像素（白色，50%透明度）
        drawRect(x, y + 1, x + width, y + 2, bottomColor);
    }

    // 保留原版的世界名称处理方法
    @Shadow
    private void func_146314_g() {}

    @Shadow
    private void func_146319_h() {}
}
