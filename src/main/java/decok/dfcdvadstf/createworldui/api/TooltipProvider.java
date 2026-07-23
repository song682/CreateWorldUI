package decok.dfcdvadstf.createworldui.api;

import java.util.List;

/**
 * <p>A tab that can supply vanilla-style tooltip lines for whatever component is
 * currently under the mouse cursor.</p>
 * <p>Because CatFrame's {@code Component} has no built-in tooltip storage, each tab
 * that owns its components is responsible for mapping the hovered component to its
 * tooltip text. The Mixin queries the current tab through this interface and renders
 * the returned lines with {@code GuiScreen.drawHoveringText}.</p>
 *
 * <p>能为鼠标当前悬停的组件提供原版风格 tooltip 文本行的标签页。</p>
 * <p>由于 CatFrame 的 {@code Component} 自身不存储 tooltip，持有组件的各标签页
 * 负责将悬停的组件映射到对应的 tooltip 文本。Mixin 通过此接口向当前标签页查询，
 * 并使用 {@code GuiScreen.drawHoveringText} 渲染返回的文本行。</p>
 */
public interface TooltipProvider {

    /**
     * <p>Returns the tooltip lines to render at the cursor, or {@code null}/empty if the
     * cursor is not over any component that has a tooltip.</p>
     * <p>返回应在光标处渲染的 tooltip 文本行；若光标未悬停在任何带 tooltip 的组件上，
     * 返回 {@code null} 或空列表。</p>
     *
     * @param mouseX mouse X coordinate / 鼠标 X 坐标
     * @param mouseY mouse Y coordinate / 鼠标 Y 坐标
     * @return tooltip lines, or {@code null}/empty when there is nothing to show
     */
    List<String> getTooltipLines(int mouseX, int mouseY);
}
