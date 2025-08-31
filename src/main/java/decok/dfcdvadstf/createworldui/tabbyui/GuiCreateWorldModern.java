package decok.dfcdvadstf.createworldui.tabbyui;

import decok.dfcdvadstf.createworldui.api.CreateWorldAPI;
import decok.dfcdvadstf.createworldui.api.CreateWorldAPI.IWorldTab;
import decok.dfcdvadstf.createworldui.api.util.TextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldSettings;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.io.IOException;

import static decok.dfcdvadstf.createworldui.api.util.TextureHelper.drawModalRectWithCustomSizedTexture;
import static decok.dfcdvadstf.createworldui.api.util.TextureManager.*;

@SuppressWarnings("unchecked")
public class GuiCreateWorldModern extends GuiScreen {
    private final GuiScreen parentScreen;
    private WorldSettings worldSettings;
    private GuiTextField worldNameField;
    private String seedText;
    private GuiTextField seedField;
    private IWorldTab currentTab;
    private final List<GuiButton> tabButtons = new ArrayList<>();

    private WorldSettings.GameType selectedGameType = WorldSettings.GameType.SURVIVAL;
    private WorldType selectedWorldType = WorldType.DEFAULT;

    private boolean spawnAnimals = true;      // 生成动物
    private boolean spawnMonsters = true;     // 生成怪物
    private boolean weatherCycle = true;      // 天气循环
    private boolean daylightCycle = true;     // 日夜循环

    private boolean allowCommands = false;
    private boolean mapFeaturesEnabled = true;
    private boolean bonusChestEnabled = false;
    private int difficulty = 2 ; // 0 = 和平，1 = 简单，2 = 普通，3 = 困难
    private static final Logger LOGGER = LogManager.getLogger();

    // 默认标签页
    private final List<IWorldTab> defaultTabs = new ArrayList<>();

    private static final ResourceLocation OPTIONS_BG_LIGHT = TextureManager.OPTIONS_BG_LIGHT;
    private static final ResourceLocation OPTIONS_BG_DARK = TextureManager.OPTIONS_BG_DARK;

    // 标签页
    public GuiCreateWorldModern(GuiScreen parent) {
        this.parentScreen = parent;
        this.worldSettings = new WorldSettings(0, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT);
        this.seedText = "";

        // 添加默认标签页
        defaultTabs.add(new GameTab());
        defaultTabs.add(new WorldTab());
        defaultTabs.add(new MoreTab());
    }

    // 使用枚举来管理标签页
    private enum TabState {
        NORMAL(TAB_NORMAL_U, TAB_NORMAL_V, 0xE0E0E0),
        HOVER(TAB_HOVER_U, TAB_HOVER_V, 0xFFFFFF),
        SELECTED(TAB_SELECTED_U, TAB_SELECTED_V, 0xFFCC00),
        SELECTED_HOVER(TAB_SELECTED_HOVER_U, TAB_SELECTED_HOVER_V, 0xFFCC00);

        final int u;
        final int v;
        final int textColor;

        TabState(int u, int v, int textColor) {
            this.u = u;
            this.v = v;
            this.textColor = textColor;
        }

        static TabState getState(boolean isSelected, boolean isHovered) {
            if (isSelected) {
                return isHovered ? SELECTED_HOVER : SELECTED;
            }
            return isHovered ? HOVER : NORMAL;
        }
    }

    private void addButtonSafely(GuiButton button) {
        this.buttonList.add(button);
    }

    @Override
    public void initGui() {
        // 初始化键盘监听
        Keyboard.enableRepeatEvents(true);

        // 使用纹理化按钮
        createTabButtons();

        // 初始化当前标签页
        if (currentTab == null) {
            currentTab = defaultTabs.get(0);
        }
        currentTab.initGui(this, worldSettings);

        // 世界名称输入框
        worldNameField = new GuiTextField(this.fontRendererObj, this.width / 2 - 100, 40, 200, 20);
        worldNameField.setFocused(true);
        worldNameField.setText(I18n.format("selectWorld.newWorld"));

        // 添加创建按钮
        this.buttonList.add(new GuiButton(0, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("gui.create")));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("gui.cancel")));
    }

    private void createTabButtons() {
        // 标签页创建
        tabButtons.clear();
        int xPos = this.width / 2 - 150;
        int yPos = 5;

        // 标签页获取
        List<IWorldTab> allTabs = getAllTabs();

        // 判断是否获取标签页
        for (int i = 0; i < allTabs.size(); i++) {
            final IWorldTab tab = allTabs.get(i);
            final boolean isSelected = currentTab == tab;

            GuiButton tabButton = new GuiButton(1000 + i, xPos, yPos, TAB_WIDTH, TAB_HEIGHT, tab.getTabName()) {
                // 判断按钮状态
                @Override
                public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                    if (this.visible) {
                        mc.getTextureManager().bindTexture(TABS);

                        boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                                mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                        TabState state = TabState.getState(isSelected, isHovered);

                        drawTexturedModalRect(this.xPosition, this.yPosition, state.u, state.v, width, height);

                        drawCenteredString(mc.fontRenderer, this.displayString,
                                this.xPosition + this.width / 2,
                                this.yPosition + (this.height - 8) / 2, state.textColor);
                    }
                }
            };

            tabButtons.add(tabButton);
            addButtonSafely(tabButton);
            xPos += TAB_WIDTH + 5;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        // 绘制浅色背景
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_LIGHT);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, this.width, this.height, this.width, this.height);

        // 绘制深色内容区域
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);
        drawModalRectWithCustomSizedTexture(10, 30, 0, 0, this.width - 20, this.height - 40, 16, 16);

        // 绘制标题
        this.drawCenteredString(this.fontRendererObj, I18n.format("selectWorld.create"), this.width / 2, 20, 0xFFFFFF);

        // 绘制世界名称标签和输入框
        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), this.width / 2 - 100, 27, 0xA0A0A0);
        worldNameField.drawTextBox();

        // 绘制当前标签页内容
        if (currentTab != null) {
            currentTab.drawScreen(mouseX, mouseY, partialTicks);
        }

        // 绘制按钮和文本
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            // 取消按钮
            this.mc.displayGuiScreen(parentScreen);
        } else if (button.id == 0) {
            // 创建世界按钮
            createWorld();
        } else if (button.id >= 1000) {
            // 标签页切换
            int tabIndex = button.id - 1000;
            List<IWorldTab> allTabs = getAllTabs();
            if (tabIndex < allTabs.size()) {
                currentTab = allTabs.get(tabIndex);
                currentTab.initGui(this, worldSettings);
            }
        }

        // 将事件传递给当前标签页
        if (currentTab != null) {
            try {
                currentTab.actionPerformed(button);
            } catch (IOException e) {
                LOGGER.error("Error in tab actionPerformed: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        try {
            worldNameField.textboxKeyTyped(typedChar, keyCode);

            // 将事件传递给当前标签页
            if (currentTab != null) {
                currentTab.keyTyped(typedChar, keyCode);
            }
        } catch (IOException e) {
            LOGGER.error("Error in tab keyTyped: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        try {
            worldNameField.mouseClicked(mouseX, mouseY, mouseButton);

            // 将事件传递给当前标签页
            if (currentTab != null) {
                currentTab.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } catch (IOException e) {
            // 处理或记录异常
            LOGGER.error("Error in tab mouseClicked: {}", e.getMessage(), e);
        }


    }

    private void createWorld() {
        /**
         * String worldName = worldNameField.getText().trim();
         *         if (worldName.isEmpty()) {
         *             worldName = I18n.format("selectWorld.newWorld");
         *         }
         *
         *         // 创建世界设置
         *         WorldSettings settings = new WorldSettings(
         *                 parseSeed(seedText),
         *                 worldSettings.getGameType(),
         *                 mapFeaturesEnabled,
         *                 bonusChestEnabled,
         *                 worldSettings.getTerrainType()
         *         );
         *
         *         // 应用当前标签页的设置
         *         if (currentTab != null) {
         *             settings = currentTab.applySettings(settings);
         *         }
         *
         *         // 创建世界
         *         this.mc.launchIntegratedServer(worldName, worldName, settings);
         *         this.mc.displayGuiScreen(null);
         */

        String worldName = worldNameField.getText().trim();
        if (worldName.isEmpty()) {
            worldName = I18n.format("selectWorld.newWorld");
        }

        long seed;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (NumberFormatException e) {
            seed = seedField.getText().hashCode();
        }

        // 创建新的 WorldSettings 实例
        worldSettings = new WorldSettings(
                seed,
                selectedGameType,
                mapFeaturesEnabled,
                bonusChestEnabled,
                selectedWorldType
        );

    }

    private long parseSeed(String seed) {
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException e) {
            return seed.hashCode();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private List<IWorldTab> getAllTabs() {
        List<IWorldTab> allTabs = new ArrayList<>(defaultTabs);
        allTabs.addAll(CreateWorldAPI.getTabs());
        allTabs.sort(Comparator.comparingInt(IWorldTab::getTabOrder));
        return allTabs;
    }

    // ================= 默认标签页实现 =================

    /**
     * Game 标签页 - 包含游戏模式、难度和作弊选项
     */
    private class GameTab implements IWorldTab {
        private GuiButton gameModeButton;
        private GuiButton difficultyButton;
        private GuiButton cheatsButton;

        @Override
        public String getTabName() {
            return I18n.format("worldcreateui.tab.game");
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            int yPos = 80;

            // 游戏模式按钮
            gameModeButton = new GuiButton(200, width / 2 - 100, yPos, 200, 20,
                    I18n.format("selectWorld.gameMode") + ": " + selectedGameType.getName());
            yPos += 25;

            // 难度按钮
            difficultyButton = new GuiButton(201, width / 2 - 100, yPos, 200, 20,
                    I18n.format("worldcreateui.button.selectWorld.difficulty") + getDifficultyText());
            yPos += 25;

            // 作弊按钮
            cheatsButton = new GuiButton(202, width / 2 - 100, yPos, 200, 20,
                    I18n.format("selectWorld.allowCommands") +
                            (allowCommands ? I18n.format("options.on") : I18n.format("options.off")));

            addButtonSafely(gameModeButton);
            addButtonSafely(difficultyButton);
            addButtonSafely(cheatsButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // 绘制标签页背景
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 150, 16, 16);

            // 绘制标签页标题
            drawCenteredString(fontRendererObj, I18n.format("worldcreateui.title.game"), width / 2, 60, 0xFFFFFF);

            // 绘制游戏模式描述
            String description = getGameModeDescription();
            List<String> lines = fontRendererObj.listFormattedStringToWidth(description, 180);
            int yPos = 170;
            for (String line : lines) {
                drawCenteredString(fontRendererObj, line, width / 2, yPos, 0xCCCCCC);
                yPos += 10;
            }
        }

        @Override
        public void actionPerformed(GuiButton button) {
            if (button == gameModeButton) {
                cycleGameMode();
                button.displayString = I18n.format("selectWorld.gameMode") + ": " + selectedGameType.getName();
            } else if (button == difficultyButton) {
                difficulty = (difficulty + 1) % 4;
                button.displayString = I18n.format("worldcreateui.button.selectWorld.difficulty")+ getDifficultyText();
            } else if (button == cheatsButton) {
                allowCommands = !allowCommands;
                button.displayString = I18n.format("selectWorld.allowCommands") +
                        (allowCommands ? I18n.format("options.on") : I18n.format("options.off"));
            }
        }

        private void cycleGameMode() {
            WorldSettings.GameType[] modes = WorldSettings.GameType.values();
            int next = (selectedGameType.ordinal() + 1) % modes.length;
            selectedGameType = modes[next];
        }

        private String getDifficultyText() {
            switch (difficulty) {
                case 0: return I18n.format("worldcreateui.button.selectWorld.difficulty.peaceful");
                case 1: return I18n.format("worldcreateui.button.selectWorld.difficulty.easy");
                case 2: return I18n.format("worldcreateui.button.selectWorld.difficulty.normal");
                case 3: return I18n.format("worldcreateui.button.selectWorld.difficulty.hard");
                default: return "";
            }
        }

        private String getGameModeDescription() {
            switch (selectedGameType) {
                case SURVIVAL:
                    return I18n.format("createworldui.desc.gamemode.survival");
                case CREATIVE:
                    return I18n.format("createworldui.desc.gamemode.creative");
                case ADVENTURE:
                    return I18n.format("createworldui.desc.gamemode.adventure");
                case NOT_SET:
                    return I18n.format("createworldui.desc.gamemode.not_set");
                default:
                    return "";
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {}
        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {}
        @Override
        public WorldSettings applySettings(WorldSettings settings) { return settings; }
        @Override
        public int getTabOrder() { return 10; }
    }

    /**
     * World 标签页 - 包含世界类型、种子、生成建筑和奖励箱
     */
    private class WorldTab implements IWorldTab {
        private GuiButton worldTypeButton;
        private GuiButton mapFeaturesButton;
        private GuiButton bonusChestButton;

        @Override
        public String getTabName() {
              return I18n.format("worldcreateui.tab.world");
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            int yPos = 80;

            // 世界类型按钮
            worldTypeButton = new GuiButton(300, width / 2 - 100, yPos, 200, 20,
                    I18n.format("selectWorld.mapType") + selectedWorldType.getWorldTypeName());
            yPos += 25;

            // 种子输入框
            seedField = new GuiTextField(fontRendererObj, width / 2 - 100, yPos, 200, 20);
            seedField.setText("");
            yPos += 25;

            // 生成建筑按钮
            mapFeaturesButton = new GuiButton(301, width / 2 - 100, yPos, 200, 20,
                    I18n.format("selectWorld.mapFeatures") +
                            (mapFeaturesEnabled ? I18n.format("options.on") : I18n.format("options.off")));
            yPos += 25;

            // 奖励箱按钮
            bonusChestButton = new GuiButton(302, width / 2 - 100, yPos, 200, 20,
                    I18n.format("selectWorld.bonusItems")  +
                            (bonusChestEnabled ? I18n.format("options.on") : I18n.format("options.off")));

            addButtonSafely(worldTypeButton);
            addButtonSafely(mapFeaturesButton);
            addButtonSafely(bonusChestButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // 绘制标签页背景
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 190, 16, 16);

            // 绘制标签页标题
            drawCenteredString(fontRendererObj, I18n.format("worldcreateui.title.world"), width / 2, 60, 0xFFFFFF);

            // 绘制种子标签
            drawString(fontRendererObj, I18n.format("selectWorld.enterSeed"), width / 2 - 100, 127, 0xA0A0A0);
            seedField.drawTextBox();

            // 绘制种子提示
            drawString(fontRendererObj, I18n.format("createworld.seed.tooltip"),
                    width / 2 - 100, 170, 0xAAAAAA);

            // 绘制世界类型描述
            String description = selectedWorldType.getTranslateName();
            List<String> lines = fontRendererObj.listFormattedStringToWidth(description, 180);
            int yPos = 190;
            for (String line : lines) {
                drawCenteredString(fontRendererObj, line, width / 2, yPos, 0xCCCCCC);
                yPos += 10;
            }
        }

        @Override
        public void actionPerformed(GuiButton button) {
            if (button == worldTypeButton) {
                cycleWorldType();
                button.displayString = I18n.format("selectWorld.mapType") + selectedWorldType.getWorldTypeName();
            } else if (button == mapFeaturesButton) {
                mapFeaturesEnabled = !mapFeaturesEnabled;
                button.displayString = I18n.format("selectWorld.mapFeatures") + (mapFeaturesEnabled ? I18n.format("options.on") : I18n.format("options.off"));
            } else if (button == bonusChestButton) {
                bonusChestEnabled = !bonusChestEnabled;
                button.displayString = I18n.format("selectWorld.bonusItems") + (bonusChestEnabled ? I18n.format("options.on") : I18n.format("options.off"));
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            seedField.textboxKeyTyped(typedChar, keyCode);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            seedField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        private void cycleWorldType() {
            WorldType[] types = {WorldType.DEFAULT, WorldType.FLAT, WorldType.LARGE_BIOMES};
            int currentIndex = Arrays.asList(types).indexOf(selectedWorldType);
            int nextIndex = (currentIndex + 1) % types.length;
            selectedWorldType = types[nextIndex];
        }

        @Override
        public WorldSettings applySettings(WorldSettings settings) { return settings; }
        @Override
        public int getTabOrder() { return 20; }
    }

    /**
     * More 标签页 - 包含游戏规则
     */
    private class MoreTab implements IWorldTab {
        private GuiButton spawnAnimalsButton;
        private GuiButton spawnMonstersButton;
        private GuiButton weatherCycleButton;
        private GuiButton daylightCycleButton;

        @Override
        public String getTabName() {
            return I18n.format("worldcreateui.tab.more");
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            int yPos = 80;
            int buttonWidth = 180;

            // 游戏规则按钮
            spawnAnimalsButton = new GuiButton(400, width / 2 - buttonWidth / 2, yPos, buttonWidth, 20,
                    I18n.format("createworldui.gamerules.spawn_animals")  +
                            (spawnAnimals ? I18n.format("options.on") : I18n.format("options.off")));
            yPos += 25;

            spawnMonstersButton = new GuiButton(401, width / 2 - buttonWidth / 2, yPos, buttonWidth, 20,
                    I18n.format("createworldui.gamerules.spawn_monsters") + ": " +
                            (spawnMonsters ? I18n.format("options.on") : I18n.format("options.off")));
            yPos += 25;

            weatherCycleButton = new GuiButton(402, width / 2 - buttonWidth / 2, yPos, buttonWidth, 20,
                    I18n.format("createworldui.gamerules.weather_cycle") + ": " +
                            (weatherCycle ? I18n.format("options.on") : I18n.format("options.off")));
            yPos += 25;

            daylightCycleButton = new GuiButton(403, width / 2 - buttonWidth / 2, yPos, buttonWidth, 20,
                    I18n.format("createworldui.gamerules.daylight_cycle") + ": " +
                            (daylightCycle ? I18n.format("options.on") : I18n.format("options.off")));

            addButtonSafely(spawnAnimalsButton);
            addButtonSafely(spawnMonstersButton);
            addButtonSafely(weatherCycleButton);
            addButtonSafely(daylightCycleButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // 绘制标签页背景
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 190, 16, 16);

            // 绘制标签页标题
            drawCenteredString(fontRendererObj, I18n.format("createworldui.gamerules.title"), width / 2, 60, 0xFFFFFF);

            // 绘制实验性警告
            drawCenteredString(fontRendererObj, I18n.format("createworldui.experimental.warning"),
                    width / 2, 170, 0xFF5555);
        }

        @Override
        public void actionPerformed(GuiButton button) {
            if (button == spawnAnimalsButton) {
                spawnAnimals = !spawnAnimals;
                button.displayString = I18n.format("createworldui.gamerules.spawn_animals") + ": " +
                        (spawnAnimals ? I18n.format("options.on") : I18n.format("options.off"));
            } else if (button == spawnMonstersButton) {
                spawnMonsters = !spawnMonsters;
                button.displayString = I18n.format("createworldui.gamerules.spawn_monsters") + ": " +
                        (spawnMonsters ? I18n.format("options.on") : I18n.format("options.off"));
            } else if (button == weatherCycleButton) {
                weatherCycle = !weatherCycle;
                button.displayString = I18n.format("createworldui.gamerules.weather_cycle") + ": " +
                        (weatherCycle ? I18n.format("options.on") : I18n.format("options.off"));
            } else if (button == daylightCycleButton) {
                daylightCycle = !daylightCycle;
                button.displayString = I18n.format("createworldui.gamerules.daylight_cycle") + ": " +
                        (daylightCycle ? I18n.format("options.on") : I18n.format("options.off"));
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            
        }
        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            
        }
        @Override
        public WorldSettings applySettings(WorldSettings settings) { return settings; }
        @Override
        public int getTabOrder() { return 30; }
    }
}