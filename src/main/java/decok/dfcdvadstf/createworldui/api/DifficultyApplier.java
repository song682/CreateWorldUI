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
     *     待应用的难度（世界加载时生效）<br>
     *     Pending difficulty (takes effect when the world loads)
     * </p>
     */
    private static EnumDifficulty pendingDifficulty = null;

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
