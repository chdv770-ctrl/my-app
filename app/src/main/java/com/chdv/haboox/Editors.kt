package com.chdv.haboox

import android.app.Activity
import android.content.Intent

/** Editor for the bottom bar: every slot can hold an action, an app, a folder, a book, or nothing. */
object DockEditor {

    fun open(a: Activity, onChange: () -> Unit) {
        val l = Dock.items()
        val items = ArrayList<Sheet.Item>()
        l.forEachIndexed { i, k ->
            items.add(Sheet.Item("מקום ${i + 1}: " + Dock.describe(k)) { slot(a, i, onChange) })
        }
        if (l.size < 6) items.add(Sheet.Item("הוספת מקום") {
            val n = Dock.items().toMutableList(); n.add("empty"); Dock.save(n); onChange()
            slot(a, n.size - 1, onChange)
        })
        val on = Prefs.b("dockOn", true)
        items.add(Sheet.Item(if (on) "הסתרת השורה התחתונה" else "הצגת השורה התחתונה") { Prefs.put("dockOn", !on); onChange() })
        items.add(Sheet.Item("איפוס לברירת המחדל") { Prefs.put("dock", null); Prefs.put("dockOn", true); onChange() })
        Sheet.actions(a, "השורה התחתונה · עד 6 מקומות", items)
    }

    fun slot(a: Activity, i: Int, onChange: () -> Unit) {
        val l = Dock.items()
        val cur = l.getOrNull(i) ?: "empty"
        fun set(k: String) { Dock.setSlot(i, k); onChange() }
        val items = ArrayList<Sheet.Item>()
        items.add(Sheet.Item("פעולה (Wi‑Fi, קבצים, ספרים, הגדרות…)") {
            Sheet.choice(a, "בחירת פעולה", Dock.builtIn, cur) { v -> set(v) }
        })
        items.add(Sheet.Item("אפליקציה…") {
            if (Apps.all.isEmpty()) Apps.load(a)
            Sheet.actions(a, "בחירת אפליקציה", Apps.everything().map { app ->
                Sheet.Item(Apps.name(app)) { set("app:" + app.key) }
            })
        })
        items.add(Sheet.Item("תיקייה…") {
            if (!Store.hasAccess(a)) Store.askAccess(a)
            else a.startActivity(Intent(a, FilesActivity::class.java).putExtra("pick", true).putExtra("dockSlot", i))
        })
        items.add(Sheet.Item("ספר…") {
            if (!Store.hasAccess(a)) Store.askAccess(a)
            else Thread {
                Books.scan()
                val list = Books.sorted("recent").take(80)
                a.runOnUiThread {
                    if (list.isEmpty()) a.toast("לא נמצאו ספרים")
                    else Sheet.actions(a, "בחירת ספר", list.map { f -> Sheet.Item(f.nameWithoutExtension) { set("book:" + f.path) } })
                }
            }.start()
        })
        items.add(Sheet.Item("ריק (בלי כלום)") { set("empty") })
        if (i > 0) items.add(Sheet.Item("הזזה ימינה") { swap(i, i - 1); onChange() })
        if (i < l.size - 1) items.add(Sheet.Item("הזזה שמאלה") { swap(i, i + 1); onChange() })
        items.add(Sheet.Item("הסרת המקום", red = true) {
            val n = Dock.items().toMutableList(); if (i in n.indices) n.removeAt(i); Dock.save(n); onChange()
        })
        Sheet.actions(a, "מקום ${i + 1} · " + Dock.describe(cur), items)
    }

    private fun swap(a: Int, b: Int) {
        val n = Dock.items().toMutableList()
        if (a !in n.indices || b !in n.indices) return
        val t = n[a]; n[a] = n[b]; n[b] = t
        Dock.save(n)
    }
}

/** Builds a personal theme from two colours and a light/dark background. */
object ThemeEditor {
    fun open(a: Activity, onDone: () -> Unit) {
        Sheet.choice(a, "ערכה משלי · רקע", listOf("light" to "בהיר", "dark" to "כהה"), Prefs.s("customBg", "light")) { bg ->
            Prefs.put("customBg", bg)
            val opts = Look.swatches.map { it.second.toString() to it.first }
            Sheet.choice(a, "צבע ראשי", opts, Prefs.i("customA", Look.c(0xFF7C3AED)).toString()) { c1 ->
                Prefs.put("customA", c1.toInt())
                Sheet.choice(a, "צבע שני (למעבר צבע)", opts, Prefs.i("customB", Look.c(0xFF0EA5E9)).toString()) { c2 ->
                    Prefs.put("customB", c2.toInt())
                    Prefs.put("theme", "custom")
                    onDone()
                }
            }
        }
    }
}
