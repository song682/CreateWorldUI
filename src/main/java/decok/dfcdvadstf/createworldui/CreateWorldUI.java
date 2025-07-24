package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import decok.dfcdvadstf.createworldui.api.hook.WorldCreationCheck;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(modid = CreateWorldUI.MODID, name = CreateWorldUI.NAME, version = CreateWorldUI.VERSION)
public class CreateWorldUI {
    public static final String MODID = "СreateWorldUI";
    public static final String NAME = "CreateWorldUI";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
        // 注册示例标签页（可选）
        // CreateWorldAPI.registerTab(new ExampleTab());
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // 检测到创建世界界面打开
        if (event.gui instanceof GuiCreateWorld) {
            GuiCreateWorld guiCreateWorld = (GuiCreateWorld) event.gui;

            // 检查是否是首次创建世界
            if (WorldCreationCheck.shouldUseModernUI()) {
                logger.info("Using modern world creation UI for new player");
                event.gui = new GuiCreateWorldModern(guiCreateWorld.parentScreen);
            } else {
                logger.debug("Using vanilla world creation UI");
            }
        }
    }
}
