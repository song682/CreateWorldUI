package decok.dfcdvadstf.createworldui.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class TexturedButton extends GuiButton {

    private final ResourceLocation texture;
    private final int texU;
    private final int texV;
    private final int hoverU;
    private final int hoverV;

    public TexturedButton(int id, int x, int y, String text,
                          ResourceLocation texture,
                          int texU, int texV,
                          int hoverU, int hoverV) {
        super(id, x, y, 100, 20, text);
        this.texture = texture;
        this.texU = texU;
        this.texV = texV;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(texture);
            boolean isHovered = mouseX >= this.xPosition &&
                    mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width &&
                    mouseY < this.yPosition + this.height;

            if (isHovered) {
                drawTexturedModalRect(this.xPosition, this.yPosition, hoverU, hoverV, width, height);
            } else {
                drawTexturedModalRect(this.xPosition, this.yPosition, texU, texV, width, height);
            }

            // 绘制按钮文本
            int color = isHovered ? 0xFFFFA0 : 0xE0E0E0;
            drawCenteredString(mc.fontRenderer, this.displayString,
                    this.xPosition + this.width / 2,
                    this.yPosition + (this.height - 8) / 2, color);
        }
    }
}
