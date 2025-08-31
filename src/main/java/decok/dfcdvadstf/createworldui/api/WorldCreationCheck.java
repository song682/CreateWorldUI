package decok.dfcdvadstf.createworldui.api;

public class WorldCreationCheck {

    // 强制使用现代UI（调试模式）
    private static final boolean FORCE_MODERN_UI = true;

    public static boolean shouldUseModernUI() {
        // 调试模式下总是返回true
        if (FORCE_MODERN_UI) {
            return true;
        }

        // 这里可以添加其他条件判断逻辑
        // 例如检查配置文件或特定条件
        return false;
    }
}