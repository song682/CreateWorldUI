package decok.dfcdvadstf.createworldui.tab;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.catframe.ui.GuiCyclableButton;
import decok.dfcdvadstf.catframe.ui.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;

import java.util.ArrayList;
import java.util.List;

public class WorldTab extends AbstractScreenTab {
    private GuiTextField seedField;
    private GuiCyclableButton<WorldType> worldTypeButton;
    private GuiButton generateStructuresButton;
    private GuiButton bonusChestButton;
    private GuiButton customizeButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public WorldTab() {
        super(101, "createworldui.tab.world");
        
        // Set tab texture in constructor to fix the texture path issue
        setTabTexture(new ResourceLocation("catframe", "textures/gui/tab/tabs.png"));
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {

        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;
        
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
        seedField.setText(access.modernWorldCreatingUI$getSeed());

        // Create world type button
        // 创建世界类型按钮
        List<WorldType> validWorldTypes = new ArrayList<>();
        for (WorldType wt : WorldType.worldTypes) {
            if (wt != null) validWorldTypes.add(wt);
        }

        // Read the world type index from vanilla field (set by DefaultWorldGenerator or vanilla)
        // 从原版字段读取世界类型索引（由 DefaultWorldGenerator 或原版设置）
        int currentIdx = access.modernWorldCreatingUI$getWorldTypeIndex();
        
        // Debug: log the current index
        // 调试：记录当前索引
        System.out.println("WorldTab: Current world type index from vanilla field: " + currentIdx);
        if (currentIdx >= 0 && currentIdx < WorldType.worldTypes.length && WorldType.worldTypes[currentIdx] != null) {
            System.out.println("WorldTab: World type name: " + WorldType.worldTypes[currentIdx].getWorldTypeName());
        }
        
        // Validate index and fallback to default if invalid
        // 验证索引，如果无效则回退到默认值
        WorldType currentType;
        if (currentIdx >= 0 && currentIdx < WorldType.worldTypes.length && WorldType.worldTypes[currentIdx] != null) {
            currentType = WorldType.worldTypes[currentIdx];
        } else {
            currentType = validWorldTypes.get(0);
            System.out.println("WorldTab: Invalid world type index, falling back to: " + currentType.getWorldTypeName());
        }

        worldTypeButton = GuiCyclableButton.<WorldType>builder(
                        wt -> I18n.format("selectWorld.mapType") + " " + I18n.format(wt.getTranslateName()))
                .values(validWorldTypes)
                .initially(currentType)
                .build(5, width / 2 - 154, height / 8 + 10, 150, 20, (button, worldType) -> {
                    // Find the original array index for the selected world type
                    for (int i = 0; i < WorldType.worldTypes.length; i++) {
                        if (WorldType.worldTypes[i] == worldType) {
                            access.modernWorldCreatingUI$setWorldTypeIndex(i);
                            break;
                        }
                    }
                });
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
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 3 - 2 - 13, 0xA0A0A0);

        // Draw text field (including placeholder)
        // 绘制输入框（包括占位符）
        seedField.drawTextBox();

        // Draw button labels
        // 绘制按钮标签
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.mapFeatures"),
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 2 + 15 + 6, 0xFFFFFF);
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.bonusItems"),
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 2 - 15 + 6, 0xFFFFFF);

        // Update button text and state
        // 更新按钮文本和状态
        if (worldTypeButton != null) worldTypeButton.updateText();

        if (WorldType.worldTypes != null && access.modernWorldCreatingUI$getWorldTypeIndex() < WorldType.worldTypes.length &&
                WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()] != null) {
            customizeButton.enabled = WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()].isCustomizable();
        } else {
            customizeButton.enabled = false;
        }

        generateStructuresButton.displayString = getGenerateStructuresText();
        bonusChestButton.displayString = getBonusChestText();

        // Update bonus chest button state based on hardcore mode
        // 根据硬核模式更新奖励筱按钮状态
        bonusChestButton.enabled = !access.modernWorldCreatingUI$getHardcore();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        System.out.println("WorldTab: Button clicked: " + button.id);

        switch (button.id) {
            case 4: // generate structures / 生成建筑
                access.modernWorldCreatingUI$setGenerateStructures(!access.modernWorldCreatingUI$getGenerateStructures());
                break;
            case 7: // bonus chest / 奖励筱
                if (!access.modernWorldCreatingUI$getHardcore()) {
                    access.modernWorldCreatingUI$setBonusChest(!access.modernWorldCreatingUI$getBonusChest());
                }
                break;
            case 8: // customize / 自定义
                // Open the customize screen
                // 打开自定义界面
                System.out.println("WorldTab: Customize button clicked");
                if (WorldType.worldTypes != null && access.modernWorldCreatingUI$getWorldTypeIndex() < WorldType.worldTypes.length &&
                        WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()] != null) {
                    WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()].onCustomizeButton(mc, guiCreateWorld);
                }
                break;
        }
    }


    private String getGenerateStructuresText() {
        return access.modernWorldCreatingUI$getGenerateStructures() ? I18n.format("options.on") : I18n.format("options.off");
    }

    private String getBonusChestText() {
        boolean bonusChest = access.modernWorldCreatingUI$getBonusChest();
        boolean hardcore = access.modernWorldCreatingUI$getHardcore();
        boolean isOn = bonusChest && !hardcore;
        return isOn ? I18n.format("options.on") : I18n.format("options.off");
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        seedField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        seedField.textboxKeyTyped(typedChar, keyCode);
        access.modernWorldCreatingUI$setSeed(seedField.getText());
    }
}