package decok.dfcdvadstf.createworldui.tabbyui;

import decok.dfcdvadstf.createworldui.api.CreateWorldAPI;
import decok.dfcdvadstf.createworldui.api.CreateWorldAPI.IWorldTab;
import decok.dfcdvadstf.createworldui.api.util.TextureHelper;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldSettings;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.io.IOException;

import static decok.dfcdvadstf.createworldui.api.util.TextureHelper.drawModalRectWithCustomSizedTexture;

public class GuiCreateWorldModern extends GuiScreen {
    private final GuiScreen parentScreen;
    private WorldSettings worldSettings;
    private GuiTextField worldNameField;
    private String seedText;
    private boolean mapFeaturesEnabled = true;
    private boolean bonusChestEnabled;
    private IWorldTab currentTab;
    private final List<GuiButton> tabButtons = new ArrayList<>();

    // 默认标签页
    private final List<IWorldTab> defaultTabs = new ArrayList<>();

    private static final ResourceLocation OPTIONS_BG_LIGHT = TextureManager.OPTIONS_BG_LIGHT;
    private static final ResourceLocation OPTIONS_BG_DARK = TextureManager.OPTIONS_BG_DARK;

    public GuiCreateWorldModern(GuiScreen parent) {
        this.parentScreen = parent;
        this.worldSettings = new WorldSettings(0, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT);
        this.seedText = "";

        // 添加默认标签页
        defaultTabs.add(new BasicTab());
        defaultTabs.add(new GameModeTab());
        defaultTabs.add(new WorldTypeTab());
        defaultTabs.add(new MoreOptionsTab());
    }

    @Override
    public void initGui() {
        // 初始化键盘监听
        Keyboard.enableRepeatEvents(true);

        // 创建标签页按钮
        createTabButtons();

        // 使用纹理化按钮替换普通按钮
        createTexturedTabButtons();

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

    private void createTexturedTabButtons() {
        tabButtons.clear();
        int xPos = 10;
        int yPos = 5;

        List<IWorldTab> allTabs = getAllTabs();

        for (int i = 0; i < allTabs.size(); i++) {
            IWorldTab tab = allTabs.get(i);
            boolean isSelected = currentTab == tab;

            TexturedButton tabButton = new TexturedButton(
                    1000 + i,
                    xPos,
                    yPos,
                    tab.getTabName(),
                    TextureManager.TABS,
                    isSelected ? TextureManager.TAB_SELECTED_U : TextureManager.TAB_NORMAL_U,
                    isSelected ? TextureManager.TAB_SELECTED_V : TextureManager.TAB_NORMAL_V,
                    TextureManager.TAB_HOVER_U,
                    TextureManager.TAB_HOVER_V
            );

            tabButtons.add(tabButton);
            this.buttonList.add(tabButton);
            xPos += 105;
        }
    }

    private void createTabButtons() {
        tabButtons.clear();
        int xPos = 10;
        int yPos = 5;

        // 合并默认标签页和API注册的标签页
        List<IWorldTab> allTabs = new ArrayList<>(defaultTabs);
        allTabs.addAll(CreateWorldAPI.getTabs());

        // 按顺序排序
        allTabs.sort(Comparator.comparingInt(IWorldTab::getTabOrder));

        for (IWorldTab tab : allTabs) {
            GuiButton tabButton = new GuiButton(1000 + tabButtons.size(), xPos, yPos, 100, 20, tab.getTabName());
            tabButtons.add(tabButton);
            this.buttonList.add(tabButton);
            xPos += 105;
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
    protected void actionPerformed(GuiButton button) throws IOException {
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
            currentTab.actionPerformed(button);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        worldNameField.textboxKeyTyped(typedChar, keyCode);

        // 将事件传递给当前标签页
        if (currentTab != null) {
            currentTab.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        worldNameField.mouseClicked(mouseX, mouseY, mouseButton);

        // 将事件传递给当前标签页
        if (currentTab != null) {
            currentTab.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void createWorld() {
        String worldName = worldNameField.getText().trim();
        if (worldName.isEmpty()) {
            worldName = I18n.format("selectWorld.newWorld");
        }

        // 创建世界设置
        WorldSettings settings = new WorldSettings(
                parseSeed(seedText),
                worldSettings.getGameType(),
                mapFeaturesEnabled,
                bonusChestEnabled,
                worldSettings.getTerrainType()
        );

        // 应用当前标签页的设置
        if (currentTab != null) {
            settings = currentTab.applySettings(settings);
        }

        // 创建世界
        this.mc.launchIntegratedServer(worldName, worldName, settings);
        this.mc.displayGuiScreen(null);
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

    private class BasicTab implements IWorldTab {
        private GuiTextField seedField;

        @Override
        public String getTabName() {
            return "Basic";
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            seedField = new GuiTextField(fontRendererObj, width / 2 - 100, 80, 200, 20);
            seedField.setText(seedText);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 100, 16, 16);

            drawString(fontRendererObj, "World Seed", width / 2 - 100, 67, 0xA0A0A0);
            seedField.drawTextBox();
        }

        @Override
        public void actionPerformed(GuiButton button) throws IOException {
            // 不需要处理
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) throws IOException {
            seedField.textboxKeyTyped(typedChar, keyCode);
            seedText = seedField.getText();
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            seedField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public WorldSettings applySettings(WorldSettings settings) {
            return settings;
        }

        @Override
        public int getTabOrder() {
            return 10;
        }
    }

    private class GameModeTab implements IWorldTab {
        private GuiButton gameModeButton;
        private GuiButton cheatsButton;

        @Override
        public String getTabName() {
            return "Game Mode";
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            gameModeButton = new GuiButton(200, width / 2 - 100, 80, 200, 20, getGameModeText());
            cheatsButton = new GuiButton(201, width / 2 - 100, 110, 200, 20,
                    I18n.format("selectWorld.allowCommands") + (worldSettings.areCommandsAllowed() ? I18n.format("options.on") : I18n.format("options.off")));
            buttonList.add(gameModeButton);
            buttonList.add(cheatsButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 100, 16, 16);

            drawCenteredString(fontRendererObj, I18n.format("selectWorld.gameMode"), width / 2, 60, 0xFFFFFF);
        }

        @Override
        public void actionPerformed(GuiButton button) throws IOException {
            if (button == gameModeButton) {
                cycleGameMode();
                button.displayString = getGameModeText();
            } else if (button == cheatsButton) {
                // 1.7.10正确方法：setEnableCommands
                worldSettings.setEnableCommands(!worldSettings.areCommandsAllowed());
                button.displayString = "Allow Cheats: " + (worldSettings.areCommandsAllowed() ? "ON" : "OFF");
            }
        }

        private void cycleGameMode() {
            WorldSettings.GameType[] modes = WorldSettings.GameType.values();
            int next = (worldSettings.getGameType().ordinal() + 1) % modes.length;
            // 1.7.10正确方法：setGameType
            worldSettings.setGameType(modes[next]);
        }

        private String getGameModeText() {
            return "Game Mode: " + worldSettings.getGameType().getName();
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) throws IOException {
            // 空实现
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            // 空实现
        }

        @Override
        public WorldSettings applySettings(WorldSettings settings) {
            return settings;
        }

        @Override
        public int getTabOrder() {
            return 20;
        }
    }

    private class WorldTypeTab implements IWorldTab {
        private GuiButton worldTypeButton;

        @Override
        public String getTabName() {
            return "World Type";
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            worldTypeButton = new GuiButton(300, width / 2 - 100, 80, 200, 20,
                    "World Type: " + settings.getTerrainType().getWorldTypeName());
            buttonList.add(worldTypeButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 100, 16, 16);

            drawCenteredString(fontRendererObj, "World Type Settings", width / 2, 60, 0xFFFFFF);
        }

        @Override
        public void actionPerformed(GuiButton button) throws IOException {
            if (button == worldTypeButton) {
                cycleWorldType();
                button.displayString = "World Type: " + worldSettings.getTerrainType().getWorldTypeName();
            }
        }

        private void cycleWorldType() {
            // 简化的世界类型循环
            WorldType[] types = {WorldType.DEFAULT, WorldType.FLAT, WorldType.LARGE_BIOMES};
            int currentIndex = Arrays.asList(types).indexOf(worldSettings.getTerrainType());
            int nextIndex = (currentIndex + 1) % types.length;
            // 1.7.10正确方法：setTerrainType
            worldSettings.setTerrainType(types[nextIndex]);
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) throws IOException {
            // 空实现
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            // 空实现
        }

        @Override
        public WorldSettings applySettings(WorldSettings settings) {
            return settings;
        }

        @Override
        public int getTabOrder() {
            return 30;
        }
    }

    private class MoreOptionsTab implements IWorldTab {
        private GuiButton mapFeaturesButton;
        private GuiButton bonusChestButton;

        @Override
        public String getTabName() {
            return "More Options";
        }

        @Override
        public void initGui(GuiScreen parent, WorldSettings settings) {
            mapFeaturesButton = new GuiButton(400, width / 2 - 100, 80, 200, 20,
                    "Generate Structures: " + (mapFeaturesEnabled ? "ON" : "OFF"));
            bonusChestButton = new GuiButton(401, width / 2 - 100, 110, 200, 20,
                    "Bonus Chest: " + (bonusChestEnabled ? "ON" : "OFF"));
            buttonList.add(mapFeaturesButton);
            buttonList.add(bonusChestButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            mc.getTextureManager().bindTexture(TextureManager.OPTIONS_BG_DARK);
            drawModalRectWithCustomSizedTexture(width / 2 - 110, 70, 0, 0, 220, 100, 16, 16);

            drawCenteredString(fontRendererObj, "Additional Options", width / 2, 60, 0xFFFFFF);
        }

        @Override
        public void actionPerformed(GuiButton button) throws IOException {
            if (button == mapFeaturesButton) {
                mapFeaturesEnabled = !mapFeaturesEnabled;
                button.displayString = "Generate Structures: " + (mapFeaturesEnabled ? "ON" : "OFF");
            } else if (button == bonusChestButton) {
                bonusChestEnabled = !bonusChestEnabled;
                button.displayString = "Bonus Chest: " + (bonusChestEnabled ? "ON" : "OFF");
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) throws IOException {
            // 空实现
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            // 空实现
        }

        @Override
        public WorldSettings applySettings(WorldSettings settings) {
            // 1.7.10正确方法：setMapFeaturesEnabled
            settings.setMapFeaturesEnabled(mapFeaturesEnabled);
            settings.enableBonusChest(bonusChestEnabled);
            return settings;
        }

        @Override
        public int getTabOrder() {
            return 40;
        }
    }
}