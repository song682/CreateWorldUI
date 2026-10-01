package decok.dfcdvadstf.createworldui.ui.tab;

import cpw.mods.fml.common.event.FMLInterModComms;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.components.Tooltip;
import decok.dfcdvadstf.catframe.ui.components.events.GuiEventListener;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Game Settings Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的游戏设置标签页。</p>
 */
public class GameTab extends CreateWorldTab {
    private SimpleEditBox worldNameField;
    private CyclingButton<String> gameModeButton;
    private CyclingButton<Boolean> allowCheatsButton;
    private CyclingButton<EnumDifficulty> difficultyButton;
    private GuiButtonAdapter difficultyLockButton;
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
        // More top spacing from tab bar / 增大标签栏间距
        verticalAlignment = 0.22F;

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
        layout.addChild(worldNameField, row++, 0);

        // Create game mode button
        String currentMode = access.modernWorldCreatingUI$getGameMode();
        if (currentMode == null || currentMode.isEmpty()) currentMode = "survival";

        gameModeButton = CyclingButton.<String>builder(
                        mode -> I18n.format("selectWorld.gameMode") + ": " + I18n.format("selectWorld.gameMode." + mode))
                .values("survival", "creative", "hardcore", "adventure")
                .initially(currentMode)
                .useVanillaTexture(true)
                .build(0, 0, 208, 20, (button, mode) -> {
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
        int difficultyWidth = showLock ? 188 : 208;

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

        // Create difficulty lock button (reflection-based, from external mod)
        GuiButtonAdapter lockAdapter = null;
        if (showLock) {
            try {
                Class<?> guiLockButtonClass = Class.forName("decok.dfcdvadstf.difficultyLocker.GuiLockButton");
                Constructor<?> constructor = guiLockButtonClass.getConstructor(int.class, int.class, int.class, boolean.class);
                final GuiButton lockButton = (GuiButton) constructor.newInstance(10, 0, 0, false);
                lockAdapter = new GuiButtonAdapter(lockButton) {
                    @Override
                    public void mouseClicked(int mx, int my, int mb) {
                        if (active && visible && getDelegate().mousePressed(Minecraft.getMinecraft(), mx, my)) {
                            try {
                                Method isLockedMethod = getDelegate().getClass().getMethod("isLocked");
                                boolean isCurrentlyLocked = (boolean) isLockedMethod.invoke(getDelegate());
                                boolean newLockedState = !isCurrentlyLocked;

                                Method setLockedMethod = getDelegate().getClass().getMethod("setLocked", boolean.class);
                                setLockedMethod.invoke(getDelegate(), newLockedState);

                                EnumDifficulty currentDifficulty = difficultyButton.getValue();
                                DifficultyLocker.setDifficultyLocked(currentDifficulty, newLockedState);

                                // IMC: 发送 runtime 消息通知 DifficultyLocker
                                NBTTagCompound imcTag = new NBTTagCompound();
                                imcTag.setInteger("difficultyId", currentDifficulty.getDifficultyId());
                                imcTag.setBoolean("locked", newLockedState);
                                FMLInterModComms.sendRuntimeMessage(CreateWorldUI.class, "difficultylocker", "lock_state_change", imcTag);

                                if (newLockedState) {
                                    difficultyButton.setActive(false);
                                } else {
                                    difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore());
                                }

                                Minecraft.getMinecraft().getSoundHandler().playSound(
                                    PositionedSoundRecord.func_147674_a(
                                        new ResourceLocation("gui.button.press"), 1.0F));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                layout.addChild(lockAdapter, row, 1);
            } catch (Exception e) {
                e.printStackTrace();
                lockAdapter = null;
            }
        }
        this.difficultyLockButton = lockAdapter;
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
                .build(0, 0, 208, 20, (button, value) -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setAllowCheats(value);
                    }
                });
        layout.addChild(allowCheatsButton, row++, 0);
    }

    /**
     * Refreshes dynamic states, tooltips and the field label before the host renders
     * this tab's children.
     * <p>在本标签页子组件渲染前刷新动态状态、tooltip 与输入框标签。</p>
     */
    @Override
    public void prepareFrame() {
        // Update button states before rendering
        difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore()
            && !DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));

        if (difficultyLockButton != null) {
            try {
                java.lang.reflect.Method method = difficultyLockButton.getDelegate().getClass().getMethod("setLocked", boolean.class);
                method.invoke(difficultyLockButton.getDelegate(),
                    DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    @Override
    public GuiEventListener getInitialFocus() {
        return worldNameField;
    }
}
