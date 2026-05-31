# Mod Developer Things

Want to integrate with CreateWorldUI? Here's what you need to know.

---

## GameRule Editor

Want to add custom game rules to the editor? Well, quite simple — just add the game rules through the `GameRule` class like you used to do, name your Rule name then set a default boolean or number value. And then, boom, they appeared on the page. Has their name and their default value will show on your screen.   
But... There's something wrong. What if I want to add tooltip descriptions for my rules, and also, how to custom vanilla Rule name tooltip?    
Well, more things need to be done. And we have two ways for it.   

### Adding Tooltip Descriptions

#### Simple way (Recommended)
[The modpack](https://github.com/song682/CreateWorldUI/wiki/For-modpack-developer#add-the-game-rules-descriptions-via-localization-files) usage is showing how to use it.
Just add entries like this:
```properties
gamerule.yourRuleName.tooltip.description=Your description here
```
That's it.   

#### Another hard way (After 1.0.1)
It can only be achieved by the hard code, and this doesn't like the simple way if you cannot handle it carefully it cannot show the localized name. so it's not welcomed to the only using simple ways.   
Codes are belowed:   
```java
package idk.unknown.which;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;

@Mod(modid = "idk", name = "idk", version = "idk")
public class idk {
    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        // There are two ways to register this
        // registerTooltip() can add one single tooltip
        // registerTooltips() can add a Map, which contains many rules and tooltips, to the tooltip list. 
        GuiScreenGameRuleEditor.registerTooltip("randomTickSpeed", "Controls how fast random ticks occur");
        GuiScreenGameRuleEditor.registerTooltip("myCustomRule", "This is a rule added by another mod"); // Former is ruleName, latter is tooltips. 
        Map<String,String> myCustomTooltips = new HashMap<>();
        myCustomTooltips.put("myCustomRule2","This is a rule added by another mod");
        myCustomTooltips.put("myCustomRule3","This is a rule added by another mod");
        myCustomTooltips.put("myCustomRule4","This is a rule added by another mod");
        // myCustomTooltips.put(........);
        GuiScreenGameRuleEditor.registerTooltips("myCustomToolTips");
    }
}

```

### Localizing GameRule Display Names (After 3.1.0)

Same idea as tooltips — just add this to your lang file:
```properties
gamerule.yourRuleName.name=Your Display Name
```

If no name localization is provided, the raw rule name is shown instead.

### In-Game `/gameruleEditor` Command

**Class**: `decok.dfcdvadstf.createworldui.command.CommandGameRuleEditor`

Opens the GameRule Editor in-game. You need either:
- `igGameruleEdit` config option enabled, **or**
- ModernDifficultyLocker installed.

Requires permission level 2 (same as `/gamerule`).

---

## API Overview

Here are the API classes you can use.

### GameRuleApplier

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier`

Handles applying game rules when a world loads.

| Method | What it does |
|---|---|
| `setPendingGameRules(Map<String, String>)` | Queue up game rules to apply when the next world is created. |
| `getPendingGameRules()` | Get the currently pending rules (may be `null`). |

Pending rules are applied via `WorldEvent.Load` and then cleared.

### GameRuleMonitorNSetter

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter`

For getting, setting, and adding game rules.

| Method | What it does |
|---|---|
| `getAllGamerules(World)` | Get all game rules from a world as `Map<String, GameruleValue>`. |
| `setGamerule(World, String, String)` | Set a game rule on a world. |

The `GameruleValue` inner class stores values in multiple types (String, boolean, int, double) and has `getOptimalValue()` to pick the best one.

### DifficultyApplier

**Package**: `decok.dfcdvadstf.createworldui.api.DifficultyApplier`

Applies difficulty settings when a world is created. Works with the tab system to set difficulty on the `IntegratedServer` via Mixin.

### GuiCyclableButton

**Package**: `decok.dfcdvadstf.createworldui.api.GuiCyclableButton`

A button that cycles through values — click it or scroll your mouse wheel over it to switch. Left-click or scroll down goes forward (+1), scroll up goes backward (−1). Used for game mode, difficulty, and world type in the built-in tabs, and you can use it in your own tabs too.

#### How it works

It takes two callbacks:
- **`TextSupplier`** — a `getText()` method that returns the current display string. Called every time the button updates.
- **`CycleHandler`** — an `onCycle(int direction)` method that gets called when the user clicks or scrolls. `direction` is `+1` or `−1`.

#### Example

```java
// A button that cycles through "Easy / Normal / Hard"
GuiCyclableButton diffButton = new GuiCyclableButton(
    200,                    // button ID
    width / 2 - 104,       // x
    height / 2,            // y
    208, 20,               // width, height
    this::getDifficultyText, // TextSupplier — returns current text
    direction -> cycleDifficulty(direction) // CycleHandler — direction is +1 or -1
);
addButton(diffButton);

// Your TextSupplier
private String getDifficultyText() {
    return "Difficulty: " + difficulties[currentIndex];
}

// Your CycleHandler
private void cycleDifficulty(int direction) {
    currentIndex += direction;
    if (currentIndex >= difficulties.length) currentIndex = 0;
    if (currentIndex < 0) currentIndex = difficulties.length - 1;
}
```

> **Tip**: Call `diffButton.updateText()` in your `drawScreen()` to keep the display text in sync — especially if the value can change from somewhere else (like hardcore mode locking difficulty to Hard).

### ContentPanelRenderer

**Package**: `decok.dfcdvadstf.createworldui.api.ContentPanelRenderer`

A shared renderer for the content panel — gives you the header separator, a tiled panel background, and the footer separator. Use the pieces you want individually, or let `drawContentPanel()` do the whole thing in one shot.(After 4.2.0)

Well, quite simple — the GameRule Editor and other screens all draw these panels the same way, so this thing packs it up so you don't have to copy-paste rendering code into your own screen.

#### What you get

| Method | What it draws |
|---|---|
| `drawContentPanel(x, top, width, bottom)` | The whole thing — header line, tiled background, footer line. |
| `drawHeaderSeparator(x, y, width)` | Just the top separator line (2px tall, tiled). |
| `drawFooterSeparator(x, y, width)` | Just the bottom separator line (2px tall, tiled). |
| `drawSeparator(x, y, width, texture)` | A separator line with your own 32×2 texture. |
| `drawPanelBackground(x, y, width, height)` | Just the panel background (tiles the 16×16 texture). |

#### Textures

| Field | Path | Size |
|---|---|---|
| `HEADER_SEPARATOR` | `createworldui:textures/gui/header_separator.png` | 32×2 |
| `FOOTER_SEPARATOR` | `createworldui:textures/gui/footer_separator.png` | 32×2 |
| `PANEL_BACKGROUND` | `createworldui:textures/gui/panel_background.png` | 16×16 |

You can override these in your resource pack — or pass a custom `ResourceLocation` to `drawSeparator()` if you want a different style.

#### Quick Example

```java
// Draw a full content panel in one call
ContentPanelRenderer.drawContentPanel(panelX, panelTop, panelWidth, panelBottom);

// Or draw pieces separately
ContentPanelRenderer.drawHeaderSeparator(panelX, panelTop, panelWidth);
ContentPanelRenderer.drawPanelBackground(panelX, panelTop + 2, panelWidth, panelHeight - 4);
ContentPanelRenderer.drawFooterSeparator(panelX, panelBottom - 2, panelWidth);
```

> **Tip**: The separator height is always 2 GUI pixels (`SEPARATOR_HEIGHT = 2`). When using `drawContentPanel()`, the background is automatically placed between the two separators.

### TabRegistry

**Package**: `decok.dfcdvadstf.createworldui.api.tab.TabRegistry`

The static registry for external mods to register custom tabs. See [Adding a Custom Tab](#adding-a-custom-tab) for details and examples.

---

## Tab System

The world creation screen uses a tab system. Each tab implements `AbstractScreenTab`.

### Built-in Tabs

| Class | Tab ID | What it does |
|---|---|---|
| `GameTab` | 100 | World name, game mode, difficulty, cheats |
| `WorldTab` | 101 | Seed, world type, structures, bonus chest |
| `MoreTab` | 102 | GameRule Editor, experiments, data packs |

### TabManager

**Package**: `decok.dfcdvadstf.createworldui.api.tab.TabManager`

`TabManager` handles tab switching, state persistence (survives window resizes), and gives you accessors for the shared state:

| Method | What it does |
|---|---|
| `getWorldName()` / `setWorldName(String)` | Get or set the world name. |
| `getGameMode()` / `setGameMode(String)` | Get or set the game mode. |
| `getDifficulty()` / `setDifficulty(EnumDifficulty)` | Get or set the difficulty. |
| `getHardcore()` / `setHardcore(boolean)` | Get or set hardcore mode. |
| `getAllowCheats()` / `setAllowCheats(boolean)` | Get or set allow cheats. |
| `getSeed()` / `setSeed(String)` | Get or set the world seed. |
| `getWorldTypeIndex()` / `setWorldTypeIndex(int)` | Get or set the world type. |
| `getGenerateStructures()` / `setGenerateStructures(boolean)` | Get or set generate structures. |
| `getBonusChest()` / `setBonusChest(boolean)` | Get or set bonus chest. |

### TabState

**Package**: `decok.dfcdvadstf.createworldui.api.tab.TabState`

Stores tab state (active tab, all tab fields) that persists across GUI reinitialization — like when you resize the window.

---

## Adding a Custom Tab

You can register custom tabs into the world creation screen using the `TabRegistry` API.

### TabRegistry

**Package**: `decok.dfcdvadstf.createworldui.api.tab.TabRegistry`

It's a static registry — you register your tabs during mod init, and when the world creation screen opens, `TabManager` freezes the registry and creates all the tabs.

#### Key Methods

| Method | What it does |
|---|---|
| `registerTab(Supplier<Tab>, int, String, int)` | Register a tab with factory, ID, name key, and priority. |
| `registerTab(Supplier<Tab>, int, String)` | Same thing, but priority defaults to `tabId`. |
| `getEntries()` | Get all entries sorted by priority (unmodifiable). |
| `isFrozen()` | Check if the registry is frozen (GUI already initialized). |
| `freeze()` | Freeze the registry. Called internally by `TabManager`. |

#### Parameters

| Parameter | Type | What it means |
|---|---|---|
| `factory` | `Supplier<Tab>` | A factory that creates your `Tab` instance. Called once when `TabManager` is constructed. |
| `tabId` | `int` | Unique tab ID. Built-in tabs use 100–102, so **start from 103**. |
| `nameKey` | `String` | Localization key for the tab name (like `"mymod.tab.custom"`). |
| `priority` | `int` | Sort order — lower comes first. Defaults to `tabId` if not specified. Built-in tabs use 0, 1, 2. |

Entries are sorted by `priority` ascending, then `tabId` ascending as a tiebreaker.

#### Registration Example

```java
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.createworldui.api.tab.AbstractScreenTab;
import decok.dfcdvadstf.createworldui.api.tab.Tab;
import decok.dfcdvadstf.createworldui.api.tab.TabManager;
import decok.dfcdvadstf.createworldui.api.tab.TabRegistry;
import net.minecraft.client.gui.GuiButton;

@Mod(modid = "mymod", name = "My Mod", version = "1.0")
public class MyMod {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TabRegistry.registerTab(
            MyCustomTab::new,        // Supplier<Tab> factory
            103,                     // Unique tab ID (start from 103)
            "mymod.tab.custom",      // Localization key
            5                        // Sort priority (lower = earlier)
        );
    }
}

// Your custom tab
public class MyCustomTab extends AbstractScreenTab {

    public MyCustomTab() {
        super(103, "mymod.tab.custom"); // Must match what you registered
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);
        // Add your buttons and fields here
        setVisible(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        // Draw your tab content here
    }

    @Override
    public void actionPerformed(GuiButton button) {
        // Handle button clicks here
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Handle mouse clicks here
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Handle key input here
    }
}
```

And don't forget the localization in your lang file:
```properties
mymod.tab.custom=My Tab
```

#### Things to Keep in Mind

- **Register early** — like `FMLPreInitializationEvent`. Once `TabManager` is constructed the registry freezes, and registering after that throws an `IllegalStateException`.
- **Tab IDs must be unique** — duplicate IDs throw an `IllegalArgumentException`.
- **`AbstractScreenTab` constructor must match** — the `tabId` and `nameKey` you pass there need to be the same ones you gave `TabRegistry.registerTab()`.
- **`initGui()` gets called by `TabManager`** — on init and on resize. Always call `super.initGui()` and set `setVisible(false)` at the end.
- **Use `tabManager` accessors** (like `getWorldName()`, `setGameMode()`) to read/write the shared world creation state.

---

## Mixin Classes

Here's what gets mixed into what:

| Mixin Class | Target | What it does |
|---|---|---|
| `MixinModernCreateWorld` | `GuiCreateWorld` | Replaces the world creation screen with the tabbed UI. |
| `MixinGuiSelectWorld` | `GuiSelectWorld` | Enhances the world selection screen. |
| `MixinIntegratedServer` | `IntegratedServer` | Applies difficulty and game rules when the world loads. |
| `MixinMinecraft` | `Minecraft` | Client-side hooks. |

---

## Dependency Setup

To use CreateWorldUI as a dependency, add this to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.song682:CreateWorldUI:Tag'
}
```

Replace `Tag` with the version you want from the [repository](https://github.com/song682/CreateWorldUI).    
Also, the `-deobf` suffix jar is available.