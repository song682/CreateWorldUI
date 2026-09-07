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
 * <p>暴露原版 {@link GuiCreateWorld} 私有字段的 Accessor mixin 接口。</p>
 * <p>嗯，原版把所有字段都 {@code private} 掉了——所以我们用 Mixin 的 {@link Accessor}
 * 在运行时自动生成 getter/setter。外部模组（或者我们自己的非 mixin 代码）只要把屏幕实例
 * 强转成 {@link IGuiCreateWorldAccess} 就能直接读写状态——不用再写一个 mixin，
 * 也不用反射去戳私有字段。</p>
 */
@Mixin(GuiCreateWorld.class)
public interface IGuiCreateWorldAccess {

    // === World name / 世界名称 ===
    @Accessor("field_146330_J") String modernWorldCreatingUI$getWorldName();
    @Accessor("field_146330_J") void   modernWorldCreatingUI$setWorldName(String value);

    // === Game mode / 游戏模式 ===
    @Accessor("field_146342_r") String modernWorldCreatingUI$getGameMode();
    @Accessor("field_146342_r") void   modernWorldCreatingUI$setGameMode(String value);

    // === Seed / 种子 ===
    @Accessor("field_146329_I") String modernWorldCreatingUI$getSeed();
    @Accessor("field_146329_I") void   modernWorldCreatingUI$setSeed(String value);

    // === World type index / 世界类型索引 ===
    @Accessor("field_146331_K") int  modernWorldCreatingUI$getWorldTypeIndex();
    @Accessor("field_146331_K") void modernWorldCreatingUI$setWorldTypeIndex(int value);

    // === Generate structures / 生成建筑 ===
    @Accessor("field_146341_s") boolean modernWorldCreatingUI$getGenerateStructures();
    @Accessor("field_146341_s") void    modernWorldCreatingUI$setGenerateStructures(boolean value);

    // === Bonus chest / 奖励箱 ===
    @Accessor("field_146338_v") boolean modernWorldCreatingUI$getBonusChest();
    @Accessor("field_146338_v") void    modernWorldCreatingUI$setBonusChest(boolean value);

    // === Allow cheats / 允许作弊 ===
    @Accessor("field_146340_t") boolean modernWorldCreatingUI$getAllowCheats();
    @Accessor("field_146340_t") void    modernWorldCreatingUI$setAllowCheats(boolean value);

    // === Hardcore / 硬核模式 ===
    @Accessor("field_146337_w") boolean modernWorldCreatingUI$getHardcore();
    @Accessor("field_146337_w") void    modernWorldCreatingUI$setHardcore(boolean value);

    // === Parent screen (read-only) / 父界面（只读） ===
    @Accessor("field_146332_f") GuiScreen modernWorldCreatingUI$getParentScreen();
}
