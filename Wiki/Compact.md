# For modpack developer

Want to tweak CreateWorldUI for your modpack? Here's what you need to know.

---

## Add the game rules descriptions via localization files
A simple way is that we can do it by using the localization files. And the format is:   
`gamerule.` + your rule name + `.tooltip.description` = ....   

Example:
```properties
gamerule.doFireTick.tooltip.description=Controls whether fire spreads and naturally extinguishes
gamerule.mobGriefing.tooltip.description=Controls whether mobs can destroy blocks
```

## Set the GameRule display names via localization files (After 3.1.0)
You can also localize the display name of game rules. The format is:   
`gamerule.` + your rule name + `.name` = ....   

Example:
```properties
# English
gamerule.doFireTick.name=Fire Tick
gamerule.mobGriefing.name=Mob Griefing
```

If no localization is provided, the original rule name (e.g., `doFireTick`) will be displayed.

---

## Configuring the Mod

You can configure things through `config/createworldui.cfg`, or in-game via **Mod Options → CreateWorldUI**.

### GameRule Editor

| Option | Default | What it does |
|---|---|---|
| `enableReloadButton` | `false` | Adds a Reset button to the GameRule Editor. |
| `igGameruleEdit` | `false` | Lets you use `/gameruleEditor` in-game. |
| `changedRulesInChatHighLighted` | `false` | Makes changed rule names yellow in chat. |
| `highlightModifiedRulesInGUI` | `true` | Makes modified rules yellow in the editor. |

### UI Management

| Option | Default | What it does |
|---|---|---|
| `topTabCharatorModernWhite` | `false` | Makes tab text white. Needs ArchaicFix. |
| `gameruleEdit` | `true` | Shows the Game Rules button on the More tab. |
| `enableOtherMoreTabButton` | `false` | Shows Experiments and Data Packs buttons on the More tab. |
| `showWorldNamePlaceHolder` | `false` | Shows placeholder text in the world name field when empty. |
| `disableCreateButtonWhenWNIsBlank` | `false` | Prevents creating a world with no name. |

---

## Resource Pack Customization

You can override these textures in your resource pack:

- **Tab buttons**: `assets/createworldui/textures/gui/tabs.png`
- **Background**: `assets/createworldui/textures/gui/options_background_dark.png`

Just drop your replacement textures at the same paths and you're good to go.

## Official Resource Pack: CreateWorldUI Compatibility

There's an official resource pack on CurseForge: [CreateWorldUI Compatibility](https://www.curseforge.com/minecraft/texture-packs/createworldui-compatibility).

This pack is designed to work with **CreateWorldUI 3.2.1+** together with [ClearMyBackground](https://modrinth.com/mod/clearmybackground) — it provides updated textures that make the world creation screen look closer to **1.20.5+** style.

To use it:
1. Install [ClearMyBackground](https://modrinth.com/mod/clearmybackground).
2. Download the resource pack from [CurseForge](https://www.curseforge.com/minecraft/texture-packs/createworldui-compatibility).
3. Put it in your `resourcepacks` folder and enable it.
4. Make sure you're running CreateWorldUI **3.2.1 or later**.

That's it — the mod handles the rest.

> **Note**: This pack doesn't requires **OptiFine** and is for **Minecraft 1.7.10 only**.

---

## ClearMyBackground Compatibility

Works with [ClearMyBackground](https://github.com/RuiXuqi/ClearMyBackground). When it's installed, the world creation screen background adapts automatically — no extra setup needed.

Want the full 1.20.5+ look? Grab the [official resource pack](https://www.curseforge.com/minecraft/texture-packs/createworldui-compatibility) — see the section [above](#official-resource-pack-createworldui-compatibility).

---

## ModernDifficultyLocker Compatibility [After 3.2.1]

When [ModernDifficultyLocker](https://github.com/song682/ModernDifficultyLocker) is installed, the `/gameruleEditor` in-game command becomes available automatically — even if `igGameruleEdit` is set to `false`.

---

## Known Incompatible Mods
|Mods|Reasons|
|---:|:---|
|BetterFPS|Unable to work with a mod that use a thing I cannot accept|
|Future Commands|The Poor ASM is a burden for me to develop a good compatibility|