package decok.dfcdvadstf.createworldui.api.gamerule;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     游戏规则显示名称注册 API<br>
 *     提供游戏规则显示名称的本地化键注册、查询和获取功能<br>
 *     用于在 GameRule Editor 中显示规则的友好名称<br>
 *     注册的是本地化键（而非原始文本），实际文字由调用方模组在自己的 lang 文件中提供
 * </p>
 * <p>
 *     GameRule Display Name Registration API<br>
 *     Provides display-name localization-key registration, query, and retrieval for game rules<br>
 *     Used to display friendly rule names in GameRule Editor<br>
 *     Localization keys (not raw text) are registered; the actual text is supplied
 *     by the calling mod's own lang files
 * </p>
 * <p>
 *     优先级说明（从高到低）：<br>
 *     1. 标准本地化键的翻译（gamerule.{ruleName}.name）<br>
 *     2. 通过此 API 注册的本地化键的翻译<br>
 *     3. 原始规则名称（如 doFireTick）
 * </p>
 * <p>
 *     Priority (high to low):<br>
 *     1. Translation of the standard localization key (gamerule.{ruleName}.name)<br>
 *     2. Translation of the localization key registered through this API<br>
 *     3. Raw rule name (e.g., doFireTick)
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleNameRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleNameRegistry");

    // 存储注册的显示名称本地化键映射（规则名 -> 本地化键）
    // Storage for registered display-name localization keys (rule name -> localization key)
    private static final Map<String, String> registeredNameKeys = new HashMap<>();

    /**
     * <p>
     *     注册单个游戏规则的显示名称本地化键<br>
     *     实际文字请在你自己模组的 lang 文件中提供该键的翻译<br>
     *     适合一次添加一个名称
     * </p>
     * <p>
     *     Register a display-name localization key for a single game rule<br>
     *     Provide the translation for this key in your own mod's lang files<br>
     *     Suitable for adding one name at a time
     * </p>
     *
     * @param ruleName 游戏规则名称（如 doFireTick）/ Game rule name (e.g., doFireTick)
     * @param nameKey 显示名称的本地化键（如 mymod.gamerule.myRule.name）/ Localization key of the display name (e.g., mymod.gamerule.myRule.name)
     */
    public static void registerName(String ruleName, String nameKey) {
        if (ruleName == null || ruleName.isEmpty()) {
            LOGGER.warn("Cannot register display name key with null or empty rule name");
            return;
        }
        if (nameKey == null || nameKey.isEmpty()) {
            LOGGER.warn("Cannot register null or empty display name key for rule: {}", ruleName);
            return;
        }
        registeredNameKeys.put(ruleName, nameKey);
        LOGGER.debug("Registered display name key for gamerule: {} -> {}", ruleName, nameKey);
    }

    /**
     * <p>
     *     批量注册多个游戏规则的显示名称本地化键<br>
     *     适合一次添加很多个名称
     * </p>
     * <p>
     *     Register display-name localization keys for multiple game rules at once<br>
     *     Suitable for adding many names in one call
     * </p>
     *
     * @param nameKeys 规则名到显示名称本地化键的映射 / Map of rule names to display-name localization keys
     */
    public static void registerNames(Map<String, String> nameKeys) {
        if (nameKeys == null) {
            LOGGER.warn("Cannot register null name keys map");
            return;
        }
        int count = 0;
        for (Map.Entry<String, String> entry : nameKeys.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                registeredNameKeys.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        LOGGER.debug("Registered {} display name keys", count);
    }

    /**
     * <p>
     *     获取指定游戏规则的显示名称（已本地化）<br>
     *     按照优先级返回：标准本地化键的翻译 > 注册的本地化键的翻译 > 原始规则名
     * </p>
     * <p>
     *     Get the (localized) display name for a specific game rule<br>
     *     Returns by priority: translation of the standard key > translation of the registered key > raw rule name
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 本地化后的显示名称，如果没有找到则返回原始规则名 / Localized display name, or raw rule name if not found
     */
    public static String getName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return ruleName;
        }

        // 1. 检查标准本地化键（gamerule.{ruleName}.name）
        // 1. Check the standard localization key (gamerule.{ruleName}.name)
        String standardKey = "gamerule." + ruleName + ".name";
        String translated = I18n.format(standardKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(standardKey)) {
            return translated;
        }

        // 2. 解析通过 API 注册的本地化键
        // 2. Resolve the localization key registered via API
        String registeredKey = registeredNameKeys.get(ruleName);
        if (registeredKey != null) {
            String resolved = I18n.format(registeredKey);
            if (resolved != null && !resolved.isEmpty() && !resolved.equals(registeredKey)) {
                return resolved;
            }
            // 翻译缺失时原样返回键名，便于调用方发现漏写 lang 条目
            // Return the key as-is when the translation is missing, so callers can spot missing lang entries
            return registeredKey;
        }

        // 3. 回退到原始规则名称
        // 3. Fallback to raw rule name
        return ruleName;
    }

    /**
     * <p>
     *     检查是否已经注册了指定规则的显示名称本地化键
     * </p>
     * <p>
     *     Check if a display-name localization key for a specific rule is already registered
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果已注册则返回 true / True if already registered
     */
    public static boolean hasRegisteredName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        return registeredNameKeys.containsKey(ruleName);
    }

    /**
     * <p>
     *     移除指定游戏规则的显示名称本地化键注册
     * </p>
     * <p>
     *     Remove the display-name localization key registration for a specific game rule
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        boolean removed = registeredNameKeys.remove(ruleName) != null;
        if (removed) {
            LOGGER.debug("Removed display name key for gamerule: {}", ruleName);
        }
        return removed;
    }

    /**
     * <p>
     *     清除所有通过 API 注册的显示名称本地化键<br>
     *     注意：这不会影响本地化文件
     * </p>
     * <p>
     *     Clear all display-name localization keys registered through this API<br>
     *     Note: This does not affect localization files
     * </p>
     */
    public static void clearAllNames() {
        registeredNameKeys.clear();
        LOGGER.info("Cleared all registered display name keys");
    }

    /**
     * <p>
     *     获取所有已注册的显示名称本地化键数量
     * </p>
     * <p>
     *     Get the count of all registered display-name localization keys
     * </p>
     *
     * @return 已注册的显示名称本地化键数量 / Number of registered display-name localization keys
     */
    public static int getRegisteredCount() {
        return registeredNameKeys.size();
    }

    /**
     * <p>
     *     获取所有已注册的显示名称本地化键映射（只读）
     * </p>
     * <p>
     *     Get all registered display-name localization keys map (read-only)
     * </p>
     *
     * @return 已注册的显示名称本地化键映射的只读副本 / Read-only copy of registered display-name localization keys map
     */
    public static Map<String, String> getAllRegisteredNames() {
        return new HashMap<>(registeredNameKeys);
    }
}
