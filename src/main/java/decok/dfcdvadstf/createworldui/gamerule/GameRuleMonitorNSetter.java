package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class GameRuleMonitorNSetter {

    private static final Logger LOGGER = LogManager.getLogger(Tags.NAME + ":GameruleMonitorAndSetter");

    /**
     * Game rule value container class with all possible types
     */

    public static class GameruleValue {
        public final String stringValue;
        public final boolean booleanValue;
        public final int intValue;
        public final double doubleValue;

        public GameruleValue(String stringValue, boolean booleanValue, int intValue, double doubleValue) {
            this.stringValue = stringValue;
            this.booleanValue = booleanValue;
            this.intValue = intValue;
            this.doubleValue = doubleValue;
        }

        @Override
        public String toString() {
            return String.format("String: %s, Boolean: %b, Int: %d, Double: %.2f",
                    stringValue, booleanValue, intValue, doubleValue);
        }

        /**
         * Get the most appropriate value type representation.
         * @return int, double, boolean, string
         */
        public Object getOptimalValue() {
            if (stringValue.matches("-?\\d+")) {
                return intValue;
            }
            else if (stringValue.matches("-?\\d+\\.\\d+")) {
                return doubleValue;
            }
            else if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return booleanValue;
            }
            else {
                return stringValue;
            }
        }
    }

    /**
     * Get all game rules with all types of values
     * @param world World object
     * @return Map containing all game rule names and complete values
     */
    public static Map<String, GameruleValue> getAllGamerules(World world) {
        Map<String, GameruleValue> gamerules = new HashMap<>();

        if (world == null) {
            LOGGER.warn("World object is null, returning empty gamerule map");
            return gamerules;
        }

        GameRules gameRules = world.getGameRules();
        String[] ruleNames = gameRules.getRules();

        for (String ruleName : ruleNames) {
            GameruleValue value = getGamerule(world, ruleName);
            if (value != null) {
                gamerules.put(ruleName, value);
            }
        }

        LOGGER.debug("Retrieved {} gamerules from world", gamerules.size());
        return gamerules;
    }

    /**
     * Get complete value of specific game rule
     * @param world World object
     * @param ruleName Rule name
     * @return Complete game rule value, null if rule doesn't exist
     */
    public static GameruleValue getGamerule(World world, String ruleName) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot get gamerule: {}", ruleName);
            return null;
        }

        if (!world.getGameRules().hasRule(ruleName)) {
            LOGGER.debug("Gamerule does not exist: {}", ruleName);
            return null;
        }

        GameRules gameRules = world.getGameRules();
        String stringValue = gameRules.getGameRuleStringValue(ruleName);
        boolean booleanValue = gameRules.getGameRuleBooleanValue(ruleName);

        int intValue = 0;
        double doubleValue = 0.0;

        try {
            java.lang.reflect.Field field = GameRules.class.getDeclaredField("theGameRules");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.TreeMap<String, Object> rulesMap = (java.util.TreeMap<String, Object>) field.get(gameRules);

            Object valueObj = rulesMap.get(ruleName);
            if (valueObj != null) {
                java.lang.reflect.Field intField = valueObj.getClass().getDeclaredField("valueInteger");
                java.lang.reflect.Field doubleField = valueObj.getClass().getDeclaredField("valueDouble");

                intField.setAccessible(true);
                doubleField.setAccessible(true);

                intValue = intField.getInt(valueObj);
                doubleValue = doubleField.getDouble(valueObj);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve numeric values for gamerule {} via reflection: {}", ruleName, e.getMessage());
            try {
                intValue = Integer.parseInt(stringValue);
            } catch (NumberFormatException e1) {
                // Keep default value 0
            }
            try {
                doubleValue = Double.parseDouble(stringValue);
            } catch (NumberFormatException e2) {
                // Keep default value 0.0
            }
        }

        return new GameruleValue(stringValue, booleanValue, intValue, doubleValue);
    }

    /**
     * Set game rule value
     * @param world World object
     * @param ruleName Rule name
     * @param value New value
     * @return True if successful
     */
    public static boolean setGamerule(World world, String ruleName, Object value) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot set gamerule: {}", ruleName);
            return false;
        }

        try {
            String stringValue;
            if (value instanceof Boolean) {
                stringValue = value.toString();
            } else if (value instanceof Integer) {
                stringValue = value.toString();
            } else if (value instanceof Double) {
                stringValue = value.toString();
            } else if (value instanceof String) {
                stringValue = (String) value;
            } else {
                stringValue = String.valueOf(value);
            }

            world.getGameRules().setOrCreateGameRule(ruleName, stringValue);
            LOGGER.debug("Successfully set gamerule {} to value: {}", ruleName, stringValue);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to set gamerule {} to value {}: {}", ruleName, value, e.getMessage());
            return false;
        }
    }

    /**
     * Add new game rule
     * @param world World object
     * @param ruleName Rule name
     * @param defaultValue Default value
     * @return True if successful
     */
    public static boolean addGamerule(World world, String ruleName, Object defaultValue) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot add gamerule: {}", ruleName);
            return false;
        }

        if (world.getGameRules().hasRule(ruleName)) {
            LOGGER.debug("Gamerule already exists: {}", ruleName);
            return false;
        }

        try {
            String stringValue = String.valueOf(defaultValue);
            world.getGameRules().addGameRule(ruleName, stringValue);
            LOGGER.debug("Successfully added new gamerule {} with default value: {}", ruleName, stringValue);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to add gamerule {} with default value {}: {}", ruleName, defaultValue, e.getMessage());
            return false;
        }
    }

    /**
     * Check if game rule exists
     * @param world World object
     * @param ruleName Rule name
     * @return True if exists
     */
    public static boolean hasGamerule(World world, String ruleName) {
        boolean exists = world != null && world.getGameRules().hasRule(ruleName);
        LOGGER.debug("Gamerule {} exists: {}", ruleName, exists);
        return exists;
    }

    /**
     * Get optimal type representation for all game rules
     * @param world World object
     * @return Map containing rule names and optimal type values
     */
    public static Map<String, Object> getOptimalGameruleValues(World world) {
        Map<String, Object> result = new HashMap<>();
        Map<String, GameruleValue> allGamerules = getAllGamerules(world);

        for (Map.Entry<String, GameruleValue> entry : allGamerules.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getOptimalValue());
        }

        LOGGER.debug("Retrieved optimal values for {} gamerules", result.size());
        return result;
    }

    /**
     * Log all game rules (for debugging)
     * @param world World object
     */
    public static void logAllGamerules(World world) {
        Map<String, GameruleValue> gamerules = getAllGamerules(world);

        LOGGER.info("=== All Game Rules (Complete Information) ===");
        for (Map.Entry<String, GameruleValue> entry : gamerules.entrySet()) {
            LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
        }
        LOGGER.info("=============================================");

        LOGGER.info("=== All Game Rules (Optimal Types) ===");
        Map<String, Object> optimalValues = getOptimalGameruleValues(world);
        for (Map.Entry<String, Object> entry : optimalValues.entrySet()) {
            LOGGER.info("{}: {} ({})", entry.getKey(), entry.getValue(), entry.getValue().getClass().getSimpleName());
        }
        LOGGER.info("======================================");
    }
}