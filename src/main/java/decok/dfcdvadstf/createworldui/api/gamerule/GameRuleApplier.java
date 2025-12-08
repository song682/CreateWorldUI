package decok.dfcdvadstf.createworldui.api.gamerule;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     游戏规则应用器<br>
 *     用于在世界加载时应用之前保存的游戏规则设置
 * </p>
 * <p>
 *     Game rule applier<br>
 *     Used to apply previously saved game rule settings when the world loads
 * </p>
 */
public class GameRuleApplier {
    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":GameRuleApplier");

    /**
     * <p>
     *     待应用的游戏规则映射（世界加载时生效）<br>
     *     Pending game rule map (takes effect when the world loads)
     * </p>
     */
    private static Map<String, String> pendingGameRules = null;
    /**
     * <p>
     *     是否已注册事件监听器<br>
     *     Whether event listener is registered
     * </p>
     */
    private static boolean registered = false;

    /**
     * <p>
     *     Set rules to be applied in the next created world<br>
     *     设置要在下一个创建的世界中应用的规则
     * </p>
     * @param gameRules Game rule map (key: rule name, value: rule value in string form)<br>游戏规则映射（键：规则名，值：字符串形式的规则值）
     */
    public static void setPendingGameRules(Map<String, String> gameRules) {
        if (gameRules == null) {
            pendingGameRules = null;
            return;
        }

        pendingGameRules = new HashMap<>(gameRules);

        // 只注册一次事件监听器
        // Register event listener only once
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(new GameRuleApplier());
            registered = true;
        }
    }

    /**
     * <p>
     *     世界加载完成后应用规则<br>
     *     仅对服务端世界生效
     * </p>
     * <p>
     *     Apply rules after world loading is complete<br>
     *     Only effective for server-side worlds
     * </p>
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!(event.world instanceof WorldServer)) return; // 仅处理服务端世界 / Only handle server-side worlds

        if (pendingGameRules != null && !pendingGameRules.isEmpty()) {

            applyGameRules(event.world);   // 通过监控器应用规则 / Apply rules via monitor

            int appliedCount = pendingGameRules.size();

            // 清理待应用规则
            // Clear pending game rules
            pendingGameRules.clear();

            // 取消事件注册
            // Unregister event
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;
            logger.info("Applied {} game rules while creating the world.", appliedCount);
        }
    }

    /**
     *     获取当前待应用的游戏规则（可能为null）<br>
     *     Get currently pending game rules (maybe null)
     * @return 待应用的游戏规则映射 / Pending game rule map
     */
    public static Map<String, String> getPendingGameRules() {
        return pendingGameRules;
    }

    /**
     * 应用游戏规则到世界<br>
     * Apply game rules to the world
     * @param world 目标世界 / Target world
     */
    private void applyGameRules(World world) {
        if (pendingGameRules == null || pendingGameRules.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : pendingGameRules.entrySet()) {
            GameRuleMonitorNSetter.setGamerule(world, entry.getKey(), entry.getValue());
        }
    }
}
