package com.colorgrid.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/** Colors of the calculator card (green palette of the expense-tracker calculator). */
class CalcColors(
    val panel: Int, val key: Int, val op: Int, val opText: Int, val eq: Int, val eqLip: Int,
    val util: Int, val utilText: Int, val display: Int, val result: Int
)

/** Name + description + 3 preview colors of a keyboard theme (used by the Settings picker). */
class ThemeInfo(val id: String, val title: String, val description: String, val swatch: List<Int>, val tag: String = "")

object KeyboardThemes {
    private fun c(s: String) = Color.parseColor(s)

    /** Key sound (a Sound Studio profile id) that goes with each theme. */
    fun soundFor(themeId: String): String = when (themeId) {
        "tok" -> "oreo"
        "machina" -> "cherry_brown_pbt"
        "arctic" -> "cherry_blue_abs"
        "walnut" -> "topre_purple"
        "magma" -> "cherry_black_abs"
        "monsoon" -> "cherry_black_pbt"
        "lumen" -> "cherry_red_abs"
        "glass" -> "crystal_purple"
        "rgb" -> "cherry_blue_abs"
        "retro" -> "cherry_blue_pbt"
        "neon" -> "crystal_purple"
        "sakura" -> "oreo"
        "matcha" -> "cherry_brown_pbt"
        "toknight" -> "cherry_red_abs"
        "titanium" -> "cherry_blue_pbt"
        "porcelain" -> "crystal_purple"
        "hifi" -> "cherry_brown_abs"
        "onyx" -> "crystal_purple"
        "lofi" -> "topre_purple"
        "bitpop" -> "cherry_blue_abs"
        "horizon" -> "oreo"
        else -> "cherry_brown_abs"
    }

    /** Built-in voice (see VoiceEngine) that goes with each theme. */
    fun voiceFor(themeId: String): String = when (themeId) {
        "tok" -> "boop"
        "toknight" -> "hud"
        "machina" -> "machina"
        "titanium" -> "titanium"
        "arctic" -> "clicky"
        "porcelain" -> "ceramic"
        "walnut" -> "marimba"
        "hifi" -> "hifi"
        "magma" -> "magma"
        "monsoon" -> "droplet"
        "lumen" -> "thock"
        "onyx" -> "crystal"
        "lofi" -> "topre"
        "bitpop" -> "chip"
        "horizon" -> "neon"
        "glass" -> "bubble"
        "classic" -> "linear"
        "rgb" -> "pulse"
        "retro" -> "deep"
        "neon" -> "neon"
        "sakura" -> "silk"
        "matcha" -> "carbon"
        else -> "boop"
    }

    /** Vibration pattern (see Haptics) that goes with each theme. */
    fun hapticFor(themeId: String): String = when (themeId) {
        "tok" -> "squish"
        "toknight" -> "blip"
        "machina" -> "thock"
        "titanium" -> "snap"
        "arctic" -> "clickjacket"
        "porcelain" -> "tink"
        "walnut" -> "mallet"
        "hifi" -> "clack"
        "magma" -> "rumble"
        "monsoon" -> "plip"
        "lumen" -> "thock"
        "onyx" -> "glasstick"
        "lofi" -> "keybed"
        "bitpop" -> "bit"
        "horizon" -> "zap"
        "glass" -> "pop"
        "classic" -> "tick"
        "rgb" -> "blip"
        "retro" -> "clack"
        "neon" -> "zap"
        "sakura" -> "squish"
        "matcha" -> "plip"
        else -> "blip"
    }

    /** Name of the space bar character of each theme. */
    fun mascotTitle(themeId: String): String = when (mascotFor(themeId)) {
        "chick" -> "Chick"; "robot" -> "Robot"; "gear" -> "Gear"; "nut" -> "Hex nut"; "penguin" -> "Penguin"
        "teacup" -> "Teacup"; "owl" -> "Owl"; "speaker" -> "Speaker"; "flame" -> "Flame"; "drop" -> "Raindrop"
        "orb" -> "Rainbow orb"; "gem" -> "Gem"; "lofi" -> "Lo-fi cat"; "invader" -> "Invader"; "sun" -> "Sunset"
        "bubble" -> "Bubble"; "cat" -> "Cat"; "ghost" -> "Ghost"; "cassette" -> "Cassette"; "alien" -> "Alien"
        "bunny" -> "Bunny"; "frog" -> "Frog"
        else -> "None"
    }

    /** The character that lives on the space bar of each theme. */
    fun mascotFor(themeId: String): String = when (themeId) {
        "tok" -> "chick"
        "toknight" -> "robot"
        "machina" -> "gear"
        "titanium" -> "nut"
        "arctic" -> "penguin"
        "porcelain" -> "teacup"
        "walnut" -> "owl"
        "hifi" -> "speaker"
        "magma" -> "flame"
        "monsoon" -> "drop"
        "lumen" -> "orb"
        "onyx" -> "gem"
        "lofi" -> "lofi"
        "bitpop" -> "invader"
        "horizon" -> "sun"
        "glass" -> "bubble"
        "classic" -> "cat"
        "rgb" -> "ghost"
        "retro" -> "cassette"
        "neon" -> "alien"
        "sakura" -> "bunny"
        "matcha" -> "frog"
        else -> ""
    }

    fun soundTitle(themeId: String): String =
        KeyboardSounds.profiles.firstOrNull { it.id == soundFor(themeId) }?.title ?: ""

    val all: List<ThemeInfo> = listOf(
        ThemeInfo("tok", "Tok", "Jelly caps \u00B7 cream & coral. Ripple on every press",
            listOf(c("#FBE0D2"), c("#FFFAF3"), c("#F4694A"), c("#C5EFDF")), "JELLY CAPS \u00B7 CREAM & CORAL"),
        ThemeInfo("toknight", "Tok Night", "Matte caps \u00B7 visor glow. Teal grid and a little robot",
            listOf(c("#0B0F14"), c("#1B2028"), c("#2EF2D0"), c("#7A4DFF")), "MATTE CAPS \u00B7 VISOR GLOW"),
        ThemeInfo("machina", "Machina", "PBT caps \u00B7 graphite alu. Orange backspace, glow on press",
            listOf(c("#33363D"), c("#EFE9DB"), c("#5B616B"), c("#F2711C")), "PBT CAPS \u00B7 GRAPHITE ALU"),
        ThemeInfo("titanium", "Titanium", "Bead-blasted titanium \u00B7 white caps. Orange backspace",
            listOf(c("#DEE1E5"), c("#F7F8FA"), c("#C9CED5"), c("#F26B1D")), "BEAD-BLASTED TITANIUM"),
        ThemeInfo("arctic", "Arctic", "Double-shot caps \u00B7 white alu. Cool blue ripple",
            listOf(c("#DDE3EA"), c("#FFFFFF"), c("#B9C4D3"), c("#3F6BFF")), "DOUBLE-SHOT CAPS \u00B7 WHITE ALU"),
        ThemeInfo("porcelain", "Porcelain", "Glazed porcelain \u00B7 blue ink drops",
            listOf(c("#F3F5F9"), c("#FFFFFF"), c("#2147A6")), "GLAZED PORCELAIN"),
        ThemeInfo("walnut", "Walnut", "Oiled walnut \u00B7 brass. Brass sparks",
            listOf(c("#3F2616"), c("#80502F"), c("#F2C14E")), "OILED WALNUT \u00B7 BRASS"),
        ThemeInfo("hifi", "Hi-Fi", "Retro caps \u00B7 walnut deck. Cream keys, brown mods",
            listOf(c("#3E2C1F"), c("#EFE3C8"), c("#9A5B34"), c("#F0A12F")), "RETRO CAPS \u00B7 WALNUT DECK"),
        ThemeInfo("magma", "Magma", "Basalt caps \u00B7 molten seams. Embers fly when you type",
            listOf(c("#0B0705"), c("#221915"), c("#FF6A00")), "BASALT CAPS \u00B7 MOLTEN SEAMS"),
        ThemeInfo("monsoon", "Monsoon", "Rain glass \u00B7 night blue. Droplet ripples",
            listOf(c("#06152A"), c("#1B3E70"), c("#3E92E8")), "RAIN GLASS \u00B7 NIGHT BLUE"),
        ThemeInfo("lumen", "Lumen", "Backlit caps \u00B7 light bleed. Rainbow letters and halos",
            listOf(c("#FF3DA5"), c("#FF8A1F"), c("#12E2B0"), c("#7A4DFF")), "BACKLIT CAPS \u00B7 LIGHT BLEED"),
        ThemeInfo("onyx", "Onyx Glass", "Smoked glass \u00B7 purple and teal waves",
            listOf(c("#3A0F4A"), c("#2E3C78"), c("#C04FD0"), c("#123A52")), "SMOKED GLASS \u00B7 WAVES"),
        ThemeInfo("lofi", "Lo-Fi Room", "Felt caps \u00B7 vinyl at dusk. Peach backspace",
            listOf(c("#583050"), c("#6A4668"), c("#F4A57A")), "FELT CAPS \u00B7 VINYL AT DUSK"),
        ThemeInfo("bitpop", "Bitpop", "Pixel caps \u00B7 arcade night. Yellow pixels and sparks",
            listOf(c("#160836"), c("#2A1A5E"), c("#FFE95A"), c("#FF3D81")), "PIXEL CAPS \u00B7 ARCADE NIGHT"),
        ThemeInfo("horizon", "Neon Horizon", "Retro-wave sunset \u00B7 grid floor. Cyan halos",
            listOf(c("#1A0533"), c("#FF3D9A"), c("#FFD35A"), c("#29E7FF")), "SUNSET GRID \u00B7 RETRO-WAVE"),
        ThemeInfo("glass", "Liquid Glass", "Clear glass \u00B7 daylight. Follows light / dark mode",
            listOf(c("#A9CBFF"), c("#FFFFFF"), c("#FBCDE4")), "CLEAR GLASS \u00B7 DAYLIGHT"),
        ThemeInfo("classic", "Classic", "Plain solid keys. Follows light / dark mode",
            listOf(c("#E9ECF2"), c("#FFFFFF"), c("#3F6BFF")), "PLAIN SOLID KEYS"),
        ThemeInfo("rgb", "RGB Gamer", "Black keycaps with a rainbow backlight wave",
            listOf(c("#0B0B10"), c("#1B1B24"), c("#FF2D95"), c("#00E5FF")), "BLACK CAPS \u00B7 RGB WAVE"),
        ThemeInfo("retro", "Retro Beige", "Beige keycaps like a classic buckling-spring keyboard",
            listOf(c("#CFC8B8"), c("#EDE6D5"), c("#4F6272")), "BEIGE CAPS \u00B7 BUCKLING SPRING"),
        ThemeInfo("neon", "Cyber Neon", "Purple night with magenta and cyan glow",
            listOf(c("#1B0B33"), c("#2D1750"), c("#FF2D95"), c("#00E5FF")), "PURPLE NIGHT \u00B7 NEON GLOW"),
        ThemeInfo("sakura", "Sakura Keycaps", "Pink pastel keycaps with a soft glow",
            listOf(c("#FFD3E2"), c("#FFF8FB"), c("#FF6FA3")), "PINK PASTEL CAPS"),
        ThemeInfo("matcha", "Matcha Mint", "Fresh green keycaps, matches the calculator colors",
            listOf(c("#BCDDC9"), c("#F4FBF6"), c("#2E8B57")), "MATCHA CAPS \u00B7 FRESH GREEN")
    )
}

class Palette(
    val dark: Boolean,
    val glass: Boolean,
    /** 3D "keycap" look: a darker lip under every key that shrinks while the key is pressed. */
    val keycap: Boolean,
    /** Rainbow backlight outline on every key. */
    val rgb: Boolean,
    /** Color of the keycap's lower edge. */
    val lip: Int,
    /** Flash color when a key is pressed (0 = none). */
    val glow: Int,
    /** Key corner radius in dp. */
    val radius: Float,
    /** Solid fallback background. */
    val bg: Int,
    /** Top-to-bottom gradient for the whole keyboard, or null for a solid [bg]. */
    val bgGradient: IntArray?,
    val key: Int,
    val special: Int,
    val text: Int,
    val subtext: Int,
    val accent: Int,
    val accentText: Int,
    val chip: Int,
    val selected: Int,
    val stroke: Int,
    /** 1dp outline drawn around keys (0 = none). */
    val keyStroke: Int,
    val spaceFrom: Int,
    val spaceTo: Int,
    val enterBg: Int,
    private val calc: CalcColors,
    /** Procedurally drawn background artwork. */
    val texture: Texture = Texture.NONE,
    /** Every key label gets its own rainbow color (Lumen). */
    val letterRainbow: Boolean = false,
    /** Letters are shown in capitals even without shift. */
    val capsLabels: Boolean = false,
    /** Default press effect of the theme: none, glow, ripple, sparks, halo. */
    val fx: String = "none",
    /** Text color on letter keys / on modifier keys (shift, 123, enter, space). */
    val keyText: Int = text,
    val modText: Int = text
) {
    val calcPanel get() = calc.panel
    val calcKey get() = calc.key
    val calcOp get() = calc.op
    val calcOpText get() = calc.opText
    val calcEq get() = calc.eq
    val calcEqLip get() = calc.eqLip
    val calcUtil get() = calc.util
    val calcUtilText get() = calc.utilText
    val calcDisplay get() = calc.display
    val calcResult get() = calc.result

    companion object {
        private fun c(s: String) = Color.parseColor(s)

        private val CALC_LIGHT = CalcColors(
            c("#FFFFFF"), c("#F2F5F3"), c("#E5EEE7"), c("#3F7D4F"), c("#4E8B58"), c("#3D6F46"),
            c("#FDECEC"), c("#E04747"), c("#F2F5F3"), c("#3E8E57")
        )
        private val CALC_DARK = CalcColors(
            c("#232A26"), c("#2F3733"), c("#34453A"), c("#8FD0A0"), c("#4E8B58"), c("#3A6B43"),
            c("#4A2E2F"), c("#FF8A8A"), c("#1C221F"), c("#7BD491")
        )

        fun isDark(context: Context, mode: Int): Boolean = when (mode) {
            1 -> false
            2 -> true
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }

        fun resolve(context: Context, mode: Int, themeId: String): Palette = when (themeId) {
            "rgb" -> rgbGamer()
            "retro" -> retro()
            "neon" -> neon()
            "sakura" -> sakura()
            "matcha" -> matcha()
            "tok" -> tok()
            "machina" -> machina()
            "arctic" -> arctic()
            "walnut" -> walnut()
            "magma" -> magma()
            "monsoon" -> monsoon()
            "lumen" -> lumen()
            "toknight" -> tokNight()
            "titanium" -> titanium()
            "porcelain" -> porcelain()
            "hifi" -> hifi()
            "onyx" -> onyx()
            "lofi" -> lofi()
            "bitpop" -> bitpop()
            "horizon" -> horizon()
            "classic" -> base(isDark(context, mode), false)
            else -> base(isDark(context, mode), true)
        }

        private fun base(dark: Boolean, glass: Boolean): Palette = if (dark) {
            Palette(
                dark = true, glass = glass, keycap = false, rgb = false, lip = 0, glow = 0,
                radius = if (glass) 13f else 9f, fx = if (glass) "ripple" else "none",
                bg = c("#1B1D23"),
                bgGradient = if (glass) intArrayOf(c("#1D2640"), c("#1B2F36"), c("#33223A")) else null,
                key = if (glass) Color.argb(62, 255, 255, 255) else c("#343842"),
                special = if (glass) Color.argb(36, 255, 255, 255) else c("#252830"),
                text = c("#EEF1F6"), subtext = c("#9AA1AE"),
                accent = if (glass) c("#4F7DF0") else c("#6F8FFF"),
                accentText = c("#FFFFFF"),
                chip = if (glass) Color.argb(48, 255, 255, 255) else c("#2B2E37"),
                selected = if (glass) Color.argb(90, 255, 255, 255) else c("#3A4260"),
                stroke = c("#6A7080"),
                keyStroke = if (glass) Color.argb(40, 255, 255, 255) else 0,
                spaceFrom = if (glass) c("#3A4F7A") else c("#343842"),
                spaceTo = if (glass) c("#5E3F69") else c("#343842"),
                enterBg = if (glass) c("#6A4670") else c("#343842"),
                calc = CALC_DARK
            )
        } else {
            Palette(
                dark = false, glass = glass, keycap = false, rgb = false, lip = 0, glow = 0,
                radius = if (glass) 13f else 9f, fx = if (glass) "ripple" else "none",
                bg = c("#E9ECF2"),
                bgGradient = if (glass) intArrayOf(c("#A9CBFF"), c("#B7EEE9"), c("#FBCDE4")) else null,
                key = if (glass) Color.argb(150, 255, 255, 255) else c("#FFFFFF"),
                special = if (glass) Color.argb(105, 255, 255, 255) else c("#CDD3DE"),
                text = c("#1C2029"), subtext = c("#5F6675"),
                accent = c("#3F6BFF"), accentText = c("#FFFFFF"),
                chip = if (glass) Color.argb(170, 255, 255, 255) else c("#FFFFFF"),
                selected = if (glass) Color.argb(220, 255, 255, 255) else c("#D3DDFF"),
                stroke = c("#B6BDCB"),
                keyStroke = if (glass) Color.argb(190, 255, 255, 255) else 0,
                spaceFrom = if (glass) c("#D4E6FF") else c("#FFFFFF"),
                spaceTo = if (glass) c("#FBD3EC") else c("#FFFFFF"),
                enterBg = if (glass) c("#FCDDF2") else c("#CDD3DE"),
                calc = CALC_LIGHT
            )
        }

        private fun rgbGamer() = Palette(
            dark = true, glass = false, keycap = true, rgb = true, lip = c("#050508"), glow = 0, radius = 9f, fx = "glow",
            bg = c("#0C0C12"), bgGradient = intArrayOf(c("#0B0B10"), c("#15101F")),
            key = c("#1B1B24"), special = c("#13131A"),
            text = c("#F2F2F7"), subtext = c("#8E8EA3"),
            accent = c("#E0245E"), accentText = c("#FFFFFF"),
            chip = c("#1B1B24"), selected = c("#33334A"), stroke = c("#6A6A80"), keyStroke = 0,
            spaceFrom = c("#1B1B24"), spaceTo = c("#1B1B24"), enterBg = c("#13131A"),
            calc = CALC_DARK
        )

        private fun retro() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#9A917B"), glow = c("#FFFFFF"), radius = 7f, fx = "glow",
            bg = c("#CFC8B8"), bgGradient = intArrayOf(c("#D6CFBF"), c("#BFB7A4")),
            key = c("#EDE6D5"), special = c("#CFC7B2"),
            text = c("#2A2A2A"), subtext = c("#6B6455"),
            accent = c("#4F6272"), accentText = c("#FFFFFF"),
            chip = c("#E4DCC8"), selected = c("#C3BAA4"), stroke = c("#9A917B"), keyStroke = 0,
            spaceFrom = c("#EDE6D5"), spaceTo = c("#EDE6D5"), enterBg = c("#CFC7B2"),
            calc = CALC_LIGHT
        )

        private fun neon() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#0F0422"), glow = c("#00E5FF"), radius = 10f, fx = "halo",
            bg = c("#1A0B2E"), bgGradient = intArrayOf(c("#1B0B33"), c("#2B0B40"), c("#0B1B3A")),
            key = c("#2D1750"), special = c("#20103C"),
            text = c("#E8F9FF"), subtext = c("#A99BD0"),
            accent = c("#FF2D95"), accentText = c("#FFFFFF"),
            chip = c("#2D1750"), selected = c("#4A2A80"), stroke = c("#7B3FE4"), keyStroke = c("#7B3FE4"),
            spaceFrom = c("#3A1A66"), spaceTo = c("#0E4A6A"), enterBg = c("#5A1A6E"),
            calc = CALC_DARK
        )

        private fun sakura() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#E7A3BC"), glow = c("#FF9EC0"), radius = 12f, fx = "ripple",
            bg = c("#FFDDE8"), bgGradient = intArrayOf(c("#FFE6EE"), c("#FFD3E2")),
            key = c("#FFF8FB"), special = c("#FFD9E6"),
            text = c("#6B2B45"), subtext = c("#A06A82"),
            accent = c("#FF6FA3"), accentText = c("#FFFFFF"),
            chip = c("#FFF1F6"), selected = c("#FFC2D8"), stroke = c("#E7A3BC"), keyStroke = 0,
            spaceFrom = c("#FFF8FB"), spaceTo = c("#FFE0EC"), enterBg = c("#FFD9E6"),
            calc = CALC_LIGHT
        )

        private fun matcha() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#8DBBA0"), glow = c("#7AD7A0"), radius = 11f, fx = "ripple",
            bg = c("#CFE8D8"), bgGradient = intArrayOf(c("#D8EEDF"), c("#BCDDC9")),
            key = c("#F4FBF6"), special = c("#D3EADB"),
            text = c("#1F4030"), subtext = c("#5C7F6B"),
            accent = c("#2E8B57"), accentText = c("#FFFFFF"),
            chip = c("#EAF6EE"), selected = c("#B5DDC4"), stroke = c("#8DBBA0"), keyStroke = 0,
            spaceFrom = c("#F4FBF6"), spaceTo = c("#E0F2E6"), enterBg = c("#D3EADB"),
            calc = CALC_LIGHT
        )

        private fun tok() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#E3BFA4"), glow = c("#FF9E80"), radius = 16f,
            bg = c("#FBE9DC"), bgGradient = intArrayOf(c("#FBE0D2"), c("#FDF1E3"), c("#E2F3EA")),
            key = c("#FFFAF3"), special = c("#F7D9C4"),
            text = c("#4A3A33"), subtext = c("#A38C80"),
            accent = c("#F4694A"), accentText = c("#FFFFFF"),
            chip = c("#FFF1E6"), selected = c("#FAD5C0"), stroke = c("#E3BFA4"), keyStroke = 0,
            spaceFrom = c("#E4FAF1"), spaceTo = c("#C5EFDF"), enterBg = c("#F7D9C4"),
            calc = CALC_LIGHT, texture = Texture.TOK, fx = "ripple"
        )

        private fun machina() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#A89F8A"), glow = c("#FFB070"), radius = 6f,
            bg = c("#2B2E34"), bgGradient = intArrayOf(c("#33363D"), c("#25282D")),
            key = c("#EFE9DB"), special = c("#5B616B"),
            text = c("#F2F2F2"), subtext = c("#A9AEB8"),
            accent = c("#F2711C"), accentText = c("#FFFFFF"),
            chip = c("#3A3E46"), selected = c("#555B66"), stroke = c("#7A808C"), keyStroke = 0,
            spaceFrom = c("#5B616B"), spaceTo = c("#5B616B"), enterBg = c("#5B616B"),
            calc = CALC_DARK, texture = Texture.BRUSHED, capsLabels = true, fx = "glow",
            keyText = c("#2A2A2A"), modText = c("#F2F2F2")
        )

        private fun arctic() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#A4B0C0"), glow = c("#9FC0FF"), radius = 10f,
            bg = c("#D5DBE3"), bgGradient = intArrayOf(c("#DDE3EA"), c("#C3CCD8")),
            key = c("#FFFFFF"), special = c("#B9C4D3"),
            text = c("#1E2530"), subtext = c("#5F6B7C"),
            accent = c("#3F6BFF"), accentText = c("#FFFFFF"),
            chip = c("#EDF0F4"), selected = c("#C3D2F5"), stroke = c("#A4B0C0"), keyStroke = 0,
            spaceFrom = c("#B4BFCE"), spaceTo = c("#B4BFCE"), enterBg = c("#B9C4D3"),
            calc = CALC_LIGHT, texture = Texture.BRUSHED, capsLabels = true, fx = "ripple"
        )

        private fun walnut() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#2E190C"), glow = c("#F2C14E"), radius = 12f,
            bg = c("#35200F"), bgGradient = intArrayOf(c("#3F2616"), c("#2A180C")),
            key = c("#80502F"), special = c("#6A4128"),
            text = c("#F3E2CC"), subtext = c("#C9A98A"),
            accent = c("#F2C14E"), accentText = c("#3A2410"),
            chip = c("#4D311D"), selected = c("#6E4A2E"), stroke = c("#8A6040"), keyStroke = 0,
            spaceFrom = c("#5C381F"), spaceTo = c("#5C381F"), enterBg = c("#6A4128"),
            calc = CALC_DARK, texture = Texture.WOOD, fx = "sparks"
        )

        private fun magma() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#050302"), glow = c("#FF7A1A"), radius = 10f,
            bg = c("#120A06"), bgGradient = intArrayOf(c("#0B0705"), c("#1B0E08")),
            key = c("#221915"), special = c("#18110E"),
            text = c("#F5EDE6"), subtext = c("#B79C8A"),
            accent = c("#FF6A00"), accentText = c("#FFFFFF"),
            chip = c("#2A1D16"), selected = c("#4A2C1C"), stroke = c("#5A3A28"), keyStroke = 0,
            spaceFrom = c("#130D0A"), spaceTo = c("#130D0A"), enterBg = c("#18110E"),
            calc = CALC_DARK, texture = Texture.LAVA, fx = "sparks"
        )

        private fun monsoon() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#071A33"), glow = c("#66B7FF"), radius = 16f,
            bg = c("#08203A"), bgGradient = intArrayOf(c("#06152A"), c("#0B2A4A"), c("#050E1C")),
            key = c("#1B3E70"), special = c("#143059"),
            text = c("#E6F1FF"), subtext = c("#8FB0D8"),
            accent = c("#3E92E8"), accentText = c("#FFFFFF"),
            chip = c("#16315A"), selected = c("#2A5598"), stroke = c("#4C7DBF"), keyStroke = c("#4C7DBF"),
            spaceFrom = c("#0E2547"), spaceTo = c("#0E2547"), enterBg = c("#143059"),
            calc = CALC_DARK, texture = Texture.RAIN, fx = "ripple"
        )

        private fun lumen() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#030304"), glow = c("#FF3DA5"), radius = 10f,
            bg = c("#0A0A0F"), bgGradient = intArrayOf(c("#0A0A0F"), c("#0E0A15")),
            key = c("#15151B"), special = c("#101015"),
            text = c("#F0F0F5"), subtext = c("#9C9CB0"),
            accent = c("#24242C"), accentText = c("#FFFFFF"),
            chip = c("#1A1A22"), selected = c("#33334A"), stroke = c("#55556A"), keyStroke = 0,
            spaceFrom = c("#101015"), spaceTo = c("#101015"), enterBg = c("#101015"),
            calc = CALC_DARK, texture = Texture.LUMEN, letterRainbow = true, fx = "halo"
        )

        private fun tokNight() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#06080B"), glow = c("#2EF2D0"), radius = 16f,
            bg = c("#0B0F14"), bgGradient = intArrayOf(c("#0B0F14"), c("#0F1A1C")),
            key = c("#1B2028"), special = c("#141920"),
            text = c("#F2F5F7"), subtext = c("#8C97A3"),
            accent = c("#2EF2D0"), accentText = c("#04201B"),
            chip = c("#1B2028"), selected = c("#2A3340"), stroke = c("#3B4654"), keyStroke = 0,
            spaceFrom = c("#12171D"), spaceTo = c("#12171D"), enterBg = c("#141920"),
            calc = CALC_DARK, texture = Texture.GRID, fx = "ripple"
        )

        private fun titanium() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#9AA1AB"), glow = c("#FFB07A"), radius = 12f,
            bg = c("#D7DADF"), bgGradient = intArrayOf(c("#DEE1E5"), c("#C4C8CE")),
            key = c("#F7F8FA"), special = c("#C9CED5"),
            text = c("#22262B"), subtext = c("#5F6670"),
            accent = c("#F26B1D"), accentText = c("#FFFFFF"),
            chip = c("#ECEEF1"), selected = c("#C9CED5"), stroke = c("#9AA1AB"), keyStroke = 0,
            spaceFrom = c("#C3C8CF"), spaceTo = c("#C3C8CF"), enterBg = c("#C9CED5"),
            calc = CALC_LIGHT, texture = Texture.BEADBLAST, fx = "ripple"
        )

        private fun porcelain() = Palette(
            dark = false, glass = false, keycap = true, rgb = false, lip = c("#B5C0D2"), glow = c("#7FA2F5"), radius = 14f,
            bg = c("#EEF1F6"), bgGradient = intArrayOf(c("#F3F5F9"), c("#DCE3EE")),
            key = c("#FFFFFF"), special = c("#DDE5F3"),
            text = c("#1F2A44"), subtext = c("#6877A0"),
            accent = c("#2147A6"), accentText = c("#FFFFFF"),
            chip = c("#F6F8FC"), selected = c("#CAD7F0"), stroke = c("#B5C0D2"), keyStroke = 0,
            spaceFrom = c("#E4EAF6"), spaceTo = c("#D2DCF0"), enterBg = c("#DDE5F3"),
            calc = CALC_LIGHT, texture = Texture.PORCELAIN, fx = "ripple"
        )

        private fun hifi() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#B8A57F"), glow = c("#FFD08A"), radius = 7f,
            bg = c("#3B2A1E"), bgGradient = intArrayOf(c("#3E2C1F"), c("#2A1D14")),
            key = c("#EFE3C8"), special = c("#9A5B34"),
            text = c("#F6EAD2"), subtext = c("#C9AE8A"),
            accent = c("#F0A12F"), accentText = c("#2A1A08"),
            chip = c("#4E3524"), selected = c("#6E4A2E"), stroke = c("#8A6040"), keyStroke = 0,
            spaceFrom = c("#9A5B34"), spaceTo = c("#9A5B34"), enterBg = c("#9A5B34"),
            calc = CALC_DARK, texture = Texture.WOOD, capsLabels = true, fx = "glow",
            keyText = c("#2A2118"), modText = c("#FBEFD9")
        )

        private fun onyx() = Palette(
            dark = true, glass = false, keycap = false, rgb = false, lip = 0, glow = c("#C04FD0"), radius = 16f,
            bg = c("#1B1233"), bgGradient = intArrayOf(c("#3A0F4A"), c("#123A52"), c("#0C1A2B")),
            key = Color.argb(150, 60, 56, 130), special = Color.argb(120, 40, 50, 100),
            text = c("#F1ECFF"), subtext = c("#B3A8D8"),
            accent = c("#C04FD0"), accentText = c("#FFFFFF"),
            chip = Color.argb(110, 60, 56, 130), selected = Color.argb(170, 110, 100, 200),
            stroke = c("#8E8BE0"), keyStroke = Color.argb(120, 170, 160, 255),
            spaceFrom = Color.argb(130, 30, 60, 110), spaceTo = Color.argb(130, 60, 40, 120), enterBg = Color.argb(160, 90, 60, 170),
            calc = CALC_DARK, texture = Texture.WAVES, fx = "ripple"
        )

        private fun lofi() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#2A1A2C"), glow = c("#F4A57A"), radius = 16f,
            bg = c("#4A2A44"), bgGradient = intArrayOf(c("#583050"), c("#2B1B32")),
            key = c("#6A4668"), special = c("#4F3350"),
            text = c("#F6E3E8"), subtext = c("#C9A2B4"),
            accent = c("#F4A57A"), accentText = c("#3B1D12"),
            chip = c("#58395A"), selected = c("#7C5580"), stroke = c("#8A6590"), keyStroke = 0,
            spaceFrom = c("#4A2F4C"), spaceTo = c("#4A2F4C"), enterBg = c("#4F3350"),
            calc = CALC_DARK, texture = Texture.VINYL, fx = "ripple"
        )

        private fun bitpop() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#0B0420"), glow = c("#5CF2FF"), radius = 3f,
            bg = c("#12062B"), bgGradient = intArrayOf(c("#160836"), c("#0D0422")),
            key = c("#2A1A5E"), special = c("#1D1145"),
            text = c("#FFE95A"), subtext = c("#9C8CD8"),
            accent = c("#FF3D81"), accentText = c("#FFFFFF"),
            chip = c("#241450"), selected = c("#3E2A86"), stroke = c("#7A5CFF"), keyStroke = c("#7A5CFF"),
            spaceFrom = c("#1D1145"), spaceTo = c("#1D1145"), enterBg = c("#1D1145"),
            calc = CALC_DARK, texture = Texture.PIXELS, fx = "sparks"
        )

        private fun horizon() = Palette(
            dark = true, glass = false, keycap = true, rgb = false, lip = c("#0D0319"), glow = c("#29E7FF"), radius = 10f,
            bg = c("#1A0533"), bgGradient = intArrayOf(c("#1A0533"), c("#3A0B4D"), c("#120626")),
            key = c("#2A1148"), special = c("#1F0C38"),
            text = c("#FFD6F6"), subtext = c("#B88FD0"),
            accent = c("#FF2E97"), accentText = c("#FFFFFF"),
            chip = c("#2A1148"), selected = c("#4A2080"), stroke = c("#B24DFF"), keyStroke = c("#B24DFF"),
            spaceFrom = c("#1F0C38"), spaceTo = c("#1F0C38"), enterBg = c("#1F0C38"),
            calc = CALC_DARK, texture = Texture.HORIZON, fx = "halo"
        )
    }
}
