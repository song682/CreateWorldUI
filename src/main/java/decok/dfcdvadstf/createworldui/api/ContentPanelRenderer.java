package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * <p>Shared renderer for the content panel used by createworld-style screens.</p>
 * <p>Well, quite simple - gives you the header separator, a tiled panel background,
 * and the footer separator. Use the pieces you want, or let {@link #drawContentPanel(int, int, int, int)}
 * do the whole thing in one shot.</p>
 *
 * <p>通用的"内容面板"绘制工具 —— 顶部分隔线、平铺面板背景、底部分隔线都打包在这儿。
 * 单独调用每一块也行；想一键出货就用 {@link #drawContentPanel(int, int, int, int)}。</p>
 */
public final class ContentPanelRenderer {

    /** Header separator (top line), 32x2 tileable. / 顶部分隔线，32x2 可平铺 */
    public static final ResourceLocation HEADER_SEPARATOR =
            new ResourceLocation("createworldui", "textures/gui/header_separator.png");

    /** Footer separator (bottom line), 32x2 tileable. / 底部分隔线，32x2 可平铺 */
    public static final ResourceLocation FOOTER_SEPARATOR =
            new ResourceLocation("createworldui", "textures/gui/footer_separator.png");

    /** Panel background, 16x16 tileable. / 面板背景，16x16 可平铺 */
    public static final ResourceLocation PANEL_BACKGROUND =
            new ResourceLocation("createworldui", "textures/gui/panel_background.png");

    /** Separator height in GUI pixels. / 分隔线在 GUI 中的高度（像素） */
    public static final int SEPARATOR_HEIGHT = 2;

    private static final int SEPARATOR_TILE_W = 32;
    private static final int SEPARATOR_TILE_H = 2;
    private static final int PANEL_TILE = 16;

    private ContentPanelRenderer() {}

    /**
     * Draw the full content panel in one call — header line on top, tiled background in the middle,
     * footer line on bottom.
     * <p>一键绘制完整内容面板：顶部分隔线 + 中间平铺背景 + 底部分隔线。</p>
     *
     * @param x      left edge / 左边缘
     * @param top    Y coordinate of the header separator (its top pixel) / 顶部分隔线的起始 Y
     * @param width  panel width / 面板宽度
     * @param bottom Y coordinate of the footer separator (its top pixel) / 底部分隔线的起始 Y
     */
    public static void drawContentPanel(int x, int top, int width, int bottom) {
        if (width <= 0) return;

        // Background sits between the two separators — right below the header, right above the footer
        // 背景夹在两条分隔线之间 —— 头线下面、尾线上面
        int bgTop = top + SEPARATOR_HEIGHT;
        int bgBottom = bottom;
        if (bgBottom > bgTop) {
            drawPanelBackground(x, bgTop, width, bgBottom - bgTop);
        }

        drawHeaderSeparator(x, top, width);
        drawFooterSeparator(x, bottom, width);
    }

    /**
     * Draw only the header separator line (2px tall, tiled horizontally).
     * <p>仅绘制顶部分隔线（2px 高，横向平铺）。</p>
     */
    public static void drawHeaderSeparator(int x, int y, int width) {
        drawSeparator(x, y, width, HEADER_SEPARATOR);
    }

    /**
     * Draw only the footer separator line (2px tall, tiled horizontally).
     * <p>仅绘制底部分隔线（2px 高，横向平铺）。</p>
     */
    public static void drawFooterSeparator(int x, int y, int width) {
        drawSeparator(x, y, width, FOOTER_SEPARATOR);
    }

    /**
     * Draw a separator line with a custom 32x2 texture — handy if you want a styled variant.
     * <p>用自定义 32x2 纹理绘制分隔线 —— 想换个花样？传进来就行。</p>
     */
    public static void drawSeparator(int x, int y, int width, ResourceLocation texture) {
        if (width <= 0 || texture == null) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        // Reset color to white so the texture isn't tinted by leftover GL state
        // 重置颜色为白色，免得前面残留的 GL 状态把纹理染色
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawTiledTexture(x, y, width, SEPARATOR_TILE_H, SEPARATOR_TILE_W, SEPARATOR_TILE_H);
    }

    /**
     * Draw only the panel background — tiles the 16x16 panel texture over the given region.
     * <p>仅绘制面板背景 —— 把 16x16 的面板纹理平铺到指定区域。</p>
     */
    public static void drawPanelBackground(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(PANEL_BACKGROUND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawTiledTexture(x, y, width, height, PANEL_TILE, PANEL_TILE);
    }

    /**
     * Tile the currently-bound texture across the given region.
     * <p>把当前绑定的纹理按 (tileWidth x tileHeight) 平铺到区域内。</p>
     */
    private static void drawTiledTexture(int x, int y, int width, int height, int tileWidth, int tileHeight) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        for (int tileX = 0; tileX < width; tileX += tileWidth) {
            for (int tileY = 0; tileY < height; tileY += tileHeight) {
                int tileW = Math.min(tileWidth, width - tileX);
                int tileH = Math.min(tileHeight, height - tileY);

                double u1 = 0.0;
                double u2 = (double) tileW / (double) tileWidth;
                double v1 = 0.0;
                double v2 = (double) tileH / (double) tileHeight;

                tessellator.addVertexWithUV(x + tileX, y + tileY + tileH, 0.0D, u1, v2);
                tessellator.addVertexWithUV(x + tileX + tileW, y + tileY + tileH, 0.0D, u2, v2);
                tessellator.addVertexWithUV(x + tileX + tileW, y + tileY, 0.0D, u2, v1);
                tessellator.addVertexWithUV(x + tileX, y + tileY, 0.0D, u1, v1);
            }
        }

        tessellator.draw();
    }
}
