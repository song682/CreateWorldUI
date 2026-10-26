package decok.dfcdvadstf.createworldui.api;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * <p>
 *     实验状态应用器：在世界加载时把创建界面里的 pending 选择落盘到该存档的
 *     {@link ExperimentData}，并把已持久化的数据绑定到 {@link ExperimentState} 供查询；
 *     世界卸载时解绑。常驻注册于 Forge 事件总线（见 {@code CreateWorldUI#init}）。
 * </p>
 * <p>
 *     Experiment state applier: on world load it commits the create-screen <em>pending</em>
 *     choices into the save's {@link ExperimentData} and binds the persisted data to
 *     {@link ExperimentState} for queries; on world unload it unbinds. Registered permanently on
 *     the Forge event bus (see {@code CreateWorldUI#init}). Only the overworld (dimension 0) on the
 *     server side is handled, since {@code mapStorage} is shared cross-dimension and lives in one
 *     {@code data/<modid>_experiments.dat} file.
 * </p>
 */
public class ExperimentApplier {

    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":ExperimentApplier");

    /**
     * <p>
     *     世界加载：读取（或首次创建）该存档的 {@link ExperimentData}，把 pending 选择写入，随后绑定。
     * </p>
     * <p>
     *     World load: read (or lazily create) the save's {@link ExperimentData}, flush the pending
     *     create-screen choices into it, then bind it as the active persisted state.
     * </p>
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.world;
        if (!(world instanceof WorldServer)) return;               // server-side only / 仅服务端
        if (world.provider == null || world.provider.dimensionId != 0) return; // overworld owns the shared file

        MapStorage mapStorage = world.mapStorage;
        ExperimentData data = (ExperimentData) mapStorage.loadData(ExperimentData.class, ExperimentData.DATA_NAME);
        if (data == null) {
            data = new ExperimentData(ExperimentData.DATA_NAME);
            mapStorage.setData(ExperimentData.DATA_NAME, data);
        }

        // Flush the player's create-world choices into the freshly loaded save (pending is empty
        // when simply re-entering an existing save, so this is a no-op then).
        Map<String, Boolean> pending = ExperimentState.pendingSnapshot();
        if (!pending.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : pending.entrySet()) {
                data.setEnabled(entry.getKey(), entry.getValue().booleanValue());
            }
            logger.info("Committed {} experiment selection(s) into the world save.", pending.size());
        }

        ExperimentState.attachWorldData(data);
    }

    /**
     * <p>
     *     世界卸载：解绑持久化状态，使查询回退到默认值，并清理任何残留 pending。
     * </p>
     * <p>
     *     World unload: unbind the persisted state so queries fall back to defaults, and drop any
     *     leftover pending selection.
     * </p>
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.world;
        if (!(world instanceof WorldServer)) return;
        if (world.provider == null || world.provider.dimensionId != 0) return;

        ExperimentState.detachWorldData();
        ExperimentState.clear();
    }
}
