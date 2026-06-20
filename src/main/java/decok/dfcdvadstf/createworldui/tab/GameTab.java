package decok.dfcdvadstf.createworldui.tab;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.EditBox;
import decok.dfcdvadstf.catframe.ui.components.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

import java.util.HashMap;
import java.util.Map;

/**
 * Game Settings Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的游戏设置标签页。</p>
 */
public class GameTab extends GridLayoutTab {
    private EditBox worldNameField;
    private CyclingButton<String> gameModeButton;
    private CyclingButton<Boolean> allowCheatsButton;
    private CyclingButton<EnumDifficulty> difficultyButton;
    private GuiButtonAdapter difficultyLockButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public GameTab() {
        super(100, "createworldui.tab.game");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;

        int row = 0;

        // Create world name text field
        worldNameField = new EditBox(0, 0, 208, 20);
        String worldName = access.modernWorldCreatingUI$getWorldName();
        if ((worldName == null || worldName.trim().isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            worldName = I18n.format("selectWorld.newWorld");
            access.modernWorldCreatingUI$setWorldName(worldName);
        } else if (worldName == null || worldName.trim().isEmpty()) {
            worldName = "";
        }
        worldNameField.setText(worldName);
        worldNameField.setFocused(true);
        layout.addChild(worldNameField, row++, 0);

        // Create game mode button
        String currentMode = access.modernWorldCreatingUI$getGameMode();
        if (currentMode == null || currentMode.isEmpty()) currentMode = "survival";

        gameModeButton = CyclingButton.<String>builder(
                        mode -> I18n.format("selectWorld.gameMode") + ": " + I18n.format("selectWorld.gameMode." + mode))
                .values("survival", "creative", "hardcore", "adventure")
                .initially(currentMode)
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

        // Create difficulty button
        int difficultyWidth = 188;

        difficultyButton = CyclingButton.<EnumDifficulty>builder(d -> {
                    if (access.modernWorldCreatingUI$getHardcore()) {
                        return I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore");
                    }
                    return I18n.format("options.difficulty") + ": " + I18n.format(d.getDifficultyResourceKey());
                })
                .values(EnumDifficulty.values())
                .initially(DifficultyApplier.getSelectedDifficulty())
                .build(0, 0, difficultyWidth, 20, (button, diff) -> {
                    if (!access.modernWorldCreatingUI$getHardcore() && !DifficultyLocker.isDifficultyLocked(diff)) {
                        DifficultyApplier.setSelectedDifficulty(diff);
                    }
                });
        layout.addChild(difficultyButton, row, 0);

        // Create difficulty lock button (reflection-based, from external mod)
        if (DifficultyLocker.isLoaded() && CreateWorldUI.config.lockDifficultyButton) {
            try {
                Class<?> guiLockButtonClass = Class.forName("decok.dfcdvadstf.difficultyLocker.GuiLockButton");
                java.lang.reflect.Constructor<?> constructor = guiLockButtonClass.getConstructor(int.class, int.class, int.class, boolean.class);
                final GuiButton lockButton = (GuiButton) constructor.newInstance(10, 0, 0, false);
                difficultyLockButton = new GuiButtonAdapter(lockButton) {
                    @Override
                    public void mouseClicked(int mx, int my, int mb) {
                        if (active && visible && getDelegate().mousePressed(Minecraft.getMinecraft(), mx, my)) {
                            try {
                                java.lang.reflect.Method isLockedMethod = getDelegate().getClass().getMethod("isLocked");
                                boolean isCurrentlyLocked = (boolean) isLockedMethod.invoke(getDelegate());
                                boolean newLockedState = !isCurrentlyLocked;

                                java.lang.reflect.Method setLockedMethod = getDelegate().getClass().getMethod("setLocked", boolean.class);
                                setLockedMethod.invoke(getDelegate(), newLockedState);

                                EnumDifficulty currentDifficulty = difficultyButton.getValue();
                                DifficultyLocker.setDifficultyLocked(currentDifficulty, newLockedState);

                                if (newLockedState) {
                                    difficultyButton.setActive(false);
                                } else {
                                    difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore());
                                }

                                Minecraft.getMinecraft().getSoundHandler().playSound(
                                    net.minecraft.client.audio.PositionedSoundRecord.func_147674_a(
                                        new net.minecraft.util.ResourceLocation("gui.button.press"), 1.0F));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                layout.addChild(difficultyLockButton, row, 1);
            } catch (Exception e) {
                e.printStackTrace();
                difficultyLockButton = null;
            }
        } else {
            difficultyLockButton = null;
        }
        row++;

        // Create allow cheats button
        allowCheatsButton = CyclingButton.<Boolean>builder(value -> {
                    boolean isOn = value && !access.modernWorldCreatingUI$getHardcore();
                    return I18n.format("selectWorld.allowCommands") + " " +
                            (isOn ? I18n.format("options.on") : I18n.format("options.off"));
                })
                .values(Boolean.TRUE, Boolean.FALSE)
                .initially(access.modernWorldCreatingUI$getAllowCheats())
                .build(0, 0, 208, 20, (button, value) -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setAllowCheats(value);
                    }
                });
        layout.addChild(allowCheatsButton, row++, 0);

        super.initGui(tabManager, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // Update button states before rendering
        if (difficultyButton != null) {
            difficultyButton.setActive(!access.modernWorldCreatingUI$getHardcore()
                && !DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));
        }

        if (difficultyLockButton != null && difficultyButton != null) {
            try {
                java.lang.reflect.Method method = difficultyLockButton.getDelegate().getClass().getMethod("setLocked", boolean.class);
                method.invoke(difficultyLockButton.getDelegate(),
                    DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Render all components
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw labels on top
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterName"),
                guiCreateWorld.width / 2 - 104,
                guiCreateWorld.height / 5 - 13, 0xA0A0A0);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // All actions handled via OnPress callbacks
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        worldNameField.mouseClicked(mouseX, mouseY, mouseButton);
        if (difficultyLockButton != null) {
            difficultyLockButton.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        worldNameField.keyTyped(typedChar, keyCode);
        access.modernWorldCreatingUI$setWorldName(worldNameField.getText());
    }
}
