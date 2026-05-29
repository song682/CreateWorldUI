package decok.dfcdvadstf.createworldui.api;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.EnumDifficulty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 *     DifficultyLocker API<br>
 *     Provides integration interface for ModernDifficultyLocker mod<br>
 *     When both mods are loaded, CreateWorldUI can communicate with DifficultyLocker's WorldDifficultyData
 * </p>
 * <p>
 *     难度锁定器API<br>
 *     为ModernDifficultyLocker模组提供集成接口<br>
 *     当两个模组都加载时，CreateWorldUI可以与DifficultyLocker的WorldDifficultyData通信
 * </p>
 */
public class DifficultyLocker {
    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":DifficultyLocker");
    
    /**
     * <p>
     *     Lock state for each difficulty level<br>
     *     锁定状态：true = 该难度被锁定，无法选择
     * </p>
     */
    private static final boolean[] difficultyLocked = new boolean[4]; // 0=peaceful, 1=easy, 2=normal, 3=hard
    
    /**
     * <p>
     *     Check if ModernDifficultyLocker mod is loaded<br>
     *     检查ModernDifficultyLocker模组是否已加载
     * </p>
     * @return true if difficultylocker mod is loaded / difficultylocker模组是否加载
     */
    public static boolean isLoaded() {
            return Loader.isModLoaded("difficultylocker");
    }
    
    /**
     * <p>
     *     Set the lock state for a specific difficulty<br>
     *     设置特定难度的锁定状态
     * </p>
     * @param difficulty The difficulty to lock/unlock / 要锁定/解锁的难度
     * @param locked true to lock, false to unlock / true锁定，false解锁
     */
    public static void setDifficultyLocked(EnumDifficulty difficulty, boolean locked) {
        if (difficulty != null) {
            difficultyLocked[difficulty.getDifficultyId()] = locked;
            logger.info("Difficulty id={} lock state set to: {}", difficulty.getDifficultyId(), locked);
        }
    }
    
    /**
     * <p>
     *     Check if a specific difficulty is locked<br>
     *     检查特定难度是否被锁定
     * </p>
     * @param difficulty The difficulty to check / 要检查的难度
     * @return true if locked / 是否锁定
     */
    public static boolean isDifficultyLocked(EnumDifficulty difficulty) {
        if (difficulty == null) return false;
        return difficultyLocked[difficulty.getDifficultyId()];
    }
    
    /**
     * <p>
     *     Get the currently locked difficulty or null if none<br>
     *     获取当前锁定的难度，如果没有则返回null
     * </p>
     * @return The locked EnumDifficulty, or null / 锁定的难度枚举，或null
     */
    public static EnumDifficulty getLockedDifficulty() {
        for (int i = 0; i < difficultyLocked.length; i++) {
            if (difficultyLocked[i]) {
                return EnumDifficulty.getDifficultyEnum(i);
            }
        }
        return null;
    }
    
    /**
     * <p>
     *     Check if any difficulty is locked<br>
     *     检查是否有任何难度被锁定
     * </p>
     * @return true if any difficulty is locked / 是否有难度被锁定
     */
    public static boolean hasLockedDifficulty() {
        for (boolean locked : difficultyLocked) {
            if (locked) return true;
        }
        return false;
    }
    
    /**
     * <p>
     *     Reset all lock states (call when world creation is cancelled or UI closes)<br>
     *     重置所有锁定状态（世界创建取消或UI关闭时调用）
     * </p>
     */
    public static void resetAllLocks() {
        for (int i = 0; i < difficultyLocked.length; i++) {
            difficultyLocked[i] = false;
        }
        logger.info("All difficulty locks reset");
    }
    
    /**
     * <p>
     *     Apply locked difficulty to WorldDifficultyData<br>
     *     This should be called during world creation to save the lock state<br>
     *     将锁定的难度应用到WorldDifficultyData<br>
     *     应在世界创建时调用以保存锁定状态
     * </p>
     * @param difficulty The locked difficulty / 锁定的难度
     */
    @Optional.Method(modid = "difficultylocker")
    public static void applyToWorldData(EnumDifficulty difficulty) {
        if (!isLoaded() || difficulty == null) {
            return;
        }
        
        try {
            // 使用反射来调用 WorldDifficultyData，因为我们的模组不应该直接依赖 DifficultyLocker
            Class<?> worldDifficultyDataClass = Class.forName("decok.dfcdvadstf.difficultyLocker.WorldDifficultyData");
            java.lang.reflect.Method getInstanceMethod = worldDifficultyDataClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            
            // 设置锁定状态
            java.lang.reflect.Method setLockedMethod = worldDifficultyDataClass.getMethod("setLocked", boolean.class);
            setLockedMethod.invoke(instance, true);
            
            // 设置锁定的难度
            java.lang.reflect.Method setDifficultyMethod = worldDifficultyDataClass.getMethod("setLockedDifficulty", difficulty.getClass());
            setDifficultyMethod.invoke(instance, difficulty);
            
            logger.info("Applied locked difficulty id={} to WorldDifficultyData", difficulty.getDifficultyId());
        } catch (Exception e) {
            logger.warn("Failed to apply to WorldDifficultyData: {}", e.getMessage());
        }
    }
    
    /**
     * <p>
     *     Load world data from WorldDifficultyData into our API state<br>
     *     This should be called when entering world settings to restore lock state<br>
     *     从WorldDifficultyData加载世界数据到我们的API状态<br>
     *     应在进入世界设置时调用以恢复锁定状态
     * </p>
     * @param saveHandler The save handler to load from / 要加载的保存处理器
     * @param worldName The world name / 世界名称
     */
    @Optional.Method(modid = "difficultylocker")
    public static void loadFromWorldData(Object saveHandler, String worldName) {
        if (!isLoaded()) {
            return;
        }
        
        try {
            // 使用反射来调用 WorldDifficultyData
            Class<?> worldDifficultyDataClass = Class.forName("decok.dfcdvadstf.difficultyLocker.WorldDifficultyData");
            java.lang.reflect.Method getInstanceMethod = worldDifficultyDataClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            
            // 加载世界数据
            java.lang.reflect.Method loadMethod = worldDifficultyDataClass.getMethod("loadWorldData", 
                Class.forName("net.minecraft.world.storage.ISaveHandler"), String.class);
            loadMethod.invoke(instance, saveHandler, worldName);
            
            // 获取锁定状态
            java.lang.reflect.Method isLockedMethod = worldDifficultyDataClass.getMethod("isLocked");
            boolean isLocked = (boolean) isLockedMethod.invoke(instance);
            
            if (isLocked) {
                // 获取锁定的难度
                java.lang.reflect.Method getDifficultyMethod = worldDifficultyDataClass.getMethod("getLockedDifficultyEnum");
                EnumDifficulty lockedDifficulty = (EnumDifficulty) getDifficultyMethod.invoke(instance);
                
                if (lockedDifficulty != null) {
                    setDifficultyLocked(lockedDifficulty, true);
                    logger.info("Loaded locked difficulty id={} from WorldDifficultyData", lockedDifficulty.getDifficultyId());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load from WorldDifficultyData: {}", e.getMessage());
        }
    }
}