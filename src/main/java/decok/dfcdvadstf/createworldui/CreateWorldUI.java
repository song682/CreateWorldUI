package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import decok.dfcdvadstf.createworldui.api.ExperimentApplier;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyLocker;
import decok.dfcdvadstf.createworldui.command.CommandGameRuleEditor;
import decok.dfcdvadstf.createworldui.config.Config;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

@Mod(
        modid = Tags.MODID,
        name = Tags.NAME,
        version = Tags.VERSION,
        guiFactory = "decok.dfcdvadstf.createworldui.ui.CreateWorldConfigUI",
        useMetadata = true,
        dependencies = "required-after:catframe@[0.10.3,);required-after:catframe_compact@[0.0.8,)",
        customProperties = {
                @Mod.CustomProperty(k = "license", v = "MIT"),
                @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/song682/CreateWorldUI/issues"),
                @Mod.CustomProperty(k = "iconFile", v = "assets/createworldui/logo.png"),
                @Mod.CustomProperty(k = "backgroundFile", v = "assets/catalogue/background.png")
        }
)
public class CreateWorldUI {

    public static Config config;
    private static Logger logger = LogManager.getLogger(Tags.NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MixinBootstrap.init();

        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
        MinecraftForge.EVENT_BUS.register(new IMCRuntimeHandler());
        // Persist/restore experiment selections into each save's WorldSavedData on load/unload.
        MinecraftForge.EVENT_BUS.register(new ExperimentApplier());
    }
    
    @EventHandler
    public void onIMC(IMCEvent event) {
        for (IMCMessage msg : event.getMessages()) {
            if ("difficultylocker".equals(msg.getSender())) {
                switch (msg.key) {
                    case "difficultylocker_config":
                        DifficultyLocker.processIMCConfig(msg.getNBTValue());
                        break;
                    default:
                        logger.warn("Unknown IMC key from difficultylocker: {}", msg.key);
                }
            }
        }
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        // Register the /gameruleEditor command
        // 注册/gameruleEditor命令
        if (config.gameruleEdit || Loader.isModLoaded("difficultylocker")) {
            event.registerServerCommand(new CommandGameRuleEditor());
            logger.info("Registered /gameruleEditor command");
        } else {
            logger.info("Failed to register /gameruleEditor, either config is disabled or mod difficultylocker is not loaded");
        }
    }
}
