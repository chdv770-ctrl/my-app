package com.chdv.haboox

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class BooksActivity : BaseActivity() {

    private var all: List<File> = emptyList()
    private var shown: List<File> = emptyList()
    private lateinit var grid: GridView
    private lateinit var status: TextView
    private lateinit var body: FrameLayout
    private lateinit var search: EditText
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = vbox()
        search = EditText(this).apply {
            hint = "חיפוש ספר"
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(pal.text)
            setHintTextColor(pal.sub)
            textDirection = View.TEXT_DIRECTION_RTL
            background = roundBg(pal.card, dp(12).toFloat())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = filter()
            })
        }
        content.addView(search, lp(MATCH, WRAP).m(dp(16), 0, dp(16), dp(10)))
        body = FrameLayout(this)
        grid = GridView(this).apply {
            numColumns = 3
            horizontalSpacing = dp(12)
            verticalSpacing = dp(16)
            setPadding(dp(16), dp(4), dp(16), dp(16))
            clipToPadding = false
            selector = ColorDrawable(Color.TRANSPARENT)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = this@BooksActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> Store.open(this@BooksActivity, shown[pos]) }
            setOnItemLongClickListener { _, _, pos, _ -> menu(shown[pos]); true }
        }
        body.addView(grid, FrameLayout.LayoutParams(MATCH, MATCH))
        status = label("סורק ספרים…", 16f, pal.sub).apply { gravity = Gravity.CENTER }
        body.addView(status, FrameLayout.LayoutParams(MATCH, MATCH))
        content.addView(body, lp(MATCH, 0, 1f))
        screen("ספרים", content, listOf(
            iconButton("sort") { sortMenu() },
            iconButton("refresh") { load(true) }
        ))
    }

    override fun onResume() {
        super.onResume()
        load(false)
    }

    private fun load(force: Boolean) {
        if (!hasFiles()) {
            body.removeAllViews()
            val w = FrameLayout(this).apply { setPadding(dp(16), dp(10), dp(16), 0) }
            w.addView(permissionCard("כדי למצוא את הספרים שבמכשיר יש לאשר להיכל הספרים גישה לקבצים."), FrameLayout.LayoutParams(MATCH, WRAP))
            body.addView(w, FrameLayout.LayoutParams(MATCH, MATCH))
            return
        }
        if (grid.parent == null) {
            body.removeAllViews()
            body.addView(grid, FrameLayout.LayoutParams(MATCH, MATCH))
            body.addView(status, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        if (Books.cache.isNotEmpty() && !force) show()
        else { status.text = "סורק ספרים…"; status.visibility = View.VISIBLE }
        Thread {
            Books.scan(force)
            runOnUiThread { show() }
        }.start()
    }

    private fun show() {
        all = Books.sorted(Prefs.s("bookSort", "recent"))
        filter()
    }

    private fun filter() {
        val q = search.text.toString().trim().lowercase()
        shown = if (q.isEmpty()) all else all.filter { it.name.lowercase().contains(q) }
        adapter.notifyDataSetChanged()
        status.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        if (shown.isEmpty()) status.text = if (all.isEmpty()) "לא נמצאו ספרים במכשיר" else "אין תוצאות"
        setScreenTitle("ספרים" + if (all.isNotEmpty()) "  ·  ${all.size}" else "")
    }

    private fun sortMenu() {
        Sheet.choice(this, "מיון ספרים", listOf("recent" to "נקראו לאחרונה", "name" to "שם"),
            Prefs.s("bookSort", "recent")) { v -> Prefs.put("bookSort", v); show() }
    }

    private fun menu(f: File) {
        Sheet.actions(this, f.nameWithoutExtension, listOf(
            Sheet.Item("פתיחה") { Store.open(this, f) },
            Sheet.Item("פתיחה באמצעות…") { Store.open(this, f, chooser = true) },
            Sheet.Item("הצגה בתיקייה") { go(Intent(this, FilesActivity::class.java).putExtra("path", f.parent)) },
            Sheet.Item("מידע") {
                Sheet.info(this, f.nameWithoutExtension, "סוג: ${f.extension.uppercase()}\nגודל: ${Store.sizeStr(f.length())}\nמיקום: ${f.parent}")
            }
        ))
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val f = shown[position]
            val cell: LinearLayout
            val iv: ImageView
            val t: TextView
            val s: TextView
            if (convertView is LinearLayout) {
                cell = convertView
                iv = cell.findViewWithTag("iv")
                t = cell.findViewWithTag("t")
                s = cell.findViewWithTag("s")
            } else {
                cell = vbox()
                val sq = SquareFrame(this@BooksActivity).apply {
                    background = roundBg(pal.card, dp(12).toFloat(), dp(1), pal.line)
                    clipToOutline = true
                }
                iv = ImageView(this@BooksActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP; tag = "iv" }
                sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                cell.addView(sq, lp(MATCH, WRAP))
                t = label("", 13f, pal.text).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END; tag = "t"; setPadding(dp(2), dp(5), dp(2), 0) }
                s = label("", 11f, pal.sub).apply { tag = "s"; setPadding(dp(2), 0, dp(2), 0) }
                cell.addView(t)
                cell.addView(s)
            }
            Covers.load(this@BooksActivity, f, iv)
            t.text = f.nameWithoutExtension
            s.text = f.extension.uppercase() + " · " + Store.sizeStr(f.length())
            return cell
        }
    }
}
