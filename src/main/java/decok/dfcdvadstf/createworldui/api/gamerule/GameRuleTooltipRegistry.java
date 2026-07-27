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
 *     游戏规则 Tooltip 注册 API<br>
 *     提供游戏规则的 tooltip 本地化键注册、查询和获取功能<br>
 *     用于在 GameRule Editor 中显示规则的说明信息<br>
 *     注册的是本地化键（而非原始文本），实际文字由调用方模组在自己的 lang 文件中提供
 * </p>
 * <p>
 *     GameRule Tooltip Registration API<br>
 *     Provides tooltip localization-key registration, query, and retrieval for game rules<br>
 *     Used to display rule descriptions in GameRule Editor<br>
 *     Localization keys (not raw text) are registered; the actual text is supplied
 *     by the calling mod's own lang files
 * </p>
 * <p>
 *     优先级说明（从高到低）：<br>
 *     1. 标准本地化键的翻译（gamerule.{ruleName}.tooltip.description）<br>
 *     2. 通过此 API 注册的本地化键的翻译
 * </p>
 * <p>
 *     Priority (high to low):<br>
 *     1. Translation of the standard localization key (gamerule.{ruleName}.tooltip.description)<br>
 *     2. Translation of the localization key registered through this API
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleTooltipRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleTooltipRegistry");

    // 存储注册的 tooltip 本地化键映射（规则名 -> 本地化键）
    // Storage for registered tooltip localization keys (rule name -> localization key)
    private static final Map<String, String> registeredTooltipKeys = new HashMap<>();

    /**
     * <p>
     *     注册单个游戏规则的 tooltip 本地化键<br>
     *     实际文字请在你自己模组的 lang 文件中提供该键的翻译<br>
     *     适合一次添加一个 tooltip
     * </p>
     * <p>
     *     Register a tooltip localization key for a single game rule<br>
     *     Provide the translation for this key in your own mod's lang files<br>
     *     Suitable for adding one tooltip at a time
     * </p>
     *
     * @param ruleName 游戏规则名称（如 doFireTick）/ Game rule name (e.g., doFireTick)
     * @param tooltipKey tooltip 的本地化键（如 mymod.gamerule.myRule.tooltip）/ Localization key of the tooltip (e.g., mymod.gamerule.myRule.tooltip)
     */
    public static void registerTooltip(String ruleName, String tooltipKey) {
        if (ruleName == null || ruleName.isEmpty()) {
            LOGGER.warn("Cannot register tooltip key with null or empty rule name");
            return;
        }
        if (tooltipKey == null || tooltipKey.isEmpty()) {
            LOGGER.warn("Cannot register null or empty tooltip key for rule: {}", ruleName);
            return;
        }
        registeredTooltipKeys.put(ruleName, tooltipKey);
        LOGGER.debug("Registered tooltip key for gamerule: {} -> {}", ruleName, tooltipKey);
    }

    /**
     * <p>
     *     批量注册多个游戏规则的 tooltip 本地化键<br>
     *     适合一次添加很多个 tooltip
     * </p>
     * <p>
     *     Register tooltip localization keys for multiple game rules at once<br>
     *     Suitable for adding many tooltips in one call
     * </p>
     *
     * @param tooltipKeys 规则名到 tooltip 本地化键的映射 / Map of rule names to tooltip localization keys
     */
    public static void registerTooltips(Map<String, String> tooltipKeys) {
        if (tooltipKeys == null) {
            LOGGER.warn("Cannot register null tooltip keys map");
            return;
        }
        int count = 0;
        for (Map.Entry<String, String> entry : tooltipKeys.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                registeredTooltipKeys.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        LOGGER.debug("Registered {} tooltip keys", count);
    }

    /**
     * <p>
     *     获取指定游戏规则的 tooltip 文本（已本地化）<br>
     *     按照优先级返回：标准本地化键的翻译 > 注册的本地化键的翻译
     * </p>
     * <p>
     *     Get the (localized) tooltip text for a specific game rule<br>
     *     Returns by priority: translation of the standard key > translation of the registered key
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 本地化后的 tooltip 文本，如果没有找到则返回 null / Localized tooltip text, or null if not found
     */
    public static String getTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return null;
        }

        // 1. 检查标准本地化键（gamerule.{ruleName}.tooltip.description）
        // 1. Check the standard localization key (gamerule.{ruleName}.tooltip.description)
        String standardKey = "gamerule." + ruleName + ".tooltip.description";
        String translated = I18n.format(standardKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(standardKey)) {
            return translated;
        }

        // 2. 解析通过 API 注册的本地化键
        // 2. Resolve the localization key registered via API
        String registeredKey = registeredTooltipKeys.get(ruleName);
        if (registeredKey != null) {
            String resolved = I18n.format(registeredKey);
            if (resolved != null && !resolved.isEmpty() && !resolved.equals(registeredKey)) {
                return resolved;
            }
            // 翻译缺失时原样返回键名，便于调用方发现漏写 lang 条目
            // Return the key as-is when the translation is missing, so callers can spot missing lang entries
            return registeredKey;
        }

        // 没有找到任何描述
        // No description found
        return null;
    }

    /**
     * <p>
     *     检查是否已经注册了指定规则的 tooltip 本地化键
     * </p>
     * <p>
     *     Check if a tooltip localization key for a specific rule is already registered
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果已注册则返回 true / True if already registered
     */
    public static boolean hasRegisteredTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        return registeredTooltipKeys.containsKey(ruleName);
    }

    /**
     * <p>
     *     移除指定游戏规则的 tooltip 本地化键注册
     * </p>
     * <p>
     *     Remove the tooltip localization key registration for a specific game rule
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        boolean removed = registeredTooltipKeys.remove(ruleName) != null;
        if (removed) {
            LOGGER.debug("Removed tooltip key for gamerule: {}", ruleName);
        }
        return removed;
    }

    /**
     * <p>
     *     清除所有通过 API 注册的 tooltip 本地化键<br>
     *     注意：这不会影响本地化文件
     * </p>
     * <p>
     *     Clear all tooltip localization keys registered through this API<br>
     *     Note: This does not affect localization files
     * </p>
     */
    public static void clearAllTooltips() {
        registeredTooltipKeys.clear();
        LOGGER.info("Cleared all registered tooltip keys");
    }

    /**
     * <p>
     *     获取所有已注册的 tooltip 本地化键数量
     * </p>
     * <p>
     *     Get the count of all registered tooltip localization keys
     * </p>
     *
     * @return 已注册的 tooltip 本地化键数量 / Number of registered tooltip localization keys
     */
    public static int getRegisteredCount() {
        return registeredTooltipKeys.size();
    }

    /**
     * <p>
     *     获取所有已注册的 tooltip 本地化键映射（只读）
     * </p>
     * <p>
     *     Get all registered tooltip localization keys map (read-only)
     * </p>
     *
     * @return 已注册的 tooltip 本地化键映射的只读副本 / Read-only copy of registered tooltip localization keys map
     */
    public static Map<String, String> getAllRegisteredTooltips() {
        return new HashMap<>(registeredTooltipKeys);
    }
}
