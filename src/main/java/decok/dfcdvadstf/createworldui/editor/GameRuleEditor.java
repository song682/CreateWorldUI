package decokalt.dfcdvadstf.createworldui.editor;

import decokalt.dfcdvadstf.createworldui.util.GameRuleMonitorNSetter;
import decokalt.dfcdvadstf.createworldui.util.GameRuleMonitorNSetter.GameruleValue;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRuleEditor extends GuiScreen {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleEditor");

    private World world;
    private Map<String, GameruleValue> originalRules;
    private final Map<String, Object> modifiedRules = new HashMap<String, Object>();
    private final Map<String, GuiComponentWrapper> ruleComponents = new HashMap<String, GuiComponentWrapper>();

    private GuiButton saveButton;
    private GuiButton cancelButton;
    private GuiButton resetButton;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private static final int ROW_HEIGHT = 25;
    private static final int VISIBLE_ROWS = 8;
    private boolean isScrolling = false;

    public GameRuleEditor(World world) {
        this.world = world;
        this.originalRules = GameRuleMonitorNSetter.getAllGamerules(world);
        this.maxScrollOffset = Math.max(0, originalRules.size() - VISIBLE_ROWS);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        // 创建按钮
        this.saveButton = new GuiButton(0, this.width / 2 - 154, this.height - 30, 100, 20, I18n.format("gui.save"));
        this.cancelButton = new GuiButton(1, this.width / 2 - 50, this.height - 30, 100, 20, I18n.format("gui.cancel"));
        this.resetButton = new GuiButton(2, this.width / 2 + 54, this.height - 30, 100, 20, I18n.format("createWorld.customize.custom.reset"));

        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.cancelButton);
        this.buttonList.add(this.resetButton);

        // 创建游戏规则组件
        createRuleComponents();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void createRuleComponents() {
        ruleComponents.clear();
        int yPos = 40;
        int index = 0;

        for (Map.Entry<String, GameruleValue> entry : originalRules.entrySet()) {
            if (index >= scrollOffset && index < scrollOffset + VISIBLE_ROWS) {
                String ruleName = entry.getKey();
                GameruleValue value = entry.getValue();
                Object optimalValue = value.getOptimalValue();

                int componentY = yPos + (index - scrollOffset) * ROW_HEIGHT;
                GuiComponentWrapper component = createComponentForRule(ruleName, optimalValue, componentY);
                if (component != null) {
                    ruleComponents.put(ruleName, component);

                    // 为按钮添加ID
                    if (component.type == ComponentType.BOOLEAN_BUTTON) {
                        ((GuiButton) component.component).id = 100 + index;
                    }
                }
            }
            index++;
        }
    }

    private GuiComponentWrapper createComponentForRule(String ruleName, Object value, int yPos) {
        int componentWidth = 150;
        int componentX = this.width / 2 + 20;

        if (value instanceof Boolean) {
            // 布尔值使用按钮
            boolean boolValue = (Boolean) value;
            String buttonText = boolValue ?
                    I18n.format("options.on") :
                    I18n.format("options.off");
            GuiButton button = new GuiButton(0, componentX, yPos, componentWidth, 20, buttonText);
            return new GuiComponentWrapper(button, ComponentType.BOOLEAN_BUTTON);
        } else {
            // 数字和字符串使用文本框
            GuiTextField textField = new GuiTextField(this.fontRendererObj, componentX, yPos, componentWidth, 20);
            textField.setText(String.valueOf(value));
            textField.setMaxStringLength(50);
            return new GuiComponentWrapper(textField, ComponentType.TEXT_FIELD);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.saveButton) {
            saveChanges();
            this.mc.displayGuiScreen(null);
        } else if (button == this.cancelButton) {
            this.mc.displayGuiScreen(null);
        } else if (button == this.resetButton) {
            resetToDefaults();
        } else if (button.id >= 100) {
            // 处理规则按钮点击（布尔值切换）
            int ruleIndex = button.id - 100;
            String ruleName = getRuleNameByIndex(ruleIndex);
            if (ruleName != null) {
                toggleBooleanRule(ruleName, button);
            }
        }
    }

    private String getRuleNameByIndex(int index) {
        if (index < 0 || index >= originalRules.size()) {
            return null;
        }

        int i = 0;
        for (String ruleName : originalRules.keySet()) {
            if (i == index) {
                return ruleName;
            }
            i++;
        }
        return null;
    }

    private void toggleBooleanRule(String ruleName, GuiButton button) {
        Object currentValue = modifiedRules.get(ruleName);
        if (currentValue == null) {
            currentValue = originalRules.get(ruleName).getOptimalValue();
        }

        boolean newValue = !(Boolean) currentValue;
        modifiedRules.put(ruleName, newValue);
        button.displayString = newValue ?
                I18n.format("options.on") :
                I18n.format("options.off");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        // 处理文本框输入
        for (Map.Entry<String, GuiComponentWrapper> entry : ruleComponents.entrySet()) {
            GuiComponentWrapper wrapper = entry.getValue();
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.textboxKeyTyped(typedChar, keyCode);

                if (keyCode == Keyboard.KEY_RETURN) {
                    // 回车键保存文本框内容
                    String newValue = textField.getText();
                    String ruleName = entry.getKey();
                    Object parsedValue = parseValue(newValue, originalRules.get(ruleName).getOptimalValue());
                    modifiedRules.put(ruleName, parsedValue);
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 处理文本框点击
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // 检查是否点击了滚动条区域
        int scrollBarX = this.width / 2 - 10;
        int scrollBarY = 40;
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
            // 处理滚动条拖动
            int scrollBarY = 40;
            int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

            float relativePosition = (float)(mouseY - scrollBarY) / scrollBarHeight;
            this.scrollOffset = (int)(relativePosition * this.maxScrollOffset);
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset));

            createRuleComponents();
        } else if (Mouse.getEventDWheel() != 0) {
            // 处理鼠标滚轮
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
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("createWorld.customize.custom.gamerules"), this.width / 2, 10, 0xFFFFFF);

        // 绘制规则列表
        drawRuleList(mouseX, mouseY);

        // 绘制组件
        for (GuiComponentWrapper wrapper : ruleComponents.values()) {
            if (wrapper.type == ComponentType.TEXT_FIELD) {
                GuiTextField textField = (GuiTextField) wrapper.component;
                textField.drawTextBox();
            } else if (wrapper.type == ComponentType.BOOLEAN_BUTTON) {
                GuiButton button = (GuiButton) wrapper.component;
                button.drawButton(this.mc, mouseX, mouseY);
            }
        }

        // 绘制滚动条
        drawScrollBar();

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 绘制悬停提示
        drawTooltips(mouseX, mouseY);
    }

    private void drawRuleList(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 40;

        for (Map.Entry<String, GameruleValue> entry : originalRules.entrySet()) {
            if (index >= scrollOffset && index < scrollOffset + VISIBLE_ROWS) {
                String ruleName = entry.getKey();
                GameruleValue originalValue = entry.getValue();
                Object currentValue = modifiedRules.get(ruleName);
                if (currentValue == null) {
                    currentValue = originalValue.getOptimalValue();
                }

                int rowY = yPos + (index - scrollOffset) * ROW_HEIGHT;

                // 绘制规则名称
                this.drawString(this.fontRendererObj, ruleName, this.width / 2 - 150, rowY + 6, 0xFFFFFF);

                // 绘制默认值提示
                String defaultValueText = I18n.format("createWorld.customize.custom.default") + ": " + originalValue.getOptimalValue();
                this.drawString(this.fontRendererObj, defaultValueText, this.width / 2 - 150, rowY + 16, 0x888888);
            }
            index++;
        }
    }

    private void drawScrollBar() {
        if (maxScrollOffset > 0) {
            int scrollBarX = this.width / 2 - 10;
            int scrollBarY = 40;
            int scrollBarHeight = VISIBLE_ROWS * ROW_HEIGHT;

            // 绘制滚动条背景
            drawRect(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0xFF000000);
            drawRect(scrollBarX + 1, scrollBarY + 1, scrollBarX + 9, scrollBarY + scrollBarHeight - 1, 0xFF666666);

            // 绘制滚动条滑块
            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int sliderHeight = Math.max(20, scrollBarHeight / (maxScrollOffset + VISIBLE_ROWS) * VISIBLE_ROWS);
            int sliderY = scrollBarY + (int) (scrollPercentage * (scrollBarHeight - sliderHeight));

            drawRect(scrollBarX + 2, sliderY, scrollBarX + 8, sliderY + sliderHeight, 0xFFCCCCCC);
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        int index = 0;
        int yPos = 40;

        for (String ruleName : originalRules.keySet()) {
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
        // 使用本地化键名获取规则描述
        String translationKey = "gamerule." + ruleName + ".description";
        String translated = I18n.format(translationKey);

        // 如果没有找到翻译，返回默认描述
        if (translated.equals(translationKey)) {
            // 默认描述映射
            Map<String, String> defaultDescriptions = new HashMap<String, String>();
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

    private Object parseValue(String textValue, Object originalValue) {
        // 根据原始值的类型来解析新值
        if (originalValue instanceof Boolean) {
            return Boolean.parseBoolean(textValue);
        } else if (originalValue instanceof Integer) {
            try {
                return Integer.parseInt(textValue);
            } catch (NumberFormatException e) {
                return originalValue; // 解析失败，返回原值
            }
        } else if (originalValue instanceof Double) {
            try {
                return Double.parseDouble(textValue);
            } catch (NumberFormatException e) {
                return originalValue; // 解析失败，返回原值
            }
        } else {
            return textValue; // 字符串类型直接返回
        }
    }

    private void saveChanges() {
        for (Map.Entry<String, Object> entry : modifiedRules.entrySet()) {
            String ruleName = entry.getKey();
            Object value = entry.getValue();

            boolean success = GameRuleMonitorNSetter.setGamerule(world, ruleName, value);
            if (success) {
                LOGGER.info("Successfully set game rule {} to {}", ruleName, value);
            } else {
                LOGGER.warn("Failed to set game rule {}", ruleName);
            }
        }
    }

    private void resetToDefaults() {
        modifiedRules.clear();
        createRuleComponents(); // 重新创建组件以恢复默认值
    }

    // 组件包装类
    private static class GuiComponentWrapper {
        public final Object component;
        public final ComponentType type;

        public GuiComponentWrapper(Object component, ComponentType type) {
            this.component = component;
            this.type = type;
        }
    }

    // 组件类型枚举
    private enum ComponentType {
        BOOLEAN_BUTTON,
        TEXT_FIELD
    }
}