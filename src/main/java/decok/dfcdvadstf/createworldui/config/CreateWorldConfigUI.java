package decok.dfcdvadstf.createworldui.config;

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

        private static List<IConfigElement> getConfigElements(final Configuration configuration) {
            List<IConfigElement> elements = new ArrayList<>();

            // 添加 GameRule Editor 分类
            ConfigCategory greCategory = configuration.getCategory("GameRule Editor");
            greCategory.setLanguageKey("config.button.GRE");
            greCategory.setComment(I18n.format("config.subtitle.GRE"));
            elements.add(new ConfigElement(greCategory));

            // 添加 UI Management 分类
            ConfigCategory uimCategory = configuration.getCategory("UI Management");
            uimCategory.setLanguageKey("config.button.UIM");
            uimCategory.setComment(I18n.format("config.subtitle.UIM"));
            elements.add(new ConfigElement(uimCategory));

            return elements;
        }
    }
}
