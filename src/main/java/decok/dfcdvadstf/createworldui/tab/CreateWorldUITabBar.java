package decok.dfcdvadstf.createworldui.tab;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.catframe.ui.tab.TabBar;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.util.ResourceLocation;

/**
 * <p>The TabBar used by ModernWorldCreatingUI's create-world screen — gives the top
 * tab strip its dark tiled background and points all tabs to the mod's own button
 * texture. Resource packs (e.g. MCNT) can still override these PNGs to re-skin
 * the bar without touching code.</p>
 * <p>ModernWorldCreatingUI 创建世界界面用的 TabBar——给顶部 tab 条配上深色平铺背景，
 * 同时把所有 tab 按钮指向本模组自己的纹理。资源包（比如 MCNT）可以直接覆盖这俩 PNG
 * 来给 bar 换皮，根本不用碰代码。</p>
 */
public class CreateWorldUITabBar extends TabBar {

    /** Unique bar identifier. / 此 Bar 的唯一标识。 */
    public static final String BAR_ID = "createworld_top_bar";

    /** Tiled top-strip background — same image the legacy code used. / 顶部条平铺背景，跟旧代码用的同一张图。 */
    public static final ResourceLocation BACKGROUND_TEXTURE =
            new ResourceLocation("createworldui", "textures/gui/options_background_dark.png");

    public CreateWorldUITabBar() {
        super(BAR_ID);
        if (Loader.isModLoaded("clearmybackground")) {
            setBackgroundColor(0x00000000);
            setTabTexture(new ResourceLocation(Tags.MODID, "textures/gui/tabs_clear.png"));
        } else {
            // Fully transparent solid fill — the tiled texture below carries all the look.
            // 完全透明的纯色填充——视觉效果全靠下面的平铺贴图。
            setBackgroundColor(0x00000000);
            setBackgroundTexture(BACKGROUND_TEXTURE);
        }
    }
}
