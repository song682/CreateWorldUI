package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.components.events.CatFrameInputScreen;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.Tab;
import decok.dfcdvadstf.catframe.ui.components.tab.TabManager;
import decok.dfcdvadstf.catframe.ui.components.tab.TabNavigationBar;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.layouts.HorizontalLayout;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import decok.dfcdvadstf.createworldui.ui.TabScreenRoot;
import decok.dfcdvadstf.createworldui.ui.tab.CreateWorldTab;
import decok.dfcdvadstf.createworldui.ui.tab.GameTab;
import decok.dfcdvadstf.createworldui.ui.tab.MoreTab;
import decok.dfcdvadstf.createworldui.ui.tab.WorldTab;
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
public abstract class MixinModernCreateWorld extends GuiScreen implements CatFrameInputScreen {

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
    private TabNavigationBar modernWorldCreatingUI$tabBar;
    @Unique
    private TabScreenRoot modernWorldCreatingUI$tabRoot;
    @Unique
    private static final int TAB_HEIGHT = 24;
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

        // ===== 组件系统 Tab 装配（对齐高版本 CreateWorldScreen.init()） =====
        // ===== Component-system tab assembly (mirrors high-version CreateWorldScreen.init()) =====
        // Drive the whole tab stack rebuild from initGui every pass — the same shape as
        // the high-version init(): a fresh navigation bar + fresh tabs, with the old root
        // discarded wholesale (the 1.7.10 counterpart of vanilla Screen's widget reset),
        // so no stale components can survive a resize or a sub-screen round-trip.
        // 每次 initGui 都重建整套 Tab 栈：新导航栏 + 新 Tab，旧 root 整体丢弃
        // （这是原版 Screen 控件重置机制在 1.7.10 下的等价物），drop 掉 resize 或
        // 子界面往返后的任何旧组件残留。
        modernWorldCreatingUI$tabRoot = new TabScreenRoot();
        modernWorldCreatingUI$tabManager = new TabManager(
                modernWorldCreatingUI$tabRoot::addChild,
                modernWorldCreatingUI$tabRoot::removeChild);
        // Cast through Object: this mixin merges into GuiCreateWorld at runtime, so
        // `this` IS the screen instance — the cast only satisfies the compile-time view.
        // 经 Object 转型：本 Mixin 运行时合并进 GuiCreateWorld，`this` 就是屏幕实例，
        // 强转仅为满足编译期类型视角。
        GuiCreateWorld screen = (GuiCreateWorld) (Object) this;
        GameTab gameTab = new GameTab(screen);
        modernWorldCreatingUI$tabBar = TabNavigationBar.builder(modernWorldCreatingUI$tabManager, this.width)
                .addTabs(gameTab, new WorldTab(screen), new MoreTab(screen))
                .build();

        // 将导航栏设置到 Header 区域，并作为根容器的第一个子组件（点击/焦点链路）
        // Set the nav bar to the Header zone and register it as the root's first child
        // (click / focus chain)
        modernWorldCreatingUI$mainLayout.setHeader(modernWorldCreatingUI$tabBar);
        modernWorldCreatingUI$tabRoot.addChild(modernWorldCreatingUI$tabBar);

        // ===== 布局、内容区域与初始选中（对齐高版本 selectTab(0) + repositionElements） =====
        // ===== Layout, tab area and initial selection (mirrors the high-version pair) =====
        modernWorldCreatingUI$tabBar.updateWidth(this.width);
        // ===== 所有内容配置完成后，重新计算整体布局 =====
        // ===== Recalculate layout after all content is configured =====
        modernWorldCreatingUI$mainLayout.recalculate(this.width, this.height);
        // Set the tab content area so GridLayoutTab.doLayout positions correctly within the content panel.
        // Tab area: below the tab bar (TAB_HEIGHT), above the footer (height - 35).
        // 设置Tab内容区域，使 GridLayoutTab.doLayout 在内容面板内正确布局。
        // Tab区域：Tab栏下方(TAB_HEIGHT)到脚部上方(height - 35)。
        modernWorldCreatingUI$tabManager.setTabArea(modernWorldCreatingUI$tabArea());
        modernWorldCreatingUI$tabManager.setCurrentTab(gameTab, false);
        modernWorldCreatingUI$tabRoot.setFocused(gameTab.getInitialFocus());

        modernWorldCreatingUI$isInitialized = true;
        modernWorldCreatingUI$logger.info("Initialized tab component stack");
    }

    /**
     * 确保字段不为null
     */
    @Unique
    private void modernWorldCreatingUI$ensureFieldsNotNull() {
        // Only set the default world name when the field is null (never set) — an
        // empty string is a valid user state (cleared name) and must not be
        // overwritten, since initGui re-runs on resize and when returning from
        // sub-screens. This also avoids clobbering values set by external mods.
        // 只在世界名称为 null（从未设置）时设置默认值——空串是合法的用户状态
        // （用户清空了名字），不能覆盖；initGui 会在窗口缩放、从子界面返回时重跑，
        // 覆盖空串会抹掉用户意图。这同时也避免覆盖外部模组设置的值。
        if (this.field_146330_J == null) {
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

        // 当前标签页的每帧准备（动态状态、tooltip、标签覆盖层）
        // Per-frame preparation of the current tab (dynamic states, tooltips, label overlays)
        if (modernWorldCreatingUI$tabManager != null) {
            Tab currentTab = modernWorldCreatingUI$tabManager.getCurrentTab();
            if (currentTab instanceof CreateWorldTab) {
                ((CreateWorldTab) currentTab).prepareFrame();
            }
        }

        // 组件系统渲染：导航栏 + 当前标签页子组件（含 hover 刷新与延迟 tooltip 泵动）
        // Component-system rendering: nav bar + current tab children (hover refresh and
        // deferred tooltip pumping included)
        if (modernWorldCreatingUI$tabRoot != null) {
            modernWorldCreatingUI$tabRoot.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        // ===== 绘制 Footer 区域的按钮 =====
        // ===== Draw buttons in Footer zone =====
        if (modernWorldCreatingUI$footerButtonLayout != null) {
            for (decok.dfcdvadstf.catframe.ui.layouts.ILayout child : modernWorldCreatingUI$footerButtonLayout.getChildren()) {
                if (child instanceof GuiButtonAdapter) {
                    // CatFrame 0.7.1.1: rendering goes through the extractRenderState
                    // template method (visibility check + hover refresh + renderWidget).
                    // Adapter-level visibility is controlled via setVisible() and is not
                    // affected by delegate.visible=true inside renderWidget.
                    // 0.7.1.1 渲染统一走 extractRenderState 模板方法（可见性检查 + hover
                    // 刷新 + renderWidget 委托）；适配器可见性由 setVisible() 控制，
                    // 不受 renderWidget 内 delegate.visible=true 影响。
                    ((GuiButtonAdapter) child).extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
                }
            }
        }
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

        // Create button (0): first push the UI-pending name/seed back into vanilla's
        // own fields, then let vanilla handle the click — vanilla's creation path
        // reads field_146333_g / field_146335_h / field_146336_i directly and never
        // touches field_146330_J / field_146329_I.
        // 创建按钮(0)：先把 UI 端待创建的名字/种子写回原版自己的字段，再交还原版
        // 处理点击——原版创建路径直接读 field_146333_g / field_146335_h /
        // field_146336_i，从不碰 field_146330_J / field_146329_I。
        if (button.id == 0) {
            modernWorldCreatingUI$syncWorldNameToVanilla();
            modernWorldCreatingUI$syncSeedToVanilla();
            return;
        }

        // Cancel button (1): let vanilla handle it
        // 取消按钮(1)：交给原版处理
        if (button.id == 1) {
            return;
        }

        // All other buttons are managed by Tab system components
        // 所有其他按钮由 Tab 系统组件管理
        ci.cancel();
    }

    /**
     * <p>Pushes the pending world name (as edited in the tab UI) into vanilla's own
     * {@code worldNameField}, then re-runs vanilla's save-dir name calculation so
     * {@code field_146336_i} picks up the sanitized / de-duplicated directory name.
     * Running vanilla's own routine keeps directory-name rules (illegal characters,
     * reserved names, name conflicts) in one place instead of duplicating them.</p>
     * <p>把 UI 端待创建的世界名写回原版自己的 {@code worldNameField}，再重跑原版的
     * 存档目录名计算，让 {@code field_146336_i} 拿到清洗过、避让过重名的目录名。
     * 直接复用原版例程，非法字符/保留名/重名避让等规则只需维护原来那一份。</p>
     */
    @Unique
    private void modernWorldCreatingUI$syncWorldNameToVanilla() {
        String pending = modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName();

        // Mirror GameTab constructor's empty-name fallback so both entry points agree
        // 与 GameTab 构造器的空名 fallback 口径保持一致
        if ((pending == null || pending.trim().isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            pending = I18n.format("selectWorld.newWorld");
            modernWorldCreatingUI$accessor.modernWorldCreatingUI$setWorldName(pending);
        }
        if (pending == null) {
            pending = "";
        }

        modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldNameField().setText(pending);
        modernWorldCreatingUI$accessor.modernWorldCreatingUI$calcSaveDirName();
    }

    /**
     * <p>Pushes the pending seed (as edited in the tab UI) into vanilla's own
     * {@code seedField} — vanilla reads the seed from that field at creation time,
     * never from {@code field_146329_I}. No extra processing is needed: an empty
     * string simply means "random seed", same as vanilla.</p>
     * <p>把 UI 端待创建的种子写回原版自己的 {@code seedField}——原版创建时从它读
     * 种子，从不读 {@code field_146329_I}。无需额外处理：空串就是“随机种子”，
     * 与原版语义一致。</p>
     */
    @Unique
    private void modernWorldCreatingUI$syncSeedToVanilla() {
        String seed = modernWorldCreatingUI$accessor.modernWorldCreatingUI$getSeed();
        if (seed == null) {
            seed = "";
        }
        modernWorldCreatingUI$accessor.modernWorldCreatingUI$getSeedField().setText(seed);
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
     * <p>Handles the merged keyTyped channel via @Inject — forwards the event to the
     * component root (leaf widgets such as edit boxes implement only keyTyped) and
     * then cancels the vanilla method to prevent keys from reaching vanilla’s
     * hardcoded text fields.</p>
     * <p>Split events (keyPressed / keyReleased / charTyped) and Tab focus navigation
     * are delivered by CatFrame’s {@link CatFrameInputScreen} bridge
     * (MixinGuiScreen → ScreenKeyboardInput) — they must not be dispatched again here.</p>
     * <p>通过 @Inject 处理合并式 keyTyped 通道——把事件转发给组件根（输入框等叶子
     * 组件只实现 keyTyped），随后取消原版方法，防止按键流进原版硬编码位置的输入框。</p>
     * <p>拆分事件（keyPressed / keyReleased / charTyped）与 Tab 焦点导航由 CatFrame 的
     * {@link CatFrameInputScreen} 桥接链路（MixinGuiScreen → ScreenKeyboardInput）投递，
     * 此处不可重复派发。</p>
     */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernWorldCreatingUI$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return; // Not initialized — let vanilla handle it / 未初始化，让原版自己处理
        }

        // Deliver the merged keyTyped event to the focused component
        // 把合并式 keyTyped 事件投递给焦点组件
        if (modernWorldCreatingUI$tabRoot != null) {
            modernWorldCreatingUI$tabRoot.dispatchKeyTyped(typedChar, keyCode);
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
     * (which dispatches to the vanilla button list via super.mouseClicked internally),
     * then forwards the raw event to the component root, which resolves the target
     * child (nav-bar switching included) and updates focus.</p>
     * <p>通过 @Inject TAIL 处理鼠标点击——在原版自己的 mouseClicked 执行完之后
     * （它内部会调 super.mouseClicked 派发给按钮列表），把原始事件交给组件根：
     * 由它解析目标子组件（含导航栏切换）并更新焦点。</p>
     */
    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void modernWorldCreatingUI$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return;
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

        // 组件系统派发：导航栏点击切换 + 当前标签页内容（自动更新焦点）
        // Component-system dispatch: nav-bar switching + current tab content
        // (focus updated automatically)
        if (modernWorldCreatingUI$tabRoot != null) {
            modernWorldCreatingUI$tabRoot.mouseClicked(mouseX, mouseY, mouseButton);
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
        int delta = Mouse.getEventDWheel();

        if (delta != 0 && modernWorldCreatingUI$tabRoot != null) {
            // Normalised delta (±1) matches CatFrame's own screen pipeline; the component
            // under the cursor decides what to do with the scroll (cycling, scrolling, …).
            // 归一化增量（±1）与 CatFrame 自有屏幕管线一致；由鼠标下的组件决定如何使用
            // （循环切换、滚动等）。
            modernWorldCreatingUI$tabRoot.dispatchMouseScrolled(mouseX, mouseY, delta > 0 ? 1 : -1);
        }
    }

    /**
     * <p>Forwards mouse-release to the component root. Kept as a plain @Override for the
     * same reason as {@link #handleMouseInput()} — the method is inherited from GuiScreen,
     * so Mixin cannot inject into it reliably.</p>
     * <p>把鼠标释放转发给组件根。保留普通 @Override，原因同 {@link #handleMouseInput()}：
     * 该方法继承自 GuiScreen，Mixin 无法稳定注入。</p>
     */
    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);

        if (!modernWorldCreatingUI$isInitialized || modernWorldCreatingUI$tabRoot == null) {
            return;
        }

        // state < 0 is a pure move event; only button releases (>= 0) are dispatched
        // state < 0 是纯移动事件；只派发按钮释放（>= 0）
        if (state >= 0) {
            modernWorldCreatingUI$tabRoot.dispatchMouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * <p>Forwards mouse-drag to the component root. Kept as a plain @Override for the
     * same reason as {@link #handleMouseInput()}.</p>
     * <p>把鼠标拖动转发给组件根。保留普通 @Override，原因同 {@link #handleMouseInput()}。</p>
     */
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (!modernWorldCreatingUI$isInitialized || modernWorldCreatingUI$tabRoot == null) {
            return;
        }

        modernWorldCreatingUI$tabRoot.dispatchMouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    /**
     * {@inheritDoc}
     * <p>CatFrame input bridge: routes split keyboard events (keyPressed / keyReleased /
     * charTyped) and Tab focus navigation to the component root. The merged keyTyped
     * channel arrives through the vanilla {@code handleKeyboardInput} loop instead.</p>
     * <p>CatFrame 输入桥：把拆分键盘事件（keyPressed / keyReleased / charTyped）与
     * Tab 焦点导航投递给组件根；合并式 keyTyped 通道则经原版
     * {@code handleKeyboardInput} 循环到达。</p>
     */
    @Override
    public GuiEventListener getEventRoot() {
        return modernWorldCreatingUI$tabRoot;
    }

    /**
     * @return the tab content area — below the tab bar (TAB_HEIGHT), above the footer
     *         (height - 35) / Tab 内容区域：Tab 栏下方（TAB_HEIGHT）到脚部上方（height - 35）
     */
    @Unique
    private ScreenRectangle modernWorldCreatingUI$tabArea() {
        int tabAreaTop = TAB_HEIGHT;
        int tabAreaBottom = this.height - 35;
        return new ScreenRectangle(0, tabAreaTop, this.width, tabAreaBottom - tabAreaTop);
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