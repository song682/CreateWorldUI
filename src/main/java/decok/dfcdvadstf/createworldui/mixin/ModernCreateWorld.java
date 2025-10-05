package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.TabState;
import decok.dfcdvadstf.createworldui.editor.GameRuleEditor;
import decok.dfcdvadstf.createworldui.util.DifficultyHandler;
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

import java.util.*;

import static decok.dfcdvadstf.createworldui.api.util.TextureHelper.drawModalRectWithCustomSizedTexture;


@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class ModernCreateWorld extends GuiScreen {

    /***
     * 
     */
    @Unique
    private static Logger modernWorldCreatingUI$logger;
    @Shadow
    private boolean field_146337_w; // 硬核模式
    @Shadow
    private boolean field_146345_x; // 防止重复创建
    @Shadow
    private String field_146330_J; // 世界显示名称
    @Shadow
    private GuiScreen field_146332_f; //parentScreen
    @Shadow
    private String field_146329_I; // 种子
    @Shadow
    private boolean field_146341_s; // 生成建筑
    @Shadow
    private boolean field_146338_v; // 奖励箱
    @Shadow
    private WorldSettings.GameType GameType; // 设置游戏类型，生存还是创造还是极限。
    @Shadow
    private WorldType worldType; // 世界类型
    @Shadow
    public String field_146334_a; // 世界名称
    @Shadow
    private GuiTextField field_146335_h; // 种子输入框
    @Shadow
    private GuiTextField field_146333_g; // 世界名称输入框
    @Shadow
    private boolean field_146340_t; // 允许作弊
    @Shadow
    private GuiButton field_146321_E; // 允许作弊按钮
    @Shadow
    private GuiButton field_146326_C; // 奖励箱按钮
    @Shadow
    private boolean field_146339_u; // 是否手动修改过允许作弊

    @Unique
    private int modernWorldCreatingUI$difficulty;
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
    @Unique
    private EnumDifficulty modernWorldCreatingUI$selectedDifficulty = EnumDifficulty.NORMAL;

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void initModernGui(CallbackInfo ci) {
        ci.cancel();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.modernWorldCreatingUI$tabButtons.clear();

        if (this.modernWorldCreatingUI$selectedDifficulty == null) {
            // 使用 DifficultyHandler 根据ID获取难度
            this.modernWorldCreatingUI$selectedDifficulty = DifficultyHandler.getDifficultyById(this.modernWorldCreatingUI$difficulty);
            // 如果获取到的难度为null，使用默认难度
            if (this.modernWorldCreatingUI$selectedDifficulty == null) {
                this.modernWorldCreatingUI$selectedDifficulty = EnumDifficulty.NORMAL;
                this.modernWorldCreatingUI$difficulty = EnumDifficulty.NORMAL.getDifficultyId();
            }
        }


        modernWorldCreatingUI$createTabButtons();

        field_146333_g = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 40, 200, 20);
        field_146333_g.setFocused(true);
        field_146333_g.setText(field_146334_a == null ? I18n.format("selectWorld.newWorld") : field_146334_a);

        field_146335_h = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 100, 200, 20);
        field_146335_h.setText(field_146329_I == null ? "" : field_146329_I);

        modernWorldCreatingUI$addButton(new GuiButton(0, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("createworldui.button.create")));
        modernWorldCreatingUI$addButton(new GuiButton(1, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("createworldui.button.cancel")));

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
                        TabState state = getTabState(mouseX, mouseY);

                        drawTexturedModalRect(this.xPosition, this.yPosition, state.u, state.v, TAB_WIDTH, TAB_HEIGHT);
                        drawCenteredString(mc.fontRenderer, this.displayString,
                                this.xPosition + this.width / 2,
                                this.yPosition + (this.height - 8) / 2, state.textColor);
                    }
                }

                private TabState getTabState(int mouseX, int mouseY) {
                    boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                            mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                    boolean isSelected = modernWorldCreatingUI$currentTab == this.id;

                    TabState state = isSelected ?
                            (isHovered ? TabState.SELECTED_HOVER : TabState.SELECTED) :
                            (isHovered ? TabState.HOVER : TabState.NORMAL);
                    return state;
                }
            };
            modernWorldCreatingUI$tabButtons.add(tabButton);
            modernWorldCreatingUI$addButton(tabButton);
            xPos += TAB_WIDTH + 5;
        }
    }

    @Unique
    private void modernWorldCreatingUI$addButton(GuiButton button) {
        this.buttonList.add(button);
    }

    @Unique
    private void modernWorldCreatingUI$addTabControls() {
        /** This one I used throws, but it didn't work.
         * this.buttonList.removeIf(button -> (button.id >= 200 && button.id < 300) || button.id == 300);
         */

        /**
         *  This one used a more traditional method of iterator.
         *         Iterator<GuiButton> iterator = this.buttonList.iterator();
         *          while (iterator.hasNext()) {
         *             GuiButton button = iterator.next();
         *             if ((button.id >= 200 && button.id < 300) || button.id == 300) {
         *                 iterator.remove();
         *             }
         *         }
         *
         *         Int yPos = 140;
         *         switch (modernWorldCreatingUI$currentTab) {
         *             case 100:
         *                 modernWorldCreatingUI$addGameTabControls(yPos);
         *                 break;
         *             case 101:
         *                 modernWorldCreatingUI$addWorldTabControls(yPos);
         *                 break;
         *             case 102:
         *                 modernWorldCreatingUI$addMoreTabControls(yPos);
         *                 break;
         *         }
         */
        // 修复：使用映射来管理标签页控件
        // 首先移除之前添加的标签页控件
        for (GuiButton button : modernWorldCreatingUI$tabControls.values()) {
            this.buttonList.remove(button);
        }
        modernWorldCreatingUI$tabControls.clear();

        int yPos = 140;
        switch (modernWorldCreatingUI$currentTab) {
            case 100:
                modernWorldCreatingUI$addGameTabControls(yPos);
                break;
            case 101:
                modernWorldCreatingUI$addWorldTabControls(yPos);
                break;
            case 102:
                modernWorldCreatingUI$addMoreTabControls(yPos);
                break;
        }

        // 将新控件添加到按钮列表
        for (GuiButton button : modernWorldCreatingUI$tabControls.values()) {
            modernWorldCreatingUI$addButton(button);
        }
    }

    @Unique
    private void modernWorldCreatingUI$addGameTabControls(int yPos) {
        modernWorldCreatingUI$addButton(new GuiButton(200, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.gameMode") + ": " + GameType.getName()));
        yPos += 25;

        // 使用 DifficultyHandler 获取显示名称
        modernWorldCreatingUI$addButton(new GuiButton(201, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("options.difficulty") + ": " +
                        DifficultyHandler.getDifficultyDisplayName(modernWorldCreatingUI$selectedDifficulty)));
        yPos += 25;

        modernWorldCreatingUI$addButton(new GuiButton(202, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.allowCommands") + (field_146340_t ? I18n.format("gui.yes") : I18n.format("gui.no"))));
    }

    @Unique
    private void modernWorldCreatingUI$addWorldTabControls(int yPos) {
        modernWorldCreatingUI$addButton(new GuiButton(203, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.mapType") + worldType.getWorldTypeName()));
        yPos += 25;

        modernWorldCreatingUI$addButton(new GuiButton(204, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.mapFeatures") + (field_146341_s ? I18n.format("gui.yes") : I18n.format("gui.no"))));
        yPos += 25;

        modernWorldCreatingUI$addButton(new GuiButton(205, this.width / 2 - 100, yPos, 200, 20,
                I18n.format("selectWorld.bonusItems")  + (field_146338_v ? I18n.format("gui.yes") : I18n.format("gui.no"))));
    }

    @Unique
    private void modernWorldCreatingUI$addMoreTabControls(int yPos) {
        // Add
        // TODO: Add datapacks support, if someone wanna do this.
        modernWorldCreatingUI$tabControls.put(300, new GuiButton(300, this.width / 2 - 100, yPos, 200, 20, I18n.format("createworldui.button.gameRuleEditor")));
    }

    /**
     *
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_LIGHT);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, this.width, this.height, 16, 16);

        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);

        this.drawCenteredString(this.fontRendererObj, I18n.format("selectWorld.create"), this.width / 2, 15, 0xFFFFFF);

        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), this.width / 2 - 100, 27, 0xA0A0A0);
        field_146333_g.drawTextBox();

        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterSeed"), this.width / 2 - 100, 87, 0xA0A0A0);
        field_146335_h.drawTextBox();

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

    /**
     * @author dfdvdsf
     * @reason
     */
    @Overwrite
    protected void actionPerformed(GuiButton button) {
        try {
            if (button.id == 1) {
                this.mc.displayGuiScreen(field_146332_f);
            } else if (button.id == 0) {
                modernWorldCreatingUI$saveSettingsAndCreateWorld();
            } else if (button.id >= 100 && button.id <= 102) {
                modernWorldCreatingUI$currentTab = button.id;
                modernWorldCreatingUI$addTabControls();
            } else if (button.id == 300) {
                // 打开游戏规则编辑器
                this.mc.displayGuiScreen(new GameRuleEditor(null));
            } else {
                modernWorldCreatingUI$handleTabButtonActions(button.id);
            }
        } catch (Exception e) {
            if (modernWorldCreatingUI$logger != null) {
                modernWorldCreatingUI$logger.error("Action didn't perform", e);
            }
        }
    }

    @Unique
    private void modernWorldCreatingUI$handleTabButtonActions(int buttonId) {
        switch (buttonId) {
            case 200:
                // 游戏模式切换
                GameType = WorldSettings.GameType.getByID((GameType.getID() + 1) % 4);

                // 根据游戏模式更新硬核模式状态
                if (GameType == WorldSettings.GameType.SURVIVAL) {
                    field_146337_w = false;
                    field_146321_E.enabled = true;
                    field_146326_C.enabled = true;
                } else if (GameType == WorldSettings.GameType.CREATIVE) {
                    field_146337_w = false;
                    field_146321_E.enabled = true;
                    field_146326_C.enabled = true;
                }
                break;

            case 201:
                modernWorldCreatingUI$selectedDifficulty = DifficultyHandler.getNextDifficulty(modernWorldCreatingUI$selectedDifficulty);
                this.modernWorldCreatingUI$difficulty = modernWorldCreatingUI$selectedDifficulty.getDifficultyId();
                break;

            case 202:
                field_146340_t = !field_146340_t;
                break;

            case 203:
                WorldType[] worldTypes = WorldType.worldTypes;
                int currentId = worldType.getWorldTypeID();
                int nextId = (currentId + 1);

                // 循环查找有效的世界类型
                while (true) {
                    if (nextId >= worldTypes.length) {
                        nextId = 0;
                    }
                    if (worldTypes[nextId] != null && worldTypes[nextId].getCanBeCreated()) {
                        worldType = worldTypes[nextId];
                        field_146334_a = ""; // 重置自定义设置
                        break;
                    }
                    nextId++;
                }
                break;

            case 204:
                field_146341_s = !field_146341_s;
                break;

            case 205:
                field_146338_v = !field_146338_v;
                break;
        }
        modernWorldCreatingUI$addTabControls();
    }

    @Unique
    private void modernWorldCreatingUI$updateButtonStates() {
        // 根据游戏模式更新按钮状态
        if (GameType.getID() == 1) { // 硬核模式
            // 硬核模式禁用允许作弊和奖励箱
            field_146340_t = false;
            field_146338_v = false;
        }
    }

    @Unique
    private void modernWorldCreatingUI$saveSettingsAndCreateWorld() {
        // 防止重复创建
        if (this.field_146345_x) {
            return;
        }
        this.field_146345_x = true;

        // 获取世界名称和种子
        field_146334_a = field_146333_g.getText().trim();
        field_146329_I = field_146335_h.getText().trim();

        // 处理世界名称（使用原始逻辑）
        modernWorldCreatingUI$processWorldName();

        // 处理种子（使用原始逻辑）
        long seed;
        if (field_146329_I.isEmpty()) {
            seed = (new Random()).nextLong();
        } else {
            try {
                seed = Long.parseLong(field_146329_I);
                if (seed == 0L) {
                    seed = (new Random()).nextLong();
                }
            } catch (NumberFormatException numberformatexception) {
                seed = (long) field_146329_I.hashCode();
            }
        }

        // 调用世界类型的预处理
        worldType.onGUICreateWorldPress();

        // 使用原始的游戏模式字符串（与原始代码保持一致）
        String GameTypeStr;
        if (GameType == WorldSettings.GameType.SURVIVAL) {
            GameTypeStr = "survival";
        } else if (GameType == WorldSettings.GameType.CREATIVE) {
            GameTypeStr = "creative";
        } else {
            GameTypeStr = "hardcore";
        }

        // 创建 WorldSettings（使用原始构造函数）
        WorldSettings worldsettings = new WorldSettings(
                seed,
                WorldSettings.GameType.getByName(GameTypeStr),
                field_146341_s,
                field_146337_w, // 硬核模式
                worldType
        );

        // 设置自定义世界生成选项
        worldsettings.func_82750_a(field_146334_a);

        // 启用奖励箱（如果选择）
        if (field_146338_v && !field_146337_w) {
            worldsettings.enableBonusChest();
        }

        // 启用作弊（如果选择）
        if (field_146340_t && !field_146337_w) {
            worldsettings.enableCommands();
        }

        // 使用原始方法创建世界
        this.mc.launchIntegratedServer(
                modernWorldCreatingUI$getFolderName(), // 文件夹名称
                field_146333_g.getText().trim(),        // 显示名称
                worldsettings
        );
    }
    
    @Unique
    private void modernWorldCreatingUI$processWorldName() {
        // 复制原始的世界名称处理逻辑
        String folderName = field_146333_g.getText().trim();
        char[] allowedChars = net.minecraft.util.ChatAllowedCharacters.allowedCharacters;

        // 替换不允许的字符
        for (char c : allowedChars) {
            folderName = folderName.replace(c, '_');
        }

        // 如果名称为空，使用默认名称
        if (net.minecraft.util.MathHelper.stringNullOrLengthZero(folderName)) {
            folderName = "World";
        }

        // 处理特殊文件名
        folderName = modernWorldCreatingUI$getValidFolderName(folderName);

        // 设置文件夹名称
        this.field_146330_J = folderName;
    }

    @Unique
    private String modernWorldCreatingUI$getValidFolderName(String name) {
        // 复制原始的文件名验证逻辑
        name = name.replaceAll("[\\./\"]", "_");

        String[] invalidNames = new String[] {
                "CON", "COM", "PRN", "AUX", "CLOCK$", "NUL",
                "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };

        for (String invalid : invalidNames) {
            if (name.equalsIgnoreCase(invalid)) {
                name = "_" + name + "_";
            }
        }

        // 检查是否已存在同名世界
        while (this.mc.getSaveLoader().getWorldInfo(name) != null) {
            name = name + "-";
        }

        return name;
    }

    @Unique
    private String modernWorldCreatingUI$getFolderName() {
        return this.field_146330_J;
    }

    @Unique
    private String modernWorldCreatingUI$getDifficultyName() {
        EnumDifficulty currentDifficulty = EnumDifficulty.getDifficultyEnum(this.modernWorldCreatingUI$difficulty);
        return I18n.format(currentDifficulty.getDifficultyResourceKey());
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        field_146333_g.textboxKeyTyped(typedChar, keyCode);
        field_146335_h.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == 1) {
            this.mc.displayGuiScreen(field_146332_f);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        field_146333_g.mouseClicked(mouseX, mouseY, mouseButton);
        field_146335_h.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
