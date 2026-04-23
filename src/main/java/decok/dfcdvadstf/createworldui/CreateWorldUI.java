package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import decok.dfcdvadstf.createworldui.api.tab.TabRegistry;
import decok.dfcdvadstf.createworldui.command.CommandGameRuleEditor;
import decok.dfcdvadstf.createworldui.config.Config;
import decok.dfcdvadstf.createworldui.tab.GameTab;
import decok.dfcdvadstf.createworldui.tab.MoreTab;
import decok.dfcdvadstf.createworldui.tab.WorldTab;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION, acceptedMinecraftVersions = "1.7.10", acceptableRemoteVersions = "1.7.10", guiFactory = "decok.dfcdvadstf.createworldui.config.CreateWorldConfigUI")
public class CreateWorldUI {

    public static Config config;
    private static Logger logger = LogManager.getLogger(Tags.NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

        // Register built-in tabs
        // 注册内置标签页
        TabRegistry.registerTab(GameTab::new, 100, "createworldui.tab.game", 0);
        TabRegistry.registerTab(WorldTab::new, 101, "createworldui.tab.world", 1);
        TabRegistry.registerTab(MoreTab::new, 102, "createworldui.tab.more", 2);
        logger.info("Registered built-in tabs: Game, World, More");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        // Register the /gameruleEditor command
        // 注册/gameruleEditor命令
        event.registerServerCommand(new CommandGameRuleEditor());
        logger.info("Registered /gameruleEditor command");
    }
}
