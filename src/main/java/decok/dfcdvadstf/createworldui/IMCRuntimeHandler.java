package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;

/**
 * <p>
 *     Polls runtime IMC messages on each client tick.<br>
 *     Registered on the Forge event bus from {@link CreateWorldUI#init}.<br>
 *     轮询运行时 IMC 消息，由 {@link CreateWorldUI#init} 注册到 Forge 事件总线。
 * </p>
 */
public class IMCRuntimeHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        DifficultyLocker.pollRuntimeMessages();
    }
}
