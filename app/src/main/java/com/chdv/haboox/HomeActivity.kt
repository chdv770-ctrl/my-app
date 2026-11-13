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

class HomeActivity : BaseActivity() {

    companion object {
        private const val HOST_ID = 4711
        private const val REQ_BIND = 41
        private const val REQ_CONF = 42
    }

    private lateinit var root: FrameLayout
    private lateinit var mainCol: LinearLayout
    private lateinit var clock: TextView
    private lateinit var greetTv: TextView
    private lateinit var gregTv: TextView
    private lateinit var hebTv: TextView
    private lateinit var eventTv: TextView
    private lateinit var statusBat: TextView
    private lateinit var statusBatIcon: ImageView
    private lateinit var statusWifi: ImageView
    private lateinit var banner: LinearLayout
    private lateinit var gadgetsBox: LinearLayout
    private lateinit var booksSection: LinearLayout
    private lateinit var booksRow: LinearLayout
    private lateinit var appsBox: FrameLayout
    private lateinit var pageInd: LinearLayout
    private lateinit var foldersSection: LinearLayout
    private lateinit var foldersRow: LinearLayout
    private lateinit var dockBox: FrameLayout
    private var panel: View? = null

    private lateinit var host: AppWidgetHost
    private lateinit var awm: AppWidgetManager
    private val widgetViews = HashMap<Int, AppWidgetHostView>()
    private var pendingWidget = -1

    private var apps: List<AppEntry> = emptyList()
    private var page = 0
    private var pages = 1
    private var sig = ""

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
        build()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panel != null) hidePanel() else if (page != 0) { page = 0; renderApps() }
            }
        })
        val pf = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try { registerReceiver(pkgReceiver, pf) } catch (e: Exception) { ignore(e) }
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
        refreshAll()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(tick) } catch (e: Exception) { ignore(e) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        hidePanel()
        if (page != 0) { page = 0; renderApps() }
        handleExtras(intent)
    }

    private fun handleExtras(i: Intent?) {
        if (i != null && i.getBooleanExtra("gadgets", false)) {
            i.removeExtra("gadgets")
            root.post { gadgetMenu() }
        }
    }

    /* ------------------------------------------------------------ layout */

    private fun build() {
        root = FrameLayout(this).apply { setBackgroundColor(pal.bg) }
        mainCol = vbox().apply {
            setPadding(dp(14), 0, dp(14), dp(10))
            isLongClickable = true
            setOnLongClickListener { homeMenu(); true }
        }
        val main = mainCol

        // status strip with pull handle
        val strip = FrameLayout(this).apply { setOnClickListener { showPanel() } }
        val leftS = hbox()
        statusWifi = ImageView(this)
        leftS.addView(statusWifi, lp(dp(18), dp(18)))
        strip.addView(leftS, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL))
        val pill = View(this).apply { background = roundBg(pal.sub, dp(3).toFloat()) }
        strip.addView(pill, FrameLayout.LayoutParams(dp(46), dp(5), Gravity.CENTER))
        val rightS = hbox()
        statusBat = label("", 13f, pal.text, true)
        statusBatIcon = ImageView(this)
        rightS.addView(statusBat)
        rightS.addView(statusBatIcon, lp(dp(22), dp(22)).m(dp(4), 0, 0, 0))
        strip.addView(rightS, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL))
        main.addView(strip, lp(MATCH, dp(28)))

        // hero card: greeting, big clock, dates
        val hero = vbox().apply {
            background = Look.heroBg(pal, dp(26).toFloat())
            setPadding(dp(20), dp(14), dp(20), dp(16))
            if (pal.vivid) elevation = dp(4).toFloat()
            setOnClickListener { go(Intent(context, CalendarActivity::class.java)) }
            setOnLongClickListener { homeMenu(); true }
        }
        val top = hbox()
        greetTv = label("", 15f, pal.heroSub, true)
        top.addView(greetTv, lp(0, WRAP, 1f))
        top.addView(ImageView(this).apply { setImageDrawable(Glyph("calendar", pal.heroSub)) }, lp(dp(20), dp(20)))
        hero.addView(top, lp(MATCH, WRAP))
        val mid = hbox().apply { gravity = Gravity.BOTTOM }
        clock = TextView(this).apply {
            setTextColor(pal.heroText)
            textSize = Look.clockSize()
            typeface = Look.heavy
            includeFontPadding = false
            textDirection = View.TEXT_DIRECTION_LTR
            letterSpacing = -0.02f
        }
        mid.addView(clock, lp(WRAP, WRAP))
        mid.addView(View(this), lp(0, 1, 1f))
        val dates = vbox().apply { gravity = Gravity.END }
        hebTv = label("", 20f, pal.heroText, true).oneLine().apply { gravity = Gravity.END }
        gregTv = label("", 13.5f, pal.heroSub).oneLine().apply { gravity = Gravity.END }
        dates.addView(hebTv, lp(WRAP, WRAP))
        dates.addView(gregTv, lp(WRAP, WRAP))
        mid.addView(dates, lp(WRAP, WRAP).m(dp(10), 0, 0, dp(8)))
        hero.addView(mid, lp(MATCH, WRAP).m(0, dp(2), 0, 0))
        eventTv = label("", 13.5f, pal.heroText, true).apply {
            val chipBg = if (pal.hero.size > 1 || pal.night || !pal.pastel) 0x33FFFFFF else 0x22000000
            background = roundBg(if (pal.heroText == Color.WHITE) chipBg else 0x1A000000, dp(12).toFloat())
            setPadding(dp(12), dp(4), dp(12), dp(5))
            visibility = View.GONE
        }
        val evWrap = hbox().apply { addView(eventTv, lp(WRAP, WRAP)) }
        hero.addView(evWrap, lp(MATCH, WRAP).m(0, dp(8), 0, 0))
        main.addView(hero, lp(MATCH, WRAP).m(0, dp(2), 0, 0))

        // setup banner
        banner = vbox().apply { visibility = View.GONE }
        main.addView(banner, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        // gadgets
        gadgetsBox = vbox()
        main.addView(gadgetsBox, lp(MATCH, WRAP))

        // recent books
        booksSection = vbox()
        booksSection.addView(sectionTitle("book", "ספרים אחרונים", "הכול") { go(Intent(this, BooksActivity::class.java)) })
        booksRow = hbox().apply { gravity = Gravity.TOP }
        booksSection.addView(booksRow, lp(MATCH, WRAP))
        main.addView(booksSection, lp(MATCH, WRAP).m(0, dp(12), 0, 0))

        // apps
        val appsHead = hbox()
        appsHead.addView(sectionTitle("grid", "אפליקציות", "הכול") { go(Intent(this, AppsActivity::class.java)) }, lp(0, WRAP, 1f))
        main.addView(appsHead, lp(MATCH, WRAP).m(0, dp(10), 0, 0))
        appsBox = FrameLayout(this)
        appsBox.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) appsBox.post { renderApps() }
        }
        main.addView(appsBox, lp(MATCH, 0, 1f))
        pageInd = hbox().apply { gravity = Gravity.CENTER }
        main.addView(pageInd, lp(MATCH, WRAP))

        // folders
        foldersSection = vbox()
        foldersSection.addView(sectionTitle("folder", "תיקיות", "הוספה") { pickFolder() })
        foldersRow = hbox()
        foldersSection.addView(foldersRow, lp(MATCH, WRAP))
        main.addView(foldersSection, lp(MATCH, WRAP).m(0, dp(4), 0, 0))

        // dock
        dockBox = FrameLayout(this)
        main.addView(dockBox, lp(MATCH, WRAP).m(0, dp(12), 0, 0))

        root.addView(main, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun sectionTitle(glyph: String, t: String, link: String?, onLink: (() -> Unit)?): View {
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
        r.setPadding(0, 0, 0, dp(7))
        return r
    }

    /* ------------------------------------------------------------ dock */

    private fun renderDock() {
        dockBox.removeAllViews()
        val keys = Dock.items()
        if (keys.isEmpty()) { dockBox.visibility = View.GONE; return }
        dockBox.visibility = View.VISIBLE
        val d = hbox().apply {
            background = roundBg(pal.dockBg, dp(30).toFloat(), if (pal.dockBg == pal.card && !pal.night) dp(1) else 0, pal.line)
            if (pal.vivid) elevation = dp(6).toFloat()
            setPadding(dp(6), dp(9), dp(6), dp(9))
        }
        keys.forEachIndexed { i, k ->
            val v = vbox().apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, dp(3))
                background = pressBg(Color.TRANSPARENT, if (pal.dockBg == Color.WHITE) pal.card2 else 0x33FFFFFF, 20)
            }
            if (k.startsWith("app:")) {
                val a = Apps.byKey(k.removePrefix("app:"))
                if (a != null) v.addView(Apps.iconView(this, a, 32, pal, i), lp(dp(32), dp(32)))
                else v.addView(glyphView("dots", pal.dockFg, 30))
            } else v.addView(glyphView(Dock.glyph(k), pal.dockFg, 30))
            v.addView(label(Dock.shortLabel(k), 12f, pal.dockFg, true).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) }, lp(MATCH, WRAP))
            v.setOnClickListener { dockAction(k) }
            v.setOnLongClickListener { dockMenu(); true }
            d.addView(v, lp(0, WRAP, 1f))
        }
        dockBox.addView(d, FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun dockAction(k: String) {
        when (k) {
            "wifi" -> openWifi()
            "files" -> go(Intent(this, FilesActivity::class.java))
            "books" -> go(Intent(this, BooksActivity::class.java))
            "settings" -> go(Intent(this, SettingsActivity::class.java))
            "apps" -> go(Intent(this, AppsActivity::class.java))
            "calendar" -> go(Intent(this, CalendarActivity::class.java))
            "control" -> showPanel()
            "refresh" -> einkRefresh()
            "bt" -> go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            "sd" -> {
                val sd = Store.volumes().firstOrNull { it.removable }
                if (sd == null) toast("לא זוהה כרטיס זיכרון")
                else go(Intent(this, FilesActivity::class.java).putExtra("volume", sd.dir.path))
            }
            "android" -> go(Intent(Settings.ACTION_SETTINGS))
            else -> Apps.byKey(k.removePrefix("app:"))?.let { Apps.launch(this, it) } ?: toast("האפליקציה לא נמצאה")
        }
    }

    fun dockMenu() {
        val opts = Dock.builtIn + Apps.everything().map { "app:" + it.key to "אפליקציה: " + Apps.name(it) }
        Sheet.multi(this, "מה יופיע בשורה התחתונה", opts, Dock.items(), 6) { sel ->
            Dock.save(sel); renderDock()
        }
    }

    /* ------------------------------------------------------------ data */

    private fun refreshAll() {
        updateClock()
        renderBanner()
        reloadApps()
        renderGadgets()
        renderFolders()
        renderBooks()
        renderDock()
    }

    private fun reloadApps() {
        Thread {
            Apps.load(this)
            runOnUiThread { apps = Apps.ordered(); renderApps(); renderDock() }
        }.start()
    }

    private fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> "בוקר טוב"
            in 12..16 -> "צהריים טובים"
            in 17..20 -> "ערב טוב"
            else -> "לילה טוב"
        }
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val h = Heb.of(now)
        clock.visibility = if (Prefs.b("showClock", true)) View.VISIBLE else View.GONE
        clock.text = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))
        greetTv.text = greeting() + " · יום " + Heb.dayNames[h.dow]
        gregTv.visibility = if (Prefs.b("showGreg", true)) View.VISIBLE else View.GONE
        gregTv.text = SimpleDateFormat("d בMMMM yyyy", Locale("he")).format(Date(now))
        hebTv.visibility = if (Prefs.b("showHeb", true)) View.VISIBLE else View.GONE
        hebTv.text = Heb.format(h)
        val ev = ArrayList(Heb.events(h))
        if (h.dow == Heb.SAT) ev.add(0, "שבת שלום")
        val om = Heb.omer(h)
        if (om > 0) ev.add("${Heb.gem(om)} לעומר")
        if (Prefs.b("showEvents", true) && ev.isNotEmpty()) {
            eventTv.text = ev.joinToString(" · ")
            eventTv.visibility = View.VISIBLE
        } else eventTv.visibility = View.GONE
        // status strip
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

    /* ------------------------------------------------------------ books */

    private fun renderBooks() {
        val show = Prefs.b("showBooks", true)
        booksSection.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
        if (!hasFiles()) {
            drawBooks(emptyList(), "לאחר אישור גישה לקבצים יופיעו כאן הספרים")
            return
        }
        drawBooks(Books.recent(4), null)
        Thread {
            Books.scan()
            val r = Books.recent(4)
            runOnUiThread { drawBooks(r, if (r.isEmpty()) "לא נמצאו עדיין ספרים במכשיר או בכרטיס הזיכרון" else null) }
        }.start()
    }

    private fun drawBooks(list: List<File>, emptyMsg: String?) {
        booksRow.removeAllViews()
        for (i in 0 until 4) {
            val f = list.getOrNull(i)
            val cell = vbox()
            val sq = SquareFrame(this).apply {
                background = roundBg(if (pal.pastel && f == null) pal.tile(i) else pal.card, dp(14).toFloat(), if (pal.vivid) 0 else dp(1), pal.line)
                clipToOutline = true
                if (pal.vivid) elevation = dp(3).toFloat()
            }
            if (f != null) {
                val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                Covers.load(this, f, iv)
                if (i == 0) {
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
            } else {
                val g = ImageView(this).apply { setImageDrawable(Glyph("book", if (pal.pastel) pal.ink else pal.line)) }
                sq.addView(g, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))
                if (emptyMsg != null && !hasFiles()) sq.setOnClickListener { askFiles() }
            }
            cell.addView(sq, lp(MATCH, WRAP))
            cell.addView(label(f?.nameWithoutExtension ?: "", 12f, pal.text).oneLine().apply { setPadding(dp(2), dp(5), dp(2), 0) })
            booksRow.addView(cell, lp(0, WRAP, 1f).m(if (i == 0) 0 else dp(5), 0, if (i == 3) 0 else dp(5), 0))
        }
        booksSection.findViewWithTag<View>("bmsg")?.let { booksSection.removeView(it) }
        if (emptyMsg != null && list.isEmpty()) {
            booksSection.addView(label(emptyMsg, 13f, pal.sub).apply { tag = "bmsg" })
        }
    }

    private fun bookMenu(f: File) {
        Sheet.actions(this, f.nameWithoutExtension, listOf(
            Sheet.Item("פתיחה") { Store.open(this, f) },
            Sheet.Item("פתיחה באמצעות…") { Store.open(this, f, chooser = true) },
            Sheet.Item("הצגה בתיקייה") {
                go(Intent(this, FilesActivity::class.java).putExtra("path", f.parent))
            },
            Sheet.Item("כל הספרים") { go(Intent(this, BooksActivity::class.java)) }
        ))
    }

    /* ------------------------------------------------------------ apps */

    private fun cellHeight(): Int {
        val labels = Prefs.b("labels", true)
        return dp(if (labels) (60 + 30 * Look.scale()).toInt() else 76)
    }

    private fun renderApps() {
        val h = appsBox.height
        if (h <= 0) return
        val cols = Prefs.i("cols", 4)
        val rows = maxOf(1, h / cellHeight())
        val per = rows * cols
        pages = maxOf(1, (apps.size + per - 1) / per)
        page = page.coerceIn(0, pages - 1)
        appsBox.removeAllViews()
        val grid = vbox()
        val iconSize = if (cols >= 5) 50 else if (cols == 3) 64 else 56
        for (r in 0 until rows) {
            val row = hbox().apply { gravity = Gravity.TOP }
            for (c in 0 until cols) {
                val idx = page * per + r * cols + c
                val a = apps.getOrNull(idx)
                if (a == null) { row.addView(View(this), lp(0, MATCH, 1f)); continue }
                row.addView(appCell(a, idx, iconSize), lp(0, MATCH, 1f))
            }
            grid.addView(row, lp(MATCH, 0, 1f))
        }
        if (apps.isEmpty()) {
            appsBox.addView(label("טוען אפליקציות…", 15f, pal.sub).apply { gravity = Gravity.CENTER },
                FrameLayout.LayoutParams(MATCH, MATCH))
        } else appsBox.addView(grid, FrameLayout.LayoutParams(MATCH, MATCH))
        renderIndicator()
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

    private fun renderIndicator() {
        pageInd.removeAllViews()
        if (pages <= 1) { pageInd.visibility = View.GONE; return }
        pageInd.visibility = View.VISIBLE
        val prev = hbox().apply {
            addView(glyphView("chev_r", if (page > 0) pal.ink else pal.line, 20))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = pressBg(Color.TRANSPARENT, pal.card2, 14)
            setOnClickListener { if (page > 0) { page--; renderApps() } }
        }
        pageInd.addView(prev)
        for (i in 0 until pages) {
            val dot = View(this).apply {
                background = if (i == page) roundBg(pal.ink, dp(4).toFloat())
                else roundBg(Color.TRANSPARENT, dp(4).toFloat(), dp(1), pal.sub)
            }
            pageInd.addView(dot, lp(dp(if (i == page) 18 else 8), dp(8)).m(dp(3), 0, dp(3), 0))
        }
        pageInd.addView(label("  ${page + 1}/$pages", 13f, pal.sub, true))
        val next = hbox().apply {
            addView(glyphView("chev_l", if (page < pages - 1) pal.ink else pal.line, 20))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = pressBg(Color.TRANSPARENT, pal.card2, 14)
            setOnClickListener { if (page < pages - 1) { page++; renderApps() } }
        }
        pageInd.addView(next)
    }

    private fun moveApp(a: AppEntry, to: Int) {
        val l = apps.toMutableList()
        val from = l.indexOfFirst { it.key == a.key }
        if (from < 0) return
        l.removeAt(from)
        l.add(to.coerceIn(0, l.size), a)
        Apps.saveOrder(l)
        apps = l
        renderApps()
    }

    private fun appMenu(a: AppEntry) {
        val pos = apps.indexOfFirst { it.key == a.key }
        Sheet.actions(this, Apps.name(a), listOf(
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם חדש למסך הבית", Apps.name(a), a.label) { s ->
                    Prefs.put("name:" + a.key, if (s.isEmpty() || s == a.label) null else s)
                    apps = Apps.ordered(); renderApps()
                }
            },
            Sheet.Item("שינוי סמל") { iconMenu(a) },
            Sheet.Item("הזזה קדימה") { moveApp(a, pos - 1) },
            Sheet.Item("הזזה אחורה") { moveApp(a, pos + 1) },
            Sheet.Item("העברה לתחילה") { moveApp(a, 0) },
            Sheet.Item("העברה לסוף") { moveApp(a, apps.size) },
            Sheet.Item("הוספה לשורה התחתונה") { Dock.addApp(a.key); renderDock() },
            Sheet.Item("הסרה ממסך הבית") {
                Apps.hide(a); apps = Apps.ordered(); renderApps()
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
            Prefs.put("icon:" + a.key, v); renderApps(); renderDock()
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

    /* ------------------------------------------------------------ home menu */

    private fun homeMenu() {
        Sheet.actions(this, "מסך הבית", listOf(
            Sheet.Item("הוספת גאדג׳ט") { gadgetPicker() },
            Sheet.Item("ניהול גאדג׳טים") { gadgetMenu() },
            Sheet.Item("ערכת צבעים") {
                Sheet.choice(this, "ערכת צבעים", listOf(
                    "bw" to "שחור‑לבן", "vivid" to "חי וצבעוני", "pastel" to "פסטל", "night" to "לילה"
                ), Look.theme) { v -> Prefs.put("theme", v); Apps.clearIcons(); recreate() }
            },
            Sheet.Item("שורה תחתונה") { dockMenu() },
            Sheet.Item("אפליקציות במסך הבית") { go(Intent(this, AppsActivity::class.java).putExtra("manage", true)) },
            Sheet.Item("הגדרות") { go(Intent(this, SettingsActivity::class.java)) }
        ))
    }

    /* ------------------------------------------------------------ gadgets */

    private fun gadgetList(): MutableList<String> = Prefs.list("gadgets")

    private fun addGadget(k: String) {
        val l = gadgetList(); if (k !in l) l.add(k); Prefs.putList("gadgets", l); renderGadgets()
    }

    private fun removeGadget(k: String) {
        val l = gadgetList(); l.remove(k); Prefs.putList("gadgets", l)
        if (k.startsWith("w:")) {
            val id = k.removePrefix("w:").toIntOrNull()
            if (id != null) { try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }; widgetViews.remove(id) }
        }
        renderGadgets()
    }

    private fun gadgetName(k: String): String = when (k) {
        "g:calendar" -> "לוח חודשי"
        "g:battery" -> "סוללה ואחסון"
        "g:note" -> "פתק"
        "g:book" -> "הספר שאני קורא"
        "g:clock" -> "שעון ענק"
        else -> {
            val id = k.removePrefix("w:").toIntOrNull()
            val info = id?.let { awm.getAppWidgetInfo(it) }
            info?.loadLabel(packageManager) ?: "גאדג׳ט"
        }
    }

    private fun gadgetPicker() {
        val items = ArrayList<Sheet.Item>()
        val internal = listOf("g:calendar", "g:battery", "g:note", "g:book", "g:clock")
        for (k in internal) items.add(Sheet.Item("הבוקס · " + gadgetName(k)) { addGadget(k) })
        val providers = try { awm.installedProviders } catch (e: Exception) { emptyList<AppWidgetProviderInfo>() }
        for (p in providers.sortedBy { it.loadLabel(packageManager) ?: "" }) {
            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(p.provider.packageName, 0)).toString()
            } catch (e: Exception) { p.provider.packageName }
            items.add(Sheet.Item(appName + " · " + (p.loadLabel(packageManager) ?: ""), false) { addAndroidWidget(p) })
        }
        Sheet.actions(this, "בחירת גאדג׳ט", items)
    }

    private fun addAndroidWidget(p: AppWidgetProviderInfo) {
        val id = try { host.allocateAppWidgetId() } catch (e: Exception) { toast("לא ניתן להוסיף גאדג׳ט"); return }
        pendingWidget = id
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
        addGadget("w:$id")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget) ?: pendingWidget
        if (requestCode == REQ_BIND) {
            if (resultCode == RESULT_OK) configureWidget(id) else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        } else if (requestCode == REQ_CONF) {
            if (resultCode == RESULT_OK) addGadget("w:$id") else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        }
    }

    private fun gadgetMenu() {
        val l = gadgetList()
        if (l.isEmpty()) {
            Sheet.confirm(this, "אין עדיין גאדג׳טים", "גאדג׳טים הם חלונות קטנים במסך הבית: לוח שנה, סוללה, פתק, הספר שאתם קוראים או גאדג׳טים של אפליקציות אחרות.", "הוספת גאדג׳ט") { gadgetPicker() }
            return
        }
        val items = ArrayList<Sheet.Item>()
        items.add(Sheet.Item("הוספת גאדג׳ט…") { gadgetPicker() })
        l.forEachIndexed { i, k ->
            items.add(Sheet.Item(gadgetName(k) + " · אפשרויות") { oneGadgetMenu(k, i) })
        }
        Sheet.actions(this, "גאדג׳טים במסך הבית", items)
    }

    private fun oneGadgetMenu(k: String, i: Int) {
        val l = gadgetList()
        val items = ArrayList<Sheet.Item>()
        if (i > 0) items.add(Sheet.Item("הזזה למעלה") { l.removeAt(i); l.add(i - 1, k); Prefs.putList("gadgets", l); renderGadgets() })
        if (i < l.size - 1) items.add(Sheet.Item("הזזה למטה") { l.removeAt(i); l.add(i + 1, k); Prefs.putList("gadgets", l); renderGadgets() })
        if (k == "g:note") items.add(Sheet.Item("עריכת הפתק") { editNote() })
        items.add(Sheet.Item("הסרה ממסך הבית", red = true) { removeGadget(k) })
        Sheet.actions(this, gadgetName(k), items)
    }

    private fun gadgetCard(k: String, i: Int): LinearLayout = vbox().apply {
        background = roundBg(if (pal.pastel && !pal.vivid) pal.tile(i + 4) else pal.card, dp(20).toFloat(),
            if (pal.vivid || pal.pastel || pal.night) 0 else dp(1), pal.line)
        if (pal.vivid) elevation = dp(3).toFloat()
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnLongClickListener { oneGadgetMenu(k, i); true }
    }

    private fun renderGadgets() {
        gadgetsBox.removeAllViews()
        val l = gadgetList()
        gadgetsBox.visibility = if (l.isEmpty()) View.GONE else View.VISIBLE
        l.forEachIndexed { i, k ->
            val v: View? = when (k) {
                "g:calendar" -> miniCalendar(k, i)
                "g:battery" -> batteryGadget(k, i)
                "g:note" -> noteGadget(k, i)
                "g:book" -> bookGadget(k, i)
                "g:clock" -> clockGadget(k, i)
                else -> androidWidget(k)
            }
            if (v != null) gadgetsBox.addView(v, lp(MATCH, WRAP).m(0, dp(10), 0, 0))
        }
    }

    private fun androidWidget(k: String): View? {
        val id = k.removePrefix("w:").toIntOrNull() ?: return null
        val info = awm.getAppWidgetInfo(id) ?: return null
        val hv = widgetViews[id] ?: try {
            host.createView(applicationContext, id, info).also { it.setAppWidget(id, info); widgetViews[id] = it }
        } catch (e: Exception) { return null }
        (hv.parent as? android.view.ViewGroup)?.removeView(hv)
        val card = FrameLayout(this).apply {
            background = roundBg(pal.card, dp(20).toFloat(), if (pal.night) 0 else dp(1), pal.line)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipToOutline = true
        }
        val h = info.minHeight.coerceIn(dp(70), dp(240))
        card.addView(hv, FrameLayout.LayoutParams(MATCH, h))
        return card
    }

    private fun miniCalendar(k: String, i: Int): View {
        val c = gadgetCard(k, i)
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
                val t = label(day.toString(), 12.5f, if (day == today) pal.onInk else pal.text, day == today || col == 6).apply {
                    gravity = Gravity.CENTER
                    if (day == today) background = roundBg(pal.ink, dp(11).toFloat())
                }
                row.addView(t, lp(0, dp(22), 1f).m(dp(3), dp(1), dp(3), dp(1)))
                day++
            }
            c.addView(row, lp(MATCH, WRAP))
        }
        return c
    }

    private fun batteryGadget(k: String, i: Int): View {
        val c = gadgetCard(k, i)
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
        Sheet.input(this, "פתק", Prefs.s("note", ""), "מה לזכור?", multiline = true) { s -> Prefs.put("note", s); renderGadgets() }
    }

    private fun noteGadget(k: String, i: Int): View {
        val c = gadgetCard(k, i)
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

    private fun bookGadget(k: String, i: Int): View {
        val c = gadgetCard(k, i)
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

    private fun clockGadget(k: String, i: Int): View {
        val c = gadgetCard(k, i).apply { gravity = Gravity.CENTER_HORIZONTAL }
        c.addView(TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            textSize = 92f; typeface = Look.heavy; setTextColor(pal.text); includeFontPadding = false
            gravity = Gravity.CENTER
        }, lp(MATCH, WRAP))
        c.addView(label(Heb.format(Heb.of(System.currentTimeMillis())), 16f, pal.sub, true).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP))
        return c
    }

    /* ------------------------------------------------------------ folders */

    private fun renderFolders() {
        val show = Prefs.b("showFolders", true)
        foldersSection.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
        foldersRow.removeAllViews()
        val all = Store.homeFolders()
        val visible = if (all.size > 4) all.take(3) else all
        visible.forEachIndexed { i, path -> foldersRow.addView(folderTile(path, i), lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0)) }
        if (all.size > 4) {
            foldersRow.addView(tile("dots", "עוד ${all.size - 3}", 3) { moreFolders(all) }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
        } else if (all.size < 4) {
            foldersRow.addView(tile("plus", "הוספה", all.size) { pickFolder() }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
            for (x in all.size + 1 until 4) foldersRow.addView(View(this), lp(0, 1, 1f).m(dp(4), 0, dp(4), 0))
        }
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

    private fun folderTile(path: String, i: Int): View {
        val isVol = Store.isVolumeRoot(path) && path != Store.root().path
        val t = tile(if (isVol) "sd" else "folder", Store.folderName(path), i) {
            if (isVol) go(Intent(this, FilesActivity::class.java).putExtra("volume", path))
            else go(Intent(this, FilesActivity::class.java).putExtra("path", path))
        }
        t.setOnLongClickListener { folderMenu(path); true }
        return t
    }

    private fun folderMenu(path: String) {
        Sheet.actions(this, Store.folderName(path), listOf(
            Sheet.Item("פתיחה") { go(Intent(this, FilesActivity::class.java).putExtra("path", path)) },
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם התיקייה במסך הבית", Store.folderName(path), File(path).name) { s ->
                    Prefs.put("fname:$path", if (s.isEmpty()) null else s); renderFolders()
                }
            },
            Sheet.Item("הסרה ממסך הבית", red = true) { Store.removeHomeFolder(path); renderFolders() }
        ))
    }

    private fun moreFolders(all: List<String>) {
        Sheet.actions(this, "תיקיות", all.map { p ->
            Sheet.Item(Store.folderName(p)) { go(Intent(this, FilesActivity::class.java).putExtra("path", p)) }
        } + Sheet.Item("הוספת תיקייה…") { pickFolder() })
    }

    private fun pickFolder() {
        if (!hasFiles()) { askFiles(); return }
        go(Intent(this, FilesActivity::class.java).putExtra("pick", true))
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
                    } else if (dy > th && abs(dy) > 1.4f * abs(dx) && !inWidget(downX, downY)) {
                        showPanel(); handled = true
                    } else if (abs(dx) > th && abs(dx) > 1.4f * abs(dy) && inView(appsBox, downY)) {
                        if (dx > 0 && page < pages - 1) { page++; renderApps() }
                        else if (dx < 0 && page > 0) { page--; renderApps() }
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
            if (hv.parent == null) continue
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
        Sheet.confirm(this, what, "כדי להשתמש בכפתור הזה יש להפעיל את השירות \"הבוקס – פעולות מסך\" בהגדרות הנגישות. השירות לא קורא שום תוכן מהמסך.", "פתיחת הגדרות נגישות") {
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
                Sheet.confirm(this, "מעבר למסך הבית הרגיל", "בחרו במסך הבית של BOOX כברירת מחדל. אפשר לחזור להבוקס בכל עת מאותו מקום.", "בחירת מסך בית") {
                    go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                }
            },
            T("calendar", "לוח שנה") { hidePanel(); go(Intent(this, CalendarActivity::class.java)) },
            T("widget", "גאדג׳טים") { hidePanel(); gadgetPicker() },
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
        val cur = try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }
        val seek = SeekBar(this).apply {
            max = 255
            progress = cur
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
        val close = FrameLayout(this).apply {
            val pill = View(context).apply { background = roundBg(pal.sub, dp(3).toFloat()) }
            addView(pill, FrameLayout.LayoutParams(dp(54), dp(5), Gravity.CENTER))
            setOnClickListener { hidePanel() }
        }
        sheet.addView(close, lp(MATCH, dp(30)).m(0, dp(8), 0, 0))

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
