package decok.dfcdvadstf.createworldui.api.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 *     外部模组Tab注册中心<br>
 *     提供静态API供外部模组在创建世界界面注册自定义标签页
 * </p>
 * <p>
 *     External mod tab registration hub<br>
 *     Provides static API for external mods to register custom tabs in the create world screen
 * </p>
 *
 */
public final class TabRegistry {

    private static final List<TabEntry> entries = new ArrayList<>();
    private static boolean frozen = false;

    private TabRegistry() {}

    /**
     * <p>
     *     注册一个自定义标签页<br>
     *     应在模组初始化阶段（如 FMLPreInitializationEvent）调用
     * </p>
     * <p>
     *     Register a custom tab<br>
     *     Should be called during mod initialization (e.g. FMLPreInitializationEvent)
     * </p>
     *
     * @param factory    Tab实例工厂 / Tab instance factory
     * @param tabId      标签页唯一ID，建议从103开始避免与内置Tab冲突 / Unique tab ID, recommended to start from 103 to avoid conflicts
     * @param nameKey    本地化键名 / Localization key
     * @param priority   排序优先级，数字越小越靠前 / Sort priority, lower number = earlier position
     */
    public static void registerTab(Supplier<Tab> factory, int tabId, String nameKey, int priority) {
        if (frozen) {
            throw new IllegalStateException(
                "TabRegistry is already frozen. Tabs must be registered before GUI initialization." +
                " / TabRegistry已冻结，必须在GUI初始化之前注册标签页。"
            );
        }

        // Check for duplicate ID
        // 检查ID是否重复
        for (TabEntry entry : entries) {
            if (entry.tabId == tabId) {
                throw new IllegalArgumentException(
                    "Tab ID " + tabId + " is already registered by " + entry.nameKey +
                    " / Tab ID " + tabId + " 已被 " + entry.nameKey + " 注册"
                );
            }
        }

        entries.add(new TabEntry(factory, tabId, nameKey, priority));
    }

    /**
     * <p>
     *     注册一个自定义标签页（使用默认优先级，默认优先级为tabId）<br>
     *     当不指定优先级时，标签页会按照 tabId 从小到大排序<br>
     *     等同于 registerTab(factory, tabId, nameKey, tabId)
     * </p>
     * <p>
     *     Register a custom tab with default priority (defaults to tabId)<br>
     *     When priority is not specified, tabs are sorted by tabId in ascending order
     * </p>
     */
    public static void registerTab(Supplier<Tab> factory, int tabId, String nameKey) {
        registerTab(factory, tabId, nameKey, tabId);
    }

    /**
     * <p>
     *     获取所有已注册的标签页条目（按优先级排序）<br>
     *     返回不可修改的副本
     * </p>
     * <p>
     *     Get all registered tab entries (sorted by priority)<br>
     *     Returns an unmodifiable copy
     * </p>
     */
    public static List<TabEntry> getEntries() {
        List<TabEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((TabEntry e) -> e.priority).thenComparingInt(e -> e.tabId));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * <p>
     *     冻结注册表，阻止后续注册<br>
     *     由TabManager在首次初始化时调用
     * </p>
     * <p>
     *     Freeze the registry to prevent further registration<br>
     *     Called by TabManager on first initialization
     * </p>
     */
    public static void freeze() {
        frozen = true;
    }

    /**
     * <p>
     *     检查注册表是否已冻结<br>
     *     Check if the registry is frozen
     * </p>
     */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * <p>
     *     清空注册表（主要用于测试或内部重置）<br>
     *     Clear the registry (mainly for testing or internal reset)
     * </p>
     */
    public static void clear() {
        entries.clear();
        frozen = false;
    }

    /**
     * <p>
     *     注册条目数据类<br>
     *     Registration entry data class
     * </p>
     */
    public static final class TabEntry {
        public final Supplier<Tab> factory;
        public final int tabId;
        public final String nameKey;
        public final int priority;

        TabEntry(Supplier<Tab> factory, int tabId, String nameKey, int priority) {
            this.factory = factory;
            this.tabId = tabId;
            this.nameKey = nameKey;
            this.priority = priority;
        }
    }
}
