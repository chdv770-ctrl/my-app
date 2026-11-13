package com.chdv.booxhome

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * In-app settings. Sections: display, home screen, books and files, system, about.
 * The toggles are placeholders that will be wired to SharedPreferences in the next step.
 * "System settings" opens the Android settings app, as agreed.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FBF9F6"))
            setPadding(24, 24, 24, 24)
        }

        root.addView(title("הגדרות"))

        root.addView(section("תצוגה", listOf(
            "גודל סמלים" to "בינוני",
            "אנימציות" to "עדינות"
        )))

        root.addView(section("מסך בית", listOf(
            "תאריך עברי" to "פעיל",
            "שורה תחתונה" to "עריכה"
        )))

        root.addView(section("ספרים וקבצים", listOf(
            "אפליקציית קריאה" to "בחירה",
            "תיקיות נבחרות" to "ניהול"
        )))

        root.addView(section("מערכת", listOf(
            "הגדרות אנדרואיד" to "פתח"
        )).apply {
            (getChildAt(1) as? TextView)?.setOnClickListener {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })

        root.addView(section("אודות", listOf(
            "BooxHome" to "גרסה 1.0",
            "פותח על ידי" to "שניאור וילהלם"
        )))

        setContentView(root)
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.parseColor("#222222"))
        gravity = Gravity.START
        setPadding(0, 0, 0, 24)
    }

    private fun section(header: String, rows: List<Pair<String, String>>) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 24) }

        addView(TextView(this@SettingsActivity).apply {
            text = header
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 0, 0, 8)
        })

        rows.forEach { (label, value) ->
            addView(TextView(this@SettingsActivity).apply {
                text = "$label  ·  $value"
                textSize = 14f
                setTextColor(Color.parseColor("#222222"))
                setBackgroundColor(Color.parseColor("#FFFFFF"))
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 2, 0, 2) }
            })
        }
    }
}
