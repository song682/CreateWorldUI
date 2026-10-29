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
    public boolean showWorldNamePlaceHolder;
    public boolean disableCreateButtonWhenWNIsBlank;
    public boolean lockDifficultyButton;
    public boolean filterNonCreatableWorldTypes;
    public boolean enableExampleExperiment;
    public boolean enableDataPack;
    public boolean enableExperiment;

    public Config(File file) {
        configFile = new Configuration(file);

        configFile.addCustomCategoryComment("UI Management", "This is some options for you to custom your own modern UI.");
        configFile.addCustomCategoryComment("GameRule Editor", "Custom Game Rule Editor");
        configFile.addCustomCategoryComment("Experiment", "Some settings about the experiment.");

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
        
        showWorldNamePlaceHolder = configFile.getBoolean("showWorldNamePlaceHolder", "UI Management", false, 
            "Show a place-holder to gently reminds player to add a own world name");
        configFile.getCategory("ui management").get("showWorldNamePlaceHolder").setLanguageKey("config.option.showWorldNamePlaceHolder");
        
        disableCreateButtonWhenWNIsBlank = configFile.getBoolean("disableCreateButtonWhenWNIsBlank", "UI Management", false, 
            "Set True to disable the Create Button when world name is blank");
        configFile.getCategory("ui management").get("disableCreateButtonWhenWNIsBlank").setLanguageKey("config.option.disableCreateButtonWhenWNIsBlank");
        
        lockDifficultyButton = configFile.getBoolean("lockDifficultyButton", "UI Management", false, 
            "Set True to lock the difficulty button");
        configFile.getCategory("ui management").get("lockDifficultyButton").setLanguageKey("config.option.lockDifficultyButton");
        
        filterNonCreatableWorldTypes = configFile.getBoolean("filterNonCreatableWorldTypes", "UI Management", true, 
            "Set True to hide world types that cannot be created (e.g. Default_1_1)");
        configFile.getCategory("ui management").get("filterNonCreatableWorldTypes").setLanguageKey("config.option.filterNonCreatableWorldTypes");

        enableDataPack = configFile.getBoolean("enableDataPack", "UI Management", false, "Set True to enable the data pack sections");
        configFile.getCategory("ui management").get("enableDataPack").setLanguageKey("config.option.enableDataPack");

        enableExperiment = configFile.getBoolean("enableExperiment", "UI Management", false, "Set True to enable the experiment content screen");
        configFile.getCategory("ui management").get("enableExperiment").setLanguageKey("config.option.enableExperiment");

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

        // Experimental Content category options
        // 实验性内容的分类配置项
        configFile.getCategory("experiment").setLanguageKey("config.option.experiment");
        enableExampleExperiment = configFile.getBoolean("enableExampleExperiment", "Experiment", false,
                "Set True to show the built-in example toggle on the experiments screen (demo only, does nothing)");
        configFile.getCategory("experiment").get("enableExampleExperiment").setLanguageKey("config.option.enableExampleExperiment");
    }

    public void saveConfigurationFile() {
        configFile.save();
    }

}