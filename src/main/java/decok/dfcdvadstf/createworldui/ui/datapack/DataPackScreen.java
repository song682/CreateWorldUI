package decok.dfcdvadstf.createworldui.ui.datapack;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.ObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.createworldui.Tags;
import decok.dfcdvadstf.createworldui.integrations.datapack.ok.OKCoreCompat;
import decok.dfcdvadstf.createworldui.integrations.datapack.ok.OKCoreCompat.PackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     数据包选择界面 —— 由 {@code MoreTab} 的 DataPack 按钮打开，兼容 OKCore 的数据包体系。<br>
 *     布局类似资源包选择界面：左侧为可选择的（未激活）数据包，右侧为已激活的数据包；
 *     中间两枚按钮 / 双击条目在两侧之间移动数据包；底部"完成"返回创建世界界面。
 * </p>
 * <p>
 * <ul>
 *     <li>屏幕基类：前置模组 CatFrame 的 {@link Screen}；两侧列表均为包含在本类内的
 *     UI 列表类（{@link AvailablePackList} / {@link ActivePackList}），继承 CatFrame 的
 *     {@link ObjectSelectionList}；滚动、裁剪与选中高亮由列表基类管理</li>
 *     <li>列表背景：深色泥土（本模组的 options_background_dark.png 平铺），见
 *     {@link #drawDarkDirt}</li>
 *     <li>数据兼容：读写均直接经 {@link OKCoreCompat} 调用 OKCore 的
 *     DatapackManager（读取注册表、enable/disable 切换实时状态）；创建世界时由
 *     MixinModernCreateWorld 在启动集成服务器前复用 OKCore 的写入器把禁用清单落盘到
 *     新世界的 okcore_datapacks.txt。本类不直接引用任何 OKCore 类型</li>
 *     <li>模组 JAR 内置包始终显示在右侧且不可停用；OKCore 在世界加载后才注册数据包，
 *     本会话尚未进入过世界时列表为空（界面会给出提示）</li>
 * </ul>
 * </p>
 * <p>
 *     Data pack selection screen — opened by the DataPack button in {@code MoreTab},
 *     compatible with OKCore's data pack system. Layout mirrors the resource pack
 *     selection screen: the left column lists selectable (deactivated) packs, the right
 *     column the activated ones; the two middle buttons / double-clicking an entry move
 *     packs between columns; a "Done" button at the bottom returns to the
 *     world-creation screen.
 * </p>
 * <p>
 * <ul>
 *     <li>Screen base: the prerequisite mod CatFrame's {@link Screen}; both columns are
 *     UI list classes contained in this screen ({@link AvailablePackList} /
 *     {@link ActivePackList}), extending CatFrame's {@link ObjectSelectionList};
 *     scrolling, clipping and selection highlight are managed by the list base</li>
 *     <li>List background: dark dirt (this mod's options_background_dark.png, tiled) —
 *     see {@link #drawDarkDirt}</li>
 *     <li>Data compatibility: reads and writes go through {@link OKCoreCompat}'s direct
 *     OKCore DatapackManager calls (registry reads, enable/disable live toggles); at
 *     world creation MixinModernCreateWorld flushes the disabled list into the new
 *     world's okcore_datapacks.txt via OKCore's own writer before the integrated server
 *     launches. No OKCore type is referenced in this class</li>
 *     <li>Mod-JAR built-in packs always appear on the right and cannot be disabled;
 *     OKCore registers packs only when a world loads, so the columns are empty until a
 *     world was entered this session (the screen shows a hint)</li>
 * </ul>
 * </p>
 */
public class DataPackScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger(Tags.NAME + ":DataPackScreen");

    /** 深色泥土列表背景纹理（16x16 平铺） / dark dirt list background texture (16x16, tiled) */
    private static final ResourceLocation DARK_DIRT_TEXTURE =
            new ResourceLocation(Tags.MODID, "textures/gui/options_background_dark.png");

    // ===== 布局常量 / Layout constants =====
    private static final int ENTRY_HEIGHT = 36;     // 数据包条目高度（名称行+来源行） / pack entry height (name + source lines)
    private static final int LIST_TOP = 36;         // 列表顶部 Y（标题/表头下方） / list top Y (below title & headers)
    private static final int FOOTER_HEIGHT = 36;    // 底部按钮区高度 / footer button area height
    private static final int MID_COLUMN_WIDTH = 60; // 中间按钮列宽 / middle button column width
    private static final int MAX_LIST_WIDTH = 200;  // 列表最大宽度 / max list width

    // ===== 状态 / State =====
    /** 父界面（创建世界界面） / parent screen (the world-creation screen) */
    private final GuiScreen parentScreen;
    /** OKCore 是否已加载（未加载时显示提示） / whether OKCore is loaded (a hint shows otherwise) */
    private final boolean okCoreLoaded;

    /** 左侧：可选择的（未激活）数据包 / left column: selectable (deactivated) packs */
    private final List<PackInfo> availablePacks = new ArrayList<>();
    /** 右侧：已激活的数据包 / right column: activated packs */
    private final List<PackInfo> activePacks = new ArrayList<>();

    // ===== 组件 / Widgets =====
    private AvailablePackList availableList;
    private ActivePackList activeList;
    private Button activateButton;
    private Button deactivateButton;
    private Button doneButton;

    // ===== 双击检测 / Double-click detection =====
    private PackEntry lastClicked;
    private long lastClickTime;

    // ===== 布局缓存（init 时计算） / cached layout (computed in init) =====
    private int availableX;
    private int activeX;
    private int listWidth;
    private int listTop;
    private int listHeight;
    private int footerTop;

    /**
     * @param parentScreen 父界面（创建世界界面） / parent screen (world-creation screen)
     */
    public DataPackScreen(GuiScreen parentScreen) {
        super(Text.translatable("createworldui.datapacks.title"));
        this.parentScreen = parentScreen;
        this.okCoreLoaded = OKCoreCompat.isLoaded();
        reloadPacks();
    }

    /**
     * <p>
     *     从 OKCore 实时状态（经 {@link OKCoreCompat}）重载两侧列表：未激活的在左，
     *     已激活的在右（模组 JAR 包由 OKCore 保证始终激活）。
     * </p>
     * <p>
     *     Reload both columns from OKCore's live state (via {@link OKCoreCompat}):
     *     deactivated on the left, activated on the right (mod-JAR packs are always
     *     enabled per OKCore).
     * </p>
     */
    private void reloadPacks() {
        availablePacks.clear();
        activePacks.clear();

        for (PackInfo pack : OKCoreCompat.getRegisteredPacks()) {
            if (pack.isEnabled()) {
                activePacks.add(pack);
            } else {
                availablePacks.add(pack);
            }
        }
    }

    // ============================================================
    // 界面生命周期 / Screen lifecycle
    // ============================================================

    @Override
    protected void init() {
        // ===== 计算布局 / Compute layout =====
        this.listTop = LIST_TOP;
        this.footerTop = this.height - FOOTER_HEIGHT;
        int listBottom = this.footerTop - 4;
        this.listHeight = Math.max(ENTRY_HEIGHT, listBottom - this.listTop);

        this.listWidth = Math.min(MAX_LIST_WIDTH, (this.width - MID_COLUMN_WIDTH - 24) / 2);
        this.listWidth = Math.max(100, this.listWidth);
        int totalWidth = this.listWidth * 2 + MID_COLUMN_WIDTH;
        this.availableX = (this.width - totalWidth) / 2;
        this.activeX = this.availableX + this.listWidth + MID_COLUMN_WIDTH;

        // ===== 两侧列表 / The two columns =====
        this.availableList = new AvailablePackList(this.listWidth, this.listHeight, this.listTop, ENTRY_HEIGHT);
        this.availableList.updateSizeAndPosition(this.listWidth, this.listHeight, this.availableX, this.listTop);
        addRenderableWidget(this.availableList);
        this.availableList.rebuild();

        this.activeList = new ActivePackList(this.listWidth, this.listHeight, this.listTop, ENTRY_HEIGHT);
        this.activeList.updateSizeAndPosition(this.listWidth, this.listHeight, this.activeX, this.listTop);
        addRenderableWidget(this.activeList);
        this.activeList.rebuild();

        // ===== 中间移动按钮 / Middle move buttons =====
        int btnX = this.availableX + this.listWidth + (MID_COLUMN_WIDTH - 54) / 2;
        int midY = this.listTop + this.listHeight / 2;
        this.activateButton = Button.builder(
                        Text.translatable("createworldui.datapacks.activate"),
                        btn -> moveSelectedToActive())
                .pos(btnX, midY - 22).width(54).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.activateButton);

        this.deactivateButton = Button.builder(
                        Text.translatable("createworldui.datapacks.deactivate"),
                        btn -> moveSelectedToAvailable())
                .pos(btnX, midY + 2).width(54).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.deactivateButton);

        // ===== 底部完成按钮 / Footer done button =====
        // 切换即时经 OKCoreCompat 直接生效（与 OKCore /datapack 命令一致），此处仅需返回
        // Toggles apply immediately via OKCoreCompat (same as OKCore's /datapack command);
        // this button just returns
        this.doneButton = Button.builder(
                        Text.literal(I18n.format("gui.done")),
                        btn -> this.mc.displayGuiScreen(this.parentScreen))
                .pos(this.width / 2 - 50, this.height - 26).width(100).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.doneButton);

        updateMiddleButtons();
    }

    @Override
    public void tick() {
        // 选中态可能随点击/焦点变化，每 tick 刷新中间按钮可用性
        // Selection can change via clicks/focus; refresh middle-button availability every tick
        updateMiddleButtons();
    }

    /**
     * Esc / 关闭返回父界面（创建世界界面），而不是回到主菜单。
     * Esc / close returns to the parent screen (world-creation screen), not the main menu.
     */
    @Override
    public void onClose() {
        this.mc.displayGuiScreen(this.parentScreen);
    }

    // 显式覆写为 public 并转发基类。编译期 classpath 上的 CatFrame 构件为 reobf(SRG)
    // 命名：其字节码中的 Screen.func_73869_a / func_73864_a 在 javac 视角下不是
    // keyTyped / mouseClicked，接口方法的 public 实现会回落到 GuiScreen 的 protected
    // 同名方法，从而报"正在尝试分配更低的访问权限"编译错误；显式 public 覆写转发基类
    // 即可消除（与 AbstractScreenGameRuleEditor 的处理一致，这两个覆写不可删除）。
    // Explicit public overrides forwarding to super (same treatment as
    // AbstractScreenGameRuleEditor). The CatFrame artifact on the compile classpath is
    // a reobf(SRG) build: its Screen.func_73869_a / func_73864_a are invisible to javac
    // as keyTyped / mouseClicked, so the public interface methods would fall back to
    // GuiScreen's protected ones and fail to compile — do not remove these overrides.
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseScrolled(int delta) {
        super.mouseScrolled(delta);
    }

    // ============================================================
    // 条目移动（经 OKCoreCompat 直接切换 OKCore 状态） / Entry moving (direct OKCore toggles via OKCoreCompat)
    // ============================================================

    /**
     * 中间"激活"按钮：把左侧当前选中条目激活（直接调用 OKCore 的 enableDatapack）。
     * The middle "activate" button: activate the left column's selected entry (direct
     * call to OKCore's enableDatapack).
     */
    private void moveSelectedToActive() {
        PackEntry selected = availableList != null ? availableList.getSelected() : null;
        if (selected != null) {
            OKCoreCompat.enableDatapack(selected.getPack().getName());
            refreshAfterToggle();
        }
    }

    /**
     * 中间"停用"按钮：把右侧当前选中条目停用（直接调用 OKCore 的 disableDatapack，
     * 模组 JAR 包会被 OKCore 拒绝）。
     * The middle "deactivate" button: deactivate the right column's selected entry
     * (direct call to OKCore's disableDatapack; mod-JAR packs are refused by OKCore).
     */
    private void moveSelectedToAvailable() {
        PackEntry selected = activeList != null ? activeList.getSelected() : null;
        if (selected != null) {
            OKCoreCompat.disableDatapack(selected.getPack().getName());
            refreshAfterToggle();
        }
    }

    /**
     * <p>
     *     切换后从 OKCore 实时状态重建两侧列表（enable/disable 直接改 OKCore 的
     *     Datapack 状态，以注册表为准）。
     * </p>
     * <p>
     *     Rebuild both columns from OKCore's live state after a toggle (enable/disable
     *     mutate OKCore's Datapack state directly; the registry is authoritative).
     * </p>
     */
    private void refreshAfterToggle() {
        reloadPacks();
        if (availableList != null) {
            availableList.rebuild();
        }
        if (activeList != null) {
            activeList.rebuild();
        }
        updateMiddleButtons();
    }

    /**
     * 条目点击处理：单击选中（由列表基类完成），双击在两侧间移动。
     * Entry click handling: single click selects (done by the list base),
     * double click moves between columns.
     */
    private void handleEntryClick(PackEntry entry) {
        if (entry == null) {
            return;
        }

        long now = Minecraft.getSystemTime();
        if (entry == lastClicked && now - lastClickTime < 300L) {
            PackInfo pack = entry.getPack();
            if (availablePacks.contains(pack)) {
                OKCoreCompat.enableDatapack(pack.getName());
            } else if (activePacks.contains(pack) && !pack.isModJar()) {
                OKCoreCompat.disableDatapack(pack.getName());
            }
            refreshAfterToggle();
            lastClicked = null;
            lastClickTime = 0L;
        } else {
            lastClicked = entry;
            lastClickTime = now;
        }
    }

    /**
     * 按当前选中态刷新中间按钮可用性（右侧的模组 JAR 包不可停用）。
     * Refresh middle-button availability from the current selection (a right-column
     * mod-JAR pack cannot be deactivated).
     */
    private void updateMiddleButtons() {
        if (activateButton != null) {
            activateButton.setActive(availableList != null && availableList.getSelected() != null);
        }
        if (deactivateButton != null) {
            PackEntry selected = activeList != null ? activeList.getSelected() : null;
            deactivateButton.setActive(selected != null && !selected.getPack().isModJar());
        }
    }

    // ============================================================
    // 渲染 / Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 背景（原版深色泥土）与已注册组件（两侧列表、中间/底部按钮）由基类渲染
        // Background (vanilla dark dirt) and registered widgets (both lists, middle /
        // footer buttons) are rendered by the base class
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 标题与两列表头
        // Title and the two column headers
        this.drawCenteredString(this.fontRendererObj, this.getTitle().getString(),
                this.width / 2, 8, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj, I18n.format("createworldui.datapacks.available"),
                this.availableX + this.listWidth / 2, 26, 0xFFFF55);
        this.drawCenteredString(this.fontRendererObj, I18n.format("createworldui.datapacks.selected"),
                this.activeX + this.listWidth / 2, 26, 0xFFFF55);

        // 提示行：未加载 OKCore，或 OKCore 尚未注册任何数据包（本会话未进入过世界）
        // Hint line: OKCore absent, or OKCore has registered no packs yet (no world
        // entered this session)
        if (!okCoreLoaded) {
            this.drawCenteredString(this.fontRendererObj,
                    I18n.format("createworldui.datapacks.okcore.missing"),
                    this.width / 2, this.footerTop - 12, 0xFFAA00);
        } else if (availablePacks.isEmpty() && activePacks.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj,
                    I18n.format("createworldui.datapacks.notregistered"),
                    this.width / 2, this.footerTop - 12, 0xFF808080);
        }
    }

    /**
     * <p>
     *     深色泥土背景绘制 —— 以 16x16 为一块平铺本模组的 options_background_dark.png
     *     （纹理本身已是深色变体，不再乘 0x404040，参照原版 GuiScreen.drawBackground
     *     的 Tessellator 写法）。供两侧列表的 renderBackground 调用。
     * </p>
     * <p>
     *     Dark dirt background — tiles this mod's options_background_dark.png in 16x16
     *     blocks (the texture is already the dark variant, so no 0x404040 tint is
     *     multiplied; follows vanilla GuiScreen.drawBackground's Tessellator pattern).
     *     Called by both columns' renderBackground.
     * </p>
     */
    private void drawDarkDirt(int x0, int y0, int x1, int y1) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        this.mc.getTextureManager().bindTexture(DARK_DIRT_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        float tile = 16.0F;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(0xFFFFFF);
        tessellator.addVertexWithUV(x0, y1, 0.0D, x0 / tile, y1 / tile);
        tessellator.addVertexWithUV(x1, y1, 0.0D, x1 / tile, y1 / tile);
        tessellator.addVertexWithUV(x1, y0, 0.0D, x1 / tile, y0 / tile);
        tessellator.addVertexWithUV(x0, y0, 0.0D, x0 / tile, y0 / tile);
        tessellator.draw();
    }

    /**
     * 构建某数据包条目的 tooltip 文本（包名 + 来源，以换行符分隔，§ 格式）。
     * Build the tooltip text for a pack entry (name + source, newline-separated,
     * §-formatted).
     */
    private String buildPackTooltipMessage(PackInfo pack) {
        StringBuilder sb = new StringBuilder();
        sb.append(EnumChatFormatting.YELLOW).append(pack.getName());
        sb.append('\n').append(EnumChatFormatting.GRAY)
                .append(I18n.format(pack.isModJar()
                        ? "createworldui.datapacks.source.modjar"
                        : "createworldui.datapacks.source.gamedir"));
        if (pack.isModJar()) {
            sb.append('\n').append(EnumChatFormatting.GRAY)
                    .append(I18n.format("createworldui.datapacks.modjar.tooltip"));
        }
        return sb.toString();
    }

    // ============================================================
    // UI 列表类（包含于本界面，继承 CatFrame 的 ObjectSelectionList）
    // UI list classes (contained in this screen, extending CatFrame's ObjectSelectionList)
    // ============================================================

    /**
     * <p>
     *     两侧数据包列表的公共基类（内部类）—— 继承前置模组 CatFrame 的
     *     {@link ObjectSelectionList}：滚动、滚动条、裁剪（scissor）与选中高亮均由
     *     CatFrame 管理；本类负责深色泥土背景、行几何（左对齐、滚动条留位）、
     *     空列表占位与条目重建。
     * </p>
     * <p>
     *     Common base (inner class) of both columns — extends the prerequisite mod
     *     CatFrame's {@link ObjectSelectionList}: scrolling, scrollbar, scissor clipping
     *     and selection highlight are handled by CatFrame; this class provides the dark
     *     dirt background, row geometry (flush left, scrollbar reserved), the
     *     empty-list placeholder and entry rebuilding.
     * </p>
     */
    private abstract class DataPackList extends ObjectSelectionList<PackEntry> {

        DataPackList(int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        /**
         * 行左对齐列表（不居中），右侧为滚动条预留位置
         * Rows align flush left (not centred); the scrollbar space is reserved on the right
         */
        @Override
        public int getRowLeft() {
            return getX() + 4;
        }

        /**
         * 行宽 = 列表宽 - 20（右侧留出滚动条位置，滚动条经
         * getRowRight()+scrollbarWidth()+2 落在列表内）
         * Row width = list width - 20 (the scrollbar, placed at
         * getRowRight()+scrollbarWidth()+2, then stays inside the list)
         */
        @Override
        public int getRowWidth() {
            return getWidth() - 20;
        }

        /**
         * 列表背景：深色泥土（本模组 options_background_dark.png 平铺），
         * 替换 CatFrame 默认的纯黑填充。
         * List background: dark dirt (this mod's options_background_dark.png, tiled),
         * replacing CatFrame's default solid black fill.
         */
        @Override
        protected void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            drawDarkDirt(getX(), getY(), getRight(), getBottom());
        }

        /**
         * 空列表时绘制居中的占位文字，否则按基类渲染条目。
         * Draw a centred placeholder when the column is empty; otherwise render
         * entries as the base does.
         */
        @Override
        protected void renderListItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            if (getItemCount() == 0) {
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                String empty = I18n.format("createworldui.datapacks.empty");
                font.drawString(empty, getX() + getWidth() / 2 - font.getStringWidth(empty) / 2,
                        getY() + getHeight() / 2 - 4, 0xFF808080);
                return;
            }
            super.renderListItems(graphics, mouseX, mouseY, partialTicks);
        }

        /**
         * 从当前数据重建全部条目。
         * Rebuild all entries from the current data.
         */
        void rebuild() {
            clearEntries();
            for (PackInfo pack : getPacks()) {
                addEntry(new PackEntry(pack), ENTRY_HEIGHT);
            }
            setScrollAmount(0);
        }

        /**
         * @return 本列对应的数据集合 / the data backing this column
         */
        protected abstract List<PackInfo> getPacks();
    }

    /**
     * 左侧列表：可选择的（未激活）数据包。
     * Left column: selectable (deactivated) data packs.
     */
    private class AvailablePackList extends DataPackList {

        AvailablePackList(int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
        }

        @Override
        protected List<PackInfo> getPacks() {
            return availablePacks;
        }
    }

    /**
     * 右侧列表：已激活的数据包。
     * Right column: activated data packs.
     */
    private class ActivePackList extends DataPackList {

        ActivePackList(int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
        }

        @Override
        protected List<PackInfo> getPacks() {
            return activePacks;
        }
    }

    // ============================================================
    // 列表条目（内部类，继承 CatFrame 的 ObjectSelectionList.Entry）
    // List entry (inner class, extending CatFrame's ObjectSelectionList.Entry)
    // ============================================================

    /**
     * <p>
     *     数据包条目 —— 名称行 + 来源行，整行 tooltip；点击交由
     *     {@link #handleEntryClick} 处理（单击选中、双击移动）。
     * </p>
     * <p>
     *     A data pack entry — a name line plus a source line with a whole-row tooltip;
     *     clicks are routed to {@link #handleEntryClick} (single click selects,
     *     double click moves).
     * </p>
     */
    private class PackEntry extends ObjectSelectionList.Entry<PackEntry> {

        private final PackInfo pack;
        private final StringWidget nameWidget;
        private final StringWidget sourceWidget;

        PackEntry(PackInfo pack) {
            this.pack = pack;
            this.nameWidget = new StringWidget(pack.getName(), pack.isModJar() ? 0xFFA0A0A0 : 0xFFFFFF);
            this.sourceWidget = new StringWidget(
                    I18n.format(pack.isModJar()
                            ? "createworldui.datapacks.source.modjar"
                            : "createworldui.datapacks.source.gamedir"),
                    0xFF808080);
            // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 统一延迟绘制
            // Component-level tooltip: auto-pumped by WidgetTooltipHolder via extractRenderState, drawn deferred by CatFrame at end of frame
            this.nameWidget.setTooltip(Tooltip.create(buildPackTooltipMessage(pack)));
        }

        /** @return 本条目的数据包信息 / the pack info of this entry */
        PackInfo getPack() {
            return pack;
        }

        /**
         * 依据条目当前位置同步子控件坐标（名称超长时截断，nameWidget 命中区域扩展至
         * 整行使 tooltip 悬停判定覆盖整行）。
         * Sync child widget coordinates to the entry's current position (long names are
         * trimmed; nameWidget's hit area is expanded to the whole row so the tooltip
         * hover detection covers the full row).
         */
        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;

            String displayName = pack.getName();
            if (pack.isModJar()) {
                displayName = displayName + " " + I18n.format("createworldui.datapacks.modjar");
            }
            displayName = font.trimStringToWidth(displayName, getContentWidth() - 8);
            String source = font.trimStringToWidth(sourceWidget.getText().getString(), getContentWidth() - 8);

            nameWidget.setText(Text.literal(displayName));
            nameWidget.setX(getContentX() + 4);
            nameWidget.setY(getY() + 9);
            nameWidget.setSize(getContentWidth(), getHeight());

            sourceWidget.setText(Text.literal(source));
            sourceWidget.setX(getContentX() + 4);
            sourceWidget.setY(getY() + 22);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameWidget.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
            sourceWidget.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            handleEntryClick(this);
        }
    }
}
