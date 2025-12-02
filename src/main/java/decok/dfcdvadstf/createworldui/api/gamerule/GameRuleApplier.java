package decok.dfcdvadstf.createworldui.api.gamerule;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于在世界加载时应用保存的游戏规则设置
 */
public class GameRuleApplier {
    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":GameRuleApplier");

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

            // 先记录数量，避免清理后 NPE
            int appliedCount = pendingGameRules.size();

            applyGameRules(event.world);   // 保留你当前逻辑，不动

            // 再用 Vanilla API 写一次（你当前逻辑）
            for (Map.Entry<String, String> e : pendingGameRules.entrySet()) {
                rules.setOrCreateGameRule(e.getKey(), e.getValue());
            }

            // 这里删除 clear() —— 直接赋 null 即可。
            pendingGameRules = null;

            // 取消注册（你原本逻辑）
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;

            // 使用已保存的 appliedCount 避免 NPE
            logger.info("Applied {} pending game rules.", appliedCount);
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
    }
}
