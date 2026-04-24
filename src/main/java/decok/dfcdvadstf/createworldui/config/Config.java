package decok.dfcdvadstf.createworldui.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config {
    public final Configuration configFile;

    public boolean enableResetButton;
    public boolean gameruleEdit;
    public boolean igGameruleEdit;
    public boolean changedRulesInChatHighLighted;
    public boolean highlightModifiedRulesInGUI;
    public boolean topTabCharatorModernWhite;
    public boolean enableOtherMoreTabButton;
    public boolean showWorldNamePlaceHolder;
    public boolean disableCreateButtonWhenWNIsBlank;

    public Config(File file) {
        configFile = new Configuration(file);

        configFile.addCustomCategoryComment("UI Management", "This is some options for you to custom your own modern UI.");
        configFile.addCustomCategoryComment("GameRule Editor", "Custom Game Rule Editor");

        configFile.load();
        UIOPtions();
        saveConfigurationFile();
    }

    public void UIOPtions(){
        // UI Management category options
        // UI管理分类配置项
        configFile.getCategory("ui management").setLanguageKey("config.button.UIM");
        
        topTabCharatorModernWhite = configFile.getBoolean("topTabCharatorModernWhite", "UI Management", false, 
            "Set True to enable the white color for the top tab text (require ArchaicFix)");
        configFile.getCategory("ui management").get("topTabCharatorModernWhite").setLanguageKey("config.option.topTabCharatorModernWhite");
        
        gameruleEdit = configFile.getBoolean("gameruleEdit", "UI Management", true, 
            "Enable the Gamerule Editor");
        configFile.getCategory("ui management").get("gameruleEdit").setLanguageKey("config.option.gameruleEdit");
        
        enableOtherMoreTabButton = configFile.getBoolean("enableOtherMoreTabButton", "UI Management", false, 
            "Enable unused modern feature button.");
        configFile.getCategory("ui management").get("enableOtherMoreTabButton").setLanguageKey("config.option.enableOtherMoreTabButton");
        
        showWorldNamePlaceHolder = configFile.getBoolean("showWorldNamePlaceHolder", "UI Management", false, 
            "Show a place-holder to gently reminds player to add a own world name");
        configFile.getCategory("ui management").get("showWorldNamePlaceHolder").setLanguageKey("config.option.showWorldNamePlaceHolder");
        
        disableCreateButtonWhenWNIsBlank = configFile.getBoolean("disableCreateButtonWhenWNIsBlank", "UI Management", false, 
            "Set True to disable the Create Button when world name is blank");
        configFile.getCategory("ui management").get("disableCreateButtonWhenWNIsBlank").setLanguageKey("config.option.disableCreateButtonWhenWNIsBlank");
        
        // GameRule Editor category options
        // 游戏规则编辑器分类配置项
        configFile.getCategory("gamerule editor").setLanguageKey("config.button.GRE");
        
        enableResetButton = configFile.getBoolean("enableReloadButton", "GameRule Editor", false, 
            "Set True to enable the Reload Button");
        configFile.getCategory("gamerule editor").get("enableReloadButton").setLanguageKey("config.option.enableReloadButton");
        
        igGameruleEdit = configFile.getBoolean("igGameruleEdit", "GameRule Editor", false, 
            "Enable gamerule editor but in-game");
        configFile.getCategory("gamerule editor").get("igGameruleEdit").setLanguageKey("config.option.igGameruleEdit");
        
        changedRulesInChatHighLighted = configFile.getBoolean("changedRulesInChatHighLighted", "GameRule Editor", false, 
            "Set True to highlight changed rules name in yellow color in chat notification");
        configFile.getCategory("gamerule editor").get("changedRulesInChatHighLighted").setLanguageKey("config.option.changedRulesInChatHighLighted");
        
        highlightModifiedRulesInGUI = configFile.getBoolean("highlightModifiedRulesInGUI", "GameRule Editor", true, 
            "Set True to highlight modified rules with yellow color in GUI");
        configFile.getCategory("gamerule editor").get("highlightModifiedRulesInGUI").setLanguageKey("config.option.highlightModifiedRulesInGUI");
    }

    public void saveConfigurationFile() {
        configFile.save();
    }

}