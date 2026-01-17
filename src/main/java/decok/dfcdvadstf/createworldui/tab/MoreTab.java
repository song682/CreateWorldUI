package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.gamerule.GameRuleEditor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.GameRules;

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
            // 创建游戏规则编辑器按钮
            gameRuleEditorButton = new GuiButton(200, width / 2 - 105,
                    height / 6 + 40, 210, 20,
                    I18n.format("createworldui.button.gameRuleEditor"));
            addButton(gameRuleEditorButton);
        }

        if (CreateWorldUI.config.enableOtherMoreTabButton){
            // 创建实验性功能按钮
            experimentsButton = new GuiButton(201, width / 2 - 105,
                    height / 6 + 65, 210, 20,
                    I18n.format("selectWorld.experiments"));
            addButton(experimentsButton);

            // 创建数据包按钮
            dataPacksButton = new GuiButton(202, width / 2 - 105,
                    height / 6 + 90, 210, 20,
                    I18n.format("selectWorld.dataPacks"));
            addButton(dataPacksButton);
        }

        // 初始隐藏所有按钮
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        // 更多标签页可能不需要额外绘制内容
    }

    @Override
    public void actionPerformed(GuiButton button) {
        System.out.println("MoreTab: Button clicked: " + button.id);

        if (button.id == 200) {
            // 打开游戏规则编辑器
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null) pending = new HashMap<>();

            // 预填充默认值
            if (pending.isEmpty()) {
                // 从临时 GameRules 获取默认值
                GameRules temp = new GameRules();
                String[] keys = temp.getRules();
                if (keys != null) {
                    for (String key : keys) {
                        pending.put(key, temp.getGameRuleStringValue(key));
                    }
                }
            }

            mc.displayGuiScreen(new GameRuleEditor(tabManager.getParent(), pending));
        } else if (button.id == 201) {
            // 打开实验性功能界面
            System.out.println("MoreTab: Experiments button clicked");
        } else if (button.id == 202) {
            // 打开数据包界面
            System.out.println("MoreTab: Data packs button clicked");
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // 无需处理
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // 无需处理
    }
}