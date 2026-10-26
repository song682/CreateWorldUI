package decok.dfcdvadstf.createworldui.api;

import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 *     实验性内容启用状态的持久化载体。作为独立的 {@code WorldSavedData} 落盘到存档目录下的
 *     {@code data/<modid>_experiments.dat}，而非写入 {@code level.dat}。
 * </p>
 * <p>
 *     只存储玩家<b>显式改动过</b>的实验（id -> 是否启用）；未出现在文件中的实验在查询时回退到其
 *     {@link IExperimentScreenUsage#defaultEnabled()}。读取时仅遍历当前已注册的实验，因此当提供方
 *     模组被删除后，残留文件对无任何影响的惰性孤儿——存档照常加载。
 * </p>
 * <p>
 *     Persistence carrier for experiment enabled/disabled state, stored as a standalone
 *     {@code WorldSavedData} file at {@code data/<modid>_experiments.dat} rather than inside
 *     {@code level.dat}. Only experiments the player <b>explicitly changed</b> are written
 *     (id -> enabled); unlisted experiments fall back to their
 *     {@link IExperimentScreenUsage#defaultEnabled()} at query time. Reads iterate only the
 *     currently registered experiments, so if the providing mod is removed the leftover file is an
 *     inert orphan that never gets read and does not affect loading the save.
 * </p>
 */
public class ExperimentData extends WorldSavedData {

    /** File name under {@code data/} (becomes {@code <DATA_NAME>.dat}). / 落盘文件名 */
    public static final String DATA_NAME = Tags.MODID + "_experiments";

    /** NBT sub-compound holding the id -> enabled overrides. / 存放覆盖项的子标签组 */
    private static final String TAG_OVERRIDES = "Overrides";

    /** Type id of {@link net.minecraft.nbt.NBTTagByte}, used with {@code hasKey(name, type)}. */
    private static final int TYPE_BYTE = 1;

    private final Map<String, Boolean> overrides = new LinkedHashMap<String, Boolean>();

    /**
     * Required by {@link net.minecraft.world.storage.MapStorage#loadData} which reflects on a
     * single {@code (String)} constructor.
     */
    public ExperimentData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        overrides.clear();
        NBTTagCompound stored = compound.getCompoundTag(TAG_OVERRIDES);
        // Only re-read experiments that are still registered; unknown/orphan keys are ignored.
        for (IExperimentScreenUsage experiment : IExperimentScreenUsage.registered()) {
            String id = experiment.getID();
            if (stored.hasKey(id, TYPE_BYTE)) {
                overrides.put(id, Boolean.valueOf(stored.getBoolean(id)));
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagCompound stored = new NBTTagCompound();
        for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
            stored.setBoolean(entry.getKey(), entry.getValue().booleanValue());
        }
        compound.setTag(TAG_OVERRIDES, stored);
    }

    /**
     * Record an explicit override and mark this data dirty so {@code MapStorage} flushes it.
     * 记录一条显式覆盖并标记为 dirty，交由 {@code MapStorage} 落盘。
     */
    public void setEnabled(String id, boolean enabled) {
        Boolean current = overrides.get(id);
        if (current == null || current.booleanValue() != enabled) {
            overrides.put(id, Boolean.valueOf(enabled));
            markDirty();
        }
    }

    /**
     * The persisted explicit choice for {@code id}, or {@code null} if never changed.
     * {@code id} 已持久化的显式选择；从未改动时返回 {@code null}。
     */
    public Boolean getOverride(String id) {
        return overrides.get(id);
    }
}
