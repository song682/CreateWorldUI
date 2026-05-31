# For Players

So you've installed CreateWorldUI. This mod brings the modern world creation screen (the one from after 23w05a) back to Minecraft 1.7.10. Here's how to use it.

---

## The Modern World Creation Screen

When you click **Create New World**, instead of the old vanilla screen you'll see a new tabbed UI.

### Tabs

Everything is organized into three tabs at the top:

| Tab | What's in it |
|---|---|
| **Game** | World name, game mode, difficulty, cheats |
| **World** | Seed, world type, structures, bonus chest |
| **More** | Game Rule Editor and other advanced stuff |

Click a tab to switch. Your selected tab sticks around even if you resize the window.

---

## Game Tab

This is the default tab you see first.

- **World Name**: Just type your world name in the text field at the top.
- **Game Mode**: Click the button to cycle through Survival → Creative → Hardcore → Adventure.
- **Difficulty**: Click to cycle through Peaceful → Easy → Normal → Hard. In Hardcore mode, this is locked to Hard and the button is disabled.
- **Allow Cheats**: Click to toggle ON/OFF. It turns on automatically in Creative mode, and is disabled in Hardcore.

> **Tip**: If the **Show World Name Placeholder** config option is on, you'll see a gray placeholder in the world name field when it's empty — just a gentle reminder to type something.

---

## World Tab

- **World Type**: Click to cycle through available world types (Default, Flat, Large Biomes, etc.). The **Customize** button next to it lights up for types that support it (like Flat World).
- **Seed**: Enter a seed if you want. A gray placeholder shows up when the field is empty and unfocused.
- **Generate Structures**: Toggle ON/OFF for villages, dungeons, etc.
- **Bonus Chest**: Toggle ON/OFF for a starter chest near spawn. Disabled in Hardcore mode.

---

## More Tab

- **Game Rules**: Click to open the **GameRule Editor** — a graphical interface for tweaking game rules before you create the world. More on that below.
- **Experiments** / **Data Packs**: These buttons show up if the **Enable Other More Tab Button** config option is on. They're placeholders from newer Minecraft versions and may not do much in 1.7.10.

---

## GameRule Editor

The GameRule Editor lets you change game rules *before* the world is created, so your rules are in effect right from the start.

### How to Open
- From the **More** tab, click the **Game Rules** button (it's there by default).
- In-game, type `/gameruleEditor` — you'll need the **In-Game Gamerule Editor** config option enabled, or have ModernDifficultyLocker installed.

### What You See

- **Boolean rules** (like `doFireTick`, `keepInventory`) show up as toggle buttons — just click to switch ON/OFF.
- **Numeric rules** (like `randomTickSpeed`) are text fields where you type a number.
- Each rule shows its name and default value for reference.
- Rules you've changed get **highlighted in yellow** (if that config option is on) so you can easily spot what you tweaked.
- **Tooltips**: Hover over a rule to see its description, if one is available.
- **Smooth scrolling**: Just use your mouse wheel to scroll through the list.

### The Buttons

| Button | What it does |
|---|---|
| **Save** | Apply your changes. The rules are stored and will take effect when the world is created. |
| **Cancel** | Throw away all changes and go back. |
| **Reset** | Reset everything back to defaults. Only shows up if the **Enable Reload Button** config option is on. |

### Editing Rules In-Game

If you turn on the **In-Game Gamerule Editor** config option, you can use `/gameruleEditor` while playing to open the editor and change rules for the current world. Changes take effect right away.

When rules are changed in-game, a chat message pops up listing what you modified. If **Highlight Changed Rules in Chat** is on, the rule names show up in yellow.

---

## Configuration

You can configure this mod through the in-game Mod Options screen, or by editing `config/createworldui.cfg` directly.

### GameRule Editor

| Option | Default | What it does |
|---|---|---|
| Enable Reload Button | `false` | Adds a Reset button to the GameRule Editor. |
| In-Game Gamerule Editor | `false` | Lets you use `/gameruleEditor` in-game. |
| Highlight Changed Rules in Chat | `false` | Makes changed rule names yellow in chat. |
| Highlight Modified Rules in GUI | `true` | Makes modified rules yellow in the editor. |

### UI Management

| Option | Default | What it does |
|---|---|---|
| Modern White Tab Text | `false` | Makes tab text white. Needs ArchaicFix. |
| Enable Gamerule Editor | `true` | Shows the Game Rules button on the More tab. |
| Enable Other More Tab Button | `false` | Shows Experiments and Data Packs buttons on the More tab. |
| Show World Name Placeholder | `false` | Shows placeholder text in the world name field when empty. |
| Disable Create Button When World Name Is Blank | `false` | Prevents creating a world with no name. |