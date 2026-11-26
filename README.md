# Cheeseboard

A real Android keyboard (Input Method Service) in a Gboard-style layout: word suggestions, a full emoji panel,
a roomy calculator and a clipboard history.

- Package: `com.colorgrid.keyboard` - Kotlin - minSdk 26 - targetSdk 35
- Keyboard UI: native Views (reliable inside an IME). Settings app: Jetpack Compose + Material 3.
- Internet is only used for GIF search (emoji panel > GIF). No signing secrets needed.

## What's new in v1.8.0

- **New app design** in the style of the Klikey app: mono captions, big light titles, rounded bordered cards, floating bottom
  menu with four tabs: **Type**, **Themes**, **Sound**, **Settings**.
  - **Type**: a test chat. Tap *Try* on a theme and it opens here with the keyboard up, so you can feel the theme right away.
  - **Themes**: one big card per theme (preview, number, name, tagline, VOICE / HAPTIC / SPACE, Try and Wear).
  - **Sound**: Sound Lab. Theme voice on/off, **24 voices** (Boop, Carbon, Machina, Titanium, Crystal, Pulse, Topre, Ceramic, Clicky,
    Deep tactile, Airy, Bubble, HUD, Neon, Droplet, Magma, Marimba, Chip, Hi-Fi, Shadow, Linear, Thock, Silk, Silent), ruler sliders
    for Volume / Pitch / Tone / Haptics / Key travel, Groove (Off / Subtle / Musical), Haptic pattern (Theme / Crisp / Soft /
    Heavy / Double), key-specific sounds and a tap-to-hear test strip. The old recorded sample packs are at the end of the voice list.
  - **Settings**: your keyboard, setup status, typing, layout, height, press effects, GIF search, Color Grid, clipboard, about.
- **Per-theme haptics**: every theme has its own vibration feel (Squish, Blip, Thock, Firm tick, Rumble, Mallet, Plip, Pop,
  Glass tick, Clack, Snap, Click jacket, Tink, 8-bit, Keybed, Zap), heavier on space and return.
- Voices are synthesized on the phone (no sound files), so pitch, tone and key travel change instantly.

## What was new in v1.7.0

- **Space bar mascot**: every theme has its own little character (chick, robot, gear, penguin, owl, flame, ...), drawn in code.
  It hops when you type. Turn it off in Settings.
- **8 more themes** (22 in total): Tok Night, Titanium, Porcelain, Hi-Fi, Onyx Glass, Lo-Fi Room, Bitpop, Neon Horizon.
- **Emoji search** (emoji panel > Search): find emoji by name or keyword (happy, cat, heart...), with a small keyboard right under the results.
- **GIF search** (emoji panel > GIF), live while you type. GIFs are sent with the image-keyboard API (WhatsApp, Telegram, Messenger, ...);
  in apps that do not accept GIFs the link is pasted instead. Tenor's API was shut down by Google on 30 June 2026, so this uses
  KLIPY (free key from klipy.com/developers) or GIPHY. Paste your key in Settings > GIF search.

## What was new in v1.6.0

- **New app UI** in a Tailwind style: slate/indigo palette, rounded-2xl bordered cards, pill chips, floating bottom nav.
  Tabs: Home (dashboard with your theme, setup status and quick toggles), Themes, Settings, About.
- **Keyboard covered by the navigation**: the in-app nav bar now hides while the keyboard is open, the screen scrolls above the
  keyboard (`imePadding`), the keyboard keeps its keys above the system navigation bar, and changing a theme no longer
  rebuilds the whole input window (it swaps the keyboard inside a fixed container).
- **Match key sound**: wearing a theme also picks the Sound Studio profile that goes with it (can be turned off in Themes).
- **Emoji row** above the keys (recent emoji) and **Compact / Regular / Tall** keyboard height buttons.

## What was new in v1.5.0

- **Themes tab** like a theme store: a live preview card for every theme, **Try** (types with it without saving) and **Wear** (keeps it).
- **7 new themes with drawn textures**: Tok (jelly caps, cream & coral), Machina (PBT caps on brushed graphite),
  Arctic (white double-shot caps), Walnut (wood grain + brass), Magma (lava cracks + embers), Monsoon (rain glass),
  Lumen (backlit rainbow letters). Backgrounds are drawn in code, no image files.
- **Press effects**: ripple, sparks, halo or glow (per theme, or pick one in Settings). **Key previews** bubble above your finger.
- **Settings**: Number row, Arrow keys, Long-press symbols on/off.

## What's new in v1.4.0

- **5 mechanical-keyboard themes** (Settings -> Keyboard theme): RGB Gamer, Retro Beige, Cyber Neon, Sakura Keycaps, Matcha Mint
  (plus the original Glass and Classic). They have 3D keycaps (the key sinks when pressed).
- **Key effects** (Settings, on/off): press glow on every key, and a rainbow RGB backlight wave on RGB Gamer.
- **Grid is now just the lines**: the keypad panel ("Pang-ilan") was removed. Toggle it from the keyboard menu; drag the gray bar to
  move it and the pink square to resize it.

## What was new in v1.3.0

- **Color Grid is back as a toggle.** Tap the 4-square icon (top left of the keyboard) -> **Grid**. Off by default; when on, the
  floating numbered 12 x 3 grid (drag bar, resize grip, number keypad panel) shows over other apps. First time it asks for
  "Display over other apps". Code is the old `colorgrid_tool` overlay, merged into this app (`GridOverlayService.kt`, `GridOverlayView.kt`),
  so you only need this one APK.

## What's in v1.2.0

- **New layout** (4 rows, no number row): `qwertyuiop` / `asdfghjkl` / shift + `zxcvbnm` + backspace /
  `123` + emoji + space + `.` + enter. The numbers and symbols are small hints in the corner of each key;
  **long-press a key to type its hint** (q=1 ... p=0, a=@ s=# d=& f=* g=- h=+ j== k=( l=), z=_ x=$ c=" v=' b=: n=; m=/).
- **Suggestion strip** above the keys: three predicted words (tap one to insert it). Left button (4 squares) opens
  Calculator / Settings / Hide; right button opens the clipboard. Turn suggestions on/off in **Settings -> Typing**.
  Words you type often are learned on-device (can be turned off / cleared). Not used in password, e-mail or URL fields.
- **Long-press `.`** shows `, ? ! ' " : ; -`. **Long-press space** switches keyboard (the old globe key).
- **Emoji panel** like Gboard: category bar at the bottom (Recent, Smileys, People, Animals, Food, Travel, Activities,
  Objects, Symbols, Flags), ABC key and backspace. Full Unicode 16 emoji list (including the cat faces
  😺 😸 😹 😻 😼 😽 🙀 😿 😾); emojis your phone cannot draw yet are hidden automatically.
- **Calculator** redone with the expense-tracker style: bigger keys, rounded white key card, green palette,
  green result display, **Copy** button (inserts the result and hides the keyboard).
- **Number pad** for number/phone fields with `?` and `$` keys (`.` long-press = `,`, `0` long-press = `-`).
- **Symbols pages** (`123`, `=\<`) keep `$` and `?`.
- **Removed:** Text Replacement and the Color Grid / custom colors.

## Build the APK with GitHub Actions

1. Upload this whole folder to a GitHub repo (keep `gradlew` and `gradle/wrapper/gradle-wrapper.jar`).
2. The *Build APK* workflow runs on every push to `main`/`master` (or run it manually from the Actions tab).
3. Download the artifact `Cheeseboard-debug-apk` -> `Cheeseboard-debug.apk` and install it.

Local build: `./gradlew assembleDebug` (JDK 17 + Android SDK 35).

## Project layout

```
app/src/main/java/com/colorgrid/keyboard/
  ColorGridKeyboardService.kt   InputMethodService: commitText, clipboard, enter/backspace, text-before-cursor
  KeyboardView.kt               keys, suggestion strip, numpad, calculator, emoji and clipboard panels
  KeyboardHost.kt               interface between view and service
  Suggester.kt                  offline word prediction + on-device word learning
  Store.kt  Models.kt           settings, clipboard history, recent emoji (SharedPreferences + JSON)
  Palette.kt  IconView.kt       themes (incl. calculator palette) and canvas-drawn icons
  MainActivity.kt               Compose app: setup guide, settings, about
app/src/main/assets/
  words.txt                     ~24k English + Filipino words, most common first (suggestions)
  emoji.txt                     emoji list by category (Unicode order)
  key_sounds/                   Sound Studio samples
```

See `SOUND_ASSET_ATTRIBUTION.txt` for the key-sound credits.
