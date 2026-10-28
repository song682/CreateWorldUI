package decok.dfcdvadstf.createworldui.ui.experiment;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.ObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.createworldui.api.ExperimentState;
import decok.dfcdvadstf.createworldui.api.IExperimentScreenUsage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *     实验性内容界面 —— 由 {@code MoreTab} 的实验按钮打开，样式对齐高版本
 *     {@code ExperimentsScreen}：标题 + 红色警告行 + 可滚动的开关列表 + 底部
 *     完成/取消。每个开关对应一个由其他模组经 {@link IExperimentScreenUsage}
 *     注册的实验（枚举常量即注册单元，见 {@link IExperimentScreenUsage#registered()}）。
 * </p>
 * <p>
 *     职责边界：本界面<b>只切换并记录启用状态</b>（经 {@link ExperimentState}），
 *     实验究竟做什么由提供它的模组作者负责；界面本身不执行任何实验逻辑，也不写盘。
 *     完成时保留当前选择返回父界面；取消时把 {@link ExperimentState} 回滚到进入
 *     界面时的快照再返回。
 * </p>
 * <p>
 *     Experiment screen — opened by the experiments button in {@code MoreTab}, styled after
 *     the high-version {@code ExperimentsScreen}: title + red warning line + a scrollable
 *     switch list + a Done/Cancel footer. Each switch is one experiment registered by another
 *     mod through {@link IExperimentScreenUsage} (the enum constant is the registration unit;
 *     see {@link IExperimentScreenUsage#registered()}).
 * </p>
 * <p>
 *     Responsibility boundary: this screen <b>only toggles and records the enabled state</b>
 *     (via {@link ExperimentState}); what an experiment actually does is the providing author
 *     mod's job — the screen performs no experiment logic and persists nothing itself. Done
 *     keeps the current selection and returns to the parent; Cancel rolls
 *     {@link ExperimentState} back to the snapshot taken on open, then returns.
 * </p>
 */
public class ExperimentScreen extends Screen {

    // ===== 布局常量 / Layout constants =====
    /** 列表行宽（约等于高版本 ExperimentsScreen 的 310 内容宽度） / list row width (~the high-version 310 content width) */
    private static final int ROW_WIDTH = 300;
    /** 单个实验条目高度 / height of one experiment row */
    private static final int ROW_HEIGHT = 24;
    /** 开关控件尺寸 / toggle control size */
    private static final int CONTROL_WIDTH = 44;
    private static final int CONTROL_HEIGHT = 20;
    /** 列表顶部 Y（标题与警告行下方） / list top Y (below title & warning line) */
    private static final int LIST_TOP = 48;
    /** 底部按钮区高度 / footer button area height */
    private static final int FOOTER_HEIGHT = 36;

    /** 父界面（创建世界界面） / parent screen (the world-creation screen) */
    private final GuiScreen parentScreen;
    /** 进入界面时的状态快照，用于取消回滚 / state snapshot on open, used to revert on Cancel */
    private final Map<String, Boolean> entrySnapshot;

    // ===== 组件 / Widgets =====
    private ExperimentList experimentList;
    private Button doneButton;
    private Button cancelButton;

    /**
     * @param parentScreen 父界面（创建世界界面） / parent screen (world-creation screen)
     */
    public ExperimentScreen(GuiScreen parentScreen) {
        // 高版本 ExperimentsScreen 的标题键为 selectWorld.experiments
        // High-version ExperimentsScreen uses the selectWorld.experiments title key
        super(Text.translatable("selectWorld.experiments"));
        this.parentScreen = parentScreen;
        // 记录进入时的启用状态快照，取消时回滚（完成则保留） / snapshot on entry; reverted on Cancel, kept on Done
        this.entrySnapshot = ExperimentState.snapshot();
    }

    // ============================================================
    // 界面生命周期 / Screen lifecycle
    // ============================================================

    @Override
    protected void init() {
        int listBottom = this.height - FOOTER_HEIGHT - 4;
        int listHeight = Math.max(ROW_HEIGHT, listBottom - LIST_TOP);

        // ===== 开关列表 / The switch list =====
        this.experimentList = new ExperimentList(this.width, listHeight, LIST_TOP, ROW_HEIGHT);
        addRenderableWidget(this.experimentList);
        this.experimentList.rebuild();

        // ===== 底部按钮（完成 / 取消，居中并排） / Footer (Done / Cancel, centred side by side) =====
        int buttonWidth = 100;
        int spacing = 4;
        int leftX = this.width / 2 - (buttonWidth + spacing);
        int rightX = this.width / 2 + spacing;
        int buttonY = this.height - 26;

        this.doneButton = Button.builder(
                        Text.literal(I18n.format("gui.done")),
                        btn -> this.mc.displayGuiScreen(this.parentScreen))
                .pos(leftX, buttonY).width(buttonWidth).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.doneButton);

        this.cancelButton = Button.builder(
                        Text.literal(I18n.format("gui.cancel")),
                        btn -> {
                            // 取消：回滚到进入界面时的快照 / Cancel: revert to the entry snapshot
                            ExperimentState.restore(this.entrySnapshot);
                            this.mc.displayGuiScreen(this.parentScreen);
                        })
                .pos(rightX, buttonY).width(buttonWidth).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.cancelButton);
    }

    /**
     * Esc / 关闭返回父界面（创建世界界面），而不是回到主菜单。保留当前选择
     * （与高版本一致：离开界面不回滚，仅取消按钮显式回滚）。
     * Esc / close returns to the parent screen (world-creation screen), not the main menu.
     * The current selection is kept (matching the high version: leaving the screen does not
     * revert; only the Cancel button explicitly reverts).
     */
    @Override
    public void onClose() {
        this.mc.displayGuiScreen(this.parentScreen);
    }

    // 显式覆写为 public 并转发基类。编译期 classpath 上的 CatFrame 构件为 reobf(SRG)
    // 命名：其字节码中的 Screen.func_73869_a / func_73864_a 在 javac 视角下不是
    // keyTyped / mouseClicked，接口方法的 public 实现会回落到 GuiScreen 的 protected
    // 同名方法，从而报"正在尝试分配更低的访问权限"编译错误；显式 public 覆写转发基类
    // 即可消除（与 DataPackScreen / AbstractScreenGameRuleEditor 的处理一致，不可删除）。
    // Explicit public overrides forwarding to super (same treatment as DataPackScreen /
    // AbstractScreenGameRuleEditor): the reobf(SRG) CatFrame artifact hides Screen's
    // keyTyped / mouseClicked from javac, so the public interface methods would fall back to
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
    // 渲染 / Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 背景与已注册组件（开关列表、底部按钮）由基类渲染
        // Background and registered widgets (the list, footer buttons) are rendered by the base
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 标题（居中白色）与红色警告行（居中），对应高版本的 TITLE / INFO
        // Title (centred white) and the red info line (centred), mirroring the high version's TITLE / INFO
        this.drawCenteredString(this.fontRendererObj, this.getTitle().getString(),
                this.width / 2, 12, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj,
                EnumChatFormatting.RED + I18n.format("selectWorld.experiments.info"),
                this.width / 2, 28, 0xFFFFFF);
    }

    /**
     * 构建某实验条目的 tooltip 文本（描述，灰色）。
     * Build the tooltip text for an experiment row (its description, grey).
     */
    private String buildExperimentTooltip(IExperimentScreenUsage experiment) {
        String description = experiment.getDescription();
        if (description == null || description.isEmpty()) {
            return EnumChatFormatting.GRAY + experiment.getTitle();
        }
        return EnumChatFormatting.YELLOW + experiment.getTitle()
                + "\n" + EnumChatFormatting.GRAY + description;
    }

    // ============================================================
    // 开关列表（继承 CatFrame 的 ObjectSelectionList） / Switch list (CatFrame ObjectSelectionList)
    // ============================================================

    /**
     * <p>
     *     实验开关列表 —— 基于前置模组 CatFrame 的 {@link ObjectSelectionList}：滚动、
     *     滚动条与裁剪由基类管理；本类负责行几何（左对齐、滚动条留位）、条目重建与
     *     空列表提示。
     * </p>
     * <p>
     *     Experiment switch list on top of the prerequisite mod CatFrame's
     *     {@link ObjectSelectionList}: scrolling, scrollbar and clipping are handled by the
     *     base; this class owns row geometry (flush left, scrollbar reserved), entry rebuild
     *     and the empty-list hint.
     * </p>
     */
    private class ExperimentList extends ObjectSelectionList<ExperimentEntry> {

        ExperimentList(int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        /** 行左对齐列表（不居中），右侧为滚动条预留位置 / rows flush left; scrollbar space reserved on the right */
        @Override
        public int getRowLeft() {
            return getX() + (getWidth() - ROW_WIDTH) / 2 + 4;
        }

        /** 行宽 = 内容宽（右侧留出滚动条位置） / row width = content width (scrollbar stays to the right) */
        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        /** 不为整行绘制选中高亮框 / do not draw a whole-row selection highlight */
        @Override
        protected boolean entriesCanBeSelected() {
            return false;
        }

        /**
         * 空列表时绘制居中的占位文字，否则按基类渲染条目。
         * Draw a centred placeholder when nothing is registered; otherwise render entries.
         */
        @Override
        protected void renderListItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            if (getItemCount() == 0) {
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                String empty = I18n.format("createworldui.experiments.empty");
                font.drawString(empty, getX() + getWidth() / 2 - font.getStringWidth(empty) / 2,
                        getY() + getHeight() / 2 - 4, 0xFF808080);
                return;
            }
            super.renderListItems(graphics, mouseX, mouseY, partialTicks);
        }

        /**
         * 从当前注册表重建全部条目。初始开关态取 {@link ExperimentState#isEnabled(String, boolean)}。
         * Rebuild all entries from the current registry; the initial toggle value comes from
         * {@link ExperimentState#isEnabled(String, boolean)}.
         */
        void rebuild() {
            clearEntries();
            List<IExperimentScreenUsage> experiments = IExperimentScreenUsage.registered();
            for (IExperimentScreenUsage experiment : experiments) {
                boolean initial = ExperimentState.isEnabled(experiment.getID(), experiment.defaultEnabled());
                addEntry(new ExperimentEntry(experiment, initial), ROW_HEIGHT);
            }
            setScrollAmount(0);
        }
    }

    // ============================================================
    // 列表条目（继承 ObjectSelectionList.Entry） / List entry (ObjectSelectionList.Entry)
    // ============================================================

    /**
     * 实验条目 —— 名称标签（带描述 tooltip） + 开关按钮；切换即时写入 {@link ExperimentState}。
     * Experiment row — a name label (with a description tooltip) plus an on/off toggle;
     * toggling writes straight through to {@link ExperimentState}.
     */
    private class ExperimentEntry extends ObjectSelectionList.Entry<ExperimentEntry> {

        private final IExperimentScreenUsage experiment;
        private final StringWidget nameLabel;
        private final CyclingButton<Boolean> toggle;

        ExperimentEntry(IExperimentScreenUsage experiment, boolean initialValue) {
            this.experiment = experiment;
            this.nameLabel = new StringWidget(experiment.getTitle(), 0xFFFFFF);
            // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 延迟绘制
            // Component-level tooltip: auto-pumped by WidgetTooltipHolder via extractRenderState, drawn deferred by CatFrame
            this.nameLabel.setTooltip(Tooltip.create(buildExperimentTooltip(experiment)));
            this.toggle = CyclingButton.onOffBuilder()
                    .values(true, false)
                    .initially(initialValue)
                    .label(Text.literal(""))
                    .useVanillaTexture(false)
                    .build(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT,
                            (btn, newVal) -> ExperimentState.setEnabled(experiment.getID(), newVal.booleanValue()));
        }

        /**
         * 依据条目当前位置同步子控件坐标（名称超长截断，nameLabel 命中区域扩展至整行，
         * 使 tooltip 悬停判定覆盖整行）。
         * Sync child widget coordinates to the row position (long names trimmed; nameLabel's
         * hit area expanded to the whole row so tooltip hover detection covers the full row).
         */
        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String displayName = font.trimStringToWidth(experiment.getTitle(), getContentWidth() - CONTROL_WIDTH - 12);
            this.nameLabel.setText(Text.literal(displayName));
            this.nameLabel.setX(getContentX() + 4);
            this.nameLabel.setY(getContentYMiddle() - font.FONT_HEIGHT / 2);
            this.nameLabel.setSize(getContentWidth(), getHeight());

            this.toggle.setX(getContentRight() - CONTROL_WIDTH);
            this.toggle.setY(getContentYMiddle() - CONTROL_HEIGHT / 2);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameLabel.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
            toggle.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            layoutWidgets();
            toggle.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
}
