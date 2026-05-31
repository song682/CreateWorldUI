# CatFrame UI Components

These are the UI building blocks provided by [CatFrame](https://github.com/song682/CatFrame) — the library mod that CreateWorldUI (6.0.0+) depends on. If you're writing a custom tab or any screen that needs cycling buttons, panel rendering, or the tab system, this is your go-to reference.

---

## GuiCyclableButton\<T\>

**Package**: `decok.dfcdvadstf.catframe.ui.GuiCyclableButton`

A generic cyclable button — similar to modern Minecraft's `CyclingButtonWidget<T>`. Click it or scroll your mouse wheel to cycle through a typed list of values. Left-click or scroll down goes forward; scroll up goes backward.

### How it works

You build one with a **Builder** — give it a value-to-text function, a list of values, and an update callback. The button handles all the cycling internally; you just react to value changes.

Key pieces:
- **`ValueToText<T>`** — converts your value to the display string. Gets called on every text refresh.
- **`Values<T>`** — provides the list of values to cycle through (supports dynamic lists too).
- **`UpdateCallback<T>`** — fires when the value changes: `(button, newValue) -> { ... }`
- **`getValue()` / `setValue(T)`** — read or programmatically set the current value.

### Example

```java
import decok.dfcdvadstf.catframe.ui.GuiCyclableButton;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.client.resources.I18n;

// A button that cycles through difficulties
GuiCyclableButton<EnumDifficulty> diffButton = GuiCyclableButton.<EnumDifficulty>builder(
        d -> I18n.format("options.difficulty") + ": " + I18n.format(d.getDifficultyResourceKey()))
    .values(EnumDifficulty.values())
    .initially(EnumDifficulty.NORMAL)
    .build(200, width / 2 - 104, height / 2, 208, 20, (button, difficulty) -> {
        // React to the new value — no manual index juggling needed
        yourDifficultySetter(difficulty);
    });
addButton(diffButton);
```

Need a label prefix? Pass it as the `String label` argument in `build()` — the display becomes `"label: valueText"` automatically:

```java
// With a label — display shows "Difficulty: Easy" etc.
.build(200, x, y, w, h, I18n.format("options.difficulty"), callback);

// Without a label — your ValueToText controls the full string
.build(200, x, y, w, h, callback);
```

Boolean on/off toggle? There's a shortcut:

```java
GuiCyclableButton<Boolean> cheatsBtn = GuiCyclableButton.onOffBuilder()
    .initially(false)
    .build(201, x, y, 208, 20, (button, value) -> setAllowCheats(value));
```

> **Tip**: Call `button.updateText()` in your `drawScreen()` if the display depends on external state. And use `setValue(T)` to sync the button when something else changes the underlying value.

---

## ContentPanelRenderer

**Package**: `decok.dfcdvadstf.catframe.ui.ContentPanelRenderer`

A shared renderer for content panels — gives you the header separator, a tiled panel background, and the footer separator. Use the pieces you want individually, or let `drawContentPanel()` do the whole thing in one shot.

### What you get

| Method | What it draws |
|---|---|
| `drawContentPanel(x, top, width, bottom)` | The whole thing — header line, tiled background, footer line. |
| `drawHeaderSeparator(x, y, width)` | Just the top separator line (2px tall, tiled). |
| `drawFooterSeparator(x, y, width)` | Just the bottom separator line (2px tall, tiled). |
| `drawSeparator(x, y, width, texture)` | A separator line with your own 32×2 texture. |
| `drawPanelBackground(x, y, width, height)` | Just the panel background (tiles the 16×16 texture). |

### Textures

| Field | Path | Size |
|---|---|---|
| `HEADER_SEPARATOR` | `createworldui:textures/gui/header_separator.png` | 32×2 |
| `FOOTER_SEPARATOR` | `createworldui:textures/gui/footer_separator.png` | 32×2 |
| `PANEL_BACKGROUND` | `createworldui:textures/gui/panel_background.png` | 16×16 |

You can override these in your resource pack — or pass a custom `ResourceLocation` to `drawSeparator()` if you want a different style.

### Quick Example

```java
import decok.dfcdvadstf.catframe.ui.ContentPanelRenderer;

// Draw a full content panel in one call
ContentPanelRenderer.drawContentPanel(panelX, panelTop, panelWidth, panelBottom);

// Or draw pieces separately
ContentPanelRenderer.drawHeaderSeparator(panelX, panelTop, panelWidth);
ContentPanelRenderer.drawPanelBackground(panelX, panelTop + 2, panelWidth, panelHeight - 4);
ContentPanelRenderer.drawFooterSeparator(panelX, panelBottom - 2, panelWidth);
```

> **Note**: The separator height is always 2 GUI pixels (`SEPARATOR_HEIGHT = 2`). When using `drawContentPanel()`, the background is automatically placed between the two separators.

---

## Tab System

The tab system from CatFrame provides a clean way to build multi-tab screens. Each tab implements the `Tab` interface, and you can extend `AbstractScreenTab` for convenience.

### Tab Interface

**Package**: `decok.dfcdvadstf.catframe.ui.tab.Tab`

The core contract for a tab:

```java
public interface Tab {
    void initGui(TabManager tabManager, int width, int height);
    void drawScreen(int mouseX, int mouseY, float partialTicks);
    void actionPerformed(GuiButton button);
    void mouseClicked(int mouseX, int mouseY, int mouseButton);
    void keyTyped(char typedChar, int keyCode);
    int getTabId();
    String getTabName();
    void setVisible(boolean visible);

    /** Default: {@code catframe:textures/gui/tabs.png} */
    default ResourceLocation getTabTexture();
}
```

### AbstractScreenTab

**Package**: `decok.dfcdvadstf.catframe.ui.tab.AbstractScreenTab`

A base implementation that handles the boring stuff — button visibility, localization, button management, and tab texture. Extend this for your custom tabs.

```java
import decok.dfcdvadstf.catframe.ui.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;

public class MyCustomTab extends AbstractScreenTab {

    public MyCustomTab() {
        super(103, "mymod.tab.custom"); // Tab ID and localization key
    }

    @Override
    public void initGui(TabManager tabManager, int width, int height) {
        super.initGui(tabManager, width, height);
        // Add your buttons and fields here
        setVisible(false); // Always hide by default — TabManager will show the active one
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

    // Optional: custom tab texture (default is tabs.png)
    @Override
    public ResourceLocation getTabTexture() {
        // Return a custom texture, or rely on the default
        return new ResourceLocation("mymod", "textures/gui/my_tabs.png");
    }
}
```

> **Tip**: Call `setTabTexture(ResourceLocation)` from the constructor or `initGui()` if you prefer a setter over overriding `getTabTexture()`. The default texture is `catframe:textures/gui/tabs.png`.

### TabManager

**Package**: `decok.dfcdvadstf.catframe.ui.tab.TabManager`

`TabManager` handles tab switching, state persistence (survives window resizes), and gives you access to the parent screen.

| Method | What it does |
|---|---|
| `addButton(GuiButton)` | Add a button to the screen's button list. |
| `registerTab(Tab)` | Register a tab instance (internal — use `TabRegistry` instead). |
| `switchToTab(int tabId)` | Switch to a tab by ID. |
| `drawScreen(...)` | Delegate drawing to the current tab. |
| `actionPerformed(GuiButton)` | Delegate button clicks to the current tab. |
| `mouseClicked(...)` | Delegate mouse clicks to the current tab. |
| `keyTyped(...)` | Delegate keyboard input to the current tab. |
| `reinitializeTabs(width, height)` | Re-init all tabs on window resize (preserves current tab). |
| `getScreen()` | Get the parent `GuiScreen` instance. |
| `getCurrentTabId()` | Get the currently active tab ID. |
| `getTabCount()` | Get the total number of registered tabs. |
| `getTabBar()` | Get the optional `TabBar` associated with this manager, or `null`. |

Two ways to construct a `TabManager` — both require a `barId` so it knows which bucket of tabs to load:

```java
// Option 1: With a TabBar — barId is taken from bar.getBarId() automatically
TabBar bar = new MyModTabBar();
TabManager manager = new TabManager(screen, buttonList, width, height, bar);

// Option 2: With a bare barId string — no TabBar, no custom background
TabManager manager = new TabManager(screen, buttonList, width, height, "my_mod_bar");
```

Either way, `TabManager` only loads entries registered under that specific `barId` — tabs from other bars won't leak in.

### TabState

**Package**: `decok.dfcdvadstf.catframe.ui.tab.TabState`

An enum that defines texture coordinates and text colors for the four tab interaction states — `NORMAL`, `HOVER`, `SELECTED`, `SELECTED_HOVER`. Used by the tab button renderer to pick the right texture slice and color.

| State | Texture (u, v) | Text Color |
|---|---|---|
| `NORMAL` | (0, 0) | White (`0xFFFFFF`) |
| `HOVER` | (0, 24) | Yellow (`0xFFFF55`) |
| `SELECTED` | (0, 48) | White (`0xFFFFFF`) |
| `SELECTED_HOVER` | (0, 72) | Yellow (`0xFFFF55`) |

---

## TabBar

**Package**: `decok.dfcdvadstf.catframe.ui.tab.TabBar`

`TabBar` is an abstract container bar for tabs. It provides a common background for the tab area and holds the tabs that belong to it. Each `TabBar` has a unique ID, and subclasses customise the look (solid colour fill + optional tiled texture, defaults to solid black).

This is the piece you extend if you want a distinct visual style for your screen's tab area.

### Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `barId` | `String` | *(required)* | Unique identifier for this bar, set via constructor. |
| `backgroundColor` | `int` | *（optional）* default: `0xFF000000` | Solid background colour in ARGB format. |
| `backgroundTexture` | `ResourceLocation` | *（optional）* default: `null` | Optional tiled background texture (16×16 tile). |
| `tabTexture` | `ResourceLocation` | *（optional）* default: `catframe:textures/gui/tabs.png` | Default tab button texture for all tabs in this bar. |

### Key Methods

| Method | What it does |
|---|---|
| `drawBackground(x, y, width, height)` | Draws the bar background — solid colour fill first, then optional tiled texture on top. |
| `registerEntry(TabEntry)` | Register a `TabRegistry.TabEntry` into this bar. Called automatically by `TabManager` during construction. |
| `getAllEntries()` | Get all registered entries (ordered by insertion). |
| `getEntry(int tabId)` | Get an entry by its tab ID. |
| `getOrCreateTab(int tabId)` | Get an existing tab instance, or create one lazily from its entry's factory. |
| `registerTab(Tab)` | Register a tab instance directly into this bar. |
| `getAllTabs()` | Get all tab instances (ordered by creation). |
| `getTab(int tabId)` | Get a tab by its ID. |
| `containsTab(int tabId)` | Check if this bar contains a tab with the given ID. |
| `getTabCount()` | Get the number of tab instances. |
| `getBarId()` | Get the unique bar identifier. |

Subclasses may override `drawTiledBackground(x, y, width, height)` to customise how the tiled texture is rendered.

### Custom TabBar Example (After 0.0.3)

```java
import decok.dfcdvadstf.catframe.ui.tab.TabBar;
import net.minecraft.util.ResourceLocation;

public class MyModTabBar extends TabBar {

    public MyModTabBar() {
        super("my_mod_bar");           // Unique bar ID
        setBackgroundColor(0xFF2D2D2D); // Optional dark grey solid fill
        setBackgroundTexture(           // Optional tiled texture
            new ResourceLocation("mymod", "textures/gui/bar_bg.png"));
        setTabTexture(                  // Optional: override default tab texture
            new ResourceLocation("mymod", "textures/gui/tab_buttons.png"));
    }
}
```

> **Notes**: Although except bar ID, rest of these are optional and can leave it unimplemented, this will end up with a soild black fill.
> Which means: `public MyModTabBar() {super("my_mod_bar");}` equals 
> ```java
> public MyModTabBar() {
>   super("my_mod_bar");
>   setBackgroundColor("0XFF000000");
>   setTabTexture(
>   new ResourceLocation("catframe", "textures/gui/tabs/buttons.png"));
> }
> ```

### Usage with TabManager

```java
// 1. Register tabs during preInit — specify which bar they belong to
TabRegistry.registerTab("my_mod_bar", MyCustomTab::new, 103, "mymod.tab.custom", 5);

// 2. Create your TabBar (its barId must match what you registered under)
TabBar bar = new MyModTabBar(); // super("my_mod_bar") inside

// 3. Pass it to TabManager — only "my_mod_bar" entries get loaded
TabManager manager = new TabManager(screen, buttonList, width, height, bar);
```

---

## TabRegistry

**Package**: `decok.dfcdvadstf.catframe.ui.tab.TabRegistry`

The static registry for external mods to register custom tabs. Entries are **bucketed by `barId`** — so a statistics screen's tabs won't pollute the create-world screen, and vice versa. Register your tabs during mod init with the target `barId`, and when the corresponding screen opens, `TabManager` freezes that bucket and creates only its own tabs.

### Key Methods

| Method | What it does |
|---|---|
| `registerTab(String barId, Supplier<Tab>, int tabId, String nameKey, int priority)` | Register a tab to a specific bar with factory, ID, name key, and priority. |
| `registerTab(String barId, Supplier<Tab>, int tabId, String nameKey)` | Same thing, but priority defaults to `tabId`. |
| `getEntries(String barId)` | Get all entries for that bar, sorted by priority (unmodifiable). |
| `isFrozen(String barId)` | Check if a specific bar's bucket is frozen. |
| `freeze(String barId)` | Freeze a bar's bucket. Called internally by `TabManager`. |
| `clear(String barId)` | Clear a specific bar's bucket (testing/reset). |
| `clearAll()` | Clear everything (testing/reset). |

### Parameters

| Parameter | Type | What it means |
|---|---|---|
| `barId` | `String` | Which bar this tab belongs to. Must match the `TabBar`'s `barId` (or the string you pass to `TabManager`). |
| `factory` | `Supplier<Tab>` | A factory that creates your `Tab` instance. Called once when `TabManager` is constructed. |
| `tabId` | `int` | Unique tab ID **within a bar**. Built-in tabs use 100–102, so **start from 103**. |
| `nameKey` | `String` | Localization key for the tab name (like `"mymod.tab.custom"`). |
| `priority` | `int` | Sort order — lower comes first. Defaults to `tabId` if not specified. Built-in tabs use 0, 1, 2. |

Entries are sorted by `priority` ascending, then `tabId` ascending as a tiebreaker. The uniqueness check for `tabId` only applies within the same `barId` bucket — two different bars can safely share the same numeric ID.

### Registration Example

```java
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import decok.dfcdvadstf.catframe.ui.tab.AbstractScreenTab;
import decok.dfcdvadstf.catframe.ui.tab.Tab;
import decok.dfcdvadstf.catframe.ui.tab.TabManager;
import decok.dfcdvadstf.catframe.ui.tab.TabRegistry;
import net.minecraft.client.gui.GuiButton;

@Mod(modid = "mymod", name = "My Mod", version = "1.0")
public class MyMod {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Register to the "create_world" bar — only shows up on that screen
        TabRegistry.registerTab(
            "create_world",          // Target bar ID
            MyCustomTab::new,        // Supplier<Tab> factory
            103,                     // Unique tab ID (start from 103)
            "mymod.tab.custom",      // Localization key
            5                        // Sort priority (lower = earlier)
        );
    }
}
```

And don't forget the localization in your lang file:
```properties
mymod.tab.custom=My Tab
```

### Things to Keep in Mind

- **Register early** — like `FMLPreInitializationEvent`. Once `TabManager` is constructed for a given bar, that bar's bucket freezes; registering to it after that throws an `IllegalStateException`.
- **Tab IDs must be unique within a bar** — duplicate IDs in the same bucket throw an `IllegalArgumentException`. Different bars can reuse the same numeric IDs without conflict.
- **`barId` must match** — the string you pass to `registerTab()` must be the same as the `TabBar`'s `barId` (or the string passed directly to `TabManager`). Typos mean your tab silently won't appear.
- **`AbstractScreenTab` constructor must match** — the `tabId` and `nameKey` you pass there need to be the same ones you gave `TabRegistry.registerTab()`.
- **`initGui()` gets called by `TabManager`** — on init and on resize. Always call `super.initGui()` and set `setVisible(false)` at the end.

---

## Dependency Setup

To use CatFrame as a dependency, add this to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.song682:CatFrame:Tag'
}
```

Replace `Tag` with the version you want from the [repository](https://github.com/song682/CatFrame).
