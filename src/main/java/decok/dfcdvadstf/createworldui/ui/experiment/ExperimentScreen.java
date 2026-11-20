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
    /** list row width (~the high-version 310 content width) */
    private static final int ROW_WIDTH = 300;
    /** height of one experiment row */
    private static final int ROW_HEIGHT = 24;
    /** toggle control size */
    private static final int CONTROL_WIDTH = 44;
    private static final int CONTROL_HEIGHT = 20;
    /** list top Y (below title & warning line) */
    private static final int LIST_TOP = 48;
    /** footer button area height */
    private static final int FOOTER_HEIGHT = 36;

    /** parent screen (the world-creation screen) */
    private final GuiScreen parentScreen;
    /** state snapshot on open, used to revert on Cancel */
    private final Map<String, Boolean> entrySnapshot;

    // ===== widgets =====
    private ExperimentList experimentList;
    private Button doneButton;
    private Button cancelButton;

    /**
     * @param parentScreen parent screen (world-creation screen)
     */
    public ExperimentScreen(GuiScreen parentScreen) {
        // High-version ExperimentsScreen uses the selectWorld.experiments title key
        super(Text.translatable("selectWorld.experiments"));
        this.parentScreen = parentScreen;
        // Snapshot on entry; reverted on Cancel, kept on Done
        this.entrySnapshot = ExperimentState.snapshot();
    }

    // ============================================================
    // Screen lifecycle
    // ============================================================

    @Override
    protected void init() {
        int listBottom = this.height - FOOTER_HEIGHT - 4;
        int listHeight = Math.max(ROW_HEIGHT, listBottom - LIST_TOP);

        // ===== The switch list =====
        this.experimentList = new ExperimentList(this.width, listHeight, LIST_TOP, ROW_HEIGHT);
        addRenderableWidget(this.experimentList);
        this.experimentList.rebuild();

        // ===== Footer (Done / Cancel, centred side by side) =====
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
                            // Cancel: revert to the entry snapshot
                            ExperimentState.restore(this.entrySnapshot);
                            this.mc.displayGuiScreen(this.parentScreen);
                        })
                .pos(rightX, buttonY).width(buttonWidth).height(20).useVanillaTexture(true).build();
        addRenderableWidget(this.cancelButton);
    }

    /**
     * Esc / close returns to the parent screen (world-creation screen), not the main menu.
     * The current selection is kept (matching the high version: leaving the screen does not
     * revert; only the Cancel button explicitly reverts).
     */
    @Override
    public void onClose() {
        this.mc.displayGuiScreen(this.parentScreen);
    }

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
    // Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Background and registered widgets (the list, footer buttons) are rendered by the base
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Title (centred white) and the red info line (centred), mirroring the high version's TITLE / INFO
        this.drawCenteredString(this.fontRendererObj, this.getTitle().getString(),
                this.width / 2, 12, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj,
                EnumChatFormatting.RED + I18n.format("selectWorld.experiments.info"),
                this.width / 2, 28, 0xFFFFFF);
    }

    /**
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
    // Switch list
    // ============================================================

    private class ExperimentList extends ObjectSelectionList<ExperimentEntry> {

        ExperimentList(int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        /** rows flush left; scrollbar space reserved on the right */
        @Override
        public int getRowLeft() {
            return getX() + (getWidth() - ROW_WIDTH) / 2 + 4;
        }

        /** row width = content width (scrollbar stays to the right) */
        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        /** do not draw a whole-row selection highlight */
        @Override
        protected boolean entriesCanBeSelected() {
            return false;
        }

        /**
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
    // List entry (ObjectSelectionList.Entry)
    // ============================================================

    private class ExperimentEntry extends ObjectSelectionList.Entry<ExperimentEntry> {

        private final IExperimentScreenUsage experiment;
        private final StringWidget nameLabel;
        private final CyclingButton<Boolean> toggle;

        ExperimentEntry(IExperimentScreenUsage experiment, boolean initialValue) {
            this.experiment = experiment;
            this.nameLabel = new StringWidget(experiment.getTitle(), 0xFFFFFF);
            this.nameLabel.setTooltip(Tooltip.create(buildExperimentTooltip(experiment)));
            this.toggle = CyclingButton.onOffBuilder()
                    .values(true, false)
                    .initially(initialValue)
                    .label(Text.literal(""))
                    .useVanillaTexture(false)
                    .build(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT,
                            (btn, newVal) -> ExperimentState.setEnabled(experiment.getID(), newVal.booleanValue()));
        }

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
