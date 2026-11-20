package decok.dfcdvadstf.createworldui.ui;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreateWorldConfigUI implements IModGuiFactory {
    @Override
    public void initialize(final Minecraft mincraftinstance) {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigUI.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(final RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigUI extends GuiConfig {
        public ConfigUI(final GuiScreen parentScreen) {
            super(parentScreen, getConfigElements (CreateWorldUI.config.configFile), Tags.MODID, false, false, I18n.format("config.title"));
        }

        @SuppressWarnings("rawtypes")
        private static List<IConfigElement> getConfigElements(final Configuration configuration) {
            List<IConfigElement> elements = new ArrayList<>();

            ConfigCategory expCategory = configuration.getCategory("experiment");
            expCategory.setLanguageKey("config.button.EXP");
            expCategory.setComment(I18n.format("config.subtitle.EXP"));
            elements.add(new ConfigElement<>(expCategory));

            ConfigCategory greCategory = configuration.getCategory("gamerule editor");
            greCategory.setLanguageKey("config.button.GRE");
            greCategory.setComment(I18n.format("config.subtitle.GRE"));
            elements.add(new ConfigElement<>(greCategory));

            ConfigCategory uimCategory = configuration.getCategory("ui management");
            uimCategory.setLanguageKey("config.button.UIM");
            uimCategory.setComment(I18n.format("config.subtitle.UIM"));
            elements.add(new ConfigElement<>(uimCategory));

            return elements;
        }
    }
}
