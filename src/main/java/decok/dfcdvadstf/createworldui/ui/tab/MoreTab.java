package decok.dfcdvadstf.createworldui.ui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.components.tab.GridLayoutTab;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.ui.datapack.DataPackScreen;
import decok.dfcdvadstf.createworldui.ui.experiment.ExperimentScreen;
import decok.dfcdvadstf.createworldui.ui.gamerule.WorldCreationGameRuleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * More Options Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的更多选项标签页。</p>
 */
public class MoreTab extends GridLayoutTab {
    private GuiCreateWorld guiCreateWorld;

    public MoreTab(GuiCreateWorld screen) {
        super(Text.translatable("createworldui.tab.more"));
        this.guiCreateWorld = screen;

        // Single column with row spacing
        layout.rowSpacing(8);

        int row = 0;

        if (CreateWorldUI.config.gameruleEdit) {
            Button gameRuleEditorButton = Button.builder(
                    Text.literal(I18n.format("createworldui.button.gamerule_editor")),
                    btn -> {
                        Map<String, String> pending = GameRuleApplier.getPendingGameRules();
                        if (pending == null) pending = new HashMap<>();
                        Map<String, String> cleanPending = new HashMap<>();
                        for (Map.Entry<String, String> entry : pending.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                cleanPending.put(entry.getKey(), entry.getValue());
                            }
                        }
                        Minecraft.getMinecraft().displayGuiScreen(new WorldCreationGameRuleScreen(guiCreateWorld, cleanPending));
                    }).useVanillaTexture(true)
                .width(210).height(20).build();
            gameRuleEditorButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.gameRuleEditor")));
            layout.addChild(gameRuleEditorButton, row++, 0);
        }

        if (CreateWorldUI.config.enableDataPack) {
            // Data packs button: opens DataPackScreen (OKCore format compatible: left selectable, right activated)
            Button dataPacksButton = Button.builder(
                    Text.literal(I18n.format("selectWorld.dataPacks")),
                    btn -> Minecraft.getMinecraft().displayGuiScreen(new DataPackScreen(guiCreateWorld)))
                .width(210).height(20).useVanillaTexture(true).build();
            dataPacksButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.dataPacks")));
            layout.addChild(dataPacksButton, row++, 0);
        }

        if (CreateWorldUI.config.enableExperiment) {
            // Experiments button: opens ExperimentScreen (switches registered by other mods via IExperimentScreenUsage)
            Button experimentsButton = Button.builder(
                            Text.literal(I18n.format("selectWorld.experiments")),
                            btn -> Minecraft.getMinecraft().displayGuiScreen(new ExperimentScreen(guiCreateWorld)))
                    .width(210).height(20).useVanillaTexture(true).build();
            experimentsButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.experiments")));
            layout.addChild(experimentsButton, row++, 0);
        }
    }
}
