package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.WorldSettings;

/**
 * @author dfdvdsf
 * @version 0.0.1.6
 *
 * @deprecated This InterFace is deprecated, integrated into CreateWorldAPI.IWorldTab
 * @see CreateWorldAPI.IWorldTab
 */

@Deprecated
public interface InterFaceWorldTab {
    String getTabName();
    GuiScreen createTab(GuiCreateWorld parent, WorldSettings settings);
    int getTabOrder();

    public static class SettingsSnapshot {
        public final String worldName;
        public final String seed;
        public final boolean generateStructures;
        public final boolean bonusChest;
        public final WorldSettings worldSettings;

        public SettingsSnapshot(String name, String seed, boolean structures, boolean chest, WorldSettings settings) {
            this.worldName = name;
            this.seed = seed;
            this.generateStructures = structures;
            this.bonusChest = chest;
            this.worldSettings = settings;
        }
    }

    SettingsSnapshot getSettingsSnapshot();
    void applySettingsSnapshot(SettingsSnapshot snapshot);
}
