package decok.dfcdvadstf.createworldui.mixin;

import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.api.tab.TabState;
import decok.dfcdvadstf.createworldui.gamerule.GameRuleEditor;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.*;

/**
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
    private GuiTextField field_146335_h;
    @Shadow
    private GuiTextField field_146333_g;
    @Shadow
    private int field_146331_K;

    // 新添加的字段
    @Unique
    private TabManager modernWorldCreatingUI$tabManager;
    @Unique
    private static final ResourceLocation OPTIONS_BG_DARK = new ResourceLocation("createworldui:textures/gui/options_background_dark.png");
    @Unique
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("createworldui:textures/gui/tabs.png");
    @Unique
    private static final int TAB_WIDTH = 130;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private final Map<Integer, String> modernWorldCreatingUI$hoverTexts = new HashMap<>();
    @Unique
    private boolean modernWorldCreatingUI$isInitialized = false;

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
        System.out.println("MixinModernCreateWorld: Initializing GUI");

        // 首先清空按钮列表，但保留创建和取消按钮
        List<GuiButton> essentialButtons = new ArrayList<>();
        for (GuiButton button : (List<GuiButton>)this.buttonList) {
            if (button.id == 0 || button.id == 1) {
                essentialButtons.add(button);
            }
        }

        this.buttonList.clear();
        this.buttonList.addAll(essentialButtons);

        // 确保字段不为空
        modernWorldCreatingUI$ensureFieldsNotNull();

        // 初始化标签页管理器
        modernWorldCreatingUI$tabManager = new TabManager(
                (GuiCreateWorld)(Object)this, this.buttonList, this.width, this.height
        );

        // 将原版状态传递给TabManager
        modernWorldCreatingUI$sendStateToTabManager();

        // 创建标签页按钮
        modernWorldCreatingUI$createTabButtons();
        modernWorldCreatingUI$repositionActionButtons();

        // 初始化悬停文本
        modernWorldCreatingUI$initHoverTexts();

        modernWorldCreatingUI$isInitialized = true;

        System.out.println("MixinModernCreateWorld: GUI initialized with " + this.buttonList.size() + " buttons");
    }

    /**
     * 同步原版状态到TabManager
     */
    @Unique
    private void modernWorldCreatingUI$sendStateToTabManager() {
        if (modernWorldCreatingUI$tabManager == null) {
            return;
        }

        System.out.println("MixinModernCreateWorld: Passing state to TabManager");
        System.out.println("  field_146330_J (world name): " + field_146330_J);
        System.out.println("  field_146342_r (game mode): " + field_146342_r);
        System.out.println("  field_146329_I (seed): " + field_146329_I);

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
     * 同步TabManager状态到原版字段
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

            System.out.println("MixinModernCreateWorld: State synced - WorldName: " + field_146330_J);
            System.out.println("MixinModernCreateWorld: State synced - GameMode: " + field_146342_r);
            System.out.println("MixinModernCreateWorld: State synced - Hardcore: " + field_146337_w);
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
        if (this.field_146330_J == null) {
            this.field_146330_J = I18n.format("selectWorld.newWorld");
            System.out.println("MixinModernCreateWorld: Set default world name: " + this.field_146330_J);
        }
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
            System.out.println("MixinModernCreateWorld: Repositioned create button");
        }

        if (cancelButton != null) {
            cancelButton.xPosition = this.width / 2 + 5;
            cancelButton.yPosition = this.height - 28;
            cancelButton.width = 150;
            cancelButton.height = 20;
            cancelButton.visible = true;
            System.out.println("MixinModernCreateWorld: Repositioned cancel button");
        }
    }

    /**
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
                        mc.getTextureManager().bindTexture(TABS_TEXTURE);
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
            System.out.println("MixinModernCreateWorld: Added tab button with ID: " + tabId);
        }
    }

    /**
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

        // 绘制分隔线
        modernWorldCreatingUI$drawColoredLine(0, TAB_HEIGHT - 3, this.width, 0x00FFFFFF, 0x40FFFFFF);
        modernWorldCreatingUI$drawColoredLine(0, this.height - 35, this.width, 0x40000000, 0x40FFFFFF);

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
     * 处理按钮点击
     */
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (!modernWorldCreatingUI$isInitialized || button == null) {
            return;
        }

        System.out.println("MixinModernCreateWorld: Button action performed: " + button.id);

        // 处理创建按钮 - 在创建前同步状态
        if (button.id == 0) {
            modernWorldCreatingUI$getStateFromTabManager();
            System.out.println("MixinModernCreateWorld: Synced state before creating world");
            // 让原版继续处理创建逻辑
            return;
        }

        // 首先处理标签页管理器的事件
        if (modernWorldCreatingUI$tabManager != null) {
            modernWorldCreatingUI$tabManager.actionPerformed(button);

            // 如果按钮ID在100-102之间，说明是标签页切换，取消后续处理
            if (button.id >= 100 && button.id <= 102) {
                ci.cancel();
                return;
            }
        }

        // 处理游戏规则编辑器按钮
        if (button.id == 200) {
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null) pending = new HashMap<>();

            try {
                Minecraft mc = Minecraft.getMinecraft();
                net.minecraft.world.World clientWorld = mc != null ? mc.theWorld : null;
                if (clientWorld != null) {
                    Map<String, Object> opt = GameRuleMonitorNSetter.getOptimalGameruleValues(clientWorld);
                    if (opt != null && !opt.isEmpty()) {
                        for (Map.Entry<String, Object> e : opt.entrySet()) {
                            pending.put(e.getKey(), String.valueOf(e.getValue()));
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            this.mc.displayGuiScreen(new GameRuleEditor((GuiCreateWorld)(Object)this, pending));
            ci.cancel();
            return;
        }

        // 其他按钮由标签页管理器处理，阻止原版处理
        if (button.id >= 2 && button.id <= 9 || button.id == 200) {
            ci.cancel();
        }
    }

    /**
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
     * 处理按键输入
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!modernWorldCreatingUI$isInitialized) {
            super.keyTyped(typedChar, keyCode);
            return;
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