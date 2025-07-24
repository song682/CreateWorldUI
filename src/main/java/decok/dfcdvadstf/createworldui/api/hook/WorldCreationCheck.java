package decok.dfcdvadstf.createworldui.api.hook;

import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.io.File;

public class WorldCreationCheck {

    public static boolean shouldUseModernUI() {
        File savesDir = new File(Minecraft.getMinecraft().mcDataDir, "saves");
        return !savesDir.exists() || (savesDir.list() != null && savesDir.list().length == 0);
    }

    public static GuiScreen createModernWorldCreationScreen(GuiScreen parent) {
        return new GuiCreateWorldModern(parent);
    }
}
