package decok.dfcdvadstf.createworldui.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 *     实验性内容的会话级启用/禁用状态。<br>
 *     按设计，CreateWorldUI <b>只记录玩家的选择并暴露查询 API</b>：实验究竟做什么、
 *     是否落盘到存档，都是提供该实验的模组作者自己的责任（作者读取 {@link #isEnabled(String)}
 *     后自行决定行为并处理持久化）。因此本类<b>不写入 level.dat NBT</b>。
 * </p>
 * <p>
 *     Session-scoped enabled/disabled state for experiments toggled in the experiment
 *     screen. By design CreateWorldUI <b>only records the player's choice and exposes a
 *     query API</b>: what an experiment actually does — and whether it is persisted into
 *     the save — is the providing author mod's responsibility (it reads
 *     {@link #isEnabled(String)} and gates/persists its own features). No {@code level.dat}
 *     NBT is written here.
 * </p>
 */
public final class ExperimentState {

    private ExperimentState() {}

    /**
     * 玩家对下一个即将创建的世界显式做出的开关选择（id -> 是否启用）。
     * Player's explicit choices for the world about to be created (id -> enabled).
     */
    private static final Map<String, Boolean> SELECTED = new LinkedHashMap<String, Boolean>();

    /**
     * <p>
     *     实验的当前有效状态：有玩家显式选择则用之，否则回退到给定默认值。
     * </p>
     * <p>
     *     Effective enabled state: the player's explicit choice if present, otherwise the
     *     supplied default (typically {@link IExperimentScreenUsage#defaultEnabled()}).
     * </p>
     */
    public static boolean isEnabled(String id, boolean defaultEnabled) {
        Boolean choice = SELECTED.get(id);
        return choice != null ? choice.booleanValue() : defaultEnabled;
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
        Boolean choice = SELECTED.get(id);
        return choice != null && choice.booleanValue();
    }

    /** Record the player's toggle for an experiment. / 记录玩家的开关选择 */
    public static void setEnabled(String id, boolean enabled) {
        SELECTED.put(id, Boolean.valueOf(enabled));
    }

    /** Snapshot the current choices (used to revert on Cancel). / 快照当前选择（取消时回滚） */
    public static Map<String, Boolean> snapshot() {
        return new LinkedHashMap<String, Boolean>(SELECTED);
    }

    /** Restore a previously captured snapshot. / 恢复此前的快照 */
    public static void restore(Map<String, Boolean> snapshot) {
        SELECTED.clear();
        if (snapshot != null) {
            SELECTED.putAll(snapshot);
        }
    }

    /** Clear all recorded choices. / 清空所有已记录的选择 */
    public static void clear() {
        SELECTED.clear();
    }
}
