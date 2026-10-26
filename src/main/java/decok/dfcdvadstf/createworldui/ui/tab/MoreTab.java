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

    /**
     * Builds the tab once; the widgets live as long as the screen instance.
     * <p>一次性构建标签页；组件随屏幕实例存活。</p>
     */
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
            // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 统一延迟绘制
            // Component-level tooltip: auto-pumped by WidgetTooltipHolder via extractRenderState, drawn deferred by CatFrame at end of frame
            gameRuleEditorButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.gameRuleEditor")));
            layout.addChild(gameRuleEditorButton, row++, 0);
        }

        if (CreateWorldUI.config.enableOtherMoreTabButton) {
            // 实验界面按钮：打开 ExperimentScreen（由其他模组经 IExperimentScreenUsage 注册的实验开关）
            // Experiments button: opens ExperimentScreen (switches registered by other mods via IExperimentScreenUsage)
            Button experimentsButton = Button.builder(
                    Text.literal(I18n.format("selectWorld.experiments")),
                    btn -> Minecraft.getMinecraft().displayGuiScreen(new ExperimentScreen(guiCreateWorld)))
                .width(210).height(20).useVanillaTexture(true).build();
            experimentsButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.experiments")));
            layout.addChild(experimentsButton, row++, 0);

            // 数据包按钮：打开 DataPackScreen（OKCore 数据包格式兼容：左可选择、右已激活）
            // Data packs button: opens DataPackScreen (OKCore format compatible: left selectable, right activated)
            Button dataPacksButton = Button.builder(
                    Text.literal(I18n.format("selectWorld.dataPacks")),
                    btn -> Minecraft.getMinecraft().displayGuiScreen(new DataPackScreen(guiCreateWorld)))
                .width(210).height(20).useVanillaTexture(true).build();
            dataPacksButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.dataPacks")));
            layout.addChild(dataPacksButton, row++, 0);
        }
    }
}
