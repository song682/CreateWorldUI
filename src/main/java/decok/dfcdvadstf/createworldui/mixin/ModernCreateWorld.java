package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.TabState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.EnumDifficulty;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static decok.dfcdvadstf.createworldui.api.util.TextureHelper.drawModalRectWithCustomSizedTexture;

@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class ModernCreateWorld extends GuiScreen {

    @Unique
    private static Logger modernWorldCreatingUI$logger;
    @Shadow
    private GuiScreen field_146332_f; //parentScreen
    @Shadow
    private String field_146329_I; // 种子
    @Shadow
    private boolean field_146341_s; // 生成建筑
    @Shadow
    private boolean field_146338_v; // 奖励箱
    @Shadow
    private WorldSettings.GameType selectedGameType;
    @Shadow
    private WorldType WorldType;
    @Shadow
    public String field_146334_a; // 世界名称
    @Shadow
    private GuiTextField field_146335_h; // 种子输入框
    @Shadow
    private GuiTextField field_146333_g; // 世界名称输入框

    @Unique
    private int modernWorldCreatingUI$difficulty;
    @Unique
    private int modernWorldCreatingUI$currentTab = 100; // 100: Game, 101: World, 102: More
    @Unique
    private final List<GuiButton> modernWorldCreatingUI$tabButtons = new ArrayList<>();
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
    @Unique
    private EnumDifficulty modernWorldCreatingUI$selectedDifficulty = EnumDifficulty.NORMAL; // 使用EnumDifficulty替代int

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void initModernGui(CallbackInfo ci) {
        ci.cancel(); // 取消原版初始化
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.modernWorldCreatingUI$tabButtons.clear();

        // 初始化selectedDifficulty
        if (this.modernWorldCreatingUI$selectedDifficulty == null) {
            this.modernWorldCreatingUI$selectedDifficulty = EnumDifficulty.getDifficultyEnum(this.modernWorldCreatingUI$difficulty);
        }

        // 创建标签页按钮
        modernWorldCreatingUI$createTabButtons();

        // 初始化世界名称输入框
        field_146333_g = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 40, 200, 20);
        field_146333_g.setFocused(true);
        field_146333_g.setText(field_146334_a == null ? I18n.format("selectWorld.newWorld") : field_146334_a);

        // 初始化种子输入框
        field_146335_h = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 100, 200, 20);
        field_146335_h.setText(field_146329_I == null ? "" : field_146329_I);

        // 添加底部按钮
        modernWorldCreatingUI$addButton(new GuiButton(0, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("createworldui.button.create")));
        modernWorldCreatingUI$addButton(new GuiButton(1, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("createworldui.button.cancel")));

        // 添加当前标签页的控件
        modernWorldCreatingUI$addTabControls();
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
                        boolean isSelected = modernWorldCreatingUI$currentTab == (this.id - 100);

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
            modernWorldCreatingUI$addButton(tabButton);
            xPos += TAB_WIDTH + 5;
        }
    }

    @Unique
    private void modernWorldCreatingUI$addButton(GuiButton button) {
        this.buttonList.add(button); // 调用父类的按钮添加方法
    }

    @Unique
    private void modernWorldCreatingUI$addTabControls() {
        // 清除现有控件（保留标签和底部按钮）
        this.buttonList.removeIf(button -> button.id < 100 || button.id >= 200);

        int yPos = 140;
        switch (modernWorldCreatingUI$currentTab) {
            case 100: // 游戏标签页
                modernWorldCreatingUI$addGameTabControls(yPos);
                break;
            case 101: // 世界标签页
                modernWorldCreatingUI$addWorldTabControls(yPos);
                break;
            case 102: // 更多标签页
                modernWorldCreatingUI$addMoreTabControls(yPos);
                break;
        }
    }

    @Unique
    private void modernWorldCreatingUI$addGameTabControls(int yPos) {
        // 游戏模式按钮
        modernWorldCreatingUI$addButton(new GuiButton(200, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.gameMode") + ": " + selectedGameType.getName()));
        yPos += 25;

        // 难度按钮 - 使用EnumDifficulty
        modernWorldCreatingUI$addButton(new GuiButton(201, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("options.difficulty") + ": " +
                        I18n.format(modernWorldCreatingUI$selectedDifficulty.getDifficultyResourceKey())));
        yPos += 25;

        // 作弊按钮
        modernWorldCreatingUI$addButton(new GuiButton(202, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.allowCommands") + ": " + (modernWorldCreatingUI$allowCommands ? I18n.format("gui.yes") : I18n.format("gui.no"))));
    }

    @Unique
    private void modernWorldCreatingUI$addWorldTabControls(int yPos) {
        // 世界类型按钮
        modernWorldCreatingUI$addButton(new GuiButton(203, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.worldType") + WorldType.getWorldTypeName()));
        yPos += 25;

        // 生成结构按钮
        modernWorldCreatingUI$addButton(new GuiButton(204, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.mapFeatures")  + (field_146341_s ? I18n.format("gui.yes") : I18n.format("gui.no"))));
        yPos += 25;

        // 奖励箱按钮
        modernWorldCreatingUI$addButton(new GuiButton(205, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.bonusChest") + ": " + (field_146338_v ? I18n.format("gui.yes") : I18n.format("gui.no"))));
    }

    @Unique
    private void modernWorldCreatingUI$addMoreTabControls(int yPos) {
        // 生成动物按钮
        modernWorldCreatingUI$addButton(new GuiButton(206, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("createworldui.gamerules.spawnAnimals") + ": " + (modernWorldCreatingUI$spawnAnimals ? I18n.format("gui.yes") : I18n.format("gui.no"))));
        yPos += 25;

        // 生成怪物按钮
        modernWorldCreatingUI$addButton(new GuiButton(207, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("createworldui.gamerules.spawnMonsters") + ": " + (modernWorldCreatingUI$spawnMonsters ? I18n.format("gui.yes") : I18n.format("gui.no"))));
        yPos += 25;

        // 天气循环按钮
        modernWorldCreatingUI$addButton(new GuiButton(208, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("createworldui.gamerules.weatherCycle") + ": " + (modernWorldCreatingUI$weatherCycle ? I18n.format("gui.yes") : I18n.format("gui.no"))));
    }


    /**
     * @author dfdvdsf
     * @reason Hence I wanna to custom it fully. I want to use my textures.
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制背景
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_LIGHT);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, this.width, this.height, 16, 16);

        // 绘制内容区域
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);
        drawModalRectWithCustomSizedTexture(10, 30, 0, 0, this.width - 20, this.height - 40, 16, 16);

        // 绘制标题
        this.drawCenteredString(this.fontRendererObj, I18n.format("selectWorld.create"), this.width / 2, 15, 0xFFFFFF);

        // 绘制世界名称标签和输入框
        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), this.width / 2 - 100, 27, 0xA0A0A0);
        field_146333_g.drawTextBox();

        // 绘制种子标签和输入框
        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterSeed"), this.width / 2 - 100, 87, 0xA0A0A0);
        field_146333_g.drawTextBox();

        // 绘制标签页内容标题
        String tabTitle = modernWorldCreatingUI$currentTab == 100 ? I18n.format("worldcreateui.tab.game") :
                          modernWorldCreatingUI$currentTab == 101 ? I18n.format("createworldui.title.world") :
                          modernWorldCreatingUI$currentTab == 103 ? I18n.format("createworldui.gamerules.title");
        this.drawCenteredString(this.fontRendererObj, tabTitle, this.width / 2, 120, 0xFFFFFF);

        // 绘制所有按钮
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * @author dfdvdsf
     * @reason Custom action performed method to modify the action when clicked the tab button
     */
    @Overwrite
    protected void actionPerformed(GuiButton button) {
        try{
            if (button.id == 1) { // 取消
                this.mc.displayGuiScreen(field_146332_f);
            } else if (button.id == 0) { // 创建世界
                modernWorldCreatingUI$saveSettingsAndCreateWorld();
            } else if (button.id >= 100 && button.id < 103) { // 标签页切换
                modernWorldCreatingUI$currentTab = button.id;
                modernWorldCreatingUI$addTabControls();
            } else {
                modernWorldCreatingUI$handleTabButtonActions(button.id);
            }
        } catch (Exception e){
            modernWorldCreatingUI$logger.error("Action didn't preformed", e);
        }
    }

    @Unique
    private void modernWorldCreatingUI$handleTabButtonActions(int buttonId) {
        switch (buttonId) {
            case 200: // 游戏模式
                selectedGameType = WorldSettings.GameType.getByID((selectedGameType.getID() + 1) % 4);
                break;
            case 201: // 难度按钮
                // 循环切换难度
                EnumDifficulty[] difficulties = EnumDifficulty.values();
                int nextOrdinal = (modernWorldCreatingUI$selectedDifficulty.ordinal() + 1) % difficulties.length;
                modernWorldCreatingUI$selectedDifficulty = difficulties[nextOrdinal];
                // 更新原版difficulty字段以保持兼容性
                this.modernWorldCreatingUI$difficulty = modernWorldCreatingUI$selectedDifficulty.getDifficultyId();
                break;
            case 202: // 作弊
                modernWorldCreatingUI$allowCommands = !modernWorldCreatingUI$allowCommands;
                break;
            case 203: // 世界类型
                WorldType = WorldSettings.getGameTypeById(WorldType.getWorldTypeID() + 1);
                break;
            case 204: // 生成结构
                field_146341_s = !field_146341_s;
                break;
            case 205: // 奖励箱
                field_146338_v = !field_146338_v;
                break;
            case 206: // 生成动物
                modernWorldCreatingUI$spawnAnimals = !modernWorldCreatingUI$spawnAnimals;
                break;
            case 207: // 生成怪物
                modernWorldCreatingUI$spawnMonsters = !modernWorldCreatingUI$spawnMonsters;
                break;
            case 208: // 天气循环
                modernWorldCreatingUI$weatherCycle = !modernWorldCreatingUI$weatherCycle;
                break;
        }
        modernWorldCreatingUI$addTabControls(); // 刷新控件显示
    }

    @Unique
    private void modernWorldCreatingUI$saveSettingsAndCreateWorld() {
        // 保存输入框内容
        field_146334_a = field_146333_g.getText().trim();
        field_146329_I = field_146335_h.getText().trim();

        // 创建世界设置
        long seed = field_146329_I.isEmpty() ? System.currentTimeMillis() :
                field_146329_I.matches("-?\\d+") ? Long.parseLong(field_146329_I) : field_146329_I.hashCode();

        WorldSettings settings = new WorldSettings(
                seed,
                selectedGameType,
                field_146341_s,
                field_146338_v,
                WorldType
        );

        // 启用命令（如果需要）
        if (modernWorldCreatingUI$allowCommands) {
            settings.enableCommands();
        }

        // 启用奖励箱（如果需要）
        if (field_146338_v) {
            settings.enableBonusChest();
        }

        // 调用原版创建世界方法
        createWorld(settings);
    }

    // 修改创建世界后的难度设置
    @Inject(method = "createWorld", at = @At("RETURN"))
    private void onWorldCreated(WorldSettings settings, CallbackInfo ci) {
        // 获取新创建的世界信息并设置难度
        try {
            // 获取集成服务器
            Object integratedServer = Minecraft.getMinecraft().getIntegratedServer();
            if (integratedServer != null) {
                // 获取世界
                Method getWorldMethod = integratedServer.getClass().getMethod("getWorld", int.class);
                Object world = getWorldMethod.invoke(integratedServer, 0);

                // 获取世界信息
                Method getWorldInfoMethod = world.getClass().getMethod("getWorldInfo");
                Object worldInfo = getWorldInfoMethod.invoke(world);

                // 设置难度
                Method setDifficultyMethod = worldInfo.getClass().getMethod("setDifficulty", EnumDifficulty.class);
                setDifficultyMethod.invoke(worldInfo, modernWorldCreatingUI$selectedDifficulty);
            }
        } catch (Exception e) {
            modernWorldCreatingUI$logger.error("Failed to set difficulty after world creation", e);
        }
    }


    @Shadow
    private void createWorld(WorldSettings settings) {}

    @Unique
    private String modernWorldCreatingUI$getDifficultyName() {
        // 通过当前难度ID获取对应的EnumDifficulty实例
        EnumDifficulty currentDifficulty = EnumDifficulty.getDifficultyEnum(this.modernWorldCreatingUI$difficulty);
        // 利用I18n格式化资源键，得到本地化名称（如"和平"、"简单"等）
        return I18n.format(currentDifficulty.getDifficultyResourceKey());
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        field_146333_g.textboxKeyTyped(typedChar, keyCode);
        field_146335_h.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == 1) { // ESC键
            this.mc.displayGuiScreen(field_146332_f);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        field_146333_g.mouseClicked(mouseX, mouseY, mouseButton);
        field_146335_h.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Unique
    private boolean modernWorldCreatingUI$spawnAnimals = true;
    @Unique
    private boolean modernWorldCreatingUI$spawnMonsters = true;
    @Unique
    private boolean modernWorldCreatingUI$weatherCycle = true;
    @Unique
    private boolean modernWorldCreatingUI$allowCommands = false;
}