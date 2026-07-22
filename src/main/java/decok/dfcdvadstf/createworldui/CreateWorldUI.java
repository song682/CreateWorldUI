package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import decok.dfcdvadstf.catframe.compact.forge.language.LanguageRegister;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.tab.TabRegistry;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;
import decok.dfcdvadstf.createworldui.command.CommandGameRuleEditor;
import decok.dfcdvadstf.createworldui.config.Config;
import decok.dfcdvadstf.createworldui.tab.CreateWorldUITabBar;
import decok.dfcdvadstf.createworldui.tab.GameTab;
import decok.dfcdvadstf.createworldui.tab.MoreTab;
import decok.dfcdvadstf.createworldui.tab.WorldTab;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MODID,
        name = Tags.NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "1.7.10",
        acceptableRemoteVersions = "1.7.10",
        guiFactory = "decok.dfcdvadstf.createworldui.config.CreateWorldConfigUI",
        useMetadata = true,
        dependencies = "required-after:dfdvdsfsAPI",
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
        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");
        LanguageRegister.domain(Tags.MODID, "assets/createworldui/lang");

        // Register built-in tabs to the CreateWorldUI bar
        // 注册内置标签页到 CreateWorldUI 的 bar 下
        TabRegistry.registerTab(CreateWorldUITabBar.BAR_ID, GameTab::new, 100, Text.translatable("createworldui.tab.game"));
        TabRegistry.registerTab(CreateWorldUITabBar.BAR_ID, WorldTab::new, 101, Text.translatable("createworldui.tab.world"));
        TabRegistry.registerTab(CreateWorldUITabBar.BAR_ID, MoreTab::new, 102, Text.translatable("createworldui.tab.more"));
        logger.info("Registered built-in tabs: Game, World, More");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
        MinecraftForge.EVENT_BUS.register(new IMCRuntimeHandler());
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
        event.registerServerCommand(new CommandGameRuleEditor());
        logger.info("Registered /gameruleEditor command");
    }
}
