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
 *     提供游戏规则的 tooltip 注册、查询和获取功能<br>
 *     用于在 GameRule Editor 中显示规则的说明信息<br>
 *     注册的 tooltip 按原样使用；若需要本地化，请调用方自行传入
 *     {@code I18n.format("gamerule.ruleNames.***")} 等已翻译好的文本
 * </p>
 * <p>
 *     GameRule Tooltip Registration API<br>
 *     Provides tooltip registration, query, and retrieval for game rules<br>
 *     Used to display rule descriptions in GameRule Editor<br>
 *     Registered tooltips are used as-is; for localization, callers should
 *     pass already-translated text such as {@code I18n.format("gamerule.ruleNames.***")}
 * </p>
 * <p>
 *     优先级说明（从高到低）：<br>
 *     1. 本地化文件中的描述（gamerule.{ruleKey}.tooltip.description）<br>
 *     2. 通过此 API 注册的 tooltip<br>
 *     3. 内置的默认描述（仅原版规则）
 * </p>
 * <p>
 *     Priority (high to low):<br>
 *     1. Description in localization file (gamerule.{ruleKey}.tooltip.description)<br>
 *     2. Tooltips registered through this API<br>
 *     3. Built-in default descriptions (vanilla rules only)
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleTooltipRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleTooltipRegistry");

    // 存储注册的 tooltip 映射（规则键 -> tooltip文本）
    // Storage for registered tooltips (rule key -> tooltip text)
    private static final Map<String, String> registeredTooltips = new HashMap<>();

    // 内置的默认 tooltip 描述（仅用于原版规则的回退）
    // Built-in default tooltip descriptions (fallback for vanilla rules only)
    private static final Map<String, String> DEFAULT_DESCRIPTIONS = new HashMap<>();

    static {
        // 初始化原版游戏规则的默认描述
        // Initialize default descriptions for vanilla game rules
        DEFAULT_DESCRIPTIONS.put("doFireTick", "Controls whether fire spreads and naturally extinguishes");
        DEFAULT_DESCRIPTIONS.put("mobGriefing", "Controls whether mobs can destroy blocks");
        DEFAULT_DESCRIPTIONS.put("keepInventory", "Keep inventory after death");
        DEFAULT_DESCRIPTIONS.put("doMobSpawning", "Natural mob spawning");
        DEFAULT_DESCRIPTIONS.put("doMobLoot", "Mobs drop loot");
        DEFAULT_DESCRIPTIONS.put("doTileDrops", "Blocks drop items when destroyed");
        DEFAULT_DESCRIPTIONS.put("commandBlockOutput", "Command blocks output to chat");
        DEFAULT_DESCRIPTIONS.put("naturalRegeneration", "Natural health regeneration");
        DEFAULT_DESCRIPTIONS.put("doDaylightCycle", "Day/night cycle");
    }

    /**
     * <p>
     *     注册单个游戏规则的 tooltip<br>
     *     tooltip 文本按原样使用；若需要本地化，请自行传入 {@code I18n.format(...)} 的结果<br>
     *     适合一次添加一个 tooltip
     * </p>
     * <p>
     *     Register tooltip for a single game rule<br>
     *     The tooltip text is used as-is; pass the result of {@code I18n.format(...)}
     *     yourself if localization is needed<br>
     *     Suitable for adding one tooltip at a time
     * </p>
     *
     * @param ruleKey 游戏规则键名（如 doFireTick）/ Game rule key (e.g., doFireTick)
     * @param tooltip 要显示的 tooltip 文本 / Tooltip text to display
     */
    public static void registerTooltip(String ruleKey, String tooltip) {
        if (ruleKey == null || ruleKey.isEmpty()) {
            LOGGER.warn("Cannot register tooltip with null or empty rule key");
            return;
        }
        if (tooltip == null) {
            LOGGER.warn("Cannot register null tooltip for rule: {}", ruleKey);
            return;
        }
        registeredTooltips.put(ruleKey, tooltip);
        LOGGER.debug("Registered tooltip for gamerule: {}", ruleKey);
    }

    /**
     * <p>
     *     批量注册多个游戏规则的 tooltip<br>
     *     tooltip 文本按原样使用；若需要本地化，请自行传入 {@code I18n.format(...)} 的结果<br>
     *     适合一次添加很多个 tooltip
     * </p>
     * <p>
     *     Register tooltips for multiple game rules at once<br>
     *     Tooltip texts are used as-is; pass the results of {@code I18n.format(...)}
     *     yourself if localization is needed<br>
     *     Suitable for adding many tooltips in one call
     * </p>
     *
     * @param tooltips 规则键到 tooltip 文本的映射 / Map of rule keys to tooltip texts
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
     *     按照优先级返回：本地化 > 注册tooltip > 默认描述
     * </p>
     * <p>
     *     Get tooltip for a specific game rule<br>
     *     Returns by priority: localization > registered tooltip > default description
     * </p>
     *
     * @param ruleKey 游戏规则键名 / Game rule key
     * @return tooltip 文本，如果没有找到则返回 null / Tooltip text, or null if not found
     */
    public static String getTooltip(String ruleKey) {
        if (ruleKey == null || ruleKey.isEmpty()) {
            return null;
        }

        // 1. 检查本地化文件
        // 1. Check localization file
        String translationKey = "gamerule." + ruleKey + ".tooltip.description";
        String translated = I18n.format(translationKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(translationKey)) {
            return translated;
        }

        // 2. 检查通过 API 注册的 tooltip（按原样返回）
        // 2. Check tooltips registered via API (returned as-is)
        if (registeredTooltips.containsKey(ruleKey)) {
            return registeredTooltips.get(ruleKey);
        }

        // 3. 回退到内置默认描述（仅原版规则）
        // 3. Fallback to built-in default descriptions (vanilla rules only)
        if (DEFAULT_DESCRIPTIONS.containsKey(ruleKey)) {
            return DEFAULT_DESCRIPTIONS.get(ruleKey);
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
     * @param ruleKey 游戏规则键名 / Game rule key
     * @return 如果已注册则返回 true / True if already registered
     */
    public static boolean hasRegisteredTooltip(String ruleKey) {
        if (ruleKey == null || ruleKey.isEmpty()) {
            return false;
        }
        return registeredTooltips.containsKey(ruleKey);
    }

    /**
     * <p>
     *     移除指定游戏规则的 tooltip 注册
     * </p>
     * <p>
     *     Remove tooltip registration for a specific game rule
     * </p>
     *
     * @param ruleKey 游戏规则键名 / Game rule key
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeTooltip(String ruleKey) {
        if (ruleKey == null || ruleKey.isEmpty()) {
            return false;
        }
        boolean removed = registeredTooltips.remove(ruleKey) != null;
        if (removed) {
            LOGGER.debug("Removed tooltip for gamerule: {}", ruleKey);
        }
        return removed;
    }

    /**
     * <p>
     *     清除所有通过 API 注册的 tooltip<br>
     *     注意：这不会影响本地化文件和内置默认描述
     * </p>
     * <p>
     *     Clear all tooltips registered through this API<br>
     *     Note: This does not affect localization files and built-in default descriptions
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
