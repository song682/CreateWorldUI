package decok.dfcdvadstf.createworldui.integrations.difficulty;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.event.FMLInterModComms;
import decok.dfcdvadstf.catframe.ui.components.Button;
import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.Tags;
import decok.dfcdvadstf.createworldui.integrations.IntgrationBase;
import decok.dfcdvadstf.difficultyLocker.CatFrameLockButton;
import decok.dfcdvadstf.difficultyLocker.WorldDifficultyData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 *     ModernDifficultyLocker 兼容层 —— 直接调用其 {@code WorldDifficultyData} /
 *     {@code CatFrameLockButton} 公开 API，不做反射、不自行重实现其存档格式，
 *     同时保留双向 InterModCommunicate（IMC）作为跨 mixin 顺序的权威同步通道：
 * </p>
 * <ul>
 *     <li>直达调用：{@code WorldDifficultyData.getInstance()} 的 setLocked /
 *     setLockedDifficulty —— 新世界启动集成服务器时直接写入其内存状态，见
 *     {@code MixinIntegratedServer#createWorldUI$onLoadAllWorlds}</li>
 *     <li>IMC 接收：init 期 {@code difficultylocker_config}（配置快照，
 *     {@code CreateWorldUI#onIMC}）；运行期 {@code world_lock_applied}（既有世界
 *     锁定回执，{@code IMCRuntimeHandler} 每秒轮询）</li>
 *     <li>IMC 发送：{@code lock_state_change}（锁按钮点击）；{@code world_created}
 *     （新世界启动）—— 对方 mixin 同在 loadAllWorlds TAIL 注入，其 loadWorldData
 *     可能晚于本侧直达调用而重置状态，IMC 由对方轮询补写，保证最终一致</li>
 *     <li>锁按钮：对方已迁移到 CatFrame，其 {@code CatFrameLockButton} 本身就是
 *     CatFrame {@link Button} —— 经 {@link Direct} 构造后以 CatFrame 类型交给界面层，
 *     界面层不引用任何 difficultylocker 类型</li>
 * </ul>
 * <p>
 *     ModernDifficultyLocker 为 compileOnly 依赖（运行时不保证存在），所有
 *     {@code decok.dfcdvadstf.difficultyLocker} 类型引用都收敛在 {@link Direct}
 *     持有类里：只有在 {@link #isLoaded()} 为 true 时才会触碰它，凭类的惰性加载
 *     避免 NoClassDefFoundError，全程无反射。
 * </p>
 * <p>
 *     ModernDifficultyLocker compatibility layer — calls its public
 *     {@code WorldDifficultyData} / {@code CatFrameLockButton} API directly:
 *     no reflection, no re-implementation of its save format, while keeping
 *     both-direction InterModCommunicate (IMC) as the authoritative sync
 *     channel across mixin ordering:
 * </p>
 * <ul>
 *     <li>Direct: {@code WorldDifficultyData.getInstance()} setLocked /
 *     setLockedDifficulty — written straight into its in-memory state when a
 *     new world's integrated server launches, see
 *     {@code MixinIntegratedServer#createWorldUI$onLoadAllWorlds}</li>
 *     <li>IMC receive: init-time {@code difficultylocker_config} (config
 *     snapshot, {@code CreateWorldUI#onIMC}); runtime {@code world_lock_applied}
 *     (receipt for an existing world's applied lock, polled once a second by
 *     {@code IMCRuntimeHandler})</li>
 *     <li>IMC send: {@code lock_state_change} (lock button press);
 *     {@code world_created} (new world launch) — the other mod's mixin also
 *     injects at loadAllWorlds TAIL, and its loadWorldData may run AFTER our
 *     direct call and reset the state; the IMC is re-applied by its poller,
 *     guaranteeing eventual consistency</li>
 *     <li>Lock button: the other mod migrated to CatFrame, and its
 *     {@code CatFrameLockButton} IS a CatFrame {@link Button} — constructed in
 *     {@link Direct} and handed to the UI layer as a plain CatFrame type, so
 *     the UI layer never references any difficultylocker type</li>
 * </ul>
 * <p>
 *     ModernDifficultyLocker is a compileOnly dependency (possibly absent at
 *     runtime), so every {@code decok.dfcdvadstf.difficultyLocker} type
 *     reference is isolated inside the {@link Direct} holder class: it is only
 *     touched when {@link #isLoaded()} is true — lazy class loading avoids
 *     NoClassDefFoundError, reflection-free throughout.
 * </p>
 */
public class DifficultyLocker {

    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":DifficultyLocker");

    /**
     * <p>
     *     创建世界界面的会话锁定状态（本次打开界面期间用户勾选的难度锁定），
     *     不落盘：世界启动时经 {@link #applyLockedDifficultyToWorldData} 直达
     *     写入对方内存状态并由 {@link #notifyWorldCreated} 经 IMC 权威同步。
     * </p>
     * <p>
     *     Session lock state for the create-world screen (whatever the user
     *     toggled while the screen is open); not persisted here: on world
     *     launch it is written directly into the other mod's in-memory state
     *     via {@link #applyLockedDifficultyToWorldData} and authoritatively
     *     synced via the {@link #notifyWorldCreated} IMC.
     * </p>
     */
    private static final boolean[] difficultyLocked = new boolean[4]; // 0=peaceful, 1=easy, 2=normal, 3=hard

    // ============================================================
    // 环境检测 / Environment
    // ============================================================

    /**
     * @return ModernDifficultyLocker 是否已加载 / whether difficultylocker is loaded
     */
    public static boolean isLoaded() {
        return IntgrationBase.isDifficultyLockerLoaded();
    }

    // ============================================================
    // IMC 配置（接收：difficultylocker 的 init 期消息） / IMC config (receive: difficultylocker's init-time message)
    // ============================================================

    // IMC 配置快照（来自 difficultylocker 的 init 期消息）
    // IMC config snapshot (from difficultylocker's init-time message)
    private static boolean imcConfigReceived = false;
    private static boolean imcShowLockButton = true;
    private static boolean imcAllowUnlock = false;
    private static int imcDefaultLockedDifficulty = 2;

    /**
     * <p>
     *     处理来自 difficultylocker 的 {@code difficultylocker_config} init 期
     *     IMC 消息（{@code CreateWorldUI#onIMC} 收到后转交至此）。
     * </p>
     * <p>
     *     Process the init-time {@code difficultylocker_config} IMC message from
     *     difficultylocker (handed over here by {@code CreateWorldUI#onIMC}).
     * </p>
     *
     * @param tag The NBT data from IMC message / IMC 消息的 NBT 数据
     */
    public static void processIMCConfig(NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        imcConfigReceived = true;
        imcShowLockButton = tag.getBoolean("showLockButton");
        imcAllowUnlock = tag.getBoolean("allowUnlock");
        imcDefaultLockedDifficulty = tag.getInteger("defaultLockedDifficulty");
        logger.info("Received IMC config from difficultylocker: showLockButton={}, allowUnlock={}, defaultDiff={}",
            imcShowLockButton, imcAllowUnlock, imcDefaultLockedDifficulty);
    }

    /** @return 是否已收到对方 IMC 配置（比 Loader.isModLoaded 更精确）/ whether the other mod's IMC config was received (more accurate than Loader.isModLoaded) */
    public static boolean hasIMCConfig() { return imcConfigReceived; }

    /** @return 对方配置：是否显示锁定按钮 / other mod's config: show the lock button */
    public static boolean getIMCShowLockButton() { return imcShowLockButton; }

    /** @return 对方配置：锁定后是否允许解锁 / other mod's config: allow unlocking after locked */
    public static boolean getIMCAllowUnlock() { return imcAllowUnlock; }

    /** @return 对方配置：锁定时的默认难度 id / other mod's config: default locked difficulty id */
    public static int getIMCDefaultLockedDifficulty() { return imcDefaultLockedDifficulty; }

    // ============================================================
    // IMC 运行期（接收轮询 + 发送） / Runtime IMC (receive polling + sending)
    // ============================================================

    private static int pollCounter = 0;

    /**
     * <p>
     *     轮询运行期 IMC 消息（{@code IMCRuntimeHandler} 每客户端 tick 调用，
     *     此处节流为每秒一次）。处理 difficultylocker 发来的
     *     {@code world_lock_applied}：既有世界加载后其锁定已生效，回写本侧
     *     会话状态。
     * </p>
     * <p>
     *     Poll runtime IMC messages (called every client tick by
     *     {@code IMCRuntimeHandler}, throttled here to once a second).
     *     Handles difficultylocker's {@code world_lock_applied}: an existing
     *     world was loaded with its lock applied — mirror it into our session
     *     state.
     * </p>
     */
    public static void pollRuntimeMessages() {
        pollCounter++;
        if (pollCounter % 20 != 0) return; // 每秒查一次 / poll once a second

        ImmutableList<FMLInterModComms.IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(CreateWorldUI.class);
        for (FMLInterModComms.IMCMessage msg : messages) {
            if ("difficultylocker".equals(msg.getSender())) {
                switch (msg.key) {
                    case "world_lock_applied":
                        NBTTagCompound nbt = msg.getNBTValue();
                        int diffId = nbt.getInteger("difficultyId");
                        EnumDifficulty diff = EnumDifficulty.getDifficultyEnum(diffId);
                        if (diff != null) {
                            setDifficultyLocked(diff, true);
                            logger.info("IMC: world lock applied, difficulty {} locked", diffId);
                        }
                        break;
                    default:
                        logger.debug("Unknown runtime IMC key from difficultylocker: {}", msg.key);
                }
            }
        }
    }

    /**
     * <p>
     *     发送运行期 IMC {@code lock_state_change}：创建世界界面里锁按钮被
     *     点击后，把新的锁定状态同步给 difficultylocker（其每秒轮询并写入
     *     {@code WorldDifficultyData}）。
     * </p>
     * <p>
     *     Send the runtime IMC {@code lock_state_change}: after the lock button
     *     on the create-world screen is pressed, sync the new lock state to
     *     difficultylocker (which polls it once a second into its
     *     {@code WorldDifficultyData}).
     * </p>
     *
     * @param difficulty 被切换锁定状态的难度 / the difficulty whose lock state toggled
     * @param locked true=锁定 / true = locked
     */
    public static void sendLockStateChange(EnumDifficulty difficulty, boolean locked) {
        if (!isLoaded() || difficulty == null) {
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("difficultyId", difficulty.getDifficultyId());
        tag.setBoolean("locked", locked);
        FMLInterModComms.sendRuntimeMessage(CreateWorldUI.class, "difficultylocker", "lock_state_change", tag);
        logger.info("Sent runtime IMC: lock_state_change, difficulty={}, locked={}",
            difficulty.getDifficultyId(), locked);
    }

    /**
     * <p>
     *     发送运行期 IMC {@code world_created}：新世界的集成服务器启动后，
     *     把界面侧的锁定结果权威同步给 difficultylocker。对方 mixin 同在
     *     loadAllWorlds TAIL 注入，其 {@code loadWorldData}（新世界无
     *     difficultylocker.dat，会重置为默认）可能晚于本侧
     *     {@link #applyLockedDifficultyToWorldData} 的直达写入执行 ——
     *     该 IMC 由对方每秒轮询补写，保证最终一致。
     * </p>
     * <p>
     *     Send the runtime IMC {@code world_created}: after a new world's
     *     integrated server launches, authoritatively sync the screen-side lock
     *     result to difficultylocker. The other mod's mixin also injects at
     *     loadAllWorlds TAIL, and its {@code loadWorldData} (a fresh world has
     *     no difficultylocker.dat, so it resets to defaults) may run AFTER our
     *     direct write from {@link #applyLockedDifficultyToWorldData} — the
     *     IMC is re-applied by its once-a-second poller, guaranteeing
     *     eventual consistency.
     * </p>
     *
     * @param worldName 世界存档目录名 / the world save-dir name
     * @param difficulty 锁定的难度 / the locked difficulty
     * @param locked 是否锁定 / whether locked
     */
    public static void notifyWorldCreated(String worldName, EnumDifficulty difficulty, boolean locked) {
        if (!isLoaded() || difficulty == null) {
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("worldName", worldName == null ? "" : worldName);
        tag.setInteger("difficulty", difficulty.getDifficultyId());
        tag.setBoolean("locked", locked);
        FMLInterModComms.sendRuntimeMessage(CreateWorldUI.class, "difficultylocker", "world_created", tag);
        logger.info("Sent runtime IMC: world_created, world='{}', difficulty={}, locked={}",
            worldName, difficulty.getDifficultyId(), locked);
    }

    // ============================================================
    // 会话锁定状态（不暴露对方类型给界面层） / Session lock state (no difficultylocker types leaked to the UI layer)
    // ============================================================

    /**
     * 设置特定难度的锁定状态。 Set the lock state for a specific difficulty.
     *
     * @param difficulty 要锁定/解锁的难度 / the difficulty to lock/unlock
     * @param locked true=锁定，false=解锁 / true = lock, false = unlock
     */
    public static void setDifficultyLocked(EnumDifficulty difficulty, boolean locked) {
        if (difficulty != null) {
            difficultyLocked[difficulty.getDifficultyId()] = locked;
            logger.info("Difficulty id={} lock state set to: {}", difficulty.getDifficultyId(), locked);
        }
    }

    /**
     * @param difficulty 要检查的难度 / the difficulty to check
     * @return 该难度是否被锁定 / whether the difficulty is locked
     */
    public static boolean isDifficultyLocked(EnumDifficulty difficulty) {
        if (difficulty == null) return false;
        return difficultyLocked[difficulty.getDifficultyId()];
    }

    /**
     * @return 当前锁定的难度，若无则 null / the currently locked difficulty, or null if none
     */
    public static EnumDifficulty getLockedDifficulty() {
        for (int i = 0; i < difficultyLocked.length; i++) {
            if (difficultyLocked[i]) {
                return EnumDifficulty.getDifficultyEnum(i);
            }
        }
        return null;
    }

    /**
     * @return 是否有任何难度被锁定 / whether any difficulty is locked
     */
    public static boolean hasLockedDifficulty() {
        for (boolean locked : difficultyLocked) {
            if (locked) return true;
        }
        return false;
    }

    /**
     * <p>
     *     重置全部会话锁定状态（世界创建应用完毕后调用，
     *     见 {@code MixinIntegratedServer}）。
     * </p>
     * <p>
     *     Reset all session lock states (called after a world creation has
     *     been applied, see {@code MixinIntegratedServer}).
     * </p>
     */
    public static void resetAllLocks() {
        for (int i = 0; i < difficultyLocked.length; i++) {
            difficultyLocked[i] = false;
        }
        logger.info("All difficulty locks reset");
    }

    // ============================================================
    // 直达调用（直接写 WorldDifficultyData） / Direct application (writing WorldDifficultyData directly)
    // ============================================================

    /**
     * <p>
     *     将锁定的难度直达写入 difficultylocker 的 {@code WorldDifficultyData}
     *     （新世界启动集成服务器、loadAllWorlds 完成后调用）。与
     *     {@link #notifyWorldCreated} 配对：直达写入立即生效，IMC 兜底对方
     *     mixin 注入顺序。
     * </p>
     * <p>
     *     Write the locked difficulty directly into difficultylocker's
     *     {@code WorldDifficultyData} (called after a new world's integrated
     *     server finishes loadAllWorlds). Paired with
     *     {@link #notifyWorldCreated}: the direct write takes effect
     *     immediately, the IMC covers the other mod's mixin ordering.
     * </p>
     *
     * @param difficulty 锁定的难度 / the locked difficulty
     */
    public static void applyLockedDifficultyToWorldData(EnumDifficulty difficulty) {
        if (!isLoaded() || difficulty == null) {
            return;
        }
        Direct.applyLockedDifficulty(difficulty);
        logger.info("Applied locked difficulty id={} to WorldDifficultyData", difficulty.getDifficultyId());
    }

    // ============================================================
    // 锁按钮（difficultylocker 的 CatFrame 组件） / Lock button (difficultylocker's CatFrame widget)
    // ============================================================

    /**
     * <p>
     *     创建 difficultylocker 的 CatFrame 锁按钮（其
     *     {@code CatFrameLockButton} 本身就是 CatFrame {@link Button}），
     *     以 CatFrame 类型返回 —— 界面层拿到的就是普通 CatFrame 按钮，
     *     点击音效/悬停/禁用由 CatFrame 基类统一处理。
     *     difficultylocker 缺席时返回 null。
     * </p>
     * <p>
     *     Create difficultylocker's CatFrame lock button (its
     *     {@code CatFrameLockButton} IS a CatFrame {@link Button}) and return
     *     it as a CatFrame type — to the UI layer it is just a plain CatFrame
     *     button; click sound / hover / disabled states are handled uniformly
     *     by the CatFrame base class. Returns null when difficultylocker is
     *     absent.
     * </p>
     *
     * @param onPress 点击回调（收到的是 CatFrame Button）/ press callback (handed a CatFrame Button)
     * @return 锁按钮，或 null（未加载）/ the lock button, or null (not loaded)
     */
    public static Button createLockButton(Button.OnPress onPress) {
        if (!isLoaded()) {
            return null;
        }
        return Direct.createLockButton(onPress);
    }

    /**
     * <p>
     *     同步锁按钮的锁定视觉状态（按钮由本类创建，内部安全回转为其
     *     {@code CatFrameLockButton} 实类型）。
     * </p>
     * <p>
     *     Sync the lock button's locked visual state (the button was created by
     *     this class; internally it is safely cast back to its real
     *     {@code CatFrameLockButton} type).
     * </p>
     *
     * @param button 由 {@link #createLockButton} 创建的按钮 / a button created by {@link #createLockButton}
     * @param locked 是否显示为已锁定 / whether to show as locked
     */
    public static void setButtonLocked(Button button, boolean locked) {
        if (!isLoaded() || button == null) {
            return;
        }
        Direct.setButtonLocked(button, locked);
    }

    // ============================================================
    // 直接调用持有类（仅 isLoaded() 时触碰） / Direct-call holder (touched only when isLoaded())
    // ============================================================

    /**
     * <p>
     *     difficultylocker 直接调用收敛处 —— 所有
     *     {@code decok.dfcdvadstf.difficultyLocker} 类型引用只出现在本类。
     *     本类仅在 {@link #isLoaded()} 为 true 时被触碰，凭惰性类加载保证
     *     difficultylocker 缺席时不触发 NoClassDefFoundError；无反射。
     * </p>
     * <p>
     *     Where all direct difficultylocker calls live — every
     *     {@code decok.dfcdvadstf.difficultyLocker} type reference appears
     *     only in this class. It is touched only when {@link #isLoaded()} is
     *     true; lazy class loading keeps difficultylocker's absence free of
     *     NoClassDefFoundError. Reflection-free.
     * </p>
     */
    private static final class Direct {

        private Direct() {
        }

        /** 直接写 WorldDifficultyData：locked=true + 锁定难度。 Write WorldDifficultyData directly: locked=true + the locked difficulty. */
        static void applyLockedDifficulty(EnumDifficulty difficulty) {
            WorldDifficultyData data = WorldDifficultyData.getInstance();
            data.setLocked(true);
            data.setLockedDifficulty(difficulty);
        }

        /** 构造对方的 CatFrame 锁按钮，以 CatFrame Button 类型交出。 Build the other mod's CatFrame lock button, handed out as a CatFrame Button. */
        static Button createLockButton(Button.OnPress onPress) {
            return new CatFrameLockButton(0, 0, false, onPress);
        }

        /** 回转实类型并同步锁定视觉（按钮必由 {@link #createLockButton} 创建）。 Cast back to the real type and sync the locked visual (the button is always created by {@link #createLockButton}). */
        static void setButtonLocked(Button button, boolean locked) {
            if (button instanceof CatFrameLockButton) {
                ((CatFrameLockButton) button).setLocked(locked);
            }
        }
    }
}
