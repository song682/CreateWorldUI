package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION, acceptedMinecraftVersions = "1.7.10", acceptableRemoteVersions = "1.7.10")
public class CreateWorldUI {

    private static Logger logger;

    @Instance(Tags.MODID)
    public static Tags INSTANCE;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

        // 初始化Mixin
        try {
            // 正确的Forge Mixin初始化方式
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.moderncreateworldui.json");
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        } catch (Exception e) {
            logger.error("Mixin initialization failed", e);
        }

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");

        // 注册示例标签页（可选）
        // CreateWorldAPI.registerTab(new ExampleTab());
    }
}
