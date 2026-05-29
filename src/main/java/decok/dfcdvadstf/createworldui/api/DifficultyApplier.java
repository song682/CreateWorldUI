package decok.dfcdvadstf.createworldui.api;

import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.EnumDifficulty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import decok.dfcdvadstf.createworldui.mixin.MixinIntegratedServer;

/**
 * <p>
 *     难度应用器<br>
 *     用于在IntegratedServer.loadAllWorlds完成后应用之前保存的难度设置<br>
 *     配合{@link MixinIntegratedServer}使用，替代原有的反射方案
 * </p>
 * <p>
 *     Difficulty applier<br>
 *     Used to apply previously saved difficulty settings after IntegratedServer.loadAllWorlds completes<br>
 *     Works with {@link MixinIntegratedServer}, replacing the original reflection approach
 * </p>
 */
public class DifficultyApplier {
    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":DifficultyApplier");

    /**
     * <p>
     *     UI 上当前选中的难度（用户在 GameTab 里选的）<br>
     *     Currently selected difficulty in the UI (picked by the user in GameTab)
     * </p>
     */
    private static EnumDifficulty selectedDifficulty = null;

    /**
     * <p>
     *     待应用的难度（世界加载时生效）<br>
     *     Pending difficulty (takes effect when the world loads)
     * </p>
     */
    private static EnumDifficulty pendingDifficulty = null;

    // ===== Selected (UI state) / 选中状态（UI 层） =====

    /**
     * <p>
     *     Get the currently selected difficulty in the create-world UI.<br>
     *     If never set, falls back to {@code mc.gameSettings.difficulty}.
     * </p>
     * <p>
     *     获取创建世界 UI 中当前选中的难度。<br>
     *     如果从未设置，回退到 {@code mc.gameSettings.difficulty}。
     * </p>
     */
    public static EnumDifficulty getSelectedDifficulty() {
        if (selectedDifficulty == null) {
            EnumDifficulty d = net.minecraft.client.Minecraft.getMinecraft().gameSettings.difficulty;
            selectedDifficulty = d != null ? d : EnumDifficulty.NORMAL;
        }
        return selectedDifficulty;
    }

    /**
     * <p>
     *     Set the selected difficulty from the UI.<br>
     *     Also syncs to {@code mc.gameSettings} immediately.
     * </p>
     * <p>
     *     从 UI 设置选中的难度。<br>
     *     同时立即同步到 {@code mc.gameSettings}。
     * </p>
     */
    public static void setSelectedDifficulty(EnumDifficulty difficulty) {
        selectedDifficulty = difficulty;
        net.minecraft.client.Minecraft.getMinecraft().gameSettings.difficulty = difficulty;
        net.minecraft.client.Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    /**
     * <p>
     *     Reset selected difficulty (call when UI closes or world is created).<br>
     *     重置选中难度（UI 关闭或世界创建时调用）。
     * </p>
     */
    public static void resetSelectedDifficulty() {
        selectedDifficulty = null;
    }

    // ===== Pending (world-load state) / 待应用状态（世界加载层） =====

    /**
     * <p>
     *     Set difficulty to be applied in the next created world<br>
     *     设置要在下一个创建的世界中应用的难度
     * </p>
     * @param difficulty The difficulty enum to apply / 要应用的难度枚举
     */
    public static void setPendingDifficulty(EnumDifficulty difficulty) {
        pendingDifficulty = difficulty;
        if (difficulty != null) {
            logger.info("Pending difficulty set to: {}", difficulty);
        }
    }

    /**
     * <p>
     *     Get and clear the pending difficulty (consume pattern)<br>
     *     获取并清除待应用的难度（消费模式）
     * </p>
     * @return The pending difficulty, or null if none / 待应用的难度，或null
     */
    public static EnumDifficulty consumePendingDifficulty() {
        EnumDifficulty diff = pendingDifficulty;
        pendingDifficulty = null;
        return diff;
    }

    /**
     * <p>
     *     Check if there is a pending difficulty<br>
     *     检查是否有待应用的难度
     * </p>
     * @return true if pending difficulty exists / 是否有待应用的难度
     */
    public static boolean hasPendingDifficulty() {
        return pendingDifficulty != null;
    }
}
