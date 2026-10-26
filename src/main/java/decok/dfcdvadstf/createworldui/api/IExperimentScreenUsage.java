package decok.dfcdvadstf.createworldui.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     实验性内容契约 —— 由其他模组作者实现，把自己的实验性内容暴露给 CreateWorldUI 的实验界面。<br>
 *     本接口直接对齐 GTNH Mixins 的
 *     {@code com.gtnewhorizon.gtnhmixins.builders.IMixins} 惯用法：<b>枚举常量就是注册单元</b>，
 *     作者把 {@link IExperimentScreenUsage} 实现到一个枚举上，枚举的每个常量即一个开关；
 *     {@link #getExperimentalThings(Class)} 经 {@link Class#getEnumConstants()} 展开常量，
 *     正如 {@code IMixins.getMixins(Class)} 展开 mixin 构建器。
 * </p>
 * <p>
 *     Contract for experimental content, implemented by other mod authors to expose their
 *     experiments to the CreateWorldUI experiment screen. Modelled directly after GTNH
 *     Mixins' {@code IMixins}: the enum constant is the registration unit — an author
 *     implements this interface on an enum where every constant is one toggle, and
 *     {@link #getExperimentalThings(Class)} expands the constants via
 *     {@link Class#getEnumConstants()}, mirroring {@code IMixins.getMixins(Class)}.
 * </p>
 * <p>Example / 示例：</p>
 * <pre>{@code
 * public enum MyModExperiments implements IExperimentScreenUsage {
 *     NEW_VILLAGER_TRADING("mymod:new_villager_trading",
 *         "New Villager Trading", "Experimental trading rework.", false);
 *
 *     private final String id, title, description;
 *     private final boolean defaultEnabled;
 *     MyModExperiments(String id, String title, String description, boolean def) {
 *         this.id = id; this.title = title; this.description = description; this.defaultEnabled = def;
 *     }
 *     public String getID() { return id; }
 *     public String getTitle() { return title; }
 *     public String getDescription() { return description; }
 *     public boolean defaultEnabled() { return defaultEnabled; }
 * }
 * // then, during the author's own init (explicit registration):
 * IExperimentScreenUsage.register(MyModExperiments.class);
 * }</pre>
 */
public interface IExperimentScreenUsage {

    /** Globally unique, mod-namespaced identifier (e.g. {@code "mymod:new_feature"}). */
    String getID();

    /** Human-readable display title shown on the toggle row. */
    String getTitle();

    /** Description surfaced as the row tooltip. */
    String getDescription();

    /**
     * Whether the experiment is enabled when the player has not chosen yet.
     * <p>玩家尚未做出选择时的默认启用状态。</p>
     */
    boolean defaultEnabled();

    /**
     * <p>
     *     把作者的实验枚举展开成其常量，是 {@code IMixins.getMixins(Class)} 的等价物：
     *     注册由枚举常量声明，经 {@link Class#getEnumConstants()} 发现。
     * </p>
     * <p>
     *     Expand an author's experiment enum into its constants — the analogue of
     *     {@code IMixins.getMixins(Class)}: registration is declared by enum constants
     *     and discovered through {@link Class#getEnumConstants()}.
     * </p>
     */
    static <T extends Enum<T> & IExperimentScreenUsage> List<IExperimentScreenUsage> getExperimentalThings(Class<T> experimentsEnum) {
        return new ArrayList<IExperimentScreenUsage>(Arrays.asList(experimentsEnum.getEnumConstants()));
    }

    /**
     * <p>
     *     显式注册入口：作者模组在自己的初始化阶段调用，把实验枚举交给本接口。
     *     注册器就是接口本身（无需额外的注册器类型）。
     * </p>
     * <p>
     *     Explicit registration entry point: the author mod calls this during its own
     *     initialisation and hands over its experiment enum. The interface itself is the
     *     registrar (no separate registry type is required).
     * </p>
     */
    static <T extends Enum<T> & IExperimentScreenUsage> void register(Class<T> experimentsEnum) {
        Store.REGISTERED.addAll(getExperimentalThings(experimentsEnum));
    }

    /**
     * @return every registered experiment, in registration order / 全部已注册实验（按注册顺序）
     */
    static List<IExperimentScreenUsage> registered() {
        return Collections.unmodifiableList(Store.REGISTERED);
    }

    /**
     * <p>
     *     注册列表的后备存储。接口无法直接持有可变状态，故列表放在本嵌套持有类中，
     *     仅通过上方静态方法访问（与 IMixins 把发现逻辑放在接口上的做法一致）。
     * </p>
     * <p>
     *     Backing store for the registration facade. Interfaces cannot hold mutable state
     *     directly, so the list lives in this nested holder and is reachable only through
     *     the static methods above.
     * </p>
     */
    final class Store {
        private Store() {}

        private static final List<IExperimentScreenUsage> REGISTERED = new ArrayList<IExperimentScreenUsage>();
    }
}
