package decok.dfcdvadstf.createworldui.api;

import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.WorldSettings;

public class CreateWorldAPI {
    private static final List<IWorldTab> registeredTabs = new ArrayList<>();
    private static final Map<Class<?>, ITabCustomizer> customizers = new HashMap<>();

    public static void registerTab(IWorldTab tab) {
        registeredTabs.add(tab);
    }

    public static void registerTabCustomizer(Class<?> targetClass, ITabCustomizer customizer) {
        customizers.put(targetClass, customizer);
    }

    public static List<IWorldTab> getTabs() {
        return Collections.unmodifiableList(registeredTabs);
    }

    public static ITabCustomizer getCustomizer(Class<?> targetClass) {
        return customizers.get(targetClass);
    }

    public static interface IWorldTab {
        /**
         * A {@code getTabName()} is a method that can be used to get a tab's name, can be returned with a string; support Localization.
         * <p>
         * A {@code initGui(GuiScreen parent, WorldSettings settings)}
         */
        String getTabName();
        void initGui(GuiScreen parent, WorldSettings settings);
        void drawScreen(int mouseX, int mouseY, float partialTicks);
        void actionPerformed(GuiButton button) throws IOException;
        void keyTyped(char typedChar, int keyCode) throws IOException;
        void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException;
        WorldSettings applySettings(WorldSettings settings);
        int getTabOrder();
    }

    public static interface ITabCustomizer {
        void customize(List<GuiButton> buttonList);
    }
}