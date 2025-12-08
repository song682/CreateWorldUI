package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;

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
public class GameRuleEditor extends GuiScreen {

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

    private int scrollOffset = 0; // 滚动偏移量 / Scroll offset
    private int maxScrollOffset = 0; // 最大滚动偏移量 / Maximum scroll offset
    private static final int ROW_HEIGHT = 25; // 行高 / Row height
    private static final int VISIBLE_ROWS = 8; // 可见行数 / Number of visible rows
    private boolean isScrolling = false; // 是否正在滚动 / Whether scrolling is in progress
    private GuiScreen parentScreen; // 父界面 / Parent screen

    /**
     * 构造游戏规则编辑器<br>
     * Constructor for GameRuleEditor
     * @param parentScreen 父界面（创建世界界面） / Parent screen (world creation screen)
     * @param editableRules 可编辑的游戏规则映射 / Editable game rule map
     */
    public GameRuleEditor(GuiScreen parentScreen, Map<String, String> editableRules) {
        this.parentScreen = parentScreen;
        LOGGER.error("GameRuleEditor CONSTRUCTOR CALLED");
        this.editableRules = (editableRules != null) ? editableRules : new HashMap<>();

        /**
         * <p>
         *     读取默认规则的顺序：<br>
         *     1) 先尝试从真实世界（client.theWorld）通过GameRuleMonitorNSetter读取（如果世界不为null）<br>
         *     2) 如果没有世界或监视器返回空，则回退到新的GameRules实例（使用原版默认值）
         * </p>
         * <p>
         *     Order of reading default rules:<br>
         *     1) First try to read from a real world (client.theWorld) via GameRuleMonitorNSetter (if the world is not null)<br>
         *     2) If no world or monitor returns empty, fall back to new GameRules instance (using vanilla defaults)
         * </p>
         */
        Map<String, GameruleValue> defaultsFromMonitor = null;
        try {
            World w = Minecraft.getMinecraft() != null ? Minecraft.getMinecraft().theWorld : null;
            if (w != null) {
                defaultsFromMonitor = GameRuleMonitorNSetter.getAllGamerules(w);
            }
        } catch (Throwable t) {
            LOGGER.warn("Error while trying to get defaults from MonitorNSetter: {}", t.getMessage());
            defaultsFromMonitor = null;
        }
        if (defaultsFromMonitor == null || defaultsFromMonitor.isEmpty()) {
            // 回退：使用临时GameRules实例（不依赖世界）获取原版默认值
            // Fallback: Use temporary GameRules instance (world-independent) to get vanilla defaults
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
        // 确保展示的规则包含defaultRules和editableRules的并集（如果editable有额外规则）
        // Ensure displayed rules include union of defaultRules and editableRules (if editable has extra rules)
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

        // 预填modifiedRules：优先使用editableRules（用户之前保存过的）
        // Pre-fill modifiedRules: prefer editableRules (previously saved by user)
        for (Map.Entry<String,String> e : this.editableRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                this.modifiedRules.put(e.getKey(), e.getValue());
            }
        }
        this.maxScrollOffset = Math.max(0, defaultRules.size() - VISIBLE_ROWS);
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

        // 按钮布局（左右居中排列）
        // Button layout (centered horizontally)
        this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 100, 20, I18n.format("options.save"));
        this.cancelButton = new GuiButton(1, this.width / 2 - 50, this.height - 30, 100, 20, I18n.format("gui.cancel"));
        this.resetButton = new GuiButton(2, this.width / 2 + 54, this.height - 30, 100, 20, I18n.format("options.reset"));

        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.cancelButton);
        this.buttonList.add(this.resetButton);

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

        // 删除旧的在用户屏幕上不可见的布尔按钮(在用户界面滚动时，防止其两个 index 不对应)
        // Remove old and disappeared from user's UI boolean buttons, while the user's UI is scrolling, and preventing from two of the index cannot correspond.
        Iterator<GuiButton> it = this.buttonList.iterator();
        while (it.hasNext()) {
            GuiButton btn = it.next();
            if (btn.id >= 100) it.remove();
        }

        ruleComponents.clear();
        int index = 0;
        int visibleUIRowIndex = 0;

        // 不在可见行中则跳过（用于滚动）
        // Skip if not in visible rows (for scrolling)
        for (Map.Entry<String, GameruleValue> entry : defaultRules.entrySet()) {
            String ruleName = entry.getKey();
            GameruleValue value = entry.getValue();

            // 不在可见行中则跳过（用于滚动）
            // Skip if not in visible rows (for scrolling)
            if (index < scrollOffset || index >= scrollOffset + VISIBLE_ROWS) {
                index++;
                continue;
            }

            // 计算Y坐标
            // Calculate Y coordinate
            int yPos = 60 + (index - scrollOffset) * ROW_HEIGHT;

            // 计算显示值：modified > editable > default
            // Calculate display value: modified > editable > default
            Object displayObj;

            if (modifiedRules.containsKey(ruleName)) {
                // modifiedRules 里面永远是 String → 需要解析回基本类型用于显示
                displayObj = parseFromString(modifiedRules.get(ruleName), value.getOptimalValue());
            } else if (editableRules.containsKey(ruleName)) {
                displayObj = parseFromString(editableRules.get(ruleName), value.getOptimalValue());
            } else {
                displayObj = value.getOptimalValue();
            }

            // 创建组件
            // Create component
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
        int componentX = this.width / 2 + 90; // 空间位置
        int componentWidth = 44;

        LOGGER.error("Add rule component: {}, value = {}", ruleName, value);

        // 布尔按钮
        // Boolean button
        if (value instanceof Boolean) {
            boolean boolValue = (Boolean) value;

            // 根据实际值初始化显示文本
            // Initialize display text based on actual value
            String display = boolValue ? I18n.format("options.on") : I18n.format("options.off");

            GuiButton button = new GuiButton(id, componentX, yPos, componentWidth, 20, display);

            this.buttonList.add(button);

            return new GuiComponentWrapper(button, ComponentType.BOOLEAN_BUTTON);
        }

        // 数字/字符串使用文本输入框
        // Number/string uses text field
        GuiTextField textField =
                new GuiTextField(this.fontRendererObj, componentX, yPos, componentWidth, 20);

        String initial; // 初始文本 / Initial text
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
        textField.setMaxStringLength(200); // 设置最大字符串长度 / Set maximum string length

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
     * 获取当前游戏规则ID所对应的布尔值（modified > editable > default）
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
        int scrollBarX = this.width / 2 - 10;
        int scrollBarY = 60;
        int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

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
            int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

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
        LOGGER.error("GameRuleEditor drawScreen()");
        drawDefaultBackground();
        drawContentPanel();

        // 标题
        this.drawCenteredString(this.fontRendererObj, I18n.format("createworldui.gamerules.title"), this.width / 2, 20, 0xFFFFFF);

        // 规则列表（名字 + 默认值提示）
        drawRuleList(mouseX, mouseY);

        // 组件渲染
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.drawTextBox();
            } else if (wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                GuiButton button = (GuiButton) wrapper.component;
                button.drawButton(this.mc, mouseX, mouseY);
            }
        }

        // 滚动条
        drawScrollBar();

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 悬停提示
        drawTooltips(mouseX, mouseY);
        LOGGER.error(" buttonList = {}", this.buttonList.size());
    }

    private void drawContentPanel() {
        int panelTop    = 50;
        int panelBottom = this.height - 50;
        int panelLeft   = 0;
        int panelRight  = this.width;

        // 深色背景
        drawGradientRect(panelLeft, panelTop, panelRight, panelBottom, 0xC0101010, 0xD0101010);

        // 内边线（可选）
        drawRect(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF000000);  // top
        drawRect(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF000000); // bottom
    }

    private void drawRuleList(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 60;

        for (Map.Entry<String, GameruleValue> entry : defaultRules.entrySet()) {
            if (index >= scrollOffset && index < scrollOffset + VISIBLE_ROWS) {
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
            int scrollBarX = this.width / 2 - 10;
            int scrollBarY = 60;
            int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

            drawRect(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0xAA333333);
            drawRect(scrollBarX + 1, scrollBarY + 1, scrollBarX + 9, scrollBarY + scrollBarHeight - 1, 0xAA555555);

            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int sliderHeight = Math.max(20, scrollBarHeight / (maxScrollOffset + VISIBLE_ROWS) * VISIBLE_ROWS);
            int sliderY = scrollBarY + (int) (scrollPercentage * (scrollBarHeight - sliderHeight));

            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight, 0xFF888888);
            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight - 1, 0xFFAAAAAA);
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 60;

        for (String ruleName : defaultRules.keySet()) {
            if (index >= scrollOffset && index < scrollOffset + VISIBLE_ROWS) {
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

    private String getRuleTooltip(String ruleName) {
        String translationKey = "gamerule." + ruleName + ".tooltip.description";
        String translated = I18n.format(translationKey);
        if (translated != null && !translated.equals(translationKey)) {
            return translated;
        }

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
     * @return 解析后的对应类型值，解析失败返回参考值 / Parsed value of corresponding type, return reference if parsing fails
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
