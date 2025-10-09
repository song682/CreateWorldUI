package decok.dfcdvadstf.createworldui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TextureHelper {

    /**
     * 绘制可自定义尺寸的纹理（支持平铺）
     * @param resource 纹理资源位置
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
            ResourceLocation resource,
            int x, int y,
            float u, float v,
            int width, int height,
            int textureWidth, int textureHeight,
            float tileScale
    ) {
        // 绑定纹理
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(resource);

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

                // 调用原版绘制方法
                Gui.func_146110_a(
                        tilePosX, tilePosY,
                        (int)(u * textureWidth), (int)(v * textureHeight),
                        tileWidth, tileHeight,
                        textureWidth, textureHeight
                );
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * 简化版本 - 绘制纹理矩形
     */
    public static void drawModalRectWithCustomSizedTexture(ResourceLocation resource,
                                                           int x, int y, float u, float v, int width, int height,
                                                           float textureWidth, float textureHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(resource);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.func_146110_a(x, y, (int)u, (int)v, width, height, (int)textureWidth, (int)textureHeight);

        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * 绘制渐变背景
     */
    public static void drawGradientBackground(int width, int height) {
        // 绘制深色渐变背景
        drawGradientRect(0, 0, width, height, 0xFF1a1a1a, 0xFF0a0a0a);
    }

    /**
     * 绘制渐变矩形
     */
    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(f1, f2, f3, f);
        GL11.glVertex2f((float)right, (float)top);
        GL11.glVertex2f((float)left, (float)top);
        GL11.glColor4f(f5, f6, f7, f4);
        GL11.glVertex2f((float)left, (float)bottom);
        GL11.glVertex2f((float)right, (float)bottom);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}