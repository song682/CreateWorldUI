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
     */
    @Inject(
            method = "loadAllWorlds",
            at = @At("TAIL")
    )
    private void createWorldUI$onLoadAllWorlds(String saveName, String worldNameIn, long seed,
                                                WorldType type, String generatorOptions, CallbackInfo ci) {
        // Check if there is a pending difficulty to apply
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
        for (int i = 0; i < server.worldServers.length; ++i) {
            WorldServer worldServer = server.worldServers[i];
            if (worldServer != null) {
                worldServer.difficultySetting = difficulty;
            }
        }

        // Update client game settings
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
        if (DifficultyLocker.isLoaded() && DifficultyLocker.hasLockedDifficulty()) {
            EnumDifficulty lockedDiff = DifficultyLocker.getLockedDifficulty();
            if (lockedDiff != null) {
                DifficultyLocker.applyLockedDifficultyToWorldData(lockedDiff);

                // IMC: notify difficultylocker the world was created, carrying the
                // screen-side lock state
                DifficultyLocker.notifyWorldCreated(worldNameIn, lockedDiff, true);

                DifficultyLocker.resetAllLocks(); // Reset after applying
                createWorldUI$logger.info("Locked difficulty '{}' also applied to WorldDifficultyData", lockedDiff.getDifficultyId());
            }
        }
    }
}
