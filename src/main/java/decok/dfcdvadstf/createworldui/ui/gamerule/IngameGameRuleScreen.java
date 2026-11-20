package decok.dfcdvadstf.createworldui.ui.gamerule;

import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     Game rule editor opened in-game.<br>
 *     Save target: immediately apply the changed rules to the current world via
 *     {@link GameRuleMonitorNSetter#setGamerule(World, String, Object)}, and update the
 *     copy of original values accordingly.
 * </p>
 */
public class IngameGameRuleScreen extends AbstractScreenGameRuleEditor {

    /**
     * @param parentScreen  parent screen (usually null in-game)
     * @param editableRules editable game rule map (current world rules)
     */
    public IngameGameRuleScreen(GuiScreen parentScreen, Map<String, String> editableRules) {
        super(parentScreen, editableRules);
    }

    @Override
    protected void persistChanges(Map<String, String> result, Set<String> changed) {
        World currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld == null || changed.isEmpty()) {
            return;
        }

        int appliedCount = 0;
        for (String ruleName : changed) {
            String newValue = result.get(ruleName);
            if (newValue != null && GameRuleMonitorNSetter.setGamerule(currentWorld, ruleName, newValue)) {
                appliedCount++;
            }
        }

        // Update the copy of original values to reflect new values
        editableRules.putAll(result);
        LOGGER.info("Applied {} game rules to current world.", appliedCount);
    }
}
