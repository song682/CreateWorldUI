package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.GuiCyclableButton;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.api.tab.TabState;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
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

import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.OpenGlHelper;

import java.lang.reflect.Field;
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
    private static final ResourceLocation OPTIONS_BG_OPAQUE = new ResourceLocation("createworldui","textures/gui/options_background_opaque.png");
    @Unique
    private static final ResourceLocation VANILLA_DIRT_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
    @Unique
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("createworldui","textures/gui/tabs.png");
    @Unique
    private static final ResourceLocation TABS_LEGACY_TEXTURE = new ResourceLocation("createworldui","textures/gui/tabs.png");
    @Unique
    private static final ResourceLocation TABS_OPAQUE_TEXTURE = new ResourceLocation("createworldui","textures/gui/tabs_opaque.png");
    @Unique
    private static final ResourceLocation LIGHT_DIRT_BACKGROUND = new ResourceLocation("createworldui","textures/gui/light_dirt_background.png");
    @Unique
    private static final ResourceLocation HEADER_SEPARATOR = new ResourceLocation("createworldui","textures/gui/header_separator.png");
    @Unique
    private static final ResourceLocation FOOTER_SEPARATOR = new ResourceLocation("createworldui","textures/gui/footer_separator.png");
    @Unique
    private static final ResourceLocation HEADER_SEPARATOR_OPAQUE = new ResourceLocation("createworldui","textures/gui/header_separator_opaque.png");
    @Unique
    private static final ResourceLocation FOOTER_SEPARATOR_OPAQUE = new ResourceLocation("createworldui","textures/gui/footer_separator_opaque.png");
    @Unique
    private static final int TAB_WIDTH = 130;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private final Map<Integer, String> modernWorldCreatingUI$hoverTexts = new HashMap<>();
    @Unique
    private boolean modernWorldCreatingUI$isInitialized = false;
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
     * Creates the tab buttons
     * 创建标签页按钮
     */
    @Unique
    private void modernWorldCreatingUI$createTabButtons() {
        int totalWidth = TAB_WIDTH * 3 + 2;
        int startX = this.width / 2 - totalWidth / 2;
        String[] tabNames = {
                I18n.format("createworldui.tab.game"),
                I18n.format("createworldui.tab.world"),
                I18n.format("createworldui.tab.more")
        };

        for (int i = 0; i < 3; i++) {
            int xPos = startX + i * (TAB_WIDTH + 1);
            final int tabId = 100 + i;
            GuiButton tabButton = new GuiButton(tabId, xPos, 0, TAB_WIDTH, TAB_HEIGHT, tabNames[i]) {
                @Override
                public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                    if (this.visible) {
                        // Select texture based on background rendering mode
                        // 根据背景渲染模式选择纹理
                        int renderType = CreateWorldUI.config.backgroundRenderingType;
                        // Mode 0: tabs_legacy.png, Mode 1: tabs.png, Mode 2: tabs_opaque.png
                        // 模式 0: tabs_legacy.png, 模式 1: tabs.png, 模式 2: tabs_opaque.png
                        ResourceLocation tabTexture;
                        switch (renderType) {
                            case 0:
                                tabTexture = TABS_LEGACY_TEXTURE;
                                break;
                            case 1:
                                tabTexture = TABS_TEXTURE;
                                break;
                            case 2:
                                tabTexture = TABS_OPAQUE_TEXTURE;
                                break;
                            default:
                                tabTexture = TABS_LEGACY_TEXTURE;
                                break;
                        }
                        mc.getTextureManager().bindTexture(tabTexture);
                        
                        boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                                mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                        boolean isSelected = modernWorldCreatingUI$tabManager != null &&
                                modernWorldCreatingUI$tabManager.getCurrentTabId() == this.id;

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
            tabButton.visible = true;
            this.buttonList.add(tabButton);
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

        int renderType = CreateWorldUI.config.backgroundRenderingType;
        boolean isClearMyBackgroundLoaded = Loader.isModLoaded("clearmybackground");

        switch (renderType) {
            case 0:
                // Legacy mode (mode 0)
                // Legacy 模式 (模式 0)
                modernWorldCreatingUI$drawLegacyBackground(isClearMyBackgroundLoaded);
                break;
            case 1:
                // 1.17-1.20.4 style (mode 1)
                // 1.17-1.20.4 风格 (模式 1)
                modernWorldCreatingUI$drawModernBackground();
                break;
            case 2:
                // 1.20.5+ style (mode 2) - requires ClearMyBackground mod
                // 1.20.5+ 风格 (模式 2) - 需要 ClearMyBackground 模组
                if (isClearMyBackgroundLoaded) {
                    modernWorldCreatingUI$drawClearMyBackgroundStyle();
                } else {
                    // Fallback to mode 1 if mod not loaded
                    // 如果模组未加载，回退到模式 1
                    modernWorldCreatingUI$drawModernBackground();
                }
                break;
            default:
                // Default to legacy mode
                // 默认使用 Legacy 模式
                modernWorldCreatingUI$drawLegacyBackground(isClearMyBackgroundLoaded);
                break;
        }

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
     * Legacy background rendering (mode 0)
     * Legacy 背景渲染 (模式 0)
     */
    @Unique
    private void modernWorldCreatingUI$drawLegacyBackground(boolean isClearMyBackgroundLoaded) {
        // Always draw vanilla dirt background, bypassing ClearMyBackground's mixin interception
        // 始终绘制原版泥土背景，绕过 ClearMyBackground 的 mixin 拦截
        modernWorldCreatingUI$drawVanillaDirtBackground();

        // Draw tab button background with OPTIONS_BG_DARK texture
        // 使用 OPTIONS_BG_DARK 纹理绘制标签按钮背景
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_DARK);
        this.modernWorldCreatingUI$drawTiledTexture(0, 0, this.width, TAB_HEIGHT, 16, 16);

        // Draw semi-transparent separator lines
        // 绘制半透明分隔线
        modernWorldCreatingUI$drawColoredLine(0, TAB_HEIGHT - 3, this.width, 0x00FFFFFF, 0x40FFFFFF);
        modernWorldCreatingUI$drawColoredLine(0, this.height - 35, this.width, 0x40000000, 0x40FFFFFF);
    }

    /**
     * Modern background rendering (mode 1, 1.17-1.20.4 style)
     * 现代背景渲染 (模式 1, 1.17-1.20.4 风格)
     */
    @Unique
    private void modernWorldCreatingUI$drawModernBackground() {
        // Draw vanilla dirt background texture, bypassing ClearMyBackground's mixin interception
        // 绘制原版泥土背景纹理，绕过 ClearMyBackground 的 mixin 拦截
        modernWorldCreatingUI$drawVanillaDirtBackground();

        // Draw tab button background: solid black #000000 with 100% opacity
        // 绘制标签按钮背景：纯黑色 #000000，不透明度 100%
        modernWorldCreatingUI$drawTabButtonBackground(100);

        // Draw textured separator lines using header_separator.png and footer_separator.png
        // 使用 header_separator.png 和 footer_separator.png 绘制纹理分隔线
        modernWorldCreatingUI$drawTexturedSeparator(0, TAB_HEIGHT - 2, this.width, true, false);
        modernWorldCreatingUI$drawTexturedSeparator(0, this.height - 35, this.width, false, false);
    }

    /**
     * ClearMyBackground style rendering (mode 2, 1.20.5+ style)
     * ClearMyBackground 风格渲染 (模式 2, 1.20.5+ 风格)
     */
    @Unique
    private void modernWorldCreatingUI$drawClearMyBackgroundStyle() {
        // Use ClearMyBackground's background rendering
        // drawDefaultBackground will be intercepted by ClearMyBackground and render its custom background
        // 使用 ClearMyBackground 的背景渲染
        // drawDefaultBackground 会被 ClearMyBackground 拦截并渲染其自定义背景
        this.drawDefaultBackground();

        // Draw tab button background with OPTIONS_BG_OPAQUE texture
        // 使用 OPTIONS_BG_OPAQUE 纹理绘制标签按钮背景
        this.mc.getTextureManager().bindTexture(OPTIONS_BG_OPAQUE);
        this.modernWorldCreatingUI$drawTiledTexture(0, 0, this.width, TAB_HEIGHT, 16, 16);

        // Draw textured separator lines using header_separator_opaque.png and footer_separator_opaque.png
        // 使用 header_separator_opaque.png 和 footer_separator_opaque.png 绘制纹理分隔线
        modernWorldCreatingUI$drawTexturedSeparator(0, TAB_HEIGHT - 2, this.width, true, true);
        modernWorldCreatingUI$drawTexturedSeparator(0, this.height - 35, this.width, false, true);
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

            // If button ID is 100-102, it's a tab switch; cancel further processing
            // 如果按钮 ID 在 100-102 之间，说明是标签页切换，取消后续处理
            if (button.id >= 100 && button.id <= 102) {
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

        // Apply difficulty setting to newly created world
        // 应用难度设置到新创建的世界
        try {
            Object integrated = this.mc.getIntegratedServer();
            if (integrated == null) {
                try {
                    Field fserv = Minecraft.class.getDeclaredField("theIntegratedServer");
                    fserv.setAccessible(true);
                    integrated = fserv.get(this.mc);
                } catch (Throwable ignored) {}
            }

            if (integrated != null) {
                Class<?> serverClass = integrated.getClass();
                try {
                    Field worldsField = serverClass.getDeclaredField("worldServers");
                    worldsField.setAccessible(true);
                    Object[] worlds = (Object[]) worldsField.get(integrated);
                    if (worlds != null && worlds.length > 0) {
                        for (Object w : worlds) {
                            if (w != null) {
                                try {
                                    Field diffField = w.getClass().getDeclaredField("difficultySetting");
                                    diffField.setAccessible(true);
                                    diffField.set(w, modernWorldCreatingUI$tabManager.getDifficulty());
                                } catch (NoSuchFieldException nsf) {
                                    try {
                                        Field diffField = w.getClass().getDeclaredField("field_73013_u");
                                        diffField.setAccessible(true);
                                        diffField.set(w, modernWorldCreatingUI$tabManager.getDifficulty());
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                        this.mc.gameSettings.difficulty = modernWorldCreatingUI$tabManager.getDifficulty();
                        this.mc.gameSettings.saveOptions();
                    }
                } catch (Throwable t) {
                    try {
                        Field tw = Minecraft.class.getDeclaredField("theWorld");
                        tw.setAccessible(true);
                        Object clientWorld = tw.get(this.mc);
                        if (clientWorld != null) {
                            try {
                                Field diffField = clientWorld.getClass().getDeclaredField("difficultySetting");
                                diffField.setAccessible(true);
                                diffField.set(clientWorld, modernWorldCreatingUI$tabManager.getDifficulty());
                            } catch (NoSuchFieldException nsf) {
                                try {
                                    Field diffField = clientWorld.getClass().getDeclaredField("field_73013_u");
                                    diffField.setAccessible(true);
                                    diffField.set(clientWorld, modernWorldCreatingUI$tabManager.getDifficulty());
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
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
                    if (button.id >= 100 && button.id <= 102) continue;
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

    /**
     * Draws light dirt background texture (1.17-1.20.4 style)
     * 绘制浅色泥土背景纹理 (1.17-1.20.4 风格)
     */
    @Unique
    private void modernWorldCreatingUI$drawLightDirtBackground() {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        this.mc.getTextureManager().bindTexture(LIGHT_DIRT_BACKGROUND);
        // Use Gui.func_146110_a to draw a stretched texture
        // 使用 Gui.func_146110_a 绘制拉伸纹理
        Gui.func_146110_a(0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);

        if (blend) GL11.glEnable(GL11.GL_BLEND);
        else GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Draws vanilla dirt background texture (bypasses ClearMyBackground mixin)
     * 绘制原版泥土背景纹理（绕过 ClearMyBackground mixin）
     */
    @Unique
    private void modernWorldCreatingUI$drawVanillaDirtBackground() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);

        Tessellator tessellator = Tessellator.instance;
        this.mc.getTextureManager().bindTexture(VANILLA_DIRT_BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        float f = 32.0F;
        tessellator.startDrawingQuads();
        // Set color to darken the background (same as vanilla: 4210752 = 0x404040)
        // 设置颜色降低背景明度（与原版一致：4210752 = 0x404040）
        tessellator.setColorOpaque_I(4210752);
        tessellator.addVertexWithUV(0.0D, (double)this.height, 0.0D, 0.0D, (double)((float)this.height / f));
        tessellator.addVertexWithUV((double)this.width, (double)this.height, 0.0D, (double)((float)this.width / f), (double)((float)this.height / f));
        tessellator.addVertexWithUV((double)this.width, 0.0D, 0.0D, (double)((float)this.width / f), 0.0D);
        tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    /**
     * Draws textured separator line using header_separator.png or footer_separator.png
     * 使用 header_separator.png 或 footer_separator.png 绘制纹理分隔线
     * @param useOpaque if true, uses _opaque suffix textures
     * @param useOpaque 如果为 true，使用 _opaque 后缀纹理
     */
    @Unique
    private void modernWorldCreatingUI$drawTexturedSeparator(int x, int y, int width, boolean isHeader, boolean useOpaque) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        ResourceLocation headerTexture = useOpaque ? HEADER_SEPARATOR_OPAQUE : HEADER_SEPARATOR;
        ResourceLocation footerTexture = useOpaque ? FOOTER_SEPARATOR_OPAQUE : FOOTER_SEPARATOR;
        this.mc.getTextureManager().bindTexture(isHeader ? headerTexture : footerTexture);
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, 2, 32, 2);

        if (blend) GL11.glEnable(GL11.GL_BLEND);
        else GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Draws solid color background with specified opacity
     * 绘制指定不透明度的纯色背景
     */
    @Unique
    private void modernWorldCreatingUI$drawSolidColorBackground(int color) {
        drawRect(0, 0, this.width, this.height, color);
    }

    /**
     * Draws tab button background with solid black color
     * 绘制标签按钮背景（纯黑色填充）
     */
    @Unique
    private void modernWorldCreatingUI$drawTabButtonBackground(int opacityPercent) {
        // Calculate ARGB color: AARRGGBB where AA is alpha (0-255)
        // 计算 ARGB 颜色：AARRGGBB，其中 AA 是透明度 (0-255)
        int alpha = (int)(opacityPercent * 2.55); // Convert percentage to 0-255 range
        int color = (alpha << 24) | 0x000000; // Black with custom opacity
        drawRect(0, 0, this.width, TAB_HEIGHT, color);
    }

    // 保留原版方法
    @Shadow
    private void func_146314_g() {}
    @Shadow
    private void func_146319_h() {}
}