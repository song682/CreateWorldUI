package decok.dfcdvadstf.createworldui.api.tab;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabManager {
    private final Map<Integer, Tab> tabs = new HashMap<>();
    private final List<GuiButton> buttonList;
    private final GuiCreateWorld parent;
    /**
     * <p>Accessor handle on the parent {@code GuiCreateWorld}. All state that vanilla already
     * stores on the screen (world name, seed, gamemode, etc.) is read/written straight through
     * this — no local copy, no bridge, no drift.</p>
     * <p>指向 parent {@code GuiCreateWorld} 的 accessor 句柄。原版已经在屏幕上存的状态
     * （世界名、种子、游戏模式等）——直接走这里读写，不再本地缓存、不再桥接、不会脱同步。</p>
     */
    private final IGuiCreateWorldAccess access;
    private int currentTabId = 100;
    private Tab currentTab;

    /**
     * <p>Difficulty is the odd one out — vanilla {@code GuiCreateWorld} has no difficulty field,
     * so we cache it locally and sync to {@code mc.gameSettings.difficulty} on every write.</p>
     * <p>难度是个特例——原版 {@code GuiCreateWorld} 根本没有难度字段，所以我们本地缓一份，
     * 每次写的时候同步到 {@code mc.gameSettings.difficulty}。</p>
     */
    private EnumDifficulty difficulty = EnumDifficulty.NORMAL;

    public TabManager(GuiCreateWorld parent, List<GuiButton> buttonList, int width, int height) {
        this.parent = parent;
        this.buttonList = buttonList;
        // Cast once and keep the handle — GuiCreateWorld implements this at runtime thanks to
        // the IGuiCreateWorldAccess mixin. All vanilla fields are read/written through this.
        // 一次 cast 拿到句柄——运行时 GuiCreateWorld 靠 IGuiCreateWorldAccess mixin 自动
        // implements 了这个接口。原版字段全部通过这个句柄读写。
        this.access = (IGuiCreateWorldAccess) (Object) parent;
        // Pull initial difficulty from game settings — vanilla doesn't store it on GuiCreateWorld.
        // 从游戏设置里拿初始难度——原版不把它存在 GuiCreateWorld 上。
        EnumDifficulty d = Minecraft.getMinecraft().gameSettings.difficulty;
        this.difficulty = d != null ? d : EnumDifficulty.NORMAL;

        // Freeze registry to prevent further registration
        // 冻结注册表，阻止后续注册
        if (!TabRegistry.isFrozen()) {
            TabRegistry.freeze();
        }

        // Create all registered tabs
        // 创建所有已注册的标签页
        for (TabRegistry.TabEntry entry : TabRegistry.getEntries()) {
            Tab tab = entry.factory.get();
            registerTab(tab);
        }

        // Initialize all tabs
        // 初始化所有标签页
        for (Tab tab : tabs.values()) {
            tab.initGui(this, width, height);
        }

        // Set current tab (default to first registered tab)
        // 设置当前标签页（默认为第一个注册的）
        List<Integer> sortedIds = getSortedTabIds();
        if (!sortedIds.isEmpty()) {
            currentTabId = sortedIds.get(0);
        }
        switchToTab(currentTabId);
    }

    // New method: Get the actual name used for world creation
    // 新增方法：获取用于创建世界的实际名称
    public String getWorldNameForCreation() {
        String current = access.createWorldUI$getWorldName();
        String trimmedName = current != null ? current.trim() : "";
        if (trimmedName.isEmpty() && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            return I18n.format("selectWorld.newWorld"); // Return default name / 返回默认名称
        }
        return trimmedName;
    }

    // Add a button to the Mixin's button list
    // 添加按钮到 Mixin 的按钮列表
    public void addButton(GuiButton button) {
        if (!buttonList.contains(button)) {
            buttonList.add(button);
        }
    }

    /**
     * <p>
     *     注册一个标签页<br>
     *     内部使用，外部模组应通过 {@link TabRegistry#registerTab} 注册
     * </p>
     * <p>
     *     Register a tab<br>
     *     For internal use; external mods should use {@link TabRegistry#registerTab}
     * </p>
     */
    public void registerTab(Tab tab) {
        tabs.put(tab.getTabId(), tab);
    }

    /**
     * <p>
     *     检查指定ID是否为标签页按钮ID<br>
     *     Check if the given ID belongs to a tab button
     * </p>
     */
    public boolean isTabButton(int id) {
        return tabs.containsKey(id);
    }

    /**
     * <p>
     *     获取按顺序排列的所有标签页ID<br>
     *     Get all tab IDs in sorted order
     * </p>
     */
    public List<Integer> getSortedTabIds() {
        List<Integer> ids = new ArrayList<>(tabs.keySet());
        Collections.sort(ids);
        return ids;
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

    /**
     * 重新初始化标签页，在窗口大小改变时调用以保持当前选中的标签页
     * @param width 新的窗口宽度
     * @param height 新的窗口高度
     */
    public void reinitializeTabs(int width, int height) {
        // 保存当前选中的标签页ID
        int savedTabId = currentTabId;

        // 重新初始化所有标签页
        for (Tab tab : tabs.values()) {
            tab.initGui(this, width, height);
        }

        // 恢复之前选中的标签页
        switchToTab(savedTabId);
    }

    // Getters and setters for shared state — all 8 below proxy straight to the vanilla
    // GuiCreateWorld fields via IGuiCreateWorldAccess. No local cache, no bridge.
    // 8 对 getter/setter ——通过 IGuiCreateWorldAccess 直接代理到原版 GuiCreateWorld
    // 的字段上。无本地缓存，无桥接。
    public String getWorldName() { return access.createWorldUI$getWorldName(); }
    public void setWorldName(String worldName) {
        access.createWorldUI$setWorldName(worldName);
        System.out.println("TabManager: World name set to: " + worldName);
    }

    public String getGameMode() { return access.createWorldUI$getGameMode(); }
    public void setGameMode(String gameMode) {
        access.createWorldUI$setGameMode(gameMode);
        System.out.println("TabManager: Game mode set to: " + gameMode);
    }

    public String getSeed() { return access.createWorldUI$getSeed(); }
    public void setSeed(String seed) {
        access.createWorldUI$setSeed(seed);
        System.out.println("TabManager: Seed set to: " + seed);
    }

    public int getWorldTypeIndex() { return access.createWorldUI$getWorldTypeIndex(); }
    public void setWorldTypeIndex(int index) {
        access.createWorldUI$setWorldTypeIndex(index);
        System.out.println("TabManager: World type index set to: " + index);
    }

    public boolean getGenerateStructures() { return access.createWorldUI$getGenerateStructures(); }
    public void setGenerateStructures(boolean value) {
        access.createWorldUI$setGenerateStructures(value);
        System.out.println("TabManager: Generate structures set to: " + value);
    }

    public boolean getBonusChest() { return access.createWorldUI$getBonusChest(); }
    public void setBonusChest(boolean value) {
        access.createWorldUI$setBonusChest(value);
        System.out.println("TabManager: Bonus chest set to: " + value);
    }

    public boolean getAllowCheats() { return access.createWorldUI$getAllowCheats(); }
    public void setAllowCheats(boolean value) {
        access.createWorldUI$setAllowCheats(value);
        System.out.println("TabManager: Allow cheats set to: " + value);
    }

    public boolean getHardcore() { return access.createWorldUI$getHardcore(); }
    public void setHardcore(boolean value) {
        access.createWorldUI$setHardcore(value);
        System.out.println("TabManager: Hardcore set to: " + value);
    }

    public EnumDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(EnumDifficulty difficulty) {
        this.difficulty = difficulty;
        System.out.println("TabManager: Difficulty set to: " + difficulty);
        Minecraft.getMinecraft().gameSettings.difficulty = difficulty;
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    /**
     * <p>
     *     获取指定ID的标签页在其排序位置中的索引<br>
     *     Get the sorted index of the tab with the given ID
     * </p>
     */
    public int getTabIndex(int tabId) {
        List<Integer> sortedIds = getSortedTabIds();
        return sortedIds.indexOf(tabId);
    }

    public GuiCreateWorld getParent() { return parent; }
    public int getCurrentTabId() { return currentTabId; }
    public int getTabCount() { return tabs.size(); }
    public Map<Integer, Tab> getAllTabs() { return tabs; }
}