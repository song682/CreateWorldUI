# Mod Developer Things

Want to integrate with CreateWorldUI? Here's what you need to know.

---

## Prerequisites (6.0.0+)

Starting from version 6.0.0, CreateWorldUI requires **[CatFrame](https://github.com/song682/CatFrame)** as a mandatory dependency. CatFrame provides the core UI components — cycling buttons, panel rendering, and the tab system.

If you're writing custom tabs or need UI building blocks, check out the [CatFrame UI Components](../ui.md) documentation.

---

## API Overview

Want to know what APIs you can work with? Here's the lineup:

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

### GameRuleTooltipRegistry (After 5.0.0)

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry`

This is your go-to API for registering and managing game rule tooltips. We pulled it out from `GuiScreenGameRuleEditor` to keep things cleaner and easier to access.

| Method | What it does |
|---|---|
| `registerTooltip(String ruleName, String tooltip)` | Register a single tooltip for a game rule |
| `registerTooltips(Map<String, String> tooltips)` | Register multiple tooltips at once |
| `getTooltip(String ruleName)` | Get tooltip for a rule (follows priority: localization > registered > default) |
| `hasRegisteredTooltip(String ruleName)` | Check if a tooltip is already registered |
| `removeTooltip(String ruleName)` | Remove a registered tooltip |
| `clearAllTooltips()` | Clear all registered tooltips |
| `getRegisteredCount()` | Get the number of registered tooltips |
| `getAllRegisteredTooltips()` | Get a read-only copy of all registered tooltips |

**How it works:**
When you call `getTooltip()`, the system checks in this order:
1. **Localization file** — `gamerule.{ruleName}.tooltip.description`
2. **Registered tooltips** — via this API
3. **Built-in defaults** — only for vanilla rules

This means your mod's tooltip can be overridden by a resource pack, which is great for localization support!

**Migration note:** If you're using the old `GuiScreenGameRuleEditor.registerTooltip()`, don't worry — it still works (marked as `@Deprecated`), but we recommend switching to the new API when you have time.

### GameRuleNameRegistry (After 5.0.0)

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry`

Need to set display names for game rules? This API's got you covered. Before 5.0.0, you were stuck with localization files only. Now you can do it through code too!

| Method | What it does |
|---|---|
| `registerName(String ruleName, String displayName)` | Register a display name for a game rule |
| `registerNames(Map<String, String> names)` | Register multiple display names at once |
| `getName(String ruleName)` | Get display name for a rule (follows priority: localization > registered > raw name) |
| `hasRegisteredName(String ruleName)` | Check if a display name is already registered |
| `removeName(String ruleName)` | Remove a registered display name |
| `clearAllNames()` | Clear all registered display names |
| `getRegisteredCount()` | Get the number of registered display names |
| `getAllRegisteredNames()` | Get a read-only copy of all registered display names |

**How it works:**
When you call `getName()`, the system checks in this order:
1. **Localization file** — `gamerule.{ruleName}.name`
2. **Registered names** — via this API
3. **Raw rule name** — e.g., `doFireTick`

This way, resource packs can still override your display names for localization purposes!

### GameRuleCategoryRegistry (After 5.0.0)

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry`

Looking to organize game rules into categories? Think of this API like folders for your rules — makes the editor way more organized!

| Method | What it does |
|---|---|
| `createCategory(String categoryKey, List<String> ruleNames)` | Create a new category with rules |
| `addRuleToCategory(String categoryKey, String ruleName)` | Add a single rule to a category |
| `addRulesToCategory(String categoryKey, List<String> ruleNames)` | Add multiple rules to a category |
| `getRulesInCategory(String categoryKey)` | Get all rules in a category (read-only list) |
| `getCategoryForRule(String ruleName)` | Get the category that a rule belongs to |
| `getAllCategories()` | Get all category keys (in registration order) |
| `getCategoryDisplayName(String categoryKey)` | Get localized display name for a category |
| `removeRuleFromCategory(String ruleName)` | Remove a rule from its category |
| `removeCategory(String categoryKey)` | Remove an entire category |
| `clearCustomCategories()` | Clear custom categories (keep defaults) |
| `clearAllCategories()` | Clear all categories (including defaults) |
| `getCategoryCount()` | Get the number of categories |
| `getAllCategoriesMap()` | Get complete category mapping (read-only) |

**Default Categories:**
Vanilla rules come pre-categorized:
- `World` — doFireTick, doTileDrops, doDaylightCycle
- `Mobs` — doMobSpawning, doMobLoot, mobGriefing
- `Player` — naturalRegeneration, keepInventory
- `Chat` — commandBlockOutput

**Localizing category names:**
```properties
gamerule.category.myMod=My Mod Rules
```

And then the category will display with that name in the editor!

### DifficultyApplier

**Package**: `decok.dfcdvadstf.createworldui.api.DifficultyApplier`

Applies difficulty settings when a world is created. Works with the tab system to set difficulty on the `IntegratedServer` via Mixin.

And here's the thing — vanilla `GuiCreateWorld` has no difficulty field at all, so the mod caches it locally and syncs it to `mc.gameSettings.difficulty` on every write. `DifficultyApplier` then queues it up as a "pending" value, and `MixinIntegratedServer` consumes it when the world finishes loading.

| Method | What it does |
|---|---|
| `setPendingDifficulty(EnumDifficulty)` | Queue a difficulty to apply on the next world load. |
| `consumePendingDifficulty()` | Get and clear the pending difficulty (consume pattern). |
| `hasPendingDifficulty()` | Check if there's a pending difficulty waiting. |

### IGuiCreateWorldAccess

**Package**: `decok.dfcdvadstf.createworldui.mixin.access.IGuiCreateWorldAccess`

Vanilla `GuiCreateWorld` hides everything behind `private` — so this is an Accessor mixin interface that auto-generates getters/setters at runtime. External mods (or our own non-mixin code) can just cast the screen instance to `IGuiCreateWorldAccess` and read/write state directly — no need to write another mixin or reflect into private fields.

Here's where it gets interesting — this interface lives under the `mixin.*` package (not `api/`) because Mixin's `package` config "seals" that package tree. Classes inside become mixin templates. But don't worry — external code can still import and cast it just fine. The sealing only blocks `Class.forName`-style direct loads from outside the mixin pipeline.

#### What you can access

| Accessor method | Vanilla field | Type |
|---|---|---|
| `createWorldUI$getWorldName()` / `setWorldName(String)` | `field_146330_J` | `String` |
| `createWorldUI$getGameMode()` / `setGameMode(String)` | `field_146342_r` | `String` |
| `createWorldUI$getSeed()` / `setSeed(String)` | `field_146329_I` | `String` |
| `createWorldUI$getWorldTypeIndex()` / `setWorldTypeIndex(int)` | `field_146331_K` | `int` |
| `createWorldUI$getGenerateStructures()` / `setGenerateStructures(boolean)` | `field_146341_s` | `boolean` |
| `createWorldUI$getBonusChest()` / `setBonusChest(boolean)` | `field_146338_v` | `boolean` |
| `createWorldUI$getAllowCheats()` / `setAllowCheats(boolean)` | `field_146340_t` | `boolean` |
| `createWorldUI$getHardcore()` / `setHardcore(boolean)` | `field_146337_w` | `boolean` |
| `createWorldUI$getParentScreen()` | `field_146332_f` | `GuiScreen` (read-only) |

#### Quick Example

```java
GuiCreateWorld gui = ...;
IGuiCreateWorldAccess access = (IGuiCreateWorldAccess)(Object) gui;
String name = access.createWorldUI$getWorldName();
access.createWorldUI$setWorldName("My Shiny New World");
```

> **Note**: All accessor methods carry the `createWorldUI$` prefix to avoid name clashes with other mods that might define their own accessor interfaces.

---

## Tab System

CreateWorldUI uses a tab system powered by CatFrame. Each tab implements `Tab` (from CatFrame), and you can extend `AbstractScreenTab` for convenience.

For detailed documentation on the tab system (including `TabBar`), cycling buttons, and panel rendering, see [CatFrame UI Components](../ui.md).



---

## Adding a Custom Tab

You can register custom tabs into the world creation screen using CatFrame's `TabRegistry` API.

### Quick Start

For full documentation on the tab system, including `TabBar`, `TabRegistry`, `TabManager`, `AbstractScreenTab`, and examples, see [CatFrame UI Components - Tab System](../ui.md#tab-system).

---

## GameRule Editor

Want to add custom game rules to the editor? Pretty straightforward — just add the game rules through the `GameRule` class like you used to do, name your Rule name then set a default boolean or number value. And just like that, they pop up on the page with their names and default values ready to go.   
But here's the catch — what if you want tooltip descriptions for your rules? Or customize how vanilla rule names appear? That's where things get interesting, and we've got a couple of approaches for you.   

### Adding Tooltip Descriptions

#### Simple way (Recommended)
[The modpack](https://github.com/song682/CreateWorldUI/wiki/For-modpack-developer#add-the-game-rules-descriptions-via-localization-files) usage is showing how to use it.
Just add entries like this:
```properties
gamerule.yourRuleName.tooltip.description=Your description here
```
That's it.   

#### Another hard way (After 1.0.1 - Legacy)
It can only be achieved by the hard code, and this doesn't like the simple way if you cannot handle it carefully it cannot show the localized name. so it's not welcomed to the only using simple ways.   

**Note for 5.0.0+:** This method is now deprecated. Please use the new `GameRuleTooltipRegistry` API (see API Overview section below). But if you're on older versions, here's how it works:

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
        GuiScreenGameRuleEditor.registerTooltips(myCustomTooltips);
    }
}

```

#### New API way (After 5.0.0 - Recommended!)
Here's the deal — starting from 5.0.0, we pulled the tooltip registration out into its own API class. So much cleaner! 

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry`

```java
package idk.unknown.which;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;

@Mod(modid = "idk", name = "idk", version = "idk")
public class idk {
    @Mod.EventHandler
    public void init(FMLInitializationEvent event){
        // Method 1: Register a single tooltip
        GameRuleTooltipRegistry.registerTooltip("randomTickSpeed", "Controls how fast random ticks occur");
        GameRuleTooltipRegistry.registerTooltip("myCustomRule", "This is a rule added by another mod");
        
        // Method 2: Register multiple tooltips at once
        Map<String, String> myCustomTooltips = new HashMap<>();
        myCustomTooltips.put("myCustomRule2", "This is a rule added by another mod");
        myCustomTooltips.put("myCustomRule3", "This is a rule added by another mod");
        GameRuleTooltipRegistry.registerTooltips(myCustomTooltips);
    }
}
```

Why the new API is better:
- ✅ Separate from GUI class — cleaner architecture
- ✅ More methods: `removeTooltip()`, `clearAllTooltips()`, `hasRegisteredTooltip()`, etc.
- ✅ Better organized and easier to maintain
- ✅ Still backward compatible — old code still works!

### Localizing GameRule Display Names (After 3.1.0)

#### Simple way (Recommended)
Same idea as tooltips — just add this to your lang file:
```properties
gamerule.yourRuleName.name=Your Display Name
```

If no name localization is provided, the raw rule name is shown instead.

#### New API way (After 5.0.0)
Guess what — you can register display names through code now too! Follows the exact same pattern as the tooltip API:

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry`

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry;

// Register a single display name
GameRuleNameRegistry.registerName("myCustomRule", "My Custom Rule");

// Or register multiple at once
Map<String, String> names = new HashMap<>();
names.put("customRule1", "Custom Rule One");
names.put("customRule2", "Custom Rule Two");
GameRuleNameRegistry.registerNames(names);
```

**Priority:** Just like tooltips, it checks localization file first, then registered names, then falls back to raw rule name.

### Organizing GameRules into Categories (After 5.0.0)

Got a ton of custom game rules and wondering how to keep them organized? You're in luck — we built a category system just for that!

#### Default Categories for Vanilla Rules

Out of the box, vanilla game rules are already categorized:
- **世界 (World)**: doFireTick, doTileDrops, doDaylightCycle
- **生物 (Mobs)**: doMobSpawning, doMobLoot, mobGriefing
- **玩家 (Player)**: naturalRegeneration, keepInventory
- **聊天 (Chat)**: commandBlockOutput

#### UI Layout

In the GameRule Editor, categories are displayed like this:

```
|--------------|
|     生物     |    ← Category title (centered, localizable)
| .....    []  |    ← Game rule controls
| .....    []  |
|--------------|
```

The category title is **centered** and **always visible** (not collapsible). Below it are all the game rule controls for that category.

#### Adding Your Custom Rules to Categories

**Package**: `decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry`

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry;
import java.util.Arrays;

// Method 1: Create a new category with rules
GameRuleCategoryRegistry.createCategory(
    "gamerule.category.myMod",  // Category key (will be localized)
    Arrays.asList("myCustomRule1", "myCustomRule2", "myCustomRule3")
);

// Method 2: Add rules to an existing category
GameRuleCategoryRegistry.addRuleToCategory("gamerule.category.myMod", "anotherRule");

// Method 3: Add multiple rules at once
GameRuleCategoryRegistry.addRulesToCategory(
    "gamerule.category.myMod",
    Arrays.asList("rule1", "rule2", "rule3")
);
```

#### Localizing Category Names

Just add this to your lang file:
```properties
gamerule.category.world=世界
gamerule.category.mobs=生物
gamerule.category.player=玩家
gamerule.category.chat=聊天
gamerule.category.myMod=My Mod Rules
```

So there you have it — your categories show up with nice localized names!

### In-Game `/gameruleEditor` Command

**Class**: `decok.dfcdvadstf.createworldui.command.CommandGameRuleEditor`

Opens the GameRule Editor in-game. You need either:
- `igGameruleEdit` config option enabled, **or**
- ModernDifficultyLocker installed.

Requires permission level 2 (same as `/gamerule`).

---

## GameRule API Examples

Want to see how it all works together? Here are some practical examples.

### Example 1: Register a single tooltip

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;

// Call this during your mod initialization
GameRuleTooltipRegistry.registerTooltip(
    "myCustomRule",
    "This is my custom game rule description"
);
```

### Example 2: Register multiple tooltips at once

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;
import java.util.HashMap;
import java.util.Map;

// Create a tooltip map
Map<String, String> tooltips = new HashMap<>();
tooltips.put("customRule1", "Description for custom rule 1");
tooltips.put("customRule2", "Description for custom rule 2");
tooltips.put("customRule3", "Description for custom rule 3");

// Register all at once
GameRuleTooltipRegistry.registerTooltips(tooltips);
```

### Example 3: Register display names

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry;

// Register a single display name
GameRuleNameRegistry.registerName("myCustomRule", "My Custom Rule");

// Or register multiple at once
Map<String, String> names = new HashMap<>();
names.put("customRule1", "Custom Rule One");
names.put("customRule2", "Custom Rule Two");
GameRuleNameRegistry.registerNames(names);
```

### Example 4: Create categories and organize rules

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry;
import java.util.Arrays;

// Create a new category with rules
GameRuleCategoryRegistry.createCategory(
    "gamerule.category.myMod",  // Category key
    Arrays.asList("myCustomRule1", "myCustomRule2", "myCustomRule3")
);

// Add more rules later
GameRuleCategoryRegistry.addRuleToCategory("gamerule.category.myMod", "anotherRule");
```

### Example 5: Complete mod integration

Here's how you'd do it all together — names, tooltips, and categories:

```java
package com.example.mymod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Mod(modid = "mymod", name = "My Mod", version = "1.0")
public class MyMod {
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 1. Register display names
        Map<String, String> names = new HashMap<>();
        names.put("enableCustomFeature", "Enable Custom Feature");
        names.put("customDamageMultiplier", "Custom Damage Multiplier");
        names.put("allowCustomSpawning", "Allow Custom Spawning");
        GameRuleNameRegistry.registerNames(names);
        
        // 2. Register tooltip descriptions
        Map<String, String> tooltips = new HashMap<>();
        tooltips.put("enableCustomFeature", "Enable or disable the custom feature");
        tooltips.put("customDamageMultiplier", "Set the damage multiplier for custom entities (0.0 - 10.0)");
        tooltips.put("allowCustomSpawning", "Allow custom entities to spawn naturally in the world");
        GameRuleTooltipRegistry.registerTooltips(tooltips);
        
        // 3. Create category and organize rules
        GameRuleCategoryRegistry.createCategory(
            "gamerule.category.mymod",
            Arrays.asList("enableCustomFeature", "customDamageMultiplier", "allowCustomSpawning")
        );
    }
}
```

### Example 6: Query and manage registrations

```java
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleTooltipRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleNameRegistry;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleCategoryRegistry;

// Check if tooltip is already registered
if (!GameRuleTooltipRegistry.hasRegisteredTooltip("myRule")) {
    GameRuleTooltipRegistry.registerTooltip("myRule", "My description");
}

// Check if display name is already registered
if (!GameRuleNameRegistry.hasRegisteredName("myRule")) {
    GameRuleNameRegistry.registerName("myRule", "My Rule");
}

// Get tooltip (automatically handles priority)
String tooltip = GameRuleTooltipRegistry.getTooltip("myRule");

// Get display name (automatically handles priority)
String displayName = GameRuleNameRegistry.getName("myRule");

// Get all rules in a category
List<String> rules = GameRuleCategoryRegistry.getRulesInCategory("gamerule.category.mymod");

// Get category for a rule
String category = GameRuleCategoryRegistry.getCategoryForRule("myCustomRule");

// Remove registrations
GameRuleTooltipRegistry.removeTooltip("myRule");
GameRuleNameRegistry.removeName("myRule");

// Clear all registrations
GameRuleTooltipRegistry.clearAllTooltips();
GameRuleNameRegistry.clearAllNames();
```

### Priority System

When you call `getTooltip()` or `getName()`, the system checks in this order:

**Tooltip Priority:**
1. **Localization file** - `gamerule.{ruleName}.tooltip.description`
2. **Tooltips registered via API**
3. **Built-in default descriptions** (vanilla rules only)

**Name Priority:**
1. **Localization file** - `gamerule.{ruleName}.name`
2. **Display names registered via API**
3. **Raw rule name** (e.g., `doFireTick`)

This means resource packs can override your API-registered content, perfect for multi-language support!

For example:
- You register the English name "My Custom Rule" via API
- A Chinese resource pack can override it with `gamerule.myCustomRule.name=我的自定义规则`
- Pretty neat, right? Chinese players see the localized name!

### Default Category Structure

Vanilla game rules are pre-categorized as follows:

**UI Display:**

Each category is displayed as a section with a centered title:

```
|-------------------|
|      Mobs         |    ← Category title (centered, localizable)
| doMobSpawning  [] |    ← Game rule controls
| doMobLoot      [] |
| mobGriefing    [] |
|-------------------|
```

**Note:** Categories are **not collapsible** — they're always expanded and visible.

```
World
  ├─ doFireTick
  ├─ doTileDrops
  └─ doDaylightCycle

Mobs
  ├─ doMobSpawning
  ├─ doMobLoot
  └─ mobGriefing

Player
  ├─ naturalRegeneration
  └─ keepInventory

Chat
  └─ commandBlockOutput
```

### Why This Matters

- ✅ **Better organization** - Categories make the UI clearer when you have many rules
- ✅ **User experience** - Players can quickly find related rules
- ✅ **Extensibility** - Each mod can have its own category
- ✅ **Backward compatible** - Old code still works!

So there you go — your game rules are all neatly organized in the editor!

---

## Mixin Classes

Here's what gets mixed into what:

| Mixin Class | Target | What it does |
|---|---|---|
| `MixinModernCreateWorld` | `GuiCreateWorld` | Replaces the world creation screen with the tabbed UI. |
| `MixinGuiSelectWorld` | `GuiSelectWorld` | Enhances the world selection screen. |
| `MixinIntegratedServer` | `IntegratedServer` | Applies difficulty and game rules when the world loads. |
| `access.IGuiCreateWorldAccess` | `GuiCreateWorld` | Accessor mixin — exposes private fields via getters/setters for external use. |

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

> **Note (6.0.0+)**: CreateWorldUI now requires CatFrame as a mandatory dependency. Make sure your users have CatFrame installed, or include it in your modpack.

```gradle
dependencies {
    implementation 'com.github.song682:CatFrame:Tag'
}
```