package com.chdv.haboox

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

/** Mode "drawer": every installed app. Mode "manage": choose which apps appear on the home screen. */
class AppsActivity : BaseActivity() {

    private var manage = false
    private var all: List<AppEntry> = emptyList()
    private var shown: List<AppEntry> = emptyList()
    private val hidden = HashSet<String>()
    private lateinit var search: EditText
    private lateinit var listView: AbsListView
    private lateinit var countTv: TextView
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manage = intent.getBooleanExtra("manage", false)
        val content = vbox()
        search = EditText(this).apply {
            hint = "חיפוש אפליקציה"
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
        content.addView(search, lp(MATCH, WRAP).m(dp(16), 0, dp(16), dp(6)))
        countTv = label("", 13f, pal.sub).apply { setPadding(dp(20), dp(2), dp(20), dp(8)) }
        content.addView(countTv, lp(MATCH, WRAP))

        if (manage) {
            val bar = hbox().apply { setPadding(dp(16), 0, dp(16), dp(8)) }
            bar.addView(pillButton("הצגת הכול", filled = false) { hidden.clear(); save() }.apply { setTextSize(14f) }, lp(0, WRAP, 1f).m(0, 0, dp(4), 0))
            bar.addView(pillButton("רק מה שהתקנתי", filled = false) {
                hidden.clear(); hidden.addAll(all.filter { it.system }.map { it.key }); save()
            }.apply { setTextSize(14f) }, lp(0, WRAP, 1f).m(dp(4), 0, 0, 0))
            content.addView(bar, lp(MATCH, WRAP))
            listView = ListView(this).apply {
                divider = ColorDrawable(pal.line)
                dividerHeight = maxOf(1, dp(0.7f))
            }
        } else {
            listView = GridView(this).apply {
                numColumns = Prefs.i("cols", 4)
                verticalSpacing = dp(10)
                setPadding(dp(10), dp(4), dp(10), dp(16))
                clipToPadding = false
                stretchMode = GridView.STRETCH_COLUMN_WIDTH
            }
        }
        listView.selector = ColorDrawable(Color.TRANSPARENT)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ ->
            val a = shown[pos]
            if (manage) toggle(a) else Apps.launch(this, a)
        }
        listView.setOnItemLongClickListener { _, _, pos, _ -> menu(shown[pos]); true }
        content.addView(listView, lp(MATCH, 0, 1f))
        screen(if (manage) "אפליקציות במסך הבית" else "כל האפליקציות", content)
    }

    override fun onResume() {
        super.onResume()
        Thread {
            Apps.load(this)
            runOnUiThread {
                all = Apps.everything()
                hidden.clear(); hidden.addAll(Apps.hidden())
                filter()
            }
        }.start()
    }

    private fun save() {
        Apps.setHidden(hidden)
        filter()
    }

    private fun toggle(a: AppEntry) {
        if (a.key in hidden) hidden.remove(a.key) else hidden.add(a.key)
        save()
    }

    private fun filter() {
        val q = search.text.toString().trim().lowercase()
        shown = if (q.isEmpty()) all else all.filter { Apps.name(it).lowercase().contains(q) || it.pkg.contains(q) }
        adapter.notifyDataSetChanged()
        val onHome = all.count { it.key !in hidden }
        countTv.text = if (manage) "$onHome מתוך ${all.size} מוצגות במסך הבית · הקישו כדי להציג או להסתיר"
        else "${all.size} אפליקציות מותקנות"
    }

    private fun menu(a: AppEntry) {
        val onHome = a.key !in hidden
        Sheet.actions(this, Apps.name(a), listOf(
            Sheet.Item("פתיחה") { Apps.launch(this, a) },
            Sheet.Item(if (onHome) "הסתרה ממסך הבית" else "הצגה במסך הבית") { toggle(a) },
            Sheet.Item("הוספה לשורה התחתונה") { Dock.addApp(a.key); toast("נוספה לשורה התחתונה") },
            Sheet.Item("מידע על האפליקציה") {
                go(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg)))
            },
            Sheet.Item("הסרת התקנה מהמכשיר", red = true) {
                go(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)))
            }
        ))
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val a = shown[position]
            return if (manage) manageRow(a, position) else drawerCell(a, position)
        }
    }

    private fun drawerCell(a: AppEntry, i: Int): View {
        val v = vbox().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2), dp(8), dp(2), dp(6))
            background = pressBg(Color.TRANSPARENT, pal.card2, 16)
        }
        v.addView(Apps.iconView(this, a, 54, pal, i), lp(dp(54), dp(54)))
        v.addView(label(Apps.name(a), 12.5f, pal.text).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) }, lp(MATCH, WRAP))
        if (a.key in hidden) v.alpha = 0.55f
        return v
    }

    private fun manageRow(a: AppEntry, i: Int): View {
        val r = hbox().apply {
            setPadding(dp(16), dp(8), dp(16), dp(8))
            minimumHeight = dp(62)
        }
        r.addView(Apps.iconView(this, a, 42, pal, i), lp(dp(42), dp(42)))
        val col = vbox()
        col.addView(label(Apps.name(a), 16f, pal.text).oneLine())
        col.addView(label(if (a.system) "אפליקציית מערכת" else "הותקנה על ידך", 12.5f, pal.sub).oneLine())
        r.addView(col, lp(0, WRAP, 1f).m(dp(12), 0, dp(8), 0))
        val sw = SwitchCompat(this).apply {
            isChecked = a.key !in hidden
            isClickable = false
            isFocusable = false
        }
        r.addView(sw)
        r.layoutParams = AbsListView.LayoutParams(MATCH, WRAP)
        ignore(null)
        return r
    }

    @Suppress("unused")
    private fun unusedHolder(): FrameLayout? = null
    @Suppress("unused")
    private fun unusedLL(): LinearLayout? = null
}
