package decok.dfcdvadstf.createworldui.mixin.middle;

import cpw.mods.fml.client.FMLClientHandler;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyApplier;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyLocker;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <p>
 *     Mixin IntegratedServer to apply pending difficulty after world loading.<br>
 *     This replaces the unreliable reflection approach in MixinModernCreateWorld (lines 441-520).<br>
 *     After loadAllWorlds completes, worldServers is fully initialized and can be accessed directly.
 * </p>
 * <p>
 *     对IntegratedServer的Mixin，在世界加载完成后应用待定的难度设置。<br>
 *     替代MixinModernCreateWorld（441-520行）中不可靠的反射方案。<br>
 *     loadAllWorlds完成后，worldServers已完全初始化，可以直接访问，无需反射。
 * </p>
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {

    private static final Logger createWorldUI$logger = LogManager.getLogger("MixinIntegratedServer");

    /**
     * <p>
     *     Inject at the tail of loadAllWorlds to apply pending difficulty.<br>
     *     At this point worldServers is fully initialized, so we can directly<br>
     *     set difficultySetting on each WorldServer without any reflection.
     * </p>
     * <p>
     *     在loadAllWorlds末尾注入以应用待定的难度设置。<br>
     *     此时worldServers已完全初始化，可以直接设置每个WorldServer的<br>
     *     difficultySetting字段，无需任何反射。
     * </p>
     */
    @Inject(
            method = "loadAllWorlds",
            at = @At("TAIL")
    )
    private void createWorldUI$onLoadAllWorlds(String saveName, String worldNameIn, long seed,
                                                WorldType type, String generatorOptions, CallbackInfo ci) {
        // Check if there is a pending difficulty to apply
        // 检查是否有待应用的难度
        if (!DifficultyApplier.hasPendingDifficulty()) {
            return;
        }

        EnumDifficulty difficulty = DifficultyApplier.consumePendingDifficulty();
        if (difficulty == null) {
            return;
        }

        createWorldUI$logger.info("Applying pending difficulty '{}' to world", difficulty);

        IntegratedServer server = (IntegratedServer) (Object) this;

        // Apply difficulty to all world dimensions (no reflection needed)
        // 应用难度到所有世界维度（无需反射）
        for (int i = 0; i < server.worldServers.length; ++i) {
            WorldServer worldServer = server.worldServers[i];
            if (worldServer != null) {
                worldServer.difficultySetting = difficulty;
            }
        }

        // Update client game settings
        // 更新客户端游戏设置
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc != null && mc.gameSettings != null) {
            mc.gameSettings.difficulty = difficulty;
            mc.gameSettings.saveOptions();
        }
        
        // Also apply to WorldDifficultyData if ModernDifficultyLocker is loaded:
        // a direct write into its in-memory state, plus the world_created IMC as
        // the authoritative sync — the other mod's own loadAllWorlds-TAIL mixin
        // may reload (reset) WorldDifficultyData after our direct write, and its
        // once-a-second poller re-applies the IMC, guaranteeing consistency.
        // 如果ModernDifficultyLocker已加载，也同步到WorldDifficultyData：
        // 直达写入其内存状态，并以 world_created IMC 做权威同步 —— 对方自己的
        // loadAllWorlds-TAIL mixin 可能在本侧直达写入之后重载（重置）
        // WorldDifficultyData，其每秒轮询会把 IMC 补写回去，保证最终一致。
        if (DifficultyLocker.isLoaded() && DifficultyLocker.hasLockedDifficulty()) {
            EnumDifficulty lockedDiff = DifficultyLocker.getLockedDifficulty();
            if (lockedDiff != null) {
                DifficultyLocker.applyLockedDifficultyToWorldData(lockedDiff);

                // IMC: 通知 difficultylocker 世界已创建，携带 UI 侧的锁定状态
                // IMC: notify difficultylocker the world was created, carrying the
                // screen-side lock state
                DifficultyLocker.notifyWorldCreated(worldNameIn, lockedDiff, true);

                DifficultyLocker.resetAllLocks(); // Reset after applying
                createWorldUI$logger.info("Locked difficulty '{}' also applied to WorldDifficultyData", lockedDiff.getDifficultyId());
            }
        }
    }
}
