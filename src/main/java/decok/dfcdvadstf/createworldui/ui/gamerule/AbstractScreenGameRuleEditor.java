package decok.dfcdvadstf.createworldui.ui.gamerule;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.ObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.layouts.HorizontalLayout;
import decok.dfcdvadstf.catframe.ui.screens.Screen;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.*;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * <p>
 *     Abstract base class for the game rule editor.<br>
 *     Responsibilities:<br>
 *     - Data source: read all game rules via {@link GameRuleMonitorNSetter} as defaults<br>
 *     - Middle list: render rule entries with the prerequisite mod CatFrame's
 *       {@link ObjectSelectionList}; clipping, scrolling and list sizing are all
 *       managed by the list itself<br>
 *     - Screen base: extends the prerequisite mod CatFrame's {@link Screen}; the
 *       middle list and footer buttons are registered as screen widgets via
 *       {@code addRenderableWidget}, so rendering and event dispatch are handled
 *       uniformly by the base class<br>
 *     - Shared UI: bottom buttons, title, tooltips, etc.<br>
 *     The concrete "save target" is provided by subclasses via
 *     {@link #persistChanges(Map, Set)}:<br>
 *     - {@link WorldCreationGameRuleScreen}: opened in the world-creation screen,
 *       saves as pending rules<br>
 *     - {@link IngameGameRuleScreen}: opened in-game, applies immediately to the
 *       current world
 * </p>
 */
public abstract class AbstractScreenGameRuleEditor extends Screen {

    protected static final Logger LOGGER = LogManager.getLogger("GameRuleEditor");

    // Rule map to be written to applier (key: rule name, value: rule value in string form)
    protected final Map<String, String> editableRules;

    // Default/original rule information (contains multiple data types)
    protected final Map<String, GameruleValue> defaultRules;

    // Temporarily save values modified by user in UI (in string form)
    protected final Map<String, String> modifiedRules = new HashMap<>();

    // Track modified rules (for displaying notifications)
    protected final Set<String> changedRules = new HashSet<>();

    // Middle rule list
    private GameRuleList ruleList;

    protected GuiScreen parentScreen;

    // ===== Layout constants =====
    private static final int ROW_HEIGHT = 25;              // Rule row height
    private static final int CATEGORY_HEADER_HEIGHT = 20;  // Category header height
    private static final int ROW_WIDTH = 308;              // List row width
    private static final int CONTROL_WIDTH = 44;           // Control width
    private static final int CONTROL_HEIGHT = 20;          // Control height
    private static final int LIST_TOP = 40;                // List top Y
    private static final int FOOTER_AREA_HEIGHT = 40;      // Footer area height

    // Current list bounds (computed in initGui)
    private int listTop = LIST_TOP;
    private int listBottom;

    /**
     * Constructor for the game rule editor
     * @param parentScreen Parent screen
     * @param editableRules Editable game rule map
     */
    public AbstractScreenGameRuleEditor(GuiScreen parentScreen, Map<String, String> editableRules) {
        super(Text.translatable("createworldui.gamerules.title"));
        this.parentScreen = parentScreen;

        // Filter out null values, ensure editableRules contains no null
        this.editableRules = new HashMap<>();
        if (editableRules != null) {
            for (Map.Entry<String, String> entry : editableRules.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    this.editableRules.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Save a copy of original rules for comparing which rules were modified
        for (Map.Entry<String, String> e : this.editableRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                this.modifiedRules.put(e.getKey(), e.getValue());
            }
        }

        /**
         * Order for reading default rules:
         * 1) If editableRules is not empty, prefer using its values
         * 2) Otherwise try reading from the real world (if world is not null)
         * 3) Fall back to a new GameRules instance (vanilla defaults) if both fail
         */
        Map<String, GameruleValue> defaultsFromMonitor = null;

        // Method 1: if editableRules is not empty, prefer using its values
        if (!this.editableRules.isEmpty()) {
            defaultsFromMonitor = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : this.editableRules.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    boolean isBoolean = "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
                    int intValue = 0;
                    double doubleValue = 0.0;
                    try { intValue = Integer.parseInt(value); } catch (Exception ignored) {}
                    try { doubleValue = Double.parseDouble(value); } catch (Exception ignored) {}
                    defaultsFromMonitor.put(key, new GameruleValue(value, isBoolean, intValue, doubleValue));
                }
            }
        }

        // Method 2: try getting from the real world
        if (defaultsFromMonitor == null || defaultsFromMonitor.isEmpty()) {
            try {
                World w = Minecraft.getMinecraft() != null ? Minecraft.getMinecraft().theWorld : null;
                if (w != null) {
                    defaultsFromMonitor = GameRuleMonitorNSetter.getAllGamerules(w);
                }
            } catch (Throwable t) {
                LOGGER.warn("Error while trying to get defaults from MonitorNSetter: {}", t.getMessage());
                defaultsFromMonitor = null;
            }
        }

        // Method 3: fall back to a temporary GameRules instance
        if (defaultsFromMonitor == null || defaultsFromMonitor.isEmpty()) {
            defaultsFromMonitor = new LinkedHashMap<>();
            try {
                GameRules temp = new GameRules();
                String[] keys = temp.getRules();
                if (keys != null) {
                    for (String key : keys) {
                        String s = temp.getGameRuleStringValue(key);
                        boolean b = temp.getGameRuleBooleanValue(key);
                        int iv = 0;
                        double dv = 0.0;
                        try { iv = Integer.parseInt(s); } catch (Exception ignored) {}
                        try { dv = Double.parseDouble(s); } catch (Exception ignored) {}
                        defaultsFromMonitor.put(key, new GameruleValue(s, b, iv, dv));
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to build defaults from temporary GameRules: {}", t.getMessage());
            }
        }

        this.defaultRules = (defaultsFromMonitor != null) ? new LinkedHashMap<>(defaultsFromMonitor) : new LinkedHashMap<>();

        // Ensure defaultRules contains all keys from editableRules
        for (String k : this.editableRules.keySet()) {
            if (!this.defaultRules.containsKey(k)) {
                String s = this.editableRules.get(k);
                boolean b = "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
                int iv = 0;
                double dv = 0.0;
                try { iv = Integer.parseInt(s); } catch (Exception ignored) {}
                try { dv = Double.parseDouble(s); } catch (Exception ignored) {}
                this.defaultRules.put(k, new GameruleValue(s, b, iv, dv));
            }
        }
    }

    // ============================================================
    // Save target provided by subclasses
    // ============================================================

    /**
     * Persist the user-modified rules to the concrete target (pending rules / current world).
     *
     * @param result  Full rule result set (String -> String)
     * @param changed Names of rules actually changed vs original
     */
    protected abstract void persistChanges(Map<String, String> result, Set<String> changed);

    // ============================================================
    // Screen lifecycle
    // ============================================================

    @Override
    protected void init() {
        Keyboard.enableRepeatEvents(true);

        // Calculate list bounds
        this.listTop = LIST_TOP;
        int footerAreaTop = this.height - FOOTER_AREA_HEIGHT;
        this.listBottom = footerAreaTop - 4;
        int listHeight = Math.max(ROW_HEIGHT, this.listBottom - this.listTop);

        // Main layout container (Header-Content-Footer three zones)
        HeaderFooterLayout mainLayout = new HeaderFooterLayout(true);
        mainLayout.setFooterHeight(30);

        // Bottom button layout container
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(4);

        // Save button
        Button saveButton;
        // Cancel button
        Button cancelButton;
        if (CreateWorldUI.config.enableResetButton) {
            // Three-button mode
            saveButton = Button.builder(
                Text.translatable("options.save"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(100).build();

            cancelButton = Button.builder(
                Text.literal(I18n.format("gui.cancel")),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(100).build();

            // Reset button
            Button resetButton = Button.builder(
                    Text.translatable("options.cancel"),
                    btn -> {
                        modifiedRules.clear();
                        changedRules.clear();
                        modifiedRules.putAll(editableRules);
                        if (ruleList != null) ruleList.rebuild();
                    }
            ).width(100).build();

            buttonLayout.addChild(saveButton);
            buttonLayout.addChild(cancelButton);
            buttonLayout.addChild(resetButton);

            // Register as screen widgets: rendering and mouse dispatch are handled by the Screen base
            addRenderableWidget(saveButton);
            addRenderableWidget(cancelButton);
            addRenderableWidget(resetButton);
        } else {
            // Two-button mode
            cancelButton = Button.builder(
                Text.translatable("options.save"),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(150).build();

            saveButton = Button.builder(
                Text.translatable("options.cancel"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(150).build();

            buttonLayout.addChild(cancelButton);
            buttonLayout.addChild(saveButton);

            addRenderableWidget(cancelButton);
            addRenderableWidget(saveButton);
        }

        mainLayout.setFooter(buttonLayout);
        mainLayout.recalculate(this.width, this.height);

        // Create middle list and build entries
        this.ruleList = new GameRuleList(this, this.width, listHeight, this.listTop, ROW_HEIGHT);
        addRenderableWidget(this.ruleList);
        this.ruleList.rebuild();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ============================================================
    // Category-ordered list building
    // ============================================================

    /**
     * Build a category-organized rule list.
     *
     * @return Ordered list, "category:" prefix marks category header, others are rule names
     */
    private List<String> buildCategoryOrderedList() {
        List<String> orderedList = new ArrayList<>();
        Set<String> allRules = defaultRules.keySet();

        List<String> categories = GameRuleCategoryRegistry.getAllCategories();

        for (String categoryKey : categories) {
            List<String> rulesInCategory = GameRuleCategoryRegistry.getRulesInCategory(categoryKey);

            List<String> validRules = new ArrayList<>();
            for (String rule : rulesInCategory) {
                if (allRules.contains(rule)) {
                    validRules.add(rule);
                }
            }

            if (!validRules.isEmpty()) {
                orderedList.add("category:" + categoryKey);
                orderedList.addAll(validRules);
            }
        }

        // Add uncategorized rules
        Set<String> categorizedRules = new HashSet<>();
        for (String categoryKey : categories) {
            categorizedRules.addAll(GameRuleCategoryRegistry.getRulesInCategory(categoryKey));
        }

        boolean hasUncategorized = false;
        for (String rule : allRules) {
            if (!categorizedRules.contains(rule)) {
                if (!hasUncategorized) {
                    // The uncategorized header also uses a localization key; display text comes from lang files.
                    // The "category:" prefix is the internal marker rebuild() uses to detect header entries; it must not be omitted
                    orderedList.add("category:gamerule.category.uncategorized");
                    hasUncategorized = true;
                }
                orderedList.add(rule);
            }
        }

        return orderedList;
    }

    // ============================================================
    // Input events
    // ============================================================

    // Mouse click/release/drag and keyboard input are dispatched by the Screen base
    // through the widget tree: dispatchMouseClicked hits the list or a footer button
    // and the event continues inside that widget (list → entry → edit box / cycling
    // button); keyboard input reaches the focused widget via Screen.keyTyped
    // (list → focused entry → edit box).

    /**
     * Mouse wheel: when hovering a boolean rule's cycling button the button consumes
     * it first (cycling the value); otherwise the base class dispatches it to the
     * component under the cursor (list scrolling, etc.).
     */
    @Override
    public void dispatchMouseScrolled(int mouseX, int mouseY, int delta) {
        if (ruleList != null && ruleList.tryScrollCyclingButton(mouseX, mouseY, delta)) {
            return;
        }
        super.dispatchMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // ============================================================
    // Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Background and registered widgets (middle list, footer buttons) rendered by the base
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Title and list header/footer separators: they do not overlap the list/button
        // areas, so drawing them after the widgets covers nothing
        this.drawCenteredString(this.fontRendererObj, this.getTitle().getString(),
            this.width / 2, 15, 0xFFFFFF);

        ContentPanelRenderer.drawHeaderSeparator(0, this.listTop - ContentPanelRenderer.SEPARATOR_HEIGHT, this.width);
        ContentPanelRenderer.drawFooterSeparator(0, this.listBottom, this.width);

        // Tooltip is driven per-entry via the nameLabel widget (WidgetTooltipHolder) and drawn deferred by CatFrame at end of frame
    }

    /**
     * Build the tooltip text for a rule (rule name + default value + description, newline-separated, §-formatted).
     */
    private String buildRuleTooltipMessage(String ruleName) {
        StringBuilder sb = new StringBuilder();
        // First line: rule name (yellow)
        sb.append(EnumChatFormatting.YELLOW).append(ruleName);

        // Default value
        GameruleValue defVal = defaultRules.get(ruleName);
        if (defVal != null) {
            sb.append('\n').append(EnumChatFormatting.GRAY)
                .append(Text.translatableString("createworldui.customize.custom.default"))
                .append(' ').append(defVal.getOptimalValue());
        }

        // Description (if any)
        String tooltip = getRuleTooltip(ruleName);
        if (tooltip != null) {
            sb.append('\n').append(EnumChatFormatting.WHITE).append(tooltip);
        }
        return sb.toString();
    }

    // ============================================================
    // Data helper methods
    // ============================================================

    /**
     * Record a user modification to a rule.
     */
    private void setRuleValue(String ruleName, String value) {
        modifiedRules.put(ruleName, value);
        changedRules.add(ruleName);
    }

    /**
     * Handle edit-box rule editing: infer type from default value then store as string.
     */
    private void onValueRuleEdited(String ruleName, String rawText) {
        GameruleValue def = defaultRules.get(ruleName);
        Object parsed = def != null ? parseFromString(rawText, def.getOptimalValue()) : rawText;
        modifiedRules.put(ruleName, String.valueOf(parsed));
        changedRules.add(ruleName);
    }

    private boolean isHighlightEnabled() {
        return CreateWorldUI.config != null && CreateWorldUI.config.highlightModifiedRulesInGUI;
    }

    /**
     * Check if a rule has been modified (different from original value).
     */
    private boolean isRuleModified(String ruleName) {
        String currentValue = modifiedRules.get(ruleName);
        String originalValue = editableRules.get(ruleName);

        if (currentValue == null && originalValue == null) {
            return false;
        }
        if (currentValue == null || originalValue == null) {
            return true;
        }
        return !currentValue.equals(originalValue);
    }

    private String getRuleTooltip(String ruleName) {
        return GameRuleTooltipRegistry.getTooltip(ruleName);
    }

    /**
     * Parse a string to a type matching the reference value.
     */
    private Object parseFromString(String text, Object originalValue) {
        if (originalValue instanceof Boolean) {
            return Boolean.parseBoolean(text);
        }
        if (originalValue instanceof Integer) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                LOGGER.error("Because of {}, this type of integer will be ignored", ignored.getMessage());
            }
        }
        if (originalValue instanceof Double) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                LOGGER.error("Because of {}, this type of double will be ignored", ignored.getMessage());
            }
        }
        return text;
    }

    // ============================================================
    // Saving
    // ============================================================

    /**
     * Collect user modifications, delegate persistence to subclass, then show notification.
     */
    protected final void saveChanges() {
        LOGGER.info("saveChanges() called");

        Map<String, String> result = new HashMap<>();
        changedRules.clear();

        for (Map.Entry<String, String> e : modifiedRules.entrySet()) {
            String ruleName = e.getKey();
            String newValue = e.getValue();

            if (ruleName != null && newValue != null) {
                result.put(ruleName, newValue);

                String originalValue = editableRules.get(ruleName);
                if (originalValue == null || !originalValue.equals(newValue)) {
                    changedRules.add(ruleName);
                }
            }
        }

        // Subclass decides the save target
        try {
            persistChanges(result, changedRules);
        } catch (Exception ex) {
            LOGGER.error("Failed to persist game rules: {}", ex.getMessage());
        }

        showSaveNotification();
    }

    /**
     * Show a chat notification of the save result.
     */
    private void showSaveNotification() {
        if (!changedRules.isEmpty()) {
            String notificationText = I18n.format("createworldui.gamerules.notification.changed");
            String rulesList = String.join(", ", changedRules);

            String message = getString(notificationText, rulesList);

            if (Minecraft.getMinecraft().ingameGUI != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message));
            }
            LOGGER.info("Changed rules: {}", changedRules);
        } else {
            String message = I18n.format("createworldui.gamerules.notification.noChanges");
            if (Minecraft.getMinecraft().ingameGUI != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(
                    new ChatComponentText(EnumChatFormatting.WHITE + message));
            }
        }
    }

    @Nonnull
    private static String getString(String notificationText, String rulesList) {
        String message;
        if (CreateWorldUI.config != null && CreateWorldUI.config.changedRulesInChatHighLighted) {
            // Highlight mode: text white, rule names yellow
            message = EnumChatFormatting.WHITE + notificationText + EnumChatFormatting.YELLOW + rulesList;
        } else {
            // Default mode: all white
            message = EnumChatFormatting.WHITE + notificationText + EnumChatFormatting.WHITE + rulesList;
        }
        return message;
    }

    // ============================================================
    // Middle list
    // ============================================================

    private static class GameRuleList extends ObjectSelectionList<RuleEntry> {

        private final AbstractScreenGameRuleEditor screen;

        GameRuleList(AbstractScreenGameRuleEditor screen, int width, int height, int y, int itemHeight) {
            super(width, height, y, itemHeight);
            this.screen = screen;
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        protected boolean entriesCanBeSelected() {
            // 不为整行绘制选中高亮框 / Do not draw a selection highlight box for whole rows
            return false;
        }

        /**
         * Rebuild all entries from the current data.
         */
        void rebuild() {
            clearEntries();

            List<String> ordered = screen.buildCategoryOrderedList();
            for (String item : ordered) {
                if (item.startsWith("category:")) {
                    addEntry(new CategoryEntry(screen, item.substring(9)), CATEGORY_HEADER_HEIGHT);
                    continue;
                }

                GameruleValue value = screen.defaultRules.get(item);
                if (value == null) {
                    LOGGER.warn("GameruleValue for {} is null, skipping", item);
                    continue;
                }

                String stringValue = screen.modifiedRules.containsKey(item) ? screen.modifiedRules.get(item)
                    : screen.editableRules.getOrDefault(item, null);
                Object displayObj = stringValue != null
                    ? screen.parseFromString(stringValue, value.getOptimalValue())
                    : value.getOptimalValue();

                if (displayObj instanceof Boolean) {
                    addEntry(new BooleanRuleEntry(screen, item, (Boolean) displayObj), ROW_HEIGHT);
                } else {
                    String initial = stringValue != null ? stringValue : String.valueOf(displayObj);
                    addEntry(new ValueRuleEntry(screen, item, initial), ROW_HEIGHT);
                }
            }

            setScrollAmount(0);
        }

        /**
         * If the mouse hovers a boolean rule's cycling button, use the wheel to cycle its value.
         *
         * @param delta normalised wheel delta (only the sign matters)
         * @return whether it was consumed by a cycling button
         */
        boolean tryScrollCyclingButton(int mouseX, int mouseY, int delta) {
            for (RuleEntry entry : children()) {
                if (entry instanceof BooleanRuleEntry) {
                    CyclingButton<Boolean> toggle = ((BooleanRuleEntry) entry).getToggle();
                    if (toggle.isMouseOver(mouseX, mouseY)) {
                        toggle.mouseScrolled(delta);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Base class for list entries.
     */
    private abstract static class RuleEntry extends ObjectSelectionList.Entry<RuleEntry> {

        protected final AbstractScreenGameRuleEditor screen;

        RuleEntry(AbstractScreenGameRuleEditor screen) {
            this.screen = screen;
        }

        /** @return rule name, category header returns null */
        String getRuleName() {
            return null;
        }
    }

    /**
     * Category header entry — displays the category name centered.
     */
    private static class CategoryEntry extends RuleEntry {

        private final String display;

        CategoryEntry(AbstractScreenGameRuleEditor screen, String categoryKey) {
            super(screen);
            this.display = GameRuleCategoryRegistry.getCategoryDisplayName(categoryKey);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textX = getX() + getWidth() / 2 - font.getStringWidth(display) / 2;
            int textY = getContentYMiddle() - font.FONT_HEIGHT / 2;
            font.drawStringWithShadow(display, textX, textY, 0xFFFF55);
        }
    }

    /**
     * Boolean rule entry — rule name + cycling on/off button.
     */
    private static class BooleanRuleEntry extends RuleEntry {

        private final String ruleName;
        private final StringWidget nameLabel;
        private final CyclingButton<Boolean> toggle;

        BooleanRuleEntry(AbstractScreenGameRuleEditor screen, String ruleName, boolean initialValue) {
            super(screen);
            this.ruleName = ruleName;
            this.nameLabel = new StringWidget(GameRuleNameRegistry.getName(ruleName), 0xFFFFFF);
            // Component-level tooltip: auto-pumped by WidgetTooltipHolder driven from extractRenderState (hover/focus + delay)
            this.nameLabel.setTooltip(Tooltip.create(screen.buildRuleTooltipMessage(ruleName)));
            this.toggle = CyclingButton.onOffBuilder()
                .values(true, false)
                .initially(initialValue)
                .label(Text.literal(""))
                .useVanillaTexture(false)
                .build(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT,
                    (btn, newVal) -> screen.setRuleValue(ruleName, String.valueOf(newVal)));
        }

        /** Sync child widget coordinates to the entry's current position */
        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            nameLabel.setX(getContentX());
            nameLabel.setY(getContentYMiddle() - font.FONT_HEIGHT / 2);
            // Expand nameLabel's hit area to the whole row (text is still drawn at x/y,
            // unaffected by size): component-level tooltip hover detection covers the full row
            nameLabel.setSize(getContentWidth(), getHeight());
            toggle.setX(getContentRight() - CONTROL_WIDTH);
            toggle.setY(getContentYMiddle() - CONTROL_HEIGHT / 2);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameLabel.setColor((screen.isHighlightEnabled() && screen.isRuleModified(ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            nameLabel.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
            toggle.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            layoutWidgets();
            toggle.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        String getRuleName() {
            return ruleName;
        }

        CyclingButton<Boolean> getToggle() {
            return toggle;
        }
    }

    /**
     * Value rule entry — rule name + text input box.
     */
    private static class ValueRuleEntry extends RuleEntry {

        private final String ruleName;
        private final StringWidget nameLabel;
        private final SimpleEditBox editBox;

        ValueRuleEntry(AbstractScreenGameRuleEditor screen, String ruleName, String initialValue) {
            super(screen);
            this.ruleName = ruleName;
            this.nameLabel = new StringWidget(GameRuleNameRegistry.getName(ruleName), 0xFFFFFF);
            this.nameLabel.setTooltip(Tooltip.create(screen.buildRuleTooltipMessage(ruleName)));
            this.editBox = new SimpleEditBox(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT);
            this.editBox.setText(initialValue);
            this.editBox.setMaxLength(200);
            this.editBox.setUseVanillaTexture(false);
            this.editBox.setForceVerticalCursor(true);
        }

        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            nameLabel.setX(getContentX());
            nameLabel.setY(getContentYMiddle() - font.FONT_HEIGHT / 2);
            nameLabel.setSize(getContentWidth(), getHeight());
            editBox.setX(getContentRight() - CONTROL_WIDTH);
            editBox.setY(getContentYMiddle() - CONTROL_HEIGHT / 2);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameLabel.setColor((screen.isHighlightEnabled() && screen.isRuleModified(ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            nameLabel.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
            editBox.extractRenderState(GuiGraphicsExtractor.getInstance(), mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            layoutWidgets();
            editBox.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (editBox.isFocused()) {
                editBox.keyTyped(typedChar, keyCode);
                screen.onValueRuleEdited(ruleName, editBox.getText());
            }
        }

        @Override
        public boolean isFocused() {
            return editBox.isFocused();
        }

        @Override
        public void setFocused(boolean focused) {
            if (!focused) {
                editBox.setFocused(false);
            }
        }

        @Override
        String getRuleName() {
            return ruleName;
        }
    }
}
