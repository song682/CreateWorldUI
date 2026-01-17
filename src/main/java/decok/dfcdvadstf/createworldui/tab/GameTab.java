package decok.dfcdvadstf.createworldui.tab;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;

public class GameTab extends AbstractScreenTab {
    private GuiTextField worldNameField;
    private GuiButton gameModeButton;
    private GuiButton allowCheatsButton;
    private GuiButton difficultyButton;

    public GameTab() {
        super(100, "createworldui.tab.game");
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);

        // 创建世界名称输入框
        worldNameField = new GuiTextField(mc.fontRenderer,
                width / 2 - 104, height / 5, 208, 20);

        // 从TabManager获取世界名称
        String worldName = getWorldName();
        if ((worldName == null || worldName.isEmpty()) && !CreateWorldUI.config.disableCreateButtonWhenWNIsBlank) {
            // 使用默认的世界名称
            worldName = I18n.format("selectWorld.newWorld");
            tabManager.setWorldName(worldName);
        }
        worldNameField.setText(worldName);
        worldNameField.setFocused(true);

        System.out.println("GameTab: Initializing with world name: " + worldName);

        // 创建游戏模式按钮
        gameModeButton = new GuiButton(2, width / 2 - 104, height / 2,
                208, 20, getGameModeText());
        addButton(gameModeButton);

        // 创建难度按钮
        difficultyButton = new GuiButton(9, width / 2 - 104, height / 2 + 25,
                208, 20, getDifficultyText());
        addButton(difficultyButton);

        // 创建允许作弊按钮
        allowCheatsButton = new GuiButton(6, width / 2 - 104, height / 2 + 50,
                208, 20, getAllowCheatsText());
        addButton(allowCheatsButton);

        // 初始隐藏所有按钮，TabManager会根据当前标签页显示
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // 绘制标签文本
        mc.fontRenderer.drawString(I18n.format("selectWorld.enterName"),
                tabManager.getParent().width / 2 - 104,
                tabManager.getParent().height / 5 - 13, 0xA0A0A0);

        // 绘制输入框
        worldNameField.drawTextBox();

        // 如果输入框为空且没有焦点，显示提示文本
        if (CreateWorldUI.config.showWorldNamePlaceHolder && worldNameField.getText().isEmpty() && !worldNameField.isFocused()) {
            String placeholder = I18n.format("createworldui.placeholder.worldName");
            int x = worldNameField.xPosition + 4;
            int y = worldNameField.yPosition + (worldNameField.height - 8) / 2;
            mc.fontRenderer.drawStringWithShadow(placeholder, x, y, 0x808080);
        }

        // 更新按钮文本
        gameModeButton.displayString = getGameModeText();
        difficultyButton.displayString = getDifficultyText();
        allowCheatsButton.displayString = getAllowCheatsText();

        // 根据硬核模式更新允许作弊按钮状态
        allowCheatsButton.enabled = !getHardcore();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 2: // 游戏模式
                cycleGameMode();
                break;
            case 6: // 允许作弊
                if (!getHardcore()) {
                    tabManager.setAllowCheats(!getAllowCheats());
                }
                break;
            case 9: // 难度
                cycleDifficulty();
                break;
        }
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
        return I18n.format("options.difficulty") + ": " +
                I18n.format(getDifficulty().getDifficultyResourceKey());
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

        // 如果是硬核模式，禁用作弊和奖励箱
        if ("hardcore".equals(newMode)) {
            tabManager.setAllowCheats(false);
            tabManager.setBonusChest(false);
        }

        // 更新按钮显示
        if (allowCheatsButton != null) {
            allowCheatsButton.enabled = !getHardcore();
        }
    }

    private void cycleDifficulty() {
        EnumDifficulty current = getDifficulty();
        int next = (current.getDifficultyId() + 1) % EnumDifficulty.values().length;
        tabManager.setDifficulty(EnumDifficulty.getDifficultyEnum(next));
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
                // 如果文本为空或是默认提示文本，则禁用按钮
                if (text.isEmpty() || text.equals(I18n.format("createworldui.placeholder.worldName"))) {
                    button.enabled = false;
                } else {
                    button.enabled = true;
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