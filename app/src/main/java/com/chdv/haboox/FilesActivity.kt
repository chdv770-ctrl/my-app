package com.chdv.haboox

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilesActivity : BaseActivity() {

    companion object {
        var clip: File? = null
        var clipCut = false
    }

    private lateinit var cur: File
    private var atVolumes = false
    private var vols: List<Store.Vol> = emptyList()
    private var items: List<File> = emptyList()
    private lateinit var list: ListView
    private lateinit var crumb: TextView
    private lateinit var empty: TextView
    private lateinit var body: FrameLayout
    private lateinit var actions: LinearLayout
    private var pickBar: View? = null
    private var pick = false
    private val adapter = Adapter()
    private val df = SimpleDateFormat("dd.MM.yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pick = intent.getBooleanExtra("pick", false)
        vols = Store.volumes()
        val start = intent.getStringExtra("path")?.let { File(it) }
        if (start != null && start.isDirectory) cur = start
        else {
            cur = Store.root()
            atVolumes = vols.size > 1 && !intent.getBooleanExtra("internal", false)
        }
        intent.getStringExtra("volume")?.let { p -> cur = File(p); atVolumes = false }

        val content = vbox()
        crumb = label("", 13f, pal.sub).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.START
            setPadding(dp(20), 0, dp(20), dp(8))
        }
        content.addView(crumb, lp(MATCH, WRAP))
        body = FrameLayout(this)
        list = ListView(this).apply {
            divider = ColorDrawable(pal.line)
            dividerHeight = maxOf(1, dp(0.7f))
            selector = ColorDrawable(Color.TRANSPARENT)
            adapter = this@FilesActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> tap(items[pos]) }
            setOnItemLongClickListener { _, _, pos, _ -> if (!atVolumes) itemMenu(items[pos]); true }
        }
        body.addView(list, FrameLayout.LayoutParams(MATCH, MATCH))
        empty = label("התיקייה ריקה", 16f, pal.sub).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        body.addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
        content.addView(body, lp(MATCH, 0, 1f))

        if (pick) {
            val bar = hbox().apply { setPadding(dp(16), dp(10), dp(16), dp(14)); gravity = Gravity.CENTER }
            val dockSlot = intent.getIntExtra("dockSlot", -1)
            bar.addView(pillButton(if (dockSlot >= 0) "בחירת התיקייה הזאת לשורה התחתונה" else "הוספת התיקייה הזאת למסך הבית") {
                if (atVolumes) { toast("בחרו קודם אחסון או תיקייה"); return@pillButton }
                if (dockSlot >= 0) {
                    Dock.setSlot(dockSlot, "folder:" + cur.path)
                    toast("נשמרה בשורה התחתונה: " + Store.folderName(cur.path))
                } else {
                    Store.addHomeFolder(cur.path)
                    toast("נוספה למסך הבית: " + Store.folderName(cur.path))
                }
                finish()
            }, lp(MATCH, WRAP))
            pickBar = bar
            content.addView(bar, lp(MATCH, WRAP))
        }

        actions = screen(if (pick) "בחירת תיקייה" else "קבצים", content, onBack = { goUp() })
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goUp()
        })
    }

    override fun onResume() {
        super.onResume()
        vols = Store.volumes()
        load()
    }

    private fun buildActions() {
        actions.removeAllViews()
        if (atVolumes) return
        if (clip != null) actions.addView(iconButton("paste") { paste() })
        actions.addView(iconButton("plus") { newFolder() })
        actions.addView(iconButton("sort") { sortMenu() })
        if (vols.size > 1) actions.addView(iconButton("sd") { atVolumes = true; load() })
        else actions.addView(iconButton("home") { cur = Store.root(); load() })
    }

    private fun goUp() {
        if (atVolumes) { finish(); return }
        val p = cur.parentFile
        if (Store.isVolumeRoot(cur.path) || p == null || !p.canRead()) {
            if (vols.size > 1 && !atVolumes) { atVolumes = true; load() } else finish()
        } else { cur = p; load() }
    }

    private fun load() {
        if (!hasFiles()) {
            body.removeAllViews()
            val wrap = FrameLayout(this).apply { setPadding(dp(16), dp(20), dp(16), 0) }
            wrap.addView(permissionCard("כדי לעיין בקבצים ובכרטיס הזיכרון יש לאשר להיכל הספרים גישה לכל הקבצים במכשיר."),
                FrameLayout.LayoutParams(MATCH, WRAP))
            body.addView(wrap, FrameLayout.LayoutParams(MATCH, MATCH))
            return
        }
        if (list.parent == null) {
            body.removeAllViews()
            body.addView(list, FrameLayout.LayoutParams(MATCH, MATCH))
            body.addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        if (atVolumes) {
            items = vols.map { it.dir }
            adapter.notifyDataSetChanged()
            empty.visibility = View.GONE
            setScreenTitle("אחסון")
            crumb.text = if (vols.any { it.removable }) "זוהה כרטיס זיכרון" else "לא זוהה כרטיס זיכרון"
            buildActions()
            return
        }
        val hidden = Prefs.b("showHidden", false)
        val raw = (cur.listFiles() ?: emptyArray()).filter { hidden || !it.name.startsWith(".") }
        val mode = Prefs.s("sort", "name")
        val byName = compareBy<File> { it.name.lowercase() }
        val cmp: Comparator<File> = when (mode) {
            "date" -> compareByDescending<File> { it.lastModified() }
            "type" -> compareBy<File> { it.extension.lowercase() }.thenBy { it.name.lowercase() }
            "size" -> compareByDescending<File> { it.length() }
            else -> byName
        }
        val dirs = raw.filter { it.isDirectory }.sortedWith(if (mode == "date") cmp else byName)
        val files = if (pick) emptyList() else raw.filter { !it.isDirectory }.sortedWith(cmp)
        items = dirs + files
        adapter.notifyDataSetChanged()
        list.setSelection(0)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        val vol = Store.volumeOf(cur.path)
        setScreenTitle(if (vol != null && vol.dir.path == cur.path) vol.name else cur.name)
        crumb.text = Store.prettyPath(cur.path)
        buildActions()
    }

    private fun tap(f: File) {
        if (atVolumes) {
            if (!f.canRead()) { toast("אין גישה לאחסון הזה. ודאו שאושרה גישה לכל הקבצים"); return }
            cur = f; atVolumes = false; load(); return
        }
        if (f.isDirectory) { cur = f; load() } else Store.open(this, f)
    }

    private fun itemMenu(f: File) {
        val l = ArrayList<Sheet.Item>()
        l.add(Sheet.Item("פתיחה") { tap(f) })
        if (!f.isDirectory) l.add(Sheet.Item("פתיחה באמצעות…") { Store.open(this, f, chooser = true) })
        l.add(Sheet.Item("שינוי שם") { rename(f) })
        l.add(Sheet.Item("העתקה") { clip = f; clipCut = false; buildActions(); toast("הועתק. עברו לתיקייה ולחצו על סמל ההדבקה") })
        l.add(Sheet.Item("גזירה / העברה") { clip = f; clipCut = true; buildActions(); toast("נגזר. עברו לתיקיית היעד ולחצו על סמל ההדבקה") })
        if (f.isDirectory) l.add(Sheet.Item("הוספה למסך הבית") { Store.addHomeFolder(f.path); toast("נוספה למסך הבית") })
        l.add(Sheet.Item("מידע") { info(f) })
        l.add(Sheet.Item("מחיקה", red = true) {
            Sheet.confirm(this, "מחיקה", "למחוק את \"${f.name}\"?" + (if (f.isDirectory) "\nכל התוכן שבתיקייה יימחק." else "") + "\nלא ניתן לבטל פעולה זו.", "מחיקה") {
                val ok = f.deleteRecursively()
                toast(if (ok) "נמחק" else "המחיקה נכשלה")
                load()
            }
        })
        Sheet.actions(this, f.name, l)
    }

    private fun rename(f: File) {
        Sheet.input(this, "שינוי שם", f.name, f.name) { s ->
            if (s.isEmpty() || s == f.name) return@input
            if (s.contains('/')) { toast("השם אינו תקין"); return@input }
            val t = File(f.parentFile, s)
            if (t.exists()) { toast("כבר קיים פריט בשם הזה"); return@input }
            if (f.renameTo(t)) load() else toast("שינוי השם נכשל")
        }
    }

    private fun info(f: File) {
        val sb = StringBuilder()
        sb.append("מיקום: ").append(Store.prettyPath(f.parent ?: "")).append('\n')
        if (f.isDirectory) sb.append("פריטים: ").append(f.list()?.size ?: 0).append('\n')
        else sb.append("גודל: ").append(Store.sizeStr(f.length())).append('\n')
        sb.append("שונה: ").append(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(f.lastModified())))
        Sheet.info(this, f.name, sb.toString())
    }

    private fun newFolder() {
        Sheet.input(this, "תיקייה חדשה", "", "שם התיקייה") { s ->
            if (s.isEmpty()) return@input
            val t = File(cur, s)
            if (t.exists()) toast("כבר קיימת תיקייה בשם הזה")
            else if (t.mkdirs()) load() else toast("לא ניתן ליצור כאן תיקייה")
        }
    }

    private fun paste() {
        val src = clip ?: return
        val cut = clipCut
        val dir = cur
        if (!src.exists()) { clip = null; buildActions(); toast("הפריט המקורי כבר לא קיים"); return }
        toast(if (cut) "מעביר…" else "מעתיק…")
        Thread {
            val ok = Store.paste(src, dir, cut)
            runOnUiThread {
                if (ok) { if (cut) clip = null; toast("הושלם") } else toast("הפעולה נכשלה")
                load()
            }
        }.start()
    }

    private fun sortMenu() {
        Sheet.choice(this, "מיון לפי", listOf(
            "name" to "שם", "date" to "תאריך שינוי", "type" to "סוג", "size" to "גודל"
        ), Prefs.s("sort", "name")) { v -> Prefs.put("sort", v); load() }
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val f = items[position]
            val row: LinearLayout
            val box: FrameLayout
            val icon: ImageView
            val title: TextView
            val sub: TextView
            val chev: ImageView
            if (convertView is LinearLayout) {
                row = convertView
                box = row.findViewWithTag("b")
                icon = row.findViewWithTag("i")
                title = row.findViewWithTag("t")
                sub = row.findViewWithTag("s")
                chev = row.findViewWithTag("c")
            } else {
                row = hbox().apply {
                    setPadding(dp(16), dp(10), dp(12), dp(10))
                    minimumHeight = dp(64)
                    background = pressBg(Color.TRANSPARENT, pal.card2, 0)
                }
                box = FrameLayout(this@FilesActivity).apply { tag = "b" }
                icon = ImageView(this@FilesActivity).apply { tag = "i" }
                box.addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
                row.addView(box, lp(dp(46), dp(46)))
                val col = vbox()
                title = label("", 16f, pal.text).oneLine().apply { tag = "t" }
                sub = label("", 12.5f, pal.sub).oneLine().apply { tag = "s" }
                col.addView(title)
                col.addView(sub)
                row.addView(col, lp(0, WRAP, 1f).m(dp(12), 0, dp(8), 0))
                chev = ImageView(this@FilesActivity).apply { tag = "c" }
                row.addView(chev, lp(dp(18), dp(18)))
            }
            val kind = if (atVolumes) (if (position == 0) "screen" else "sd") else Store.kindOf(f)
            val tint = if (pal.pastel) pal.tile(kind.hashCode()) else pal.card
            box.background = roundBg(tint, dp(12).toFloat())
            icon.setImageDrawable(Glyph(kind, pal.ink))
            chev.setImageDrawable(Glyph("chev_l", pal.sub))
            if (atVolumes) {
                val v = vols[position]
                title.text = v.name
                sub.text = Store.sizeStr(v.dir.freeSpace) + " פנויים מתוך " + Store.sizeStr(v.dir.totalSpace)
                chev.visibility = View.VISIBLE
                return row
            }
            title.text = f.name
            sub.text = if (f.isDirectory) {
                val n = f.list()?.size ?: 0
                "$n פריטים · " + df.format(Date(f.lastModified()))
            } else Store.sizeStr(f.length()) + " · " + df.format(Date(f.lastModified())) + " · " + f.extension.uppercase()
            chev.visibility = if (f.isDirectory) View.VISIBLE else View.INVISIBLE
            if (clip != null && clip?.path == f.path) title.text = f.name + (if (clipCut) "  (נגזר)" else "  (הועתק)")
            return row
        }
    }
}
