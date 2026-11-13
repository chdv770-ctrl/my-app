package com.chdv.haboox

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : BaseActivity() {

    private lateinit var list: LinearLayout
    private var iconIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = vbox().apply { setPadding(dp(16), 0, dp(16), dp(28)) }
        val sv = ScrollView(this).apply { addView(list) }
        screen("הגדרות", sv)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun restartLook() {
        Apps.clearIcons()
        recreate()
    }

    /* ------------------------------------------------------------ row builders */

    private var group: LinearLayout? = null

    private fun header(t: String) {
        list.addView(label(t, 13f, pal.sub, true).apply { setPadding(dp(14), dp(22), dp(14), dp(7)) })
        group = vbox().apply { background = roundBg(pal.card, dp(16).toFloat()) }
        list.addView(group, lp(MATCH, WRAP))
    }

    private fun iconTile(kind: String): View {
        val i = iconIndex++
        val f = android.widget.FrameLayout(this)
        f.background = roundBg(if (pal.pastel) pal.tile(i) else pal.ink, dp(8).toFloat())
        f.addView(ImageView(this).apply { setImageDrawable(Glyph(kind, if (pal.pastel) pal.ink else pal.onInk, 2f)) },
            android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        f.layoutParams = lp(dp(32), dp(32))
        return f
    }

    private fun addRow(v: View) {
        val g = group ?: return
        if (g.childCount > 0) {
            g.addView(View(this).apply { setBackgroundColor(pal.line) }, lp(MATCH, maxOf(1, dp(0.7f))).m(dp(58), 0, 0, 0))
        }
        g.addView(v, lp(MATCH, WRAP))
    }

    private fun baseRow(kind: String, title: String): LinearLayout = hbox().apply {
        setPadding(dp(12), dp(10), dp(12), dp(10))
        minimumHeight = dp(54)
        addView(iconTile(kind))
        addView(label(title, 16.5f, pal.text), lp(0, WRAP, 1f).m(dp(14), 0, dp(8), 0))
    }

    private fun row(kind: String, title: String, value: String?, onClick: () -> Unit) {
        val r = baseRow(kind, title)
        if (value != null) r.addView(label(value, 15f, pal.sub).oneLine().apply { maxWidth = dp(170) })
        r.addView(glyphView("chev_l", pal.sub, 18).apply { (layoutParams as LinearLayout.LayoutParams).setMargins(dp(6), 0, 0, 0) })
        r.background = pressBg(Color.TRANSPARENT, pal.card2, 16)
        r.setOnClickListener { onClick() }
        addRow(r)
    }

    private fun toggle(kind: String, title: String, key: String, def: Boolean, after: (() -> Unit)? = null) {
        val r = baseRow(kind, title)
        val sw = SwitchCompat(this).apply {
            isChecked = Prefs.b(key, def)
            setOnCheckedChangeListener { _, v -> Prefs.put(key, v); after?.invoke() }
        }
        r.addView(sw)
        r.setOnClickListener { sw.toggle() }
        addRow(r)
    }

    private fun choice(kind: String, title: String, key: String, def: String, opts: List<Pair<String, String>>, after: (() -> Unit)? = null) {
        val cur = Prefs.s(key, def)
        val curLabel = opts.firstOrNull { it.first == cur }?.second ?: ""
        row(kind, title, curLabel) {
            Sheet.choice(this, title, opts, cur) { v -> Prefs.put(key, v); if (after != null) after() else build() }
        }
    }

    /* ------------------------------------------------------------ content */

    private fun build() {
        list.removeAllViews()
        iconIndex = 0

        // hero card
        val hero = hbox().apply {
            background = roundBg(pal.card, dp(18).toFloat())
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        hero.addView(ImageView(this).apply { setImageResource(R.drawable.logo_big) }, lp(dp(64), dp(64)))
        val hv = vbox()
        hv.addView(label("הבוקס", 22f, pal.text, true))
        hv.addView(label("מסך בית ל‑BOOX Go 6 · גרסה " + version(), 14f, pal.sub))
        hero.addView(hv, lp(0, WRAP, 1f).m(dp(14), 0, 0, 0))
        list.addView(hero, lp(MATCH, WRAP).m(0, dp(4), 0, 0))

        // display
        header("תצוגה")
        choice("palette", "ערכת צבעים", "theme", "bw", listOf("bw" to "שחור‑לבן", "pastel" to "פסטל")) { restartLook() }
        choice("text", "גודל טקסט", "fontSize", "normal", listOf("normal" to "רגיל", "large" to "גדול", "xlarge" to "גדול מאוד")) { restartLook() }
        toggle("edit", "טקסט מודגש", "boldText", false) { restartLook() }
        toggle("grid", "סמלים בגווני אפור", "grayIcons", !Look.pastel) { Apps.clearIcons() }

        // home screen
        header("מסך הבית")
        toggle("calendar", "שעון", "showClock", true)
        toggle("calendar", "תאריך עברי", "showHeb", true)
        toggle("calendar", "תאריך לועזי", "showGreg", true)
        toggle("star", "חגים, מועדים וספירת העומר", "showEvents", true)
        toggle("book", "ספרים אחרונים", "showBooks", true)
        toggle("folder", "תיקיות", "showFolders", true)
        toggle("text", "שמות מתחת לסמלים", "labels", true)
        choice("grid", "עמודות אפליקציות", "colsS", Prefs.i("cols", 4).toString(), listOf("3" to "3", "4" to "4", "5" to "5")) {
            Prefs.put("cols", Prefs.s("colsS", "4").toInt()); build()
        }
        val hidden = Prefs.list("hidden")
        row("hide", "אפליקציות מוסתרות", if (hidden.isEmpty()) "אין" else hidden.size.toString()) { hiddenApps() }
        row("folder", "תיקיות במסך הבית", Store.homeFolders().size.toString()) { manageFolders() }
        row("refresh", "איפוס שמות, סמלים וסדר", null) {
            Sheet.confirm(this, "איפוס", "כל השמות, הסמלים והסדר שהגדרתם לאפליקציות יחזרו למצב המקורי.", "איפוס") {
                for (k in Prefs.keys()) if (k.startsWith("name:") || k.startsWith("icon:")) Prefs.put(k, null)
                Prefs.put("order", null)
                toast("בוצע איפוס")
                build()
            }
        }

        // books & files
        header("ספרים וקבצים")
        row("book", "אפליקציית קריאה", readerName()) { pickReader() }
        choice("sort", "מיון קבצים", "sort", "name", listOf("name" to "שם", "date" to "תאריך", "type" to "סוג", "size" to "גודל"))
        toggle("hide", "הצגת קבצים מוסתרים", "showHidden", false)
        row("search", "סריקת ספרים מחדש", null) {
            Thread { Books.scan(true); runOnUiThread { toast("נמצאו ${Books.cache.size} ספרים") } }.start()
        }
        row("image", "ניקוי מטמון כריכות", null) { Covers.clear(this); toast("המטמון נוקה") }
        row("trash", "ניקוי היסטוריית קריאה", null) {
            Sheet.confirm(this, "ניקוי היסטוריה", "רשימת הספרים שנפתחו לאחרונה תימחק. הספרים עצמם לא יימחקו.", "ניקוי") {
                Books.clearHistory(); toast("ההיסטוריה נוקתה")
            }
        }

        // system
        header("מערכת")
        row("folder", "גישה לכל הקבצים", if (hasFiles()) "מאושר" else "נדרש אישור") { askFiles() }
        row("home", "מסך הבית של המכשיר", if (isDefaultHome()) "הבוקס" else "לא הבוקס") {
            go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        }
        row("windows", "שירות פעולות (חזרה, חלונות)", if (ActionService.enabled(this)) "פעיל" else "כבוי") {
            go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        row("sun", "הרשאת שינוי בהירות", if (Settings.System.canWrite(this)) "מאושר" else "נדרש אישור") {
            go(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
        }
        row("info", "הבוקס בהגדרות אנדרואיד", null) {
            go(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        // separate Android settings button
        val androidBtn = pillButton("פתיחת הגדרות אנדרואיד", filled = true) { go(Intent(Settings.ACTION_SETTINGS)) }
        list.addView(androidBtn, lp(MATCH, WRAP).m(0, dp(14), 0, 0))

        // about
        header("אודות")
        row("info", "גרסה", version()) { }
        row("edit", "פותח", "שניאור וילהלם") { }
        row("screen", "מותאם ל", "BOOX Go 6 · E‑ink") { }
        row("calendar", "לוח עברי", "אופליין, ללא אינטרנט") { }

        val foot = vbox().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(26), 0, 0) }
        foot.addView(ImageView(this).apply { setImageResource(R.drawable.logo_big) }, lp(dp(84), dp(84)))
        foot.addView(label("הבוקס", 18f, pal.text, true).apply { setPadding(0, dp(8), 0, 0) })
        foot.addView(label("נבנה באהבה לקוראים", 13f, pal.sub))
        list.addView(foot, lp(MATCH, WRAP))
    }

    private fun version(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (e: Exception) { "" }

    private fun isDefaultHome(): Boolean {
        val r = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
        )
        return r?.activityInfo?.packageName == packageName
    }

    private fun readers(): List<Pair<String, String>> {
        val map = LinkedHashMap<String, String>()
        val probe = Uri.parse("content://$packageName.files/storage/a")
        for (m in listOf("application/pdf", "application/epub+zip", "application/x-mobipocket-ebook")) {
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(probe, m)
            for (ri in packageManager.queryIntentActivities(i, 0)) {
                val p = ri.activityInfo.packageName
                if (p != packageName && p !in map) map[p] = ri.loadLabel(packageManager).toString()
            }
        }
        return map.entries.map { it.key to it.value }
    }

    private fun readerName(): String {
        val r = Prefs.s("reader", "")
        if (r.isEmpty()) return "ברירת המחדל"
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(r, 0)).toString()
        } catch (e: Exception) { r }
    }

    private fun pickReader() {
        val opts = listOf("" to "ברירת המחדל של המערכת") + readers()
        Sheet.choice(this, "פתיחת ספרים באמצעות", opts, Prefs.s("reader", "")) { v -> Prefs.put("reader", v); build() }
    }

    private fun hiddenApps() {
        val hid = Prefs.list("hidden")
        if (hid.isEmpty()) { toast("אין אפליקציות מוסתרות"); return }
        if (Apps.all.isEmpty()) Apps.load(this)
        Sheet.actions(this, "הקישו כדי להחזיר למסך הבית", hid.map { k ->
            val name = Apps.byKey(k)?.label ?: k.substringBefore('/')
            Sheet.Item(name) { Apps.unhide(k); build() }
        })
    }

    private fun manageFolders() {
        val l = Store.homeFolders()
        val items = l.map { p ->
            Sheet.Item("הסרת " + Store.folderName(p), red = true) { Store.removeHomeFolder(p); build() }
        } + Sheet.Item("הוספת תיקייה…") {
            if (!hasFiles()) askFiles() else go(Intent(this, FilesActivity::class.java).putExtra("pick", true))
        } + Sheet.Item("החזרת ברירת המחדל") { Prefs.put("folders", null); build() }
        Sheet.actions(this, "תיקיות במסך הבית", items)
    }
}
