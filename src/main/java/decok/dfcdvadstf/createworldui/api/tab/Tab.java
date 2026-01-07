package decok.dfcdvadstf.createworldui.api.tab;

import net.minecraft.client.gui.GuiButton;

/**
 *
 */
public interface Tab {
    void initGui(TabManager tabManager, int width, int height);
    void drawScreen(int mouseX, int mouseY, float partialTicks);
    void actionPerformed(GuiButton button);
    void mouseClicked(int mouseX, int mouseY, int mouseButton);
    void keyTyped(char typedChar, int keyCode);
    int getTabId();
    String getTabName();
    void setVisible(boolean visible);
}
