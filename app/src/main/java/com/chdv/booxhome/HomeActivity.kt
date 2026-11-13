package com.chdv.booxhome

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Home screen laid out in four horizontal bands, top to bottom:
 *   1. Last four books read (four tiles)
 *   2. Installed apps (middle third)
 *   3. Folders (lower third)
 *   4. Fixed dock (always visible)
 *
 * Soft pastel palette, flat surfaces, no animations (e-ink friendly).
 * Last-books list and folders are placeholders until the reader/file-manager steps.
 */
class HomeActivity : AppCompatActivity() {

    private val pastel = listOf(
        "#F4D9E3", "#D9EEDF", "#DCE8F7", "#F7E9C8"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FBF9F6"))
            setPadding(24, 24, 24, 24)
        }

        root.addView(buildBooksBand(), wrap(weight = 0f))
        root.addView(buildAppsBand(), wrap(weight = 1f))
        root.addView(buildFoldersBand(), wrap(weight = 0.6f))
        root.addView(buildDock(), wrap(weight = 0f))

        setContentView(root)
    }

    private fun wrap(weight: Float) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        if (weight == 0f) ViewGroup.LayoutParams.WRAP_CONTENT else 0,
        weight
    ).apply { setMargins(0, 8, 0, 8) }

    /** Band 1: four tiles for the last four books. Placeholder titles for now. */
    private fun buildBooksBand(): GridLayout {
        val grid = GridLayout(this).apply { columnCount = 4 }
        val titles = listOf("ספר א׳", "ספר ב׳", "ספר ג׳", "ספר ד׳")
        titles.forEachIndexed { i, title ->
            grid.addView(TextView(this).apply {
                text = title
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
                setBackgroundColor(Color.parseColor(pastel[i]))
                gravity = Gravity.BOTTOM or Gravity.START
                setPadding(12, 12, 12, 12)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 180
                    columnSpec = GridLayout.spec(i, 1f)
                    setMargins(4, 4, 4, 4)
                }
            })
        }
        return grid
    }

    /** Band 2: installed apps, minus this launcher. */
    private fun buildAppsBand(): android.widget.ScrollView {
        val grid = GridLayout(this).apply { columnCount = 4 }
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString() }

        apps.forEach { info ->
            grid.addView(TextView(this).apply {
                text = info.loadLabel(pm).toString()
                textSize = 11f
                setTextColor(Color.parseColor("#222222"))
                gravity = Gravity.CENTER
                setPadding(4, 12, 4, 12)
                setOnClickListener {
                    pm.getLaunchIntentForPackage(info.packageName)?.let { startActivity(it) }
                }
            })
        }
        return android.widget.ScrollView(this).apply { addView(grid) }
    }

    /** Band 3: folders. Placeholder list until the file manager step. */
    private fun buildFoldersBand(): GridLayout {
        val grid = GridLayout(this).apply { columnCount = 3 }
        listOf("גמרא", "הלכה", "סריקות").forEachIndexed { i, name ->
            grid.addView(TextView(this).apply {
                text = name
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
                setBackgroundColor(Color.parseColor("#FFFFFF"))
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(i, 1f)
                    setMargins(4, 4, 4, 4)
                }
            })
        }
        return grid
    }

    /** Band 4: fixed dock. Opens the system settings for now. */
    private fun buildDock(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }
        listOf("Wi-Fi", "קבצים", "ספרים", "הגדרות").forEach { label ->
            row.addView(TextView(this).apply {
                text = label
                textSize = 11f
                setTextColor(Color.parseColor("#222222"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    if (label == "הגדרות") {
                        startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
                    }
                }
            })
        }
        return row
    }
}
