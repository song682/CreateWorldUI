package decok.dfcdvadstf.createworldui.ui.gamerule;

import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import net.minecraft.client.gui.GuiScreen;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     Game rule editor opened from the world-creation screen.<br>
 *     Save target: set the modified rules as "pending rules" via
 *     {@link GameRuleApplier#setPendingGameRules(Map)}, applied together when the new
 *     world is created. There is no current world here, so no live application is done.
 * </p>
 */
public class WorldCreationGameRuleScreen extends AbstractScreenGameRuleEditor {

    /**
     * @param parentScreen  parent screen (world-creation screen)
     * @param editableRules editable game rule map (pending rules)
     */
    public WorldCreationGameRuleScreen(GuiScreen parentScreen, Map<String, String> editableRules) {
        super(parentScreen, editableRules);
    }

    @Override
    protected void persistChanges(Map<String, String> result, Set<String> changed) {
        GameRuleApplier.setPendingGameRules(result);
        LOGGER.info("Saved {} modified game rules to pendingGameRules.", result.size());
    }
}
