package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

/**
 * <p>
 *     游戏规则编辑器（用于创建世界前编辑待应用的游戏规则）<br>
 *     功能说明：<br>
 *     - 显示来源：通过GameRuleMonitorNSetter从当前世界获取所有游戏规则（GameRuleMonitorNSetter.getAllGamerules(currentWorld)）<br>
 *     - 保存目标：通过GameRuleApplier将修改后的规则设置为待应用规则（GameRuleApplier.setPendingGameRules(Map<String,String>)）<br>
 *     - 核心处理类：GameRuleMonitorNSetter
 * </p>
 * <p>
 *     Game rule editor (for editing pending game rules before world creation)<br>
 *     Function description:<br>
 *     - Data source: Get all game rules from the current world via GameRuleMonitorNSetter
 *     - Save target: Set modified rules as pending rules via GameRuleApplier
 *     - Core handler: GameRuleMonitorNSetter
 * </p>
 * <p>
 *     注意：所有保存到待应用规则的值均转换为字符串（与GameRules存储格式一致）<br>
 *     Note: All values saved to pending rules are converted to strings (consistent with GameRules storage format)
 * </p>
 */

@SuppressWarnings("unchecked")
public class GuiScreenGameRuleEditor extends GuiScreen {

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

    // 规则与UI组件的映射
    // Map of rules to UI components
    private final Map<String, GuiComponentWrapper> ruleComponents = new LinkedHashMap<>();

    private GuiButton saveButton;// 保存按钮 / Save button
    private GuiButton cancelButton; // 取消按钮 / Cancel button
    private GuiButton resetButton; // 重置按钮 / Reset button
    private static final Map<String, String> hardcodeToolTip = new HashMap<>();// 允许外部模组自己添加自己的描述 / Allow developers to add their own tooltips from external.

    private int scrollOffset = 0; // 滚动偏移量 / Scroll offset
    private int maxScrollOffset; // 最大滚动偏移量 / Maximum scroll offset
    private static final int ROW_HEIGHT = 25; // 行高 / Row height
    private int visibleRows = 8; // 可见行数 / Number of visible rows
    private boolean isScrolling = false; // 是否正在滚动 / Whether scrolling is in progress
    private GuiScreen parentScreen; // 父界面 / Parent screen
    private static final int PANEL_TOP = 50;

    /**
     * 构造游戏规则编辑器<br>
     * Constructor for GameRuleEditor
     * @param parentScreen 父界面（创建世界界面） / Parent screen (world creation screen)
     * @param editableRules 可编辑的游戏规则映射 / Editable game rule map
     */
    public GuiScreenGameRuleEditor(GuiScreen parentScreen, Map<String, String> editableRules) {
        this.parentScreen = parentScreen;
        LOGGER.error("GameRuleEditor CONSTRUCTOR CALLED");

        // 过滤掉 null 值，确保 editableRules 不包含 null
        this.editableRules = new HashMap<>();
        if (editableRules != null) {
            for (Map.Entry<String, String> entry : editableRules.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    this.editableRules.put(entry.getKey(), entry.getValue());
                }
            }
        }

        /**
         * 读取默认规则的顺序：
         * 1) 如果 editableRules 不为空，优先使用其中的值构建默认规则
         * 2) 否则尝试从真实世界读取（如果世界不为null）
         * 3) 如果都失败，回退到新的GameRules实例（使用原版默认值）
         */
        Map<String, GameruleValue> defaultsFromMonitor = null;

        // 方法1: 如果 editableRules 不为空，优先使用其中的值构建默认规则
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

        // 方法2: 尝试从真实世界获取
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

        // 方法3: 回退到临时 GameRules 实例
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

        // 预填modifiedRules
        for (Map.Entry<String,String> e : this.editableRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                this.modifiedRules.put(e.getKey(), e.getValue());
            }
        }

        this.maxScrollOffset = Math.max(0, defaultRules.size() - visibleRows);

        // 确保 buttonList 不为 null
        if (this.buttonList == null) {
            this.buttonList = new ArrayList<>();
        }
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
        LOGGER.error("GameRuleEditor initGui()");
        Keyboard.enableRepeatEvents(true);

        this.buttonList.clear();

        int panelBottom = this.height - 50;
        this.visibleRows = Math.max(1, (panelBottom - PANEL_TOP) / ROW_HEIGHT);
        this.maxScrollOffset = Math.max(0, defaultRules.size() - this.visibleRows);

        // 按钮布局
        if (CreateWorldUI.config.enableResetButton) {
            this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 100, 20, I18n.format("options.save"));
            this.cancelButton = new GuiButton(1, this.width / 2 - 50, this.height - 30, 100, 20, I18n.format("gui.cancel"));
            this.resetButton = new GuiButton(2, this.width / 2 + 54, this.height - 30, 100, 20, I18n.format("options.reset"));
        } else {
            this.cancelButton = new GuiButton(1, this.width / 2 + 50, this.height - 30, 120, 20, I18n.format("gui.cancel"));
            this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 120, 20, I18n.format("options.save"));
        }

        // 确保添加的按钮不为 null
        if (this.saveButton != null) this.buttonList.add(this.saveButton);
        if (this.cancelButton != null) this.buttonList.add(this.cancelButton);
        if (this.resetButton != null) this.buttonList.add(this.resetButton);

        createRuleComponents();
        LOGGER.error("initGui() called");
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
     * 创建并布局规则组件（布尔值使用按钮，其他类型使用文本框）
     *
     * Create and layout rule components (boolean uses button, other types use text field)
     */
    private void createRuleComponents() {
        LOGGER.info("createRuleComponents() START, total rules = {}", defaultRules.size());

        // 安全检查：确保 buttonList 不为 null
        if (this.buttonList == null) {
            LOGGER.error("buttonList is null! Initializing...");
            this.buttonList = new ArrayList<>();
            return;
        }

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

        for (Map.Entry<String, GameruleValue> entry : defaultRules.entrySet()) {
            String ruleName = entry.getKey();
            GameruleValue value = entry.getValue();

            // 确保 value 不为 null
            if (value == null) {
                LOGGER.warn("GameruleValue for {} is null, skipping", ruleName);
                index++;
                continue;
            }

            // 不在可见行中则跳过
            if (index < scrollOffset || index >= scrollOffset + visibleRows) {
                index++;
                continue;
            }

            int yPos = 60 + (index - scrollOffset) * ROW_HEIGHT;

            // 计算显示值
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
                ruleComponents.put(ruleName, wrapper);
            }

            visibleUIRowIndex++;
            index++;
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

        LOGGER.error("Add rule component: {}, value = {}", ruleName, value);

        // 布尔按钮
        if (value instanceof Boolean) {
            boolean boolValue = (Boolean) value;
            String display = boolValue ? I18n.format("options.on") : I18n.format("options.off");

            GuiButton button = new GuiButton(id, componentX, yPos, componentWidth, 20, display);

            // 确保按钮不为 null 再添加到 buttonList
            if (button != null) {
                this.buttonList.add(button);
                return new GuiComponentWrapper(button, ComponentType.BOOLEAN_BUTTON);
            }
            return null;
        }

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
                createRuleComponents();
                return;
        }

        // 处理布尔值按钮（ID >= 100）
        // Handle boolean buttons (ID >= 100)
        if (id >= 100) {
            int visibleUIRowIndex =  id - 100;
            int globalIndex = scrollOffset + visibleUIRowIndex;

            // 按顺序找第 ruleIndex 个键
            String ruleName = getRuleNameByIndex(globalIndex);

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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 文本框交互
        // Text Field interaction
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // 检查是否点击滚动条区域（用于拖动）
        // Check whether click the scroll bar.
        int scrollBarX = this.width / 2 - 149;
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
     * handleMouseInput
     */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (this.isScrolling) {
            int scrollBarY = 60;
            int scrollBarHeight = visibleRows * ROW_HEIGHT;

            float relativePosition = (float) (mouseY - scrollBarY) / (float) scrollBarHeight;
            this.scrollOffset = (int) (relativePosition * (this.maxScrollOffset));
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset));

            createRuleComponents();
        } else if (Mouse.getEventDWheel() != 0) {
            int scrollAmount = Mouse.getEventDWheel() > 0 ? -1 : 1;
            this.scrollOffset += scrollAmount;
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset));
            createRuleComponents();
        }
    }

    @Override
    public void updateScreen() {
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.updateCursorCounter();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawContentPanel();

        // 动态计算可见行数 / 最大滚动量
        // Calculate the dynamic visible rule roles / maximum scrolling amount
        int panelBottom = this.height - 50;
        this.visibleRows = Math.max(1, (panelBottom - PANEL_TOP) / ROW_HEIGHT);
        this.maxScrollOffset = Math.max(0, defaultRules.size() - this.visibleRows);

        this.drawCenteredString(this.fontRendererObj, I18n.format("createworldui.gamerules.title"), this.width / 2, 20, 0xFFFFFF);

        drawRuleList(mouseX, mouseY);

        // 组件渲染 - 添加空值检查
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
                        button.drawButton(this.mc, mouseX, mouseY);
                    }
                }
            }
        }

        drawScrollBar();

        // 在调用 super.drawScreen 前，确保 buttonList 中没有 null 元素
        List<GuiButton> cleanButtonList = new ArrayList<>();
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton btn = (GuiButton) obj;
                if (btn != null) {
                    cleanButtonList.add(btn);
                }
            }
        }
        this.buttonList.clear();
        this.buttonList.addAll(cleanButtonList);

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawTooltips(mouseX, mouseY);
    }

    private void drawContentPanel() {
        int panelLeft = 0;
        int panelRight = this.width;
        int panelBottom = this.height - 50;

        // 深色背景
        drawGradientRect(panelLeft, PANEL_TOP, panelRight, panelBottom, 0xC0101010, 0xD0101010);

        // 内边线（可选）
        drawRect(panelLeft, PANEL_TOP, panelRight, PANEL_TOP + 1, 0xFF000000);  // top
        drawRect(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF000000); // bottom
    }

    private void drawRuleList(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 60;

        for (Map.Entry<String, GameruleValue> entry : defaultRules.entrySet()) {
            if (index >= scrollOffset && index < scrollOffset + visibleRows) {
                String ruleName = entry.getKey();
                GameruleValue originalValue = entry.getValue();

                // 当前显示值（优先 modified -> editable -> default）
                String curStr;
                if (modifiedRules.containsKey(ruleName)) {
                    curStr = modifiedRules.get(ruleName);
                } else if (editableRules.containsKey(ruleName)) {
                    curStr = editableRules.get(ruleName);
                } else {
                    curStr = String.valueOf(originalValue.getOptimalValue());
                }

                int rowY = yPos + (index - scrollOffset) * ROW_HEIGHT;

                this.drawString(this.fontRendererObj, ruleName, this.width / 2 - 155, rowY + 6, 0xFFFFFF);
            }
            index++;
        }
    }

    private void drawScrollBar() {
        if (maxScrollOffset > 0) {
            int scrollBarX = this.width / 2 + 149;
            int scrollBarY = 60;
            int scrollBarHeight = visibleRows * ROW_HEIGHT;

            drawRect(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0xAA333333);
            drawRect(scrollBarX + 1, scrollBarY + 1, scrollBarX + 9, scrollBarY + scrollBarHeight - 1, 0xAA555555);

            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int sliderHeight = Math.max(20, scrollBarHeight / (maxScrollOffset + visibleRows) * visibleRows);
            int sliderY = scrollBarY + (int) (scrollPercentage * (scrollBarHeight - sliderHeight));

            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight, 0xFF888888);
            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight - 1, 0xFFAAAAAA);
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 60;

        for (String ruleName : defaultRules.keySet()) {
            if (index >= scrollOffset && index < scrollOffset + visibleRows) {
                int rowY = yPos + (index - scrollOffset) * ROW_HEIGHT;

                if (isMouseOverRuleName(mouseX, mouseY, rowY)) {
                    List<String> tooltipList = new ArrayList<>();

                    // 第一行显示规则名（黄色）
                    tooltipList.add(EnumChatFormatting.YELLOW + ruleName);

                    // 添加默认值
                    GameruleValue defVal = defaultRules.get(ruleName);
                    if (defVal != null) {
                        tooltipList.add(EnumChatFormatting.GRAY + I18n.format("createworldui.customize.custom.default") + " " + defVal.getOptimalValue());
                    }

                    // 添加描述（若有）
                    String tooltip = getRuleTooltip(ruleName);
                    if (tooltip != null) {
                        tooltipList.add(EnumChatFormatting.WHITE + tooltip);
                    }
                    this.func_146283_a(tooltipList, mouseX, mouseY);
                }
            }
            index++;
        }
    }

    private boolean isMouseOverRuleName(int mouseX, int mouseY, int rowY) {
        return mouseX >= this.width / 2 - 155 && mouseX <= this.width / 2 + 134 &&
                mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
    }

    /**
     * <p>
     *     两个硬编码注册tooltip的方式。<br>
     *     {@code registerTooltip} 适合放一个tooltip。<br>
     *     {@code registerTooltips} 适合一次放很多个tooltips。
     * </p>
     * <p>
     *     Two ways to register tooltips.<br>
     *     {@code registerTooltip} is for adding a single tooltip at once
     *     {@code registerToolTips} is for adding multitooltips
     * </p>
     */
    public static void registerTooltip(String ruleName, String tooltip) {
        hardcodeToolTip.put(ruleName, tooltip);
    }

    public static void registerTooltips(Map<String, String> tooltips) {
        if (tooltips != null) hardcodeToolTip.putAll(tooltips);
    }

    private String getRuleTooltip(String ruleName) {
        String translationKey = "gamerule." + ruleName + ".tooltip.description";
        String translated = I18n.format(translationKey);
        if (translated != null && !translated.equals(translationKey)) {
            return translated;
        }

        // 其次查 mod 自己注册的 tooltip（允许外部覆盖）
        if (hardcodeToolTip.containsKey(ruleName)) return hardcodeToolTip.get(ruleName);

        if (translated.equals(translationKey)) {
            Map<String, String> defaultDescriptions = new HashMap<>();
            defaultDescriptions.put("doFireTick", "Controls whether fire spreads and naturally extinguishes");
            defaultDescriptions.put("mobGriefing", "Controls whether mobs can destroy blocks");
            defaultDescriptions.put("keepInventory", "Keep inventory after death");
            defaultDescriptions.put("doMobSpawning", "Natural mob spawning");
            defaultDescriptions.put("doMobLoot", "Mobs drop loot");
            defaultDescriptions.put("doTileDrops", "Blocks drop items when destroyed");
            defaultDescriptions.put("commandBlockOutput", "Command blocks output to chat");
            defaultDescriptions.put("naturalRegeneration", "Natural health regeneration");
            defaultDescriptions.put("doDaylightCycle", "Day/night cycle");
            return defaultDescriptions.get(ruleName);
        }
        return translated;
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
     * 3. 通过GameRuleApplier设置为待应用规则
     *
     * Save game rules modified by user
     * 1. Extract modified values from UI components
     * 2. Update editableRules map
     * 3. Set as pending rules via GameRuleApplier
     */
    private void saveChanges() {
        LOGGER.info("saveChanges() called");

        // 仅写入用户修改过的规则（String → String）
        Map<String, String> result = new HashMap<>();

        // 从UI组件提取值并更新modifiedRules
        // Extract values from UI components and update modifiedRules and editableRules
        for (Map.Entry<String, String> e : modifiedRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                result.put(e.getKey(), e.getValue());
            }
        }

        // 将修改后的规则设置为待应用规则
        // Set modified rules as pending rules
        try {
            GameRuleApplier.setPendingGameRules(result);
            LOGGER.info("Saved {} modified game rules to pendingGameRules.", result.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to set pending game rules: {}", ex.getMessage());
        }
    }

    // 组件包装
    // Component Wrapper
    private static class GuiComponentWrapper {
        public final Object component;
        public final ComponentType type;
        public boolean currentBooleanValue;

        public GuiComponentWrapper(Object component, ComponentType type) {
            this.component = component;
            this.type = type;
        }
    }

    /**
     * <p>
     *     枚举定义的组件类型<br>
     *     一种是按钮（专门处理布尔值）：BOOLEAN_BUTTON<br>
     *     一种是编辑框（处理数字）：TEXT_FIELD
     * </p>
     */
    private enum ComponentType {
        BOOLEAN_BUTTON,
        TEXT_FIELD
    }
}