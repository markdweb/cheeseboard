package com.colorgrid.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

data class TabRequest(val id: Int, val tab: String?)

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)
    private var tabRequest by mutableStateOf(TabRequest(0, null))
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = Store(this)
        tabRequest = TabRequest(1, intent?.getStringExtra("tab"))
        setContent {
            CheeseTheme {
                AppRoot(store, tick, tabRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tabRequest = TabRequest(tabRequest.id + 1, intent.getStringExtra("tab"))
    }

    override fun onStop() {
        // a theme that was only being tried does not stay after leaving the app
        store.previewTheme = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

// ------------------------------------------------------------------ Design system

object Kx {
    val red = Color(0xFFE5484D)
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF22C55E)
}

@Composable
fun CheeseTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFF4F4F5), onPrimary = Color(0xFF0E0E11),
            primaryContainer = Color(0xFF26262E), onPrimaryContainer = Color(0xFFF4F4F5),
            secondaryContainer = Color(0xFF26262E), onSecondaryContainer = Color(0xFFF4F4F5),
            background = Color(0xFF0E0E11), onBackground = Color(0xFFF4F4F5),
            surface = Color(0xFF151519), onSurface = Color(0xFFF4F4F5),
            surfaceVariant = Color(0xFF1F1F26), onSurfaceVariant = Color(0xFF8D8D98),
            outline = Color(0xFF2C2C34), outlineVariant = Color(0xFF2C2C34)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF14151A), onPrimary = Color.White,
            primaryContainer = Color(0xFFE6E9F3), onPrimaryContainer = Color(0xFF14151A),
            secondaryContainer = Color(0xFFE6E9F3), onSecondaryContainer = Color(0xFF14151A),
            background = Color(0xFFF3F5FB), onBackground = Color(0xFF14151A),
            surface = Color.White, onSurface = Color(0xFF14151A),
            surfaceVariant = Color(0xFFECEEF5), onSurfaceVariant = Color(0xFF6B6F80),
            outline = Color(0xFFDADEEA), outlineVariant = Color(0xFFDADEEA)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Small letter-spaced monospace caption. */
@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, size: Int = 12) {
    Text(text, modifier = modifier, color = color, fontFamily = FontFamily.Monospace, fontSize = size.sp, letterSpacing = 1.6.sp)
}

@Composable
fun BigTitle(text: String) {
    Text(text, fontSize = 54.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp, lineHeight = 60.sp)
}

@Composable
fun Heading(text: String) {
    Text(text, fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 30.dp, bottom = 12.dp, start = 2.dp))
}

@Composable
fun KCard(modifier: Modifier = Modifier, radius: Int = 26, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(content = content)
    }
}

@Composable
fun KDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun Pill(label: String, strong: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .border(1.dp, if (strong) cs.onSurface else cs.outline, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Mono(label, color = if (strong) cs.onSurface else cs.onSurfaceVariant) }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) cs.primary else cs.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (selected) cs.onPrimary else cs.onSurfaceVariant)
    }
}

@Composable
fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** Pill-shaped switcher: the chosen option is a filled pill. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().border(1.dp, cs.outline, RoundedCornerShape(26.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (sel) cs.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (sel) cs.onPrimary else cs.onSurfaceVariant)
            }
        }
    }
}

/** Slider drawn like a ruler: tick marks with a red marker. */
@Composable
fun RulerSlider(
    label: String,
    valueText: String,
    fraction: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    val on = cs.onSurface
    val off = cs.onSurfaceVariant.copy(alpha = 0.45f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Mono(valueText, size = 14)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { o ->
                        change((o.x / size.width).coerceIn(0f, 1f))
                        finished()
                    })
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> change((o.x / size.width).coerceIn(0f, 1f)) },
                        onDragEnd = { finished() },
                        onDragCancel = { finished() }
                    ) { c, _ ->
                        c.consume()
                        change((c.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            val ticks = 41
            val px = fraction.coerceIn(0f, 1f) * size.width
            for (i in 0 until ticks) {
                val x = i * size.width / (ticks - 1)
                val len = when {
                    i % 10 == 0 -> size.height * 0.72f
                    i % 5 == 0 -> size.height * 0.52f
                    else -> size.height * 0.32f
                }
                val cy = size.height / 2f
                drawLine(
                    color = if (x <= px) on else off,
                    start = Offset(x, cy - len / 2f),
                    end = Offset(x, cy + len / 2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawLine(
                color = Kx.red,
                start = Offset(px, 2.dp.toPx()),
                end = Offset(px, size.height - 2.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun NavGlyph(kind: Int, tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        val w = size.width
        val stroke = Stroke(width = 2.2.dp.toPx())
        when (kind) {
            0 -> drawRoundRect(
                color = tint, topLeft = Offset(w * 0.08f, w * 0.26f), size = Size(w * 0.84f, w * 0.48f),
                cornerRadius = CornerRadius(w * 0.16f), style = stroke
            )
            1 -> {
                val p = Path()
                p.moveTo(w * 0.5f, w * 0.08f)
                p.lineTo(w * 0.92f, w * 0.5f)
                p.lineTo(w * 0.5f, w * 0.92f)
                p.lineTo(w * 0.08f, w * 0.5f)
                p.close()
                drawPath(p, color = tint, style = stroke)
            }
            2 -> {
                drawCircle(color = tint, radius = w * 0.38f, center = Offset(w / 2f, w / 2f), style = stroke)
                drawLine(color = tint, start = Offset(w / 2f, w * 0.3f), end = Offset(w / 2f, w * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(color = tint, radius = w * 0.38f, center = Offset(w / 2f, w / 2f), style = stroke)
                drawCircle(color = tint, radius = w * 0.1f, center = Offset(w / 2f, w / 2f))
            }
        }
    }
}

@Composable
fun KBottomNav(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(34.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outline),
        shadowElevation = 8.dp
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            labels.forEachIndexed { i, label ->
                val sel = i == selected
                val tint = if (sel) cs.onSurface else cs.onSurfaceVariant
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).clickable { onSelect(i) }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavGlyph(i, tint)
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 13.sp, color = tint, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

// ------------------------------------------------------------------ Root

@Composable
fun AppRoot(store: Store, tick: Int, request: TabRequest) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var focusType by remember { mutableIntStateOf(0) }
    LaunchedEffect(request.id) {
        when (request.tab) {
            "settings" -> tab = 3
        }
    }
    // The navigation bar must never sit on top of the keyboard: hide it while the keyboard is open.
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val labels = listOf("Type", "Themes", "Sound", "Settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!keyboardOpen) KBottomNav(labels, tab) { tab = it }
        }
    ) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad).imePadding().fillMaxSize()) {
            when (tab) {
                0 -> TypeScreen(store, tick, focusType)
                1 -> ThemesScreen(store) {
                    // "Try": go to the Type tab with the keyboard open so the theme can be felt right away
                    tab = 0
                    focusType++
                }
                2 -> SoundScreen(store)
                else -> SettingsScreen(store, tick) { tab = 1 }
            }
        }
    }
}

// ------------------------------------------------------------------ Type (test typing)

class Msg(val sent: Boolean, val text: String)

@Composable
fun StatusRow(done: Boolean, title: String, body: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).background(if (done) Kx.green else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (done) "\u2713" else "!", color = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            PrimaryButton(action) { onAction() }
        }
    }
}

@Composable
fun TypeScreen(store: Store, tick: Int, focusRequest: Int) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    val enabled = remember(tick) { imm.enabledInputMethodList.any { it.packageName == ctx.packageName } }
    val selected = remember(tick) {
        android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith(ctx.packageName + "/") == true
    }
    val themeId = remember(tick, focusRequest) { store.activeTheme }
    val themeTitle = KeyboardThemes.all.firstOrNull { it.id == themeId }?.title ?: ""

    val messages = remember {
        mutableStateListOf(
            Msg(false, "are we still on for tonight?"),
            Msg(true, "yes, booked for 8"),
            Msg(false, "ugh can we do 8:30? stuck at work"),
            Msg(true, "perfect, see you there")
        )
    }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }

    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            try {
                focus.requestFocus()
            } catch (e: Exception) {
                // field not ready yet; tap it manually
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(Brush.linearGradient(listOf(Color(0xFF4F8AF2), Color(0xFFB07CE0))), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("M", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mira", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text("online", fontSize = 15.sp, color = cs.onSurfaceVariant)
            }
            Pill(themeTitle.uppercase())
        }
        KDivider()
        if (!enabled || !selected) {
            KCard(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (!enabled) {
                    StatusRow(false, "Turn on Cheeseboard", "Enable it in your keyboard list first", "Enable") {
                        ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
                    }
                } else {
                    StatusRow(false, "Pick Cheeseboard", "Choose it as your active keyboard", "Switch") { imm.showInputMethodPicker() }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages.size) { i ->
                val m = messages[i]
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.sent) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = if (m.sent) cs.primary else cs.surfaceVariant
                    ) {
                        Text(
                            m.text,
                            fontSize = 20.sp,
                            color = if (m.sent) cs.onPrimary else cs.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).border(1.dp, cs.outline, CircleShape), contentAlignment = Alignment.Center) {
                Text("+", fontSize = 24.sp, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(28.dp),
                maxLines = 3,
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(46.dp).border(1.dp, cs.outline, CircleShape).clip(CircleShape).clickable {
                    val t = draft.trim()
                    if (t.isNotEmpty()) {
                        messages.add(Msg(true, t))
                        draft = ""
                    }
                },
                contentAlignment = Alignment.Center
            ) { Text("\u2191", fontSize = 22.sp, color = cs.onSurface) }
        }
    }
}

// ------------------------------------------------------------------ Themes

@Composable
fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
        Mono(label, size = 11)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ThemesScreen(store: Store, onTry: () -> Unit) {
    val ctx = LocalContext.current
    var worn by remember { mutableStateOf(store.keyboardTheme) }
    var trying by remember { mutableStateOf(store.previewTheme) }
    val total = KeyboardThemes.all.size

    fun tryTheme(id: String) {
        store.previewTheme = id
        trying = id
        if (store.sound) {
            VoiceEngine.prepare(ctx, store.soundConfig())
        }
        onTry()
    }

    ScreenColumn {
        Spacer(Modifier.height(28.dp))
        Mono("$total THEMES")
        Spacer(Modifier.height(10.dp))
        BigTitle("Themes")
        Spacer(Modifier.height(10.dp))
        Text(
            "Scroll the collection. Tap any keyboard to try it.",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))

        KeyboardThemes.all.forEachIndexed { idx, t ->
            KCard(Modifier.padding(bottom = 20.dp), radius = 34) {
                Column(Modifier.padding(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(26.dp))) {
                        AndroidView(
                            factory = { c -> ThemePreviewView(c, t.id) },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(Modifier.matchParentSize().clickable { tryTheme(t.id) })
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Mono("%02d / %02d".format(idx + 1, total), modifier = Modifier.weight(1f))
                        if (trying == t.id) Pill("TRYING", strong = true)
                        else if (worn == t.id) Pill("\u25CF WEARING", strong = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(t.title, fontSize = 42.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 6.dp))
                    Spacer(Modifier.height(6.dp))
                    Mono(t.tag, modifier = Modifier.padding(horizontal = 6.dp))
                    Spacer(Modifier.height(16.dp))
                    KDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Stat("VOICE", VoiceEngine.voiceTitle(KeyboardThemes.voiceFor(t.id)), Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(52.dp).background(MaterialTheme.colorScheme.outline))
                        Stat("HAPTIC", Haptics.title(KeyboardThemes.hapticFor(t.id)), Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(52.dp).background(MaterialTheme.colorScheme.outline))
                        Stat("SPACE", KeyboardThemes.mascotTitle(t.id), Modifier.weight(1f))
                    }
                    KDivider()
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { tryTheme(t.id) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
                        ) { Text("\u25B6  Try", fontSize = 18.sp) }
                        if (worn == t.id && trying == null) {
                            OutlinedButton(
                                onClick = {},
                                border = BorderStroke(2.dp, Kx.blue),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Wearing", fontSize = 18.sp) }
                        } else {
                            Button(
                                onClick = {
                                    store.keyboardTheme = t.id
                                    store.previewTheme = null
                                    worn = t.id
                                    trying = null
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Wear", fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Sound lab

@Composable
fun VoiceCard(voice: VoiceEngine.Voice, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val accent = Color(voice.color)
    Column(
        Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) cs.onSurface else cs.outline, RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text("\u271A", fontSize = 30.sp, color = accent) }
        Spacer(Modifier.height(16.dp))
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val n = 16
            for (i in 0 until n) {
                val h = (size.height * exp(-i * 0.32f)).coerceAtLeast(3.dp.toPx())
                val x = i * size.width / n + 2.dp.toPx()
                drawLine(
                    color = if (selected) accent else cs.onSurfaceVariant.copy(alpha = 0.4f),
                    start = Offset(x, size.height),
                    end = Offset(x, size.height - h),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(voice.title, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Mono(voice.type, size = 10, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SoundScreen(store: Store) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val vibrator = remember { ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    var sound by remember { mutableStateOf(store.sound) }
    var themeVoice by remember { mutableStateOf(store.themeVoice) }
    var voiceId by remember { mutableStateOf(store.voiceId) }
    var volume by remember { mutableFloatStateOf(store.soundVolume / 100f) }
    var pitch by remember { mutableFloatStateOf(store.pitchSt) }
    var tone by remember { mutableFloatStateOf(store.tone / 100f) }
    var strength by remember { mutableFloatStateOf(store.hapticStrength / 100f) }
    var travel by remember { mutableFloatStateOf(store.keyTravel) }
    var groove by remember { mutableIntStateOf(store.groove) }
    var hapticPattern by remember { mutableStateOf(store.hapticPattern) }
    var keySpecific by remember { mutableStateOf(store.keySpecific) }
    var vibrate by remember { mutableStateOf(store.vibrate) }

    val themeId = remember { store.activeTheme }
    val activeVoice = if (themeVoice) KeyboardThemes.voiceFor(themeId) else voiceId
    val voiceName = VoiceEngine.voiceTitle(activeVoice)
    val patternId = Haptics.resolve(hapticPattern, themeId)

    fun rebuild() = VoiceEngine.prepare(ctx, store.soundConfig())
    fun hear(category: String = "letter") {
        VoiceEngine.preview(ctx, store.soundConfig(), category)
        if (store.vibrate) Haptics.play(vibrator, Haptics.resolve(store.hapticPattern, store.activeTheme), store.hapticStrength, category != "letter")
    }
    LaunchedEffect(Unit) { rebuild() }

    ScreenColumn {
        Spacer(Modifier.height(28.dp))
        Mono("SOUND LAB \u00B7 TUNED PER THEME")
        Spacer(Modifier.height(10.dp))
        BigTitle("Sound")
        Spacer(Modifier.height(22.dp))

        KCard {
            SwitchRow("Keyboard sounds", "Play a sound when you press a key", sound) {
                sound = it; store.sound = it
                if (it) rebuild()
            }
        }
        Spacer(Modifier.height(14.dp))
        KCard {
            SwitchRow(
                "Theme voice",
                (if (themeVoice) "Following theme" else "Custom") + " \u00B7 " + voiceName,
                themeVoice
            ) {
                themeVoice = it; store.themeVoice = it
                rebuild()
            }
        }

        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Mono("VOICE", modifier = Modifier.weight(1f))
            Mono(voiceName.uppercase(), color = cs.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(VoiceEngine.allVoices()) { v ->
                VoiceCard(v, v.id == activeVoice) {
                    voiceId = v.id; store.voiceId = v.id
                    themeVoice = false; store.themeVoice = false
                    rebuild()
                    hear()
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        KCard {
            RulerSlider("Volume", "%03d %%".format((volume * 100).roundToInt()), volume, {
                volume = it; store.soundVolume = (it * 100).roundToInt()
            }) { hear() }
            RulerSlider("Pitch", "%+.1f st".format(pitch), (pitch + 12f) / 24f, {
                pitch = ((it * 24f - 12f) * 2f).roundToInt() / 2f; store.pitchSt = pitch
            }) { rebuild(); hear() }
            RulerSlider(
                "Tone",
                (if (tone < 0.34f) "Dark" else if (tone < 0.67f) "Warm" else "Bright") + " \u00B7 %03d".format((tone * 100).roundToInt()),
                tone,
                { tone = it; store.tone = (it * 100).roundToInt() }
            ) { rebuild(); hear() }
            RulerSlider("Haptics", "%03d %%".format((strength * 100).roundToInt()), strength, {
                strength = it; store.hapticStrength = (it * 100).roundToInt()
            }) {
                Haptics.play(vibrator, patternId, store.hapticStrength)
            }
            RulerSlider("Key travel", "%.2f mm".format(travel), (travel - 1f) / 3f, {
                travel = 1f + it * 3f; store.keyTravel = travel
            }) { rebuild(); hear("space") }
            Spacer(Modifier.height(6.dp))
        }

        Heading("Groove")
        Segmented(listOf("Off", "Subtle", "Musical"), groove) { groove = it; store.groove = it; hear() }
        Spacer(Modifier.height(10.dp))
        Text(
            when (groove) {
                0 -> "Every key plays the same note."
                1 -> "$voiceName walks a quiet chord change with a soft four-beat accent, so the same note doesn't repeat."
                else -> "$voiceName climbs a bright run of notes, so typing sounds like a little melody."
            },
            fontSize = 17.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Heading("Haptic pattern")
        val patternKeys = listOf("theme", "crisp", "soft", "heavy", "double")
        Segmented(listOf("Theme", "Crisp", "Soft", "Heavy", "Double"), patternKeys.indexOf(hapticPattern).coerceAtLeast(0)) {
            hapticPattern = patternKeys[it]; store.hapticPattern = patternKeys[it]
            Haptics.play(vibrator, Haptics.resolve(patternKeys[it], store.activeTheme), store.hapticStrength)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            Haptics.title(patternId) + " \u00B7 " + Haptics.description(patternId) +
                (if (hapticPattern == "theme") ". Matched to the $voiceName voice, with heavier hits on space and return." else ". Heavier hits on space and return."),
            fontSize = 17.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(20.dp))
        KCard {
            SwitchRow("Vibrate on keypress", "Turn all vibration on or off", vibrate) {
                vibrate = it; store.vibrate = it
            }
            KDivider()
            SwitchRow("Key-specific sounds", "Space, return and delete get their own voice", keySpecific) {
                keySpecific = it; store.keySpecific = it
                hear("space")
            }
        }

        Heading("Test strip \u00B7 tap to hear")
        val pal = remember(themeId) { Palette.resolve(ctx, 1, themeId) }
        val bg = pal.bgGradient
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    if (bg != null) Brush.verticalGradient(bg.map { Color(it) }) else Brush.verticalGradient(listOf(Color(pal.bg), Color(pal.bg)))
                )
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("c", "l", "i", "k", "\u232B").forEachIndexed { i, label ->
                    val accent = i == 4
                    Box(
                        Modifier
                            .weight(if (accent) 1.5f else 1f)
                            .height(74.dp)
                            .clip(RoundedCornerShape(maxOf(pal.radius, 10f).dp))
                            .background(if (accent) Color(pal.accent) else Color(pal.key).copy(alpha = 1f))
                            .clickable { hear(if (accent) "delete" else "letter") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 26.sp, color = if (accent) Color(pal.accentText) else Color(pal.keyText))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Voice, pitch and tone are made on your phone, so every change is instant. Recorded sample packs are at the end of the voice list.",
            fontSize = 14.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ------------------------------------------------------------------ Settings

@Composable
fun SettingsScreen(store: Store, tick: Int, onChangeTheme: () -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    var refresh by remember { mutableIntStateOf(0) }
    val enabled = remember(tick, refresh) { imm.enabledInputMethodList.any { it.packageName == ctx.packageName } }
    val selected = remember(tick, refresh) {
        android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith(ctx.packageName + "/") == true
    }
    val worn = remember(tick) { store.keyboardTheme }
    val wornInfo = KeyboardThemes.all.firstOrNull { it.id == worn }

    var theme by remember { mutableIntStateOf(store.themeMode) }
    var height by remember { mutableFloatStateOf(store.heightScale) }
    var keySize by remember { mutableFloatStateOf(store.keyScale) }
    var effects by remember { mutableStateOf(store.keyEffects) }
    var pressEffect by remember { mutableStateOf(store.pressEffect) }
    var keyPreviews by remember { mutableStateOf(store.keyPreviews) }
    var hintSymbols by remember { mutableStateOf(store.hintSymbols) }
    var numberRow by remember { mutableStateOf(store.numberRow) }
    var arrowKeys by remember { mutableStateOf(store.arrowKeys) }
    var emojiRow by remember { mutableStateOf(store.emojiRow) }
    var mascot by remember { mutableStateOf(store.spacebarMascot) }
    var gifProvider by remember { mutableStateOf(store.gifProvider) }
    var gifKey by remember { mutableStateOf(store.gifApiKey) }
    var predict by remember { mutableStateOf(store.predictEnabled) }
    var learn by remember { mutableStateOf(store.learnWords) }
    var history by remember { mutableStateOf(store.historyEnabled) }
    var capture by remember { mutableStateOf(store.captureExternal) }
    var limit by remember { mutableIntStateOf(store.historyLimit) }

    ScreenColumn {
        Spacer(Modifier.height(28.dp))
        Mono("CHEESEBOARD \u00B7 V1.8.0")
        Spacer(Modifier.height(10.dp))
        BigTitle("Settings")
        Spacer(Modifier.height(22.dp))

        KCard(radius = 32) {
            Column(Modifier.padding(14.dp)) {
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp))) {
                    androidx.compose.runtime.key(worn) {
                        AndroidView(factory = { c -> ThemePreviewView(c, worn) }, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Mono("YOUR KEYBOARD")
                        Text(wornInfo?.title ?: "Theme", fontSize = 30.sp, fontWeight = FontWeight.Light)
                    }
                    OutlinedButton(onClick = onChangeTheme, shape = RoundedCornerShape(50)) { Text("Change \u203A") }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        KCard {
            StatusRow(
                enabled, "Keyboard enabled",
                if (enabled) "Cheeseboard is in your keyboard list" else "Turn on Cheeseboard in the keyboard list",
                "Enable"
            ) { ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            KDivider()
            StatusRow(
                selected, "Active keyboard",
                if (selected) "You are typing with Cheeseboard" else "Pick Cheeseboard. Long-press the space bar to switch back",
                "Switch"
            ) { imm.showInputMethodPicker() }
            KDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { refresh++ }) { Text("Refresh status") }
            }
        }

        Heading("Typing")
        KCard {
            SwitchRow("Word suggestions", "Predicted words above the keys. Tap one to insert it", predict) {
                predict = it; store.predictEnabled = it
            }
            KDivider()
            SwitchRow("Learn my words", "Remember words you type often. Stays on this device, never used in password fields", learn) {
                learn = it; store.learnWords = it
            }
            KDivider()
            SwitchRow("Long-press symbols", "Small @ # & * printed on the letters. Hold a key to type it", hintSymbols) {
                hintSymbols = it; store.hintSymbols = it
            }
            KDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    store.clearLearned()
                    Toast.makeText(ctx, "Learned words cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear learned words") }
            }
        }

        Heading("Layout")
        KCard {
            SwitchRow("Number row", "1-0 always above the letters", numberRow) { numberRow = it; store.numberRow = it }
            KDivider()
            SwitchRow("Arrow keys", "Left and right cursor keys next to the space bar", arrowKeys) { arrowKeys = it; store.arrowKeys = it }
            KDivider()
            SwitchRow("Emoji row", "Your recent emoji in a row above the keys", emojiRow) { emojiRow = it; store.emojiRow = it }
            KDivider()
            SwitchRow("Space bar mascot", "A little character on the space bar. Every theme has its own and it hops when you type", mascot) {
                mascot = it; store.spacebarMascot = it
            }
        }
        Heading("Keyboard height")
        Segmented(listOf("Compact", "Regular", "Tall"), if (height < 0.92f) 0 else if (height > 1.1f) 2 else 1) {
            height = if (it == 0) 0.85f else if (it == 2) 1.2f else 1.0f
            store.heightScale = height
        }
        Spacer(Modifier.height(14.dp))
        KCard {
            RulerSlider("Key size", "%03d %%".format((keySize * 100).roundToInt()), (keySize - 0.8f) / 0.5f, {
                keySize = 0.8f + it * 0.5f; store.keyScale = keySize
            }) {}
        }

        Heading("Press effects")
        KCard {
            SwitchRow("Key previews", "Bubble with the letter above your finger", keyPreviews) { keyPreviews = it; store.keyPreviews = it }
            KDivider()
            SwitchRow("Press effects", "Glow, ripple, sparks or halo when a key is pressed, plus the RGB wave on RGB Gamer", effects) {
                effects = it; store.keyEffects = it
            }
            KDivider()
            ChipRow {
                listOf("auto" to "Theme", "glow" to "Glow", "ripple" to "Ripple").forEach { (id, label) ->
                    Chip(label, pressEffect == id) { pressEffect = id; store.pressEffect = id }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("sparks" to "Sparks", "halo" to "Halo").forEach { (id, label) ->
                    Chip(label, pressEffect == id) { pressEffect = id; store.pressEffect = id }
                }
            }
        }

        Heading("Light / dark")
        KCard {
            Text(
                "Only used by the Liquid Glass and Classic themes. The other themes keep their own colors.",
                fontSize = 14.sp, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)
            )
            ChipRow {
                listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                    Chip(label, theme == i) { theme = i; store.themeMode = i }
                }
            }
        }

        Heading("GIF search")
        KCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Search GIFs from the emoji panel (GIF button). It needs a free API key. Google closed Tenor's API in June 2026, " +
                        "so Cheeseboard uses KLIPY (free, get a key at klipy.com/developers) or GIPHY (key from developers.giphy.com). " +
                        "Paste the key below. This is the only feature that uses the internet, and only while you search GIFs.",
                    fontSize = 14.sp, color = cs.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("KLIPY (free)", gifProvider == "klipy") { gifProvider = "klipy"; store.gifProvider = "klipy" }
                    Chip("GIPHY", gifProvider == "giphy") { gifProvider = "giphy"; store.gifProvider = "giphy" }
                }
                OutlinedTextField(
                    value = gifKey,
                    onValueChange = { gifKey = it; store.gifApiKey = it.trim() },
                    label = { Text("API key") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Heading("Color Grid")
        KCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A floating numbered 12 x 3 grid (just the lines) for counting items on screen. Turn it on or off from the keyboard: " +
                        "tap the 4-square icon (top left), then Grid. Drag the gray bar to move it and the pink square to resize it. " +
                        "Off by default; needs \"Display over other apps\".",
                    fontSize = 14.sp, color = cs.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:" + ctx.packageName)
                            )
                        )
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow display over other apps") }
            }
        }

        Heading("Clipboard")
        KCard {
            SwitchRow("Keep clipboard history", "Remember calculator results and text you copy", history) { history = it; store.historyEnabled = it }
            KDivider()
            SwitchRow("Record text copied elsewhere", "Also add text copied in other apps while this keyboard is active", capture) {
                capture = it; store.captureExternal = it
            }
            KDivider()
            Text("History size", fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp, top = 16.dp))
            ChipRow {
                listOf(10, 25, 50).forEach { n -> Chip("$n", limit == n) { limit = n; store.historyLimit = n } }
            }
            KDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    store.clearHistory(false)
                    Toast.makeText(ctx, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear clipboard history") }
            }
        }

        Heading("About")
        KCard {
            Text(
                "Cheeseboard 1.8.0\n\n" +
                    "\u2022 Long-press a letter to type the small number or symbol in its corner.\n" +
                    "\u2022 Long-press the . key for ? ! , and other punctuation.\n" +
                    "\u2022 Long-press the space bar to switch to another keyboard.\n" +
                    "\u2022 Tap the 4-square icon (top left) for the calculator, Grid toggle, settings and hide button.\n" +
                    "\u2022 In the emoji panel, tap Search to find an emoji, or GIF to search GIFs.\n" +
                    "\u2022 Open the Themes tab, tap Try to type with a theme, then Wear it. Tune its sound in the Sound tab.\n" +
                    "\u2022 In the calculator, Copy inserts the result into the field and hides the keyboard.",
                fontSize = 15.sp,
                modifier = Modifier.padding(20.dp)
            )
            KDivider()
            Text(
                "Privacy: settings, learned words and clipboard history stay on your device. The app only uses the internet when you " +
                    "search GIFs (your search words go to the GIF service you chose). Text marked as sensitive is never recorded, " +
                    "and words are never learned in password, e-mail or URL fields.",
                fontSize = 14.sp, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}
