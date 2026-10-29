package decok.dfcdvadstf.createworldui.api.tab;

import decok.dfcdvadstf.catframe.ui.components.tab.Tab;
import net.minecraft.client.gui.GuiCreateWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 *     外部模组Tab注册中心<br>
 *     提供静态API供外部模组在创建世界界面注册自定义标签页
 * </p>
 * <p>
 *     External mod tab registration hub<br>
 *     Provides static API for external mods to register custom tabs in the create world screen
 * </p>
 * <p>
 *     当前架构（CatFrame 组件化 Tab + TabNavigationBar）下的适配说明：<br>
 *     1. Tab 工厂接收宿主 {@code GuiCreateWorld} 屏幕实例（内置 Tab 同样以屏幕为构造参数）；<br>
 *     2. 导航栏不再使用整型按钮 id，排序完全由优先级与注册顺序决定；<br>
 *     3. 内置 Tab 的锚点优先级为 Game=100、World=101、More=102，
 *        外部 Tab 默认优先级 {@value #DEFAULT_PRIORITY}（即排在全部内置 Tab 之后），
 *        与旧版"建议从 103 起"的约定保持一致。
 * </p>
 * <p>
 *     Adaptation notes for the current architecture (CatFrame component-based tabs +
 *     TabNavigationBar):<br>
 *     1. Tab factories receive the host {@code GuiCreateWorld} screen instance, exactly
 *        like the built-in tabs;<br>
 *     2. the navigation bar no longer carries integer button ids — ordering is decided
 *        purely by priority and registration order;<br>
 *     3. built-in tabs hold anchor priorities Game=100, World=101, More=102; external
 *        tabs default to priority {@value #DEFAULT_PRIORITY} (after all built-ins),
 *        preserving the legacy "start from 103" convention.
 * </p>
 */
public final class TabRegistry {

    private static final Logger logger = LogManager.getLogger("CreateWorldUI/TabRegistry");
    private static final List<TabEntry> entries = new ArrayList<>();
    private static boolean frozen = false;

    /**
     * <p>内置 Tab 的锚点优先级基数：第 i 个内置 Tab 的优先级为 BASE + i
     * （Game=100、World=101、More=102）</p>
     * <p>Anchor priority of the built-in tabs: the i-th built-in tab holds
     * BASE + i (Game=100, World=101, More=102)</p>
     */
    public static final int BUILT_IN_PRIORITY_BASE = 100;

    /**
     * <p>外部 Tab 的默认优先级 —— 排在全部内置 Tab 之后，等同旧版推荐的 103</p>
     * <p>Default priority for external tabs — after all built-in tabs, matching
     * the legacy recommended id of 103</p>
     */
    public static final int DEFAULT_PRIORITY = BUILT_IN_PRIORITY_BASE + 3;

    private TabRegistry() {}

    /**
     * <p>
     *     Register a custom tab<br>
     *     Should be called during mod initialization (e.g. FMLPreInitializationEvent)
     * </p>
     *
     * @param factory    Tab instance factory, receives the host create-world screen
     * @param tabId      Unique tab key, "modid:name" format recommended
     * @param priority   Sort priority, lower number = earlier position; ties put the built-in tab first
     */
    public static void registerTab(Function<GuiCreateWorld, Tab> factory, String tabId, int priority) {
        if (frozen) {
            throw new IllegalStateException(
                "TabRegistry is already frozen. Tabs must be registered before GUI initialization."
            );
        }

        // Check for duplicate key (the nav bar no longer has numeric ids to collide on)
        // 检查唯一键是否重复（导航栏已没有可冲突的数字 id）
        for (TabEntry entry : entries) {
            if (entry.tabId.equals(tabId)) {
                throw new IllegalArgumentException(
                    "Tab ID \"" + tabId + "\" is already registered by " + entry.sourceModHint
                );
            }
        }

        entries.add(new TabEntry(factory, tabId, priority));
    }

    /**
     * <p>
     *     Register a custom tab with the default priority (after all built-in tabs)<br>
     *     Equivalent to registerTab(factory, tabId, DEFAULT_PRIORITY)
     * </p>
     */
    public static void registerTab(Function<GuiCreateWorld, Tab> factory, String tabId) {
        registerTab(factory, tabId, DEFAULT_PRIORITY);
    }

    /**
     * <p>
     *     获取所有已注册的标签页条目（按优先级排序，同优先级按注册顺序）<br>
     *     返回不可修改的副本
     * </p>
     * <p>
     *     Get all registered tab entries (sorted by priority, ties keep registration
     *     order) / Returns an unmodifiable copy
     * </p>
     */
    public static List<TabEntry> getEntries() {
        List<TabEntry> sorted = new ArrayList<>(entries);
        // List.sort is stable, so equal priorities keep their insertion order
        // List.sort 是稳定排序，同优先级保持注册顺序
        sorted.sort(Comparator.comparingInt((TabEntry e) -> e.priority));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * <p>
     *     构建最终显示的 Tab 序列：实例化所有外部注册项，并按优先级与内置 Tab 合并。<br>
     *     由宿主屏幕每次 initGui 调用；每次调用都会重新经工厂创建实例，与内置
     *     Tab 每轮重建的生命周期一致。首个外部注册项工厂实例化失败时会被记录日志
     *     并跳过，不会让整个创建世界界面崩溃。
     * </p>
     * <p>
     *     Build the final tab sequence: instantiate every registered external entry and
     *     merge it with the built-in tabs by priority.<br>
     *     Called from the host screen's initGui each pass; factories run per call, so
     *     external tabs share the per-rebuild lifecycle of the built-ins. A failing
     *     external factory is logged and skipped instead of crashing the screen.
     * </p>
     *
     * @param screen      宿主创建世界屏幕 / host create-world screen passed to each factory
     * @param builtInTabs 内置 Tab，按显示顺序传入（优先级为 BASE + index）/ built-in tabs in display order (priority = BASE + index)
     * @return 合并排序后的 Tab 列表 / the merged, priority-sorted tab list
     */
    public static List<Tab> buildTabList(GuiCreateWorld screen, Tab... builtInTabs) {
        // Freeze on first GUI build — registration must happen during mod init
        freeze();

        // Pair each tab with its priority; stable sort keeps built-ins ahead of
        // externals with the same priority (built-ins are inserted first).
        List<PriorityPair> pairs = new ArrayList<>();
        for (int i = 0; i < builtInTabs.length; i++) {
            pairs.add(new PriorityPair(BUILT_IN_PRIORITY_BASE + i, builtInTabs[i]));
        }
        for (TabEntry entry : getEntries()) {
            Tab tab;
            try {
                tab = entry.factory.apply(screen);
            } catch (Throwable t) {
                // A broken external tab must not take down world creation
                // 外部 Tab 构建失败不应连带整个创建世界界面崩溃
                logger.error("Failed to instantiate external tab {}, skipping: {}", entry.tabId, t);
                continue;
            }
            if (tab == null) {
                logger.warn("External tab factory for {} returned null, skipping", entry.tabId);
                continue;
            }
            pairs.add(new PriorityPair(entry.priority, tab));
        }
        pairs.sort(Comparator.comparingInt(pair -> pair.priority));

        List<Tab> ordered = new ArrayList<>(pairs.size());
        for (PriorityPair pair : pairs) {
            ordered.add(pair.tab);
        }
        return ordered;
    }

    /**
     * <p>
     *     冻结注册表，阻止后续注册<br>
     *     由 {@link #buildTabList} 在首次构建时调用
     * </p>
     * <p>
     *     Freeze the registry to prevent further registration<br>
     *     Called by {@link #buildTabList} on the first build
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
        /** Tab实例工厂，接收宿主屏幕 / Tab instance factory, receives the host screen */
        public final Function<GuiCreateWorld, Tab> factory;
        /** 唯一键 / Unique key */
        public final String tabId;
        /** 排序优先级 / Sort priority */
        public final int priority;
        /** 注册来源提示（当前仅注册者自身可查，留作诊断信息）/ Source hint for diagnostics */
        public final String sourceModHint;

        TabEntry(Function<GuiCreateWorld, Tab> factory, String tabId, int priority) {
            this.factory = factory;
            this.tabId = tabId;
            this.priority = priority;
            // Derive the owning mod id from the "modid:name" key when available
            // 尽量从 "modid:name" 形式的键中取出所属模组 id，用于诊断信息
            int colon = tabId.indexOf(':');
            this.sourceModHint = colon > 0 ? tabId.substring(0, colon) : tabId;
        }
    }

    /** Priority-to-tab pair used only by {@link #buildTabList} for the stable merge. */
    private static final class PriorityPair {
        final int priority;
        final Tab tab;

        PriorityPair(int priority, Tab tab) {
            this.priority = priority;
            this.tab = tab;
        }
    }
}
