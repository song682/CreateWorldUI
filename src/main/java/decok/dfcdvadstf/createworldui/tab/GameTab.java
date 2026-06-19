package decok.dfcdvadstf.createworldui.tab;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.catframe.ui.GuiCyclableButton;
import decok.dfcdvadstf.catframe.ui.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import decok.dfcdvadstf.createworldui.api.DifficultyLocker;
import decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;

public class GameTab extends AbstractScreenTab {
    private GuiTextField worldNameField;
    private GuiCyclableButton<String> gameModeButton;
    private GuiCyclableButton<Boolean> allowCheatsButton;
    private GuiCyclableButton<EnumDifficulty> difficultyButton;
    private Object difficultyLockButton; // 使用原始模组的按钮
    private GuiCreateWorld guiCreateWorld;
    private IGuiCreateWorldAccess access;

    public GameTab() {
        super(100, "createworldui.tab.game");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        guiCreateWorld = (GuiCreateWorld) tabManager.getScreen();
        access = (IGuiCreateWorldAccess) guiCreateWorld;
        super.initGui(tabManager, width, height);

        // Create world name text field
        // 创建世界名称输入框
        worldNameField = new GuiTextField(mc.fontRenderer,
                width / 2 - 104, height / 5, 208, 20);

        // 从TabManager获取世界名称
        String worldName = access.modernWorldCreatingUI$getWorldName();
        if ((worldName == null || worldName.trim().isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            // 使用默认的世界名称
            worldName = I18n.format("selectWorld.newWorld");
            access.modernWorldCreatingUI$setWorldName(worldName);
        } else if (worldName == null || worldName.trim().isEmpty()) {
            // 如果启用了disableCreateButtonWhenWNIsBlank且世界名称为空，则保持为空
            worldName = "";
        }
        worldNameField.setText(worldName);
        worldNameField.setFocused(true);

        System.out.println("GameTab: Initializing with world name: " + worldName);

        // Create game mode button
        // 创建游戏模式按钮
        String currentMode = access.modernWorldCreatingUI$getGameMode();
        if (currentMode == null || currentMode.isEmpty()) currentMode = "survival";

        gameModeButton = GuiCyclableButton.<String>builder(
                        mode -> I18n.format("selectWorld.gameMode") + ": " + I18n.format("selectWorld.gameMode." + mode))
                .values("survival", "creative", "hardcore", "adventure")
                .initially(currentMode)
                .build(2, width / 2 - 104, height / 2, 208, 20, (button, mode) -> {
                    access.modernWorldCreatingUI$setGameMode(mode);
                    access.modernWorldCreatingUI$setHardcore("hardcore".equals(mode));

                    if ("hardcore".equals(mode)) {
                        access.modernWorldCreatingUI$setAllowCheats(false);
                        access.modernWorldCreatingUI$setBonusChest(false);
                    } else if ("creative".equals(mode)) {
                        access.modernWorldCreatingUI$setAllowCheats(true);
                    }

                    // Update dependent buttons
                    if (allowCheatsButton != null) {
                        allowCheatsButton.enabled = !"hardcore".equals(mode);
                        allowCheatsButton.setValue(access.modernWorldCreatingUI$getAllowCheats());
                    }
                    if (difficultyButton != null) {
                        difficultyButton.enabled = !"hardcore".equals(mode);
                        difficultyButton.updateText();
                    }
                });
        addButton(gameModeButton);

        // Create difficulty button (with lock button support)
        // 创建难度按钮（带锁定按钮支持）
        int difficultyX = width / 2 - 104;
        int difficultyY = height / 2 + 25;
        int difficultyWidth = 188; // 减少20像素，给锁定按钮留空间
        
        difficultyButton = GuiCyclableButton.<EnumDifficulty>builder(d -> {
                    if (access.modernWorldCreatingUI$getHardcore()) {
                        return I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore");
                    }
                    return I18n.format("options.difficulty") + ": " + I18n.format(d.getDifficultyResourceKey());
                })
                .values(EnumDifficulty.values())
                .initially(DifficultyApplier.getSelectedDifficulty())
                .build(9, difficultyX, difficultyY, difficultyWidth, 20, (button, diff) -> {
                    if (!access.modernWorldCreatingUI$getHardcore() && !DifficultyLocker.isDifficultyLocked(diff)) {
                        DifficultyApplier.setSelectedDifficulty(diff);
                    }
                });
        addButton(difficultyButton);
        
        // Create difficulty lock button (only if ModernDifficultyLocker is loaded and config is enabled)
        // 创建难度锁定按钮（仅当ModernDifficultyLocker加载且配置启用时）
        if (DifficultyLocker.isLoaded() && CreateWorldUI.config.lockDifficultyButton) {
            try {
                Class<?> guiLockButtonClass = Class.forName("decok.dfcdvadstf.difficultyLocker.GuiLockButton");
                java.lang.reflect.Constructor<?> constructor = guiLockButtonClass.getConstructor(int.class, int.class, int.class, boolean.class);
                difficultyLockButton = constructor.newInstance(10, difficultyX + difficultyWidth + 2, difficultyY, false);
                addButton((net.minecraft.client.gui.GuiButton) difficultyLockButton);
            } catch (Exception e) {
                e.printStackTrace();
                difficultyLockButton = null;
            }
        } else {
            difficultyLockButton = null;
        }

        // Create allow cheats button
        // 创建允许作弊按钮
        allowCheatsButton = GuiCyclableButton.<Boolean>builder(value -> {
                    boolean isOn = value && !access.modernWorldCreatingUI$getHardcore();
                    return I18n.format("selectWorld.allowCommands") + " " +
                            (isOn ? I18n.format("options.on") : I18n.format("options.off"));
                })
                .values(Boolean.TRUE, Boolean.FALSE)
                .initially(access.modernWorldCreatingUI$getAllowCheats())
                .build(6, width / 2 - 104, height / 2 + 50, 208, 20, (button, value) -> {
                    if (!access.modernWorldCreatingUI$getHardcore()) {
                        access.modernWorldCreatingUI$setAllowCheats(value);
                    }
                });
        addButton(allowCheatsButton);

        // Initially hide all buttons; TabManager will show them based on the active tab
        // 初始隐藏所有按钮，TabManager会根据当前标签页显示
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // 绘制标签文本
        // Draw label text
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterName"),
                guiCreateWorld.width / 2 - 104,
                guiCreateWorld.height / 5 - 13, 0xA0A0A0);

        // 绘制输入框
        // Draw text field (including placeholder)
        worldNameField.drawTextBox();

        // 如果输入框为空且没有焦点，显示提示文本
        // If text field is empty and not focused, display placeholder text
        if (CreateWorldUI.config.showWorldNamePlaceHolder && worldNameField.getText().isEmpty() && !worldNameField.isFocused()) {
            String placeholder = I18n.format("createworldui.placeholder.worldName");
            int x = worldNameField.xPosition + 4;
            int y = worldNameField.yPosition + (worldNameField.height - 8) / 2;
            mc.fontRenderer.drawStringWithShadow(placeholder, x, y, 0x808080);
        }

        // 更新按钮文本
        // Update button text
        if (gameModeButton != null) gameModeButton.updateText();
        if (difficultyButton != null) difficultyButton.updateText();
        if (allowCheatsButton != null) allowCheatsButton.updateText();

        // 根据硬核模式更新允许作弊按钮以及难度状态
        // Update allow cheats button and difficulty status based on hardcore mode
        // 根据硬核模式更新允许作弊按钮
        if (difficultyButton != null) difficultyButton.enabled = !access.modernWorldCreatingUI$getHardcore() && !DifficultyLocker.isDifficultyLocked(difficultyButton.getValue());
        
        // 更新锁定按钮状态
        if (difficultyLockButton != null && difficultyButton != null) {
            try {
                java.lang.reflect.Method method = difficultyLockButton.getClass().getMethod("setLocked", boolean.class);
                method.invoke(difficultyLockButton, DifficultyLocker.isDifficultyLocked(difficultyButton.getValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // 处理难度锁定按钮
        if (button == difficultyLockButton) {
            try {
                java.lang.reflect.Method isLockedMethod = difficultyLockButton.getClass().getMethod("isLocked");
                boolean isCurrentlyLocked = (boolean) isLockedMethod.invoke(difficultyLockButton);
                boolean newLockedState = !isCurrentlyLocked;
                
                java.lang.reflect.Method setLockedMethod = difficultyLockButton.getClass().getMethod("setLocked", boolean.class);
                setLockedMethod.invoke(difficultyLockButton, newLockedState);
                
                EnumDifficulty currentDifficulty = difficultyButton.getValue();
                DifficultyLocker.setDifficultyLocked(currentDifficulty, newLockedState);
                
                // 当锁定时，禁用难度按钮
                if (newLockedState) {
                    difficultyButton.enabled = false;
                } else {
                    difficultyButton.enabled = !access.modernWorldCreatingUI$getHardcore();
                }
                
                // 播放按钮音效
                button.func_146113_a(mc.getSoundHandler());
                
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        
        // 循环按钮的逻辑已在创建时定义，无需在此处理
        // 此方法保留用于处理其他类型的按钮事件
        // Cycling buttons' logic is handled in the creation, no need to process here
        // This method is reserved for processing other types of button events
    }



    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        worldNameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        worldNameField.textboxKeyTyped(typedChar, keyCode);
        access.modernWorldCreatingUI$setWorldName(worldNameField.getText());

        // 更新创建按钮状态
        updateCreateButtonState();
    }

    private void updateCreateButtonState() {
        // 查找创建世界按钮并更新状态
        for (GuiButton button : tabButtons) {
            if (button.id == 0) {
                String text = worldNameField.getText().trim();
                // 如果启用了disableCreateButtonWhenWNIsBlank配置，则检查世界名称是否为空
                if (CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
                    // 如果文本为空，则禁用按钮
                    button.enabled = !text.isEmpty();
                } else {
                    // 如果文本为空或是默认提示文本，则禁用按钮
                    if (text.isEmpty() || text.equals(I18n.format("createworldui.placeholder.worldName"))) {
                        button.enabled = false;
                    } else {
                        button.enabled = true;
                    }
                }
                break;
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // 当标签页变为可见时，确保输入框获得焦点
            worldNameField.setFocused(true);
        }
    }
}