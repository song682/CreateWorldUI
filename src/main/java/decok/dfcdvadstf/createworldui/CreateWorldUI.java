package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import decok.dfcdvadstf.createworldui.api.WorldCreationCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION)
public class CreateWorldUI {

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

        // 注册事件处理器 - 使用高优先级确保我们先处理
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("CreateWorldUI Mod loaded successfully");
    }

    // 使用高优先级确保我们先处理GUI打开事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiCreateWorld) {
            logger.info("Detected GuiCreateWorld opening");

            // 强制使用现代UI（调试模式）
            if (WorldCreationCheck.shouldUseModernUI()) {
                logger.info("Using modern world creation UI");

                // 获取父屏幕
                GuiScreen parentScreen = getParentScreen((GuiCreateWorld) event.gui);

                // 替换为现代UI
                event.gui = new GuiCreateWorldModern(parentScreen);
            } else {
                logger.info("Using vanilla world creation UI");
            }
        }
    }

    /**
     * 安全获取GuiCreateWorld的父屏幕
     */
    private GuiScreen getParentScreen(GuiCreateWorld gui) {
        try {
            // 使用反射获取父屏幕字段
            Field parentField = GuiCreateWorld.class.getDeclaredField("parentScreen");
            parentField.setAccessible(true);
            return (GuiScreen) parentField.get(gui);
        } catch (Exception e) {
            logger.error("Failed to get parent screen from GuiCreateWorld", e);

            // 备选方案：使用当前屏幕作为父屏幕
            return Minecraft.getMinecraft().currentScreen;
        }
    }
}