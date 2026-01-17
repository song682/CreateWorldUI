package decok.dfcdvadstf.createworldui.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class Config {
    public final Configuration configFile;

    public boolean enableResetButton;
    public boolean gameruleEdit;
    public boolean igGameruleEdit;
    public boolean topTabCharatorLegacyYellow;
    public boolean enableOtherMoreTabButton;
    public boolean showWorldNamePlaceHolder;
    public boolean disableCreateButtonWhenWNIsBlank;
    public String paddingColor;
    public String customLineColorTop;
    public String customLineColorDown;

    public Config(File file) {
        configFile = new Configuration(file);

        configFile.addCustomCategoryComment("UI Management", "This is some options for you to custom your own modern UI.");
        configFile.addCustomCategoryComment("GameRule Editor", "Custom Game Rule Editor");

        configFile.load();
        UIOPtions();
        saveConfigurationFile();
    }

    public void UIOPtions(){
        enableResetButton = configFile.getBoolean("enableReloadButton", "GameRule Editor", false, "Set True to enable the Reload Button");
        gameruleEdit = configFile.getBoolean("gameruleEdit", "UI Management", true, "Enable the Gamerule Editor");
        igGameruleEdit = configFile.getBoolean("igGameruleEdit", "GameRule Editor", false, "Enable gamerule editor but in-game");
        enableOtherMoreTabButton = configFile.getBoolean("enableOtherMoreTabButton", "UI Management", false, "Enable unused modern feature button.");
        showWorldNamePlaceHolder = configFile.getBoolean("showWorldNamePlaceHolder", "UI Management", false, "Show a place-holder to gently reminds player to add a own world name");
    }

    public void saveConfigurationFile() {
        configFile.save();
    }

}
