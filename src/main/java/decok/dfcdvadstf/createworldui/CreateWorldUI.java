package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION)
public class CreateWorldUI {

    private static Logger logger;

    // 强制使用现代UI（调试模式）
    private static final boolean FORCE_MODERN_UI = true;

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

            // 检查是否应该使用现代UI
            if (shouldUseModernUI()) {
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
     * 检查是否应该使用现代UI
     */
    private boolean shouldUseModernUI() {
        // 调试模式下总是返回true
        if (FORCE_MODERN_UI) {
            logger.info("DEBUG MODE: Forcing modern UI");
            return true;
        }

        try {
            File savesDir = new File(Minecraft.getMinecraft().mcDataDir, "saves");
            boolean exists = savesDir.exists();
            boolean isEmpty = !exists || (savesDir.list() != null && savesDir.list().length == 0);

            logger.info("Saves directory check: exists=" + exists + ", empty=" + isEmpty);

            return isEmpty;
        } catch (Exception e) {
            logger.error("Error checking saves directory", e);
            return true;
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