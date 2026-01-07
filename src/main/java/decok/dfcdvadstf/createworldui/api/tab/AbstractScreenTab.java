package decok.dfcdvadstf.createworldui.api.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractScreenTab implements Tab {
    protected TabManager tabManager;
    protected Minecraft mc;
    protected List<GuiButton> tabButtons = new ArrayList<>();
    protected boolean visible = true;
    protected int tabId;
    protected String tabNameKey;

    public AbstractScreenTab(int tabId, String tabNameKey) {
        this.tabId = tabId;
        this.tabNameKey = tabNameKey;
        this.mc = Minecraft.getMinecraft();
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        this.tabManager = tabManager;
        tabButtons.clear();
    }

    @Override
    public int getTabId() {
        return tabId;
    }

    @Override
    public String getTabName() {
        return I18n.format(tabNameKey);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        for (GuiButton button : tabButtons) {
            button.visible = visible;
        }
    }

    protected void addButton(GuiButton button) {
        tabButtons.add(button);
        tabManager.addButton(button);
    }

    // 辅助方法获取状态
    protected String getWorldName() { return tabManager.getWorldName(); }
    protected String getGameMode() { return tabManager.getGameMode(); }
    protected String getSeed() { return tabManager.getSeed(); }
    protected int getWorldTypeIndex() { return tabManager.getWorldTypeIndex(); }
    protected boolean getGenerateStructures() { return tabManager.getGenerateStructures(); }
    protected boolean getBonusChest() { return tabManager.getBonusChest(); }
    protected boolean getAllowCheats() { return tabManager.getAllowCheats(); }
    protected boolean getHardcore() { return tabManager.getHardcore(); }
    protected EnumDifficulty getDifficulty() { return tabManager.getDifficulty(); }
}