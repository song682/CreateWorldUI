package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.gamerule.GameRuleMonitorNSetter.GameruleValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

/// GameRule 编辑器（用于创建世界前编辑 pendingGameRules）
/// 设计：
/// - 显示来源：GameRuleMonitorNSetter.getAllGamerules(currentWorld)
/// - 保存目标：GameRuleApplier.setPendingGameRules(Map<String,String>)
/// 注意：所有保存到 pending 的值都转为 String（与 GameRules 存储一致）
@SuppressWarnings("unchecked")
public class GameRuleEditor extends GuiScreen {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleEditor");

    // 待写入 Applier 的 Map (String -> String)
    private final Map<String, String> editableRules;

    // 默认/原始规则信息（带丰富类型）
    private final Map<String, GameruleValue> defaultRules;

    // 临时保存用户在 UI 中修改的值（String）
    private final Map<String, String> modifiedRules = new HashMap<>();

    // Rule -> UI component wrapper
    private final Map<String, GuiComponentWrapper> ruleComponents = new LinkedHashMap<>();

    private GuiButton saveButton;
    private GuiButton cancelButton;
    private GuiButton resetButton;

    private int scrollOffset = 0;
    private int maxScrollOffset =   0;
    private static final int ROW_HEIGHT = 25;
    private static final int VISIBLE_ROWS = 8;
    private boolean isScrolling = false;
    private GuiScreen parentScreen;

    public GameRuleEditor(Map<String, String> editableRules) {
    LOGGER.error("GameRuleEditor CONSTRUCTOR CALLED");
    this.editableRules = (editableRules != null) ? editableRules : new HashMap<>();

    /**
     * 读取默认 rules 的顺序：
     * 1) 先尝试从真实 world（client.theWorld）通过 MonitorNSetter 读取（如果 theWorld != null）
     * 2) 如果没有 world 或 Monitor 返回为空，则回退为 new GameRules()（原版默认值）
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
        // 回退：使用临时 GameRules 实例（不依赖 world），以获取原版默认值
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
    this.defaultRules = (   defaultsFromMonitor != null) ? new LinkedHashMap<>(defaultsFromMonitor) : new LinkedHashMap<>();
    // 最终保证展示时使用的是 defaultRules 的 keys 与 editableKeys 的并集（如果 editable 有额外 key）
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
    // 预填 modifiedRules：优先使用 editableRules（用户之前保存过的）
    for (Map.Entry<String,String> e : this.editableRules.entrySet()) {
        if (e.getKey() != null && e.getValue() != null) {
            this.modifiedRules.put(e.getKey(), e.getValue());
        }
    }
    this.maxScrollOffset = Math.max(0, defaultRules.size() - VISIBLE_ROWS);
}


    @Override
    public void initGui() {
        LOGGER.error("GameRuleEditor initGui()");
        Keyboard.enableRepeatEvents(true);

        this.buttonList.clear();

        // 按钮布局（左右居中排列）
        this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 100, 20, I18n.format("options.save"));
        this.cancelButton = new GuiButton(1, this.width / 2 - 50, this.height - 30, 100, 20, I18n.format("gui.cancel"));
        this.resetButton = new GuiButton(2, this.width / 2 + 54, this.height - 30, 100, 20, I18n.format("options.reset"));

        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.cancelButton);
        this.buttonList.add(this.resetButton);

        createRuleComponents();
        LOGGER.error("GameRuleEditor: initGui() called");
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * 创建并布局规则组件（布尔使用按钮，其他类型使用文本框）
     */
    private void createRuleComponents() {
        LOGGER.error("GameRuleEditor: createRuleComponents() START, total rules = " + defaultRules.size());

        ruleComponents.clear();
        int index = 0;

        for (Map.Entry<String, GameruleValue> entry : defaultRules.entrySet()) {
            String ruleName = entry.getKey();
            GameruleValue value = entry.getValue();

            // 不在可见行中跳过（用于滚动）
            if (index < scrollOffset || index >= scrollOffset + VISIBLE_ROWS) {
                index++;
                continue;
            }

            // 计算 Y 坐标
            int yPos = 60 + (index - scrollOffset) * ROW_HEIGHT;

            // ----------------------------
            // 计算显示值：modified > editable > default
            // ----------------------------
            Object displayObj;

            if (modifiedRules.containsKey(ruleName)) {
                // modifiedRules 里面永远是 String → 需要解析回基本类型用于显示
                displayObj = parseFromString(modifiedRules.get(ruleName), value.getOptimalValue());
            } else if (editableRules.containsKey(ruleName)) {
                displayObj = parseFromString(editableRules.get(ruleName), value.getOptimalValue());
            } else {
                displayObj = value.getOptimalValue();
            }

            // ----------------------------
            // 创建组件
            // ----------------------------
            GuiComponentWrapper wrapper = createComponentForRule(ruleName, displayObj, yPos, index);
            if (wrapper != null) {
                ruleComponents.put(ruleName, wrapper);
            }

            index++;
        }
    }

    private GuiComponentWrapper createComponentForRule(String ruleName, Object value, int yPos, int index) {
        int componentWidth = 150;
        int componentX = this.width / 2 + 20;

        LOGGER.error("GameRuleEditor: add rule component: " + ruleName + " value=" + value);


        /**
         * 布尔按钮
         */
        if (value instanceof Boolean) {
            boolean boolValue = (Boolean) value;

            // 根据实际值初始化显示
            String display = boolValue ? I18n.format("options.on") : I18n.format("options.off");

            GuiButton button = new GuiButton(
                    100 + index,              // ID
                    componentX,
                    yPos,
                    componentWidth,
                    20,
                    display
            );

            this.buttonList.add(button);

            return new GuiComponentWrapper(button, ComponentType.BOOLEAN_BUTTON);
        }

        // -------------------------
        // 数字/字符串 → 文本输入框
        // -------------------------
        GuiTextField textField =
                new GuiTextField(this.fontRendererObj, componentX, yPos, componentWidth, 20);

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

    @Override
    protected void actionPerformed(GuiButton button) {
        int id = button.id;

        // Save
        if (id == 0) {
            saveChanges();
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }

        // Cancel
        if (id == 1) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }

        // Reset
        if (id == 2) {
            modifiedRules.clear();
            createRuleComponents();
            return;
        }

        // Boolean buttons
        if (id >= 100) {
            int index = id - 100;

            // 按 defaultRules 顺序取 key
            int i = 0;
            String ruleName = null;
            for (String s : defaultRules.keySet()) {
                if (i == index) {
                    ruleName = s;
                    break;
                }
                i++;
            }

            if (ruleName == null) return;

            GuiComponentWrapper wrapper = ruleComponents.get(ruleName);

            if (wrapper != null && wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                GuiButton boolBtn = (GuiButton) wrapper.component;

                boolean newVal = boolBtn.displayString.equals(I18n.format("options.off"));
                boolBtn.displayString = newVal ? I18n.format("options.on") : I18n.format("options.off");

                // 更新 modifiedRules string 形式
                modifiedRules.put(ruleName, Boolean.toString(newVal));
            }
        }
    }

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

    private void toggleBooleanRule(String ruleName, GuiButton button) {
        // 获取当前值（modified > editable > default）
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
        modifiedRules.put(ruleName, String.valueOf(next));

        // 更新按钮文本
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
                    String input = textField.getText();

                    // parsed 仅用于内部展示类型推断，不影响最终保存
                    Object parsed = parseFromString(input, defaultRules.get(ruleName).getOptimalValue());

                    // 真正存储 String → String
                    modifiedRules.put(ruleName, String.valueOf(parsed));
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 文本框交互
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // 检查是否点击滚动条区域（用于拖动）
        int scrollBarX = this.width / 2 - 10;
        int scrollBarY = 60;
        int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

        if (mouseX >= scrollBarX && mouseX <= scrollBarX + 10 &&
                mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
            this.isScrolling = true;
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state == 0 || state == 1) {
            this.isScrolling = false;
        }
    }

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
        // 背景 + 面板
        drawBackground(0);
        drawContentPanel();

        // 标题
        this.drawCenteredString(this.fontRendererObj, I18n.format("createWorld.customize.custom.gamerules"), this.width / 2, 20, 0xFFFFFF);

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
        LOGGER.error("GameRuleEditor: buttonList=" + this.buttonList.size());
    }

    private void drawContentPanel() {
        int panelWidth = this.width - 100;
        int panelHeight = this.height - 100;
        int panelX = 50;
        int panelY = 40;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA222222);
        drawRect(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY, 0xFF555555);
        drawRect(panelX - 1, panelY + panelHeight, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        drawRect(panelX - 1, panelY, panelX, panelY + panelHeight, 0xFF555555);
        drawRect(panelX + panelWidth, panelY, panelX + panelWidth + 1, panelY + panelHeight, 0xFF555555);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 20, 0xAA444444);
        drawRect(panelX, panelY + 20, panelX + panelWidth, panelY + 21, 0xFF666666);
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

                this.drawString(this.fontRendererObj, ruleName, this.width / 2 - 150, rowY + 6, 0xFFFFFF);

                String defaultValueText = I18n.format("createWorld.customize.custom.default") + ": " + originalValue.getOptimalValue();
                this.drawString(this.fontRendererObj, defaultValueText, this.width / 2 - 150, rowY + 16, 0x888888);
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
                    String tooltip = getRuleTooltip(ruleName);
                    if (tooltip != null) {
                        List<String> tooltipList = Arrays.asList(tooltip);
                        this.func_146283_a(tooltipList, mouseX, mouseY);
                    }
                }
            }
            index++;
        }
    }

    private boolean isMouseOverRuleName(int mouseX, int mouseY, int rowY) {
        return mouseX >= this.width / 2 - 150 && mouseX <= this.width / 2 - 20 &&
                mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
    }

    private String getRuleTooltip(String ruleName) {
        String translationKey = "gamerule." + ruleName + ".description";
        String translated = I18n.format(translationKey);

        if (translated.equals(translationKey)) {
            Map<String, String> defaultDescriptions = new HashMap<String, String>();
            defaultDescriptions.put("doFireTick", "Controls whether fire spreads and naturally extinguishes");
            defaultDescriptions.put("mobGriefing", "Controls whether mobs can destroy blocks");
            defaultDescriptions.put("keepInventory", "Keep inventory after death");
            defaultDescriptions.put("doMobSpawning", "Natural mob spawning");
            defaultDescriptions.put("doMobLoot", "Mobs drop loot");
            defaultDescriptions.put("doTileDrops", "Blocks drop items when destroyed");
            defaultDescriptions.put("doEntityDrops", "Entities drop items");
            defaultDescriptions.put("commandBlockOutput", "Command blocks output to chat");
            defaultDescriptions.put("naturalRegeneration", "Natural health regeneration");
            defaultDescriptions.put("doDaylightCycle", "Day/night cycle");
            defaultDescriptions.put("logAdminCommands", "Log admin commands to server log");
            defaultDescriptions.put("showDeathMessages", "Show death messages in chat");
            defaultDescriptions.put("randomTickSpeed", "Random tick speed (plant growth, etc.)");
            defaultDescriptions.put("sendCommandFeedback", "Show command execution feedback");
            defaultDescriptions.put("reducedDebugInfo", "Reduce debug screen information");

            return defaultDescriptions.get(ruleName);
        }

        return translated;
    }

    /**
     * 根据 originalValue 的类型尝试把字符串解析为更合适的类型（用于显示/校验）
     */
    private Object parseFromString(String text, Object originalValue) {
        if (originalValue instanceof Boolean) {
            return Boolean.parseBoolean(text);
        }
        if (originalValue instanceof Integer) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        }
        if (originalValue instanceof Double) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return text;
    }

    /**
     * 把用户修改保存到 GameRuleApplier.pendingGameRules（String->String）
     */
    private void saveChanges() {
        LOGGER.error("GameRuleEditor: saveChanges() called");

        // 仅写入用户修改过的规则（String → String）
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> e : modifiedRules.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                result.put(e.getKey(), e.getValue());
            }
        }

        // 写入到 Applier（静态变量 pendingGameRules）
        try {
            GameRuleApplier.setPendingGameRules(result);
            LOGGER.info("Saved {} modified game rules to pendingGameRules.", result.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to set pending game rules: {}", ex.getMessage());
        }
    }

    private void resetToDefaults() {
        modifiedRules.clear();
        createRuleComponents();
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

    private enum ComponentType {
        BOOLEAN_BUTTON,
        TEXT_FIELD
    }
}
