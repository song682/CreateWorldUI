package decok.dfcdvadstf.createworldui.api;

public enum TabState {
    NORMAL(0, 0, 0xFFFFFF),
    HOVER(0, 24, 0xFFFFFF),
    SELECTED(0, 48, 0xFFFFFF),
    SELECTED_HOVER(0, 72, 0xFFFFFF);

    public final int u;
    public final int v;
    public final int textColor;

    TabState(int u, int v, int textColor) {
        this.u = u;
        this.v = v;
        this.textColor = textColor;
    }
}
