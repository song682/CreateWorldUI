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
 *     GameRule Tooltip Registration API<br>
 *     Provides tooltip registration, query, and retrieval for game rules<br>
 *     Used to display rule descriptions in GameRule Editor<br>
 *     Registered tooltips are used as-is; for localization, callers should
 *     pass already-translated text such as {@code I18n.format("gamerule.ruleNames.***")}
 * </p>
 * <p>
 *     Priority (high to low):<br>
 *     1. Description in localization file (gamerule.{ruleName}.tooltip.description)<br>
 *     2. Tooltips registered through this API<br>
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleTooltipRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleTooltipRegistry");

    // 存储注册的 tooltip 映射（规则键 -> tooltip文本）
    // Storage for registered tooltips (rule key -> tooltip text)
    private static final Map<String, String> registeredTooltips = new HashMap<>();

    /**
     * <p>
     *     Register tooltip for a single game rule<br>
     *     The tooltip text is used as-is; pass the result of {@code I18n.format(...)}
     *     yourself if localization is needed<br>
     *     Suitable for adding one tooltip at a time
     * </p>
     *
     * @param ruleName Game rule key (e.g., doFireTick)
     * @param tooltip Tooltip text to display
     */
    public static void registerTooltip(String ruleName, String tooltip) {
        if (ruleName == null || ruleName.isEmpty()) {
            LOGGER.warn("Cannot register tooltip with null or empty rule key");
            return;
        }
        if (tooltip == null) {
            LOGGER.warn("Cannot register null tooltip for rule: {}", ruleName);
            return;
        }
        registeredTooltips.put(ruleName, tooltip);
        LOGGER.debug("Registered tooltip for gamerule: {}", ruleName);
    }

    /**
     * <p>
     *     Register tooltips for multiple game rules at once<br>
     *     Tooltip texts are used as-is; pass the results of {@code I18n.format(...)}
     *     yourself if localization is needed<br>
     *     Suitable for adding many tooltips in one call
     * </p>
     *
     * @param tooltips Map of rule keys to tooltip texts
     */
    public static void registerTooltips(Map<String, String> tooltips) {
        if (tooltips == null) {
            LOGGER.warn("Cannot register null tooltips map");
            return;
        }
        int count = 0;
        for (Map.Entry<String, String> entry : tooltips.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                registeredTooltips.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        LOGGER.debug("Registered {} tooltips", count);
    }

    /**
     * <p>
     *     获取指定游戏规则的 tooltip<br>
     *     按照优先级返回：本地化 > 注册tooltip
     * </p>
     * <p>
     *     Get tooltip for a specific game rule<br>
     *     Returns by priority: localization > registered tooltip
     * </p>
     *
     * @param ruleName 游戏规则键名 / Game rule key
     * @return tooltip A tooltip text, or null if not found
     */
    public static String getTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return null;
        }

        // 1. 检查本地化文件
        // 1. Check localization file
        String translationKey = "gamerule." + ruleName + ".tooltip.description";
        String translated = I18n.format(translationKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(translationKey)) {
            return translated;
        }

        // 2. 检查通过 API 注册的 tooltip（按原样返回）
        // 2. Check tooltips registered via API (returned as-is)
        if (registeredTooltips.containsKey(ruleName)) {
            return registeredTooltips.get(ruleName);
        }

        // 没有找到任何描述
        // No description found
        return null;
    }

    /**
     * <p>
     *     检查是否已经注册了指定规则的 tooltip
     * </p>
     * <p>
     *     Check if tooltip for a specific rule is already registered
     * </p>
     *
     * @param ruleName 游戏规则键名 / Game rule key
     * @return 如果已注册则返回 true / True if already registered
     */
    public static boolean hasRegisteredTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        return registeredTooltips.containsKey(ruleName);
    }

    /**
     * <p>
     *     移除指定游戏规则的 tooltip 注册
     * </p>
     * <p>
     *     Remove tooltip registration for a specific game rule
     * </p>
     *
     * @param ruleName 游戏规则键名 / Game rule key
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeTooltip(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }
        boolean removed = registeredTooltips.remove(ruleName) != null;
        if (removed) {
            LOGGER.debug("Removed tooltip for gamerule: {}", ruleName);
        }
        return removed;
    }

    /**
     * <p>
     *     清除所有通过 API 注册的 tooltip<br>
     *     注意：这不会影响本地化文件
     * </p>
     * <p>
     *     Clear all tooltips registered through this API<br>
     *     Note: This does not affect localization files
     * </p>
     */
    public static void clearAllTooltips() {
        registeredTooltips.clear();
        LOGGER.info("Cleared all registered tooltips");
    }

    /**
     * <p>
     *     获取所有已注册的 tooltip 数量
     * </p>
     * <p>
     *     Get the count of all registered tooltips
     * </p>
     *
     * @return 已注册的 tooltip 数量 / Number of registered tooltips
     */
    public static int getRegisteredCount() {
        return registeredTooltips.size();
    }

    /**
     * <p>
     *     获取所有已注册的 tooltip 映射（只读）
     * </p>
     * <p>
     *     Get all registered tooltips map (read-only)
     * </p>
     *
     * @return 已注册的 tooltip 映射的只读副本 / Read-only copy of registered tooltips map
     */
    public static Map<String, String> getAllRegisteredTooltips() {
        return new HashMap<>(registeredTooltips);
    }
}
