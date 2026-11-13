package com.chdv.haboox

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/* ---------------------------------------------------------------- prefs */

object Prefs {
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        if (sp == null) sp = c.applicationContext.getSharedPreferences("haboox", Context.MODE_PRIVATE)
        Store.app = c.applicationContext
    }

    private fun p(): SharedPreferences = sp!!

    fun b(k: String, d: Boolean): Boolean = p().getBoolean(k, d)
    fun s(k: String, d: String): String = p().getString(k, d) ?: d
    fun i(k: String, d: Int): Int = p().getInt(k, d)
    fun has(k: String): Boolean = p().contains(k)
    fun keys(): Set<String> = p().all.keys

    fun put(k: String, v: Any?) {
        val e = p().edit()
        when (v) {
            null -> e.remove(k)
            is Boolean -> e.putBoolean(k, v)
            is Int -> e.putInt(k, v)
            is Long -> e.putLong(k, v)
            else -> e.putString(k, v.toString())
        }
        e.apply()
    }

    fun list(k: String): MutableList<String> =
        s(k, "").split('\n').filter { it.isNotEmpty() }.toMutableList()

    fun putList(k: String, l: List<String>) = put(k, l.joinToString("\n"))
}

/* ---------------------------------------------------------------- palette */

/** pastel == "colorful" (pastel or vivid themes). */
class Pal(
    val bg: Int, val card: Int, val card2: Int, val text: Int, val sub: Int,
    val line: Int, val ink: Int, val onInk: Int, val tiles: IntArray, val pastel: Boolean,
    val hero: IntArray, val heroText: Int, val heroSub: Int,
    val dockBg: Int, val dockFg: Int, val night: Boolean = false, val vivid: Boolean = false
) {
    fun tile(i: Int): Int = tiles[Math.floorMod(i, tiles.size)]
}

object Look {
    val theme: String get() = Prefs.s("theme", "bw")
    val pastel: Boolean get() = theme !in setOf("bw", "contrast", "paper", "night")

    fun c(v: Long): Int = v.toInt()

    val themes: List<Pair<String, String>> = listOf(
        "bw" to "שחור‑לבן", "contrast" to "ניגודיות גבוהה", "paper" to "נייר",
        "vivid" to "חי וצבעוני", "pastel" to "פסטל", "ocean" to "אוקיינוס", "forest" to "יער",
        "sunset" to "שקיעה", "night" to "לילה (שחור)", "midnight" to "כהה צבעוני", "custom" to "ערכה משלי"
    )

    val swatches: List<Pair<String, Int>> = listOf(
        "סגול" to c(0xFF7C3AED), "כחול" to c(0xFF2563EB), "תכלת" to c(0xFF0EA5E9), "טורקיז" to c(0xFF14B8A6),
        "ירוק" to c(0xFF16A34A), "ליים" to c(0xFF84CC16), "צהוב" to c(0xFFEAB308), "כתום" to c(0xFFF97316),
        "אדום" to c(0xFFDC2626), "ורוד" to c(0xFFDB2777), "חום" to c(0xFF92400E), "אפור" to c(0xFF475569),
        "שחור" to c(0xFF111111)
    )

    fun mix(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * (1 - t) + Color.red(b) * t).toInt()
        val g = (Color.green(a) * (1 - t) + Color.green(b) * t).toInt()
        val bl = (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun lightColorful(bg: Long, card2: Long, text: Long, sub: Long, line: Long, tiles: IntArray, a: Int, b: Int): Pal = Pal(
        bg = c(bg), card = Color.WHITE, card2 = c(card2), text = c(text), sub = c(sub), line = c(line),
        ink = c(text), onInk = Color.WHITE, tiles = tiles, pastel = true,
        hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.15f),
        dockBg = c(text), dockFg = Color.WHITE, vivid = true
    )

    fun pal(): Pal = when (theme) {
        "pastel" -> Pal(
            bg = c(0xFFFBF8F3), card = c(0xFFFFFFFF), card2 = c(0xFFF1ECE4),
            text = c(0xFF1C1C1E), sub = c(0xFF6B6B70), line = c(0xFFE4DED4),
            ink = c(0xFF1C1C1E), onInk = Color.WHITE,
            tiles = intArrayOf(
                c(0xFFFFD9D6), c(0xFFD6E8FF), c(0xFFDDF2D8), c(0xFFFFF0C7),
                c(0xFFE8DDFF), c(0xFFFFE0F0), c(0xFFD5F3F0), c(0xFFFFE6CC)
            ), pastel = true,
            hero = intArrayOf(c(0xFFFFD9D6), c(0xFFD6E8FF)), heroText = c(0xFF1C1C1E), heroSub = c(0xFF5A5A60),
            dockBg = c(0xFFFFFFFF), dockFg = c(0xFF1C1C1E)
        )
        "vivid" -> lightColorful(0xFFF6F3EE, 0xFFEDE8E0, 0xFF16161A, 0xFF6A6A72, 0xFFE3DDD3, intArrayOf(
            c(0xFFFF8A7A), c(0xFF5EC8C0), c(0xFFFFC857), c(0xFF9B8CFF),
            c(0xFF6EB5FF), c(0xFF8BD17C), c(0xFFFF9FCB), c(0xFFFFA65C)
        ), c(0xFF4F46E5), c(0xFFC026D3))
        "ocean" -> lightColorful(0xFFF1F7FB, 0xFFE2EEF6, 0xFF0F2433, 0xFF557080, 0xFFD3E3EE, intArrayOf(
            c(0xFF8FD3FF), c(0xFF7FE0D0), c(0xFFB8C8FF), c(0xFFA5E8FF), c(0xFF9FD8C8), c(0xFFC7E3FF)
        ), c(0xFF0EA5E9), c(0xFF2563EB))
        "forest" -> lightColorful(0xFFF3F6EF, 0xFFE4EBDB, 0xFF1B2A17, 0xFF5F6F57, 0xFFD8E0CE, intArrayOf(
            c(0xFFB5E3A1), c(0xFFE8D98F), c(0xFF9CD3B4), c(0xFFD7E8A6), c(0xFFF2C79A), c(0xFFA8D8C0)
        ), c(0xFF16A34A), c(0xFF0D9488))
        "sunset" -> lightColorful(0xFFFFF6EF, 0xFFFBE7D8, 0xFF2B1A12, 0xFF7A5A4A, 0xFFF0DCCD, intArrayOf(
            c(0xFFFFB38A), c(0xFFFFD27A), c(0xFFFF9AA8), c(0xFFFFC2A0), c(0xFFF7A6D2), c(0xFFFFE08A)
        ), c(0xFFF97316), c(0xFFDB2777))
        "paper" -> Pal(
            bg = c(0xFFF5EFE3), card = c(0xFFFBF7EE), card2 = c(0xFFEDE4D3),
            text = c(0xFF3B2F22), sub = c(0xFF7D6B55), line = c(0xFFE0D5C1),
            ink = c(0xFF3B2F22), onInk = c(0xFFF5EFE3),
            tiles = intArrayOf(c(0xFFFBF7EE)), pastel = false,
            hero = intArrayOf(c(0xFF3B2F22)), heroText = c(0xFFF5EFE3), heroSub = c(0xFFCDBFA8),
            dockBg = c(0xFF3B2F22), dockFg = c(0xFFF5EFE3)
        )
        "contrast" -> Pal(
            bg = Color.WHITE, card = Color.WHITE, card2 = c(0xFFDDDDDD),
            text = Color.BLACK, sub = c(0xFF222222), line = Color.BLACK,
            ink = Color.BLACK, onInk = Color.WHITE,
            tiles = intArrayOf(Color.WHITE), pastel = false,
            hero = intArrayOf(Color.BLACK), heroText = Color.WHITE, heroSub = c(0xFFDDDDDD),
            dockBg = Color.BLACK, dockFg = Color.WHITE
        )
        "night" -> Pal(
            bg = Color.BLACK, card = c(0xFF1C1C1E), card2 = c(0xFF2C2C2E),
            text = Color.WHITE, sub = c(0xFFAAAAAA), line = c(0xFF3A3A3C),
            ink = Color.WHITE, onInk = Color.BLACK,
            tiles = intArrayOf(c(0xFF1C1C1E)), pastel = false,
            hero = intArrayOf(c(0xFF1C1C1E)), heroText = Color.WHITE, heroSub = c(0xFFAAAAAA),
            dockBg = c(0xFF1C1C1E), dockFg = Color.WHITE, night = true
        )
        "midnight" -> Pal(
            bg = c(0xFF0F1220), card = c(0xFF1A1F35), card2 = c(0xFF262C48),
            text = Color.WHITE, sub = c(0xFFA7B0D0), line = c(0xFF2C3352),
            ink = Color.WHITE, onInk = c(0xFF0F1220),
            tiles = intArrayOf(c(0xFF3B3F8F), c(0xFF0E6B6B), c(0xFF7A3B8F), c(0xFF8F4A2E), c(0xFF2E5C8F), c(0xFF2E7A45)),
            pastel = true,
            hero = intArrayOf(c(0xFF6366F1), c(0xFFEC4899)), heroText = Color.WHITE, heroSub = c(0xFFE0E0FF),
            dockBg = c(0xFF1A1F35), dockFg = Color.WHITE, night = true, vivid = true
        )
        "custom" -> custom()
        else -> Pal(
            bg = Color.WHITE, card = c(0xFFF2F2F2), card2 = c(0xFFE4E4E4),
            text = c(0xFF111111), sub = c(0xFF555555), line = c(0xFFCFCFCF),
            ink = c(0xFF111111), onInk = Color.WHITE,
            tiles = intArrayOf(c(0xFFF2F2F2)), pastel = false,
            hero = intArrayOf(c(0xFF111111)), heroText = Color.WHITE, heroSub = c(0xFFBDBDBD),
            dockBg = c(0xFF111111), dockFg = Color.WHITE
        )
    }

    private fun custom(): Pal {
        val a = Prefs.i("customA", c(0xFF7C3AED))
        val b = Prefs.i("customB", c(0xFF0EA5E9))
        val dark = Prefs.s("customBg", "light") == "dark"
        return if (dark) Pal(
            bg = mix(Color.BLACK, a, 0.08f), card = mix(Color.BLACK, a, 0.16f), card2 = mix(Color.BLACK, a, 0.26f),
            text = Color.WHITE, sub = mix(Color.WHITE, a, 0.3f), line = mix(Color.BLACK, a, 0.3f),
            ink = Color.WHITE, onInk = Color.BLACK,
            tiles = intArrayOf(mix(Color.BLACK, a, 0.45f), mix(Color.BLACK, b, 0.45f), mix(Color.BLACK, mix(a, b, 0.5f), 0.45f)),
            pastel = true, hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.2f),
            dockBg = mix(Color.BLACK, a, 0.16f), dockFg = Color.WHITE, night = true, vivid = true
        ) else Pal(
            bg = mix(Color.WHITE, a, 0.05f), card = Color.WHITE, card2 = mix(Color.WHITE, a, 0.14f),
            text = c(0xFF16161A), sub = c(0xFF62626C), line = mix(Color.WHITE, a, 0.18f),
            ink = c(0xFF16161A), onInk = Color.WHITE,
            tiles = intArrayOf(mix(Color.WHITE, a, 0.45f), mix(Color.WHITE, b, 0.45f), mix(Color.WHITE, mix(a, b, 0.5f), 0.45f)),
            pastel = true, hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.15f),
            dockBg = mix(c(0xFF16161A), a, 0.25f), dockFg = Color.WHITE, vivid = true
        )
    }

    fun heroBg(p: Pal, radius: Float): Drawable {
        val base: GradientDrawable =
            if (p.hero.size > 1) GradientDrawable(GradientDrawable.Orientation.TR_BL, p.hero).apply { cornerRadius = radius }
            else roundBg(p.hero[0], radius)
        if (!Prefs.b("heroDeco", true)) return base
        val light = Color.red(p.heroText) > 128
        return android.graphics.drawable.LayerDrawable(arrayOf<Drawable>(base, Deco(if (light) 0x22FFFFFF else 0x14000000)))
    }
    fun clockSize(): Float = when (Prefs.s("clockSize", "large")) {
        "normal" -> 50f
        "huge" -> 84f
        else -> 66f
    }

    val heavy: Typeface by lazy { Typeface.create("sans-serif-black", Typeface.NORMAL) }

    fun scale(): Float = when (Prefs.s("fontSize", "normal")) {
        "large" -> 1.15f
        "xlarge" -> 1.3f
        else -> 1f
    }

    fun bold(): Boolean = Prefs.b("boldText", false)
    fun grayIcons(): Boolean = Prefs.b("grayIcons", !pastel)
    fun sig(): String = Prefs.s("theme", "bw") + Prefs.s("fontSize", "normal") + bold() + grayIcons() +
        Prefs.s("clockSize", "large") + Prefs.b("iconTiles", true) + Prefs.b("heroDeco", true) +
        Prefs.i("customA", 0) + Prefs.i("customB", 0) + Prefs.s("customBg", "light")
}

/* ---------------------------------------------------------------- helpers */

fun Context.dp(v: Number): Int = (v.toFloat() * resources.displayMetrics.density + 0.5f).toInt()

fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

fun Context.go(i: Intent, fallback: Intent? = null): Boolean {
    if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(i); true
    } catch (e: Exception) {
        if (fallback != null) {
            try {
                if (this !is Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallback); true
            } catch (e2: Exception) {
                toast("הפעולה אינה זמינה במכשיר זה"); false
            }
        } else {
            toast("הפעולה אינה זמינה במכשיר זה"); false
        }
    }
}

fun roundBg(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

fun Context.pressBg(color: Int, pressed: Int, radiusDp: Int, stroke: Int = 0, strokeColor: Int = 0): Drawable {
    val r = dp(radiusDp).toFloat()
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), roundBg(pressed, r, stroke, strokeColor))
        addState(intArrayOf(), roundBg(color, r, stroke, strokeColor))
    }
}

fun Context.label(s: CharSequence, size: Float, color: Int, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size * Look.scale())
        setTextColor(color)
        typeface = if (bold || Look.bold()) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textDirection = View.TEXT_DIRECTION_RTL
    }

fun TextView.oneLine(): TextView {
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    return this
}

fun Context.vbox(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
fun Context.hbox(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h, weight)

fun LinearLayout.LayoutParams.m(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
    setMargins(l, t, r, b); return this
}

fun Context.glyphView(kind: String, color: Int, sizeDp: Int): ImageView = ImageView(this).apply {
    setImageDrawable(Glyph(kind, color))
    layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
}

class SquareFrame(c: Context) : FrameLayout(c) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}

/* ---------------------------------------------------------------- glyphs */

class Glyph(private val kind: String, private val color: Int, private val strokeW: Float = 1.9f) : Drawable() {
    var fill: Float = 1f
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    companion object {
        private val cache = HashMap<String, Path>()
        private val tok = Regex("[MLHVQCZ]|-?[0-9]*\\.?[0-9]+")

        fun path(d: String): Path {
            cache[d]?.let { return it }
            val p = Path()
            val t = tok.findAll(d).map { it.value }.toList()
            var i = 0
            var cmd = 'M'
            var x = 0f
            var y = 0f
            fun n(): Float = t[i++].toFloat()
            while (i < t.size) {
                val s = t[i]
                if (s[0].isLetter()) {
                    cmd = s[0]; i++
                    if (cmd == 'Z') { p.close(); continue }
                }
                when (cmd) {
                    'M' -> { x = n(); y = n(); p.moveTo(x, y); cmd = 'L' }
                    'L' -> { x = n(); y = n(); p.lineTo(x, y) }
                    'H' -> { x = n(); p.lineTo(x, y) }
                    'V' -> { y = n(); p.lineTo(x, y) }
                    'Q' -> { val a = n(); val b = n(); x = n(); y = n(); p.quadTo(a, b, x, y) }
                    'C' -> {
                        val a = n(); val b = n(); val c = n(); val e = n(); x = n(); y = n()
                        p.cubicTo(a, b, c, e, x, y)
                    }
                    else -> i++
                }
            }
            cache[d] = p
            return p
        }
    }

    private lateinit var cv: Canvas
    private fun st(d: String) = cv.drawPath(path(d), sp)
    private fun fl(d: String) = cv.drawPath(path(d), fp)
    private fun circ(x: Float, y: Float, r: Float, fill: Boolean = false) = cv.drawCircle(x, y, r, if (fill) fp else sp)
    private fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float, fill: Boolean = false) =
        cv.drawRoundRect(RectF(l, t, r, b), rad, rad, if (fill) fp else sp)

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()) / 24f
        sp.color = color; fp.color = color; sp.strokeWidth = strokeW
        cv = canvas
        canvas.save()
        canvas.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f)
        canvas.scale(s, s)
        when (kind) {
            "wifi" -> {
                for (r in floatArrayOf(4f, 8f, 12f)) {
                    canvas.drawArc(RectF(12f - r, 19.5f - r, 12f + r, 19.5f + r), 225f, 90f, false, sp)
                }
                circ(12f, 19.5f, 1.7f, true)
            }
            "folder" -> {
                st("M3 6.5 Q3 5 4.5 5 H9 L11 7 H19.5 Q21 7 21 8.5 V17.5 Q21 19 19.5 19 H4.5 Q3 19 3 17.5 Z")
                st("M3 9.5 H21")
            }
            "book" -> {
                st("M12 6.5 C10 5 6.5 4.8 3 5.5 V18.5 C6.5 17.8 10 18 12 19.5")
                st("M12 6.5 C14 5 17.5 4.8 21 5.5 V18.5 C17.5 17.8 14 18 12 19.5")
                st("M12 6.5 V19.5")
            }
            "gear" -> {
                circ(12f, 12f, 3f)
                circ(12f, 12f, 6.8f)
                val old = sp.strokeWidth
                sp.strokeWidth = 2.6f
                for (i in 0 until 8) {
                    canvas.save(); canvas.rotate(45f * i, 12f, 12f)
                    canvas.drawLine(12f, 2.6f, 12f, 5.0f, sp)
                    canvas.restore()
                }
                sp.strokeWidth = old
            }
            "bt" -> st("M7 7.5 L17 16.5 L12 21 V3 L17 7.5 L7 16.5")
            "battery" -> {
                rr(2.5f, 7.5f, 19.5f, 16.5f, 2f)
                rr(20.4f, 10.3f, 22f, 13.7f, 0.8f, true)
                val w = 13f * fill.coerceIn(0.05f, 1f)
                rr(4.5f, 9.5f, 4.5f + w, 14.5f, 1f, true)
            }
            "sun" -> {
                circ(12f, 12f, 3.6f)
                for (i in 0 until 8) {
                    canvas.save(); canvas.rotate(45f * i, 12f, 12f)
                    canvas.drawLine(12f, 2.8f, 12f, 5.2f, sp)
                    canvas.restore()
                }
            }
            "back" -> { st("M4 12 H19.5"); st("M13.5 6 L19.5 12 L13.5 18") }
            "chev_l" -> st("M15 5 L8 12 L15 19")
            "chev_r" -> st("M9 5 L16 12 L9 19")
            "chev_down" -> st("M5 9 L12 16 L19 9")
            "windows" -> {
                rr(3f, 8f, 15f, 20f, 2f)
                st("M7.5 8 V5.5 Q7.5 4 9 4 H19.5 Q21 4 21 5.5 V14.5 Q21 16 19.5 16 H15")
            }
            "refresh" -> {
                canvas.drawArc(RectF(4.5f, 4.5f, 19.5f, 19.5f), 320f, 300f, false, sp)
                st("M18.2 3.6 L18.3 8 L13.9 8.1")
            }
            "home" -> {
                st("M3.5 11 L12 4 L20.5 11"); st("M6 9.5 V20 H18 V9.5"); st("M10 20 V14.5 H14 V20")
            }
            "sliders" -> {
                st("M4 7 H20"); st("M4 12 H20"); st("M4 17 H20")
                circ(15f, 7f, 2.2f, true); circ(9f, 12f, 2.2f, true); circ(13.5f, 17f, 2.2f, true)
            }
            "calendar" -> {
                rr(3.5f, 5f, 20.5f, 20.5f, 2.5f)
                st("M3.5 9.5 H20.5"); st("M8 3 V7"); st("M16 3 V7")
                for (yy in floatArrayOf(13f, 16.8f)) for (xx in floatArrayOf(7.5f, 12f, 16.5f)) circ(xx, yy, 1.05f, true)
            }
            "plus" -> { st("M12 5 V19"); st("M5 12 H19") }
            "file" -> { st("M6 3 H14 L19 8 V21 H6 Z"); st("M14 3 V8 H19") }
            "image" -> {
                rr(3f, 5f, 21f, 19f, 2.5f); circ(8.5f, 9.5f, 1.6f, true)
                st("M3.5 17 L9 12 L13 15.5 L16 13 L20.5 16.5")
            }
            "music" -> { st("M9 17 V5.5 L19 3.5 V15"); circ(6.8f, 17f, 2.2f, true); circ(16.8f, 15f, 2.2f, true) }
            "video" -> { rr(3f, 5f, 21f, 19f, 2.5f); fl("M10 9 L15.5 12 L10 15 Z") }
            "archive" -> {
                rr(4f, 3f, 20f, 21f, 2.5f)
                st("M12 3 V5"); st("M12 7 V9"); st("M12 11 V13"); rr(10.5f, 14f, 13.5f, 18f, 1f)
            }
            "apk" -> {
                rr(5f, 9f, 19f, 20f, 3f); st("M8 9 Q8 4.5 12 4.5 Q16 4.5 16 9"); st("M7.5 4 L9 6"); st("M16.5 4 L15 6")
            }
            "sort" -> { st("M4 7 H20"); st("M7 12 H17"); st("M10 17 H14") }
            "dots" -> { circ(6f, 12f, 1.7f, true); circ(12f, 12f, 1.7f, true); circ(18f, 12f, 1.7f, true) }
            "paste" -> { rr(5f, 5f, 19f, 21f, 2.5f); rr(9f, 3f, 15f, 7f, 1.2f) }
            "search" -> { circ(10.5f, 10.5f, 6f); st("M15 15 L20.5 20.5") }
            "up" -> { st("M12 19.5 V5"); st("M6 11 L12 5 L18 11") }
            "check" -> st("M5 12.5 L10 17.5 L19.5 7")
            "close" -> { st("M6.5 6.5 L17.5 17.5"); st("M17.5 6.5 L6.5 17.5") }
            "trash" -> { st("M4 7 H20"); st("M9 7 V4.5 H15 V7"); st("M6 7 L7 20.5 H17 L18 7") }
            "edit" -> st("M4 20 L5 15.5 L15.5 5 L19 8.5 L8.5 19 Z")
            "copy" -> {
                rr(8f, 8f, 20f, 20f, 2.5f)
                st("M16 8 V5.5 Q16 4 14.5 4 H5.5 Q4 4 4 5.5 V14.5 Q4 16 5.5 16 H8")
            }
            "cut" -> { circ(7f, 17.5f, 2.6f); circ(17f, 17.5f, 2.6f); st("M8.8 15.6 L17 4"); st("M15.2 15.6 L7 4") }
            "info" -> { circ(12f, 12f, 9f); st("M12 11 V17"); circ(12f, 7.6f, 1.2f, true) }
            "screen" -> { rr(6f, 2.5f, 18f, 21.5f, 2.8f); st("M10 18.5 H14") }
            "hide" -> {
                st("M2.5 12 Q12 3 21.5 12 Q12 21 2.5 12 Z"); circ(12f, 12f, 3f); st("M4 4 L20 20")
            }
            "bell" -> {
                st("M6 17 V11 Q6 5 12 5 Q18 5 18 11 V17 Z"); st("M4.5 17 H19.5"); st("M10 20 Q12 21.5 14 20")
            }
            "grid" -> {
                rr(4f, 4f, 10.5f, 10.5f, 2f); rr(13.5f, 4f, 20f, 10.5f, 2f)
                rr(4f, 13.5f, 10.5f, 20f, 2f); rr(13.5f, 13.5f, 20f, 20f, 2f)
            }
            "text" -> { st("M5 19 L10 5 L15 19"); st("M7 14 H13"); st("M16.5 19 V13"); circ(18.5f, 16.8f, 2.2f) }
            "palette" -> {
                st("M12 3 Q21 3 21 11 Q21 15 17 15 H15 Q13 15 13.8 17 Q15 21 12 21 Q3 21 3 12 Q3 3 12 3 Z")
                circ(8f, 10f, 1.3f, true); circ(12f, 7f, 1.3f, true); circ(16.2f, 9.5f, 1.3f, true)
            }
            "note" -> { st("M5 3.5 H15 L19 7.5 V20.5 H5 Z"); st("M8 10 H16"); st("M8 13.5 H16"); st("M8 17 H13") }
            "sd" -> {
                st("M8 3 H18 Q19.5 3 19.5 4.5 V19.5 Q19.5 21 18 21 H6 Q4.5 21 4.5 19.5 V7 Z")
                st("M9.5 6 V9"); st("M12.5 6 V9"); st("M15.5 6 V9")
            }
            "widget" -> {
                rr(3.5f, 3.5f, 11f, 11f, 2f); rr(13f, 3.5f, 20.5f, 11f, 2f, true)
                rr(3.5f, 13f, 20.5f, 20.5f, 2f)
            }
            "star" ->st("M12 3.5 L14.6 9 L20.5 9.6 L16 13.6 L17.3 19.5 L12 16.5 L6.7 19.5 L8 13.6 L3.5 9.6 L9.4 9 Z")
            else -> circ(12f, 12f, 8f)
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        sp.alpha = alpha; fp.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        sp.colorFilter = colorFilter; fp.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = 72
    override fun getIntrinsicHeight(): Int = 72
}

/** Decorative translucent circles for the hero card. */
class Deco(private val color: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    override fun draw(canvas: Canvas) {
        val b = bounds
        p.color = color
        val w = b.width().toFloat(); val h = b.height().toFloat()
        canvas.save()
        canvas.clipRect(b)
        canvas.drawCircle(b.left + w * 0.1f, b.top + h * 0.05f, h * 0.75f, p)
        canvas.drawCircle(b.left + w * 0.34f, b.top + h * 1.05f, h * 0.42f, p)
        canvas.drawCircle(b.left + w * 0.92f, b.top + h * 0.18f, h * 0.12f, p)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { p.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/* ---------------------------------------------------------------- base activity */

open class BaseActivity : AppCompatActivity() {
    lateinit var pal: Pal

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.init(this)
        pal = Look.pal()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = pal.bg
        window.navigationBarColor = pal.bg
        window.setBackgroundDrawable(ColorDrawable(pal.bg))
        window.decorView.systemUiVisibility = if (pal.night) 0 else
            (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    fun iconButton(kind: String, onClick: () -> Unit): View = FrameLayout(this).apply {
        val g = ImageView(context).apply { setImageDrawable(Glyph(kind, pal.ink, 2.1f)) }
        addView(g, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        background = pressBg(Color.TRANSPARENT, pal.card2, 22)
        layoutParams = lp(dp(46), dp(46))
        setOnClickListener { onClick() }
    }

    /** iOS style screen: back link, large title, body. Returns the actions bar for later edits. */
    fun screen(title: String, body: View, actions: List<View> = emptyList(), onBack: (() -> Unit)? = null): LinearLayout {
        val root = vbox().apply { setBackgroundColor(pal.bg) }
        val bar = hbox().apply { setPadding(dp(8), dp(6), dp(8), 0) }
        val back = hbox().apply {
            addView(glyphView("chev_r", pal.ink, 22))
            addView(label("חזרה", 17f, pal.ink))
            setPadding(dp(6), dp(8), dp(12), dp(8))
            background = pressBg(Color.TRANSPARENT, pal.card2, 18)
            setOnClickListener { if (onBack != null) onBack() else finish() }
        }
        bar.addView(back, lp(WRAP, WRAP))
        bar.addView(View(this), lp(0, 1, 1f))
        val act = hbox()
        actions.forEach { act.addView(it) }
        bar.addView(act, lp(WRAP, WRAP))
        root.addView(bar, lp(MATCH, WRAP))
        val t = label(title, 30f, pal.text, true).oneLine()
        t.setPadding(dp(20), dp(2), dp(20), dp(10))
        t.tag = "title"
        root.addView(t, lp(MATCH, WRAP))
        root.addView(body, lp(MATCH, 0, 1f))
        setContentView(root)
        return act
    }

    fun setScreenTitle(s: String) {
        (window.decorView.findViewWithTag<TextView>("title"))?.text = s
    }

    fun hasFiles(): Boolean = Store.hasAccess(this)
    fun askFiles() = Store.askAccess(this)

    fun permissionCard(text: String): View {
        val c = vbox().apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundBg(pal.card, dp(18).toFloat())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        c.addView(glyphView("folder", pal.ink, 40))
        c.addView(label(text, 16f, pal.text).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(14)) })
        c.addView(pillButton("אישור גישה לקבצים") { askFiles() })
        return c
    }

    fun pillButton(text: String, filled: Boolean = true, onClick: () -> Unit): TextView =
        label(text, 16f, if (filled) pal.onInk else pal.ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(11), dp(22), dp(11))
            background = if (filled) pressBg(pal.ink, 0xFF444444.toInt(), 22)
            else pressBg(Color.TRANSPARENT, pal.card2, 22, dp(2), pal.ink)
            setOnClickListener { onClick() }
        }
}

/* ---------------------------------------------------------------- sheets (iOS action sheets) */

object Sheet {
    private fun make(ctx: Context, content: View, bottom: Boolean = true): Dialog {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(content)
        d.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(MATCH, WRAP)
            w.setGravity(if (bottom) Gravity.BOTTOM else Gravity.CENTER)
            w.setWindowAnimations(0)
            w.setDimAmount(0.3f)
            w.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        return d
    }

    private fun shell(ctx: Context): LinearLayout = ctx.vbox().apply {
        setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(14))
    }

    private fun card(ctx: Context, pal: Pal): LinearLayout = ctx.vbox().apply {
        background = roundBg(pal.card, ctx.dp(18).toFloat(), ctx.dp(1), pal.line)
    }

    private fun divider(ctx: Context, pal: Pal): View = View(ctx).apply {
        setBackgroundColor(pal.line)
        layoutParams = lp(MATCH, maxOf(1, ctx.dp(0.7f)))
    }

    private fun row(ctx: Context, pal: Pal, text: String, red: Boolean, bold: Boolean, onClick: () -> Unit): View =
        ctx.label(text, 18f, pal.ink, bold || red).apply {
            gravity = Gravity.CENTER
            setPadding(ctx.dp(16), ctx.dp(15), ctx.dp(16), ctx.dp(15))
            background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
            if (red) setTextColor(if (pal.pastel) 0xFFC0392B.toInt() else pal.ink)
            setOnClickListener { onClick() }
        }

    private fun limit(ctx: Context, inner: View, count: Int): View {
        val sv = ScrollView(ctx)
        sv.addView(inner)
        val h = (ctx.resources.displayMetrics.heightPixels * 0.62f).toInt()
        sv.layoutParams = lp(MATCH, if (count > 8) h else WRAP)
        return sv
    }

    class Item(val text: String, val red: Boolean = false, val action: () -> Unit)

    fun actions(ctx: Context, title: String?, items: List<Item>) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        if (title != null) {
            c.addView(ctx.label(title, 14f, pal.sub, true).apply {
                gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
            })
            c.addView(divider(ctx, pal))
        }
        val list = ctx.vbox()
        items.forEachIndexed { i, item ->
            if (i > 0) list.addView(divider(ctx, pal))
            list.addView(row(ctx, pal, item.text, item.red, false) { d.dismiss(); item.action() })
        }
        c.addView(limit(ctx, list, items.size))
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun choice(ctx: Context, title: String, options: List<Pair<String, String>>, current: String, onPick: (String) -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 14f, pal.sub, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        })
        c.addView(divider(ctx, pal))
        val list = ctx.vbox()
        options.forEachIndexed { i, (value, text) ->
            if (i > 0) list.addView(divider(ctx, pal))
            val r = ctx.hbox().apply {
                setPadding(ctx.dp(18), ctx.dp(14), ctx.dp(18), ctx.dp(14))
                background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
                addView(ctx.label(text, 17f, pal.ink, value == current), lp(0, WRAP, 1f))
                if (value == current) addView(ctx.glyphView("check", pal.ink, 22))
                setOnClickListener { d.dismiss(); onPick(value) }
            }
            list.addView(r)
        }
        c.addView(limit(ctx, list, options.size))
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun confirm(ctx: Context, title: String, msg: String, ok: String, onOk: () -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(4))
        })
        c.addView(ctx.label(msg, 15f, pal.sub).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(4), ctx.dp(18), ctx.dp(14))
        })
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, ok, true, true) { d.dismiss(); onOk() })
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun info(ctx: Context, title: String, msg: String) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(4))
        })
        c.addView(ctx.label(msg, 15f, pal.sub).apply {
            gravity = Gravity.START; setPadding(ctx.dp(18), ctx.dp(6), ctx.dp(18), ctx.dp(14))
        })
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, "סגירה", false, true) { d.dismiss() })
        sh.addView(c, lp(MATCH, WRAP))
        d = make(ctx, sh)
        d.show()
    }

    /** Multi-select with order kept by selection. */
    fun multi(ctx: Context, title: String, options: List<Pair<String, String>>, selected: List<String>, max: Int, onDone: (List<String>) -> Unit) {
        val pal = Look.pal()
        val sel = selected.filter { s -> options.any { it.first == s } }.toMutableList()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 14f, pal.sub, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(4))
        })
        val hint = ctx.label("", 12.5f, pal.sub).apply { gravity = Gravity.CENTER; setPadding(ctx.dp(16), 0, ctx.dp(16), ctx.dp(10)) }
        c.addView(hint)
        c.addView(divider(ctx, pal))
        val list = ctx.vbox()
        val marks = ArrayList<TextView>()
        fun refresh() {
            hint.text = "נבחרו ${sel.size} מתוך עד $max · הסדר לפי סדר הבחירה"
            options.forEachIndexed { i, (v, _) ->
                val n = sel.indexOf(v)
                marks[i].text = if (n >= 0) (n + 1).toString() else ""
                marks[i].background = if (n >= 0) roundBg(pal.ink, ctx.dp(13).toFloat())
                else roundBg(Color.TRANSPARENT, ctx.dp(13).toFloat(), ctx.dp(1), pal.sub)
            }
        }
        options.forEachIndexed { i, (value, text) ->
            if (i > 0) list.addView(divider(ctx, pal))
            val mark = ctx.label("", 13f, pal.onInk, true).apply { gravity = Gravity.CENTER }
            marks.add(mark)
            val r = ctx.hbox().apply {
                setPadding(ctx.dp(18), ctx.dp(12), ctx.dp(18), ctx.dp(12))
                background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
                addView(ctx.label(text, 17f, pal.ink), lp(0, WRAP, 1f))
                addView(mark, lp(ctx.dp(26), ctx.dp(26)))
                setOnClickListener {
                    if (value in sel) sel.remove(value)
                    else if (sel.size < max) sel.add(value)
                    else ctx.toast("אפשר לבחור עד $max")
                    refresh()
                }
            }
            list.addView(r)
        }
        c.addView(limit(ctx, list, options.size))
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, "שמירה", false, true) { d.dismiss(); onDone(sel) })
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        refresh()
        d = make(ctx, sh)
        d.show()
    }

    fun input(ctx: Context, title: String, initial: String, hint: String, multiline: Boolean = false, onOk: (String) -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(10))
        })
        val et = EditText(ctx).apply {
            setText(initial)
            setHint(hint)
            if (multiline) {
                setSingleLine(false); minLines = 3; maxLines = 8; gravity = Gravity.TOP or Gravity.START
            } else setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(pal.text)
            setHintTextColor(pal.sub)
            textDirection = View.TEXT_DIRECTION_RTL
            background = roundBg(pal.bg, ctx.dp(12).toFloat(), ctx.dp(1), pal.line)
            setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
            setSelectAllOnFocus(true)
        }
        c.addView(et, lp(MATCH, WRAP).m(ctx.dp(16), 0, ctx.dp(16), ctx.dp(16)))
        c.addView(divider(ctx, pal))
        val btns = ctx.hbox()
        btns.addView(row(ctx, pal, "ביטול", false, false) { d.dismiss() }, lp(0, WRAP, 1f))
        btns.addView(View(ctx).apply { setBackgroundColor(pal.line) }, lp(maxOf(1, ctx.dp(0.7f)), MATCH))
        btns.addView(row(ctx, pal, "שמירה", false, true) {
            d.dismiss(); onOk(et.text.toString().trim())
        }, lp(0, WRAP, 1f))
        c.addView(btns)
        sh.addView(c, lp(MATCH, WRAP))
        d = make(ctx, sh, bottom = false)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        et.requestFocus()
        et.postDelayed({
            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
    }
}

/* ---------------------------------------------------------------- misc */

fun isAtLeastR(): Boolean = Build.VERSION.SDK_INT >= 30

fun ignore(e: Throwable?) {}

fun safeOpenTry(ctx: Context, i: Intent): Boolean = try {
    ctx.startActivity(i); true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
