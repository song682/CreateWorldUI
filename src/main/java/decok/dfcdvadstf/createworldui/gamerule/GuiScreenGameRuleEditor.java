package decok.dfcdvadstf.createworldui.gamerule;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
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
import net.minecraft.client.gui.GuiButton;
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
import java.util.function.BiConsumer;

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

    // 规则与CatFrame UI组件的映射
    // Map of rules to CatFrame UI components
    private final Map<String, RuleListItem> ruleComponents = new LinkedHashMap<>();

    private Button saveButton;// 保存按钮 / Save button
    private Button cancelButton; // 取消按钮 / Cancel button
    private Button resetButton; // 重置按钮 / Reset button
    
    // 主布局容器（Header-Content-Footer三区域）/ Main layout container (Header-Content-Footer three zones)
    private HeaderFooterLayout mainLayout;
    // 底部按钮布局容器 / Bottom button layout container
    private HorizontalLayout buttonLayout;

    // ===== 滚动相关字段 / Scroll related fields =====

    // 当前滚动位置（像素级，步长为SCROLL_STEP）/ Current scroll position (pixel-level, step = SCROLL_STEP)
    private float scrollPosition = 0f;
    // 上次组件创建时的scrollPosition，用于判断是否需要重建
    private float lastScrollPosition = -1f;
    // 最大滚动位置（像素） / Maximum scroll position (pixels)
    private int maxScrollPosition;

    // 焦点保存：滚动导致组件重建时保存/恢复文本框焦点
    // Focus preservation: save/restore text field focus when components are rebuilt due to scrolling
    private String focusedRuleName = null;

    // 步长 = 1/2 行高 / Step = 1/2 row height
    private static final int SCROLL_STEP = 12;

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
        this.scrollPosition = Math.max(0, Math.min(this.scrollPosition, this.maxScrollPosition));

        // ===== 使用 HeaderFooterLayout 作为主布局 =====
        // ===== Use HeaderFooterLayout as main layout =====
        // 全屏布局：从 y=0 到 y=height
        // Full-screen layout: from y=0 to y=height
        mainLayout = new HeaderFooterLayout(true); // true = 绘制面板背景
        // recalculate 在 Footer 全部设置完后统一调用
        // recalculate called later after all footer setup

        // Footer 区域：底部按钮（高度 30px）
        // Footer zone: bottom buttons (height 30px)
        mainLayout.setFooterHeight(30);
        
        // 创建底部按钮布局
        // Create bottom button layout
        buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(4); // 按钮间距 / Button spacing
        
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
                    createRuleComponents();
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
        
        // 将按钮布局设置到 Footer 区域
        // Set button layout to Footer zone
        mainLayout.setFooter(buttonLayout);

        // ===== Footer / button layout 全部设置完后统一 recalculate =====
        // ===== Unified recalculate after ALL footer / button setup =====
        mainLayout.recalculate(this.width, this.height);

        // CatFrame buttons don't need to be added to buttonList, they're rendered manually
        // CatFrame按钮不需要添加到buttonList，它们由手动渲染

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

        // ===== 保存当前焦点文本框 / Save current focused text field =====
        focusedRuleName = null;
        for (RuleListItem item : ruleComponents.values()) {
            if (item instanceof ValueRuleComponent) {
                ValueRuleComponent vc = (ValueRuleComponent) item;
                if (vc.isFocused()) {
                    focusedRuleName = vc.ruleName;
                    break;
                }
            }
        }

        ruleComponents.clear();
        int index = 0;

        // 构建分类列表
        List<String> categoryOrderedList = buildCategoryOrderedList();
        
        // 计算可见区域高度
        // Calculate visible area height
        int panelBottom = this.height - 50;
        int visibleHeight = panelBottom - CONTENT_TOP;
        int currentY = 0; // 当前项的Y坐标（像素，相对于列表顶部）

        boolean highlightEnabled = CreateWorldUI.config != null && CreateWorldUI.config.highlightModifiedRulesInGUI;

        for (String item : categoryOrderedList) {
            if (item.startsWith("category:")) {
                // 分类标题可见性检查 / Category header visibility check
                if (currentY >= scrollPosition && currentY < scrollPosition + visibleHeight) {
                    int yPos = CONTENT_TOP + currentY - Math.round(scrollPosition);
                    ruleComponents.put(item,
                        new CategoryHeaderComponent(item.substring(9), yPos, this.width));
                }
                currentY += CATEGORY_HEADER_HEIGHT;
                index++;
                continue;
            }
            
            // 规则名
            String ruleName = item;
            GameruleValue value = defaultRules.get(ruleName);

            if (value == null) {
                LOGGER.warn("GameruleValue for {} is null, skipping", ruleName);
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 检查规则行是否与可见区域重叠
            // Check if rule row overlaps with visible area
            int itemBottom = currentY + ROW_HEIGHT;
            if (itemBottom <= scrollPosition || currentY >= scrollPosition + visibleHeight) {
                currentY += ROW_HEIGHT;
                index++;
                continue;
            }

            // 计算屏幕Y坐标
            int yPos = CONTENT_TOP + currentY - Math.round(scrollPosition);

            // 计算显示値 / Calculate display value
            String stringValue = modifiedRules.containsKey(ruleName) ? modifiedRules.get(ruleName)
                : editableRules.containsKey(ruleName) ? editableRules.get(ruleName) : null;
            Object displayObj = stringValue != null
                ? parseFromString(stringValue, value.getOptimalValue())
                : value.getOptimalValue();

            if (displayObj instanceof Boolean) {
                BooleanRuleComponent comp = new BooleanRuleComponent(ruleName, (Boolean) displayObj,
                    yPos, this.width, (rn, newVal) -> {
                        modifiedRules.put(rn, newVal);
                        changedRules.add(rn);
                    });
                if (highlightEnabled && isRuleModified(ruleName)) {
                    comp.setNameColor(0xFFFF55);
                }
                ruleComponents.put(ruleName, comp);
            } else {
                String initial = stringValue != null ? stringValue : String.valueOf(displayObj);
                boolean focused = ruleName.equals(focusedRuleName);
                ValueRuleComponent comp = new ValueRuleComponent(ruleName, initial,
                    yPos, this.width, focused);
                if (highlightEnabled && isRuleModified(ruleName)) {
                    comp.setNameColor(0xFFFF55);
                }
                ruleComponents.put(ruleName, comp);
            }

            currentY += ROW_HEIGHT;
            index++;
        }

        lastScrollPosition = scrollPosition;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        for (RuleListItem item : ruleComponents.values()) {
            if (item instanceof ValueRuleComponent) {
                ValueRuleComponent vc = (ValueRuleComponent) item;
                
                if (vc.isFocused()) {
                    vc.keyTyped(typedChar, keyCode);
                    
                    // 获取用户输入 / Get user input
                    String input = vc.getText();

                    // parsed 仅用于内部展示类型推断，不影响最终保存
                    // parsed only used for internal display type inference, does not affect final saving
                    Object parsed = parseFromString(input, defaultRules.get(vc.ruleName).getOptimalValue());

                    // 真正存储 String → String
                    // Actually Store String to String
                    modifiedRules.put(vc.ruleName, String.valueOf(parsed));
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

        // ===== Footer区域按钮点击检测 / Footer zone button click detection =====
        if (mainLayout != null && mainLayout.getFooterFrame() != null) {
            for (decok.dfcdvadstf.catframe.ui.layouts.ILayout child : mainLayout.getFooterFrame().getChildren()) {
                if (child instanceof HorizontalLayout) {
                    HorizontalLayout hLayout = (HorizontalLayout) child;
                    for (decok.dfcdvadstf.catframe.ui.layouts.ILayout buttonChild : hLayout.getChildren()) {
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

        // ===== 规则组件 - 仅在面板可见区域内交互 / Rule components - only interact within panel visible area =====
        if (mouseY >= CONTENT_TOP && mouseY <= panelBottom) {
            for (RuleListItem item : ruleComponents.values()) {
                item.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } else {
            // 点击面板外 - 取消所有文本框焦点 / Click outside panel - unfocus all text fields
            for (RuleListItem item : ruleComponents.values()) {
                if (item instanceof ValueRuleComponent) {
                    ((ValueRuleComponent) item).setFocused(false);
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
     *     滚轮：即时滚动1/2行（SCROLL_STEP）。<br>
     *     滚动条拖动：直接设置scrollPosition（即时响应）。
     * </p>
     * <p>
     *     Handle mouse input (wheel and scrollbar drag).<br>
     *     Wheel: instant scroll by 1/2 row (SCROLL_STEP).<br>
     *     Scrollbar drag: directly sets scrollPosition (instant response).
     * </p>
     */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        // 处理CyclingButton的滚轮输入 / Handle scroll input for CyclingButton
        if (Mouse.getEventDWheel() != 0) {
            for (RuleListItem item : ruleComponents.values()) {
                if (item instanceof BooleanRuleComponent) {
                    CyclingButton<?> cyclingButton = ((BooleanRuleComponent) item).getToggle();
                    if (cyclingButton.isMouseOver(mouseX, mouseY)) {
                        cyclingButton.mouseScrolled(Mouse.getEventDWheel());
                        return; //  consumed by cycling button
                    }
                }
            }
        }

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
            // 滚动条拖动：即时响应
            // Scrollbar drag: instant response
            this.scrollPosition = newPos;

            if (Math.abs(scrollPosition - lastScrollPosition) > 0.5f) {
                createRuleComponents();
            }
        } else if (Mouse.getEventDWheel() != 0) {
            int scrollAmount = Mouse.getEventDWheel() > 0 ? -1 : 1;
            
            // 滚轮：先重新计算maxScrollPosition，再即时滚动
            // Wheel: recalculate maxScrollPosition first, then instant scroll
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

            // 即时滚动1/2行 / Instant scroll by 1/2 row
            this.scrollPosition += scrollAmount * SCROLL_STEP;
            this.scrollPosition = Math.max(0, Math.min(this.scrollPosition, (float)this.maxScrollPosition));
            createRuleComponents();
        }
    }

    @Override
    public void updateScreen() {
        // EditBox光标更新在render()内部处理，无需额外操作
        // EditBox cursor update handled internally in render(); no extra action needed
    }

    /**
     * <p>
     *     主渲染方法。使用GL Scissor裁剪，放宽SCROLL_STEP像素让1/2组件在边缘可见。<br>
     *     渲染流程：<br>
     *     1. 动态计算可见行数和最大滚动量<br>
     *     2. 绘制背景和面板<br>
     *     3. 启用Scissor裁剪<br>
     *     4. 绘制规则列表和组件<br>
     *     5. 绘制滚动条和tooltip
     * </p>
     * <p>
     *     Main render method. Uses GL Scissor clipping, expanded by SCROLL_STEP for 1/2 component edge visibility.<br>
     *     Render flow:<br>
     *     1. Calculate dynamic visible rows and max scroll position<br>
     *     2. Draw background and panel<br>
     *     3. Enable Scissor clipping<br>
     *     4. Draw rule list and components<br>
     *     5. Draw scrollbar and tooltips
     * </p>
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // ===== 动态计算可见行数和最大滚动量 =====
        // Calculate dynamic visible rows and max scroll position
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

        drawDefaultBackground();

        // ===== 手动绘制面板背景：Header 分隔线 + 内容区背景 + Footer 分隔线 =====
        // ===== Manual panel drawing: header separator + content background + footer separator =====
        int headerSepY = Math.max(0, CONTENT_TOP - ContentPanelRenderer.SEPARATOR_HEIGHT);

        if (CLEAR_MY_BACKGROUND_LOADED) {
            // 有 ClearMyBackground 时，默认背景被清空，需要完整的 CatFrame 面板来框边界
            // With ClearMyBackground: default bg is cleared, need full panel to frame the boundary
            ContentPanelRenderer.drawHeaderSeparator(0, headerSepY, this.width);
            ContentPanelRenderer.drawPanelBackground(0, CONTENT_TOP, this.width, panelBottom - CONTENT_TOP);
            ContentPanelRenderer.drawFooterSeparator(0, panelBottom, this.width);
        } else {
            // 无 ClearMyBackground 时，背景拓宽到 Scissor 裁剪边界（上下各 1/2 行），颜色为黑色
            // Without ClearMyBackground: background expanded to Scissor clip bounds (1/2 row on each side), color black
            int bgTop = CONTENT_TOP - SCROLL_STEP;
            int bgBottom = panelBottom + SCROLL_STEP;
            drawRect(0, bgTop, this.width, bgBottom, 0xFF000000);
            int fadeHeight = 4;
            drawGradientRect(0, bgTop, this.width, bgTop + fadeHeight, 0xCC000000, 0x00000000);
            drawGradientRect(0, bgBottom - fadeHeight, this.width, bgBottom, 0x00000000, 0xCC000000);
        }

        this.drawCenteredString(this.fontRendererObj, Text.translatableString(Tags.MODID, "createworldui.gamerules.title"), this.width / 2, 20, 0xFFFFFF);

        // ===== GL Scissor裁剪：限制绘制区域到内容区，放宽SCROLL_STEP像素让1/2组件在边缘可见 =====
        // GL Scissor clipping: limit drawing to content area, expanded by SCROLL_STEP for 1/2 component edge visibility
        double scaleY = (double) mc.displayHeight / this.height;
        int scissorX = 0;
        int scissorWidth = mc.displayWidth;
        int clipTop = CONTENT_TOP - SCROLL_STEP;
        int clipBottom = panelBottom + SCROLL_STEP;
        int scissorY = (int) Math.floor((this.height - clipBottom) * scaleY);
        int scissorHeight = (int) Math.ceil((clipBottom - clipTop) * scaleY);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        // 绘制规则列表 / Draw rule list
        drawRuleList(mouseX, mouseY, partialTicks);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 绘制滚动条 / Draw scrollbar
        drawScrollBar();

        // ===== 渲染Footer区域的按钮 / Render buttons in Footer zone =====
        // 通过HeaderFooterLayout的FooterFrame自动管理
        // Automatically managed by HeaderFooterLayout's FooterFrame
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

        drawTooltips(mouseX, mouseY);
    }


    /**
     * 绘制规则列表。在GL Translate上下文中调用，组件通过统一的 RuleListItem.render 完成渲染。
     *
     * Draw rule list. Called within GL Translate context, rendering via unified RuleListItem.render.
     */
    private void drawRuleList(int mouseX, int mouseY, float partialTicks) {
        // 更新标签颜色 / Update label colors
        boolean highlightEnabled = CreateWorldUI.config != null && CreateWorldUI.config.highlightModifiedRulesInGUI;

        for (RuleListItem item : ruleComponents.values()) {
            if (item instanceof BooleanRuleComponent) {
                BooleanRuleComponent bc = (BooleanRuleComponent) item;
                bc.setNameColor(
                    (highlightEnabled && isRuleModified(bc.ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            } else if (item instanceof ValueRuleComponent) {
                ValueRuleComponent vc = (ValueRuleComponent) item;
                vc.setNameColor(
                    (highlightEnabled && isRuleModified(vc.ruleName)) ? 0xFFFF55 : 0xFFFFFF);
            }
            item.render(mouseX, mouseY, partialTicks);
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

            float scrollPercentage = maxScrollPosition > 0 ? scrollPosition / maxScrollPosition : 0;
            
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
            int rowY = yPos + (currentY - Math.round(scrollPosition));

            if (isMouseOverRuleName(mouseX, mouseY, rowY)) {
                List<String> tooltipList = new ArrayList<>();

                // First line: rule name (yellow)
                // 第一行显示规则名（黄色）
                tooltipList.add(EnumChatFormatting.YELLOW + ruleName);

                // Add default value
                // 添加默认値
                GameruleValue defVal = defaultRules.get(ruleName);
                if (defVal != null) {
                    tooltipList.add(EnumChatFormatting.GRAY + Text.translatableString(Tags.MODID, "createworldui.customize.custom.default") + " " + defVal.getOptimalValue());
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

    // ============================================================
    // 三种规则列表组件类型 / Three rule list component types
    // ============================================================

    /**
     * Generic interface for all rule list items (render + interaction).
     * 所有规则列表项目的通用接口（渲染 + 交互）。
     */
    private interface RuleListItem {
        void render(int mouseX, int mouseY, float partialTicks);
        default void mouseClicked(int mouseX, int mouseY, int mouseButton) {}
        default void keyTyped(char typedChar, int keyCode) {}
    }

    /**
     * Category header — a centered singleton StringWidget showing category name.
     * 分类标题——居中单例 StringWidget，显示分类名称。
     */
    private static class CategoryHeaderComponent implements RuleListItem {
        private final StringWidget label;

        CategoryHeaderComponent(String categoryKey, int y, int parentWidth) {
            String displayName = GameRuleCategoryRegistry.getCategoryDisplayName(categoryKey);
            int textWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(displayName);
            this.label = new StringWidget(displayName, 0xFFFF55);
            this.label.setX(parentWidth / 2 - textWidth / 2);
            this.label.setY(y + 4);
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            label.render(mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Boolean rule row — StringWidget (rule name) + CyclingButton (on/off toggle).
     * 布尔规则行——StringWidget（规则名）+ CyclingButton（开关）。
     */
    private static class BooleanRuleComponent implements RuleListItem {
        final String ruleName;
        private final StringWidget nameLabel;
        private final CyclingButton<Boolean> toggle;

        BooleanRuleComponent(String ruleName, boolean initialValue, int y, int parentWidth,
                             BiConsumer<String, String> onChange) {
            this.ruleName = ruleName;

            String localizedName = GameRuleNameRegistry.getName(ruleName);
            this.nameLabel = new StringWidget(localizedName, 0xFFFFFF);
            this.nameLabel.setX(parentWidth / 2 - 155);
            this.nameLabel.setY(y + 6);

            this.toggle = CyclingButton.<Boolean>onOffBuilder()
                .values(true, false)
                .initially(initialValue)
                .label(Text.literal(""))
                .useVanillaTexture(false)
                .build(parentWidth / 2 + 90, y, 44, 20,
                    (btn, newVal) -> onChange.accept(ruleName, String.valueOf(newVal)));
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            nameLabel.render(mouseX, mouseY, partialTicks);
            toggle.render(mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            toggle.mouseClicked(mouseX, mouseY, mouseButton);
        }

        void setNameColor(int color) {
            nameLabel.setColor(color);
        }

        CyclingButton<Boolean> getToggle() {
            return toggle;
        }
    }

    /**
     * Value rule row — StringWidget (rule name) + SimpleEditBox (text input).
     * 値规则行——StringWidget（规则名）+ SimpleEditBox（文本输入）。
     */
    private static class ValueRuleComponent implements RuleListItem {
        final String ruleName;
        final SimpleEditBox editBox;
        private final StringWidget nameLabel;

        ValueRuleComponent(String ruleName, String initialValue, int y, int parentWidth,
                           boolean focused) {
            this.ruleName = ruleName;

            String localizedName = GameRuleNameRegistry.getName(ruleName);
            this.nameLabel = new StringWidget(localizedName, 0xFFFFFF);
            this.nameLabel.setX(parentWidth / 2 - 155);
            this.nameLabel.setY(y + 6);

            this.editBox = new SimpleEditBox(parentWidth / 2 + 90, y, 44, 20);
            this.editBox.setText(initialValue);
            this.editBox.setMaxLength(200);
            this.editBox.setUseVanillaTexture(false);
            this.editBox.setForceVerticalCursor(true);
            if (focused) {
                this.editBox.setFocused(true);
            }
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            nameLabel.render(mouseX, mouseY, partialTicks);
            editBox.render(mouseX, mouseY, partialTicks);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            editBox.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (editBox.isFocused()) {
                editBox.keyTyped(typedChar, keyCode);
            }
        }

        boolean isFocused() {
            return editBox.isFocused();
        }

        void setFocused(boolean focused) {
            editBox.setFocused(focused);
        }

        void setNameColor(int color) {
            nameLabel.setColor(color);
        }

        String getText() {
            return editBox.getText();
        }
    }
}
