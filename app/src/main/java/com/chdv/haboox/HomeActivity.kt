package com.chdv.haboox

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
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
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class HomeActivity : BaseActivity() {

    private lateinit var root: FrameLayout
    private lateinit var clock: TextView
    private lateinit var gregTv: TextView
    private lateinit var hebTv: TextView
    private lateinit var eventTv: TextView
    private lateinit var banner: LinearLayout
    private lateinit var booksSection: LinearLayout
    private lateinit var booksRow: LinearLayout
    private lateinit var appsBox: FrameLayout
    private lateinit var pageInd: LinearLayout
    private lateinit var foldersSection: LinearLayout
    private lateinit var foldersRow: LinearLayout
    private var panel: View? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sig = Look.sig()
        build()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panel != null) hidePanel() else if (page != 0) { page = 0; renderApps() }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (Look.sig() != sig) { recreate(); return }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
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
    }

    /* ------------------------------------------------------------ layout */

    private fun build() {
        root = FrameLayout(this).apply { setBackgroundColor(pal.bg) }
        val main = vbox().apply { setPadding(dp(16), dp(2), dp(16), dp(10)) }

        // pull handle
        val handle = FrameLayout(this).apply {
            val pill = View(context).apply { background = roundBg(pal.line, dp(3).toFloat()) }
            addView(pill, FrameLayout.LayoutParams(dp(44), dp(5), Gravity.CENTER))
            setOnClickListener { showPanel() }
        }
        main.addView(handle, lp(MATCH, dp(20)))

        // header: clock + dates
        val header = hbox().apply {
            gravity = Gravity.BOTTOM
            setOnClickListener { go(Intent(context, CalendarActivity::class.java)) }
        }
        val dates = vbox()
        gregTv = label("", 15f, pal.sub).oneLine()
        hebTv = label("", 21f, pal.text, true).oneLine()
        dates.addView(gregTv)
        dates.addView(hebTv)
        header.addView(dates, lp(0, WRAP, 1f))
        clock = label("", 46f, pal.text, true).apply {
            includeFontPadding = false
            textDirection = View.TEXT_DIRECTION_LTR
        }
        header.addView(clock, lp(WRAP, WRAP))
        main.addView(header, lp(MATCH, WRAP))

        eventTv = label("", 13.5f, pal.onInk, true).apply {
            background = roundBg(pal.ink, dp(12).toFloat())
            setPadding(dp(12), dp(4), dp(12), dp(5))
            visibility = View.GONE
        }
        val evWrap = hbox().apply { addView(eventTv, lp(WRAP, WRAP)) }
        main.addView(evWrap, lp(MATCH, WRAP).m(0, dp(6), 0, 0))

        // setup banner
        banner = vbox().apply { visibility = View.GONE }
        main.addView(banner, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        // recent books
        booksSection = vbox()
        booksSection.addView(sectionTitle("ספרים אחרונים", "הכול") { go(Intent(this, BooksActivity::class.java)) })
        booksRow = hbox().apply { gravity = Gravity.TOP }
        booksSection.addView(booksRow, lp(MATCH, WRAP))
        main.addView(booksSection, lp(MATCH, WRAP).m(0, dp(10), 0, 0))

        // apps
        val appsHead = hbox()
        appsHead.addView(label("אפליקציות", 17f, pal.text, true), lp(0, WRAP, 1f))
        pageInd = hbox()
        appsHead.addView(pageInd, lp(WRAP, dp(30)))
        main.addView(appsHead, lp(MATCH, WRAP).m(0, dp(14), 0, dp(4)))
        appsBox = FrameLayout(this)
        appsBox.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) appsBox.post { renderApps() }
        }
        main.addView(appsBox, lp(MATCH, 0, 1f))

        // folders
        foldersSection = vbox()
        foldersSection.addView(sectionTitle("תיקיות", "הוספה") { pickFolder() })
        foldersRow = hbox()
        foldersSection.addView(foldersRow, lp(MATCH, WRAP))
        main.addView(foldersSection, lp(MATCH, WRAP).m(0, dp(6), 0, 0))

        // dock
        main.addView(dock(), lp(MATCH, WRAP).m(0, dp(12), 0, 0))

        root.addView(main, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun sectionTitle(t: String, link: String?, onLink: (() -> Unit)?): View {
        val r = hbox()
        r.addView(label(t, 17f, pal.text, true), lp(0, WRAP, 1f))
        if (link != null && onLink != null) {
            val l = hbox().apply {
                addView(label(link, 15f, pal.sub))
                addView(glyphView("chev_l", pal.sub, 16))
                setPadding(dp(8), dp(4), dp(4), dp(4))
                background = pressBg(Color.TRANSPARENT, pal.card2, 12)
                setOnClickListener { onLink() }
            }
            r.addView(l, lp(WRAP, WRAP))
        }
        r.setPadding(0, 0, 0, dp(6))
        return r
    }

    private fun dock(): View {
        val d = hbox().apply {
            background = roundBg(pal.card, dp(28).toFloat(), if (pal.pastel) 0 else dp(1), pal.line)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }
        fun item(kind: String, text: String, f: () -> Unit) {
            val v = vbox().apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, dp(4))
                background = pressBg(Color.TRANSPARENT, pal.card2, 20)
                addView(glyphView(kind, pal.ink, 30))
                addView(label(text, 12.5f, pal.text).apply { setPadding(0, dp(3), 0, 0) })
                setOnClickListener { f() }
            }
            d.addView(v, lp(0, WRAP, 1f))
        }
        item("wifi", "Wi‑Fi") { openWifi() }
        item("folder", "קבצים") { go(Intent(this, FilesActivity::class.java)) }
        item("book", "ספרים") { go(Intent(this, BooksActivity::class.java)) }
        item("gear", "הגדרות") { go(Intent(this, SettingsActivity::class.java)) }
        return d
    }

    /* ------------------------------------------------------------ data */

    private fun refreshAll() {
        updateClock()
        renderBanner()
        Thread {
            Apps.load(this)
            runOnUiThread { apps = Apps.ordered(); renderApps() }
        }.start()
        renderFolders()
        renderBooks()
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val showClock = Prefs.b("showClock", true)
        clock.visibility = if (showClock) View.VISIBLE else View.GONE
        clock.text = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))
        gregTv.visibility = if (Prefs.b("showGreg", true)) View.VISIBLE else View.GONE
        gregTv.text = Heb.gregLong(now)
        val h = Heb.of(now)
        hebTv.visibility = if (Prefs.b("showHeb", true)) View.VISIBLE else View.GONE
        hebTv.text = Heb.format(h)
        val ev = ArrayList(Heb.events(h))
        val om = Heb.omer(h)
        if (om > 0) ev.add("${Heb.gem(om)} לעומר")
        if (Prefs.b("showEvents", true) && ev.isNotEmpty()) {
            eventTv.text = ev.joinToString(" · ")
            eventTv.visibility = View.VISIBLE
        } else eventTv.visibility = View.GONE
    }

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
        if (needFiles) parts.add("גישה לקבצים (לספרים ולסייר)")
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
            runOnUiThread { drawBooks(r, if (r.isEmpty()) "לא נמצאו עדיין ספרים במכשיר" else null) }
        }.start()
    }

    private fun drawBooks(list: List<File>, emptyMsg: String?) {
        booksRow.removeAllViews()
        for (i in 0 until 4) {
            val f = list.getOrNull(i)
            val cell = vbox()
            val sq = SquareFrame(this).apply {
                background = roundBg(pal.card, dp(12).toFloat(), dp(1), pal.line)
                clipToOutline = true
            }
            if (f != null) {
                val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                Covers.load(this, f, iv)
                sq.setOnClickListener { Store.open(this, f) }
                sq.setOnLongClickListener { bookMenu(f); true }
            } else {
                val g = ImageView(this).apply { setImageDrawable(Glyph("book", pal.line)) }
                sq.addView(g, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))
                if (emptyMsg != null && !hasFiles()) sq.setOnClickListener { askFiles() }
            }
            cell.addView(sq, lp(MATCH, WRAP))
            val name = if (f != null) f.nameWithoutExtension else if (i == 0 && emptyMsg != null) "" else ""
            cell.addView(label(name, 12f, pal.text).oneLine().apply { setPadding(dp(2), dp(4), dp(2), 0) })
            booksRow.addView(cell, lp(0, WRAP, 1f).m(if (i == 0) 0 else dp(5), 0, if (i == 3) 0 else dp(5), 0))
        }
        if (emptyMsg != null && list.isEmpty()) {
            (booksRow.getChildAt(0) as? LinearLayout)?.let { ignore(null) }
            booksSection.findViewWithTag<View>("bmsg")?.let { booksSection.removeView(it) }
            booksSection.addView(label(emptyMsg, 13f, pal.sub).apply { tag = "bmsg" })
        } else {
            booksSection.findViewWithTag<View>("bmsg")?.let { booksSection.removeView(it) }
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
        return dp(if (labels) (58 + 30 * Look.scale()).toInt() else 74)
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
        if (pages <= 1) return
        pageInd.addView(iconButton("chev_r") { if (page > 0) { page--; renderApps() } }.apply {
            layoutParams = lp(dp(34), dp(30))
        })
        for (i in 0 until pages) {
            val dot = View(this).apply {
                background = if (i == page) roundBg(pal.ink, dp(4).toFloat())
                else roundBg(Color.TRANSPARENT, dp(4).toFloat(), dp(1), pal.sub)
            }
            pageInd.addView(dot, lp(dp(if (i == page) 16 else 8), dp(8)).m(dp(3), 0, dp(3), 0))
        }
        pageInd.addView(iconButton("chev_l") { if (page < pages - 1) { page++; renderApps() } }.apply {
            layoutParams = lp(dp(34), dp(30))
        })
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
            Sheet.Item("הסרה ממסך הבית") {
                Apps.hide(a); apps = Apps.ordered(); renderApps()
                toast("אפשר להחזיר מההגדרות ← מסך הבית ← אפליקציות מוסתרות")
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
            Prefs.put("icon:" + a.key, v); renderApps()
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
            for (k in all.size + 1 until 4) foldersRow.addView(View(this), lp(0, 1, 1f).m(dp(4), 0, dp(4), 0))
        }
    }

    private fun tile(kind: String, text: String, i: Int, onClick: () -> Unit): LinearLayout = vbox().apply {
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(10), dp(4), dp(8))
        val bgc = if (pal.pastel) pal.tile(i + 2) else pal.card
        background = pressBg(bgc, pal.card2, 16)
        addView(glyphView(kind, pal.ink, 28))
        addView(label(text, 12.5f, pal.text).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }, lp(MATCH, WRAP))
        setOnClickListener { onClick() }
    }

    private fun folderTile(path: String, i: Int): View {
        val t = tile("folder", Store.folderName(path), i) {
            go(Intent(this, FilesActivity::class.java).putExtra("path", path))
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
                    } else if (dy > th && abs(dy) > 1.4f * abs(dx)) {
                        showPanel(); handled = true
                    } else if (abs(dx) > th && abs(dx) > 1.4f * abs(dy) && inApps(downY)) {
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

    private fun inApps(rawY: Float): Boolean {
        val loc = IntArray(2)
        appsBox.getLocationOnScreen(loc)
        return rawY >= loc[1] && rawY <= loc[1] + appsBox.height
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
            setBackgroundColor(if (pal.pastel) 0xCCFBF8F3.toInt() else 0xDDFFFFFF.toInt())
            setOnClickListener { hidePanel() }
            isClickable = true
        }
        val sheet = vbox().apply {
            background = roundBg(pal.bg, dp(26).toFloat(), dp(2), pal.ink)
            setPadding(dp(16), dp(14), dp(16), dp(10))
            isClickable = true
        }
        // header
        val head = hbox()
        val hv = vbox()
        hv.addView(label("מרכז בקרה", 22f, pal.text, true))
        val bat = batteryLevel()
        hv.addView(label(SimpleDateFormat("HH:mm", Locale.US).format(Date()) + (if (bat >= 0) "  ·  סוללה $bat%" else ""), 14f, pal.sub))
        head.addView(hv, lp(0, WRAP, 1f))
        head.addView(iconButton("close") { hidePanel() })
        sheet.addView(head, lp(MATCH, WRAP))

        // tiles
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
            T("folder", "קבצים") { hidePanel(); go(Intent(this, FilesActivity::class.java)) },
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

        // brightness
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
        // close handle
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
