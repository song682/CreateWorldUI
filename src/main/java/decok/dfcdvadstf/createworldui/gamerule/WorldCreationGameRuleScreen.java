package decok.dfcdvadstf.createworldui.gamerule;

import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import net.minecraft.client.gui.GuiScreen;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     创建世界界面打开的游戏规则编辑器。<br>
 *     保存目标：通过 {@link GameRuleApplier#setPendingGameRules(Map)} 将修改后的规则
 *     设置为"待应用规则"，在新世界创建时统一应用。此时尚无当前世界，不做即时应用。
 * </p>
 * <p>
 *     Game rule editor opened from the world-creation screen.<br>
 *     Save target: set the modified rules as "pending rules" via
 *     {@link GameRuleApplier#setPendingGameRules(Map)}, applied together when the new
 *     world is created. There is no current world here, so no live application is done.
 * </p>
 */
public class WorldCreationGameRuleScreen extends AbstractScreenGameRuleEditor {

    /**
     * @param parentScreen  父界面（创建世界界面） / parent screen (world-creation screen)
     * @param editableRules 可编辑的游戏规则映射（待应用规则） / editable game rule map (pending rules)
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
