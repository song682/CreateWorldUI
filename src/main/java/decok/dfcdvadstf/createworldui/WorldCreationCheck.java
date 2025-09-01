package decok.dfcdvadstf.createworldui.api;

/**
 * @deprecated - Integrated into Main Mod class, then fully controllable in configuration
 * @see decok.dfcdvadstf.createworldui.CreateWorldUI
 * @author dfdvdsf
 * @version 0.0.1.6
 */

@Deprecated
public class WorldCreationCheck {
    private static final boolean FORCE_MODERN_UI = true;

    public static boolean shouldUseModernUI() {
        return FORCE_MODERN_UI;
    }
}