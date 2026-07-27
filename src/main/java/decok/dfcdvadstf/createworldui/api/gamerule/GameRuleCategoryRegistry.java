package decok.dfcdvadstf.createworldui.api.gamerule;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * <p>
 *     游戏规则分类注册 API<br>
 *     提供游戏规则的分类管理功能<br>
 *     用于在 GameRule Editor 中按分类组织和显示游戏规则<br>
 *     分类键本身就是本地化键，分类标题文字由调用方模组在自己的 lang 文件中提供
 * </p>
 * <p>
 *     GameRule Category Registration API<br>
 *     Provides category management for game rules<br>
 *     Used to organize and display game rules by category in GameRule Editor<br>
 *     The category key itself is a localization key; the category header text is
 *     supplied by the calling mod's own lang files
 * </p>
 * <p>
 *     分类显示示例：<br>
 *     📁 世界 (World)<br>
 *     &nbsp;&nbsp;├─ doFireTick<br>
 *     &nbsp;&nbsp;└─ doTileDrops<br>
 *     📁 生物 (Mobs)<br>
 *     &nbsp;&nbsp;├─ doMobSpawning<br>
 *     &nbsp;&nbsp;├─ doMobLoot<br>
 *     &nbsp;&nbsp;└─ mobGriefing
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleCategoryRegistry {

    private static final Logger LOGGER = LogManager.getLogger("GameRuleCategoryRegistry");

    // 存储分类映射（分类本地化键 -> 该分类下的规则列表）
    // Storage for category mappings (category localization key -> list of rules in that category)
    private static final Map<String, List<String>> categoryKeyToRules = new LinkedHashMap<>();

    // 存储规则到分类的反向映射（规则键 -> 分类本地化键）
    // Reverse mapping for rule to category (rule key -> category localization key)
    private static final Map<String, String> ruleNameToCategoryKey = new HashMap<>();

    // 原版游戏规则的默认分类
    // Default categories for vanilla game rules
    private static final Map<String, List<String>> VANILLA_DEFAULT_CATEGORIES = new LinkedHashMap<>();

    static {
        // 初始化原版游戏规则的默认分类
        // Initialize default categories for vanilla game rules
        
        // 世界相关 / World related
        List<String> worldRules = new ArrayList<>();
        worldRules.add("doFireTick");
        worldRules.add("doTileDrops");
        worldRules.add("doDaylightCycle");
        VANILLA_DEFAULT_CATEGORIES.put("gamerule.category.world", worldRules);
        
        // 生物相关 / Mobs related
        List<String> mobsRules = new ArrayList<>();
        mobsRules.add("doMobSpawning");
        mobsRules.add("doMobLoot");
        mobsRules.add("mobGriefing");
        VANILLA_DEFAULT_CATEGORIES.put("gamerule.category.mobs", mobsRules);
        
        // 玩家相关 / Player related
        List<String> playerRules = new ArrayList<>();
        playerRules.add("naturalRegeneration");
        playerRules.add("keepInventory");
        VANILLA_DEFAULT_CATEGORIES.put("gamerule.category.player", playerRules);
        
        // 聊天/命令相关 / Chat/Command related
        List<String> chatRules = new ArrayList<>();
        chatRules.add("commandBlockOutput");
        VANILLA_DEFAULT_CATEGORIES.put("gamerule.category.chat", chatRules);
    }

    // 标记是否已初始化默认分类
    // Flag to track if default categories have been initialized
    private static boolean defaultsInitialized = false;

    /**
     * <p>
     *     初始化默认分类（懒加载）<br>
     *     Initialize default categories (lazy loading)
     * </p>
     */
    private static void initializeDefaults() {
        if (defaultsInitialized) {
            return;
        }

        // 将默认分类添加到主映射中
        // Add default categories to main map
        for (Map.Entry<String, List<String>> entry : VANILLA_DEFAULT_CATEGORIES.entrySet()) {
            String categoryKey = entry.getKey();
            List<String> ruleNames = entry.getValue();

            if (!categoryKeyToRules.containsKey(categoryKey)) {
                categoryKeyToRules.put(categoryKey, new ArrayList<>());
            }
            
            // 添加规则列表
            for (String ruleName : ruleNames) {
                categoryKeyToRules.get(categoryKey).add(ruleName);
                ruleNameToCategoryKey.put(ruleName, categoryKey);
            }
        }

        defaultsInitialized = true;
        LOGGER.debug("Initialized default game rule categories");
    }

    /**
     * <p>
     *     创建一个新的分类<br>
     *     Create a new category
     * </p>
     *
     * @param categoryKey 分类键名，本身即本地化键（如 "gamerule.category.world"）/ Category key, itself a localization key (e.g., "gamerule.category.world")
     * @param ruleNames 该分类下的游戏规则列表 / List of game rules in this category
     */
    public static void createCategory(String categoryKey, List<String> ruleNames) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            LOGGER.warn("Cannot create category with null or empty key");
            return;
        }
        if (ruleNames == null) {
            LOGGER.warn("Cannot create category with null rule list");
            return;
        }

        initializeDefaults();

        categoryKeyToRules.put(categoryKey, new ArrayList<>(ruleNames));

        // 更新反向映射
        // Update reverse mapping
        for (String ruleName : ruleNames) {
            if (ruleName != null && !ruleName.isEmpty()) {
                ruleNameToCategoryKey.put(ruleName, categoryKey);
            }
        }

        LOGGER.debug("Created category: {} with {} rules", categoryKey, ruleNames.size());
    }

    /**
     * <p>
     *     将游戏规则添加到指定分类<br>
     *     Add a game rule to a specific category
     * </p>
     *
     * @param categoryKey 分类键名 / Category key
     * @param ruleName 游戏规则名称 / Game rule name
     */
    public static void addRuleToCategory(String categoryKey, String ruleName) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            LOGGER.warn("Cannot add rule to null or empty category key");
            return;
        }
        if (ruleName == null || ruleName.isEmpty()) {
            LOGGER.warn("Cannot add null or empty rule name");
            return;
        }

        initializeDefaults();

        if (!categoryKeyToRules.containsKey(categoryKey)) {
            categoryKeyToRules.put(categoryKey, new ArrayList<>());
        }

        List<String> rules = categoryKeyToRules.get(categoryKey);
        if (!rules.contains(ruleName)) {
            rules.add(ruleName);
            ruleNameToCategoryKey.put(ruleName, categoryKey);
            LOGGER.debug("Added rule {} to category {}", ruleName, categoryKey);
        }
    }

    /**
     * <p>
     *     批量添加游戏规则到指定分类<br>
     *     Add multiple game rules to a specific category
     * </p>
     *
     * @param categoryKey 分类键名 / Category key
     * @param ruleNames 游戏规则名称列表 / List of game rule names
     */
    public static void addRulesToCategory(String categoryKey, List<String> ruleNames) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            LOGGER.warn("Cannot add rules to null or empty category key");
            return;
        }
        if (ruleNames == null) {
            LOGGER.warn("Cannot add null rule list");
            return;
        }

        initializeDefaults();

        if (!categoryKeyToRules.containsKey(categoryKey)) {
            categoryKeyToRules.put(categoryKey, new ArrayList<>());
        }

        List<String> rules = categoryKeyToRules.get(categoryKey);
        for (String ruleName : ruleNames) {
            if (ruleName != null && !ruleName.isEmpty() && !rules.contains(ruleName)) {
                rules.add(ruleName);
                ruleNameToCategoryKey.put(ruleName, categoryKey);
            }
        }

        LOGGER.debug("Added {} rules to category {}", ruleNames.size(), categoryKey);
    }

    /**
     * <p>
     *     获取指定分类下的所有游戏规则<br>
     *     Get all game rules in a specific category
     * </p>
     *
     * @param categoryKey 分类键名 / Category key
     * @return 游戏规则列表（只读）/ List of game rules (read-only)
     */
    public static List<String> getRulesInCategory(String categoryKey) {
        initializeDefaults();

        List<String> rules = categoryKeyToRules.get(categoryKey);
        if (rules == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(rules);
    }

    /**
     * <p>
     *     获取游戏规则所属的分类<br>
     *     Get the category that a game rule belongs to
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 分类键名，如果没有找到则返回 null / Category key, or null if not found
     */
    public static String getCategoryForRule(String ruleName) {
        initializeDefaults();
        return ruleNameToCategoryKey.get(ruleName);
    }

    /**
     * <p>
     *     获取所有分类的键名（按注册顺序）<br>
     *     Get all category keys (in registration order)
     * </p>
     *
     * @return 分类键名列表（只读）/ List of category keys (read-only)
     */
    public static List<String> getAllCategories() {
        initializeDefaults();
        return Collections.unmodifiableList(new ArrayList<>(categoryKeyToRules.keySet()));
    }

    /**
     * <p>
     *     获取分类的显示名称<br>
     *     分类键本身即本地化键，直接解析其翻译<br>
     *     优先级：本地化键的翻译 > 分类键名原样返回
     * </p>
     * <p>
     *     Get the display name for a category<br>
     *     The category key itself is a localization key and is resolved directly<br>
     *     Priority: translation of the localization key > category key as-is
     * </p>
     *
     * @param categoryKey 分类键名 / Category key
     * @return 显示名称 / Display name
     */
    public static String getCategoryDisplayName(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return categoryKey;
        }

        // 分类键本身即本地化键，尝试解析其翻译
        // The category key itself is a localization key; try to resolve its translation
        String translated = net.minecraft.client.resources.I18n.format(categoryKey);
        if (translated != null && !translated.isEmpty() && !translated.equals(categoryKey)) {
            return translated;
        }

        // 翻译缺失时原样返回键名，便于调用方发现漏写 lang 条目
        // Return the key as-is when the translation is missing, so callers can spot missing lang entries
        return categoryKey;
    }

    /**
     * <p>
     *     从分类中移除游戏规则<br>
     *     Remove a game rule from its category
     * </p>
     *
     * @param ruleName 游戏规则名称 / Game rule name
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeRuleFromCategory(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            return false;
        }

        initializeDefaults();

        String categoryKey = ruleNameToCategoryKey.get(ruleName);
        if (categoryKey == null) {
            return false;
        }

        List<String> rules = categoryKeyToRules.get(categoryKey);
        if (rules != null) {
            boolean removed = rules.remove(ruleName);
            if (removed) {
                ruleNameToCategoryKey.remove(ruleName);
                LOGGER.debug("Removed rule {} from category {}", ruleName, categoryKey);
            }
            return removed;
        }

        return false;
    }

    /**
     * <p>
     *     移除整个分类及其所有规则<br>
     *     Remove an entire category and all its rules
     * </p>
     *
     * @param categoryKey 分类键名 / Category key
     * @return 如果成功移除则返回 true / True if successfully removed
     */
    public static boolean removeCategory(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return false;
        }

        initializeDefaults();

        List<String> rules = categoryKeyToRules.remove(categoryKey);
        if (rules != null) {
            // 清除反向映射
            // Clear reverse mapping
            for (String ruleName : rules) {
                ruleNameToCategoryKey.remove(ruleName);
            }
            LOGGER.debug("Removed category: {} with {} rules", categoryKey, rules.size());
            return true;
        }

        return false;
    }

    /**
     * <p>
     *     清除所有自定义分类（保留默认分类）<br>
     *     Clear all custom categories (keep default categories)
     * </p>
     */
    public static void clearCustomCategories() {
        // 重新初始化为默认状态
        // Reinitialize to default state
        categoryKeyToRules.clear();
        ruleNameToCategoryKey.clear();
        defaultsInitialized = false;
        initializeDefaults();
        LOGGER.info("Cleared all custom categories, restored defaults");
    }

    /**
     * <p>
     *     清除所有分类（包括默认分类）<br>
     *     Clear all categories (including defaults)
     * </p>
     */
    public static void clearAllCategories() {
        categoryKeyToRules.clear();
        ruleNameToCategoryKey.clear();
        defaultsInitialized = false;
        LOGGER.info("Cleared all categories");
    }

    /**
     * <p>
     *     获取已注册的分类数量<br>
     *     Get the number of registered categories
     * </p>
     *
     * @return 分类数量 / Number of categories
     */
    public static int getCategoryCount() {
        initializeDefaults();
        return categoryKeyToRules.size();
    }

    /**
     * <p>
     *     获取完整的分类映射（只读）<br>
     *     Get the complete category mapping (read-only)
     * </p>
     *
     * @return 分类映射的只读副本 / Read-only copy of category mapping
     */
    public static Map<String, List<String>> getAllCategoriesMap() {
        initializeDefaults();
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : categoryKeyToRules.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return copy;
    }
}
