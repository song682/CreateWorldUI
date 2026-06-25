package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.layouts.HorizontalLayout;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.catframe.ui.tab.Tab;
import decok.dfcdvadstf.catframe.ui.tab.TabBar;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.Tags;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import decok.dfcdvadstf.createworldui.tab.CreateWorldUITabBar;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * <p>Transforms the vanilla world creation screen via Mixin to implement a tabbed layout.</p>
 * <p>Note: vanilla's private fields are exposed via the separate
 * {@code IGuiCreateWorldAccess} accessor mixin — this class focuses purely on injecting
 * tab-related logic, while field access lives in its own place.</p>
 * <p>通过Mixin技术改造原版创建世界界面，实现标签页式布局。</p>
 * <p>注：原版私有字段的外部访问由独立的 {@code IGuiCreateWorldAccess} accessor mixin 提供——
 * 本类专注于注入 tab 相关逻辑，字段访问的责任排到另一处了。</p>
 */
@SuppressWarnings("unchecked")
@Mixin(GuiCreateWorld.class)
public abstract class MixinModernCreateWorld extends GuiScreen {

    // 原版字段
    @Shadow
    private GuiScreen field_146332_f;
    @Shadow
    private String field_146330_J;
    @Shadow
    private String field_146342_r;
    @Shadow
    private String field_146329_I;
    @Shadow
    private int field_146331_K;

    // 新添加的字段
    @Unique
    private TabManager modernWorldCreatingUI$tabManager;
    @Unique
    private TabBar modernWorldCreatingUI$tabBar;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private final Map<Integer, String> modernWorldCreatingUI$hoverTexts = new HashMap<>();
    @Unique
    private boolean modernWorldCreatingUI$isInitialized = false;
    @Unique
    private static final Logger modernWorldCreatingUI$logger = LogManager.getLogger("MixinGuiCreateWorld");
    @Unique
    private final IGuiCreateWorldAccess modernWorldCreatingUI$accessor = (IGuiCreateWorldAccess) this;
    
    // HeaderFooterLayout 主布局容器
    @Unique
    private HeaderFooterLayout modernWorldCreatingUI$mainLayout;
    @Unique
    private HorizontalLayout modernWorldCreatingUI$footerButtonLayout;
    @Unique
    private GuiButtonAdapter modernWorldCreatingUI$createButtonAdapter;
    @Unique
    private GuiButtonAdapter modernWorldCreatingUI$cancelButtonAdapter;

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
        GuiButton createButton = null;
        GuiButton cancelButton = null;
        
        for (GuiButton button : (List<GuiButton>)this.buttonList) {
            if (button.id == 0) {
                createButton = button;
                essentialButtons.add(button);
            } else if (button.id == 1) {
                cancelButton = button;
                essentialButtons.add(button);
            }
        }

        this.buttonList.clear();
        this.buttonList.addAll(essentialButtons);

        // ===== 使用 HeaderFooterLayout 作为主布局 =====
        // ===== Use HeaderFooterLayout as main layout =====
        modernWorldCreatingUI$mainLayout = new HeaderFooterLayout(true); // true = 绘制面板背景
        
        // Header 区域：TabBar (高度 = TAB_HEIGHT)
        // Header zone: TabBar
        modernWorldCreatingUI$mainLayout.setHeaderHeight(TAB_HEIGHT);
        
        // Footer 区域：底部按钮 (高度 30px)
        // Footer zone: bottom buttons (height 30px)
        modernWorldCreatingUI$mainLayout.setFooterHeight(30);
        
        // 创建 Footer 按钮布局
        // Create Footer button layout
        modernWorldCreatingUI$footerButtonLayout = new HorizontalLayout();
        modernWorldCreatingUI$footerButtonLayout.setSpacing(10); // 按钮间距
        
        // 使用 GuiButtonAdapter 包装原版按钮
        // Wrap vanilla buttons with GuiButtonAdapter
        if (createButton != null) {
            modernWorldCreatingUI$createButtonAdapter = new GuiButtonAdapter(createButton);
            modernWorldCreatingUI$footerButtonLayout.addChild(modernWorldCreatingUI$createButtonAdapter);
        }
        
        if (cancelButton != null) {
            modernWorldCreatingUI$cancelButtonAdapter = new GuiButtonAdapter(cancelButton);
            modernWorldCreatingUI$footerButtonLayout.addChild(modernWorldCreatingUI$cancelButtonAdapter);
        }
        
        // 将按钮布局设置到 Footer 区域
        // Set button layout to Footer zone
        modernWorldCreatingUI$mainLayout.setFooter(modernWorldCreatingUI$footerButtonLayout);

        // Check whether this is a re-init triggered by resize (TabManager already exists)
        // 检查是否是 resize 导致的重新初始化（TabManager 已存在）
        if (modernWorldCreatingUI$tabManager != null) {
            // resize case: reinitialize tabs in TabManager without creating a new one
            // resize 情况：重新初始化 TabManager 中的 tabs，而不是创建新的 TabManager
            modernWorldCreatingUI$tabManager.reinitializeTabs(this.width, this.height);
            modernWorldCreatingUI$logger.info("Reinitialized tabs after resize");
            modernWorldCreatingUI$mainLayout.recalculate(this.width, this.height);
        } else {
            // First initialization: create the TabBar and a new TabManager
            // 首次初始化：创建 TabBar 与新的 TabManager
            modernWorldCreatingUI$tabBar = new CreateWorldUITabBar();
            modernWorldCreatingUI$tabManager = new TabManager(
                    this, this.buttonList, this.width, this.height,
                    modernWorldCreatingUI$tabBar
            );
        }

        // TabBar handles tab button creation and layout
        // TabBar 负责标签页按钮的创建和布局
        if (modernWorldCreatingUI$tabBar != null) {
            if (modernWorldCreatingUI$tabBar.getTabCount() == 0) {
                // First init: register all tabs into TabBar via Builder
                // 首次初始化：通过 Builder 将全部 Tab 注册到 TabBar
                TabBar.builder(modernWorldCreatingUI$tabManager, this.width)
                    .addAllFromManager()
                    .build(modernWorldCreatingUI$tabBar);
            } else {
                // Resize: recalculate nav element positions
                // 窗口大小改变：重新计算导航元素位置
                modernWorldCreatingUI$tabBar.setNavWidth(this.width);
            }
            
            // 将 TabBar 设置到 Header 区域
            // Set TabBar to Header zone
            modernWorldCreatingUI$mainLayout.setHeader(modernWorldCreatingUI$tabBar);
        }

        // ===== 所有内容配置完成后，重新计算整体布局 =====
        // ===== Recalculate layout after all content is configured =====
        modernWorldCreatingUI$mainLayout.recalculate(this.width, this.height);

        // Initialize hover texts
        // 初始化悬停文本
        modernWorldCreatingUI$initHoverTexts();

        modernWorldCreatingUI$isInitialized = true;

        // Set the tab content area so GridLayoutTab.doLayout positions correctly within the content panel.
        // Tab area: below the tab bar (TAB_HEIGHT), above the footer (height - 35).
        // 设置Tab内容区域，使 GridLayoutTab.doLayout 在内容面板内正确布局。
        // Tab区域：Tab栏下方(TAB_HEIGHT)到脚部上方(height - 35)。
        int tabAreaTop = TAB_HEIGHT;
        int tabAreaBottom = this.height - 35;
        modernWorldCreatingUI$tabManager.setTabArea(
            new ScreenRectangle(0, tabAreaTop, this.width, tabAreaBottom - tabAreaTop)
        );
    }

    /**
     * <p>Initialize hover texts for vanilla buttons.</p>
     * <p>为原版按钮初始化悬停提示文本。</p>
     */
    @Unique
    private void modernWorldCreatingUI$initHoverTexts() {
        modernWorldCreatingUI$hoverTexts.put(2, Text.translatableString(Tags.MODID,"createworldui.hover.gameMode.survival"));
        modernWorldCreatingUI$hoverTexts.put(4, Text.translatableString(Tags.MODID,"createworldui.hover.generateStructures"));
        modernWorldCreatingUI$hoverTexts.put(5, Text.translatableString(Tags.MODID,"createworldui.hover.worldType"));
        modernWorldCreatingUI$hoverTexts.put(6, Text.translatableString(Tags.MODID,"createworldui.hover.allowCheats"));
        modernWorldCreatingUI$hoverTexts.put(7, Text.translatableString(Tags.MODID,"createworldui.hover.bonusChest"));
        modernWorldCreatingUI$hoverTexts.put(8, Text.translatableString(Tags.MODID,"createworldui.hover.customize"));
        modernWorldCreatingUI$hoverTexts.put(9, Text.translatableString(Tags.MODID,"createworldui.hover.difficulty"));
        modernWorldCreatingUI$hoverTexts.put(200, Text.translatableString(Tags.MODID,"createworldui.hover.gameRuleEditor"));
    }

    /**
     * 确保字段不为null
     */
    @Unique
    private void modernWorldCreatingUI$ensureFieldsNotNull() {
        // Only set default world name if it's null or empty
        // 只在世界名称为 null 或空时设置默认值,避免覆盖外部模组设置的值
        if (this.field_146330_J == null || this.field_146330_J.isEmpty()) {
            this.field_146330_J = I18n.format("selectWorld.newWorld");
            modernWorldCreatingUI$logger.info("Set default world name: " + this.field_146330_J);
        }

        if (this.field_146329_I == null) {
            this.field_146329_I = "";
        }
        if (this.field_146342_r == null) {
            this.field_146342_r = "survival";
        }

        // Validate world type index — only reset if truly invalid
        // 验证世界类型索引——只在真正无效时才重置
        // This preserves indices set by DefaultWorldGenerator or other mods
        // 这样能保留 DefaultWorldGenerator 或其他模组设置的索引
        if (WorldType.worldTypes == null || this.field_146331_K < 0) {
            this.field_146331_K = 0;
        } else if (this.field_146331_K >= WorldType.worldTypes.length ||
                WorldType.worldTypes[this.field_146331_K] == null) {
            // Index out of range or points to null — find a valid default
            // 索引超出范围或指向 null——找一个有效的默认值
            modernWorldCreatingUI$logger.warn("Invalid world type index: " + this.field_146331_K + ", resetting to 0");
            this.field_146331_K = 0;
        }
    }

    /**
     * Draws the screen
     * 绘制屏幕
     */
    @Inject(method = {"drawScreen"}, at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {

        ci.cancel();

        // 绘制主背景
        this.drawBackground(0);

        // ===== Footer 分隔线 + Panel 背景 =====
        // ===== Footer separator + Panel background =====
        if (modernWorldCreatingUI$mainLayout != null) {
            int footerY = this.height - modernWorldCreatingUI$mainLayout.getFooterHeight();
            // 画 Footer 分隔线
            ContentPanelRenderer.drawFooterSeparator(0, footerY, this.width);
            // 画 Panel 背景（TabBar 下方到 Footer 上方）
            ContentPanelRenderer.drawPanelBackground(0, TAB_HEIGHT, this.width, footerY - TAB_HEIGHT);
        }

        // 绘制 TabBar 导航按钮（Tab 按钮）
        // Draw TabBar nav buttons (tab buttons)
        if (modernWorldCreatingUI$tabBar != null && modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabBar.drawNavButtons(mouseX, mouseY, partialTicks, modernWorldCreatingUI$tabManager);
        }

        // 绘制当前标签页内容
        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.drawScreen(mouseX, mouseY, partialTicks);
        }

        // ===== 绘制 Footer 区域的按钮 =====
        // ===== Draw buttons in Footer zone =====
        if (modernWorldCreatingUI$footerButtonLayout != null) {
            for (decok.dfcdvadstf.catframe.ui.layouts.ILayout child : modernWorldCreatingUI$footerButtonLayout.getChildren()) {
                if (child instanceof GuiButtonAdapter) {
                    ((GuiButtonAdapter) child).render(mouseX, mouseY, partialTicks);
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

        // Only Create (0) and Cancel (1) are in buttonList; let vanilla handle them
        // buttonList 中只有创建(0)和取消(1)按钮，交给原版处理
        if (button.id == 0 || button.id == 1) {
            return;
        }

        // All other buttons are managed by Tab system components
        // 所有其他按钮由 Tab 系统组件管理
        ci.cancel();
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
        // Set pending difficulty from the UI-selected value, which will be applied by
        // MixinIntegratedServer.loadAllWorlds TAIL
        // 从 UI 选中的难度设置待应用难度，将由 MixinIntegratedServer.loadAllWorlds TAIL 应用
        DifficultyApplier.setPendingDifficulty(DifficultyApplier.getSelectedDifficulty());
    }

    /**
     * <p>Handles keyboard input via @Inject — intercepts Ctrl+Tab / Ctrl+Number for tab switching,
     * then delegates char input to {@link TabBar}, and finally cancels the vanilla method to prevent
     * the original keyTyped from feeding keys into its hardcoded text fields.</p>
     * <p>通过 @Inject 处理按键输入——拦截 Ctrl+Tab / Ctrl+数字进行 Tab 切换，
     * 随后把字符输入交给 {@link TabBar}，最后取消原版方法，防止按键被送进原版硬编码位置的输入框。</p>
     */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernWorldCreatingUI$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return; // Not initialized — let vanilla handle it / 未初始化，让原版自己处理
        }

        // Delegate Ctrl+Tab / Ctrl+digit tab switching to TabBar
        // 将 Ctrl+Tab / Ctrl+数字 的 Tab 切换委托给 TabBar
        if (modernWorldCreatingUI$tabBar != null && modernWorldCreatingUI$tabManager != null) {
            if (modernWorldCreatingUI$tabBar.keyPressedNav(keyCode, isCtrlKeyDown(), isShiftKeyDown(), modernWorldCreatingUI$tabManager)) {
                ci.cancel();
                return;
            }
        }

        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.keyTyped(typedChar, keyCode);
        }

        // 更新创建按钮状态
        GuiButton createButton = modernWorldCreatingUI$getButtonById(0);
        if (createButton != null) {
            createButton.enabled = modernWorldCreatingUI$tabManager != null &&
                    !modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName().trim().isEmpty();
        }

        // Manually handle ESC since we are about to cancel vanilla keyTyped
        // 手动处理 ESC，因为下面要取消原版 keyTyped
        if (keyCode == 1) {
            this.mc.displayGuiScreen(field_146332_f);
        }

        // Always cancel vanilla — otherwise keys would flow into vanilla's hardcoded text fields
        // 始终取消原版——否则按键会流进原版硬编码位置的输入框
        ci.cancel();
    }

    /**
     * <p>Handles mouse clicks via @Inject TAIL — runs after vanilla's own mouseClicked
     * (which dispatches to vanilla button list via super.mouseClicked internally),
     * then hands off to TabManager for tab-content click handling.</p>
     * <p>通过 @Inject TAIL 处理鼠标点击——在原版自己的 mouseClicked 执行完之后
     * （它内部会调 super.mouseClicked 派发给按钮列表），再把事件交给 TabManager 处理 tab 内容。</p>
     */
    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void modernWorldCreatingUI$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return;
        }

        // Delegate tab button clicks to TabBar (e.g. switching tabs)
        // 将 Tab 按钮点击委托给 TabBar（如切换标签页）
        if (modernWorldCreatingUI$tabBar != null && modernWorldCreatingUI$tabManager != null) {
            if (modernWorldCreatingUI$tabBar.mouseClickedNav(mouseX, mouseY, mouseButton, modernWorldCreatingUI$tabManager)) {
                return; // Handled by TabBar / 由 TabBar 处理
            }
        }

        // ===== 处理 Footer 按钮点击 =====
        // ===== Handle Footer button clicks =====
        if (modernWorldCreatingUI$footerButtonLayout != null) {
            for (decok.dfcdvadstf.catframe.ui.layouts.ILayout child : modernWorldCreatingUI$footerButtonLayout.getChildren()) {
                if (child instanceof GuiButtonAdapter) {
                    ((GuiButtonAdapter) child).mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }

        // Forward to current tab for content click handling
        // 转发到当前 Tab 以处理内容点击
        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * <p>Handles mouse scroll.</p>
     * <p>NOTE: This is kept as a plain @Override rather than a Mixin @Inject because
     * vanilla GuiCreateWorld does not override handleMouseInput — it inherits from GuiScreen.
     * Mixin cannot reliably inject into inherited methods, so @Override is the pragmatic choice.</p>
     * <p>处理鼠标滚动。</p>
     * <p>注意：这里保留普通 @Override 而不是 Mixin @Inject——因为原版 GuiCreateWorld
     * 没有重写 handleMouseInput，它是从 GuiScreen 继承的。Mixin 无法稳定注入继承方法，
     * 所以 @Override 是更务实的选择。</p>
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
            Tab currentTab = modernWorldCreatingUI$tabManager.getCurrentTab();
            if (currentTab != null) {
                currentTab.visitComponents(comp -> {
                    // Handle scroll wheel for CyclingButton components
                    if (comp instanceof CyclingButton && comp.isVisible() && comp.isActive()
                            && comp.isMouseOver(mouseX, mouseY)) {
                        int delta = Mouse.getEventDWheel();
                        if (delta != 0) {
                            comp.mouseScrolled(delta);
                        }
                    }
                });
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

                    // 跳过创建和取消按钮
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
            String worldName = modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName();
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
}