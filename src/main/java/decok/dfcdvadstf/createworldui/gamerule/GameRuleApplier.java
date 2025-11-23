package decok.dfcdvadstf.createworldui.gamerule;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于在世界加载时应用保存的游戏规则设置
 */
public class GameRuleApplier {

    private static Map<String, String> pendingGameRules = null;
    private static boolean registered = false;

    /**
     * 设置要在下一个创建的世界中应用的规则
     */
    public static void setPendingGameRules(Map<String, String> gameRules) {
        if (gameRules == null) {
            pendingGameRules = null;
            return;
        }

        pendingGameRules = new HashMap<>(gameRules);

        // 只注册一次
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(new GameRuleApplier());
            registered = true;
        }
    }

    /**
     * 世界加载完成后应用规则
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!(event.world instanceof WorldServer)) return;

        if (pendingGameRules != null && !pendingGameRules.isEmpty()) {
            GameRules rules = event.world.getGameRules();

            for (Map.Entry<String, String> e : pendingGameRules.entrySet()) {
                rules.setOrCreateGameRule(e.getKey(), e.getValue());
            }

            // 用完立即清理
            pendingGameRules.clear();
            pendingGameRules = null;

            // 注销监听器（避免重复触发）
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;

            System.out.println("[GameRuleApplier] Applied pending game rules.");
        }
    }

    /**
     * 获取当前 pendingGameRules（可能为 null）
     */
    public static Map<String, String> getPendingGameRules() {
        return pendingGameRules;
    }


    private void applyGameRules(World world) {
        for (Map.Entry<String, String> entry : pendingGameRules.entrySet()) {
            GameRuleMonitorNSetter.setGamerule(world, entry.getKey(), entry.getValue());
        }

        // 记录应用的游戏规则数量
        org.apache.logging.log4j.LogManager.getLogger("GameRuleApplier")
                .info("Applied {} game rules while creating the world.", pendingGameRules.size());
    }
}
