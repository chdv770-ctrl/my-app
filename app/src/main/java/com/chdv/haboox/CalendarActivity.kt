package com.chdv.haboox

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class CalendarActivity : BaseActivity() {

    private val shown: Calendar = Calendar.getInstance()
    private var selected: Long = System.currentTimeMillis()
    private lateinit var titleTv: android.widget.TextView
    private lateinit var subTitle: android.widget.TextView
    private lateinit var grid: LinearLayout
    private lateinit var detail: LinearLayout
    private var downX = 0f
    private var downY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shown.set(Calendar.DAY_OF_MONTH, 1)
        val content = vbox().apply { setPadding(dp(12), 0, dp(12), dp(12)) }

        val nav = hbox()
        nav.addView(iconButton("chev_r") { shift(-1) })
        val tv = vbox().apply { gravity = Gravity.CENTER }
        titleTv = label("", 21f, pal.text, true).apply { gravity = Gravity.CENTER }
        subTitle = label("", 14f, pal.sub).apply { gravity = Gravity.CENTER }
        tv.addView(titleTv, lp(MATCH, WRAP))
        tv.addView(subTitle, lp(MATCH, WRAP))
        nav.addView(tv, lp(0, WRAP, 1f))
        nav.addView(iconButton("chev_l") { shift(1) })
        content.addView(nav, lp(MATCH, WRAP))

        val wk = hbox().apply { setPadding(0, dp(10), 0, dp(4)) }
        for (d in 1..7) wk.addView(label(Heb.dayLetters[d], 13f, pal.sub, d == 7).apply { gravity = Gravity.CENTER }, lp(0, WRAP, 1f))
        content.addView(wk, lp(MATCH, WRAP))

        grid = vbox()
        content.addView(grid, lp(MATCH, WRAP))

        detail = vbox().apply {
            background = roundBg(pal.card, dp(18).toFloat())
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        content.addView(detail, lp(MATCH, WRAP).m(0, dp(14), 0, 0))

        val sv = ScrollView(this).apply { addView(content) }
        screen("לוח שנה", sv, listOf(pillButton("היום", filled = false) { today() }.apply {
            setTextSize(14f); setPadding(dp(16), dp(6), dp(16), dp(6))
        }))
        render()
    }

    private fun today() {
        shown.timeInMillis = System.currentTimeMillis()
        shown.set(Calendar.DAY_OF_MONTH, 1)
        selected = System.currentTimeMillis()
        render()
    }

    private fun shift(n: Int) {
        shown.add(Calendar.MONTH, n)
        render()
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a }
        val y = Calendar.getInstance().apply { timeInMillis = b }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }

    private fun render() {
        val he = Locale("he")
        titleTv.text = SimpleDateFormat("LLLL yyyy", he).format(shown.time)
        val first = shown.clone() as Calendar
        first.set(Calendar.HOUR_OF_DAY, 12)
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val last = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, days) }
        val h1 = Heb.of(first.timeInMillis)
        val h2 = Heb.of(last.timeInMillis)
        subTitle.text = if (h1.m == h2.m) "${Heb.monthName(h1.y, h1.m)} ${Heb.yearStr(h1.y)}"
        else if (h1.y == h2.y) "${Heb.monthName(h1.y, h1.m)} – ${Heb.monthName(h2.y, h2.m)} ${Heb.yearStr(h2.y)}"
        else "${Heb.monthName(h1.y, h1.m)} ${Heb.yearStr(h1.y)} – ${Heb.monthName(h2.y, h2.m)} ${Heb.yearStr(h2.y)}"

        grid.removeAllViews()
        val offset = first.get(Calendar.DAY_OF_WEEK) - 1
        val now = System.currentTimeMillis()
        var day = 1
        for (w in 0 until 6) {
            if (day > days) break
            val row = hbox().apply { gravity = Gravity.TOP }
            for (c in 0 until 7) {
                val cellIndex = w * 7 + c
                if (cellIndex < offset || day > days) {
                    row.addView(View(this), lp(0, dp(64), 1f))
                    continue
                }
                val cal = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                val ms = cal.timeInMillis
                val h = Heb.of(ms)
                val ev = Heb.events(h)
                val isToday = sameDay(ms, now)
                val isSel = sameDay(ms, selected)
                val sat = c == 6
                val fg = if (isToday) pal.onInk else pal.text
                val cell = vbox().apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(1), dp(5), dp(1), dp(3))
                    background = when {
                        isToday -> roundBg(pal.ink, dp(14).toFloat())
                        isSel -> roundBg(Color.TRANSPARENT, dp(14).toFloat(), dp(2), pal.ink)
                        ev.isNotEmpty() && pal.pastel -> roundBg(pal.tile(h.m), dp(14).toFloat())
                        else -> pressBg(Color.TRANSPARENT, pal.card2, 14)
                    }
                    addView(label(day.toString(), 18f, fg, sat || isToday).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP))
                    addView(label(Heb.gem(h.d), 11.5f, if (isToday) pal.onInk else pal.sub).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP))
                    val mark = label(if (ev.isNotEmpty()) ev[0].substringBefore(" ·") else "", 8.5f, fg, true).oneLine()
                    mark.gravity = Gravity.CENTER
                    addView(mark, lp(MATCH, WRAP))
                    setOnClickListener { selected = ms; render() }
                }
                row.addView(cell, lp(0, dp(64), 1f).m(dp(2), dp(2), dp(2), dp(2)))
                day++
            }
            grid.addView(row, lp(MATCH, WRAP))
        }
        renderDetail()
    }

    private fun renderDetail() {
        detail.removeAllViews()
        val h = Heb.of(selected)
        detail.addView(label("יום " + Heb.dayNames[h.dow], 14f, pal.sub, true))
        detail.addView(label(Heb.format(h), 22f, pal.text, true))
        detail.addView(label(Heb.gregLong(selected), 15f, pal.sub))
        val ev = ArrayList(Heb.events(h))
        if (h.dow == Heb.SAT) ev.add(0, "שבת קודש")
        val om = Heb.omer(h)
        if (om > 0) ev.add("היום ${Heb.gem(om)} לעומר")
        if (ev.isNotEmpty()) {
            val chips = vbox().apply { setPadding(0, dp(10), 0, 0) }
            for (e in ev) {
                val r = hbox().apply { setPadding(0, dp(3), 0, dp(3)) }
                r.addView(View(this).apply { background = roundBg(pal.ink, dp(4).toFloat()) }, lp(dp(8), dp(8)).m(0, 0, 0, 0))
                r.addView(label(e, 16f, pal.text), lp(WRAP, WRAP).m(dp(10), 0, dp(10), 0))
                chips.addView(r)
            }
            detail.addView(chips)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) { downX = ev.rawX; downY = ev.rawY }
        if (ev.actionMasked == MotionEvent.ACTION_UP) {
            val dx = ev.rawX - downX
            val dy = ev.rawY - downY
            if (abs(dx) > dp(80) && abs(dx) > 1.5f * abs(dy)) {
                shift(if (dx > 0) 1 else -1)
                val c = MotionEvent.obtain(ev); c.action = MotionEvent.ACTION_CANCEL
                super.dispatchTouchEvent(c); c.recycle()
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
