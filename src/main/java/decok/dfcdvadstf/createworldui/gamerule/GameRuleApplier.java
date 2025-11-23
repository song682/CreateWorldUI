package decok.dfcdvadstf.createworldui.gamerule;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于在世界加载时应用保存的游戏规则设置
 */
public class GameRuleApplier {

    private static Map<String, String> pendingGameRules;

    /**
     * 设置要在下一个创建的世界中应用的规则
     */
    public static void setPendingGameRules(Map<String, String> gameRules) {
        if (gameRules == null) {
            pendingGameRules = null;
            return;
        }
        // 确保是可变的 map（防止不可变 map 抛异常）
        pendingGameRules = new HashMap<String, String>(gameRules);
        MinecraftForge.EVENT_BUS.register(new GameRuleApplier());
    }

    /**
     * 获取当前 pendingGameRules（可能为 null）
     */
    public static Map<String, String> getPendingGameRules() {
        return pendingGameRules;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (pendingGameRules != null && !pendingGameRules.isEmpty() && event.world != null) {
            // 只在服务器端主世界应用（你原来的逻辑）
            if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
                applyGameRules(event.world);

                // 应用后清除并取消注册
                pendingGameRules = null;
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
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
