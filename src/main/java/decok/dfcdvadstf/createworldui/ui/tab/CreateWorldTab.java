package decok.dfcdvadstf.createworldui.ui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.layouts.FrameLayout;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;

/**
 * <p>Base class of the create-world tabs — a {@link GridLayoutTab} with a per-frame
 * preparation hook, an initial-focus provider, and the per-tab vertical alignment
 * factor kept from the previous tab architecture.</p>
 * <p>创建世界各标签页的公共基类 —— 在 {@link GridLayoutTab} 基础上提供每帧准备钩子、
 * 初始焦点提供者，并保留旧标签页架构中每个 Tab 独立的垂直对齐系数。</p>
 */
public abstract class CreateWorldTab extends GridLayoutTab {

    /**
     * Vertical alignment factor inside the tab area (upstream default 0.16666667F ≈ 1/6);
     * subclasses may raise it for extra top spacing.
     * <p>在标签页内容区域内的垂直对齐系数（上游默认 0.16666667F ≈ 1/6）；
     * 子类可调大以获得更大的顶部间距。</p>
     */
    protected float verticalAlignment = 0.16666667F;

    protected CreateWorldTab(Text title) {
        super(title);
    }

    /**
     * {@inheritDoc}
     * <p>复刻上游实现，但使用本类的 {@link #verticalAlignment} 而非硬编码值。</p>
     */
    @Override
    public void doLayout(ScreenRectangle screenRectangle) {
        this.layout.arrangeElements();
        FrameLayout.alignInRectangle(this.layout, screenRectangle.x, screenRectangle.y,
                screenRectangle.width, screenRectangle.height, 0.5F, this.verticalAlignment);
    }

    /**
     * Per-frame preparation — called by the host before the tab's children are
     * rendered; use for dynamic states, tooltips and label overlays.
     * <p>每帧准备 —— 宿主在本标签页子组件渲染前调用；用于动态状态、tooltip 与
     * 顶层标签绘制。</p>
     */
    public void prepareFrame() {
    }

    /**
     * @return the widget to focus when this tab becomes current, or {@code null}
     *         / 本标签页成为当前页时应当聚焦的组件；无则 {@code null}
     */
    public GuiEventListener getInitialFocus() {
        return null;
    }
}
