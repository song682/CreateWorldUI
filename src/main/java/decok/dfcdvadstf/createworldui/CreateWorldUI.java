package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.api.hook.WorldCreationCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.lang.reflect.Field;

@Mod(modid = Tags.MODID, name = Tags.NAME, version = Tags.VERSION)
public class CreateWorldUI {

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Initializing CreateWorldUI Mod");

        // 初始化Mixin
        try {
            // 正确的Forge Mixin初始化方式
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.createworldui.json");
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

    // 添加这个方法来确保事件监听器被正确注册
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiCreateWorld) {
            logger.info("Detected GuiCreateWorld opening");

            if (WorldCreationCheck.shouldUseModernUI()) {
                logger.info("Using modern world creation UI");

                // 获取父屏幕
                GuiScreen parentScreen = getParentScreen((GuiCreateWorld) event.gui);

                // 替换为现代UI
                event.gui = WorldCreationCheck.createModernWorldCreationScreen(parentScreen);
            } else {
                logger.info("Using vanilla world creation UI");
            }
        }
    }

    // 确保这个方法能够正确获取父屏幕
    private GuiScreen getParentScreen(GuiCreateWorld gui) {
        try {
            // 使用反射获取父屏幕字段
            Field parentField = GuiCreateWorld.class.getDeclaredField("parentScreen");
            parentField.setAccessible(true);
            return (GuiScreen) parentField.get(gui);
        } catch (Exception e) {
            logger.error("Failed to get parent screen from GuiCreateWorld", e);

            // 备选方案：使用主菜单作为父屏幕
            return Minecraft.getMinecraft().currentScreen;
        }
    }
}
