package decok.dfcdvadstf.createworldui.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 *     实验性内容的启用/禁用状态。分为两层：
 * </p>
 * <ol>
 *     <li><b>pending（待写入）</b>：玩家在创建世界界面里显式做出的开关选择，此时还没有 {@code World}，
 *         仅存在于内存；世界加载时由 {@link ExperimentApplier} 落盘。</li>
 *     <li><b>world data（已持久化）</b>：当前激活存档的 {@link ExperimentData}（写入
 *         {@code data/<modid>_experiments.dat}），跨重启保留。</li>
 * </ol>
 * <p>
 *     查询优先级：已持久化的世界状态 &gt; pending 选择 &gt; 实验默认值。
 *     按设计，CreateWorldUI 仍然<b>只记录玩家的选择并暴露查询 API</b>：实验究竟做什么、
 *     是否改变世界生成，都是提供该实验的模组作者自己的责任（作者读取 {@link #isEnabled(String)}
 *     后自行决定行为）。持久化的是"哪些实验被启用"这一事实，而非其效果。
 * </p>
 * <p>
 *     Enabled/disabled state for experiments, kept in two layers: a <b>pending</b> in-memory
 *     map of the player's explicit choices made in the create-world screen (there is no
 *     {@code World} yet at that point), and the active save's persisted
 *     {@link ExperimentData} ({@code data/<modid>_experiments.dat}), which survives restarts.
 *     Query precedence: persisted world state &gt; pending choice &gt; the experiment's default.
 *     By design CreateWorldUI still <b>only records which experiments are enabled and exposes a
 *     query API</b> — what an experiment actually does remains the providing author mod's
 *     responsibility (it reads {@link #isEnabled(String)} and gates its own features).
 *     This design writes a standalone {@code WorldSavedData} file rather than {@code level.dat}
 *     NBT, so removing the mod leaves only an inert orphan file in {@code data/}.
 * </p>
 */
public final class ExperimentState {

    private ExperimentState() {}

    /**
     * 玩家对下一个即将创建的世界显式做出的开关选择（id -> 是否启用）；世界加载并落盘后清空。
     * Player's explicit choices for the world about to be created (id -> enabled); cleared once
     * the pending selection has been committed to the loaded world's {@link ExperimentData}.
     */
    private static final Map<String, Boolean> PENDING = new LinkedHashMap<>();

    /**
     * 当前激活存档的持久化实验状态；无世界加载时为 {@code null}。
     * Persisted experiment state of the currently active world, or {@code null} when no world is
     * loaded (or on the client for a world not yet entered).
     */
    private static ExperimentData activeWorldData = null;

    /**
     * <p>
     *     实验的当前有效状态：已持久化的世界状态优先，其次 pending 选择，最后回退到给定默认值。
     * </p>
     * <p>
     *     Effective enabled state: the persisted world override first, then the pending choice,
     *     otherwise the supplied default (typically {@link IExperimentScreenUsage#defaultEnabled()}).
     * </p>
     */
    public static boolean isEnabled(String id, boolean defaultEnabled) {
        if (activeWorldData != null) {
            Boolean persisted = activeWorldData.getOverride(id);
            if (persisted != null) {
                return persisted;
            }
        }
        Boolean choice = PENDING.get(id);
        return choice != null ? choice : defaultEnabled;
    }

    /**
     * <p>
     *     便捷查询：按 id 找到已注册实验的默认值再判定。未注册的 id 仅当被显式启用时返回 true。
     * </p>
     * <p>
     *     Convenience lookup for author mods: resolves the registered experiment's default
     *     by id, then applies {@link #isEnabled(String, boolean)}. An unregistered id is
     *     enabled only if it was explicitly turned on.
     * </p>
     */
    public static boolean isEnabled(String id) {
        for (IExperimentScreenUsage experiment : IExperimentScreenUsage.registered()) {
            if (id.equals(experiment.getID())) {
                return isEnabled(id, experiment.defaultEnabled());
            }
        }
        return isEnabled(id, false);
    }

    /**
     * <p>
     *     记录玩家的开关选择。创建世界时写入 pending；若世界已加载，则同时立即落盘。
     * </p>
     * <p>
     *     Record the player's toggle. During world creation this updates the pending map; if a
     *     world is already active, the change is persisted into its {@link ExperimentData} too.
     * </p>
     */
    public static void setEnabled(String id, boolean enabled) {
        PENDING.put(id, enabled);
        if (activeWorldData != null) {
            activeWorldData.setEnabled(id, enabled);
        }
    }

    /** Snapshot the pending choices (used to revert on Cancel). / 快照 pending 选择（取消时回滚） */
    public static Map<String, Boolean> snapshot() {
        return new LinkedHashMap<>(PENDING);
    }

    /** Restore a previously captured pending snapshot. / 恢复此前的 pending 快照 */
    public static void restore(Map<String, Boolean> snapshot) {
        PENDING.clear();
        if (snapshot != null) {
            PENDING.putAll(snapshot);
        }
    }

    /** Clear pending choices. / 清空 pending 选择 */
    public static void clear() {
        PENDING.clear();
    }

    /**
     * Read-only view of the pending choices, for {@link ExperimentApplier} to commit at load.
     * pending 选择的只读视图，供 {@link ExperimentApplier} 在世界加载时落盘。
     */
    public static Map<String, Boolean> pendingSnapshot() {
        return java.util.Collections.unmodifiableMap(PENDING);
    }

    /**
     * Bind the loaded world's persisted data so queries consult it, then drop the pending choices
     * (they now live on disk). / 绑定已加载世界的持久化数据并丢弃 pending（已落盘）
     */
    public static void attachWorldData(ExperimentData data) {
        activeWorldData = data;
        PENDING.clear();
    }

    /** Unbind on world unload so queries fall back to defaults. / 世界卸载时解绑 */
    public static void detachWorldData() {
        activeWorldData = null;
    }
}
