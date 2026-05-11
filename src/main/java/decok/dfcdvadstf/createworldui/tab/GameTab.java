package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.GuiCyclableButton;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

public class GameTab extends AbstractScreenTab {
    private GuiTextField worldNameField;
    private GuiCyclableButton gameModeButton;
    private GuiCyclableButton allowCheatsButton;
    private GuiCyclableButton difficultyButton;

    public GameTab() {
        super(100, "createworldui.tab.game");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);

        // Create world name text field
        // 创建世界名称输入框
        worldNameField = new GuiTextField(mc.fontRenderer,
                width / 2 - 104, height / 5, 208, 20);

        // 从TabManager获取世界名称
        String worldName = getWorldName();
        if ((worldName == null || worldName.trim().isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            // 使用默认的世界名称
            worldName = I18n.format("selectWorld.newWorld");
            tabManager.setWorldName(worldName);
        } else if (worldName == null || worldName.trim().isEmpty()) {
            // 如果启用了disableCreateButtonWhenWNIsBlank且世界名称为空，则保持为空
            worldName = "";
        }
        worldNameField.setText(worldName);
        worldNameField.setFocused(true);

        System.out.println("GameTab: Initializing with world name: " + worldName);

        // Create game mode button
        // 创建游戏模式按钮
        gameModeButton = new GuiCyclableButton(2, width / 2 - 104, height / 2,
                208, 20, this::getGameModeText, direction -> cycleGameMode());
        addButton(gameModeButton);

        // Create difficulty button
        // 创建难度按钮
        difficultyButton = new GuiCyclableButton(9, width / 2 - 104, height / 2 + 25,
                208, 20, this::getDifficultyText, direction -> {
            if (!getHardcore()) {
                cycleDifficulty();
            } else {
                hardcoreSetToHard();
            }
        });
        addButton(difficultyButton);

        // Create allow cheats button
        // 创建允许作弊按钮
        allowCheatsButton = new GuiCyclableButton(6, width / 2 - 104, height / 2 + 50,
                208, 20, this::getAllowCheatsText, direction -> {
            if (!getHardcore()) {
                tabManager.setAllowCheats(!getAllowCheats());
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
                tabManager.getParent().width / 2 - 104,
                tabManager.getParent().height / 5 - 13, 0xA0A0A0);

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
        if (difficultyButton != null) difficultyButton.enabled = !getHardcore();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // 循环按钮的逻辑已在创建时定义，无需在此处理
        // 此方法保留用于处理其他类型的按钮事件
        // Cycling buttons' logic is handled in the creation, no need to process here
        // This method is reserved for processing other types of button events
    }

    private String getGameModeText() {
        String mode = getGameMode();
        if (mode == null || mode.isEmpty()) {
            mode = "survival";
        }
        return I18n.format("selectWorld.gameMode") + ": " +
                I18n.format("selectWorld.gameMode." + mode);
    }

    private String getDifficultyText() {
        if (getHardcore()){
            return I18n.format("options.difficulty") + ": " +
                    I18n.format("options.difficulty.hardcore");
        } else {
            return I18n.format("options.difficulty") + ": " +
                    I18n.format(getDifficulty().getDifficultyResourceKey());
        }
    }

    private String getAllowCheatsText() {
        boolean allowCheats = getAllowCheats();
        boolean hardcore = getHardcore();
        boolean isOn = allowCheats && !hardcore;
        return I18n.format("selectWorld.allowCommands") + " " +
                (isOn ? I18n.format("options.on") : I18n.format("options.off"));
    }

    private void cycleGameMode() {
        String[] modes = {"survival", "creative", "hardcore", "adventure"};
        String currentMode = getGameMode();
        if (currentMode == null) currentMode = "survival";

        int currentIndex = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(currentMode)) {
                currentIndex = i;
                break;
            }
        }

        String newMode = modes[(currentIndex + 1) % modes.length];
        tabManager.setGameMode(newMode);
        tabManager.setHardcore("hardcore".equals(newMode));

        // 如果是硬核模式，禁用作弊和奖励箱；如果是创造模式，自动开启作弊
        if ("hardcore".equals(newMode)) {
            tabManager.setAllowCheats(false);
            tabManager.setBonusChest(false);
        } else if ("creative".equals(newMode)) {
            tabManager.setAllowCheats(true);
        }

        // 更新按钮显示
        if (allowCheatsButton != null) {
            allowCheatsButton.enabled = !getHardcore();
            allowCheatsButton.updateText();
        }
        if (difficultyButton != null) {
            difficultyButton.enabled = !getHardcore();
            difficultyButton.updateText();
        }
    }

    private void cycleDifficulty() {
        EnumDifficulty current = getDifficulty();
        int next = (current.getDifficultyId() + 1) % EnumDifficulty.values().length;
        tabManager.setDifficulty(EnumDifficulty.getDifficultyEnum(next));
    }

    private void hardcoreSetToHard() {
        EnumDifficulty difficulty = getDifficulty();
        int hcs2d = difficulty.getDifficultyId();
        tabManager.setDifficulty(EnumDifficulty.getDifficultyEnum(hcs2d));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        worldNameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        worldNameField.textboxKeyTyped(typedChar, keyCode);
        tabManager.setWorldName(worldNameField.getText());

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