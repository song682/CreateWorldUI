package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION, acceptedMinecraftVersions = "1.7.10", acceptableRemoteVersions = "1.7.10")
public class CreateWorldUI {

    public static Config config;
    private static Logger logger = LogManager.getLogger(Tags.NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
    }
}
