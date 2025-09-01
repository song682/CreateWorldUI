package decok.dfcdvadstf.createworldui.api.util;

import static decok.dfcdvadstf.createworldui.api.util.TextureManager.*;

public enum TabState {
    NORMAL(TAB_NORMAL_U, TAB_NORMAL_V, 0xE0E0E0),
    HOVER(TAB_HOVER_U, TAB_HOVER_V, 0xFFFFFF),
    SELECTED(TAB_SELECTED_U, TAB_SELECTED_V, 0xFFCC00),
    SELECTED_HOVER(TAB_SELECTED_HOVER_U, TAB_SELECTED_HOVER_V, 0xFFCC00);

    final int u;
    final int v;
    final int textColor;

    TabState(int u, int v, int textColor) {
        this.u = u;
        this.v = v;
        this.textColor = textColor;
    }

    static TabState getState(boolean isSelected, boolean isHovered) {
        if (isSelected) {
            return isHovered ? SELECTED_HOVER : SELECTED;
        }
        return isHovered ? HOVER : NORMAL;
    }
}
