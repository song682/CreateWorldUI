package decok.dfcdvadstf.createworldui.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {
    ACCESSOR(Side.CLIENT, "middle.access.IGuiCreateWorldAccessor"),

    AUTO_TRANSFER_TO_CREATEWORLD(Side.CLIENT, "middle.MixinGuiSelectWorld"),

    APPLY_DIFFICULTY(Side.CLIENT, "middle.MixinIntegratedServer"),

    MODERN_WORLD_CREATION(Side.CLIENT, "middle.MixinModernCreateWorld");

    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins);
    }

    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
