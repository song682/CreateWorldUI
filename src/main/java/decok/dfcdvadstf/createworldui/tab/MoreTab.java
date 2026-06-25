package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * More Options Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的更多选项标签页。</p>
 */
public class MoreTab extends GridLayoutTab {
    private Button gameRuleEditorButton;
    private Button experimentsButton;
    private Button dataPacksButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public MoreTab() {
        super(102, Text.translatable("createworldui","createworldui.tab.more"));
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;

        // Single column with row spacing
        layout.rowSpacing(8);

        int row = 0;

        if (CreateWorldUI.config.gameruleEdit) {
            gameRuleEditorButton = Button.builder(
                    Text.translatable("createworldui", "createworldui.button.gamerule_editor"),
                    btn -> {
                        Map<String, String> pending = GameRuleApplier.getPendingGameRules();
                        if (pending == null) pending = new HashMap<>();
                        Map<String, String> cleanPending = new HashMap<>();
                        for (Map.Entry<String, String> entry : pending.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                cleanPending.put(entry.getKey(), entry.getValue());
                            }
                        }
                        mc.displayGuiScreen(new GuiScreenGameRuleEditor(guiCreateWorld, cleanPending));
                    }).useVanillaTexture(true)
                .width(210).height(20).build();
            layout.addChild(gameRuleEditorButton, row++, 0);
        }

        if (CreateWorldUI.config.enableOtherMoreTabButton) {
            experimentsButton = Button.builder(
                    Text.literal(I18n.format("selectWorld.experiments")),
                    btn -> {})
                .width(210).height(20).useVanillaTexture(true).build();
            layout.addChild(experimentsButton, row++, 0);

            dataPacksButton = Button.builder(
                    Text.literal(I18n.format("selectWorld.dataPacks")),
                    btn -> {})
                .width(210).height(20).useVanillaTexture(true).build();
            layout.addChild(dataPacksButton, row++, 0);
        }

        super.initGui(tabManager, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        super.drawScreen(mouseX, mouseY, partialTicks);
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
    }
}
