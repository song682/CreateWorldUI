package decok.dfcdvadstf.createworldui.ui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.StringWidget;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.components.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.layouts.LayoutSettings;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.mixin.middle.access.IGuiCreateWorldAccess;
import net.minecraft.client.Minecraft;
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
    private CyclingButton<WorldType> worldTypeButton;
    private Button customizeButton;
    private Button generateStructuresButton;
    private Button bonusChestButton;
    private final GuiCreateWorld guiCreateWorld;
    private final IGuiCreateWorldAccess access;

    /**
     * Builds the tab once; the widgets live as long as the screen instance.
     * <p>一次性构建标签页；组件随屏幕实例存活。</p>
     */
    public WorldTab(GuiCreateWorld screen) {
        super(Text.translatable("createworldui.tab.world"));
        this.guiCreateWorld = screen;
        this.access = (IGuiCreateWorldAccess) screen;

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
        // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 统一延迟绘制
        // Component-level tooltip: auto-pumped by WidgetTooltipHolder via extractRenderState, drawn deferred by CatFrame at end of frame
        worldTypeButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.worldType")));
        layout.addChild(worldTypeButton, row, 0);

        // Create customize button
        customizeButton = Button.builder(
                Text.translatable("selectWorld.customizeType"),
                btn -> {
                    if (WorldType.worldTypes != null && access.modernWorldCreatingUI$getWorldTypeIndex() < WorldType.worldTypes.length &&
                            WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()] != null) {
                        WorldType.worldTypes[access.modernWorldCreatingUI$getWorldTypeIndex()].onCustomizeButton(Minecraft.getMinecraft(), guiCreateWorld);
                    }
                })
                .useVanillaTexture(true)
            .width(150).height(20).build();
        customizeButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.customize")));
        layout.addChild(customizeButton, row++, 1);

        // Row 1: Seed label (spans 2 cols) + spacing
        // 第1行：种子标签（跨2列）
        layout.addChild(
            new StringWidget(I18n.format("selectWorld.enterSeed"), 0xA0A0A0),
            row, 0, 1, 2);
        row++;

        // Row 2: Seed field (spans 2 columns)
        // 第2行：种子输入框（跨2列）
        // Seed field; edits are pushed back into the host screen immediately through
        // the component's onTextChanged hook.
        // 26.1.2 WorldTab: 种子框 308 宽（310 跨列 labeled widget 内缩 2px）
        // 26.1.2 WorldTab: the seed field is 308 wide (2px inset inside the 310 spanning
        // labeled widget)
        // 种子输入框；编辑通过组件的 onTextChanged 钩子即时写回宿主界面。
        SimpleEditBox seedField = new SimpleEditBox(0, 0, 308, 20) {
            @Override
            protected void onTextChanged() {
                access.modernWorldCreatingUI$setSeed(getText());
            }
        };
        seedField.setText(access.modernWorldCreatingUI$getSeed());
        // 308 宽种子框在 310 跨列单元格中水平居中（对齐 26.1.2 labeled widget 的居中）
        // The 308-wide seed field horizontally centered in the 310 spanning cell
        // (aligned with the 26.1.2 labeled widget's centering)
        layout.addChild(seedField, row++, 0, 1, 2, LayoutSettings::alignHorizontallyCenter);

        // Layout settings for toggle buttons and labels
        // 开关按钮和标签的对齐设置
        LayoutSettings leftAlign = layout.newCellSettings().align(0.0F, 0.5F);  // default: left-top
        LayoutSettings rightAlign = layout.newCellSettings().align(1.0F, 0.5F);

        // Row 3: Generate Structures label + toggle button
        // 第3行：生成结构标签 + 开关按钮
        layout.addChild(
            new StringWidget(Text.translatable("createworldui.select_world.gen_structure"), 0xFFFFFF),
            row, 0, leftAlign);
        generateStructuresButton = Button.builder(
                Text.literal(getGenerateStructuresText()),
                btn -> access.modernWorldCreatingUI$setGenerateStructures(
                    !access.modernWorldCreatingUI$getGenerateStructures()))
            .width(44).height(20).useVanillaTexture(true).build();
        generateStructuresButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.generateStructures")));
        layout.addChild(generateStructuresButton, row++, 1, rightAlign);

        // Row 4: Bonus Chest label + toggle button
        // 第4行：奖励箱标签 + 开关按钮
        layout.addChild(
            new StringWidget(Text.translatable("createworldui.select_world.bonus_chest"), 0xFFFFFF),
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
        bonusChestButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.bonusChest")));
        layout.addChild(bonusChestButton, row++, 1, rightAlign);
    }

    /**
     * Refreshes button states and toggle labels before the host renders this tab's
     * children.
     * <p>在本标签页子组件渲染前刷新按钮状态与开关文案。</p>
     */
    public void prepareFrame() {
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
