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
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * <p>
 *     游戏规则编辑器的抽象基类。<br>
 *     本类负责：<br>
 *     - 数据来源：通过{@link GameRuleMonitorNSetter}读取所有游戏规则作为默认值<br>
 *     - 中间列表：使用前置模组 CatFrame 的 {@link ObjectSelectionList} 渲染规则条目，
 *       裁剪、滚动和列表尺寸均由列表自身管理<br>
 *     - 屏幕基类：继承前置模组 CatFrame 的 {@link Screen}，中间列表与底部按钮经
 *       {@code addRenderableWidget} 注册为屏幕组件，渲染与事件派发统一由基类处理<br>
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
        super(Text.translatable("createworldui.gamerules.title"));
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
    protected void init() {
        Keyboard.enableRepeatEvents(true);

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
                Text.translatable("options.save"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(100).build();

            this.cancelButton = Button.builder(
                Text.literal(I18n.format("gui.cancel")),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(100).build();

            this.resetButton = Button.builder(
                Text.translatable("options.cancel"),
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

            // 注册为屏幕组件：渲染与鼠标事件派发由 Screen 基类统一处理
            // Register as screen widgets: rendering and mouse dispatch are handled by the Screen base
            addRenderableWidget(this.saveButton);
            addRenderableWidget(this.cancelButton);
            addRenderableWidget(this.resetButton);
        } else {
            // 两按钮模式 / Two-button mode
            this.cancelButton = Button.builder(
                Text.translatable("options.save"),
                btn -> this.mc.displayGuiScreen(this.parentScreen)
            ).width(150).build();

            this.saveButton = Button.builder(
                Text.translatable("options.cancel"),
                btn -> { saveChanges(); this.mc.displayGuiScreen(this.parentScreen); }
            ).width(150).build();

            buttonLayout.addChild(this.cancelButton);
            buttonLayout.addChild(this.saveButton);

            addRenderableWidget(this.cancelButton);
            addRenderableWidget(this.saveButton);
        }

        mainLayout.setFooter(buttonLayout);
        mainLayout.recalculate(this.width, this.height);

        // ===== 创建中间列表并构建条目 / Create middle list and build entries =====
        this.ruleList = new GameRuleList(this, this.width, listHeight, this.listTop, ROW_HEIGHT);
        addRenderableWidget(this.ruleList);
        this.ruleList.rebuild();
    }

    @Override
    public void removed() {
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
                    // 未分类标题同样使用本地化键，由 lang 文件提供显示文字；
                    // "category:" 前缀是 rebuild() 识别分类标题条目的内部标记，不可省略
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
    // 输入事件 / Input events
    // ============================================================

    // 鼠标点击/释放/拖动与键盘输入由 Screen 基类按组件树派发：dispatchMouseClicked
    // 命中列表或底部按钮后，事件在对应组件内部继续分发（列表 → 条目 → EditBox /
    // 循环按钮）；键盘输入经 Screen.keyTyped 送达焦点组件（列表 → 聚焦条目 → EditBox）。
    // Mouse click/release/drag and keyboard input are dispatched by the Screen base
    // through the widget tree: dispatchMouseClicked hits the list or a footer button
    // and the event continues inside that widget (list → entry → edit box / cycling
    // button); keyboard input reaches the focused widget via Screen.keyTyped
    // (list → focused entry → edit box).

    /**
     * 鼠标滚轮：悬停在布尔规则的循环按钮上时优先由该按钮消费（切换值），
     * 否则交给基类派发给鼠标下的组件（列表滚动等）。<br>
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

    // 显式覆写为 public 并转发基类（子类一并受益）。编译期 classpath 上的 CatFrame
    // 构件为 reobf(SRG) 命名：其字节码中的 Screen.func_73869_a / func_73864_a 在
    // javac 视角下不是 keyTyped / mouseClicked，接口方法的 public 实现会回落到
    // GuiScreen 的 protected 同名方法，从而报"正在尝试分配更低的访问权限"编译错误；
    // 显式 public 覆写转发基类即可消除。运行时 reobf 后，super 调用会经 JVM
    // invokespecial 的超类方法选择重新命中 Screen 的覆写（Esc 处理 / 组件事件派发），
    // 行为不变——这两个覆写不可删除。
    // Explicit public overrides forwarding to super (subclasses benefit too). The
    // CatFrame artifact on the compile classpath is a reobf(SRG) build: its
    // Screen.func_73869_a / func_73864_a are invisible to javac as keyTyped /
    // mouseClicked, so the public interface methods would fall back to GuiScreen's
    // protected ones and fail to compile ("attempting to assign weaker access
    // privileges"). After reobf, the super calls re-select Screen's overrides at
    // runtime via JVM invokespecial superclass method selection (Esc handling /
    // widget event dispatch), so behaviour is preserved — do not remove.
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // ============================================================
    // 渲染 / Rendering
    // ============================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 背景与已注册组件（中间列表、底部按钮）由基类渲染
        // Background and registered widgets (middle list, footer buttons) rendered by the base
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 标题与列表上下分隔线：与列表/按钮区域不重叠，后绘不会遮盖任何组件
        // Title and list header/footer separators: they do not overlap the list/button
        // areas, so drawing them after the widgets covers nothing
        this.drawCenteredString(this.fontRendererObj, this.getTitle().getString(),
            this.width / 2, 15, 0xFFFFFF);

        ContentPanelRenderer.drawHeaderSeparator(0, this.listTop - ContentPanelRenderer.SEPARATOR_HEIGHT, this.width);
        ContentPanelRenderer.drawFooterSeparator(0, this.listBottom, this.width);

        // Tooltip 由各条目 nameLabel 组件自行泵动（WidgetTooltipHolder），帧末由 CatFrame 统一延迟绘制
        // Tooltip is driven per-entry via the nameLabel widget (WidgetTooltipHolder) and drawn deferred by CatFrame at end of frame
    }

    /**
     * 构建某条规则的 tooltip 文本（规则名 + 默认值 + 描述，以换行符分隔，支持 § 格式）。<br>
     * Build the tooltip text for a rule (rule name + default value + description, newline-separated, §-formatted).
     */
    private String buildRuleTooltipMessage(String ruleName) {
        StringBuilder sb = new StringBuilder();
        // 第一行：规则名（黄色） / First line: rule name (yellow)
        sb.append(EnumChatFormatting.YELLOW).append(ruleName);

        // 默认值 / Default value
        GameruleValue defVal = defaultRules.get(ruleName);
        if (defVal != null) {
            sb.append('\n').append(EnumChatFormatting.GRAY)
                .append(Text.translatableString("createworldui.customize.custom.default"))
                .append(' ').append(defVal.getOptimalValue());
        }

        // 描述（若有） / Description (if any)
        String tooltip = getRuleTooltip(ruleName);
        if (tooltip != null) {
            sb.append('\n').append(EnumChatFormatting.WHITE).append(tooltip);
        }
        return sb.toString();
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
         * 像素坐标（并翻转 Y 轴原点），以正确处理任意 GUI 缩放。
         * CatFrame 0.7.1.1 的 AbstractScrollArea.enableScissor 仍使用 GUI 坐标，
         * 此覆写在 0.7.1.1 下必须保留。<br>
         * Override the prerequisite mod's clipping: CatFrame 0.7.1.1 calls glScissor
         * with raw GUI coordinates, which clips the wrong region when the GUI scale
         * != 1. Here we convert the list bounds from GUI coordinates to framebuffer
         * pixel coordinates (flipping the Y origin) so any GUI scale is handled
         * correctly; this override must be kept on 0.7.1.1.
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
         * @param delta 归一化滚轮增量（仅符号有意义） / normalised wheel delta (only the sign matters)
         * @return 是否已被循环按钮消费 / whether it was consumed by a cycling button
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
            // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动（悬停/焦点 + 延迟）
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

        /** 依据条目当前位置同步子控件坐标 / Sync child widget coordinates to the entry's current position */
        private void layoutWidgets() {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            nameLabel.setX(getContentX());
            nameLabel.setY(getContentYMiddle() - font.FONT_HEIGHT / 2);
            // 将 nameLabel 命中区域扩展至整行（文字仍按 x/y 绘制，不受尺寸影响），
            // 使组件级 tooltip 的悬停判定覆盖整行，与高版本规则界面一致
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
            // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动（悬停/焦点 + 延迟）
            // Component-level tooltip: auto-pumped by WidgetTooltipHolder driven from extractRenderState (hover/focus + delay)
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
            // 将 nameLabel 命中区域扩展至整行（文字仍按 x/y 绘制，不受尺寸影响），
            // 使组件级 tooltip 的悬停判定覆盖整行，与高版本规则界面一致
            // Expand nameLabel's hit area to the whole row (text is still drawn at x/y,
            // unaffected by size): component-level tooltip hover detection covers the full row
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
