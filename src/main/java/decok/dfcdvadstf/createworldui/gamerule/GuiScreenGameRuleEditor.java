package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.ContentPanelRenderer;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
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

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.*;

/**
 * <p>
 *     游戏规则编辑器（用于创建世界前编辑待应用的游戏规则）<br>
 *     功能说明：<br>
 *     - 显示来源：通过{@link GameRuleMonitorNSetter}从当前世界获取所有游戏规则（{@link GameRuleMonitorNSetter#getAllGamerules(World)} (currentWorld)）<br>
 *     - 保存目标：通过{@link GameRuleApplier}将修改后的规则设置为待应用规则（{@link GameRuleApplier#setPendingGameRules(Map)} (Map<\String,String>)）<br>
 *     - 核心处理类：{@link GameRuleMonitorNSetter}
 * </p>
 * <p>
 *     Game rule editor (for editing pending game rules before world creation)<br>
 *     Function description:<br>
 *     - Data source: Get all game rules from the current world via {@link GameRuleMonitorNSetter} ({@link GameRuleMonitorNSetter#getAllGamerules(World)} (currentWorld))<br>
 *     - Save target: Set modified rules as pending rules via {@link GameRuleApplier}({@link GameRuleApplier#setPendingGameRules(Map)} (Map<\String,String>))<br>
 *     - Core handler: {@link GameRuleMonitorNSetter}
 * </p>
 * <p>
 *     注意：所有保存到待应用规则的值均转换为字符串（与{@link GameRules}存储格式一致）<br>
 *     Note: All values saved to pending rules are converted to strings (consistent with {@link GameRules} storage format)
 * </p>
 * <p>
 *     平滑滚动特性：使用像素级滚动位置（scrollPosition）代替行级整数偏移，<br>
 *     通过GL Scissor裁剪和GL Translate偏移实现亚像素级平滑过渡，<br>
 *     允许行在面板边缘部分可见，且部分可见的交互组件仍可操作。<br>
 *     Smooth scroll feature: Uses pixel-level scroll position instead of integer row offset,<br>
 *     achieving sub-pixel smooth transition via GL Scissor clipping and GL Translate offset,<br>
 *     allowing rows to be partially visible at panel edges with interactive components still operable.
 * </p>
 */

@SuppressWarnings("unchecked")
public class GuiScreenGameRuleEditor extends GuiScreen {

    @SideOnly(Side.CLIENT)

    private static final Logger LOGGER = LogManager.getLogger("GameRuleEditor");

    // 待写入应用器的规则映射（键：规则名，值：字符串形式的规则值）
    // Rule map to be written to applier (key: rule name, value: rule value in string form)
    private final Map<String, String> editableRules;

    // 默认/原始规则信息（包含多种数据类型）
    // Default/original rule information (contains multiple data types)
    private final Map<String, GameruleValue> defaultRules;

    // 临时保存用户在UI中修改的值（字符串形式）
    // Temporarily save values modified by user in UI (in string form)
    private final Map<String, String> modifiedRules = new HashMap<>();

    // 跟踪已修改的规则（用于显示通知）
    // Track modified rules (for displaying notifications)
    private final Set<String> changedRules = new HashSet<>();

    // 规则与UI组件的映射
    // Map of rules to UI components
    private final Map<String, GuiComponentWrapper> ruleComponents = new LinkedHashMap<>();

    private GuiButton saveButton;// 保存按钮 / Save button
    private GuiButton cancelButton; // 取消按钮 / Cancel button
    private GuiButton resetButton; // 重置按钮 / Reset button

    // ===== 平滑滚动相关字段 / Smooth scroll related fields =====

    // 当前滚动位置（像素级） / Current scroll position (pixel-level)
    private float scrollPosition = 0f;
    // 平滑插值目标滚动位置 / Target scroll position for smooth lerp
    private float targetScrollPosition = 0f;
    // 行内像素偏移量（由scrollPosition派生，用于GL Translate） / Sub-row pixel offset (derived from scrollPosition, for GL Translate)
    private float scrollSubOffset = 0f;
    // 最大滚动位置（像素） / Maximum scroll position (pixels)
    private int maxScrollPosition;
    // 整数行偏移（由scrollPosition派生，用于组件创建索引）/ Integer row offset (derived from scrollPosition, for component creation index)
    private int scrollOffset = 0;

    // 上次组件创建时的scrollOffset，用于判断是否需要重建组件
    // scrollOffset at last component creation, used to determine if components need rebuilding
    private int lastComponentScrollOffset = -1;
    // 上次组件创建时的scrollPosition，用于在scrollOffset不变时检测scrollPosition增量变化
    // scrollPosition at last component creation, used to detect scrollPosition delta changes within same scrollOffset
    private float lastComponentCreationScrollPosition = -1;

    // 焦点保存：滚动导致组件重建时保存/恢复文本框焦点
    // Focus preservation: save/restore text field focus when components are rebuilt due to scrolling
    private String focusedRuleName = null;

    // 插值速度 / Lerp speed (0.0~1.0, higher = faster snap)
    private static final float SCROLL_LERP_SPEED = 0.2f;

    private static final int ROW_HEIGHT = 25; // 行高 / Row height
    private static final int CATEGORY_HEADER_HEIGHT = 20; // 分类标题高度 / Category header height
    private int visibleRows = 8; // 可见行数 / Number of visible rows
    private boolean isScrolling = false; // 是否正在滚动 / Whether scrolling is in progress
    private GuiScreen parentScreen; // 父界面 / Parent screen
    private static final int PANEL_TOP = 50;
    // 内容区起始Y（面板顶部内边距后） / Content area start Y (after panel top padding)
    private static final int CONTENT_TOP = 60;

    // Cached ClearMyBackground presence — checked once at class load
    // 缓存 ClearMyBackground 是否加载——类加载时检查一次
    private static final boolean CLEAR_MY_BACKGROUND_LOADED = Loader.isModLoaded("clearmybackground");

    /**
     * 构造游戏规则编辑器<br>
     * Constructor for GameRuleEditor
     * @param parentScreen 父界面（创建世界界面） / Parent screen (world creation screen)
     * @param editableRules 可编辑的游戏规则映射 / Editable game rule map
     */
    public GuiScreenGameRuleEditor(GuiScreen parentScreen, Map<String, String> editableRules) {
        this.parentScreen = parentScreen;

        // 过滤掉 null 值，确保 editableRules 不包含 null
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
        for (Map.Entry<String,String> e : this.editableRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                this.modifiedRules.put(e.getKey(), e.getValue());
            }
        }

        /**
         * Order for reading default rules:
         * 1) If editableRules is not empty, prefer using its values to build default rules
         * 2) Otherwise try reading from the real world (if world is not null)
         * 3) Fall back to a new GameRules instance (vanilla defaults) if both fail
         *
         * 读取默认规则的顺序：
         * 1) 如果 editableRules 不为空，优先使用其中的値构建默认规则
         * 2) 否则尝试从真实世界读取（如果世界不为 null）
         * 3) 如果都失败，回退到新的 GameRules 实例（使用原版默认値）
         */
        Map<String, GameruleValue> defaultsFromMonitor = null;

        // Method 1: if editableRules is not empty, prefer using its values
        // 方法 1: 如果 editableRules 不为空，优先使用其中的値构建默认规则
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
        // 方法 2: 尝试从真实世界获取
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
        // 方法 3: 回退到临时 GameRules 实例
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
        // 确保 defaultRules 至少包含 editableRules 的所有键
        for (String k : this.editableRules.keySet()) {
            if (!this.defaultRules.containsKey(k)) {
                String s = this.editableRules.get(k);
                boolean b = "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
                int iv = 0;
                double dv = 0.0;
                try { iv = Integer.parseInt(s); } catch (Exception ignored) {}
                try { dv = Double.parseDouble(s); } catch (Exception ignored) {}
                GameruleValue gv = new GameruleValue(s, b, iv, dv);
                this.defaultRules.put(k, gv);
            }
        }

        // 初始maxScrollPosition在initGui中根据实际可见行数计算
        // Initial maxScrollPosition is calculated in initGui based on actual visible rows
        this.maxScrollPosition = 0;
    }


    /**
     * <p>
     *     初始化界面组件<br>
     *     包括按钮和规则编辑组件
     * </p>
     * <p>
     *     Initialize UI components<br>
     *     Including buttons and rule editing components
     * </p>
     */
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        this.buttonList.clear();

        int panelBottom = this.height - 50;
        this.visibleRows = Math.max(1, (panelBottom - CONTENT_TOP) / ROW_HEIGHT);
        
        // 计算总内容高度（包括分类标题）
        List<String> categoryOrderedList = buildCategoryOrderedList();
        int totalContentHeight = 0;
        for (String item : categoryOrderedList) {
            if (item.startsWith("category:")) {
                totalContentHeight += CATEGORY_HEADER_HEIGHT;
            } else {
                totalContentHeight += ROW_HEIGHT;
            }
        }
        
        this.maxScrollPosition = Math.max(0, totalContentHeight - (this.visibleRows * ROW_HEIGHT));

        // Clamp scroll position after resize
        // resize后夹紧滚动位置
        this.targetScrollPosition = Math.max(0, Math.min(this.targetScrollPosition, this.maxScrollPosition));
        this.scrollPosition = Math.max(0, Math.min(this.scrollPosition, this.maxScrollPosition));
        updateScrollDerivedValues(buildCategoryOrderedList().size());

        // Button layout
        // 按钮布局
        if (CreateWorldUI.config.enableResetButton) {
            this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 100, 20, I18n.format("options.save"));
            this.cancelButton = new GuiButton(1, this.width / 2 - 50, this.height - 30, 100, 20, I18n.format("gui.cancel"));
            this.resetButton = new GuiButton(2, this.width / 2 + 54, this.height - 30, 100, 20, I18n.format("options.reset"));
        } else {
            this.cancelButton = new GuiButton(1, this.width / 2 + 2, this.height - 30, 150, 20, I18n.format("gui.cancel"));
            this.saveButton = new GuiButton(0, this.width / 2 - 152, this.height - 30, 150, 20, I18n.format("options.save"));
        }

        // Ensure added buttons are not null
        // 确保添加的按钮不为 null
        if (this.saveButton != null) this.buttonList.add(this.saveButton);
        if (this.cancelButton != null) this.buttonList.add(this.cancelButton);
        if (this.resetButton != null) this.buttonList.add(this.resetButton);

        createRuleComponents();
    }

    /**
     * 关闭界面时调用
     * 禁用键盘重复事件
     *
     * Called when closing the screen
     * Disable keyboard repeat events
     */
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * <p>
     *     从scrollPosition派生scrollOffset（整数行偏移）和scrollSubOffset（行内像素偏移）。<br>
     *     这两个值用于组件创建（scrollOffset）和GL渲染偏移（scrollSubOffset）。
     * </p>
     * <p>
     *     Derive scrollOffset (integer row offset) and scrollSubOffset (sub-row pixel offset) from scrollPosition.<br>
     *     These values are used for component creation (scrollOffset) and GL render offset (scrollSubOffset).
     * </p>
     */
    private void updateScrollDerivedValues(int totalItems) {
        int newScrollOffset = (int)(scrollPosition / ROW_HEIGHT);
        // 限制scrollOffset不超过总项数，确保能滚动到最后一项
        // Clamp scrollOffset to total item count, ensuring last item is reachable
        int maxRowOffset = Math.max(0, totalItems - visibleRows);
        newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxRowOffset));
        this.scrollOffset = newScrollOffset;
        this.scrollSubOffset = scrollPosition - this.scrollOffset * ROW_HEIGHT;
        // 确保scrollSubOffset非负 / Ensure scrollSubOffset is non-negative
        if (this.scrollSubOffset < 0) this.scrollSubOffset = 0;
    }

    /**
     * 构建按分类组织的规则列表
     * Build a category-organized rule list
     * 
     * @return 有序的列表，包含分类标题（null表示分类标题）和规则名
     *         Ordered list containing category headers (null means category header) and rule names
     */
    private List<String> buildCategoryOrderedList() {
        List<String> orderedList = new ArrayList<>();
        Set<String> allRules = defaultRules.keySet();
        
        // 获取所有分类
        List<String> categories = GameRuleCategoryRegistry.getAllCategories();
        
        // 按分类添加规则
        for (String categoryKey : categories) {
            List<String> rulesInCategory = GameRuleCategoryRegistry.getRulesInCategory(categoryKey);
            
            // 只添加实际存在的规则
            List<String> validRules = new ArrayList<>();
            for (String rule : rulesInCategory) {
                if (allRules.contains(rule)) {
                    validRules.add(rule);
                }
            }
            
            // 如果分类下有规则，添加分类标题和规则
            if (!validRules.isEmpty()) {
                orderedList.add("category:" + categoryKey); // 分类标记
                orderedList.addAll(validRules);
            }
        }
        
        // 添加未分类的规则
        Set<String> categorizedRules = new HashSet<>();
        for (String categoryKey : categories) {
            categorizedRules.addAll(GameRuleCategoryRegistry.getRulesInCategory(categoryKey));
        }
        
        boolean hasUncategorized = false;
        for (String rule : allRules) {
            if (!categorizedRules.contains(rule)) {
                if (!hasUncategorized) {
                    orderedList.add("category:uncategorized"); // 未分类标记
                    hasUncategorized = true;
                }
                orderedList.add(rule);
            }
        }
        
        return orderedList;
    }

    /**
     * 创建并布局规则组件（布尔值使用按钮，其他类型使用文本框）<br>
     * 支持平滑滚动：包含额外一行以处理底部部分可见行，保存/恢复文本框焦点。
     *
     * Create and layout rule components (boolean uses button, other types use text field).<br>
     * Smooth scroll support: includes one extra row for bottom partial visibility, saves/restores text field focus.
     */
    private void createRuleComponents() {

        // 安全检查：确保 buttonList 不为 null
        if (this.buttonList == null) {
            LOGGER.error("buttonList is null! Initializing...");
            this.buttonList = new ArrayList<>();
            return;
        }

        // ===== 保存当前焦点文本框 / Save current focused text field =====
        focusedRuleName = null;
        for (Map.Entry<String, GuiComponentWrapper> entry : ruleComponents.entrySet()) {
            if (entry.getValue().type == ComponentType.TEXT_FIELD) {
                GuiTextField tf = (GuiTextField) entry.getValue().component;
                if (tf.isFocused()) {
                    focusedRuleName = entry.getKey();
                    break;
                }
            }
        }

        // Remove old boolean buttons that are not visible on screen
        // 删除旧的在用户屏幕上不可见的布尔按钮
        Iterator<GuiButton> it = this.buttonList.iterator();
        while (it.hasNext()) {
            GuiButton btn = it.next();
            if (btn != null && btn.id >= 100) {
                it.remove();
            }
        }

        ruleComponents.clear();
        int index = 0;
        int visibleUIRowIndex = 0;

        // 构建分类列表
        List<String> categoryOrderedList = buildCategoryOrderedList();
        
        // 计算总高度（用于滚动）
        int totalHeight = 0;
        for (String item : categoryOrderedList) {
            if (item.startsWith("category:")) {
                totalHeight += CATEGORY_HEADER_HEIGHT;
            } else {
                totalHeight += ROW_HEIGHT;
            }
        }
        
        // 更新最大滚动位置（在 drawScreen 中统一计算，这里保留兼容性）
        // maxScrollPosition is now calculated in drawScreen, kept here for compatibility
        
        // 计算可见区域高度（像素）- 使用面板实际高度而不是 visibleRows * ROW_HEIGHT
        // Calculate visible area height (pixels) - use actual panel height instead of visibleRows * ROW_HEIGHT
        int panelBottom = this.height - 50;
        int visibleHeight = panelBottom - CONTENT_TOP;
        int currentY = 0; // 当前项的Y坐标（像素，相对于列表顶部）

        for (String item : categoryOrderedList) {
            // 分类标题（跳过，不在这里处理）
            if (item.startsWith("category:")) {
                currentY += CATEGORY_HEADER_HEIGHT;
                index++;
                continue;
            }
            
            // 规则名
            String ruleName = item;
            GameruleValue value = defaultRules.get(ruleName);

            // 确保 value 不为 null
            if (value == null) {
                LOGGER.warn("GameruleValue for {} is null, skipping", ruleName);
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 检查规则行是否与可见区域重叠（考虑项的高度）
            // Check if rule row overlaps with visible area (considering item height)
            int itemBottom = currentY + ROW_HEIGHT;
            if (itemBottom <= scrollPosition || currentY >= scrollPosition + visibleHeight) {
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 计算屏幕Y坐标（相对于内容区顶部）
            // 使用 scrollOffset * ROW_HEIGHT 而不是 (int)scrollPosition，确保与 GL Translate 的 scrollSubOffset 计算一致
            // Use scrollOffset * ROW_HEIGHT instead of (int)scrollPosition to ensure consistency with GL Translate's scrollSubOffset calculation
            int yPos = CONTENT_TOP + (currentY - scrollOffset * ROW_HEIGHT);

            // Calculate display value
            // 计算显示値
            Object displayObj;
            String stringValue = null;

            if (modifiedRules.containsKey(ruleName)) {
                stringValue = modifiedRules.get(ruleName);
            } else if (editableRules.containsKey(ruleName)) {
                stringValue = editableRules.get(ruleName);
            }

            if (stringValue != null) {
                displayObj = parseFromString(stringValue, value.getOptimalValue());
            } else {
                displayObj = value.getOptimalValue();
            }

            // 创建组件
            GuiComponentWrapper wrapper = createComponentForRule(ruleName, displayObj, yPos, 100 + visibleUIRowIndex);
            if (wrapper != null) {
                wrapper.globalIndex = index;
                wrapper.ruleName = ruleName; // 存储规则名，用于 actionPerformed 中查找
                ruleComponents.put(ruleName, wrapper);
            }

            visibleUIRowIndex++;
            currentY += ROW_HEIGHT;
            index++;
        }

        lastComponentScrollOffset = scrollOffset;
        lastComponentCreationScrollPosition = scrollPosition;

        // ===== 恢复焦点 / Restore focus =====
        if (focusedRuleName != null && ruleComponents.containsKey(focusedRuleName)) {
            GuiComponentWrapper wrapper = ruleComponents.get(focusedRuleName);
            if (wrapper != null && wrapper.type == ComponentType.TEXT_FIELD) {
                ((GuiTextField) wrapper.component).setFocused(true);
            }
        }
    }

    /**
     * 为特定规则创建对应的UI组件
     *
     * Create corresponding UI component for specific rule
     *
     * @param ruleName 规则名称 / Rule name
     * @param value 规则值 / Rule value
     * @param yPos Y坐标 / Y coordinate
     * @param id 组件ID / Component ID
     * @return UI组件包装器 / UI component wrapper
     */
    private GuiComponentWrapper createComponentForRule(String ruleName, Object value, int yPos, int id) {
        int componentX = this.width / 2 + 90;
        int componentWidth = 44;

        // Boolean button
        // 布尔按钮
        if (value instanceof Boolean) {
            boolean boolValue = (Boolean) value;
            String display = boolValue ? I18n.format("options.on") : I18n.format("options.off");

            GuiButton button = new GuiButton(id, componentX, yPos, componentWidth, 20, display);

            // Ensure button is not null before adding to buttonList
            // 确保按钮不为 null 再添加到 buttonList
            if (button != null) {
                this.buttonList.add(button);
                return new GuiComponentWrapper(button, ComponentType.BOOLEAN_BUTTON);
            }
            return null;
        }

        // Number/string: use text field
        // 数字/字符串使用文本输入框
        GuiTextField textField = new GuiTextField(this.fontRendererObj, componentX, yPos, componentWidth, 20);

        String initial;
        if (modifiedRules.containsKey(ruleName)) {
            initial = modifiedRules.get(ruleName);
        } else if (editableRules.containsKey(ruleName)) {
            initial = editableRules.get(ruleName);
        } else if (value != null) {
            initial = String.valueOf(value);
        } else {
            initial = "";
        }

        textField.setText(initial);
        textField.setMaxStringLength(200);

        return new GuiComponentWrapper(textField, ComponentType.TEXT_FIELD);
    }

    /**
     * 处理按钮点击事件
     *
     * Handle button click events
     *
     * @param button 被点击的按钮 / Clicked button
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        int id = button.id;

        switch (id){
            case 0:
                saveChanges();
                this.mc.displayGuiScreen(this.parentScreen);
                return;
            case 1:
                this.mc.displayGuiScreen(this.parentScreen);
                return;
            case 2:
                modifiedRules.clear();
                changedRules.clear();
                // 重置为原始值
                // Reset to original values
                modifiedRules.putAll(editableRules);
                createRuleComponents();
                return;
        }

        // 处理布尔值按钮（ID >= 100）
        // Handle boolean buttons (ID >= 100)
        if (id >= 100) {
            // 通过按钮ID找到对应的控件，然后获取规则名
            // Find component by button ID, then get rule name
            String ruleName = null;
            for (Map.Entry<String, GuiComponentWrapper> entry : ruleComponents.entrySet()) {
                if (entry.getValue().component instanceof GuiButton) {
                    GuiButton btn = (GuiButton) entry.getValue().component;
                    if (btn.id == id) {
                        ruleName = entry.getValue().ruleName;
                        break;
                    }
                }
            }
            
            if (ruleName == null || !defaultRules.containsKey(ruleName)) {
                return;
            }

            GuiComponentWrapper wrapper = ruleComponents.get(ruleName);
            if (wrapper != null && wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                toggleBooleanRule(ruleName, button);
            }
        }
    }

    /**
     * 通过 {@code index} 显示游戏规则
     * Get GameRule's name through index ({@code index})
     * @param index the index of current GameRule Map / 当前的游戏规则映射的 index
     * @return String of Rule name like {@code doFireTick} / 游戏规则ID
     */
    private String getRuleNameByIndex(int index) {
        if (index < 0 || index >= defaultRules.size()) {
            return null;
        }
        int i = 0;
        for (String ruleName : defaultRules.keySet()) {
            if (i == index) return ruleName;
            i++;
        }
        return null;
    }

    /**
     * 获取当前游戏规则ID所对应的布尔值（modified > editable > default）<br>
     * Get the boolean value of current Rule name, priority is modified > editable > default
     */
    private void toggleBooleanRule(String ruleName, GuiButton button) {
        // 获取当前游戏规则ID所对应的布尔值（modified > editable > default）
        // Get the boolean value of current Rule name.
        String curStr = null;
        if (modifiedRules.containsKey(ruleName)) curStr = modifiedRules.get(ruleName);
        else if (editableRules.containsKey(ruleName)) curStr = editableRules.get(ruleName);
        else {
            GameruleValue def = defaultRules.get(ruleName);
            curStr = (def != null) ? String.valueOf(def.getOptimalValue()) : "false";
        }

        boolean cur = Boolean.parseBoolean(curStr);
        boolean next = !cur;
        // 保存为 String
        // Save as String
        modifiedRules.put(ruleName, String.valueOf(next));

        // 更新按钮文本
        // Update the Button text.
        button.displayString = next ? I18n.format("options.on") : I18n.format("options.off");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        for (Map.Entry<String, GuiComponentWrapper> entry : ruleComponents.entrySet()) {
            String ruleName = entry.getKey();
            GuiComponentWrapper wrapper = entry.getValue();

            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;

                if (textField.textboxKeyTyped(typedChar, keyCode)) {
                    // 获取用户输入
                    // Get users input
                    String input = textField.getText();

                    // parsed 仅用于内部展示类型推断，不影响最终保存
                    // parsed only used for internal display type inference, does not affect final saving
                    Object parsed = parseFromString(input, defaultRules.get(ruleName).getOptimalValue());

                    // 真正存储 String → String
                    // Actually Store String to String
                    modifiedRules.put(ruleName, String.valueOf(parsed));
                }
            }
        }
    }

    /**
     * <p>
     *     处理鼠标点击事件（覆盖父类方法）。<br>
     *     标准按钮使用原始坐标，规则组件使用调整后的坐标（补偿scrollSubOffset），<br>
     *     且仅当点击位于面板可见区域内时才处理规则组件交互。
     * </p>
     * <p>
     *     Handle mouse click events (override).<br>
     *     Standard buttons use original coordinates, rule components use adjusted coordinates (compensating scrollSubOffset),<br>
     *     and rule component interaction is only processed when the click is within the panel's visible area.
     * </p>
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int panelBottom = this.height - 50;

        // ===== 标准按钮（保存/取消/重置）- 使用原始坐标 / Standard buttons - use original coordinates =====
        for (GuiButton button : (List<GuiButton>)this.buttonList) {
            if (button.id < 100 && button.enabled) {
                if (button.mousePressed(this.mc, mouseX, mouseY)) {
                    button.func_146113_a(this.mc.getSoundHandler());
                    this.actionPerformed(button);
                }
            }
        }

        // ===== 规则组件 - 仅在面板可见区域内交互 / Rule components - only interact within panel visible area =====
        if (mouseY >= CONTENT_TOP && mouseY <= panelBottom) {
            // 补偿GL Translate偏移：屏幕mouseY → 组件空间mouseY
            // Compensate GL Translate offset: screen mouseY → component-space mouseY
            int adjustedMouseY = mouseY + Math.round(scrollSubOffset);

            for (GuiComponentWrapper wrapper : ruleComponents.values()) {
                if (wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                    GuiButton button = (GuiButton) wrapper.component;
                    if (button.enabled && button.mousePressed(this.mc, mouseX, adjustedMouseY)) {
                        button.func_146113_a(this.mc.getSoundHandler());
                        this.actionPerformed(button);
                    }
                } else if (wrapper.type == ComponentType.TEXT_FIELD) {
                    GuiTextField textField = (GuiTextField) wrapper.component;
                    textField.mouseClicked(mouseX, adjustedMouseY, mouseButton);
                }
            }
        } else {
            // 点击面板外 - 取消所有文本框焦点 / Click outside panel - unfocus all text fields
            for (GuiComponentWrapper wrapper : ruleComponents.values()) {
                if (wrapper.type == ComponentType.TEXT_FIELD) {
                    ((GuiTextField) wrapper.component).setFocused(false);
                }
            }
        }

        // ===== 滚动条交互 / Scrollbar interaction =====
        int scrollBarX = this.width / 2 + 149;
        int scrollBarY = 60;
        int scrollBarHeight = visibleRows * ROW_HEIGHT;

        if (mouseX >= scrollBarX && mouseX <= scrollBarX + 10 &&
                mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
            this.isScrolling = true;
        }
    }

    /**
     * Check whether click the scroll bar.
     * 检查是否点击滚动条区域（用于拖动）
     */
    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state == 0 || state == 1) {
            this.isScrolling = false;
        }
    }

    /**
     * <p>
     *     处理鼠标输入（滚轮和滚动条拖动）。<br>
     *     滚轮：设置targetScrollPosition，由drawScreen中lerp平滑过渡。<br>
     *     滚动条拖动：直接设置scrollPosition和targetScrollPosition（即时响应）。
     * </p>
     * <p>
     *     Handle mouse input (wheel and scrollbar drag).<br>
     *     Wheel: sets targetScrollPosition, smoothly interpolated in drawScreen.<br>
     *     Scrollbar drag: directly sets both scrollPosition and targetScrollPosition (instant response).
     * </p>
     */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (this.isScrolling) {
            int scrollBarY = 60;
            int scrollBarHeight = visibleRows * ROW_HEIGHT;
            // 滑块高度计算（与drawScrollBar一致）/ Slider height calc (consistent with drawScrollBar)
            List<String> categoryOrderedList = buildCategoryOrderedList();
            int totalItems = categoryOrderedList.size();
            int sliderHeight = Math.max(20, scrollBarHeight * visibleRows / totalItems);

            // 基于滑块中心位置计算滚动比例，使拖动时滑块跟随鼠标
            // Calculate scroll ratio based on slider center, so slider follows mouse during drag
            float relativePosition = (float) (mouseY - scrollBarY - sliderHeight / 2) / (float) (scrollBarHeight - sliderHeight);
            float newPos = relativePosition * this.maxScrollPosition;
            newPos = Math.max(0, Math.min(newPos, this.maxScrollPosition));
            // 滚动条拖动：即时响应，不使用lerp
            // Scrollbar drag: instant response, no lerp
            this.scrollPosition = this.targetScrollPosition = newPos;
            updateScrollDerivedValues(buildCategoryOrderedList().size());

            if (scrollOffset != lastComponentScrollOffset || 
                Math.abs(scrollPosition - lastComponentCreationScrollPosition) > ROW_HEIGHT * 0.5f) {
                createRuleComponents();
            }
        } else if (Mouse.getEventDWheel() != 0) {
            int scrollAmount = Mouse.getEventDWheel() > 0 ? -1 : 1;
            
            // 滚轮：先重新计算maxScrollPosition，再限制targetScrollPosition
            // Wheel: recalculate maxScrollPosition first, then clamp targetScrollPosition
            int panelBottom = this.height - 50;
            List<String> categoryOrderedList = buildCategoryOrderedList();
            int totalHeight = 0;
            for (String item : categoryOrderedList) {
                if (item.startsWith("category:")) {
                    totalHeight += CATEGORY_HEADER_HEIGHT;
                } else {
                    totalHeight += ROW_HEIGHT;
                }
            }
            int actualVisibleHeight = panelBottom - CONTENT_TOP;
            this.maxScrollPosition = Math.max(0, totalHeight - actualVisibleHeight);

            // 设置目标位置，由drawScreen中lerp平滑过渡
            // Set target position, smoothly interpolated in drawScreen
            this.targetScrollPosition += scrollAmount * ROW_HEIGHT;
            this.targetScrollPosition = Math.max(0, Math.min(this.targetScrollPosition, this.maxScrollPosition));
            // 不在此处调用createRuleComponents，由drawScreen中的lerp逻辑处理
            // Don't call createRuleComponents here; handled by lerp logic in drawScreen
        }
    }

    @Override
    public void updateScreen() {
        // 文本框光标更新 / Text field cursor update
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.updateCursorCounter();
            }
        }
    }

    /**
     * <p>
     *     主渲染方法。包含平滑滚动插值、GL Scissor裁剪和GL Translate偏移。<br>
     *     渲染流程：<br>
     *     1. 插值scrollPosition → targetScrollPosition<br>
     *     2. 更新派生值，必要时重建组件<br>
     *     3. 绘制背景和面板<br>
     *     4. 启用Scissor裁剪 + GL Translate偏移<br>
     *     5. 绘制规则列表和组件<br>
     *     6. 关闭Scissor和Translate<br>
     *     7. 绘制滚动条和tooltip
     * </p>
     * <p>
     *     Main render method. Includes smooth scroll lerp, GL Scissor clipping, and GL Translate offset.<br>
     *     Render flow:<br>
     *     1. Lerp scrollPosition → targetScrollPosition<br>
     *     2. Update derived values, rebuild components if needed<br>
     *     3. Draw background and panel<br>
     *     4. Enable Scissor clipping + GL Translate offset<br>
     *     5. Draw rule list and components<br>
     *     6. Disable Scissor and Translate<br>
     *     7. Draw scrollbar and tooltips
     * </p>
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // ===== 动态计算可见行数和最大滚动量（在lerp之前，确保派生值使用最新参数）=====
        // Calculate dynamic visible rows and max scroll position (before lerp, to ensure derived values use latest parameters)
        int panelBottom = this.height - 50;
        this.visibleRows = Math.max(1, (panelBottom - CONTENT_TOP) / ROW_HEIGHT);
        
        // 构建分类列表以计算总高度
        List<String> categoryOrderedList = buildCategoryOrderedList();
        int totalHeight = 0;
        for (String item : categoryOrderedList) {
            if (item.startsWith("category:")) {
                totalHeight += CATEGORY_HEADER_HEIGHT;
            } else {
                totalHeight += ROW_HEIGHT;
            }
        }
        
        // 最大滚动位置 = 总高度 - 可见区域高度（使用面板实际高度）
        // Max scroll position = total height - visible area height (use actual panel height)
        int actualVisibleHeight = panelBottom - CONTENT_TOP;
        this.maxScrollPosition = Math.max(0, totalHeight - actualVisibleHeight);

        // ===== 平滑滚动插值 / Smooth scroll lerp =====
        if (Math.abs(scrollPosition - targetScrollPosition) > 0.5f) {
            scrollPosition += (targetScrollPosition - scrollPosition) * SCROLL_LERP_SPEED;
        } else if (scrollPosition != targetScrollPosition) {
            scrollPosition = targetScrollPosition;
        }

        // 更新派生值 / Update derived values
        updateScrollDerivedValues(buildCategoryOrderedList().size());

        // 当scrollOffset改变或scrollPosition变化超过半行时重建组件
        // Rebuild components when scrollOffset changes or scrollPosition moves more than half a row
        if (scrollOffset != lastComponentScrollOffset || 
            Math.abs(scrollPosition - lastComponentCreationScrollPosition) > ROW_HEIGHT * 0.5f) {
            createRuleComponents();
        }

        drawDefaultBackground();
        drawContentPanel();

        this.drawCenteredString(this.fontRendererObj, I18n.format("createworldui.gamerules.title"), this.width / 2, 20, 0xFFFFFF);

        // ===== GL Scissor裁剪：限制绘制区域到内容区内 / GL Scissor clipping: limit drawing to content area =====
        // 使用浮点比例计算，避免GuiScale Auto时整数乘法的舍入误差
        // Use floating-point ratio to avoid integer multiplication rounding errors with GuiScale Auto
        // glScissor使用OpenGL坐标系（原点左下角，y向上），与GUI坐标系Y轴方向相反
        // glScissor uses OpenGL coordinate system (origin bottom-left, y upward), opposite to GUI coordinate Y axis
        double scaleY = (double) mc.displayHeight / this.height;
        int scissorX = 0;
        int scissorWidth = mc.displayWidth;
        // scissorY: 面板底边以下的帧缓冲像素数（GUI Y=panelBottom → GL Y方向计算）
        // scissorY: framebuffer pixels below panel bottom edge (GUI Y=panelBottom → GL Y calculation)
        int scissorY = (int) Math.floor((this.height - panelBottom) * scaleY);
        // scissorHeight: 内容区在帧缓冲中的像素高度（用ceil向上取整确保不裁小）
        // scissorHeight: content area height in framebuffer pixels (ceil to ensure we don't clip too small)
        int scissorHeight = (int) Math.ceil((panelBottom - CONTENT_TOP) * scaleY);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        // ===== GL Translate偏移：实现亚像素平滑滚动 / GL Translate offset: sub-pixel smooth scrolling =====
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, -scrollSubOffset, 0f);

        // 绘制规则列表（文本在GL Translate下自然偏移）
        // Draw rule list (text naturally shifted under GL Translate)
        drawRuleList(mouseX, mouseY);

        // 组件渲染 - 使用调整后的mouseY以正确处理悬停状态
        // Component rendering - use adjusted mouseY for correct hover state
        int adjustedMouseY = mouseY + Math.round(scrollSubOffset);
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper != null && wrapper.component != null) {
                if (wrapper.type == ComponentType.TEXT_FIELD) {
                    GuiTextField textField = (GuiTextField) wrapper.component;
                    if (textField != null) {
                        textField.drawTextBox();
                    }
                } else if (wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                    GuiButton button = (GuiButton) wrapper.component;
                    if (button != null) {
                        button.drawButton(this.mc, mouseX, adjustedMouseY);
                    }
                }
            }
        }

        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 滚动条不受GL Translate影响 / Scrollbar is not affected by GL Translate
        drawScrollBar();

        // ===== 仅保留标准按钮给super.drawScreen / Only keep standard buttons for super.drawScreen =====
        // 规则按钮已由上方显式绘制（在GL Translate内），不应再由super重复绘制
        // Rule buttons are already drawn explicitly above (within GL Translate), should not be redrawn by super
        List<GuiButton> savedButtonList = new ArrayList<>(this.buttonList);
        List<GuiButton> standardButtons = new ArrayList<>();
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton btn = (GuiButton) obj;
                if (btn != null && btn.id < 100) {
                    standardButtons.add(btn);
                }
            }
        }
        this.buttonList.clear();
        this.buttonList.addAll(standardButtons);
        super.drawScreen(mouseX, mouseY, partialTicks);
        // 恢复完整buttonList（用于事件处理）/ Restore full buttonList (for event handling)
        this.buttonList.clear();
        this.buttonList.addAll(savedButtonList);

        drawTooltips(mouseX, mouseY);
    }

    private void drawContentPanel() {
        int panelLeft = 0;
        int panelRight = this.width;
        int panelBottom = this.height - 50;

        // When ClearMyBackground is loaded — use the shared textured panel (header + tiled bg + footer)
        // 当 ClearMyBackground 加载时，用提取出来的纹理化面板（顶线 + 平铺背景 + 底线）
        if (CLEAR_MY_BACKGROUND_LOADED) {
            ContentPanelRenderer.drawContentPanel(panelLeft, PANEL_TOP, panelRight - panelLeft, panelBottom);
            return;
        }

        // Fallback: vanilla-style semi-transparent gradient similar to GuiOptions
        // 回退：与 GuiOptions 类似的半透明渐变背景
        drawGradientRect(panelLeft, PANEL_TOP, panelRight, panelBottom, 0x60101010, 0x80101010);

        // Border lines
        // 内边线
        drawRect(panelLeft, PANEL_TOP, panelRight, PANEL_TOP + 1, 0xFF000000);  // top
        drawRect(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF000000); // bottom
    }

    /**
     * 绘制规则列表。在GL Translate上下文中调用，文本位置使用scrollPosition（像素），
     * GL Translate负责亚像素偏移。
     *
     * Draw rule list. Called within GL Translate context, text positions use scrollPosition (pixels),
     * GL Translate handles sub-pixel offset.
     */
    private void drawRuleList(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 60; // 内容区起始Y坐标
        
        // 构建分类列表
        List<String> categoryOrderedList = buildCategoryOrderedList();
        
        // 计算可见区域高度（像素）- 使用面板实际高度而不是 visibleRows * ROW_HEIGHT
        // Calculate visible area height (pixels) - use actual panel height instead of visibleRows * ROW_HEIGHT
        int panelBottom = this.height - 50;
        int visibleHeight = panelBottom - CONTENT_TOP;
        int currentY = 0; // 当前项的Y坐标（像素，相对于列表顶部）

        for (String item : categoryOrderedList) {
            // 分类标题
            if (item.startsWith("category:")) {
                String categoryKey = item.substring(9); // 去掉 "category:" 前缀
                
                // 检查是否在可见范围内
                if (currentY >= scrollPosition && currentY < scrollPosition + visibleHeight) {
                    // 计算屏幕Y坐标（相对于内容区顶部）
                    // 使用 scrollOffset * ROW_HEIGHT 而不是 (int)scrollPosition，确保与 GL Translate 的 scrollSubOffset 计算一致
                    int rowY = yPos + (currentY - scrollOffset * ROW_HEIGHT);
                    
                    // 获取分类显示名称
                    String categoryName = GameRuleCategoryRegistry.getCategoryDisplayName(categoryKey);
                    
                    // 居中绘制分类标题
                    int textWidth = this.fontRendererObj.getStringWidth(categoryName);
                    int centerX = this.width / 2 - textWidth / 2;
                    this.drawString(this.fontRendererObj, categoryName, centerX, rowY + 4, 0xFFFF55);
                }
                
                currentY += CATEGORY_HEADER_HEIGHT;
                index++;
                continue;
            }
            
            // 规则名
            String ruleName = item;
            GameruleValue originalValue = defaultRules.get(ruleName);
            
            if (originalValue == null) {
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 检查规则行是否与可见区域重叠（考虑项的高度）
            // Check if rule row overlaps with visible area (considering item height)
            int itemBottom = currentY + ROW_HEIGHT;
            if (itemBottom <= scrollPosition || currentY >= scrollPosition + visibleHeight) {
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 计算屏幕Y坐标（相对于内容区顶部）
            // 使用 scrollOffset * ROW_HEIGHT 而不是 (int)scrollPosition，确保与 GL Translate 的 scrollSubOffset 计算一致
            int rowY = yPos + (currentY - scrollOffset * ROW_HEIGHT);

            // 当前显示值（优先 modified -> editable -> default）
            String curStr;
            if (modifiedRules.containsKey(ruleName)) {
                curStr = modifiedRules.get(ruleName);
            } else if (editableRules.containsKey(ruleName)) {
                curStr = editableRules.get(ruleName);
            } else {
                curStr = String.valueOf(originalValue.getOptimalValue());
            }

            // 获取显示名称（优先 本地化 > 注册名称 > 原始规则名）
            // Get display name (priority: localization > registered name > raw rule name)
            String localizedRuleName = GameRuleNameRegistry.getName(ruleName);
            
            // 检查规则是否被修改过，如果是则用不同颜色显示
            // Check if rule was modified, if so display with different color
            boolean isModified = isRuleModified(ruleName);
            int textColor;
            
            // 根据配置项决定是否高亮
            // Check config to decide whether to highlight
            if (isModified && CreateWorldUI.config != null && CreateWorldUI.config.highlightModifiedRulesInGUI) {
                textColor = 0xFFFF55; // 黄色表示已修改 / Yellow for modified
            } else {
                textColor = 0xFFFFFF; // 白色 / White
            }
            
            this.drawString(this.fontRendererObj, localizedRuleName, this.width / 2 - 155, rowY + 6, textColor);
            
            currentY += ROW_HEIGHT;
            index++;
        }
    }

    /**
     * 绘制滚动条。使用scrollPosition（float）计算滑块位置。
     * Draw scrollbar. Uses scrollPosition (float) for slider position calculation.
     */
    private void drawScrollBar() {
        if (maxScrollPosition > 0) {
            int scrollBarX = this.width / 2 + 149;
            int scrollBarY = 60;
            int scrollBarHeight = visibleRows * ROW_HEIGHT;

            drawRect(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0xAA333333);
            drawRect(scrollBarX + 1, scrollBarY + 1, scrollBarX + 9, scrollBarY + scrollBarHeight - 1, 0xAA555555);

            float scrollPercentage = maxScrollPosition > 0 ? (float) scrollPosition / maxScrollPosition : 0;
            
            // 计算总项目数（包括分类标题）
            List<String> categoryOrderedList = buildCategoryOrderedList();
            int totalItems = categoryOrderedList.size();
            
            int sliderHeight = Math.max(20, scrollBarHeight * visibleRows / totalItems);
            int sliderY = scrollBarY + (int) (scrollPercentage * (scrollBarHeight - sliderHeight));

            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight, 0xFF888888);
            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight - 1, 0xFFAAAAAA);
        }
    }

    /**
     * 绘制tooltip。使用调整后的坐标进行悬停检测。
     * Draw tooltips. Uses adjusted coordinates for hover detection.
     */
    private void drawTooltips(int mouseX, int mouseY) {
        int panelBottom = this.height - 50;
        // 仅在内容区域内显示tooltip / Only show tooltip within content area
        if (mouseY < CONTENT_TOP || mouseY > panelBottom) return;
    
        int index = 0;
        int yPos = 60; // 内容区起始Y坐标
        // 补偿GL Translate偏移用于悬停检测 / Compensate GL Translate offset for hover detection
        int adjustedMouseY = mouseY + Math.round(scrollSubOffset);
            
        // 构建分类列表
        List<String> categoryOrderedList = buildCategoryOrderedList();
            
        // 计算可见区域高度（像素）- 使用面板实际高度而不是 visibleRows * ROW_HEIGHT
        // Calculate visible area height (pixels) - use actual panel height instead of visibleRows * ROW_HEIGHT
        int visibleHeight = panelBottom - CONTENT_TOP;
        int currentY = 0; // 当前项的Y坐标（像素，相对于列表顶部）
    
        for (String item : categoryOrderedList) {
            // 分类标题（跳过）
            if (item.startsWith("category:")) {
                currentY += CATEGORY_HEADER_HEIGHT;
                index++;
                continue;
            }
                
            // 规则名
            String ruleName = item;
                
            // 检查规则行是否与可见区域重叠（考虑项的高度）
            // Check if rule row overlaps with visible area (considering item height)
            int itemBottom = currentY + ROW_HEIGHT;
            if (itemBottom <= scrollPosition || currentY >= scrollPosition + visibleHeight) {
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }
    
            // 计算屏幕Y坐标（相对于内容区顶部）
            // 使用 scrollOffset * ROW_HEIGHT 而不是 (int)scrollPosition，确保与 GL Translate 的 scrollSubOffset 计算一致
            int rowY = yPos + (currentY - scrollOffset * ROW_HEIGHT);

            if (isMouseOverRuleName(mouseX, adjustedMouseY, rowY)) {
                List<String> tooltipList = new ArrayList<>();

                // First line: rule name (yellow)
                // 第一行显示规则名（黄色）
                tooltipList.add(EnumChatFormatting.YELLOW + ruleName);

                // Add default value
                // 添加默认値
                GameruleValue defVal = defaultRules.get(ruleName);
                if (defVal != null) {
                    tooltipList.add(EnumChatFormatting.GRAY + I18n.format("createworldui.customize.custom.default") + " " + defVal.getOptimalValue());
                }

                // Add description (if any)
                // 添加描述（若有）
                String tooltip = getRuleTooltip(ruleName);
                if (tooltip != null) {
                    tooltipList.add(EnumChatFormatting.WHITE + tooltip);
                }
                this.func_146283_a(tooltipList, mouseX, mouseY);
            }
            
            currentY += ROW_HEIGHT;
            index++;
        }
    }

    /**
     * 检查规则是否被修改过（与原始值不同）
     * Check if a rule has been modified (different from original value)
     * 
     * @param ruleName 规则名称 / Rule name
     * @return 如果规则被修改过则返回true / True if rule was modified
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

    private boolean isMouseOverRuleName(int mouseX, int mouseY, int rowY) {
        return mouseX >= this.width / 2 - 155 && mouseX <= this.width / 2 + 134 &&
                mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
    }

    /**
     * <p>
     *     两个硬编码注册tooltip的方式（已废弃，请使用 GameRuleTooltipRegistry API）。<br>
     *     {@code registerTooltip} 适合放一个tooltip。<br>
     *     {@code registerTooltips} 适合一次放很多个tooltips。
     * </p>
     * <p>
     *     Two ways to register tooltips (deprecated, please use GameRuleTooltipRegistry API instead).<br>
     *     {@code registerTooltip} is for adding a single tooltip at once<br>
     *     {@code registerToolTips} is for adding multitooltips
     * </p>
     * @deprecated 请使用 {@link GameRuleTooltipRegistry#registerTooltip(String, String)} 代替
     * @deprecated Please use {@link GameRuleTooltipRegistry#registerTooltip(String, String)} instead
     */
    @Deprecated
    public static void registerTooltip(String ruleName, String tooltip) {
        GameRuleTooltipRegistry.registerTooltip(ruleName, tooltip);
    }

    /**
     * @deprecated 请使用 {@link GameRuleTooltipRegistry#registerTooltips(Map)} 代替
     * @deprecated Please use {@link GameRuleTooltipRegistry#registerTooltips(Map)} instead
     */
    @Deprecated
    public static void registerTooltips(Map<String, String> tooltips) {
        GameRuleTooltipRegistry.registerTooltips(tooltips);
    }

    private String getRuleTooltip(String ruleName) {
        // 使用新的 API 获取 tooltip（自动处理优先级：本地化 > 注册 > 默认）
        // Use new API to get tooltip (automatically handles priority: localization > registered > default)
        return GameRuleTooltipRegistry.getTooltip(ruleName);
    }

    /**
     * 将字符串解析为与参考值匹配的类型
     * Parse string to type matching reference value
     *
     * @param text 待解析的字符串 / String to be parsed
     * @param originalValue 参考值（用于确定目标类型） / Reference value (to determine target type)
     * @return 解析后的对应类型值，解析失败返回参考值 / Parsed value of a corresponding type, return reference if parsing fails
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

    /**
     * 保存用户修改的游戏规则
     * 1. 从UI组件中提取修改后的值
     * 2. 更新editableRules映射
     * 3. 如果是在游戏中（有当前世界），立即应用到当前世界
     * 4. 通过GameRuleApplier设置为待应用规则（用于新世界创建）
     * 5. 显示通知告知用户哪些规则被修改
     *
     * Save game rules modified by user
     * 1. Extract modified values from UI components
     * 2. Update editableRules map
     * 3. If in-game (has current world), apply to current world immediately
     * 4. Set as pending rules via GameRuleApplier (for new world creation)
     * 5. Display notification to inform user which rules were modified
     */
    private void saveChanges() {
        LOGGER.info("saveChanges() called");

        // 收集用户修改过的规则（String -> String）
        // Only write rules modified by user (String -> String)
        Map<String, String> result = new HashMap<>();
        changedRules.clear();

        // 比较modifiedRules和editableRules，找出真正被修改的规则
        // Compare modifiedRules and editableRules to find actually changed rules
        for (Map.Entry<String, String> e : modifiedRules.entrySet()) {
            String ruleName = e.getKey();
            String newValue = e.getValue();
            
            if (ruleName != null && newValue != null) {
                result.put(ruleName, newValue);
                
                // 检查是否与原始值不同
                // Check if different from original value
                String originalValue = editableRules.get(ruleName);
                if (originalValue == null || !originalValue.equals(newValue)) {
                    changedRules.add(ruleName);
                }
            }
        }

        // 如果是在游戏中，立即应用到当前世界
        // If in-game, apply to current world immediately
        World currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld != null && !changedRules.isEmpty()) {
            int appliedCount = 0;
            for (String ruleName : changedRules) {
                String newValue = result.get(ruleName);
                if (newValue != null) {
                    boolean success = GameRuleMonitorNSetter.setGamerule(currentWorld, ruleName, newValue);
                    if (success) {
                        appliedCount++;
                    }
                }
            }
            
            // 更新editableRules以反映新值
            // Update editableRules to reflect new values
            editableRules.putAll(result);
            
            LOGGER.info("Applied {} game rules to current world.", appliedCount);
        }

        // 将修改后的规则设置为待应用规则（用于新世界）
        // Set modified rules as pending rules (for new world)
        try {
            GameRuleApplier.setPendingGameRules(result);
            LOGGER.info("Saved {} modified game rules to pendingGameRules.", result.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to set pending game rules: {}", ex.getMessage());
        }

        // 显示通知
        // Display notification
        if (!changedRules.isEmpty()) {
            String notificationText = I18n.format("createworldui.gamerules.notification.changed");
            String rulesList = String.join(", ", changedRules);
            
            String message;
            if (CreateWorldUI.config != null && CreateWorldUI.config.changedRulesInChatHighLighted) {
                // 高亮模式：提示文字白色，规则名黄色
                // Highlight mode: notification text white, rule names yellow
                message = EnumChatFormatting.WHITE + notificationText + 
                         EnumChatFormatting.YELLOW + rulesList;
            } else {
                // 默认模式：全部白色
                // Default mode: all white
                message = EnumChatFormatting.WHITE + notificationText + 
                         EnumChatFormatting.WHITE + rulesList;
            }
            
            if (Minecraft.getMinecraft().ingameGUI != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(
                    new ChatComponentText(message)
                );
            }
            LOGGER.info("Changed rules: {}", changedRules);
        } else {
            String message = I18n.format("createworldui.gamerules.notification.noChanges");
            
            if (Minecraft.getMinecraft().ingameGUI != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(
                    new ChatComponentText(EnumChatFormatting.WHITE + message)
                );
            }
        }
    }

    // 组件包装
    // Component Wrapper
    private static class GuiComponentWrapper {
        public final Object component;
        public final ComponentType type;
        public boolean currentBooleanValue;
        // 全局索引（用于平滑滚动时定位）/ Global index (for positioning during smooth scroll)
        public int globalIndex = 0;
        // 规则名（用于 actionPerformed 中查找）/ Rule name (for lookup in actionPerformed)
        public String ruleName = null;

        public GuiComponentWrapper(Object component, ComponentType type) {
            this.component = component;
            this.type = type;
        }
    }

        /**
         * Enum defining component types.<br>
         * One is button (handles boolean values): BOOLEAN_BUTTON<br>
         * One is text field (handles numbers): TEXT_FIELD
         * <p>
         * 枚举定义的组件类型<br>
         * 一种是按钮（专门处理布尔値）：BOOLEAN_BUTTON<br>
         * 一种是编辑框（处理数字）：TEXT_FIELD
         */
    private enum ComponentType {
        BOOLEAN_BUTTON,
        TEXT_FIELD
    }
}
