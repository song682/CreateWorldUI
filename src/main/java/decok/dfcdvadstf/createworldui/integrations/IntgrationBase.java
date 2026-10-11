package decok.dfcdvadstf.createworldui.integrations;

import cpw.mods.fml.common.Loader;
import decok.dfcdvadstf.createworldui.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IntgrationBase {
    private static Logger logger = LogManager.getLogger(Tags.NAME + "/ compat");

    public static boolean isOKCoreLoaded() {
        boolean loaded = Loader.isModLoaded("okcore");
        if (loaded) logger.info("OK Core Loaded");
        return loaded;
    }

    public static boolean isDataBackLoaded() {
        boolean loaded = Loader.isModLoaded("databack");
        if (loaded) logger.info("DataBack Loaded");
        return loaded;
    }

    public static boolean isDifficultyLockerLoaded() {
        boolean loaded = Loader.isModLoaded("difficultylocker");
        if (loaded) logger.info("ModernDifficultyLocker Loaded");
        return loaded;
    }
}
