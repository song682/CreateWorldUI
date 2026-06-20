package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.EditBox;
import decok.dfcdvadstf.catframe.ui.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
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
    private EditBox seedField;
    private CyclingButton<WorldType> worldTypeButton;
    private Button customizeButton;
    private Button generateStructuresButton;
    private Button bonusChestButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public WorldTab() {
        super(101, "createworldui.tab.world");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;

        int row = 0;

        // Create seed text field
        seedField = new EditBox(0, 0, 308, 20);
        seedField.setText(access.modernWorldCreatingUI$getSeed());
        layout.addChild(seedField, row++, 0);

        // Create world type button
        List<WorldType> validWorldTypes = new ArrayList<>();
        for (WorldType wt : WorldType.worldTypes) {
            if (wt != null) validWorldTypes.add(wt);
        }

        int currentIdx = access.modernWorldCreatingUI$getWorldTypeIndex();
        WorldType currentType;
        if (currentIdx >= 0 && currentIdx < WorldType.worldTypes.length && WorldType.worldTypes[currentIdx] != null) {
            currentType = WorldType.worldTypes[currentIdx];
        } else {
            currentType = validWorldTypes.get(0);
        }

        worldTypeButton = CyclingButton.<WorldType>builder(
                        wt -> I18n.format("selectWorld.mapType") + " " + I18n.format(wt.getTranslateName()))
                .values(validWorldTypes)
                .initially(currentType)
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
            .width(150).height(20).build();
        layout.addChild(customizeButton, row++, 1);

        // Create generate structures button
        generateStructuresButton = Button.builder(
                Text.literal(getGenerateStructuresText()),
                btn -> access.modernWorldCreatingUI$setGenerateStructures(
                    !access.modernWorldCreatingUI$getGenerateStructures()))
            .width(44).height(20).build();
        layout.addChild(generateStructuresButton, row, 1);

        // Create bonus chest button
        bonusChestButton = Button.builder(
                Text.literal(getBonusChestText()),
                btn -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setBonusChest(
                            !access.modernWorldCreatingUI$getBonusChest());
                    }
                })
            .width(44).height(20).build();
        layout.addChild(bonusChestButton, row++, 0);

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

        // Draw labels on top
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterSeed"),
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 3 - 2 - 13, 0xA0A0A0);
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.mapFeatures"),
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 2 + 15 + 6, 0xFFFFFF);
        mc.fontRenderer.drawString(I18n.format("createworldui.selectWorld.bonusItems"),
                guiCreateWorld.width / 2 - 154,
                guiCreateWorld.height / 2 - 15 + 6, 0xFFFFFF);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // All actions handled via OnPress callbacks
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        seedField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        seedField.keyTyped(typedChar, keyCode);
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
