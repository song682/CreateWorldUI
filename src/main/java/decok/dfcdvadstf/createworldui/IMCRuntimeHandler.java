package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyLocker;

/**
 * <p>
 *     Polls runtime IMC messages on each client tick.<br>
 *     Registered on the Forge event bus from {@link CreateWorldUI#init}.
 * </p>
 */
public class IMCRuntimeHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        DifficultyLocker.pollRuntimeMessages();
    }
}
