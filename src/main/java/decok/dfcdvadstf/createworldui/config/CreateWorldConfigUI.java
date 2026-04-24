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

            // Note: Forge Configuration lowercases category names internally,
            // so we must use lowercase here to match the stored categories.
            // 注意：Forge Configuration 内部会将分类名转为小写，所以这里必须用小写来匹配。
            ConfigCategory greCategory = configuration.getCategory("gamerule editor");
            greCategory.setLanguageKey("config.button.GRE");
            greCategory.setComment(I18n.format("config.subtitle.GRE"));
            elements.add(new ConfigElement(greCategory));

            ConfigCategory uimCategory = configuration.getCategory("ui management");
            uimCategory.setLanguageKey("config.button.UIM");
            uimCategory.setComment(I18n.format("config.subtitle.UIM"));
            elements.add(new ConfigElement(uimCategory));

            return elements;
        }
    }
}
