package decok.dfcdvadstf.createworldui;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import decok.dfcdvadstf.createworldui.api.CreateWorldAPI;
import decok.dfcdvadstf.createworldui.tabbyui.GuiCreateWorldModern;
import decok.dfcdvadstf.createworldui.api.hook.WorldCreationCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
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

    @EventHandler
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote && nextWorldGameRules != null) {
            // 应用游戏规则
            event.world.getGameRules().setOrCreateGameRule("doDaylightCycle", nextWorldGameRules.daylightCycle ? "true" : "false");
            event.world.getGameRules().setOrCreateGameRule("doWeatherCycle", nextWorldGameRules.weatherCycle ? "true" : "false");
            event.world.getGameRules().setOrCreateGameRule("doMobSpawning", nextWorldGameRules.spawnAnimals ? "true" : "false");
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // 检测到创建世界界面打开
        if (event.gui instanceof GuiCreateWorld) {
            // 检查是否是首次创建世界
            if (WorldCreationCheck.shouldUseModernUI()) {
                logger.info("Using modern world creation UI for new player");

                // 获取父屏幕的可靠方法
                GuiScreen parentScreen = getParentScreen((GuiCreateWorld) event.gui);

                // 创建自定义UI实例
                event.gui = new GuiCreateWorldModern(parentScreen);
            } else {
                logger.debug("Using vanilla world creation UI");
            }
        }
    }

    /**
     * 安全获取GuiCreateWorld的父屏幕
     *
     * 在1.7.10中，GuiCreateWorld的父屏幕字段是protected，
     * 我们需要使用反射来访问它
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
