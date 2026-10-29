package decok.dfcdvadstf.createworldui.api;

import decok.dfcdvadstf.catframe.ui.GuiGraphicsExtractor;
import decok.dfcdvadstf.catframe.ui.components.Renderable;
import decok.dfcdvadstf.catframe.ui.components.events.AbstractContainerEventHandler;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Component-system root container of the modern create-world screen (CatFrame 0.10+).</p>
 * <p>Holds the tab navigation bar plus the current tab's children; rendering, focus
 * navigation and event dispatch all flow through the CatFrame component pipeline, so the
 * host mixin only forwards raw input and never touches individual widgets.</p>
 * <p>现代创建世界界面的组件系统根容器（CatFrame 0.10+）。</p>
 * <p>持有 Tab 导航栏与当前 Tab 的子组件；渲染、焦点导航与事件分发全部经由 CatFrame
 * 组件管线，宿主 Mixin 只负责转发原始输入，不再直接操作任何组件。</p>
 */
public class TabScreenRoot extends AbstractContainerEventHandler {

    private final List<GuiEventListener> children = new ArrayList<>();

    public TabScreenRoot() {
        // The root hosts the pipeline but owns no visual area of its own: children carry
        // their own bounds, so a 0x0 footprint keeps its own hover checks inert.
        // 根容器承载整条组件管线但不拥有任何可视区域：边界由子组件携带，
        // 以 0x0 尺寸让自身的 hover 检查保持惰性。
        super(0, 0, 0, 0);
    }

    /**
     * Register a child (wired as TabManager's {@code addWidget} consumer).
     * <p>注册子组件（作为 TabManager 的 addWidget 消费者接入）。</p>
     */
    public void addChild(GuiEventListener child) {
        this.children.add(child);
    }

    /**
     * Unregister a child (wired as TabManager's {@code removeWidget} consumer); clears
     * focus first when the removed child currently holds it.
     * <p>注销子组件（作为 TabManager 的 removeWidget 消费者接入）；若被移除者当前
     * 持有焦点则先清空焦点。</p>
     */
    public void removeChild(GuiEventListener child) {
        if (this.getFocused() == child) {
            this.setFocused(null);
        }
        this.children.remove(child);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        for (GuiEventListener child : this.children) {
            if (child.isVisible() && child instanceof Renderable) {
                ((Renderable) child).extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        }
    }
}
