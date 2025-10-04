package decokalt.dfcdvadstf.createworldui.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TextureHelper {

    /**
     * 绘制可自定义尺寸的纹理（支持平铺）
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     * @param u 纹理U坐标 (0-1)
     * @param v 纹理V坐标 (0-1)
     * @param width 绘制宽度
     * @param height 绘制高度
     * @param textureWidth 纹理实际宽度（像素）
     * @param textureHeight 纹理实际高度（像素）
     * @param tileScale 平铺缩放比例（1.0为原始尺寸）
     */

    public static void drawCustomTexturedRect(
            int x, int y,
            float u, float v,
            int width, int height,
            int textureWidth, int textureHeight,
            float tileScale
    ) {
        // 绑定纹理
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(new ResourceLocation("yourmodid", "textures/gui/background.png"));

        // 计算平铺参数
        float scaledWidth = width / tileScale;
        float scaledHeight = height / tileScale;
        int tileCountX = (int) Math.ceil(scaledWidth);
        int tileCountY = (int) Math.ceil(scaledHeight);

        // 启用混合和纹理
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 平铺绘制
        for (int tileX = 0; tileX < tileCountX; tileX++) {
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                int tilePosX = x + (int)(tileX * tileScale * textureWidth);
                int tilePosY = y + (int)(tileY * tileScale * textureHeight);
                int tileWidth = Math.min((int)(textureWidth * tileScale), width - tileX * (int)(textureWidth * tileScale));
                int tileHeight = Math.min((int)(textureHeight * tileScale), height - tileY * (int)(textureHeight * tileScale));

                // 调用原版绘制方法:cite[2]:cite[3]
                Gui.func_146110_a( // 实际方法名：drawTexturedModalRect
                        tilePosX, tilePosY,
                        (int)(u * textureWidth), (int)(v * textureHeight),
                        tileWidth, tileHeight,
                        textureWidth, textureHeight
                );
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void drawModalRectWithCustomSizedTexture(int i, int i1, int i2, int i3, int width, int height, int width1, int height1) {
    }
}