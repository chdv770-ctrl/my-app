package com.chdv.haboox

import android.accessibilityservice.AccessibilityService
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Home screen made of "windows" (pages). Each window holds blocks: hero (clock), books, apps,
 * folders and gadgets. Apps that don't fit spill over into automatic extra windows.
 */
class HomeActivity : BaseActivity() {

    companion object {
        private const val HOST_ID = 4711
        private const val REQ_BIND = 41
        private const val REQ_CONF = 42
    }

    private class Ref(val user: Int, val appsFrom: Int)

    private lateinit var root: FrameLayout
    private lateinit var mainCol: LinearLayout
    private lateinit var pageHost: FrameLayout
    private lateinit var dots: LinearLayout
    private lateinit var dockBox: FrameLayout
    private lateinit var banner: LinearLayout
    private lateinit var statusBat: TextView
    private lateinit var statusBatIcon: ImageView
    private lateinit var statusWifi: ImageView
    private var panel: View? = null

    // hero references (only when the hero is on the visible window)
    private var clockTv: TextView? = null
    private var greetTv: TextView? = null
    private var hebTv: TextView? = null
    private var gregTv: TextView? = null
    private var eventTv: TextView? = null
    private val bigClocks = ArrayList<TextView>()

    private lateinit var host: AppWidgetHost
    private lateinit var awm: AppWidgetManager
    private val widgetViews = HashMap<Int, AppWidgetHostView>()
    private var pendingWidget = -1
    private var pendingPage = 0

    private var apps: List<AppEntry> = emptyList()
    private var userPages: MutableList<MutableList<String>> = mutableListOf()
    private var refs: List<Ref> = emptyList()
    private var cur = 0
    private val baseShown = HashMap<Int, Int>()
    private var fullPer = 0
    private var sig = ""
    private var lastBookCount = -1

    private var downX = 0f
    private var downY = 0f
    private var downT = 0L

    private val tick = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = updateClock()
    }
    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = reloadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sig = Look.sig()
        awm = AppWidgetManager.getInstance(this)
        host = AppWidgetHost(applicationContext, HOST_ID)
        userPages = Pages.load()
        build()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panel != null) hidePanel() else if (cur != 0) { cur = 0; renderPage() }
            }
        })
        val pf = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val sdf = IntentFilter().apply {
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
            addAction(Intent.ACTION_USER_UNLOCKED)
            addAction(Intent.ACTION_MEDIA_MOUNTED)
        }
        try { registerReceiver(pkgReceiver, pf) } catch (e: Exception) { ignore(e) }
        try { registerReceiver(pkgReceiver, sdf) } catch (e: Exception) { ignore(e) }
        handleExtras(intent)
    }

    override fun onDestroy() {
        try { unregisterReceiver(pkgReceiver) } catch (e: Exception) { ignore(e) }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        try { host.startListening() } catch (e: Exception) { ignore(e) }
    }

    override fun onStop() {
        super.onStop()
        try { host.stopListening() } catch (e: Exception) { ignore(e) }
    }

    override fun onResume() {
        super.onResume()
        if (Look.sig() != sig) { recreate(); return }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(tick, f)
        userPages = Pages.load()
        renderBanner()
        renderDock()
        renderPage()
        reloadApps()
        scanBooks()
        if (Prefs.b("autoUpdate", true)) Updater.dailyCheck(this)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(tick) } catch (e: Exception) { ignore(e) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        hidePanel()
        if (cur != 0) { cur = 0; renderPage() }
        handleExtras(intent)
    }

    private fun handleExtras(i: Intent?) {
        if (i != null && i.getBooleanExtra("gadgets", false)) {
            i.removeExtra("gadgets")
            root.post { addBlockMenu(curUser()) }
        }
    }

    private fun curUser(): Int = refs.getOrNull(cur)?.user ?: 0

    private fun savePages() = Pages.save(userPages)

    /* ------------------------------------------------------------ frame */

    private fun build() {
        root = FrameLayout(this).apply { setBackgroundColor(pal.bg) }
        mainCol = vbox().apply { setPadding(dp(14), dp(4), dp(14), dp(10)) }

        // wide control-center button (instead of a thin pull bar)
        val bar = hbox().apply {
            background = pressBg(pal.card, pal.card2, 20, if (pal.night || pal.vivid) 0 else dp(1), pal.line)
            setPadding(dp(14), dp(7), dp(12), dp(7))
            setOnClickListener { showPanel() }
        }
        bar.addView(glyphView("sliders", pal.ink, 20))
        bar.addView(label("מרכז בקרה", 14.5f, pal.text, true), lp(WRAP, WRAP).m(dp(8), 0, dp(4), 0))
        bar.addView(glyphView("chev_down", pal.sub, 16))
        bar.addView(View(this), lp(0, 1, 1f))
        statusWifi = ImageView(this)
        bar.addView(statusWifi, lp(dp(18), dp(18)).m(0, 0, dp(10), 0))
        statusBat = label("", 13.5f, pal.text, true)
        bar.addView(statusBat)
        statusBatIcon = ImageView(this)
        bar.addView(statusBatIcon, lp(dp(24), dp(24)).m(dp(4), 0, 0, 0))
        mainCol.addView(bar, lp(MATCH, WRAP))

        banner = vbox().apply { visibility = View.GONE }
        mainCol.addView(banner, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        pageHost = FrameLayout(this)
        pageHost.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) pageHost.post { computeFullPer(); renderPage() }
        }
        mainCol.addView(pageHost, lp(MATCH, 0, 1f))

        dots = hbox().apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) }
        mainCol.addView(dots, lp(MATCH, WRAP))

        dockBox = FrameLayout(this)
        mainCol.addView(dockBox, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        root.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun cellHeight(): Int {
        val labels = Prefs.b("labels", true)
        return dp(if (labels) (60 + 30 * Look.scale()).toInt() else 76)
    }

    private fun computeFullPer() {
        val h = pageHost.height - dp(40)
        if (h <= 0) return
        fullPer = maxOf(1, h / cellHeight()) * Prefs.i("cols", 4)
    }

    private fun computeRefs() {
        val out = ArrayList<Ref>()
        userPages.forEachIndexed { ui, blocks ->
            out.add(Ref(ui, -1))
            if ("apps" in blocks && fullPer > 0 && apps.isNotEmpty()) {
                var off = baseShown[ui] ?: (fullPer / 2)
                while (off < apps.size) { out.add(Ref(ui, off)); off += fullPer }
            }
        }
        refs = out
        cur = cur.coerceIn(0, maxOf(0, refs.size - 1))
    }

    private fun goToUserPage(ui: Int) {
        computeRefs()
        val i = refs.indexOfFirst { it.user == ui && it.appsFrom < 0 }
        if (i >= 0) cur = i
        renderPage()
    }

    /* ------------------------------------------------------------ rendering */

    private fun renderPage() {
        clockTv = null; greetTv = null; hebTv = null; gregTv = null; eventTv = null
        bigClocks.clear()
        if (userPages.isEmpty()) userPages.add(mutableListOf())
        computeRefs()
        pageHost.removeAllViews()
        val ref = refs.getOrNull(cur) ?: return
        val v = if (ref.appsFrom >= 0) appsOnlyPage(ref.appsFrom) else basePage(ref.user)
        pageHost.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        renderDots()
        updateClock()
    }

    private fun renderDots() {
        dots.removeAllViews()
        if (refs.size <= 1 && !Prefs.b("showAddWin", true)) { dots.visibility = View.GONE; return }
        dots.visibility = View.VISIBLE
        refs.forEachIndexed { i, r ->
            val active = i == cur
            val d = View(this).apply {
                background = if (active) roundBg(pal.ink, dp(4).toFloat())
                else if (r.appsFrom >= 0) roundBg(pal.sub, dp(1).toFloat())
                else roundBg(Color.TRANSPARENT, dp(4).toFloat(), dp(1), pal.sub)
            }
            val wrap = FrameLayout(this).apply {
                addView(d, FrameLayout.LayoutParams(dp(if (active) 20 else 8), dp(8), Gravity.CENTER))
                setOnClickListener { cur = i; renderPage() }
            }
            dots.addView(wrap, lp(dp(if (active) 28 else 18), dp(24)))
        }
        if (Prefs.b("showAddWin", true)) {
            val plus = FrameLayout(this).apply {
                addView(ImageView(context).apply { setImageDrawable(Glyph("plus", pal.sub, 2.2f)) },
                    FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
                setOnClickListener { newPage() }
            }
            dots.addView(plus, lp(dp(28), dp(24)).m(dp(6), 0, 0, 0))
        }
    }

    private fun basePage(ui: Int): View {
        val col = vbox().apply {
            isLongClickable = true
            setOnLongClickListener { homeMenu(); true }
        }
        val blocks = userPages[ui]
        if (blocks.isEmpty()) {
            val c = vbox().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                background = roundBg(pal.card, dp(22).toFloat(), if (pal.night) 0 else dp(1), pal.line)
                setPadding(dp(18), dp(22), dp(18), dp(22))
            }
            c.addView(glyphView("widget", pal.ink, 42))
            c.addView(label("חלון ריק", 19f, pal.text, true).apply { setPadding(0, dp(10), 0, 0) })
            c.addView(label("אפשר להוסיף לכאן שעון, ספרים, אפליקציות, תיקיות או גאדג׳טים. לחיצה ארוכה על המסך פותחת את האפשרויות.", 14f, pal.sub).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(14))
            })
            c.addView(pillButton("הוספת רכיב") { addBlockMenu(ui) })
            if (userPages.size > 1) c.addView(pillButton("סגירת החלון", filled = false) { closePage(ui) }, lp(WRAP, WRAP).m(0, dp(8), 0, 0))
            col.addView(c, lp(MATCH, WRAP).m(0, dp(10), 0, 0))
            col.addView(View(this), lp(MATCH, 0, 1f))
            return col
        }
        val flex = if ("apps" in blocks) "apps" else if ("books" in blocks) "books" else null
        blocks.forEachIndexed { bi, k ->
            val isFlex = k == flex
            val v = blockView(k, ui, bi, isFlex) ?: return@forEachIndexed
            col.addView(v, if (isFlex) lp(MATCH, 0, 1f).m(0, dp(10), 0, 0) else lp(MATCH, WRAP).m(0, dp(10), 0, 0))
        }
        if (flex == null) col.addView(View(this), lp(MATCH, 0, 1f))
        return col
    }

    private fun blockView(k: String, ui: Int, bi: Int, flex: Boolean): View? {
        val onLong = { blockMenu(k, ui, bi) }
        return when (k) {
            "hero" -> heroView(onLong)
            "books" -> booksBlock(flex, onLong)
            "apps" -> appsBlock(ui, onLong)
            "folders" -> foldersBlock(onLong)
            "g:calendar" -> miniCalendar(bi, onLong)
            "g:battery" -> batteryGadget(bi, onLong)
            "g:note" -> noteGadget(bi, onLong)
            "g:book" -> bookGadget(bi, onLong)
            "g:clock" -> clockGadget(bi, onLong)
            else -> if (k.startsWith("w:")) androidWidget(k, onLong) else null
        }
    }

    private fun sectionTitle(glyph: String, t: String, link: String?, onLong: () -> Unit, onLink: (() -> Unit)?): View {
        val r = hbox()
        val badge = FrameLayout(this).apply {
            background = roundBg(if (pal.pastel) pal.tile(t.length) else pal.ink, dp(8).toFloat())
            addView(ImageView(context).apply { setImageDrawable(Glyph(glyph, if (pal.pastel) pal.ink else pal.onInk, 2.1f)) },
                FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
        }
        r.addView(badge, lp(dp(26), dp(26)))
        r.addView(label(t, 17f, pal.text, true), lp(0, WRAP, 1f).m(dp(8), 0, 0, 0))
        if (link != null && onLink != null) {
            val l = hbox().apply {
                addView(label(link, 14.5f, pal.sub, true))
                addView(glyphView("chev_l", pal.sub, 16))
                setPadding(dp(10), dp(4), dp(6), dp(4))
                background = pressBg(pal.card, pal.card2, 14)
                setOnClickListener { onLink() }
            }
            r.addView(l, lp(WRAP, WRAP))
        }
        val more = FrameLayout(this).apply {
            addView(ImageView(context).apply { setImageDrawable(Glyph("dots", pal.sub)) }, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            background = pressBg(Color.TRANSPARENT, pal.card2, 14)
            setOnClickListener { onLong() }
        }
        r.addView(more, lp(dp(34), dp(30)).m(dp(4), 0, 0, 0))
        r.setPadding(0, 0, 0, dp(7))
        r.setOnLongClickListener { onLong(); true }
        return r
    }

    /* ------------------------------------------------------------ hero */

    private fun heroView(onLong: () -> Unit): View {
        val hero = vbox().apply {
            background = Look.heroBg(pal, dp(26).toFloat())
            clipToOutline = true
            setPadding(dp(20), dp(14), dp(20), dp(16))
            if (pal.vivid) elevation = dp(4).toFloat()
            setOnClickListener { go(Intent(context, CalendarActivity::class.java)) }
            setOnLongClickListener { onLong(); true }
        }
        val top = hbox()
        val g = label("", 15f, pal.heroSub, true)
        greetTv = g
        top.addView(g, lp(0, WRAP, 1f))
        top.addView(ImageView(this).apply { setImageDrawable(Glyph("calendar", pal.heroSub)) }, lp(dp(20), dp(20)))
        hero.addView(top, lp(MATCH, WRAP))
        val mid = hbox().apply { gravity = Gravity.BOTTOM }
        val c = TextView(this).apply {
            setTextColor(pal.heroText)
            textSize = Look.clockSize()
            typeface = Look.heavy
            includeFontPadding = false
            textDirection = View.TEXT_DIRECTION_LTR
            letterSpacing = -0.02f
        }
        clockTv = c
        mid.addView(c, lp(WRAP, WRAP))
        mid.addView(View(this), lp(0, 1, 1f))
        val dates = vbox().apply { gravity = Gravity.END }
        val h = label("", 20f, pal.heroText, true).oneLine().apply { gravity = Gravity.END }
        val gr = label("", 13.5f, pal.heroSub).oneLine().apply { gravity = Gravity.END }
        hebTv = h; gregTv = gr
        dates.addView(h, lp(WRAP, WRAP))
        dates.addView(gr, lp(WRAP, WRAP))
        mid.addView(dates, lp(WRAP, WRAP).m(dp(10), 0, 0, dp(8)))
        hero.addView(mid, lp(MATCH, WRAP).m(0, dp(2), 0, 0))
        val ev = label("", 13.5f, pal.heroText, true).apply {
            background = roundBg(if (Color.red(pal.heroText) > 128) 0x33FFFFFF else 0x1A000000, dp(12).toFloat())
            setPadding(dp(12), dp(4), dp(12), dp(5))
            visibility = View.GONE
        }
        eventTv = ev
        val evWrap = hbox().apply { addView(ev, lp(WRAP, WRAP)) }
        hero.addView(evWrap, lp(MATCH, WRAP).m(0, dp(8), 0, 0))
        return hero
    }

    private fun greeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "בוקר טוב"
            in 12..16 -> "צהריים טובים"
            in 17..20 -> "ערב טוב"
            else -> "לילה טוב"
        }
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val h = Heb.of(now)
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))
        clockTv?.let {
            it.visibility = if (Prefs.b("showClock", true)) View.VISIBLE else View.GONE
            it.text = time
        }
        greetTv?.text = greeting() + " · יום " + Heb.dayNames[h.dow]
        gregTv?.let {
            it.visibility = if (Prefs.b("showGreg", true)) View.VISIBLE else View.GONE
            it.text = SimpleDateFormat("d בMMMM yyyy", Locale("he")).format(Date(now))
        }
        hebTv?.let {
            it.visibility = if (Prefs.b("showHeb", true)) View.VISIBLE else View.GONE
            it.text = Heb.format(h)
        }
        eventTv?.let { evTv ->
            val ev = ArrayList(Heb.events(h))
            if (h.dow == Heb.SAT) ev.add(0, "שבת שלום")
            val om = Heb.omer(h)
            if (om > 0) ev.add("${Heb.gem(om)} לעומר")
            if (Prefs.b("showEvents", true) && ev.isNotEmpty()) {
                evTv.text = ev.joinToString(" · ")
                evTv.visibility = View.VISIBLE
            } else evTv.visibility = View.GONE
        }
        for (b in bigClocks) b.text = time
        val bat = batteryLevel()
        statusBat.text = if (bat >= 0) "$bat%" else ""
        statusBatIcon.setImageDrawable(Glyph("battery", pal.text).apply { fill = if (bat >= 0) bat / 100f else 1f })
        statusWifi.setImageDrawable(if (wifiOn()) Glyph("wifi", pal.text) else null)
    }

    private fun wifiOn(): Boolean = try {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } catch (e: Exception) { false }

    private fun isDefaultHome(): Boolean {
        val r = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        return r?.activityInfo?.packageName == packageName
    }

    private fun renderBanner() {
        banner.removeAllViews()
        val needHome = !isDefaultHome() && !Prefs.b("bannerHomeOff", false)
        val needFiles = !hasFiles()
        if (!needHome && !needFiles) { banner.visibility = View.GONE; return }
        banner.visibility = View.VISIBLE
        val c = hbox().apply {
            background = roundBg(pal.card, dp(16).toFloat(), dp(1), pal.line)
            setPadding(dp(14), dp(10), dp(8), dp(10))
        }
        val txt = vbox()
        txt.addView(label("כמה צעדים להשלמת ההתקנה", 15f, pal.text, true))
        val parts = ArrayList<String>()
        if (needFiles) parts.add("גישה לקבצים ולכרטיס הזיכרון")
        if (needHome) parts.add("קביעה כמסך הבית")
        txt.addView(label(parts.joinToString(" · "), 13f, pal.sub))
        c.addView(txt, lp(0, WRAP, 1f))
        c.addView(pillButton("השלמה") {
            if (needFiles) askFiles() else go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        }.apply { setTextSize(14f) })
        if (!needFiles) c.addView(iconButton("close") { Prefs.put("bannerHomeOff", true); renderBanner() })
        banner.addView(c, lp(MATCH, WRAP))
    }

    /* ------------------------------------------------------------ apps */

    private fun reloadApps() {
        Thread {
            Apps.load(this)
            val o = Apps.ordered()
            runOnUiThread {
                val changed = o.map { it.key } != apps.map { it.key }
                apps = o
                if (changed) renderPage()
                renderDock()
            }
        }.start()
    }

    private fun appsBlock(ui: Int, onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("grid", "אפליקציות", "הכול", onLong) { go(Intent(this, AppsActivity::class.java)) })
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) area.post {
                val shown = fillApps(area, 0)
                if (baseShown[ui] != shown) { baseShown[ui] = shown; computeRefs(); renderDots() }
            }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    private fun appsOnlyPage(offset: Int): View {
        val box = vbox().apply {
            isLongClickable = true
            setOnLongClickListener { homeMenu(); true }
        }
        val head = hbox().apply { setPadding(0, dp(10), 0, dp(6)) }
        head.addView(label("אפליקציות", 17f, pal.text, true), lp(0, WRAP, 1f))
        head.addView(label("${offset + 1}–${minOf(apps.size, offset + fullPer)} מתוך ${apps.size}", 13f, pal.sub, true))
        box.addView(head, lp(MATCH, WRAP))
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) area.post { fillApps(area, offset) }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    /** Fills a grid with as many apps as fit; returns how many were shown. */
    private fun fillApps(area: FrameLayout, offset: Int): Int {
        val h = area.height
        if (h <= 0) return 0
        area.removeAllViews()
        if (apps.isEmpty()) {
            area.addView(label("טוען אפליקציות…", 15f, pal.sub).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(MATCH, MATCH))
            return 0
        }
        val cols = Prefs.i("cols", 4)
        val rows = maxOf(1, h / cellHeight())
        val iconSize = if (cols >= 5) 50 else if (cols == 3) 64 else 56
        val grid = vbox()
        var shown = 0
        for (r in 0 until rows) {
            val row = hbox().apply { gravity = Gravity.TOP }
            for (c in 0 until cols) {
                val idx = offset + r * cols + c
                val a = apps.getOrNull(idx)
                if (a == null) { row.addView(View(this), lp(0, MATCH, 1f)); continue }
                row.addView(appCell(a, idx, iconSize), lp(0, MATCH, 1f))
                shown++
            }
            grid.addView(row, lp(MATCH, 0, 1f))
        }
        area.addView(grid, FrameLayout.LayoutParams(MATCH, MATCH))
        return shown
    }

    private fun appCell(a: AppEntry, idx: Int, iconSize: Int): View {
        val v = vbox().apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            setPadding(dp(2), dp(6), dp(2), dp(2))
            background = pressBg(Color.TRANSPARENT, pal.card2, 16)
        }
        v.addView(Apps.iconView(this, a, iconSize, pal, idx), lp(dp(iconSize), dp(iconSize)))
        if (Prefs.b("labels", true)) {
            v.addView(label(Apps.name(a), 12.5f, pal.text).oneLine().apply {
                gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0)
            }, lp(MATCH, WRAP))
        }
        v.setOnClickListener { Apps.launch(this, a) }
        v.setOnLongClickListener { appMenu(a); true }
        return v
    }

    private fun moveApp(a: AppEntry, to: Int) {
        val l = apps.toMutableList()
        val from = l.indexOfFirst { it.key == a.key }
        if (from < 0) return
        l.removeAt(from)
        l.add(to.coerceIn(0, l.size), a)
        Apps.saveOrder(l)
        apps = l
        renderPage()
    }

    private fun appMenu(a: AppEntry) {
        val pos = apps.indexOfFirst { it.key == a.key }
        Sheet.actions(this, Apps.name(a), listOf(
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם חדש למסך הבית", Apps.name(a), a.label) { s ->
                    Prefs.put("name:" + a.key, if (s.isEmpty() || s == a.label) null else s)
                    apps = Apps.ordered(); renderPage(); renderDock()
                }
            },
            Sheet.Item("שינוי סמל") { iconMenu(a) },
            Sheet.Item("הזזה קדימה") { moveApp(a, pos - 1) },
            Sheet.Item("הזזה אחורה") { moveApp(a, pos + 1) },
            Sheet.Item("העברה לתחילה") { moveApp(a, 0) },
            Sheet.Item("העברה לסוף") { moveApp(a, apps.size) },
            Sheet.Item("הוספה לשורה התחתונה") { Dock.addApp(a.key); renderDock() },
            Sheet.Item("הסרה ממסך הבית") {
                Apps.hide(a); apps = Apps.ordered(); renderPage()
                toast("אפשר להחזיר מ'כל האפליקציות' או מההגדרות")
            },
            Sheet.Item("מידע על האפליקציה") {
                go(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg)))
            },
            Sheet.Item("הסרת התקנה מהמכשיר", red = true) {
                go(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)))
            }
        ))
    }

    private fun iconMenu(a: AppEntry) {
        fun set(v: String?) {
            Prefs.put("icon:" + a.key, v); renderPage(); renderDock()
        }
        Sheet.actions(this, "סמל עבור " + Apps.name(a), listOf(
            Sheet.Item("סמל מקורי") { set(null) },
            Sheet.Item("אות ראשונה") { set("letter") },
            Sheet.Item("אימוג׳י או תו לבחירה…") {
                Sheet.input(this, "הקלידו אימוג׳י או תו", "", "למשל 📖 או א") { s ->
                    if (s.isNotEmpty()) set("emoji:" + s.take(4))
                }
            },
            Sheet.Item("סמל של אפליקציה אחרת…") {
                val others = Apps.all.sortedBy { it.label.lowercase() }
                Sheet.actions(this, "בחירת סמל", others.map { o ->
                    Sheet.Item(o.label) { set("app:" + o.key) }
                })
            }
        ))
    }

    /* ------------------------------------------------------------ books */

    private fun scanBooks() {
        if (!hasFiles()) return
        Thread {
            Books.scan()
            val n = Books.cache.size
            runOnUiThread {
                if (n != lastBookCount) {
                    lastBookCount = n
                    if (userPages.getOrNull(curUser())?.any { it == "books" || it == "g:book" } == true) renderPage()
                }
            }
        }.start()
    }

    private fun booksBlock(flex: Boolean, onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("book", "ספרים אחרונים", "הכול", onLong) { go(Intent(this, BooksActivity::class.java)) })
        if (!hasFiles()) {
            box.addView(pillButton("אישור גישה לקבצים כדי להציג ספרים", filled = false) { askFiles() }.apply { setTextSize(14f) })
            return box
        }
        if (!flex) {
            val grid = vbox()
            drawBookRows(grid, 1)
            box.addView(grid, lp(MATCH, WRAP))
            return box
        }
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, orr, ob ->
            if (b - t != ob - ot || r - l != orr - ol) area.post {
                val w = area.width
                val rowH = (w - dp(30)) / 4 + dp(26)
                val rows = maxOf(1, area.height / maxOf(1, rowH))
                area.removeAllViews()
                val grid = vbox()
                drawBookRows(grid, rows)
                area.addView(grid, FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT))
            }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    private fun drawBookRows(target: LinearLayout, rows: Int) {
        val list = Books.recent(rows * 4)
        if (list.isEmpty()) {
            target.addView(label("לא נמצאו עדיין ספרים במכשיר או בכרטיס הזיכרון", 13f, pal.sub))
            return
        }
        for (r in 0 until rows) {
            val row = hbox().apply { gravity = Gravity.TOP }
            for (i in 0 until 4) {
                val idx = r * 4 + i
                val f = list.getOrNull(idx)
                val cell = vbox()
                if (f != null) {
                    val sq = SquareFrame(this).apply {
                        background = roundBg(pal.card, dp(14).toFloat(), if (pal.vivid) 0 else dp(1), pal.line)
                        clipToOutline = true
                        if (pal.vivid) elevation = dp(3).toFloat()
                    }
                    val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                    Covers.load(this, f, iv)
                    if (idx == 0) {
                        val tag = label("ממשיכים", 10.5f, pal.onInk, true).apply {
                            background = roundBg(pal.ink, dp(8).toFloat())
                            setPadding(dp(7), dp(2), dp(7), dp(3))
                        }
                        sq.addView(tag, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
                            setMargins(dp(6), 0, dp(6), dp(6))
                        })
                    }
                    sq.setOnClickListener { Store.open(this, f) }
                    sq.setOnLongClickListener { bookMenu(f); true }
                    cell.addView(sq, lp(MATCH, WRAP))
                    cell.addView(label(f.nameWithoutExtension, 12f, pal.text).oneLine().apply { setPadding(dp(2), dp(5), dp(2), 0) })
                }
                row.addView(cell, lp(0, WRAP, 1f).m(if (i == 0) 0 else dp(5), 0, if (i == 3) 0 else dp(5), 0))
            }
            target.addView(row, lp(MATCH, WRAP).m(0, if (r == 0) 0 else dp(8), 0, 0))
        }
    }

    private fun bookMenu(f: File) {
        Sheet.actions(this, f.nameWithoutExtension, listOf(
            Sheet.Item("פתיחה") { Store.open(this, f) },
            Sheet.Item("פתיחה באמצעות…") { Store.open(this, f, chooser = true) },
            Sheet.Item("הצגה בתיקייה") { go(Intent(this, FilesActivity::class.java).putExtra("path", f.parent)) },
            Sheet.Item("שמירה בשורה התחתונה") { Dock.setSlot(Dock.items().indexOf("empty").let { if (it < 0) Dock.items().size else it }, "book:" + f.path); renderDock() },
            Sheet.Item("כל הספרים") { go(Intent(this, BooksActivity::class.java)) }
        ))
    }

    /* ------------------------------------------------------------ folders */

    private fun foldersBlock(onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("folder", "תיקיות", "הוספה", onLong) { pickFolder() })
        val row = hbox()
        val all = Store.homeFolders()
        val visible = if (all.size > 4) all.take(3) else all
        visible.forEachIndexed { i, path -> row.addView(folderTile(path, i), lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0)) }
        if (all.size > 4) {
            row.addView(tile("dots", "עוד ${all.size - 3}", 3) { moreFolders(all) }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
        } else if (all.size < 4) {
            row.addView(tile("plus", "הוספה", all.size) { pickFolder() }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
            for (x in all.size + 1 until 4) row.addView(View(this), lp(0, 1, 1f).m(dp(4), 0, dp(4), 0))
        }
        box.addView(row, lp(MATCH, WRAP))
        return box
    }

    private fun tile(kind: String, text: String, i: Int, onClick: () -> Unit): LinearLayout = vbox().apply {
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(10), dp(4), dp(8))
        val bgc = if (pal.pastel) pal.tile(i + 2) else pal.card
        background = pressBg(bgc, pal.card2, 16)
        if (pal.vivid) elevation = dp(2).toFloat()
        addView(glyphView(kind, pal.ink, 28))
        addView(label(text, 12.5f, pal.text, pal.vivid).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }, lp(MATCH, WRAP))
        setOnClickListener { onClick() }
    }

    private fun openFolder(path: String) {
        val isVol = Store.isVolumeRoot(path) && path != Store.root().path
        if (isVol) go(Intent(this, FilesActivity::class.java).putExtra("volume", path))
        else go(Intent(this, FilesActivity::class.java).putExtra("path", path))
    }

    private fun folderTile(path: String, i: Int): View {
        val isVol = Store.isVolumeRoot(path) && path != Store.root().path
        val t = tile(if (isVol) "sd" else "folder", Store.folderName(path), i) { openFolder(path) }
        t.setOnLongClickListener { folderMenu(path); true }
        return t
    }

    private fun folderMenu(path: String) {
        Sheet.actions(this, Store.folderName(path), listOf(
            Sheet.Item("פתיחה") { openFolder(path) },
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם התיקייה במסך הבית", Store.folderName(path), File(path).name) { s ->
                    Prefs.put("fname:$path", if (s.isEmpty()) null else s); renderPage()
                }
            },
            Sheet.Item("שמירה בשורה התחתונה") {
                Dock.setSlot(Dock.items().indexOf("empty").let { if (it < 0) Dock.items().size else it }, "folder:$path"); renderDock()
            },
            Sheet.Item("הסרה ממסך הבית", red = true) { Store.removeHomeFolder(path); renderPage() }
        ))
    }

    private fun moreFolders(all: List<String>) {
        Sheet.actions(this, "תיקיות", all.map { p ->
            Sheet.Item(Store.folderName(p)) { openFolder(p) }
        } + Sheet.Item("הוספת תיקייה…") { pickFolder() })
    }

    private fun pickFolder() {
        if (!hasFiles()) { askFiles(); return }
        go(Intent(this, FilesActivity::class.java).putExtra("pick", true))
    }

    /* ------------------------------------------------------------ dock */

    private fun renderDock() {
        dockBox.removeAllViews()
        val keys = Dock.items()
        if (keys.isEmpty() || !Prefs.b("dockOn", true)) { dockBox.visibility = View.GONE; return }
        dockBox.visibility = View.VISIBLE
        val d = hbox().apply {
            background = roundBg(pal.dockBg, dp(30).toFloat(), if (pal.dockBg == pal.card && !pal.night) dp(1) else 0, pal.line)
            if (pal.vivid) elevation = dp(6).toFloat()
            setPadding(dp(6), dp(9), dp(6), dp(9))
            isLongClickable = true
            setOnLongClickListener { DockEditor.open(this@HomeActivity) { renderDock() }; true }
        }
        keys.forEachIndexed { i, k ->
            if (k == "empty") { d.addView(View(this), lp(0, dp(10), 1f)); return@forEachIndexed }
            val v = vbox().apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, dp(3))
                background = pressBg(Color.TRANSPARENT, if (Color.red(pal.dockBg) > 200) pal.card2 else 0x33FFFFFF, 20)
            }
            when {
                k.startsWith("app:") -> {
                    val a = Apps.byKey(k.removePrefix("app:"))
                    if (a != null) v.addView(Apps.iconView(this, a, 32, pal, i), lp(dp(32), dp(32)))
                    else v.addView(glyphView("dots", pal.dockFg, 30))
                }
                k.startsWith("book:") -> {
                    val sq = FrameLayout(this).apply { background = roundBg(pal.card, dp(8).toFloat()); clipToOutline = true }
                    val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                    val f = File(k.removePrefix("book:"))
                    if (f.exists()) Covers.load(this, f, iv) else iv.setImageDrawable(Glyph("book", pal.ink))
                    v.addView(sq, lp(dp(32), dp(32)))
                }
                else -> v.addView(glyphView(Dock.glyph(k), pal.dockFg, 30))
            }
            v.addView(label(Dock.shortLabel(k), 12f, pal.dockFg, true).oneLine().apply { gravity = Gravity.CENTER; setPadding(dp(2), dp(3), dp(2), 0) }, lp(MATCH, WRAP))
            v.setOnClickListener { dockAction(k) }
            v.setOnLongClickListener { DockEditor.slot(this@HomeActivity, i) { renderDock() }; true }
            d.addView(v, lp(0, WRAP, 1f))
        }
        dockBox.addView(d, FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun dockAction(k: String) {
        when {
            k.startsWith("app:") -> Apps.byKey(k.removePrefix("app:"))?.let { Apps.launch(this, it) } ?: toast("האפליקציה לא נמצאה")
            k.startsWith("folder:") -> openFolder(k.removePrefix("folder:"))
            k.startsWith("book:") -> {
                val f = File(k.removePrefix("book:"))
                if (f.exists()) Store.open(this, f) else toast("הספר לא נמצא")
            }
            else -> when (k) {
                "wifi" -> openWifi()
                "files" -> go(Intent(this, FilesActivity::class.java))
                "books" -> go(Intent(this, BooksActivity::class.java))
                "settings" -> go(Intent(this, SettingsActivity::class.java))
                "apps" -> go(Intent(this, AppsActivity::class.java))
                "calendar" -> go(Intent(this, CalendarActivity::class.java))
                "control" -> showPanel()
                "refresh" -> einkRefresh()
                "bt" -> go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                "addwin" -> newPage()
                "sd" -> {
                    val sd = Store.volumes().firstOrNull { it.removable }
                    if (sd == null) toast("לא זוהה כרטיס זיכרון")
                    else go(Intent(this, FilesActivity::class.java).putExtra("volume", sd.dir.path))
                }
                "android" -> go(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    /* ------------------------------------------------------------ windows & blocks */

    private fun homeMenu() {
        val ui = curUser()
        val items = ArrayList<Sheet.Item>()
        items.add(Sheet.Item("הוספת רכיב לחלון הזה") { addBlockMenu(ui) })
        items.add(Sheet.Item("חלון חדש") { newPage() })
        if (userPages.size > 1) {
            if (ui > 0) items.add(Sheet.Item("הזזת החלון ימינה") { swapPages(ui, ui - 1) })
            if (ui < userPages.size - 1) items.add(Sheet.Item("הזזת החלון שמאלה") { swapPages(ui, ui + 1) })
            items.add(Sheet.Item("סגירת החלון הזה", red = true) { closePage(ui) })
        }
        items.add(Sheet.Item("ערכת נושא") { themeMenu() })
        items.add(Sheet.Item("השורה התחתונה") { DockEditor.open(this) { renderDock() } })
        items.add(Sheet.Item("אילו אפליקציות יופיעו") { go(Intent(this, AppsActivity::class.java).putExtra("manage", true)) })
        items.add(Sheet.Item("הגדרות") { go(Intent(this, SettingsActivity::class.java)) })
        Sheet.actions(this, "חלון ${ui + 1} מתוך ${userPages.size}", items)
    }

    private fun themeMenu() {
        Sheet.choice(this, "ערכת נושא", Look.themes, Look.theme) { v ->
            Prefs.put("theme", v); Apps.clearIcons()
            if (v == "custom") ThemeEditor.open(this) { recreate() } else recreate()
        }
    }

    private fun newPage() {
        val ui = curUser()
        userPages.add(ui + 1, mutableListOf())
        savePages()
        goToUserPage(ui + 1)
        toast("נפתח חלון חדש")
    }

    private fun swapPages(a: Int, b: Int) {
        val t = userPages[a]; userPages[a] = userPages[b]; userPages[b] = t
        savePages(); goToUserPage(b)
    }

    private fun closePage(ui: Int) {
        Sheet.confirm(this, "סגירת החלון", "החלון ייסגר והרכיבים שבו יוסרו. אם יש בו אפליקציות או שעון, הם יועברו לחלון הראשון.", "סגירה") {
            val blocks = userPages[ui]
            userPages.removeAt(ui)
            if (userPages.isEmpty()) userPages.add(mutableListOf())
            for (k in blocks) {
                if (k == "apps" || k == "hero") { if (Pages.pageOf(userPages, k) < 0) userPages[0].add(if (k == "hero") 0 else userPages[0].size, k) }
                else if (k.startsWith("w:")) deleteWidget(k)
            }
            savePages(); cur = 0; renderPage()
        }
    }

    private fun deleteWidget(k: String) {
        val id = k.removePrefix("w:").toIntOrNull() ?: return
        try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        widgetViews.remove(id)
    }

    private fun addBlock(ui: Int, k: String) {
        val u = ui.coerceIn(0, userPages.size - 1)
        val existing = Pages.pageOf(userPages, k)
        if (existing >= 0 && !k.startsWith("w:")) { userPages[existing].remove(k) }
        if (k == "hero") userPages[u].add(0, k) else userPages[u].add(k)
        savePages()
        goToUserPage(u)
    }

    private fun addBlockMenu(ui: Int) {
        val items = ArrayList<Sheet.Item>()
        for ((k, n) in Pages.coreBlocks + Pages.gadgetBlocks) {
            val where = Pages.pageOf(userPages, k)
            val suffix = if (where == ui) " (כבר כאן)" else if (where >= 0) " (מחלון ${where + 1})" else ""
            if (where == ui) continue
            items.add(Sheet.Item(n + suffix) { addBlock(ui, k) })
        }
        items.add(Sheet.Item("גאדג׳ט של אפליקציה…") { widgetPicker(ui) })
        Sheet.actions(this, "הוספה לחלון ${ui + 1}", items)
    }

    private fun blockMenu(k: String, ui: Int, bi: Int) {
        val blocks = userPages[ui]
        val items = ArrayList<Sheet.Item>()
        if (bi > 0) items.add(Sheet.Item("הזזה למעלה") { blocks.removeAt(bi); blocks.add(bi - 1, k); savePages(); renderPage() })
        if (bi < blocks.size - 1) items.add(Sheet.Item("הזזה למטה") { blocks.removeAt(bi); blocks.add(bi + 1, k); savePages(); renderPage() })
        items.add(Sheet.Item("העברה לחלון אחר…") { moveBlockMenu(k, ui) })
        if (k == "g:note") items.add(Sheet.Item("עריכת הפתק") { editNote() })
        if (k == "books") items.add(Sheet.Item("כל הספרים") { go(Intent(this, BooksActivity::class.java)) })
        if (k == "apps") items.add(Sheet.Item("אילו אפליקציות יופיעו") { go(Intent(this, AppsActivity::class.java).putExtra("manage", true)) })
        items.add(Sheet.Item("הסרה מהחלון", red = true) {
            blocks.remove(k)
            if (k.startsWith("w:")) deleteWidget(k)
            savePages(); renderPage()
        })
        Sheet.actions(this, blockName(k), items)
    }

    private fun moveBlockMenu(k: String, ui: Int) {
        val items = ArrayList<Sheet.Item>()
        for (i in userPages.indices) {
            if (i == ui) continue
            items.add(Sheet.Item("חלון ${i + 1}" + if (userPages[i].isEmpty()) " (ריק)" else "") { moveBlock(k, ui, i) })
        }
        items.add(Sheet.Item("חלון חדש") {
            userPages.add(mutableListOf()); moveBlock(k, ui, userPages.size - 1)
        })
        Sheet.actions(this, "להעביר את \"${blockName(k)}\" אל", items)
    }

    private fun moveBlock(k: String, from: Int, to: Int) {
        userPages[from].remove(k)
        if (k == "hero") userPages[to].add(0, k) else userPages[to].add(k)
        savePages()
        goToUserPage(to)
    }

    private fun blockName(k: String): String {
        if (k.startsWith("w:")) {
            val id = k.removePrefix("w:").toIntOrNull()
            return id?.let { awm.getAppWidgetInfo(it) }?.loadLabel(packageManager) ?: "גאדג׳ט"
        }
        return Pages.name(k)
    }

    /* ------------------------------------------------------------ android widgets */

    private fun widgetPicker(ui: Int) {
        val providers = try { awm.installedProviders } catch (e: Exception) { emptyList<AppWidgetProviderInfo>() }
        if (providers.isEmpty()) { toast("אין במכשיר גאדג׳טים של אפליקציות"); return }
        val items = ArrayList<Sheet.Item>()
        for (p in providers.sortedBy { it.loadLabel(packageManager) ?: "" }) {
            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(p.provider.packageName, 0)).toString()
            } catch (e: Exception) { p.provider.packageName }
            items.add(Sheet.Item(appName + " · " + (p.loadLabel(packageManager) ?: "")) { addAndroidWidget(p, ui) })
        }
        Sheet.actions(this, "גאדג׳טים של אפליקציות", items)
    }

    private fun addAndroidWidget(p: AppWidgetProviderInfo, ui: Int) {
        val id = try { host.allocateAppWidgetId() } catch (e: Exception) { toast("לא ניתן להוסיף גאדג׳ט"); return }
        pendingWidget = id
        pendingPage = ui
        val ok = try { awm.bindAppWidgetIdIfAllowed(id, p.provider) } catch (e: Exception) { false }
        if (ok) configureWidget(id)
        else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(i, REQ_BIND)
            } catch (e: Exception) {
                host.deleteAppWidgetId(id); toast("המכשיר לא מאפשר להוסיף את הגאדג׳ט הזה")
            }
        }
    }

    private fun configureWidget(id: Int) {
        val info = awm.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONF, null)
                return
            } catch (e: Exception) { ignore(e) }
        }
        addBlock(pendingPage, "w:$id")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget) ?: pendingWidget
        if (requestCode == REQ_BIND) {
            if (resultCode == RESULT_OK) configureWidget(id) else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        } else if (requestCode == REQ_CONF) {
            if (resultCode == RESULT_OK) addBlock(pendingPage, "w:$id") else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        }
    }

    private fun androidWidget(k: String, onLong: () -> Unit): View? {
        val id = k.removePrefix("w:").toIntOrNull() ?: return null
        val info = awm.getAppWidgetInfo(id) ?: return null
        val hv = widgetViews[id] ?: try {
            host.createView(applicationContext, id, info).also { widgetViews[id] = it }
        } catch (e: Exception) { return null }
        (hv.parent as? android.view.ViewGroup)?.removeView(hv)
        val box = vbox()
        val head = hbox()
        head.addView(label(blockName(k), 12.5f, pal.sub, true).oneLine(), lp(0, WRAP, 1f))
        head.addView(FrameLayout(this).apply {
            addView(ImageView(context).apply { setImageDrawable(Glyph("dots", pal.sub)) }, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
            setOnClickListener { onLong() }
        }, lp(dp(30), dp(24)))
        box.addView(head, lp(MATCH, WRAP))
        val card = FrameLayout(this).apply {
            background = roundBg(pal.card, dp(20).toFloat(), if (pal.night) 0 else dp(1), pal.line)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipToOutline = true
        }
        val h = info.minHeight.coerceIn(dp(70), dp(240))
        card.addView(hv, FrameLayout.LayoutParams(MATCH, h))
        box.addView(card, lp(MATCH, WRAP))
        return box
    }

    /* ------------------------------------------------------------ built-in gadgets */

    private fun gadgetCard(i: Int, onLong: () -> Unit): LinearLayout = vbox().apply {
        background = roundBg(if (pal.pastel && !pal.vivid) pal.tile(i + 4) else pal.card, dp(20).toFloat(),
            if (pal.vivid || pal.pastel || pal.night) 0 else dp(1), pal.line)
        if (pal.vivid) elevation = dp(3).toFloat()
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnLongClickListener { onLong(); true }
    }

    private fun miniCalendar(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        c.setOnClickListener { go(Intent(this, CalendarActivity::class.java)) }
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val hd = Heb.of(cal.timeInMillis)
        val head = hbox()
        head.addView(label(SimpleDateFormat("LLLL", Locale("he")).format(cal.time), 16f, pal.text, true), lp(0, WRAP, 1f))
        head.addView(label(Heb.monthName(hd.y, hd.m) + " " + Heb.yearStr(hd.y), 14f, pal.sub, true))
        c.addView(head, lp(MATCH, WRAP))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val offset = cal.get(Calendar.DAY_OF_WEEK) - 1
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val wk = hbox().apply { setPadding(0, dp(6), 0, dp(2)) }
        for (d in 1..7) wk.addView(label(Heb.dayLetters[d], 11f, pal.sub, true).apply { gravity = Gravity.CENTER }, lp(0, WRAP, 1f))
        c.addView(wk, lp(MATCH, WRAP))
        var day = 1
        for (w in 0 until 6) {
            if (day > days) break
            val row = hbox()
            for (col in 0 until 7) {
                val idx = w * 7 + col
                if (idx < offset || day > days) { row.addView(View(this), lp(0, dp(22), 1f)); continue }
                val isToday = day == today
                val t = label(day.toString(), 12.5f, if (isToday) pal.onInk else pal.text, isToday || col == 6).apply {
                    gravity = Gravity.CENTER
                    if (isToday) background = roundBg(pal.ink, dp(11).toFloat())
                }
                row.addView(t, lp(0, dp(22), 1f).m(dp(3), dp(1), dp(3), dp(1)))
                day++
            }
            c.addView(row, lp(MATCH, WRAP))
        }
        return c
    }

    private fun batteryGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val r = hbox()
        val bat = batteryLevel()
        r.addView(ImageView(this).apply { setImageDrawable(Glyph("battery", pal.ink, 1.6f).apply { fill = if (bat >= 0) bat / 100f else 1f }) }, lp(dp(54), dp(54)))
        val col = vbox()
        col.addView(label(if (bat >= 0) "$bat%" else "—", 28f, pal.text, true).apply { typeface = Look.heavy })
        col.addView(label("סוללה", 13f, pal.sub))
        r.addView(col, lp(WRAP, WRAP).m(dp(10), 0, dp(16), 0))
        val st = vbox()
        for (v in Store.volumes()) {
            st.addView(label(v.name, 13f, pal.text, true).oneLine())
            st.addView(label(Store.sizeStr(v.dir.freeSpace) + " פנויים", 12f, pal.sub).oneLine())
        }
        r.addView(st, lp(0, WRAP, 1f))
        c.addView(r)
        c.setOnClickListener { go(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_SETTINGS)) }
        return c
    }

    private fun editNote() {
        Sheet.input(this, "פתק", Prefs.s("note", ""), "מה לזכור?", multiline = true) { s -> Prefs.put("note", s); renderPage() }
    }

    private fun noteGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val head = hbox()
        head.addView(glyphView("note", pal.ink, 20))
        head.addView(label("פתק", 15f, pal.text, true), lp(0, WRAP, 1f).m(dp(8), 0, 0, 0))
        c.addView(head)
        val n = Prefs.s("note", "")
        c.addView(label(if (n.isEmpty()) "הקישו כדי לכתוב פתק" else n, 15f, if (n.isEmpty()) pal.sub else pal.text).apply {
            maxLines = 5; setPadding(0, dp(6), 0, 0)
        })
        c.setOnClickListener { editNote() }
        return c
    }

    private fun bookGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val f = Books.recent(1).firstOrNull()
        val r = hbox()
        val sq = SquareFrame(this).apply {
            background = roundBg(pal.card2, dp(10).toFloat()); clipToOutline = true
        }
        val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
        if (f != null) Covers.load(this, f, iv) else iv.setImageDrawable(Glyph("book", pal.sub))
        r.addView(sq, lp(dp(78), dp(78)))
        val col = vbox()
        col.addView(label("ממשיכים לקרוא", 13f, pal.sub, true))
        col.addView(label(f?.nameWithoutExtension ?: "עדיין לא נפתח ספר", 17f, pal.text, true).apply { maxLines = 2 })
        if (f != null) col.addView(pillButton("המשך קריאה") { Store.open(this, f) }.apply {
            setTextSize(13.5f); setPadding(dp(14), dp(6), dp(14), dp(6))
        }, lp(WRAP, WRAP).m(0, dp(6), 0, 0))
        r.addView(col, lp(0, WRAP, 1f).m(dp(12), 0, 0, 0))
        c.addView(r)
        c.setOnClickListener { if (f != null) Store.open(this, f) else go(Intent(this, BooksActivity::class.java)) }
        return c
    }

    private fun clockGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong).apply { gravity = Gravity.CENTER_HORIZONTAL }
        val t = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            textSize = 92f; typeface = Look.heavy; setTextColor(pal.text); includeFontPadding = false
            gravity = Gravity.CENTER
        }
        bigClocks.add(t)
        c.addView(t, lp(MATCH, WRAP))
        c.addView(label(Heb.format(Heb.of(System.currentTimeMillis())), 16f, pal.sub, true).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP))
        return c
    }

    /* ------------------------------------------------------------ gestures */

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; downT = ev.eventTime }
            MotionEvent.ACTION_UP -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                val dt = ev.eventTime - downT
                val th = dp(70)
                var handled = false
                if (dt < 900) {
                    if (panel != null) {
                        if (dy < -th && abs(dy) > abs(dx)) { hidePanel(); handled = true }
                    } else if (dy > th && abs(dy) > 1.4f * abs(dx) && Prefs.b("swipeDown", true)
                        && downY > dp(90) && !inWidget(downX, downY)) {
                        showPanel(); handled = true
                    } else if (abs(dx) > th && abs(dx) > 1.4f * abs(dy) && !inWidget(downX, downY) && inView(pageHost, downY)) {
                        if (dx > 0 && cur < refs.size - 1) { cur++; renderPage() }
                        else if (dx < 0 && cur > 0) { cur--; renderPage() }
                        handled = true
                    }
                }
                if (handled) {
                    val c = MotionEvent.obtain(ev)
                    c.action = MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(c)
                    c.recycle()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun inView(v: View, rawY: Float): Boolean {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return rawY >= loc[1] && rawY <= loc[1] + v.height
    }

    private fun inWidget(x: Float, y: Float): Boolean {
        for (hv in widgetViews.values) {
            if (hv.parent == null || !hv.isShown) continue
            val loc = IntArray(2)
            hv.getLocationOnScreen(loc)
            if (x >= loc[0] && x <= loc[0] + hv.width && y >= loc[1] && y <= loc[1] + hv.height) return true
        }
        return false
    }

    /* ------------------------------------------------------------ control center */

    private fun batteryLevel(): Int = try {
        (getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) { -1 }

    private fun openWifi() {
        if (Build.VERSION.SDK_INT >= 29) go(Intent(Settings.Panel.ACTION_WIFI), Intent(Settings.ACTION_WIFI_SETTINGS))
        else go(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun needService(what: String) {
        Sheet.confirm(this, what, "כדי להשתמש בכפתור הזה יש להפעיל את השירות \"היכל הספרים – פעולות מסך\" בהגדרות הנגישות. השירות לא קורא שום תוכן מהמסך.", "פתיחת הגדרות נגישות") {
            go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    fun einkRefresh() {
        val v = View(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        root.postDelayed({
            v.setBackgroundColor(Color.WHITE)
            root.postDelayed({ root.removeView(v) }, 140)
        }, 140)
    }

    private fun showPanel() {
        if (panel != null) return
        val veil = FrameLayout(this).apply {
            setBackgroundColor(if (pal.night) 0xDD000000.toInt() else 0xDDFFFFFF.toInt())
            setOnClickListener { hidePanel() }
            isClickable = true
        }
        val sheet = vbox().apply {
            background = roundBg(pal.bg, dp(26).toFloat(), dp(2), pal.ink)
            setPadding(dp(16), dp(14), dp(16), dp(10))
            isClickable = true
        }
        val head = hbox()
        val hv = vbox()
        hv.addView(label("מרכז בקרה", 22f, pal.text, true))
        val bat = batteryLevel()
        hv.addView(label(SimpleDateFormat("HH:mm", Locale.US).format(Date()) + (if (bat >= 0) "  ·  סוללה $bat%" else ""), 14f, pal.sub))
        head.addView(hv, lp(0, WRAP, 1f))
        head.addView(iconButton("close") { hidePanel() })
        sheet.addView(head, lp(MATCH, WRAP))

        class T(val kind: String, val text: String, val f: () -> Unit)
        val tiles = listOf(
            T("wifi", "Wi‑Fi") { openWifi() },
            T("bt", "בלוטות׳") { go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            T("battery", if (bat >= 0) "$bat%" else "סוללה") {
                go(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            },
            T("refresh", "רענון מסך") { hidePanel(); root.post { einkRefresh() } },
            T("back", "חזרה") {
                val k = Prefs.s("lastApp", "")
                val a = Apps.byKey(k)
                if (a != null) { hidePanel(); Apps.launch(this, a) }
                else if (!ActionService.act(AccessibilityService.GLOBAL_ACTION_BACK)) toast("עדיין לא נפתחה אפליקציה")
            },
            T("windows", "חלונות") {
                if (!ActionService.act(AccessibilityService.GLOBAL_ACTION_RECENTS)) needService("חלונות אחרונים")
                else hidePanel()
            },
            T("bell", "התראות") {
                if (!ActionService.act(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) {
                    try {
                        @Suppress("WrongConstant")
                        val sb = getSystemService("statusbar")
                        sb?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(sb)
                    } catch (e: Exception) { needService("התראות") }
                }
            },
            T("screen", "מסך רגיל") {
                Sheet.confirm(this, "מעבר למסך הבית הרגיל", "בחרו במסך הבית של BOOX כברירת מחדל. אפשר לחזור להיכל הספרים בכל עת מאותו מקום.", "בחירת מסך בית") {
                    go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                }
            },
            T("calendar", "לוח שנה") { hidePanel(); go(Intent(this, CalendarActivity::class.java)) },
            T("widget", "הוספת רכיב") { hidePanel(); addBlockMenu(curUser()) },
            T("palette", "ערכת נושא") { hidePanel(); themeMenu() },
            T("plus", "חלון חדש") { hidePanel(); newPage() },
            T("folder", "קבצים") { hidePanel(); go(Intent(this, FilesActivity::class.java)) },
            T("grid", "אפליקציות") { hidePanel(); go(Intent(this, AppsActivity::class.java)) },
            T("gear", "הגדרות") { hidePanel(); go(Intent(this, SettingsActivity::class.java)) },
            T("sliders", "אנדרואיד") { hidePanel(); go(Intent(Settings.ACTION_SETTINGS)) }
        )
        var row: LinearLayout? = null
        tiles.forEachIndexed { i, t ->
            if (i % 4 == 0) {
                row = hbox()
                sheet.addView(row, lp(MATCH, WRAP).m(0, dp(10), 0, 0))
            }
            val v = vbox().apply {
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(12), dp(2), dp(10))
                background = pressBg(if (pal.pastel) pal.tile(i) else pal.card, pal.card2, 18)
                val g = Glyph(t.kind, pal.ink)
                if (t.kind == "battery" && bat >= 0) g.fill = bat / 100f
                addView(ImageView(context).apply { setImageDrawable(g) }, lp(dp(30), dp(30)))
                addView(label(t.text, 12.5f, pal.text).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) }, lp(MATCH, WRAP))
                setOnClickListener { t.f() }
            }
            row?.addView(v, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
        }

        val br = hbox().apply {
            background = roundBg(pal.card, dp(18).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        br.addView(glyphView("sun", pal.ink, 26))
        val canWrite = Settings.System.canWrite(this)
        val curB = try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }
        val seek = SeekBar(this).apply {
            max = 255
            progress = curB
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    if (Settings.System.canWrite(this@HomeActivity)) {
                        try {
                            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, p)
                        } catch (e: Exception) { ignore(e) }
                    } else {
                        val lpw = window.attributes
                        lpw.screenBrightness = maxOf(0.01f, p / 255f)
                        window.attributes = lpw
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        br.addView(seek, lp(0, WRAP, 1f))
        sheet.addView(label("בהירות", 14f, pal.sub, true).apply { setPadding(dp(4), dp(14), dp(4), dp(4)) })
        sheet.addView(br, lp(MATCH, WRAP))
        if (!canWrite) {
            sheet.addView(label("לשליטה בבהירות המערכת יש לאשר הרשאה – הקישו כאן", 12.5f, pal.sub).apply {
                setPadding(dp(4), dp(6), dp(4), 0)
                setOnClickListener {
                    go(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                }
            })
        }
        val close = pillButton("סגירה", filled = false) { hidePanel() }
        sheet.addView(close, lp(MATCH, WRAP).m(0, dp(12), 0, dp(2)))

        val slp = FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        slp.setMargins(dp(10), dp(8), dp(10), 0)
        veil.addView(sheet, slp)
        root.addView(veil, FrameLayout.LayoutParams(MATCH, MATCH))
        panel = veil
    }

    private fun hidePanel() {
        panel?.let { root.removeView(it) }
        panel = null
    }
}
