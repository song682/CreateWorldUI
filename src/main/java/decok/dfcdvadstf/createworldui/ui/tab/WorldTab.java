package decok.dfcdvadstf.createworldui.ui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.layouts.LayoutSettings;
import decok.dfcdvadstf.catframe.ui.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;

import java.util.ArrayList;
import java.util.List;

/**
 * World Options Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的世界选项标签页。</p>
 */
public class WorldTab extends GridLayoutTab {
    private SimpleEditBox seedField;
    private CyclingButton<WorldType> worldTypeButton;
    private Button customizeButton;
    private Button generateStructuresButton;
    private Button bonusChestButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public WorldTab() {
        super(101, Text.translatable("createworldui.tab.world"));
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;

        // Match high-version WorldTab: 2 columns with column and row spacing
        layout.columnSpacing(10).rowSpacing(8);

        int row = 0;

        // Row 0: World Type button (left) + Customize button (right)
        // 第0行：世界类型按钮（左）+ 自定义按钮（右）
        // Create world type button
        List<WorldType> validWorldTypes = new ArrayList<>();
        boolean filterEnabled = CreateWorldUI.config != null && CreateWorldUI.config.filterNonCreatableWorldTypes;
        for (WorldType wt : WorldType.worldTypes) {
            if (wt != null && (!filterEnabled || wt.getCanBeCreated())) {
                validWorldTypes.add(wt);
            }
        }

        int currentIdx = access.modernWorldCreatingUI$getWorldTypeIndex();
        WorldType currentType;
        if (currentIdx >= 0 && currentIdx < WorldType.worldTypes.length && WorldType.worldTypes[currentIdx] != null) {
            currentType = WorldType.worldTypes[currentIdx];
            // If filtering is enabled and current type is not creatable, reset to first valid type
            if (filterEnabled && !currentType.getCanBeCreated()) {
                currentType = validWorldTypes.get(0);
                for (int i = 0; i < WorldType.worldTypes.length; i++) {
                    if (WorldType.worldTypes[i] == currentType) {
                        access.modernWorldCreatingUI$setWorldTypeIndex(i);
                        break;
                    }
                }
            }
        } else {
            currentType = validWorldTypes.get(0);
        }

        worldTypeButton = CyclingButton.<WorldType>builder(
                        wt -> I18n.format("selectWorld.mapType") + " " + I18n.format(wt.getTranslateName()))
                .values(validWorldTypes)
                .initially(currentType)
                .useVanillaTexture(true)
                .build(0, 0, 150, 20, (button, worldType) -> {
                    for (int i = 0; i < WorldType.worldTypes.length; i++) {
                        if (WorldType.worldTypes[i] == worldType) {
                            access.modernWorldCreatingUI$setWorldTypeIndex(i);
                            break;
                        }
                    }
                });
        layout.addChild(worldTypeButton, row, 0);

        // Create customize button
        customizeButton = Button.builder(
                Text.literal(I18n.format("selectWorld.customizeType")),
                btn -> {
                    if (WorldType.worldTypes != null && access.modernWorldCreatingUI$getWorldTypeIndex() < WorldType.worldTypes.length &&
                            WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()] != null) {
                        WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()].onCustomizeButton(mc, guiCreateWorld);
                    }
                })
                .useVanillaTexture(true)
            .width(150).height(20).build();
        layout.addChild(customizeButton, row++, 1);

        // Row 1: Seed label (spans 2 cols) + spacing
        // 第1行：种子标签（跨2列）
        layout.addChild(
            new StringWidget(I18n.format("selectWorld.enterSeed"), 0xA0A0A0),
            row, 0, 1, 2);
        row++;

        // Row 2: Seed field (spans 2 columns)
        // 第2行：种子输入框（跨2列）
        seedField = new SimpleEditBox(0, 0, 310, 20);
        seedField.setText(access.modernWorldCreatingUI$getSeed());
        layout.addChild(seedField, row++, 0, 1, 2);

        // Layout settings for toggle buttons and labels
        // 开关按钮和标签的对齐设置
        LayoutSettings leftAlign = layout.newCellSettings().align(0.0F, 0.5F);  // default: left-top
        LayoutSettings rightAlign = layout.newCellSettings().align(1.0F, 0.5F);

        // Row 3: Generate Structures label + toggle button
        // 第3行：生成结构标签 + 开关按钮
        layout.addChild(
            new StringWidget(Text.translatable("createworldui","createworldui.select_world.gen_structure"), 0xFFFFFF),
            row, 0, leftAlign);
        generateStructuresButton = Button.builder(
                Text.literal(getGenerateStructuresText()),
                btn -> access.modernWorldCreatingUI$setGenerateStructures(
                    !access.modernWorldCreatingUI$getGenerateStructures()))
            .width(44).height(20).useVanillaTexture(true).build();
        layout.addChild(generateStructuresButton, row++, 1, rightAlign);

        // Row 4: Bonus Chest label + toggle button
        // 第4行：奖励箱标签 + 开关按钮
        layout.addChild(
            new StringWidget(Text.translatable("createworldui","createworldui.select_world.bonus_chest"), 0xFFFFFF),
            row, 0, leftAlign);
        bonusChestButton = Button.builder(
                Text.literal(getBonusChestText()),
                btn -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setBonusChest(
                            !access.modernWorldCreatingUI$getBonusChest());
                    }
                })
            .width(44).height(20).useVanillaTexture(true).build();
        layout.addChild(bonusChestButton, row++, 1, rightAlign);

        super.initGui(tabManager, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // Update button states before rendering
        if (WorldType.worldTypes != null && access.modernWorldCreatingUI$getWorldTypeIndex() < WorldType.worldTypes.length &&
                WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()] != null) {
            customizeButton.setActive(WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()].isCustomizable());
        } else {
            customizeButton.setActive(false);
        }

        generateStructuresButton.setMessage(Text.literal(getGenerateStructuresText()));
        bonusChestButton.setMessage(Text.literal(getBonusChestText()));
        bonusChestButton.setActive(!access.modernWorldCreatingUI$getHardcore());

        // Render all components
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Update toggle button text each frame (labels are now layout elements)
        generateStructuresButton.setMessage(Text.literal(getGenerateStructuresText()));
        bonusChestButton.setMessage(Text.literal(getBonusChestText()));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // All actions handled via OnPress callbacks
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Delegate to GridLayoutTab which forwards to all Components
        // 委托给 GridLayoutTab，它会转发到所有 Component
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Forward key to all Components via GridLayoutTab, then sync seed
        // 转发按键到所有 Component，然后同步种子
        super.keyTyped(typedChar, keyCode);
        access.modernWorldCreatingUI$setSeed(seedField.getText());
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
}
