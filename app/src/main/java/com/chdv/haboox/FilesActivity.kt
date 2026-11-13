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
    private var items: List<File> = emptyList()
    private lateinit var list: ListView
    private lateinit var crumb: TextView
    private lateinit var empty: TextView
    private lateinit var body: FrameLayout
    private lateinit var actions: LinearLayout
    private var pick = false
    private val adapter = Adapter()
    private val df = SimpleDateFormat("dd.MM.yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pick = intent.getBooleanExtra("pick", false)
        val start = intent.getStringExtra("path")?.let { File(it) }
        cur = if (start != null && start.isDirectory) start else Store.root()

        val content = vbox()
        crumb = label("", 13f, pal.sub).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.START
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.END
            setPadding(dp(20), 0, dp(20), dp(8))
        }
        content.addView(crumb, lp(MATCH, WRAP))
        body = FrameLayout(this)
        list = ListView(this).apply {
            divider = ColorDrawable(pal.line)
            dividerHeight = maxOf(1, dp(0.7f))
            selector = ColorDrawable(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = true
            adapter = this@FilesActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> tap(items[pos]) }
            setOnItemLongClickListener { _, _, pos, _ -> itemMenu(items[pos]); true }
        }
        body.addView(list, FrameLayout.LayoutParams(MATCH, MATCH))
        empty = label("התיקייה ריקה", 16f, pal.sub).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        body.addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
        content.addView(body, lp(MATCH, 0, 1f))

        if (pick) {
            val bar = hbox().apply { setPadding(dp(16), dp(10), dp(16), dp(14)); gravity = Gravity.CENTER }
            bar.addView(pillButton("הוספת התיקייה הזאת למסך הבית") {
                Store.addHomeFolder(cur.path)
                toast("נוספה למסך הבית: " + Store.folderName(cur.path))
                finish()
            }, lp(MATCH, WRAP))
            content.addView(bar, lp(MATCH, WRAP))
        }

        actions = screen(if (pick) "בחירת תיקייה" else "קבצים", content, onBack = { goUp() })
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goUp()
        })
        buildActions()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun buildActions() {
        actions.removeAllViews()
        if (clip != null) actions.addView(iconButton("paste") { paste() })
        actions.addView(iconButton("plus") { newFolder() })
        actions.addView(iconButton("sort") { sortMenu() })
        actions.addView(iconButton("home") { cur = Store.root(); load() })
    }

    private fun goUp() {
        val p = cur.parentFile
        if (cur.path == Store.root().path || p == null || !p.canRead()) finish()
        else { cur = p; load() }
    }

    private fun load() {
        if (!hasFiles()) {
            body.removeAllViews()
            val wrap = FrameLayout(this).apply { setPadding(dp(16), dp(20), dp(16), 0) }
            wrap.addView(permissionCard("כדי לעיין בקבצים יש לאשר להבוקס גישה לכל הקבצים במכשיר."),
                FrameLayout.LayoutParams(MATCH, WRAP))
            body.addView(wrap, FrameLayout.LayoutParams(MATCH, MATCH))
            return
        }
        if (list.parent == null) {
            body.removeAllViews()
            body.addView(list, FrameLayout.LayoutParams(MATCH, MATCH))
            body.addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        val hidden = Prefs.b("showHidden", false)
        val raw = (cur.listFiles() ?: emptyArray()).filter { hidden || !it.name.startsWith(".") }
        val mode = Prefs.s("sort", "name")
        val cmp: Comparator<File> = when (mode) {
            "date" -> compareByDescending<File> { it.lastModified() }
            "type" -> compareBy<File> { it.extension.lowercase() }.thenBy { it.name.lowercase() }
            "size" -> compareByDescending<File> { it.length() }
            else -> compareBy<File> { it.name.lowercase() }
        }
        val dirs = raw.filter { it.isDirectory }.sortedWith(if (mode == "type" || mode == "size") compareBy<File> { it.name.lowercase() } else cmp)
        val files = if (pick) emptyList() else raw.filter { !it.isDirectory }.sortedWith(cmp)
        items = dirs + files
        adapter.notifyDataSetChanged()
        list.setSelection(0)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        setScreenTitle(if (cur.path == Store.root().path) (if (pick) "בחירת תיקייה" else "אחסון פנימי") else cur.name)
        crumb.text = cur.path.replace(Store.root().path, "אחסון")
        buildActions()
    }

    private fun tap(f: File) {
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
        sb.append("מיקום: ").append(f.parent ?: "").append('\n')
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
            val icon: ImageView
            val title: TextView
            val sub: TextView
            val chev: ImageView
            if (convertView is LinearLayout) {
                row = convertView
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
                val box = FrameLayout(this@FilesActivity).apply { background = roundBg(pal.card, dp(12).toFloat()) }
                icon = ImageView(this@FilesActivity).apply { tag = "i" }
                box.addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
                row.addView(box, lp(dp(46), dp(46)))
                val col = vbox()
                title = label("", 16f, pal.text).oneLine().apply { tag = "t" } as TextView
                sub = label("", 12.5f, pal.sub).oneLine().apply { tag = "s" } as TextView
                col.addView(title)
                col.addView(sub)
                row.addView(col, lp(0, WRAP, 1f).m(dp(12), 0, dp(8), 0))
                chev = ImageView(this@FilesActivity).apply { tag = "c" }
                row.addView(chev, lp(dp(18), dp(18)))
            }
            val kind = Store.kindOf(f)
            icon.setImageDrawable(Glyph(kind, pal.ink))
            title.text = f.name
            sub.text = if (f.isDirectory) {
                val n = f.list()?.size ?: 0
                "$n פריטים · " + df.format(Date(f.lastModified()))
            } else Store.sizeStr(f.length()) + " · " + df.format(Date(f.lastModified())) + " · " + f.extension.uppercase()
            if (f.isDirectory) { chev.visibility = View.VISIBLE; chev.setImageDrawable(Glyph("chev_l", pal.sub)) }
            else chev.visibility = View.INVISIBLE
            if (clip != null && clip?.path == f.path) title.text = f.name + (if (clipCut) "  (נגזר)" else "  (הועתק)")
            return row
        }
    }
}
