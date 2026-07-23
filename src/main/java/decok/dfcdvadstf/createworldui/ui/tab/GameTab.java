package decok.dfcdvadstf.createworldui.ui.tab;

import cpw.mods.fml.common.event.FMLInterModComms;
import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.components.CyclingButton;
import decok.dfcdvadstf.catframe.ui.components.SimpleEditBox;
import decok.dfcdvadstf.catframe.ui.components.GuiButtonAdapter;
import decok.dfcdvadstf.catframe.ui.tab.GridLayoutTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;
import decok.dfcdvadstf.createworldui.api.TooltipProvider;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Game Settings Tab with GridLayout-based layout.
 * <p>使用 GridLayout 布局的游戏设置标签页。</p>
 */
public class GameTab extends GridLayoutTab implements TooltipProvider {
    private SimpleEditBox worldNameField;
    private CyclingButton<String> gameModeButton;
    private CyclingButton<Boolean> allowCheatsButton;
    private CyclingButton<EnumDifficulty> difficultyButton;
    private GuiButtonAdapter difficultyLockButton;
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public GameTab() {
        super(100, Text.translatable("createworldui.tab.game"));
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;

        // Match high-version GameTab: single column with row spacing
        layout.rowSpacing(8);
        // More top spacing from tab bar / 增大标签栏间距
        verticalAlignment = 0.22F;

        int row = 0;

        // Create world name text field
        worldNameField = new SimpleEditBox(0, 0, 208, 20);
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
        if (showLock) {
            try {
                Class<?> guiLockButtonClass = Class.forName("decok.dfcdvadstf.difficultyLocker.GuiLockButton");
                Constructor<?> constructor = guiLockButtonClass.getConstructor(int.class, int.class, int.class, boolean.class);
                final GuiButton lockButton = (GuiButton) constructor.newInstance(10, 0, 0, false);
                difficultyLockButton = new GuiButtonAdapter(lockButton) {
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
                .useVanillaTexture(true)
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

        // Draw labels on top - position relative to the component's layout position
        // 在顶层绘制标签——相对于组件的布局位置
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterName"),
                worldNameField.getX(), worldNameField.getY() - mc.fontRenderer.FONT_HEIGHT - 3, 0xA0A0A0);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // All actions handled via OnPress callbacks
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Delegate to GridLayoutTab which forwards to all Components
        // 委托给 GridLayoutTab，它会转发到所有 Component
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (difficultyLockButton != null) {
            difficultyLockButton.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Forward key to all Components via GridLayoutTab, then sync world name
        // 转发按键到所有 Component，然后同步世界名称
        super.keyTyped(typedChar, keyCode);
        access.modernWorldCreatingUI$setWorldName(worldNameField.getText());
    }

    /**
     * Maps the hovered component to its vanilla-style tooltip lines.
     * <p>将悬停的组件映射到其原版风格 tooltip 文本行。</p>
     */
    @Override
    public List<String> getTooltipLines(int mouseX, int mouseY) {
        // World name field: prompt to enter a name, or echo the current name
        // 世界名称输入框：提示输入名称，或回显当前名称
        if (worldNameField != null && worldNameField.isVisible()
                && worldNameField.isMouseOver(mouseX, mouseY)) {
            String worldName = access.modernWorldCreatingUI$getWorldName();
            if (worldName == null || worldName.isEmpty()) {
                return singleLine(I18n.format("createworldui.hover.worldName.empty"));
            }
            return singleLine(I18n.format("createworldui.hover.worldName.filled", worldName));
        }

        // Game mode: describe the currently selected mode
        // 游戏模式：描述当前选中的模式
        if (gameModeButton != null && gameModeButton.isVisible()
                && gameModeButton.isMouseOver(mouseX, mouseY)) {
            String mode = access.modernWorldCreatingUI$getGameMode();
            if (mode == null || mode.isEmpty()) mode = "survival";
            return singleLine(I18n.format("createworldui.hover.gameMode." + mode));
        }

        // Difficulty: base description, plus a locked note when this difficulty is locked
        // 难度：基础说明；若当前难度被锁定则追加一行提示
        if (difficultyButton != null && difficultyButton.isVisible()
                && difficultyButton.isMouseOver(mouseX, mouseY)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("createworldui.hover.difficulty"));
            if (DifficultyLocker.isDifficultyLocked(difficultyButton.getValue())) {
                lines.add(I18n.format("createworldui.hover.difficulty.locked"));
            }
            return lines;
        }

        // Difficulty lock button: click-to-lock / click-to-unlock depending on state
        // 难度锁定按钮：根据状态显示“点击锁定 / 点击解锁”
        if (difficultyLockButton != null && difficultyLockButton.isVisible()
                && difficultyLockButton.isMouseOver(mouseX, mouseY) && difficultyButton != null) {
            boolean locked = DifficultyLocker.isDifficultyLocked(difficultyButton.getValue());
            return singleLine(I18n.format(locked
                    ? "createworldui.hover.difficulty.unlockButton"
                    : "createworldui.hover.difficulty.lockButton"));
        }

        // Allow cheats
        // 允许作弊
        if (allowCheatsButton != null && allowCheatsButton.isVisible()
                && allowCheatsButton.isMouseOver(mouseX, mouseY)) {
            return singleLine(I18n.format("createworldui.hover.allowCheats"));
        }

        return null;
    }

    private static List<String> singleLine(String line) {
        List<String> lines = new ArrayList<>(1);
        lines.add(line);
        return lines;
    }
}
