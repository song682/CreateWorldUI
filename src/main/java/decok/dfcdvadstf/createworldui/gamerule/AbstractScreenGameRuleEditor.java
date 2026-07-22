package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.ObjectSelectionList;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.layouts.HeaderFooterLayout;
import decok.dfcdvadstf.catframe.ui.layouts.HorizontalLayout;
import decok.dfcdvadstf.catframe.ui.layouts.ILayout;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.Tags;
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
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * <p>
 *     游戏规则编辑器的抽象基类。<br>
 *     本类负责：<br>
 *     - 数据来源：通过{@link GameRuleMonitorNSetter}读取所有游戏规则作为默认值<br>
 *     - 中间列表：使用前置模组 CatFrame 的 {@link ObjectSelectionList} 渲染规则条目，
 *       裁剪、滚动和列表尺寸均由列表自身管理<br>
 *     - 底部按钮、标题、tooltip 等公共界面元素<br>
 *     具体的"保存目标"由子类通过 {@link #persistChanges(Map, Set)} 实现：<br>
 *     - {@link WorldCreationGameRuleScreen}：在创建世界界面打开，保存为待应用规则<br>
 *     - {@link IngameGameRuleScreen}：在游戏内打开，立即应用到当前世界
 * </p>
 * <p>
 *     Abstract base class for the game rule editor.<br>
 *     Responsibilities:<br>
 *     - Data source: read all game rules via {@link GameRuleMonitorNSetter} as defaults<br>
 *     - Middle list: render rule entries with the prerequisite mod CatFrame's
 *       {@link ObjectSelectionList}; clipping, scrolling and list sizing are all
 *       managed by the list itself<br>
 *     - Shared UI: bottom buttons, title, tooltips, etc.<br>
 *     The concrete "save target" is provided by subclasses via
 *     {@link #persistChanges(Map, Set)}:<br>
 *     - {@link WorldCreationGameRuleScreen}: opened in the world-creation screen,
 *       saves as pending rules<br>
 *     - {@link IngameGameRuleScreen}: opened in-game, applies immediately to the
 *       current world
 * </p>
 */
public abstract class AbstractScreenGameRuleEditor extends GuiScreen {

    protected static final Logger LOGGER = LogManager.getLogger("GameRuleEditor");

    // 待写入应用器的规则映射（键：规则名，值：字符串形式的规则值）
    // Rule map to be written to applier (key: rule name, value: rule value in string form)
    protected final Map<String, String> editableRules;

    // 默认/原始规则信息（包含多种数据类型）
    // Default/original rule information (contains multiple data types)
    protected final Map<String, GameruleValue> defaultRules;

    // 临时保存用户在UI中修改的值（字符串形式）
    // Temporarily save values modified by user in UI (in string form)
    protected final Map<String, String> modifiedRules = new HashMap<>();

    // 跟踪已修改的规则（用于显示通知）
    // Track modified rules (for displaying notifications)
    protected final Set<String> changedRules = new HashSet<>();

    private Button saveButton;   // 保存按钮 / Save button
    private Button cancelButton;  // 取消按钮 / Cancel button
    private Button resetButton;   // 重置按钮 / Reset button

    // 主布局容器（Header-Content-Footer三区域）/ Main layout container (Header-Content-Footer three zones)
    private HeaderFooterLayout mainLayout;
    // 底部按钮布局容器 / Bottom button layout container
    private HorizontalLayout buttonLayout;

    // 中间规则列表（由 CatFrame 的 ObjectSelectionList 实现）
    // Middle rule list (implemented by CatFrame's ObjectSelectionList)
    private GameRuleList ruleList;

    protected GuiScreen parentScreen; // 父界面 / Parent screen

    // ===== 布局常量 / Layout constants =====
    private static final int ROW_HEIGHT = 25;              // 规则行高 / Rule row height
    private static final int CATEGORY_HEADER_HEIGHT = 20;  // 分类标题高度 / Category header height
    private static final int ROW_WIDTH = 308;              // 列表行宽 / List row width
    private static final int CONTROL_WIDTH = 44;           // 控件宽度 / Control width
    private static final int CONTROL_HEIGHT = 20;          // 控件高度 / Control height
    private static final int LIST_TOP = 40;                // 列表顶部 Y / List top Y
    private static final int FOOTER_AREA_HEIGHT = 40;      // 底部按钮区高度 / Footer area height

    // 当前列表边界（在 initGui 中根据窗口尺寸计算） / Current list bounds (computed in initGui)
    private int listTop = LIST_TOP;
    private int listBottom;

    /**
     * 构造游戏规则编辑器<br>
     * Constructor for the game rule editor
     * @param parentScreen 父界面 / Parent screen
     * @param editableRules 可编辑的游戏规则映射 / Editable game rule map
     */
    public AbstractScreenGameRuleEditor(GuiScreen parentScreen, Map<String, String> editableRules) {
        this.parentScreen = parentScreen;

        // 过滤掉 null 值，确保 editableRules 不包含 null
        // Filter out null values, ensure editableRules contains no null
        this.editableRules = new HashMap<>();
        if (editableRules != null) {
            for (Map.Entry<String, String> entry : editableRules.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    this.editableRules.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // 保存原始规则的副本，用于比较哪些规则被修改了
        // Save a copy of original rules for comparing which rules were modified
        for (Map.Entry<String, String> e : this.editableRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                this.modifiedRules.put(e.getKey(), e.getValue());
            }
        }

        /*
         * 读取默认规则的顺序：
         * 1) 如果 editableRules 不为空，优先使用其中的值构建默认规则
         * 2) 否则尝试从真实世界读取（如果世界不为 null）
         * 3) 如果都失败，回退到新的 GameRules 实例（使用原版默认值）
         *
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
    // 保存目标由子类实现 / Save target provided by subclasses
    // ============================================================

    /**
     * 将用户修改后的规则持久化到具体目标（待应用规则 / 当前世界）。<br>
     * Persist the user-modified rules to the concrete target (pending rules / current world).
     *
     * @param result  完整的规则结果集（String -> String） / full rule result set (String -> String)
     * @param changed 相比原始值真正发生变化的规则名集合 / names of rules actually changed vs original
     */
    protected abstract void persistChanges(Map<String, String> result, Set<String> changed);

    // ============================================================
    // 界面生命周期 / Screen lifecycle
    // ============================================================

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        // ===== 计算列表边界 / Compute list bounds =====
        this.listTop = LIST_TOP;
        int footerAreaTop = this.height - FOOTER_AREA_HEIGHT;
        this.listBottom = footerAreaTop - 4;
        int listHeight = Math.max(ROW_HEIGHT, this.listBottom - this.listTop);

        // ===== 使用 HeaderFooterLayout 定位底部按钮 / Use HeaderFooterLayout to position footer buttons =====
        mainLayout = new HeaderFooterLayout(true);
        mainLayout.setFooterHeight(30);

        buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(4);

        if (CreateWorldUI.config.enableResetButton) {
            // 三按钮模式 / Three-button mode
            this.saveButton = Button.builder(
                Text.translatable(Tags.MODID, "options.save"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(100).build();

            this.cancelButton = Button.builder(
                Text.literal(I18n.format("gui.cancel")),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(100).build();

            this.resetButton = Button.builder(
                Text.translatable(Tags.MODID, "options.cancel"),
                btn -> {
                    modifiedRules.clear();
                    changedRules.clear();
                    modifiedRules.putAll(editableRules);
                    if (ruleList != null) ruleList.rebuild();
                }
            ).width(100).build();

            buttonLayout.addChild(this.saveButton);
            buttonLayout.addChild(this.cancelButton);
            buttonLayout.addChild(this.resetButton);
        } else {
            // 两按钮模式 / Two-button mode
            this.cancelButton = Button.builder(
                Text.translatable(Tags.MODID, "options.save"),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(150).build();

            this.saveButton = Button.builder(
                Text.translatable(Tags.MODID, "options.cancel"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(150).build();

            buttonLayout.addChild(this.cancelButton);
            buttonLayout.addChild(this.saveButton);
        }

        mainLayout.setFooter(buttonLayout);
        mainLayout.recalculate(this.width, this.height);

        // ===== 创建中间列表并构建条目 / Create middle list and build entries =====
        this.ruleList = new GameRuleList(this, this.width, listHeight, this.listTop, ROW_HEIGHT);
        this.ruleList.rebuild();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ============================================================
    // 分类列表构建 / Category-ordered list building
    // ============================================================

    /**
     * 构建按分类组织的规则列表。<br>
     * Build a category-organized rule list.
     *
     * @return 有序列表，"category:"前缀表示分类标题，其余为规则名 /
     *         ordered list, "category:" prefix marks category header, others are rule names
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

        // 添加未分类的规则 / Add uncategorized rules
        Set<String> categorizedRules = new HashSet<>();
        for (String categoryKey : categories) {
            categorizedRules.addAll(GameRuleCategoryRegistry.getRulesInCategory(categoryKey));
        }

        boolean hasUncategorized = false;
        for (String rule : allRules) {
            if (!categorizedRules.contains(rule)) {
                if (!hasUncategorized) {
                    orderedList.add("category:uncategorized");
                    hasUncategorized = true;
                }
                orderedList.add(rule);
            }
        }

        return orderedList;
    }

    // ============================================================
    // 输入事件转发到列表 / Input event forwarding to the list
    // ============================================================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // ===== Footer区域按钮点击检测 / Footer zone button click detection =====
        if (mainLayout != null && mainLayout.getFooterFrame() != null) {
            for (ILayout child : mainLayout.getFooterFrame().getChildren()) {
                if (child instanceof HorizontalLayout) {
                    HorizontalLayout hLayout = (HorizontalLayout) child;
                    for (ILayout buttonChild : hLayout.getChildren()) {
                        if (buttonChild instanceof Button) {
                            Button button = (Button) buttonChild;
                            if (button.isMouseOver(mouseX, mouseY)) {
                                button.mouseClicked(mouseX, mouseY, mouseButton);
                            }
                        }
                    }
                }
            }
        }

        // ===== 列表交互（裁剪、条目、滚动条由列表自身管理） =====
        // ===== List interaction (clipping, entries, scrollbar all managed by the list) =====
        if (ruleList != null) {
            ruleList.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (ruleList != null) {
            ruleList.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (ruleList != null) {
            ruleList.mouseDrag(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && ruleList != null) {
            // 悬停在循环按钮上时优先用滚轮切换其值 / When hovering a cycling button, wheel cycles its value first
            if (ruleList.tryScrollCyclingButton(mouseX, mouseY, dWheel)) {
                return;
            }
            // 否则滚动列表 / Otherwise scroll the list
            if (ruleList.isMouseOver(mouseX, mouseY)) {
                ruleList.mouseScrolled(dWheel > 0 ? 1 : -1);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        if (ruleList != null) {
            ruleList.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        // EditBox光标更新在render()内部处理，无需额外操作
        // EditBox cursor update handled internally in render(); no extra action needed
    }

    // ============================================================
    // 渲染 / Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // 标题 / Title
        this.drawCenteredString(this.fontRendererObj,
            Text.translatableString(Tags.MODID, "createworldui.gamerules.title"),
            this.width / 2, 15, 0xFFFFFF);

        // 列表上下分隔线 / List header/footer separators
        ContentPanelRenderer.drawHeaderSeparator(0, this.listTop - ContentPanelRenderer.SEPARATOR_HEIGHT, this.width);
        ContentPanelRenderer.drawFooterSeparator(0, this.listBottom, this.width);

        // 列表（自带背景、Scissor 裁剪与滚动条）/ List (own background, scissor clipping and scrollbar)
        if (ruleList != null) {
            ruleList.render(mouseX, mouseY, partialTicks);
        }

        // ===== 渲染Footer区域的按钮 / Render buttons in Footer zone =====
        if (mainLayout != null && mainLayout.getFooterFrame() != null) {
            for (ILayout child : mainLayout.getFooterFrame().getChildren()) {
                if (child instanceof HorizontalLayout) {
                    HorizontalLayout hLayout = (HorizontalLayout) child;
                    for (ILayout buttonChild : hLayout.getChildren()) {
                        if (buttonChild instanceof Button) {
                            ((Button) buttonChild).render(mouseX, mouseY, partialTicks);
                        }
                    }
                }
            }
        }

        // Tooltip（在列表裁剪之外绘制）/ Tooltip (drawn outside list clipping)
        if (ruleList != null) {
            String ruleName = ruleList.getTooltipRuleName(mouseX, mouseY);
            if (ruleName != null) {
                drawRuleTooltip(ruleName, mouseX, mouseY);
            }
        }
    }

    /**
     * 绘制某条规则的 tooltip（规则名 + 默认值 + 描述）。<br>
     * Draw the tooltip for a rule (rule name + default value + description).
     */
    private void drawRuleTooltip(String ruleName, int mouseX, int mouseY) {
        List<String> tooltipList = new ArrayList<>();

        // 第一行：规则名（黄色） / First line: rule name (yellow)
        tooltipList.add(EnumChatFormatting.YELLOW + ruleName);

        // 默认值 / Default value
        GameruleValue defVal = defaultRules.get(ruleName);
        if (defVal != null) {
            tooltipList.add(EnumChatFormatting.GRAY
                + Text.translatableString(Tags.MODID, "createworldui.customize.custom.default")
                + " " + defVal.getOptimalValue());
        }

        // 描述（若有） / Description (if any)
        String tooltip = getRuleTooltip(ruleName);
        if (tooltip != null) {
            tooltipList.add(EnumChatFormatting.WHITE + tooltip);
        }

        this.func_146283_a(tooltipList, mouseX, mouseY);
    }

    // ============================================================
    // 数据辅助方法 / Data helper methods
    // ============================================================

    /**
     * 记录用户对某条规则的修改。<br>
     * Record a user modification to a rule.
     */
    private void setRuleValue(String ruleName, String value) {
        modifiedRules.put(ruleName, value);
        changedRules.add(ruleName);
    }

    /**
     * 处理文本框规则的编辑：按默认值类型推断后再以字符串存储。<br>
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
     * 检查规则是否被修改过（与原始值不同）。<br>
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
     * 将字符串解析为与参考值匹配的类型。<br>
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
    // 保存 / Saving
    // ============================================================

    /**
     * 收集用户修改并交由子类持久化，随后显示通知。<br>
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

        // 由子类决定保存目标 / Subclass decides the save target
        try {
            persistChanges(result, changedRules);
        } catch (Exception ex) {
            LOGGER.error("Failed to persist game rules: {}", ex.getMessage());
        }

        showSaveNotification();
    }

    /**
     * 在聊天栏显示保存结果通知。<br>
     * Show a chat notification of the save result.
     */
    private void showSaveNotification() {
        if (!changedRules.isEmpty()) {
            String notificationText = I18n.format("createworldui.gamerules.notification.changed");
            String rulesList = String.join(", ", changedRules);

            String message;
            if (CreateWorldUI.config != null && CreateWorldUI.config.changedRulesInChatHighLighted) {
                // 高亮模式：提示文字白色，规则名黄色 / Highlight mode: text white, rule names yellow
                message = EnumChatFormatting.WHITE + notificationText + EnumChatFormatting.YELLOW + rulesList;
            } else {
                // 默认模式：全部白色 / Default mode: all white
                message = EnumChatFormatting.WHITE + notificationText + EnumChatFormatting.WHITE + rulesList;
            }

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

    // ============================================================
    // 中间列表（基于 CatFrame ObjectSelectionList 自实现）
    // Middle list (self-implemented on top of CatFrame ObjectSelectionList)
    // ============================================================

    /**
     * <p>
     *     游戏规则列表 —— 直接复用前置模组 CatFrame 的 {@link ObjectSelectionList}。<br>
     *     裁剪（Scissor）、滚动、滚动条与内容高度均由父类管理；本类只负责构建条目、
     *     行宽/居中、以及 tooltip/滚轮切换等业务逻辑。
     * </p>
     * <p>
     *     Game rule list — directly reuses the prerequisite mod CatFrame's
     *     {@link ObjectSelectionList}. Clipping (scissor), scrolling, scrollbar and
     *     content height are all handled by the superclass; this class only builds
     *     entries, sets row width/centering, and handles business logic such as
     *     tooltips and wheel cycling.
     * </p>
     */
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

        /**
         * 覆盖前置模组的裁剪实现：CatFrame 直接用 GUI 坐标调用 glScissor，
         * 在 GUI 缩放 != 1 时会裁错区域。这里把列表边界从 GUI 坐标换算成帧缓冲
         * 像素坐标（并翻转 Y 轴原点），以正确处理任意 GUI 缩放。<br>
         * Override the prerequisite mod's clipping: CatFrame calls glScissor with raw
         * GUI coordinates, which clips the wrong region when the GUI scale != 1. Here we
         * convert the list bounds from GUI coordinates to framebuffer pixel coordinates
         * (flipping the Y origin) so any GUI scale is handled correctly.
         */
        @Override
        protected void enableScissor() {
            Minecraft mc = Minecraft.getMinecraft();
            double scaleX = (double) mc.displayWidth / (double) screen.width;
            double scaleY = (double) mc.displayHeight / (double) screen.height;
            int sx = (int) Math.floor(getX() * scaleX);
            int sw = (int) Math.ceil(getWidth() * scaleX);
            int sy = (int) Math.floor((screen.height - (getY() + getHeight())) * scaleY);
            int sh = (int) Math.ceil(getHeight() * scaleY);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(sx, sy, sw, sh);
        }

        @Override
        protected boolean entriesCanBeSelected() {
            // 不为整行绘制选中高亮框 / Do not draw a selection highlight box for whole rows
            return false;
        }

        /**
         * 根据当前数据重建全部条目。<br>
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
                    : screen.editableRules.containsKey(item) ? screen.editableRules.get(item) : null;
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
         * 若鼠标悬停在某个布尔规则的循环按钮上，用滚轮切换其值。<br>
         * If the mouse hovers a boolean rule's cycling button, use the wheel to cycle its value.
         *
         * @return 是否已被循环按钮消费 / whether it was consumed by a cycling button
         */
        boolean tryScrollCyclingButton(int mouseX, int mouseY, int rawWheel) {
            for (RuleEntry entry : children()) {
                if (entry instanceof BooleanRuleEntry) {
                    CyclingButton<Boolean> toggle = ((BooleanRuleEntry) entry).getToggle();
                    if (toggle.isMouseOver(mouseX, mouseY)) {
                        toggle.mouseScrolled(rawWheel);
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 返回当前应显示 tooltip 的规则名（鼠标悬停在规则名区域时）。<br>
         * Return the rule name whose tooltip should be shown (when hovering the name area).
         */
        String getTooltipRuleName(int mouseX, int mouseY) {
            RuleEntry hovered = getHovered();
            if (hovered != null && hovered.getRuleName() != null && hovered.isOverName(mouseX, mouseY)) {
                return hovered.getRuleName();
            }
            return null;
        }
    }

    /**
     * 列表条目基类。<br>
     * Base class for list entries.
     */
    private abstract static class RuleEntry extends ObjectSelectionList.Entry<RuleEntry> {

        protected final AbstractScreenGameRuleEditor screen;

        RuleEntry(AbstractScreenGameRuleEditor screen) {
            this.screen = screen;
        }

        /** @return 规则名，分类标题返回 null / rule name, category header returns null */
        String getRuleName() {
            return null;
        }

        /** @return 鼠标是否位于规则名区域 / whether the mouse is over the rule-name area */
        boolean isOverName(int mouseX, int mouseY) {
            return false;
        }
    }

    /**
     * 分类标题条目 —— 居中显示分类名称。<br>
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
     * 布尔规则条目 —— 规则名 + 循环开关按钮。<br>
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
            this.toggle = CyclingButton.onOffBuilder()
                .values(true, false)
                .initially(initialValue)
                .label(Text.literal(""))
                .useVanillaTexture(false)
                .build(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT,
                    (btn, newVal) -> screen.setRuleValue(ruleName, String.valueOf(newVal)));
        }

        /** 依据条目当前位置同步子控件坐标 / Sync child widget coordinates to the entry's current position */
        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            nameLabel.setX(getContentX());
            nameLabel.setY(getContentYMiddle() - font.FONT_HEIGHT / 2);
            toggle.setX(getContentRight() - CONTROL_WIDTH);
            toggle.setY(getContentYMiddle() - CONTROL_HEIGHT / 2);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameLabel.setColor((screen.isHighlightEnabled() && screen.isRuleModified(ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            nameLabel.render(mouseX, mouseY, partialTicks);
            toggle.render(mouseX, mouseY, partialTicks);
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

        @Override
        boolean isOverName(int mouseX, int mouseY) {
            return mouseX >= getContentX() && mouseX < toggle.getX()
                && mouseY >= getY() && mouseY < getY() + getHeight();
        }

        CyclingButton<Boolean> getToggle() {
            return toggle;
        }
    }

    /**
     * 值规则条目 —— 规则名 + 文本输入框。<br>
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
            editBox.setX(getContentRight() - CONTROL_WIDTH);
            editBox.setY(getContentYMiddle() - CONTROL_HEIGHT / 2);
        }

        @Override
        public void renderContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
            layoutWidgets();
            nameLabel.setColor((screen.isHighlightEnabled() && screen.isRuleModified(ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            nameLabel.render(mouseX, mouseY, partialTicks);
            editBox.render(mouseX, mouseY, partialTicks);
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

        @Override
        boolean isOverName(int mouseX, int mouseY) {
            return mouseX >= getContentX() && mouseX < editBox.getX()
                && mouseY >= getY() && mouseY < getY() + getHeight();
        }
    }
}
