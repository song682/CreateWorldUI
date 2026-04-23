package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.GuiCyclableButton;
import decok.dfcdvadstf.createworldui.api.gamerule.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.api.tab.TabState;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * <p>Transforms the vanilla world creation screen via Mixin to implement a tabbed layout.</p>
 * <p>通过Mixin技术改造原版创建世界界面，实现标签页式布局</p>
 */
@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class MixinModernCreateWorld extends GuiScreen {

    // 原版字段
    @Shadow
    private GuiScreen field_146332_f;
    @Shadow
    private boolean field_146337_w;
    @Shadow
    private String field_146330_J;
    @Shadow
    private String field_146342_r;
    @Shadow
    private String field_146329_I;
    @Shadow
    private boolean field_146341_s;
    @Shadow
    private boolean field_146338_v;
    @Shadow
    private boolean field_146340_t;
    @Shadow
    private int field_146331_K;

    // 新添加的字段
    @Unique
    private TabManager modernWorldCreatingUI$tabManager;
    @Unique
    private static final ResourceLocation OPTIONS_BG_DARK = new ResourceLocation("createworldui","textures/gui/options_background_dark.png");
    @Unique
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("createworldui","textures/gui/tabs.png");
    @Unique
    private static final int TAB_WIDTH = 130;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private final Map<Integer, String> modernWorldCreatingUI$hoverTexts = new HashMap<>();
    @Unique
    private boolean modernWorldCreatingUI$isInitialized = false;
    @Unique
    private int modernWorldCreatingUI$tabButtonWidth = TAB_WIDTH;
    @Unique
    private static final Logger modernWorldCreatingUI$logger = LogManager.getLogger("MixinGuiCreateWorld");

    /**
     * 检查是否按下了Shift键
     */
    @Unique
    /**
     * 初始化
     */
    @Inject(method = "initGui", at = @At("HEAD"))
    private void onInitGuiHead(CallbackInfo ci) {
        modernWorldCreatingUI$ensureFieldsNotNull();
        modernWorldCreatingUI$isInitialized = false;
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGuiTail(CallbackInfo ci) {
        modernWorldCreatingUI$logger.info("Initializing GUI");

        // Clear button list, but keep the Create and Cancel buttons
        // 首先清空按钮列表，但保留创建和取消按钮
        List<GuiButton> essentialButtons = new ArrayList<>();
        for (GuiButton button : (List<GuiButton>)this.buttonList) {
            if (button.id == 0 || button.id == 1) {
                essentialButtons.add(button);
            }
        }

        this.buttonList.clear();
        this.buttonList.addAll(essentialButtons);

        // Check whether this is a re-init triggered by resize (TabManager already exists)
        // 检查是否是 resize 导致的重新初始化（TabManager 已存在）
        if (modernWorldCreatingUI$tabManager != null) {
            // resize case: reinitialize tabs in TabManager without creating a new one
            // resize 情况：重新初始化 TabManager 中的 tabs，而不是创建新的 TabManager
            modernWorldCreatingUI$tabManager.reinitializeTabs(this.width, this.height);
            modernWorldCreatingUI$logger.info("Reinitialized tabs after resize");
        } else {
            // First initialization: create a new TabManager
            // 首次初始化：创建新的 TabManager
            modernWorldCreatingUI$tabManager = new TabManager(
                    (GuiCreateWorld)(Object)this, this.buttonList, this.width, this.height
            );
            // Pass vanilla state to TabManager
            // 将原版状态传递给 TabManager
            modernWorldCreatingUI$sendStateToTabManager();
        }

        // Create tab buttons (need to recreate on resize as button positions may change)
        // 创建标签页按钮（resize 时需要重新创建，因为按钮位置可能改变）
        modernWorldCreatingUI$createTabButtons();
        modernWorldCreatingUI$repositionActionButtons();

        // Initialize hover texts
        // 初始化悬停文本
        modernWorldCreatingUI$initHoverTexts();

        modernWorldCreatingUI$isInitialized = true;
    }

    /**
     * Synchronize vanilla state to TabManager
     * 同步原版状态到 TabManager
     */
    @Unique
    private void modernWorldCreatingUI$sendStateToTabManager() {
        if (modernWorldCreatingUI$tabManager == null) {
            return;
        }

        modernWorldCreatingUI$logger.info("MixinModernCreateWorld: Passing state to TabManager");

        // 获取当前游戏设置中的难度
        EnumDifficulty currentDifficulty = mc.gameSettings.difficulty;
        if (currentDifficulty == null) {
            currentDifficulty = EnumDifficulty.NORMAL;
        }

        modernWorldCreatingUI$tabManager.setInitialState(
                field_146330_J,      // 世界名称
                field_146342_r,      // 游戏模式
                field_146329_I,      // 种子
                field_146331_K,      // 世界类型索引
                field_146341_s,      // 生成建筑
                field_146338_v,      // 奖励箱
                field_146340_t,      // 允许作弊
                field_146337_w,      // 硬核模式
                currentDifficulty    // 难度
        );
    }

    /**
     * Synchronize TabManager state back to vanilla fields
     * 同步 TabManager 状态到原版字段
     */
    @Unique
    private void modernWorldCreatingUI$getStateFromTabManager() {
        if (modernWorldCreatingUI$tabManager != null) {
            field_146330_J = modernWorldCreatingUI$tabManager.getWorldName();
            field_146342_r = modernWorldCreatingUI$tabManager.getGameMode();
            field_146329_I = modernWorldCreatingUI$tabManager.getSeed();
            field_146331_K = modernWorldCreatingUI$tabManager.getWorldTypeIndex();
            field_146341_s = modernWorldCreatingUI$tabManager.getGenerateStructures();
            field_146338_v = modernWorldCreatingUI$tabManager.getBonusChest();
            field_146340_t = modernWorldCreatingUI$tabManager.getAllowCheats();
            field_146337_w = modernWorldCreatingUI$tabManager.getHardcore();
            field_146330_J = modernWorldCreatingUI$tabManager.getWorldName();
        }
    }

    @Unique
    private void modernWorldCreatingUI$initHoverTexts() {
        modernWorldCreatingUI$hoverTexts.put(2, I18n.format("createworldui.hover.gameMode"));
        modernWorldCreatingUI$hoverTexts.put(4, I18n.format("createworldui.hover.generateStructures"));
        modernWorldCreatingUI$hoverTexts.put(5, I18n.format("createworldui.hover.worldType"));
        modernWorldCreatingUI$hoverTexts.put(6, I18n.format("createworldui.hover.allowCheats"));
        modernWorldCreatingUI$hoverTexts.put(7, I18n.format("createworldui.hover.bonusChest"));
        modernWorldCreatingUI$hoverTexts.put(8, I18n.format("createworldui.hover.customize"));
        modernWorldCreatingUI$hoverTexts.put(9, I18n.format("createworldui.hover.difficulty"));
        modernWorldCreatingUI$hoverTexts.put(200, I18n.format("createworldui.hover.gameRuleEditor"));
    }

    /**
     * 确保字段不为null
     */
    @Unique
    private void modernWorldCreatingUI$ensureFieldsNotNull() {
        this.field_146330_J = I18n.format("selectWorld.newWorld");
        modernWorldCreatingUI$logger.info("Set default world name: " + this.field_146330_J);

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

    /**
     * Repositions the action buttons (Create and Cancel)
     * 重新定位操作按钮（创建和取消）
     */
    @Unique
    private void modernWorldCreatingUI$repositionActionButtons() {
        GuiButton createButton = modernWorldCreatingUI$getButtonById(0);
        GuiButton cancelButton = modernWorldCreatingUI$getButtonById(1);

        if (createButton != null) {
            createButton.xPosition = this.width / 2 - 155;
            createButton.yPosition = this.height - 28;
            createButton.width = 150;
            createButton.height = 20;
            createButton.visible = true;
        }

        if (cancelButton != null) {
            cancelButton.xPosition = this.width / 2 + 5;
            cancelButton.yPosition = this.height - 28;
            cancelButton.width = 150;
            cancelButton.height = 20;
            cancelButton.visible = true;
        }
    }

    /**
     * Creates the tab buttons dynamically based on registered tabs
     * 根据已注册的标签页动态创建标签页按钮
     */
    @Unique
    private void modernWorldCreatingUI$createTabButtons() {
        int tabCount = modernWorldCreatingUI$tabManager != null ? modernWorldCreatingUI$tabManager.getTabCount() : 3;
        if (tabCount <= 0) tabCount = 3;

        modernWorldCreatingUI$tabButtonWidth = Math.min(TAB_WIDTH, this.width / tabCount);
        int totalWidth = modernWorldCreatingUI$tabButtonWidth * tabCount;
        int startX = this.width / 2 - totalWidth / 2;

        if (modernWorldCreatingUI$tabManager != null) {
            java.util.List<Integer> sortedIds = modernWorldCreatingUI$tabManager.getSortedTabIds();
            for (int i = 0; i < sortedIds.size(); i++) {
                int tabId = sortedIds.get(i);
                decok.dfcdvadstf.createworldui.api.tab.Tab tab = modernWorldCreatingUI$tabManager.getAllTabs().get(tabId);
                String tabName = tab != null ? tab.getTabName() : "";
                int xPos = startX + i * modernWorldCreatingUI$tabButtonWidth;

                GuiButton tabButton = new GuiButton(tabId, xPos, 0, modernWorldCreatingUI$tabButtonWidth, TAB_HEIGHT, tabName) {
                    @Override
                    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                        if (this.visible) {
                            mc.getTextureManager().bindTexture(TABS_TEXTURE);
                            // Reset OpenGL color state to white to prevent texture tinting
                            // 重置OpenGL颜色状态为白色，防止纹理被着色
                            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                            boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                            boolean isSelected = modernWorldCreatingUI$tabManager != null &&
                                    modernWorldCreatingUI$tabManager.getCurrentTabId() == this.id;

                            TabState state = isSelected ?
                                    (isHovered ? TabState.SELECTED_HOVER : TabState.SELECTED) :
                                    (isHovered ? TabState.HOVER : TabState.NORMAL);

                            drawTexturedModalRect(this.xPosition, this.yPosition, state.u, state.v, this.width, TAB_HEIGHT);
                            drawCenteredString(mc.fontRenderer, this.displayString,
                                    this.xPosition + this.width / 2,
                                    this.yPosition + (this.height - 8) / 2, state.getTextColor());
                        }
                    }
                };
                tabButton.visible = true;
                this.buttonList.add(tabButton);
            }
        }
    }

    /**
     * Draws the screen
     * 绘制屏幕
     */
    @Inject(method = {"drawScreen"}, at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return;
        }

        ci.cancel();

        // 绘制主背景
        this.drawBackground(0);

        // 绘制顶部背景
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);
        this.modernWorldCreatingUI$drawTiledTexture(0, 0, this.width, TAB_HEIGHT - 2, 16, 16);

        // 绘制分隔线（选中Tab下方隐藏）
        // Draw separator lines (hidden under selected tab)
        int lineY = TAB_HEIGHT - 2; // 下移一格 / Move down one pixel
        int currentTabId = modernWorldCreatingUI$tabManager != null ?
                modernWorldCreatingUI$tabManager.getCurrentTabId() : -1;

        // Dynamically calculate tab layout based on registered tab count
        // 根据已注册标签页数量动态计算布局
        int tabCount = modernWorldCreatingUI$tabManager != null ? modernWorldCreatingUI$tabManager.getTabCount() : 3;
        if (tabCount <= 0) tabCount = 3;
        int actualTabWidth = Math.min(TAB_WIDTH, this.width / tabCount);
        int totalWidth = actualTabWidth * tabCount;
        int startX = this.width / 2 - totalWidth / 2;
        int tabIndex = modernWorldCreatingUI$tabManager != null ?
                modernWorldCreatingUI$tabManager.getTabIndex(currentTabId) : -1;

        // 颜色：顶部黑色75%，底部白色20%
        // Colors: top black 75%, bottom white 20%
        int lineTopColor = 0xC0000000;
        int lineBottomColor = 0x33FFFFFF;

        if (tabIndex >= 0 && tabIndex < tabCount) {
            // 选中的Tab位置
            // Position of selected tab
            int selectedTabX = startX + tabIndex * actualTabWidth;
            int selectedTabEnd = selectedTabX + actualTabWidth;

            // 绘制选中Tab左侧的分隔线
            // Draw separator line left of selected tab
            if (selectedTabX > 0) {
                modernWorldCreatingUI$drawColoredLine(0, lineY, selectedTabX, lineTopColor, lineBottomColor);
            }
            // 绘制选中Tab右侧的分隔线
            // Draw separator line right of selected tab
            if (selectedTabEnd < this.width) {
                modernWorldCreatingUI$drawColoredLine(selectedTabEnd, lineY, this.width - selectedTabEnd, lineTopColor, lineBottomColor);
            }
        } else {
            // 没有选中的Tab，绘制完整分隔线
            // No selected tab, draw full separator line
            modernWorldCreatingUI$drawColoredLine(0, lineY, this.width, lineTopColor, lineBottomColor);
        }
        modernWorldCreatingUI$drawColoredLine(0, this.height - 35, this.width, lineTopColor, lineBottomColor);

        // 绘制当前标签页内容
        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.drawScreen(mouseX, mouseY, partialTicks);
        }

        // 绘制所有按钮
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.visible) {
                    button.drawButton(this.mc, mouseX, mouseY);
                }
            }
        }

        // 绘制悬停文本
        modernWorldCreatingUI$drawHoverText(mouseX, mouseY);
    }

    /**
     * Handles button click events
     * 处理按钮点击
     */
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized || button == null) {
            return;
        }

        // Handle Create button - sync state before creating world
        // 处理创建按钮 - 在创建前同步状态
        if (button.id == 0) {
            modernWorldCreatingUI$getStateFromTabManager();
            // 让原版继续处理创建逻辑
            return;
        }

        // 首先处理标签页管理器的事件
        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.actionPerformed(button);

            // If it's a tab switch button, cancel further processing
            // 如果是标签页切换按钮，取消后续处理
            if (modernWorldCreatingUI$tabManager.isTabButton(button.id)) {
                ci.cancel();
                return;
            }
        }

        // 处理游戏规则编辑器按钮
        if (button.id == 200) {
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null) pending = new HashMap<>();

            // 过滤掉 null 值
            Map<String, String> cleanPending = new HashMap<>();
            for (Map.Entry<String, String> entry : pending.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    cleanPending.put(entry.getKey(), entry.getValue());
                }
            }

            try {
                Minecraft mc = Minecraft.getMinecraft();
                net.minecraft.world.World clientWorld = mc != null ? mc.theWorld : null;
                if (clientWorld != null) {
                    Map<String, Object> opt = GameRuleMonitorNSetter.getOptimalGameruleValues(clientWorld);
                    if (opt != null && !opt.isEmpty()) {
                        for (Map.Entry<String, Object> e : opt.entrySet()) {
                            if (e.getKey() != null && e.getValue() != null) {
                                cleanPending.put(e.getKey(), String.valueOf(e.getValue()));
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                modernWorldCreatingUI$logger.error("On opening GameRuleEditor, an error occoured is: ", t.getMessage());
            }

            this.mc.displayGuiScreen(new GuiScreenGameRuleEditor((GuiCreateWorld)(Object)this, cleanPending));
            ci.cancel();
            return;
        }

        // Other buttons are handled by TabManager; prevent vanilla processing
        // 其他按钮由标签页管理器处理，阻止原版处理
        if (button.id >= 2 && button.id <= 9) {
            ci.cancel();
        }
    }

    /**
     * Post-world launch processing
     * 世界启动后处理
     */
    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;launchIntegratedServer(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void modernWorldCreatingUI$afterLaunchWorld(GuiButton button, CallbackInfo ci) {
        // 同步TabManager状态到原版字段
        modernWorldCreatingUI$getStateFromTabManager();

        if (modernWorldCreatingUI$tabManager == null) {
            return;
        }

        // Set pending difficulty, which will be applied by MixinIntegratedServer.loadAllWorlds TAIL
        // No reflection needed - MixinIntegratedServer directly accesses worldServers after initialization
        //
        // 设置待应用的难度，将由MixinIntegratedServer.loadAllWorlds TAIL处应用
        // 无需反射 - MixinIntegratedServer在worldServers初始化后直接访问
        DifficultyApplier.setPendingDifficulty(modernWorldCreatingUI$tabManager.getDifficulty());
    }

    /**
     * Handles keyboard input
     * 处理按键输入
     */    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!modernWorldCreatingUI$isInitialized) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        // Handle Control + Tab and Control + Shift + Tab to switch tabs
        // 处理 Control + Tab 和 Control + Shift + Tab 切换 Tab
        if (isCtrlKeyDown() && keyCode == 15) { // Tab键的键码是15
            if (modernWorldCreatingUI$tabManager != null) {
                java.util.Map<Integer, ?> availableTabs = modernWorldCreatingUI$tabManager.getAllTabs();
                java.util.List<Integer> sortedTabIds = new java.util.ArrayList<>(availableTabs.keySet());
                java.util.Collections.sort(sortedTabIds);

                if (!sortedTabIds.isEmpty()) {
                    int currentTabId = modernWorldCreatingUI$tabManager.getCurrentTabId();
                    int currentIndex = sortedTabIds.indexOf(currentTabId);

                    int nextIndex;
                    if (isShiftKeyDown()) {
                        // Control + Shift + Tab: switch left (cycle)
                        // Control + Shift + Tab: 向左切换 (循环)
                        nextIndex = (currentIndex - 1 + sortedTabIds.size()) % sortedTabIds.size();
                    } else {
                        // Control + Tab: switch right (cycle)
                        // Control + Tab: 向右切换 (循环)
                        nextIndex = (currentIndex + 1) % sortedTabIds.size();
                    }

                    int targetTabId = sortedTabIds.get(nextIndex);
                    modernWorldCreatingUI$tabManager.switchToTab(targetTabId);
                }
            }
            return; // 拦截按键，不继续处理
        }

        // Handle Control/Command + number keys to switch tabs
        // 处理 Control/Command + 数字键切换 Tab
        if (isCtrlKeyDown()) {  // 使用Minecraft内置的isCtrlKeyDown方法，该方法已处理Mac和Windows/Linux的差异
            // Handle number keys 1-9 and 0 (0 is usually at position 10)
            // 处理数字键 1-9 和 0 (0 通常在位置10)
            if (keyCode >= 2 && keyCode <= 11) { // 键盘上的1-9,0键
                int tabNumber = keyCode - 1; // 键码2对应数字1，码3对应数字2，以此类推
                if (keyCode == 11) { // 数字0键
                    tabNumber = 10;
                }

                // 获取所有可用的标签页ID并排序
                if (modernWorldCreatingUI$tabManager != null) {
                    java.util.Map<Integer, ?> availableTabs = modernWorldCreatingUI$tabManager.getAllTabs();
                    java.util.List<Integer> sortedTabIds = new java.util.ArrayList<>(availableTabs.keySet());
                    java.util.Collections.sort(sortedTabIds);

                    // 确保索引不超出范围（Fallback到最大可用Tab）
                    int targetIndex = Math.min(tabNumber - 1, sortedTabIds.size() - 1);
                    if (targetIndex >= 0 && targetIndex < sortedTabIds.size()) {
                        int targetTabId = sortedTabIds.get(targetIndex);
                        modernWorldCreatingUI$tabManager.switchToTab(targetTabId);
                    }
                }
                return; // 拦截按键，不继续处理
            }
        }

        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.keyTyped(typedChar, keyCode);
        }

        // 更新创建按钮状态
        GuiButton createButton = modernWorldCreatingUI$getButtonById(0);
        if (createButton != null) {
            createButton.enabled = modernWorldCreatingUI$tabManager != null &&
                    !modernWorldCreatingUI$tabManager.getWorldName().trim().isEmpty();
        }

        if (keyCode == 1) {
            this.mc.displayGuiScreen(field_146332_f);
        }
    }

    /**
     * Handles mouse clicks
     * 处理鼠标点击
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!modernWorldCreatingUI$isInitialized) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Handles mouse scroll
     * 处理鼠标滚动
     */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        if (!modernWorldCreatingUI$isInitialized) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (modernWorldCreatingUI$tabManager != null) {
            // 遍历当前标签页的按钮，查找是否有GuiCyclableButton需要处理滚动事件
            for (Object obj : this.buttonList) {
                if (obj instanceof GuiCyclableButton) {
                    GuiCyclableButton button = (GuiCyclableButton) obj;
                    if (button.visible && button.enabled &&
                            mouseX >= button.xPosition && mouseX < button.xPosition + button.width &&
                            mouseY >= button.yPosition && mouseY < button.yPosition + button.height) {
                        int delta = Mouse.getEventDWheel();
                        if (delta != 0) {
                            button.mouseScrolled(delta);
                        }
                    }
                }
            }
        }
    }

    /**
     * Draws hover text
     * 绘制悬停文本
     */
    @Unique
    private void modernWorldCreatingUI$drawHoverText(int mouseX, int mouseY) {
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.visible && mouseX >= button.xPosition && mouseY >= button.yPosition &&
                        mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height) {

                    // 跳过标签页按钮、创建和取消按钮
                    if (modernWorldCreatingUI$tabManager != null && modernWorldCreatingUI$tabManager.isTabButton(button.id)) continue;
                    if (button.id == 0 || button.id == 1) continue;

                    // 从Map中获取悬停文本
                    String hoverText = modernWorldCreatingUI$hoverTexts.get(button.id);
                    if (hoverText != null && !hoverText.isEmpty()) {
                        this.drawHoveringText(Arrays.asList(hoverText), mouseX, mouseY, this.fontRendererObj);
                        return;
                    }
                }
            }
        }

        // 检查世界名称输入框的悬停提示
        if (modernWorldCreatingUI$tabManager != null &&
                modernWorldCreatingUI$tabManager.getCurrentTabId() == 100) {
            String worldName = modernWorldCreatingUI$tabManager.getWorldName();
            String hoverText;
            if (worldName == null || worldName.isEmpty()) {
                hoverText = I18n.format("createworldui.hover.worldName.empty");
            } else {
                hoverText = I18n.format("createworldui.hover.worldName.filled", worldName);
            }

            // 检查鼠标是否在世界名称输入框区域
            int inputX = this.width / 2 - 104;
            int inputY = this.height / 5;
            if (mouseX >= inputX && mouseX <= inputX + 208 &&
                    mouseY >= inputY && mouseY <= inputY + 20) {
                this.drawHoveringText(Arrays.asList(hoverText), mouseX, mouseY, this.fontRendererObj);
            }
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
     * Draws a tiled texture
     * 绘制平铺纹理
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
     * Draws a colored line
     * 绘制彩色线条
     */
    @Unique
    private void modernWorldCreatingUI$drawColoredLine(int x, int y, int width, int topColor, int bottomColor){
        // 绘制上半像素
        drawRect(x, y, x + width, y + 1, topColor);
        // 绘制下半像素
        drawRect(x, y + 1, x + width, y + 2, bottomColor);
    }

    // 保留原版方法
    @Shadow
    private void func_146314_g() {}
    @Shadow
    private void func_146319_h() {}
}