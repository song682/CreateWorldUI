package decok.dfcdvadstf.createworldui.ui.gamerule;

import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     游戏内打开的游戏规则编辑器。<br>
 *     保存目标：通过 {@link GameRuleMonitorNSetter#setGamerule(World, String, Object)}
 *     将变化的规则立即应用到当前世界，并同步更新原始值副本。
 * </p>
 * <p>
 *     Game rule editor opened in-game.<br>
 *     Save target: immediately apply the changed rules to the current world via
 *     {@link GameRuleMonitorNSetter#setGamerule(World, String, Object)}, and update the
 *     copy of original values accordingly.
 * </p>
 */
public class IngameGameRuleScreen extends AbstractScreenGameRuleEditor {

    /**
     * @param parentScreen  父界面（游戏内通常为 null） / parent screen (usually null in-game)
     * @param editableRules 可编辑的游戏规则映射（当前世界规则） / editable game rule map (current world rules)
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

        // 更新原始值副本以反映新值 / Update the copy of original values to reflect new values
        editableRules.putAll(result);
        LOGGER.info("Applied {} game rules to current world.", appliedCount);
    }
}
