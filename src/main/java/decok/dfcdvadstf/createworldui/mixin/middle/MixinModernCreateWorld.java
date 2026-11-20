package decok.dfcdvadstf.createworldui.mixin.middle;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.extended.adapter.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.components.events.CatFrameInputScreen;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.Tab;
import decok.dfcdvadstf.catframe.ui.components.tab.TabManager;
import decok.dfcdvadstf.catframe.ui.components.tab.TabNavigationBar;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.layouts.HorizontalLayout;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.TabScreenRoot;
import decok.dfcdvadstf.createworldui.api.tab.TabRegistry;
import decok.dfcdvadstf.createworldui.integrations.datapack.ok.OKCoreCompat;
import decok.dfcdvadstf.createworldui.mixin.middle.access.IGuiCreateWorldAccess;
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
 * {@link IGuiCreateWorldAccess} accessor mixin — this class focuses purely on injecting
 * tab-related logic, while field access lives in its own place.</p>
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
    private TabScreenRoot modernWorldCreatingUI$tabRoot;
    /**
     * 26.1.2 {@code HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT} — the
     * footer zone height ({@code MAGIC_PADDING 13} + a 20-tall button).
     */
    @Unique
    private static final int modernWorldCreatingUI$FOOTER_HEIGHT = 33;
    /**
     * 26.1.2 footer button spacing: {@code DirectionalLayoutWidget.horizontal().spacing(8)}.
     */
    @Unique
    private static final int modernWorldCreatingUI$FOOTER_SPACING = 8;
    /**
     * 26.1.2 default button width ({@code ButtonWidget.builder(...).build()} is 200x20).
     */
    @Unique
    private static final int modernWorldCreatingUI$BUTTON_WIDTH = 200;
    @Unique
    private boolean modernWorldCreatingUI$isInitialized = false;
    @Unique
    private static final Logger modernWorldCreatingUI$logger = LogManager.getLogger("MixinGuiCreateWorld");
    @Unique
    private final IGuiCreateWorldAccess modernWorldCreatingUI$accessor = (IGuiCreateWorldAccess) this;
    
    // HeaderFooterLayout main layout container
    @Unique
    private HeaderFooterLayout modernWorldCreatingUI$mainLayout;
    @Unique
    private HorizontalLayout modernWorldCreatingUI$footerButtonLayout;

    /**
     * Before UI Initialization.<br>
     * Notes: there is used to directly cancel the initGui method call during the HEAD injection phase, BUT, it caused a crash when opening this mod,
     * so this method is also called during the TAIL phase.
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

        // ===== Main layout: 26.1.2 CreateWorldScreen's ThreePartsLayout/HeaderAndFooterLayout =====
        modernWorldCreatingUI$mainLayout = new HeaderFooterLayout(true); // true = 绘制面板背景

        // ===== Footer (26.1.2 init(): addFooter(DirectionalLayoutWidget.horizontal().spacing(8))) =====
        modernWorldCreatingUI$footerButtonLayout = new HorizontalLayout();
        modernWorldCreatingUI$footerButtonLayout.setSpacing(modernWorldCreatingUI$FOOTER_SPACING);

        // Wrap vanilla buttons for footer layout; 26.1.2's Create/Cancel are default
        // 200-wide buttons. The buttons stay in vanilla's buttonList, so vanilla's own
        // mouseClicked dispatch owns the press sound and actionPerformed — the adapter
        // only bridges rendering and layout positioning.
        if (createButton != null) {
            createButton.width = modernWorldCreatingUI$BUTTON_WIDTH;
            GuiButtonAdapter modernWorldCreatingUI$createButtonAdapter = new GuiButtonAdapter(createButton);
            modernWorldCreatingUI$footerButtonLayout.addChild(modernWorldCreatingUI$createButtonAdapter);
        }

        if (cancelButton != null) {
            cancelButton.width = modernWorldCreatingUI$BUTTON_WIDTH;
            GuiButtonAdapter modernWorldCreatingUI$cancelButtonAdapter = new GuiButtonAdapter(cancelButton);
            modernWorldCreatingUI$footerButtonLayout.addChild(modernWorldCreatingUI$cancelButtonAdapter);
        }

        modernWorldCreatingUI$mainLayout.setFooterHeight(modernWorldCreatingUI$FOOTER_HEIGHT);
        modernWorldCreatingUI$mainLayout.setFooter(modernWorldCreatingUI$footerButtonLayout);

        // ===== Component-system tab assembly (mirrors high-version CreateWorldScreen.init()) =====
        // Drive the whole tab stack rebuild from initGui every pass — the same shape as
        // the high-version init(): a fresh navigation bar + fresh tabs, with the old root
        // discarded wholesale (the 1.7.10 counterpart of vanilla Screen's widget reset),
        // so no stale components can survive a resize or a sub-screen round-trip.
        modernWorldCreatingUI$tabRoot = new TabScreenRoot();
        modernWorldCreatingUI$tabManager = new TabManager(
                modernWorldCreatingUI$tabRoot::addChild,
                modernWorldCreatingUI$tabRoot::removeChild);
        // Cast through Object: this mixin merges into GuiCreateWorld at runtime, so
        // `this` IS the screen instance — the cast only satisfies the compile-time view.
        GuiCreateWorld screen = (GuiCreateWorld) (Object) this;
        GameTab gameTab = new GameTab(screen);
        // Merge external tabs registered through the TabRegistry API with the built-ins,
        // sorted by priority; the registry freezes on this first build.
        List<Tab> orderedTabs = TabRegistry.buildTabList(screen,
                gameTab, new WorldTab(screen), new MoreTab(screen));
        TabNavigationBar modernWorldCreatingUI$tabBar = TabNavigationBar.builder(modernWorldCreatingUI$tabManager, this.width)
                .addTabs(orderedTabs.toArray(new Tab[0]))
                .build();

        // Set the nav bar to the Header zone and register it as the root's first child
        // (click / focus chain)
        modernWorldCreatingUI$mainLayout.setHeader(modernWorldCreatingUI$tabBar);
        modernWorldCreatingUI$tabRoot.addChild(modernWorldCreatingUI$tabBar);

        // ===== 26.1.2 initTabNavigation(): layout, tab area and initial selection =====
        // After the nav bar width update, its bottom edge is the header height
        // (26.1.2: getNavigationFocus().getBottom())
        modernWorldCreatingUI$tabBar.updateWidth(this.width);
        modernWorldCreatingUI$mainLayout.setHeaderHeight(modernWorldCreatingUI$tabBar.getRectangle().bottom());
        modernWorldCreatingUI$mainLayout.recalculate(this.width, this.height);
        // Tab content area: from the nav bar's bottom edge to the footer's top edge
        // (26.1.2: ScreenRect(0, i, width, height - footerHeight - i))
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
        // This preserves indices set by DefaultWorldGenerator or other mods
        if (WorldType.worldTypes == null || this.field_146331_K < 0) {
            this.field_146331_K = 0;
        } else if (this.field_146331_K >= WorldType.worldTypes.length ||
                WorldType.worldTypes[this.field_146331_K] == null) {
            // Index out of range or points to null — find a valid default
            modernWorldCreatingUI$logger.warn("Invalid world type index: " + this.field_146331_K + ", resetting to 0");
            this.field_146331_K = 0;
        }
    }

    /**
     * Draws the screen
     */
    @Inject(method = {"drawScreen"}, at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {

        ci.cancel();

        // draw background
        this.drawBackground(0);

        // ===== 26.1.2 renderDarkening() + render(): header background, darkening below
        // the header, footer separator =====
        if (modernWorldCreatingUI$mainLayout != null) {
            int headerHeight = modernWorldCreatingUI$mainLayout.getHeaderHeight();
            int footerTop = this.height - modernWorldCreatingUI$mainLayout.getFooterHeight();
             // Header zone: the tab header background (26.1.2's TAB_HEADER_BACKGROUND_TEXTURE
            // tiled over the header)
            ContentPanelRenderer.drawPanelBackground(0, 0, this.width, headerHeight);
            // Everything below the header darkened (26.1.2's renderDarkening covers from
            // below the header down to the screen bottom, footer zone included)
            ContentPanelRenderer.drawPanelBackground(0, headerHeight, this.width, this.height - headerHeight);
            // Footer separator: 2px tall, right above the footer top (26.1.2 render():
            // height - footerHeight - 2)
            ContentPanelRenderer.drawFooterSeparator(0, footerTop - 2, this.width);
        }

        // Per-frame preparation of the current tab (dynamic states, tooltips, label
        // overlays) — each concrete tab implements its own
        if (modernWorldCreatingUI$tabManager != null) {
            Tab currentTab = modernWorldCreatingUI$tabManager.getCurrentTab();
            if (currentTab instanceof GameTab) {
                ((GameTab) currentTab).prepareFrame();
            } else if (currentTab instanceof WorldTab) {
                ((WorldTab) currentTab).prepareFrame();
            }
        }

        // Component-system rendering: nav bar + current tab children (hover refresh and
        // deferred tooltip pumping included)
        if (modernWorldCreatingUI$tabRoot != null) {
            modernWorldCreatingUI$tabRoot.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        // ===== Draw buttons in Footer zone =====
        if (modernWorldCreatingUI$footerButtonLayout != null) {
            for (decok.dfcdvadstf.catframe.ui.layouts.ILayout child : modernWorldCreatingUI$footerButtonLayout.getChildren()) {
                if (child instanceof GuiButtonAdapter) {
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
     */
    @Unique
    private void modernWorldCreatingUI$syncWorldNameToVanilla() {
        String pending = modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName();

        // Mirror GameTab constructor's empty-name fallback so both entry points agree
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
        // 从 UI 选中的难度设置待应用难度，将由 MixinIntegratedServer.loadAllWorlds TAIL 应用
        DifficultyApplier.setPendingDifficulty(DifficultyApplier.getSelectedDifficulty());
    }

    /**
     * <p>
     *     World-launch pre-processing: if the DataPackScreen toggled any OKCore
     *     datapack, flush the disabled list into the new world's
     *     {@code okcore_datapacks.txt} BEFORE the integrated server thread starts —
     *     via OKCoreCompat's direct call to OKCore's own saveDisabledPacksConfig.
     *     OKCore reads that file at {@code FMLServerAboutToStartEvent}
     *     (IntegratedServer.startServer runs {@code handleServerAboutToStart}
     *     before {@code loadAllWorlds}), so writing any later — e.g. at
     *     loadAllWorlds TAIL like the difficulty — would be too late for the first
     *     world load. The world dir does not exist yet on the client thread, so the
     *     compat layer creates it first.
     * </p>
     */
    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;launchIntegratedServer(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void modernWorldCreatingUI$beforeLaunchWorld(GuiButton button, CallbackInfo ci) {
        // Only flush when DataPackScreen toggled something; reuse OKCore's own writer (see OKCoreCompat)
        OKCoreCompat.savePendingSelectionToWorldDir(
                modernWorldCreatingUI$accessor.modernWorldCreatingUI$getSaveDirName());
    }

    /**
     * <p>Handles the merged keyTyped channel via @Inject — forwards the event to the
     * component root (leaf widgets such as edit boxes implement only keyTyped) and
     * then cancels the vanilla method to prevent keys from reaching vanilla’s
     * hardcoded text fields.</p>
     * <p>Split events (keyPressed / keyReleased / charTyped) and Tab focus navigation
     * are delivered by CatFrame’s {@link CatFrameInputScreen} bridge
     * (MixinGuiScreen → ScreenKeyboardInput) — they must not be dispatched again here.</p>
     */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void modernWorldCreatingUI$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return; // Not initialized — let vanilla handle it
        }

        // Deliver the merged keyTyped event to the focused component
        if (modernWorldCreatingUI$tabRoot != null) {
            modernWorldCreatingUI$tabRoot.dispatchKeyTyped(typedChar, keyCode);
        }

        // Update create world button state
        GuiButton createButton = modernWorldCreatingUI$getButtonById(0);
        if (createButton != null) {
            createButton.enabled = modernWorldCreatingUI$tabManager != null &&
                    (!modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName().trim().isEmpty() ||
                            modernWorldCreatingUI$accessor.modernWorldCreatingUI$getWorldName() != null) &&
            modernWorldCreatingUI$isInitialized;
        }

        // Manually handle ESC since we are about to cancel vanilla keyTyped
        if (keyCode == 1) {
            this.mc.displayGuiScreen(field_146332_f);
        }

        // Always cancel vanilla — otherwise keys would flow into vanilla's hardcoded text fields
        ci.cancel();
    }

    /**
     * <p>Handles mouse clicks via @Inject TAIL — runs after vanilla's own mouseClicked
     * (which dispatches to the vanilla button list via super.mouseClicked internally),
     * then forwards the raw event to the component root, which resolves the target
     * child (nav-bar switching included) and updates focus.</p>
     */
    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void modernWorldCreatingUI$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized) {
            return;
        }

        // Footer Create/Cancel clicks need no dispatch here — they remain in vanilla's
        // buttonList and the vanilla mouseClicked body (running before this TAIL
        // injection) already delivered the press sound and actionPerformed.

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
            modernWorldCreatingUI$tabRoot.dispatchMouseScrolled(mouseX, mouseY, delta > 0 ? 1 : -1);
        }
    }

    /**
     * <p>Forwards mouse-release to the component root. Kept as a plain @Override for the
     * same reason as {@link #handleMouseInput()} — the method is inherited from GuiScreen,
     * so Mixin cannot inject into it reliably.</p>
     */
    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);

        if (!modernWorldCreatingUI$isInitialized || modernWorldCreatingUI$tabRoot == null) {
            return;
        }

        // state < 0 is a pure move event; only button releases (>= 0) are dispatched
        if (state >= 0) {
            modernWorldCreatingUI$tabRoot.dispatchMouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * <p>Forwards mouse-drag to the component root. Kept as a plain @Override for the
     * same reason as {@link #handleMouseInput()}.</p>
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

     */
    @Override
    public GuiEventListener getEventRoot() {
        return modernWorldCreatingUI$tabRoot;
    }

    /**
     * @return the tab content area — from the tab bar's bottom edge (header height) down
     *         to the footer's top edge (height - footerHeight), mirroring 26.1.2's
     *         {@code ScreenRect(0, i, width, height - footerHeight - i)}
     */
    @Unique
    private ScreenRectangle modernWorldCreatingUI$tabArea() {
        int tabAreaTop = modernWorldCreatingUI$mainLayout.getHeaderHeight();
        int tabAreaBottom = this.height - modernWorldCreatingUI$mainLayout.getFooterHeight();
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