package decok.dfcdvadstf.createworldui.mixin.access;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * <p>Accessor mixin interface exposing the private fields of vanilla {@code GuiCreateWorld}.</p>
 * <p>Well, vanilla hides everything behind {@code private} — so we use Mixin's {@link Accessor}
 * to auto-generate getters/setters at runtime. External mods (or our own non-mixin code) can
 * just cast the screen instance to {@link IGuiCreateWorldAccess} and read/write state directly —
 * no need to write another mixin or reflect into private fields.</p>
 *
 * <p>暴露原版 {@code GuiCreateWorld} 私有字段的 Accessor mixin 接口。</p>
 * <p>嗯，原版把所有字段都 {@code private} 掉了——所以我们用 Mixin 的 {@link Accessor}
 * 在运行时自动生成 getter/setter。外部模组（或者我们自己的非 mixin 代码）只要把屏幕实例
 * 强转成 {@link IGuiCreateWorldAccess} 就能直接读写状态——不用再写一个 mixin，
 * 也不用反射去戳私有字段。</p>
 *
 * <h3>Why {@code @Mixin} on the interface? / 为什么接口上要加 {@code @Mixin}？</h3>
 * <p>Because Mixin only processes {@link Accessor @Accessor} methods on types it actually
 * recognizes as mixins. A plain interface without {@code @Mixin} — even if it's implemented
 * by another mixin class — won't get accessor bodies generated, and calls would explode
 * with {@code AbstractMethodError} at runtime. The interface also needs to be listed in
 * {@code mixins.createworldui.json} so the config loader feeds it to the Mixin engine.</p>
 * <p>因为 Mixin 只会处理它认得的 mixin 类型上的 {@link Accessor @Accessor} 方法。一个
 * 没加 {@code @Mixin} 的普通接口——即使被另一个 mixin 类 implements 了——也不会被生成
 * accessor 方法体，调用时运行时会直接炸 {@code AbstractMethodError}。
 * 接口还需要被列在 {@code mixins.createworldui.json} 里，配置加载器才会把它送进 Mixin 引擎。</p>
 *
 * <h3>Why under {@code mixin.*} package? / 为什么放在 {@code mixin.*} 包下？</h3>
 * <p>Because Mixin's {@code package} config entry "seals" that package and all its subpackages —
 * classes inside become mixin templates and cannot be referenced directly by game code.
 * So this interface has to live inside the mixin package tree, not under {@code api/}.
 * External code still accesses it just fine via import + cast — the sealing only blocks
 * {@code Class.forName}-style direct loads from outside the mixin pipeline.</p>
 * <p>因为 Mixin 配置里的 {@code package} 字段会"封印"那个包及其所有子包——里面的类被当成
 * mixin 模板，无法被游戏代码直接引用。所以这个接口必须住在 mixin 包树下，不能放在 {@code api/} 里。
 * 外部代码通过 import + 强转访问它没问题——"封印"只挡从 mixin 流水线外走 {@code Class.forName}
 * 之类的直接加载。</p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * GuiCreateWorld gui = ...;
 * IGuiCreateWorldAccess access = (IGuiCreateWorldAccess)(Object) gui;
 * String name = access.createWorldUI$getWorldName();
 * access.createWorldUI$setWorldName("My Shiny New World");
 * }</pre>
 *
 * <h3>Notes / 注意</h3>
 * <ul>
 *   <li>Field names are MCP/SRG-style ({@code field_146330_J} etc.) because this is
 *       Minecraft 1.7.10. Mixin's refmap handles the remap automatically.</li>
 *   <li>All accessor methods carry the {@code createWorldUI$} prefix to avoid name clashes
 *       with other mods that might define their own accessor interfaces.</li>
 *   <li>字段名是 MCP/SRG 风格（{@code field_146330_J} 这种）——因为这是 1.7.10，Mixin
 *       的 refmap 会自动处理重映射。</li>
 *   <li>所有 accessor 方法都带 {@code createWorldUI$} 前缀，避免跟其他模组自定义的接口撞名。</li>
 * </ul>
 */
@Mixin(GuiCreateWorld.class)
public interface IGuiCreateWorldAccess {

    // === World name / 世界名称 ===
    @Accessor("field_146330_J") String createWorldUI$getWorldName();
    @Accessor("field_146330_J") void   createWorldUI$setWorldName(String value);

    // === Game mode / 游戏模式 ===
    @Accessor("field_146342_r") String createWorldUI$getGameMode();
    @Accessor("field_146342_r") void   createWorldUI$setGameMode(String value);

    // === Seed / 种子 ===
    @Accessor("field_146329_I") String createWorldUI$getSeed();
    @Accessor("field_146329_I") void   createWorldUI$setSeed(String value);

    // === World type index / 世界类型索引 ===
    @Accessor("field_146331_K") int  createWorldUI$getWorldTypeIndex();
    @Accessor("field_146331_K") void createWorldUI$setWorldTypeIndex(int value);

    // === Generate structures / 生成建筑 ===
    @Accessor("field_146341_s") boolean createWorldUI$getGenerateStructures();
    @Accessor("field_146341_s") void    createWorldUI$setGenerateStructures(boolean value);

    // === Bonus chest / 奖励箱 ===
    @Accessor("field_146338_v") boolean createWorldUI$getBonusChest();
    @Accessor("field_146338_v") void    createWorldUI$setBonusChest(boolean value);

    // === Allow cheats / 允许作弊 ===
    @Accessor("field_146340_t") boolean createWorldUI$getAllowCheats();
    @Accessor("field_146340_t") void    createWorldUI$setAllowCheats(boolean value);

    // === Hardcore / 硬核模式 ===
    @Accessor("field_146337_w") boolean createWorldUI$getHardcore();
    @Accessor("field_146337_w") void    createWorldUI$setHardcore(boolean value);

    // === Parent screen (read-only) / 父界面（只读） ===
    @Accessor("field_146332_f") GuiScreen createWorldUI$getParentScreen();
}
