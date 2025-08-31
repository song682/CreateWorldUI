package decok.dfcdvadstf.createworldui.api.hook;

import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.io.File;

public class WorldCreationCheck {

    private static final boolean FORCE_MODERN_UI = true;

    public static boolean shouldUseModernUI() {

        if (FORCE_MODERN_UI) {
            return true;
        }

        try {
            File savesDir = new File(Minecraft.getMinecraft().mcDataDir, "saves");
            // 检查saves目录是否存在且为空
            return !savesDir.exists() || (savesDir.list() != null && savesDir.list().length == 0);
        } catch (Exception e) {
            // 如果出现任何异常，默认使用现代UI
            return true;
        }
    }

    public static GuiScreen createModernWorldCreationScreen(GuiScreen parent) {
        return new GuiCreateWorldModern(parent);
    }
}
