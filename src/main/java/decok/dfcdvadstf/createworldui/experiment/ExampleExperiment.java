package decok.dfcdvadstf.createworldui.experiment;

import decok.dfcdvadstf.createworldui.api.IExperimentScreenUsage;

/**
 * <p>
 *     示例实验 —— 演示其他模组作者如何通过 {@link IExperimentScreenUsage} 注册实验性内容：
 *     枚举常量即注册单元（IMixins 惯用法），每个常量对应实验界面上的一个开关。
 *     本实验<b>不做任何实际事情</b>，仅用于展示注册流程与界面效果；
 *     由配置项 {@code enableExampleExperiment} 门控注册（见 {@code CreateWorldUI#init}）。
 * </p>
 * <p>
 *     Example experiment — demonstrates how an author mod registers experimental content
 *     through {@link IExperimentScreenUsage}: the enum constant is the registration unit
 *     (the IMixins idiom) and each constant becomes one toggle on the experiment screen.
 *     This experiment <b>does nothing at all</b>; it exists only to showcase the registration
 *     flow and the screen. Its registration is gated by the {@code enableExampleExperiment}
 *     config option (see {@code CreateWorldUI#init}).
 * </p>
 */
public enum ExampleExperiment implements IExperimentScreenUsage {

    /** A no-op toggle that only demonstrates the API. / 仅演示 API 的空操作开关 */
    EXAMPLE_TOGGLE("createworldui:example_toggle",
            "Example Experiment",
            "A demo toggle registered by CreateWorldUI itself. It does nothing — its only purpose is to show how author mods expose experiments through IExperimentScreenUsage.",
            false);

    private final String id;
    private final String title;
    private final String description;
    private final boolean defaultEnabled;

    ExampleExperiment(String id, String title, String description, boolean defaultEnabled) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.defaultEnabled = defaultEnabled;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean defaultEnabled() {
        return this.defaultEnabled;
    }
}
