package decok.dfcdvadstf.createworldui.api.tab;

import decok.dfcdvadstf.createworldui.tab.GameTab;
import decok.dfcdvadstf.createworldui.tab.MoreTab;
import decok.dfcdvadstf.createworldui.tab.WorldTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class TabManager {
    private final Map<Integer, Tab> tabs = new HashMap<>();
    private final List<GuiButton> buttonList;
    private final GuiCreateWorld parent;
    private int currentTabId = 100;
    private Tab currentTab;

    // 从原版界面共享的状态字段
    private String worldName = "";
    private String gameMode = "survival";
    private String seed = "";
    private int worldTypeIndex = 0;
    private boolean generateStructures = true;
    private boolean bonusChest = false;
    private boolean allowCheats = false;
    private boolean hardcore = false;
    private EnumDifficulty difficulty = EnumDifficulty.NORMAL;
    private static final String DEFAULT_WORLD_NAME = "New World";

    public TabManager(GuiCreateWorld parent, List<GuiButton> buttonList, int width, int height) {
        this.parent = parent;
        this.buttonList = buttonList;

        // 创建所有标签页
        registerTab(new GameTab());
        registerTab(new WorldTab());
        registerTab(new MoreTab());

        // 初始化所有标签页
        for (Tab tab : tabs.values()) {
            tab.initGui(this, width, height);
        }

        // 设置当前标签页
        switchToTab(currentTabId);
    }

    // 从Mixin直接设置初始状态的方法
    public void setInitialState(String worldName, String gameMode, String seed,
                                int worldTypeIndex, boolean generateStructures,
                                boolean bonusChest, boolean allowCheats,
                                boolean hardcore, EnumDifficulty difficulty) {
        System.out.println("TabManager: Setting initial state from Mixin");
        System.out.println("  World name: " + worldName);
        System.out.println("  Game mode: " + gameMode);
        System.out.println("  Seed: " + seed);

        this.worldName = worldName != null ? worldName : "";
        this.gameMode = gameMode != null ? gameMode : "survival";
        this.seed = seed != null ? seed : "";
        this.worldTypeIndex = worldTypeIndex;
        this.generateStructures = generateStructures;
        this.bonusChest = bonusChest;
        this.allowCheats = allowCheats;
        this.hardcore = hardcore;
        this.difficulty = difficulty != null ? difficulty : EnumDifficulty.NORMAL;

        // 更新游戏设置
        Minecraft.getMinecraft().gameSettings.difficulty = this.difficulty;
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    // 新增方法：获取用于创建世界的实际名称
    public String getWorldNameForCreation() {
        if (worldName == null || worldName.trim().isEmpty()) {
            return I18n.format("selectWorld.newWorld"); // 返回默认名称
        }
        return worldName.trim();
    }

    // 从TabManager获取状态回传给Mixin
    public void getCurrentState(String[] worldName, String[] gameMode, String[] seed,
                                int[] worldTypeIndex, boolean[] generateStructures,
                                boolean[] bonusChest, boolean[] allowCheats,
                                boolean[] hardcore, EnumDifficulty[] difficulty) {
        worldName[0] = this.worldName;
        gameMode[0] = this.gameMode;
        seed[0] = this.seed;
        worldTypeIndex[0] = this.worldTypeIndex;
        generateStructures[0] = this.generateStructures;
        bonusChest[0] = this.bonusChest;
        allowCheats[0] = this.allowCheats;
        hardcore[0] = this.hardcore;
        difficulty[0] = this.difficulty;
    }

    // 添加按钮到Mixin的按钮列表
    public void addButton(GuiButton button) {
        if (!buttonList.contains(button)) {
            buttonList.add(button);
        }
    }

    private void registerTab(Tab tab) {
        tabs.put(tab.getTabId(), tab);
    }

    public void switchToTab(int tabId) {
        // 隐藏当前标签页
        if (currentTab != null) {
            currentTab.setVisible(false);
        }

        // 显示新标签页
        currentTabId = tabId;
        currentTab = tabs.get(tabId);
        if (currentTab != null) {
            currentTab.setVisible(true);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (currentTab != null) {
            currentTab.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    public void actionPerformed(GuiButton button) {
        // 首先处理标签页切换
        if (button.id >= 100 && button.id <= 102) {
            switchToTab(button.id);
            return;
        }

        // 然后传递给当前标签页处理
        if (currentTab != null) {
            currentTab.actionPerformed(button);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (currentTab != null) {
            currentTab.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (currentTab != null) {
            currentTab.keyTyped(typedChar, keyCode);
        }
    }

    // Getters and setters for shared state
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) {
        this.worldName = worldName;
        System.out.println("TabManager: World name set to: " + worldName);
    }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
        System.out.println("TabManager: Game mode set to: " + gameMode);
    }

    public String getSeed() { return seed; }
    public void setSeed(String seed) {
        this.seed = seed;
        System.out.println("TabManager: Seed set to: " + seed);
    }

    public int getWorldTypeIndex() { return worldTypeIndex; }
    public void setWorldTypeIndex(int index) {
        this.worldTypeIndex = index;
        System.out.println("TabManager: World type index set to: " + index);
    }

    public boolean getGenerateStructures() { return generateStructures; }
    public void setGenerateStructures(boolean value) {
        this.generateStructures = value;
        System.out.println("TabManager: Generate structures set to: " + value);
    }

    public boolean getBonusChest() { return bonusChest; }
    public void setBonusChest(boolean value) {
        this.bonusChest = value;
        System.out.println("TabManager: Bonus chest set to: " + value);
    }

    public boolean getAllowCheats() { return allowCheats; }
    public void setAllowCheats(boolean value) {
        this.allowCheats = value;
        System.out.println("TabManager: Allow cheats set to: " + value);
    }

    public boolean getHardcore() { return hardcore; }
    public void setHardcore(boolean value) {
        this.hardcore = value;
        System.out.println("TabManager: Hardcore set to: " + value);
    }

    public EnumDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(EnumDifficulty difficulty) {
        this.difficulty = difficulty;
        System.out.println("TabManager: Difficulty set to: " + difficulty);
        Minecraft.getMinecraft().gameSettings.difficulty = difficulty;
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    public GuiCreateWorld getParent() { return parent; }
    public int getCurrentTabId() { return currentTabId; }
}