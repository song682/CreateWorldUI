package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.createworldui.api.GuiCyclableButton;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;

public class WorldTab extends AbstractScreenTab {
    private GuiTextField seedField;
    private GuiCyclableButton worldTypeButton;
    private GuiButton generateStructuresButton;
    private GuiButton bonusChestButton;
    private GuiButton customizeButton;

    public WorldTab() {
        super(101, "createworldui.tab.world");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);

        // Create seed text field
        // 创建种子输入框
        seedField = new GuiTextField(mc.fontRenderer,
                width / 2 - 154, height / 3 - 1, 308, 20) {
            @Override
            public void drawTextBox() {
                super.drawTextBox();
                // Draw placeholder text
                // 绘制占位符
                if (this.getText().isEmpty() && !this.isFocused()) {
                    String placeholder = I18n.format("selectWorld.seedInfo");
                    int textColor = 0x808080; // gray / 灰色
                    int x = this.xPosition + 4;
                    int y = this.yPosition + (this.height - 8) / 2;
                    mc.fontRenderer.drawStringWithShadow(placeholder, x, y, textColor);
                }
            }
        };
        seedField.setText(getSeed());

        // Create world type button
        // 创建世界类型按钮
        worldTypeButton = new GuiCyclableButton(5, width / 2 - 154, height / 8 + 10,
                150, 20, this::getWorldTypeText, direction -> cycleWorldType());
        addButton(worldTypeButton);

        // Create customize button
        // 创建自定义按钮
        customizeButton = new GuiButton(8, width / 2 + 4, height / 8 + 10,
                150, 20, I18n.format("selectWorld.customizeType"));
        addButton(customizeButton);

        // Create generate structures button
        // 创建生成建筑按钮
        generateStructuresButton = new GuiButton(4, width / 2 + 110, height / 2 + 15,
                44, 20, getGenerateStructuresText());
        addButton(generateStructuresButton);

        // Create bonus chest button
        // 创建奖励筱按钮
        bonusChestButton = new GuiButton(7, width / 2 + 110, height / 2 - 15,
                44, 20, getBonusChestText());
        addButton(bonusChestButton);

        // Initially hide all buttons
        // 初始隐藏所有按钮
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // Draw seed label
        // 绘制种子标签
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterSeed"),
                tabManager.getParent().width / 2 - 154,
                tabManager.getParent().height / 3 - 2 - 13, 0xA0A0A0);

        // Draw text field (including placeholder)
        // 绘制输入框（包括占位符）
        seedField.drawTextBox();

        // Draw button labels
        // 绘制按钮标签
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.mapFeatures"),
                tabManager.getParent().width / 2 - 154,
                tabManager.getParent().height / 2 + 15 + 6, 0xFFFFFF);
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.bonusItems"),
                tabManager.getParent().width / 2 - 154,
                tabManager.getParent().height / 2 - 15 + 6, 0xFFFFFF);

        // Update button text and state
        // 更新按钮文本和状态
        if (worldTypeButton != null) worldTypeButton.updateText();

        if (WorldType.worldTypes != null && getWorldTypeIndex() < WorldType.worldTypes.length &&
                WorldType.worldTypes[getWorldTypeIndex()] != null) {
            customizeButton.enabled = WorldType.worldTypes[getWorldTypeIndex()].isCustomizable();
        } else {
            customizeButton.enabled = false;
        }

        generateStructuresButton.displayString = getGenerateStructuresText();
        bonusChestButton.displayString = getBonusChestText();

        // Update bonus chest button state based on hardcore mode
        // 根据硬核模式更新奖励筱按钮状态
        bonusChestButton.enabled = !getHardcore();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        System.out.println("WorldTab: Button clicked: " + button.id);

        switch (button.id) {
            case 4: // generate structures / 生成建筑
                tabManager.setGenerateStructures(!getGenerateStructures());
                break;
            case 7: // bonus chest / 奖励筱
                if (!getHardcore()) {
                    tabManager.setBonusChest(!getBonusChest());
                }
                break;
            case 8: // customize / 自定义
                // Open the customize screen
                // 打开自定义界面
                System.out.println("WorldTab: Customize button clicked");
                break;
        }
    }

    private String getWorldTypeText() {
        int index = getWorldTypeIndex();
        if (WorldType.worldTypes == null || index >= WorldType.worldTypes.length ||
                WorldType.worldTypes[index] == null) {
            return I18n.format("selectWorld.mapType") + " " + I18n.format("selectWorld.mapType.normal");
        }
        return I18n.format("selectWorld.mapType") + " " +
                I18n.format(WorldType.worldTypes[index].getTranslateName());
    }

    private String getGenerateStructuresText() {
        return getGenerateStructures() ? I18n.format("options.on") : I18n.format("options.off");
    }

    private String getBonusChestText() {
        boolean bonusChest = getBonusChest();
        boolean hardcore = getHardcore();
        boolean isOn = bonusChest && !hardcore;
        return isOn ? I18n.format("options.on") : I18n.format("options.off");
    }

    private void cycleWorldType() {
        if (WorldType.worldTypes == null) return;

        int currentIndex = getWorldTypeIndex();
        int newIndex = currentIndex;

        do {
            newIndex = (newIndex + 1) % WorldType.worldTypes.length;
        } while (WorldType.worldTypes[newIndex] == null && newIndex != currentIndex);

        if (WorldType.worldTypes[newIndex] != null) {
            tabManager.setWorldTypeIndex(newIndex);
            if (worldTypeButton != null) {
                worldTypeButton.updateText();
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        seedField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        seedField.textboxKeyTyped(typedChar, keyCode);
        tabManager.setSeed(seedField.getText());
    }
}