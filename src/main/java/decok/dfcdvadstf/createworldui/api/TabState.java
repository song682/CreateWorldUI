package decok.dfcdvadstf.createworldui.api;

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
    NORMAL(0, 0, 0xFFFFFF),

    /**
     * 悬停状态：未选中但鼠标hover<br>
     * Hover state: unselected but mouse hovered
     */
    HOVER(0, 24, 0xFFFFFF),
    /**
     * 选中状态：已选中且未hover<br>
     * Selected state: selected and not hovered
     */
    SELECTED(0, 48, 0xFFFFFF),

    /**
     * 选中悬停状态：已选中且鼠标hover<br>
     * Selected hover state: selected and mouse hovered
     */
    SELECTED_HOVER(0, 72, 0xFFFFFF);

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
     * 文本颜色（RGB十六进制值）<br>
     * Text color (RGB hex value)
     */
    public final int textColor;

    /**
     * 构造标签页状态<br>
     * Constructor for tab state
     * @param u 纹理X坐标 / Texture X coordinate
     * @param v 纹理Y坐标 / Texture Y coordinate
     * @param textColor 文本颜色 / Text color
     */
    TabState(int u, int v, int textColor) {
        this.u = u;
        this.v = v;
        this.textColor = textColor;
    }
}