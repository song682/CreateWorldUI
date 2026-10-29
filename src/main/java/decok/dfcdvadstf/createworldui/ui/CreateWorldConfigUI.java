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

        /**
         * Builds the top level config elements, one per category.
         *
         * <p>The raw type argument {@code IConfigElement} is kept intentionally: FML's
         * {@link GuiConfig} constructor is declared as {@code List<IConfigElement>} with a raw
         * type argument, so a parameterized {@code List<IConfigElement<?>>} cannot be passed to it
         * (generics are invariant). The warning is therefore suppressed locally instead of being
         * "fixed" into a non-compilable signature.
         * {@code ConfigElement} instantiations are parameterized explicitly where possible.
         * FML 的 GuiConfig 构造形参本身就是带原始类型参数的 {@code List<IConfigElement>}，
         * 这里只能沿用原始类型并定点抑制告警。
         */
        @SuppressWarnings("rawtypes")
        private static List<IConfigElement> getConfigElements(final Configuration configuration) {
            List<IConfigElement> elements = new ArrayList<>();

            // Note: Forge Configuration lowercases category names internally,
            // so we must use lowercase here to match the stored categories.
            // 注意：Forge Configuration 内部会将分类名转为小写，所以这里必须用小写来匹配。
            ConfigCategory expCategory = configuration.getCategory("experiment");
            expCategory.setLanguageKey("config.button.EXP");
            expCategory.setComment(I18n.format("config.subtitle.EXP"));
            elements.add(new ConfigElement<Object>(expCategory));

            ConfigCategory greCategory = configuration.getCategory("gamerule editor");
            greCategory.setLanguageKey("config.button.GRE");
            greCategory.setComment(I18n.format("config.subtitle.GRE"));
            elements.add(new ConfigElement<Object>(greCategory));

            ConfigCategory uimCategory = configuration.getCategory("ui management");
            uimCategory.setLanguageKey("config.button.UIM");
            uimCategory.setComment(I18n.format("config.subtitle.UIM"));
            elements.add(new ConfigElement<Object>(uimCategory));

            return elements;
        }
    }
}
