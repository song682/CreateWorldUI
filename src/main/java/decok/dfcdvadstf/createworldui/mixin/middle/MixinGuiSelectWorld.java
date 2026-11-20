package decok.dfcdvadstf.createworldui.mixin.middle;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.world.storage.ISaveFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <p>Modifies the vanilla select world screen via Mixin to redirect directly to
 * the create world screen when no saves are detected.</p>
 */
@Mixin(GuiSelectWorld.class)
public class MixinGuiSelectWorld extends GuiScreen {

    @Shadow
    protected GuiScreen field_146632_a;

    @Unique
    private static final Logger modernWorldCreatingUI$logger = LogManager.getLogger("MixinGuiSelectWorld");

    /**
     * Checks for saves on GUI init; if none exist, redirects to the Create World screen.
     */
    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void onInitGuiHead(CallbackInfo ci) {
        modernWorldCreatingUI$logger.info("Initializing GuiSelectWorld");

        // Check whether any saves exist
        if (modernWorldCreatingUI$hasNoSaves()) {
            modernWorldCreatingUI$logger.info("No saves detected, redirecting to Create World screen");

            // Redirect to Create World screen and cancel vanilla initGui
            Minecraft mcInstance = FMLClientHandler.instance().getClient();
            mcInstance.displayGuiScreen(new GuiCreateWorld(field_146632_a)); // 使用原版的父界面引用
            ci.cancel();
        }
    }

    /**
     * Checks whether there are no saves.
     * 检测是否有存档
     */
    @Unique
    private boolean modernWorldCreatingUI$hasNoSaves() {
        try {
            // Get the saves list
            Minecraft mcInstance = FMLClientHandler.instance().getClient();
            ISaveFormat saveFormat = mcInstance.getSaveLoader();
            List<?> saveList = saveFormat.getSaveList();

            if (saveList == null) {
                modernWorldCreatingUI$logger.warn("Could not get save list");
                return true; // Cannot get save list, treat as no saves
            }

            modernWorldCreatingUI$logger.info("Found {} save entries", saveList.size());

            return saveList.isEmpty();
        } catch (Exception e) {
            modernWorldCreatingUI$logger.error("Error checking for saves: ", e);
            return true; // On error, treat as no saves to ensure user can create a new world
        }
    }
}
