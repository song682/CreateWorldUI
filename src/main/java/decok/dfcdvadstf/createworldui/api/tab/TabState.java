package decok.dfcdvadstf.createworldui.api.tab;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.createworldui.CreateWorldUI;

/**
 * <p>
 *     标签页状态枚举<br>
 *     定义标签页在不同交互状态下的纹理坐标和文本颜色
 * </p>
 * <p>
 *     Tab state enumeration<br>
 *     Defines texture coordinates and text colors for tabs in different interaction states
 * </p>
 *
 */
public enum TabState {
    /**
     * 正常状态：未选中且未hover<br>
     * Normal state: unselected and not hovered
     */
    NORMAL(0, 0, 0xFFFFFF, false),

    /**
     * 悬停状态：未选中但鼠标hover<br>
     * Hover state: unselected but mouse hovered
     */
    HOVER(0, 24, 0xFFFF55, true),
    /**
     * 选中状态：已选中且未hover<br>
     * Selected state: selected and not hovered
     */
    SELECTED(0, 48, 0xFFFFFF, false),

    /**
     * 选中悬停状态：已选中且鼠标hover<br>
     * Selected hover state: selected and mouse hovered
     */
    SELECTED_HOVER(0, 72, 0xFFFF55, true);

    /**
     * 纹理X坐标（在纹理图中的水平位置）<br>
     * Texture X coordinate (horizontal position in texture image)
     */
    public final int u;

    /**
     * 纹理Y坐标（在纹理图中的垂直位置）<br>
     * Texture Y coordinate (vertical position in texture image)
     */
    public final int v;

    /**
     * 基础文本颜色（RGB十六进制值）<br>
     * Base text color (RGB hex value)
     */
    private final int baseTextColor;

    /**
     * 是否为高亮状态（需要根据条件判断颜色）<br>
     * Whether this is a highlight state (color depends on conditions)
     */
    private final boolean isHighlight;

    /**
     * 黄色文本颜色<br>
     * Yellow text color for highlight states
     */
    private static final int YELLOW_COLOR = 0xFFFF55;

    /**
     * 白色文本颜色<br>
     * White text color
     */
    private static final int WHITE_COLOR = 0xFFFFFF;

    /**
     * 构造标签页状态<br>
     * Constructor for tab state
     * @param u 纹理X坐标 / Texture X coordinate
     * @param v 纹理Y坐标 / Texture Y coordinate
     * @param textColor 基础文本颜色 / Base text color
     * @param isHighlight 是否为高亮状态 / Whether this is a highlight state
     */
    TabState(int u, int v, int textColor, boolean isHighlight) {
        this.u = u;
        this.v = v;
        this.baseTextColor = textColor;
        this.isHighlight = isHighlight;
    }

    /**
     * 获取文本颜色<br>
     * Gets the text color based on current conditions
     * <p>
     *     当ArchaicFix模组加载且配置项topTabCharatorModernWhite为true时，<br>
     *     高亮状态使用白色，否则使用黄色
     * </p>
     * <p>
     *     When ArchaicFix mod is loaded and topTabCharatorModernWhite config is true,<br>
     *     highlight states use white color, otherwise use yellow
     * </p>
     * @return 文本颜色 / Text color
     */
    public int getTextColor() {
        if (isHighlight) {
            // Check if ArchaicFix is loaded and config option is enabled
            // 检查是否加载了ArchaicFix且配置项已启用
            boolean archaicFixLoaded = Loader.isModLoaded("archaicfix");
            boolean configEnabled = CreateWorldUI.config != null && 
                                    CreateWorldUI.config.topTabCharatorModernWhite;
            
            if (archaicFixLoaded && configEnabled) {
                return WHITE_COLOR;
            }
            return YELLOW_COLOR;
        }
        return baseTextColor;
    }
}