package decok.dfcdvadstf.createworldui.integrations.datapack.ok;

import cpw.mods.fml.common.FMLCommonHandler;
import decok.dfcdvadstf.createworldui.Tags;
import decok.dfcdvadstf.createworldui.integrations.IntgrationBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ruiseki.okcore.data.DatapackManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     OKCore 数据包兼容层 —— 直接调用 OKCore 的 {@link DatapackManager} 公开 API，不做反射、
 *     不自行重实现其数据包格式：
 * </p>
 * <ul>
 *     <li>读取：{@code DatapackManager.INSTANCE.getDatapackRegistry()} —— 世界加载后由 OKCore
 *     注册的数据包（模组 JAR 包 + 世界/游戏目录数据包），与 OKCore 的 /datapack list 命令同源</li>
 *     <li>切换：{@code enableDatapack(name)} / {@code disableDatapack(name)} —— 直接改 OKCore 的
 *     实时状态（模组 JAR 包不可禁用，与命令行为一致）</li>
 *     <li>落盘：{@code saveDisabledPacksConfig(worldDir)} —— 复用 OKCore 自己的禁用清单写入
 *     （okcore_datapacks.txt）。OKCore 在 FMLServerAboutToStartEvent（早于 loadAllWorlds）读取
 *     该文件，故创建新世界时必须在启动集成服务器之前写入世界目录，见
 *     {@code MixinModernCreateWorld#modernWorldCreatingUI$beforeLaunchWorld}</li>
 * </ul>
 * <p>
 *     OKCore 为 compileOnly 依赖（运行时不保证存在），所有 OKCore 类引用都收敛在
 *     {@link Direct} 持有类里：只有在 {@link #isLoaded()} 为 true 时才会触碰它，凭类的惰性加载
 *     避免 NoClassDefFoundError，全程无反射。
 * </p>
 * <p>
 *     OKCore data pack compatibility layer — calls OKCore's public
 *     {@link DatapackManager} API directly: no reflection, no re-implementation of
 *     its pack format:
 * </p>
 * <ul>
 *     <li>Read: {@code getDatapackRegistry()} — the packs OKCore registered after a
 *     world load (mod-JAR packs + world/game-dir packs), same source as OKCore's
 *     /datapack list command</li>
 *     <li>Toggle: {@code enableDatapack(name)} / {@code disableDatapack(name)} —
 *     mutate OKCore's live state directly (mod-JAR packs cannot be disabled, same
 *     as the command)</li>
 *     <li>Persist: {@code saveDisabledPacksConfig(worldDir)} — reuse OKCore's own
 *     disabled-list writer (okcore_datapacks.txt). OKCore reads the file at
 *     FMLServerAboutToStartEvent (before loadAllWorlds), so when creating a new
 *     world it must be flushed into the world dir before the integrated server
 *     launches — see
 *     {@code MixinModernCreateWorld#modernWorldCreatingUI$beforeLaunchWorld}</li>
 * </ul>
 * <p>
 *     OKCore is a compileOnly dependency (possibly absent at runtime), so every
 *     OKCore class reference is isolated inside the {@link Direct} holder class:
 *     it is only touched when {@link #isLoaded()} is true — lazy class loading
 *     avoids NoClassDefFoundError, reflection-free throughout.
 * </p>
 */
public class OKCoreCompat {

    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":OKCoreCompat");

    /**
     * <p>
     *     界面是否改动过待落盘（任一 enable/disable 成功即置位）。创建世界时若置位，
     *     由 {@link #savePendingSelectionToWorldDir(String)} 复用 OKCore 的
     *     saveDisabledPacksConfig 写入新世界目录后复位。
     * </p>
     * <p>
     *     Whether the screen made any change pending a flush (set by any successful
     *     enable/disable). When a world is created, if set,
     *     {@link #savePendingSelectionToWorldDir(String)} flushes it via OKCore's
     *     saveDisabledPacksConfig into the new world dir and resets it.
     * </p>
     */
    private static boolean dirty = false;

    // ============================================================
    // 环境检测 / Environment
    // ============================================================

    /**
     * @return OKCore 是否已加载 / whether OKCore is loaded
     */
    public static boolean isLoaded() {
        return IntgrationBase.isOKCoreLoaded();
    }

    // ============================================================
    // 数据包信息（不暴露 OKCore 类型给界面层） / Pack info (no OKCore types leaked to the UI layer)
    // ============================================================

    /**
     * <p>
     *     数据包信息的值对象：把 OKCore 的 {@code DatapackManager.Datapack} 状态快照下来，
     *     使界面层不直接引用 OKCore 类型。
     * </p>
     * <p>
     *     Pack info value object: snapshots an OKCore {@code DatapackManager.Datapack}
     *     so the UI layer never references OKCore types directly.
     * </p>
     */
    public static class PackInfo {

        private final String name;
        private final boolean modJar;
        private final boolean enabled;

        public PackInfo(String name, boolean modJar, boolean enabled) {
            this.name = name;
            this.modJar = modJar;
            this.enabled = enabled;
        }

        /** @return 包名（文件夹名/JAR 文件名） / pack name (folder / jar file name) */
        public String getName() {
            return name;
        }

        /** @return 是否为模组 JAR 内置包（不可禁用） / whether this is a mod-JAR built-in pack */
        public boolean isModJar() {
            return modJar;
        }

        /** @return 是否已激活 / whether the pack is enabled */
        public boolean isEnabled() {
            return enabled;
        }
    }

    // ============================================================
    // 读取 / Reading
    // ============================================================

    /**
     * <p>
     *     读取 OKCore 已注册的数据包（世界加载后 OKCore 于 AboutToStart 阶段注册；
     *     本会话尚未加载过世界时为空列表）。
     * </p>
     * <p>
     *     Read the packs OKCore has registered (registered at the AboutToStart
     *     phase of a world load; empty before any world was loaded this session).
     * </p>
     *
     * @return 按名称排序的数据包列表 / name-sorted pack list
     */
    public static List<PackInfo> getRegisteredPacks() {
        if (!isLoaded()) {
            return new ArrayList<>();
        }
        return Direct.getRegisteredPacks();
    }

    // ============================================================
    // 切换（直接调用 OKCore） / Toggling (direct OKCore calls)
    // ============================================================

    /**
     * 激活数据包（直接调用 {@code DatapackManager.enableDatapack}）。
     * Enable a pack (direct call to {@code DatapackManager.enableDatapack}).
     *
     * @param name 包名 / pack name
     * @return 是否成功切换（未注册/已激活则 false） / whether the toggle succeeded
     */
    public static boolean enableDatapack(String name) {
        if (!isLoaded() || name == null) {
            return false;
        }
        boolean success = Direct.enableDatapack(name);
        if (success) {
            dirty = true;
        }
        return success;
    }

    /**
     * 停用数据包（直接调用 {@code DatapackManager.disableDatapack}，模组 JAR 包不可停用）。
     * Disable a pack (direct call to {@code DatapackManager.disableDatapack};
     * mod-JAR packs cannot be disabled).
     *
     * @param name 包名 / pack name
     * @return 是否成功切换（未注册/已停用/模组 JAR 包则 false） / whether the toggle succeeded
     */
    public static boolean disableDatapack(String name) {
        if (!isLoaded() || name == null) {
            return false;
        }
        boolean success = Direct.disableDatapack(name);
        if (success) {
            dirty = true;
        }
        return success;
    }

    // ============================================================
    // 落盘（复用 OKCore 的写入） / Persisting (reusing OKCore's own writer)
    // ============================================================

    /**
     * <p>
     *     若界面改动过数据包状态，在启动集成服务器之前把当前禁用清单写入新世界的
     *     {@code okcore_datapacks.txt}（直接调用 OKCore 的
     *     {@code saveDisabledPacksConfig}）。此时世界目录尚不存在，需先创建；
     *     写入后复位脏标记。
     * </p>
     * <p>
     *     If the screen changed any pack state, write the current disabled list into
     *     the new world's {@code okcore_datapacks.txt} before the integrated server
     *     launches (direct call to OKCore's {@code saveDisabledPacksConfig}). The
     *     world dir does not exist yet, so it is created here first; the dirty flag
     *     is reset afterwards.
     * </p>
     *
     * @param saveDirName 世界存档目录名（GuiCreateWorld 的 field_146336_i）
     *                    / the world save-dir name (GuiCreateWorld's field_146336_i)
     */
    public static void savePendingSelectionToWorldDir(String saveDirName) {
        if (!dirty || !isLoaded()) {
            return;
        }
        if (saveDirName == null || saveDirName.trim().isEmpty()) {
            return;
        }

        File worldDir = new File(FMLCommonHandler.instance().getSavesDirectory(), saveDirName);
        if (!worldDir.exists() && !worldDir.mkdirs()) {
            logger.error("Failed to create world dir for datapack config: {}", worldDir);
            return;
        }

        Direct.saveDisabledPacksConfig(worldDir);
        dirty = false;
        logger.info("Flushed datapack selection into {}", new File(worldDir, "okcore_datapacks.txt"));
    }

    // ============================================================
    // 直接调用持有类（仅 isLoaded() 时触碰） / Direct-call holder (touched only when isLoaded())
    // ============================================================

    /**
     * <p>
     *     OKCore 直接调用收敛处 —— 所有 {@code ruiseki.okcore} 类型引用只出现在本类。
     *     本类仅在 {@link #isLoaded()} 为 true 时被触碰，凭惰性类加载保证 OKCore
     *     缺席时不触发 NoClassDefFoundError；无反射。
     * </p>
     * <p>
     *     Where all direct OKCore calls live — every {@code ruiseki.okcore} type
     *     reference appears only in this class. It is touched only when
     *     {@link #isLoaded()} is true; lazy class loading keeps OKCore's absence
     *     free of NoClassDefFoundError. Reflection-free.
     * </p>
     */
    private static final class Direct {

        private Direct() {
        }

        /**
         * 读取注册表并快照为 PackInfo 列表（与 /datapack list 命令同源）。
         * Read the registry and snapshot it as a PackInfo list (same source as the
         * /datapack list command).
         */
        static List<PackInfo> getRegisteredPacks() {
            List<PackInfo> result = new ArrayList<>();

            Map<String, DatapackManager.Datapack> registry = DatapackManager.INSTANCE.getDatapackRegistry();
            for (DatapackManager.Datapack pack : registry.values()) {
                result.add(new PackInfo(pack.getName(), pack.isModJar(), pack.isEnabled()));
            }

            Collections.sort(result, new Comparator<PackInfo>() {
                @Override
                public int compare(PackInfo a, PackInfo b) {
                    return a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
                }
            });
            return result;
        }

        /** 直接调用 OKCore 的 enableDatapack。 Direct call to OKCore's enableDatapack. */
        static boolean enableDatapack(String name) {
            return DatapackManager.INSTANCE.enableDatapack(name);
        }

        /** 直接调用 OKCore 的 disableDatapack。 Direct call to OKCore's disableDatapack. */
        static boolean disableDatapack(String name) {
            return DatapackManager.INSTANCE.disableDatapack(name);
        }

        /** 直接调用 OKCore 的 saveDisabledPacksConfig。 Direct call to OKCore's saveDisabledPacksConfig. */
        static void saveDisabledPacksConfig(File worldDir) {
            DatapackManager.INSTANCE.saveDisabledPacksConfig(worldDir);
        }
    }
}
