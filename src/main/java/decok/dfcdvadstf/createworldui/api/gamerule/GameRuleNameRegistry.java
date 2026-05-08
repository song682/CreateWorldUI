package decok.dfcdvadstf.createworldui.api.gamerule;

import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     游戏规则显示名称注册 API<br>
 *     提供游戏规则的显示名称注册、查询和获取功能<br>
 *     用于在 GameRule Editor 中显示规则的友好名称
 * </p>
 * <p>
 *     GameRule Display Name Registration API<br>
 *     Provides display name registration, query, and retrieval for game rules<br>
 *     Used to display friendly rule names in GameRule Editor
 * </p>
 * <p>
 *     优先级说明（从高到低）：<br>
 *     1. 本地化文件中的名称（gamerule.{ruleName}.name）<br>
 *     2. 通过此 API 注册的显示名称<br>
 *     3. 原始规则名称（如 doFireTick）
 * </p>
 * <p>
 *     Priority (high to low):<br>
 *     1. Name in localization file (gamerule.{ruleName}.name)<br>
 *     2. Display names registered through this API<br>
 *     3. Raw rule name (e.g., doFireTick)
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleNameRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleNameRegistry");

    // 存储注册的显示名称映射（规则名 -> 显示名称）
    // Storage for registered display names (rule name -> display name)
    private static final Map<String, String> registeredNames = new HashMap<>();

    /**
     * <p>
     *     注册单个游戏规则的显示名称<br>
     *     适合一次添加一个名称
     * </p>
     * <p>
     *     Register display name for a single game rule<br>
     *     Suitable for adding one name at a time
     * </p>
     *
     * @param ruleName 游戏规则名称（如 doFireTick）/ Game rule name (e.g., doFireTick)
     * @param displayName 要显示的友好名称 / Friendly display name to show
     */
    public static void registerName(String ruleName, String displayName) {
        if (ruleName == null || ruleName.isEmpty()) {
            LOGGER.warn("Cannot register display name with null or empty rule name");
            return;
        }
        if (displayName == null || displayName.isEmpty()) {
            LOGGER.warn("Cannot register null or empty display name for rule: {}", ruleName);
            return;
        }
        registeredNames.put(ruleName, displayName);
        LOGGER.debug("Registered display name for gamerule: {} -> {}", ruleName, displayName);
    }

    /**
     * <p>
     *     批量注册多个游戏规则的显示名称<br>
     *     适合一次添加很多个名称
     * </p>
     * <p>
     *     Register display names for multiple game rules at once<br>
     *     Suitable for adding many names in one call
     * </p>
     *
     * @param names 规则名到显示名称的映射 / Map of rule names to display names
     */
    public static void registerNames(Map<String, String> names) {
        if (names == null) {
            LOGGER.warn("Cannot register null names map");
            return;
        }
        int count = 0;
        for (Map.Entry<String, String> entry : names.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                registeredNames.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        LOGGER.debug("Registered {} display names", count);
    }

    /**
     * <p>
     *     获取指定游戏规则的显示名称<br>
     *     按照优先级返回：本地化 > 注册名称 > 原始规则名
     * </p>
     * <p>
     *     Get display name for a specific game rule<br>
     *     Returns by priority: localization > registered name > raw rule name
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 显示名称，如果没有找到则返回原始规则名 / Display name, or raw rule name if not found
     */
    public static String getName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return ruleName;
        }

        // 1. 检查本地化文件
        // 1. Check localization file
        String translationKey = "gamerule." + ruleName + ".name";
        String translated = I18n.format(translationKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(translationKey)) {
            return translated;
        }

        // 2. 检查通过 API 注册的显示名称
        // 2. Check display names registered via API
        if (registeredNames.containsKey(ruleName)) {
            return registeredNames.get(ruleName);
        }

        // 3. 回退到原始规则名称
        // 3. Fallback to raw rule name
        return ruleName;
    }

    /**
     * <p>
     *     检查是否已经注册了指定规则的显示名称
     * </p>
     * <p>
     *     Check if display name for a specific rule is already registered
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果已注册则返回 true / True if already registered
     */
    public static boolean hasRegisteredName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        return registeredNames.containsKey(ruleName);
    }

    /**
     * <p>
     *     移除指定游戏规则的显示名称注册
     * </p>
     * <p>
     *     Remove display name registration for a specific game rule
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        boolean removed = registeredNames.remove(ruleName) != null;
        if (removed) {
            LOGGER.debug("Removed display name for gamerule: {}", ruleName);
        }
        return removed;
    }

    /**
     * <p>
     *     清除所有通过 API 注册的显示名称<br>
     *     注意：这不会影响本地化文件
     * </p>
     * <p>
     *     Clear all display names registered through this API<br>
     *     Note: This does not affect localization files
     * </p>
     */
    public static void clearAllNames() {
        registeredNames.clear();
        LOGGER.info("Cleared all registered display names");
    }

    /**
     * <p>
     *     获取所有已注册的显示名称数量
     * </p>
     * <p>
     *     Get the count of all registered display names
     * </p>
     *
     * @return 已注册的显示名称数量 / Number of registered display names
     */
    public static int getRegisteredCount() {
        return registeredNames.size();
    }

    /**
     * <p>
     *     获取所有已注册的显示名称映射（只读）
     * </p>
     * <p>
     *     Get all registered display names map (read-only)
     * </p>
     *
     * @return 已注册的显示名称映射的只读副本 / Read-only copy of registered display names map
     */
    public static Map<String, String> getAllRegisteredNames() {
        return new HashMap<>(registeredNames);
    }
}
