package decok.dfcdvadstf.createworldui.ui.tab;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.catframe.ui.components.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.layouts.FrameLayout;
import decok.dfcdvadstf.catframe.ui.layouts.LayoutSettings;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyApplier;
import decok.dfcdvadstf.createworldui.integrations.difficulty.DifficultyLocker;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

/**
 * Game Settings Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的游戏设置标签页。</p>
 */
public class GameTab extends GridLayoutTab {
    private SimpleEditBox worldNameField;
    private CyclingButton<String> gameModeButton;
    private CyclingButton<Boolean> allowCheatsButton;
    private CyclingButton<EnumDifficulty> difficultyButton;
    private Button difficultyLockButton;
    private final IGuiCreateWorldAccess access;

    /**
     * Builds the tab once; the widgets live as long as the screen instance.
     * <p>一次性构建标签页；组件随屏幕实例存活。</p>
     */
    public GameTab(GuiCreateWorld screen) {
        super(Text.translatable("createworldui.tab.game"));
        this.access = (IGuiCreateWorldAccess) screen;

        // Match high-version GameTab: single column with row spacing
        layout.rowSpacing(8);

        int row = 0;

        // Create world name text field; edits are pushed back into the host screen
        // immediately through the component's onTextChanged hook.
        // 创建世界名输入框；编辑通过组件的 onTextChanged 钩子即时写回宿主界面。
        worldNameField = new SimpleEditBox(0, 0, 208, 20) {
            @Override
            protected void onTextChanged() {
                access.modernWorldCreatingUI$setWorldName(getText());
            }
        };
        String worldName = access.modernWorldCreatingUI$getWorldName();
        if ((worldName == null || worldName.trim().isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            worldName = I18n.format("selectWorld.newWorld");
            access.modernWorldCreatingUI$setWorldName(worldName);
        } else if (worldName == null || worldName.trim().isEmpty()) {
            worldName = "";
        }
        worldNameField.setText(worldName);
        // 26.1.2 GameTab: 208 宽名字框以 alignHorizontalCenter 居中在 210 宽的列中
        // 26.1.2 GameTab: the 208-wide name field centered via alignHorizontalCenter in
        // the 210-wide column
        layout.addChild(worldNameField, row++, 0, LayoutSettings::alignHorizontallyCenter);

        // Create game mode button
        String currentMode = access.modernWorldCreatingUI$getGameMode();
        if (currentMode == null || currentMode.isEmpty()) currentMode = "survival";

        gameModeButton = CyclingButton.<String>builder(
                        mode -> I18n.format("selectWorld.gameMode") + ": " + I18n.format("selectWorld.gameMode." + mode))
                .values("survival", "creative", "hardcore", "adventure")
                .initially(currentMode)
                .useVanillaTexture(true)
                // 26.1.2 GameTab: 游戏模式/难度/作弊按钮均为 210 宽
                // 26.1.2 GameTab: game mode / difficulty / cheats buttons are all 210 wide
                .build(0, 0, 210, 20, (button, mode) -> {
                    access.modernWorldCreatingUI$setGameMode(mode);
                    access.modernWorldCreatingUI$setHardcore("hardcore".equals(mode));

                    if ("hardcore".equals(mode)) {
                        access.modernWorldCreatingUI$setAllowCheats(false);
                        access.modernWorldCreatingUI$setBonusChest(false);
                    } else if ("creative".equals(mode)) {
                        access.modernWorldCreatingUI$setAllowCheats(true);
                    }

                    if (allowCheatsButton != null) {
                        allowCheatsButton.setActive(!"hardcore".equals(mode));
                        allowCheatsButton.setValue(access.modernWorldCreatingUI$getAllowCheats());
                    }
                    if (difficultyButton != null) {
                        difficultyButton.setActive(!"hardcore".equals(mode));
                    }
                });
        layout.addChild(gameModeButton, row++, 0);

        boolean showLock = (DifficultyLocker.hasIMCConfig()
            ? DifficultyLocker.getIMCShowLockButton()
            : DifficultyLocker.isLoaded()) && CreateWorldUI.config.lockDifficultyButton;
        // 26.1.2 难度按钮为 210 宽；显示锁按钮时收窄为 188（188 + 2 间距 + 20 锁按钮 = 210）
        // 26.1.2's difficulty button is 210 wide; narrowed to 188 when the lock button
        // is shown (188 + 2 gap + 20 lock = 210)
        int difficultyWidth = showLock ? 188 : 210;

        difficultyButton = CyclingButton.<EnumDifficulty>builder(d -> {
                    if (access.modernWorldCreatingUI$getHardcore()) {
                        return I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore");
                    }
                    return I18n.format("options.difficulty") + ": " + I18n.format(d.getDifficultyResourceKey());
                })
                .values(EnumDifficulty.values())
                .initially(DifficultyApplier.getSelectedDifficulty())
                .useVanillaTexture(true)
                .build(0, 0, difficultyWidth, 20, (button, diff) -> {
                    if (!access.modernWorldCreatingUI$getHardcore() && !DifficultyLocker.isDifficultyLocked(diff)) {
                        DifficultyApplier.setSelectedDifficulty(diff);
                    }
                });
        layout.addChild(difficultyButton, row, 0);

        // Create difficulty lock button via the compat layer — difficultylocker
        // (CatFrame-migrated) hands out its CatFrame lock button directly: no
        // reflection, no GuiButtonAdapter; click sound / hover / disabled states
        // are handled by the CatFrame base class.
        // 经兼容层创建难度锁定按钮 —— difficultylocker（已迁移 CatFrame）直接
        // 交出其 CatFrame 锁按钮：无反射、无 GuiButtonAdapter；点击音效/悬停/禁用
        // 状态由 CatFrame 基类统一处理。
        Button lockButton = null;
        if (showLock) {
            lockButton = DifficultyLocker.createLockButton(btn -> {
                EnumDifficulty currentDifficulty = difficultyButton.getValue();
                boolean newLockedState = !DifficultyLocker.isDifficultyLocked(currentDifficulty);
                DifficultyLocker.setDifficultyLocked(currentDifficulty, newLockedState);

                // IMC: 同步锁定状态变化给 difficultylocker
                // IMC: sync the lock-state change to difficultylocker
                DifficultyLocker.sendLockStateChange(currentDifficulty, newLockedState);

                if (newLockedState) {
                    difficultyButton.setActive(false);
                } else {
                    difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore());
                }
            });
            if (lockButton != null) {
                layout.addChild(lockButton, row, 1);
            }
        }
        this.difficultyLockButton = lockButton;
        row++;

        // Create allow cheats button
        allowCheatsButton = CyclingButton.<Boolean>builder(value -> {
                    boolean isOn = value && !access.modernWorldCreatingUI$getHardcore();
                    return I18n.format("selectWorld.allowCommands") + " " +
                            (isOn ? I18n.format("options.on") : I18n.format("options.off"));
                })
                .values(Boolean.TRUE, Boolean.FALSE)
                .initially(access.modernWorldCreatingUI$getAllowCheats())
                .useVanillaTexture(true)
                .build(0, 0, 210, 20, (button, value) -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setAllowCheats(value);
                    }
                });
        layout.addChild(allowCheatsButton, row++, 0);
    }

    /**
     * <p>
     *     Extra top spacing relative to GridLayoutTab's default 0.16666667F — kept from
     *     the previous tab architecture. {@code doLayout} is the only place CatFrame
     *     lets a single tab customize its alignment, hence this local override.
     * </p>
     * <p>
     *     相比 GridLayoutTab 默认 0.16666667F 的额外顶部间距 —— 沿袭旧标签页架构。
     *     {@code doLayout} 是 CatFrame 允许单个标签页自定义对齐的唯一位置，故在此本地覆写。
     * </p>
     */
    @Override
    public void doLayout(ScreenRectangle screenRectangle) {
        this.layout.arrangeElements();
        FrameLayout.alignInRectangle(this.layout, screenRectangle.x, screenRectangle.y,
                screenRectangle.width, screenRectangle.height, 0.5F, 0.22F);
    }

    /**
     * Refreshes dynamic states, tooltips and the field label before the host renders
     * this tab's children.
     * <p>在本标签页子组件渲染前刷新动态状态、tooltip 与输入框标签。</p>
     */
    public void prepareFrame() {
        // Update button states before rendering
        difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore()
            && !DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));

        if (difficultyLockButton != null) {
            boolean lockButtonLocked = DifficultyLocker.isDifficultyLocked(difficultyButton.getValue());
            // 同步锁按钮的锁定视觉与启用状态（HardCore 模式恒锁死，对方界面同规则）
            // Sync the lock button's locked visual and active state (hardcore is
            // always locked — same rule as the other mod's own screen)
            DifficultyLocker.setButtonLocked(difficultyLockButton, lockButtonLocked);
            difficultyLockButton.setActive(!access.modernWorldCreatingUI$getHardcore());
        }

        // 组件级 tooltip：由 extractRenderState 驱动的 WidgetTooltipHolder 自动泵动，帧末由 CatFrame 统一延迟绘制；
        // 每帧重建以反映世界名、游戏模式、难度锁定等动态状态
        // Component-level tooltip: auto-pumped by WidgetTooltipHolder via extractRenderState, drawn deferred by
        // CatFrame at end of frame; rebuilt each frame to reflect dynamic state (world name, mode, difficulty lock)
        String worldName = access.modernWorldCreatingUI$getWorldName();
        if (worldName == null || worldName.isEmpty()) {
            worldNameField.setTooltip(Tooltip.create(I18n.format("createworldui.hover.worldName.empty")));
        } else {
            worldNameField.setTooltip(Tooltip.create(I18n.format("createworldui.hover.worldName.filled", worldName)));
        }

        String mode = access.modernWorldCreatingUI$getGameMode();
        if (mode == null || mode.isEmpty()) mode = "survival";
        gameModeButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.gameMode." + mode)));

        String difficultyTip = I18n.format("createworldui.hover.difficulty");
        if (DifficultyLocker.isDifficultyLocked(difficultyButton.getValue())) {
            difficultyTip += "\n" + I18n.format("createworldui.hover.difficulty.locked");
        }
        difficultyButton.setTooltip(Tooltip.create(difficultyTip));

        if (difficultyLockButton != null) {
            boolean locked = DifficultyLocker.isDifficultyLocked(difficultyButton.getValue());
            difficultyLockButton.setTooltip(Tooltip.create(I18n.format(locked
                    ? "createworldui.hover.difficulty.unlockButton"
                    : "createworldui.hover.difficulty.lockButton")));
        }

        allowCheatsButton.setTooltip(Tooltip.create(I18n.format("createworldui.hover.allowCheats")));

        // Draw the label above the field — positioned relative to the component's layout position
        // 在输入框上方绘制标签——相对于组件的布局位置
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterName"),
                worldNameField.getX(), worldNameField.getY() - mc.fontRenderer.FONT_HEIGHT - 3, 0xA0A0A0);
    }

    public GuiEventListener getInitialFocus() {
        return worldNameField;
    }
}
