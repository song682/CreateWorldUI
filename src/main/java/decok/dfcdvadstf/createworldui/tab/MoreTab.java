package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import java.util.HashMap;
import java.util.Map;


public class MoreTab extends AbstractScreenTab {
    private GuiButton gameRuleEditorButton;
    private GuiButton experimentsButton;
    private GuiButton dataPacksButton;

    public MoreTab() {
        super(102, "createworldui.tab.more");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);

        if (CreateWorldUI.config.gameruleEdit){
            // Create game rule editor button
            // 创建游戏规则编辑器按钮
            gameRuleEditorButton = new GuiButton(200, width / 2 - 105,
                    height / 6 + 40, 210, 20,
                    I18n.format("createworldui.button.gameRuleEditor"));
            addButton(gameRuleEditorButton);
        }

        if (CreateWorldUI.config.enableOtherMoreTabButton){
            // Create experiments button
            // 创建实验性功能按钮
            experimentsButton = new GuiButton(201, width / 2 - 105,
                    height / 6 + 65, 210, 20,
                    I18n.format("selectWorld.experiments"));
            addButton(experimentsButton);

            // Create data packs button
            // 创建数据包按钮
            dataPacksButton = new GuiButton(202, width / 2 - 105,
                    height / 6 + 90, 210, 20,
                    I18n.format("selectWorld.dataPacks"));
            addButton(dataPacksButton);
        }

        // Initially hide all buttons
        // 初始隐藏所有按钮
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        // More tab may not need extra drawing
        // 更多标签页可能不需要额外绘制内容
    }

    @Override
    public void actionPerformed(GuiButton button) {
        System.out.println("MoreTab: Button clicked: " + button.id);

        if (button.id == 200) {
            // Open game rule editor
            // 打开游戏规则编辑器
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null) pending = new HashMap<>();

            // Filter out null values
            // 过滤掉 null 値
            Map<String, String> cleanPending = new HashMap<>();
            for (Map.Entry<String, String> entry : pending.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    cleanPending.put(entry.getKey(), entry.getValue());
                }
            }

            mc.displayGuiScreen(new GuiScreenGameRuleEditor(tabManager.getParent(), cleanPending));
        } else if (button.id == 201) {
            // Open experiments screen
            // 打开实验性功能界面
            System.out.println("MoreTab: Experiments button clicked");
        } else if (button.id == 202) {
            // Open data packs screen
            // 打开数据包界面
            System.out.println("MoreTab: Data packs button clicked");
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // No handling needed / 无需处理
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // No handling needed / 无需处理
    }
}