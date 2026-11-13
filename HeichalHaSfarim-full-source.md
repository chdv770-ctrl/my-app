# היכל הספרים (HaBoox): קוד המקור המלא

מסמך זה מכיל את כל קבצי הקוד של האפליקציה. כדי להמשיך לפתח: פרסו כל קובץ לנתיב שמופיע בכותרת שלו (או קחו מהמאגר github.com/chdv770-ctrl/my-app), קראו את HANDOFF.md, ובקשו מ-Claude Code להמשיך. הקבצים הבינאריים (לוגו PNG ומפתח החתימה app/haboox.p12) נמצאים במאגר בלבד.

---

## `HANDOFF.md`

````markdown
# היכל הספרים (HaBoox): מסמך המשך עבודה למפתח או ל-Claude Code

מסך בית (Launcher) לאנדרואיד, מותאם ל-**BOOX Go 6** (E-ink, ‏1072×1448, ‏Android 11, ‏2GB RAM).
שם האפליקציה: **היכל הספרים**. החבילה: `com.chdv.haboox`. הלוגו: סמל ישיבה גדולה ליובאוויטש, ומעליו "היכל הספרים" בחצי עיגול.
המאגר: `github.com/chdv770-ctrl/my-app` (פומבי).

## בנייה ופרסום
- אין צורך ב-Android Studio. כל push ל-`main` מפעיל את GitHub Actions (`.github/workflows/build-apk.yml`), שבונה APK חתום (release) ומפרסם Release בשם `v2.0.<run_number>` עם הקובץ `HaBoox.apk`.
- **העלאת קוד בלי git:** מעלים קובץ `source.zip` לשורש המאגר דרך "Add file → Upload files" באתר. ה-workflow פורס אותו, עושה commit לקוד, מוחק את ה-zip ובונה. ה-zip מכיל את `app/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md` ו-`HANDOFF.md`.
- **חתימה:** הקובץ `app/haboox.p12` (PKCS12, סיסמה `haboox123`). ה-alias מתגלה אוטומטית ב-workflow. **אסור להחליף את המפתח**, אחרת עדכונים לא יותקנו מעל הגרסה הקיימת.
- **כשהבנייה נכשלת:** נוצר Release מסוג pre-release בשם `build-log`, ובו `build-log.txt` עם שגיאות הקומפילציה.
- **גרסה:** `versionName = 2.0.<GITHUB_RUN_NUMBER>`, ‏`versionCode = 100 + run`.
- **עדכון בתוך האפליקציה:** `Updater.kt` בודק את `releases/latest` ב-API של GitHub, משווה את מספר הבנייה, מוריד את `HaBoox.apk` ומפעיל את המתקין. נדרשת הרשאת "התקנת אפליקציות לא מוכרות".

## מבנה הקוד (Kotlin, בלי XML layouts, בלי ספריות מלבד androidx core/appcompat)
| קובץ | תפקיד |
|---|---|
| `Ui.kt` | `Prefs` (SharedPreferences), ‏`Pal`/`Look` (ערכות נושא, כולל ערכה אישית), עזרים (`dp`, `label`, `vbox`/`hbox`, `roundBg`, `pressBg`), ‏`Glyph` (סמלים מצוירים בקוד), ‏`Deco`, ‏`BaseActivity` (מסך בסגנון iOS עם "חזרה" וכותרת גדולה), ‏`Sheet` (חלונות פעולה, בחירה, בחירה מרובה, קלט ואישור) |
| `Data.kt` | `Apps` (רשימת אפליקציות מ-LauncherApps, ‏PackageManager ו-getLaunchIntentForPackage, כולל שמות וסמלים מותאמים, הסתרה וסדר), ‏`Store` (הרשאות, אחסון פנימי וכרטיס זיכרון דרך `volumes()`, פתיחת קבצים עם FileProvider, העתקה והעברה, תיקיות במסך הבית), ‏`Books` (סריקת ספרים בכל האחסונים, היסטוריה), ‏`Dock` (מקומות בשורה התחתונה), ‏`Pages` (חלונות ורכיבים), ‏`Covers` (כריכות PDF/EPUB/CBZ ומטמון), ‏`ActionService` (נגישות: חזרה, חלונות אחרונים, התראות) |
| `Heb.kt` | לוח עברי אופליין (android.icu HebrewCalendar), גימטריה, חגים, צומות, ראש חודש, חנוכה, ספירת העומר |
| `HomeActivity.kt` | מסך הבית: חלונות (pages) עם רכיבים `hero`/`books`/`apps`/`folders`/`g:*`/`w:<id>`. אפליקציות שלא נכנסות גולשות לחלונות-המשך אוטומטיים. רכיב גמיש (אפליקציות, ואם אין אז ספרים) ממלא את המקום הפנוי. בנוסף: גאדג'טים של אנדרואיד (AppWidgetHost), שורה תחתונה, מרכז בקרה, מחוות |
| `Editors.kt` | `DockEditor` (כל מקום: פעולה, אפליקציה, תיקייה, ספר או ריק), ‏`ThemeEditor` (ערכה אישית משני צבעים) |
| `Updater.kt` | עדכון עצמי מ-GitHub Releases |
| `FilesActivity.kt` | סייר קבצים: מסך בחירת אחסון, ניווט, העתקה, גזירה, מחיקה, שינוי שם, מיון, ומצב בחירת תיקייה (`pick`, ‏`dockSlot`) |
| `BooksActivity.kt` | ספרייה: חיפוש, מיון, כריכות |
| `AppsActivity.kt` | כל האפליקציות (`manage=true` לבחירה מה יופיע במסך הבית) |
| `CalendarActivity.kt` | לוח שנה חודשי עברי-לועזי |
| `SettingsActivity.kt` | הגדרות: תצוגה, מסך הבית, ספרים וקבצים, מערכת ואודות |

## מפתחות Prefs חשובים
`theme`, `customA`, `customB`, `customBg`, `fontSize`, `clockSize`, `iconTiles`, `heroDeco`, `pages` (חלונות מופרדים בשורה חדשה, רכיבים בפסיקים), `dock`, `dockOn`, `hidden`, `order`, `name:<pkg/cls>`, `icon:<pkg/cls>`, `folders`, `fname:<path>`, `hist` (היסטוריית קריאה), `reader`, `swipeDown`, `autoUpdate`, `cols`, `labels`, `userFirst`.

## עקרונות עיצוב
- E-ink: בלי אנימציות (`windowAnimationStyle=@null`), ניגודיות גבוהה, החלפת דפים במקום גלילה.
- RTL מלא בעברית.
- ברירת המחדל היא שחור-לבן. יש 11 ערכות נושא, כולל ערכה אישית.
- מרכז הבקרה נפתח מכפתור רחב למעלה. ההחלקה למטה לא מתחילה מקצה המסך, כדי לא להתנגש בפאנל של BOOX.

פותח: שניאור וילהלם
````

---

## `README.md`

````markdown
# היכל הספרים

מסך בית בעיצוב נקי בהשראת iOS, בשחור-לבן, ל-BOOX Go 6 (E-ink, Android 11).

## מה יש באפליקציה
- **מסך הבית:** שעון, תאריך עברי ולועזי, חגים וספירת העומר, ארבעה ספרים אחרונים עם כריכה, אפליקציות בדפים בלי גלילה, תיקיות ושורה תחתונה (Wi-Fi, קבצים, ספרים, הגדרות).
- **מרכז בקרה** שנפתח בגרירה למטה: Wi-Fi, בלוטות', סוללה, בהירות, רענון מסך E-ink, חזרה, חלונות, התראות, מעבר למסך הרגיל והגדרות אנדרואיד.
- **לחיצה ארוכה על אפליקציה:** שינוי שם, שינוי סמל (אות, אימוג'י או סמל של אפליקציה אחרת), הזזה, הסתרה ומידע.
- **סייר קבצים:** ניווט, תיקייה חדשה, העתקה, גזירה והדבקה, שינוי שם, מחיקה עם אישור ומיון לפי שם, תאריך, סוג או גודל.
- **ספרייה:** כל הספרים שבמכשיר (PDF, EPUB, MOBI, FB2, DJVU ועוד) עם כריכות, חיפוש ומיון.
- **לוח שנה** עברי-לועזי עם חגים ומועדים, בלי צורך באינטרנט.
- **הגדרות מלאות:** תצוגה, מסך הבית, ספרים וקבצים, מערכת ואודות.

## התקנה
1. מורידים את `HaBoox.apk` מה-[Release האחרון](../../releases/latest).
2. מתקינים אותו במכשיר.
3. פותחים את "היכל הספרים" ולוחצים על "השלמה": מאשרים גישה לקבצים וקובעים אותה כמסך הבית.

פותח: שניאור וילהלם
````

---

## `settings.gradle.kts`

````kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HaBoox"
include(":app")
````

---

## `build.gradle.kts`

````kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
}
````

---

## `gradle.properties`

````
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
````

---

## `.gitignore`

````
.gradle/
build/
app/build/
local.properties
*.iml
.idea/
````

---

## `.github/workflows/build-apk.yml`

````yaml
name: Build HaBoox APK

on:
  push:
    branches: [main, master]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Unpack uploaded source (if present)
        run: |
          if [ -f source.zip ]; then
            rm -rf app build.gradle.kts settings.gradle.kts gradle.properties
            unzip -o -q source.zip -d .
            rm -f source.zip
            git config user.name "github-actions[bot]"
            git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
            git add -A
            git commit -m "HaBoox: unpack source" || true
            git push || true
          fi

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Signing key alias
        run: |
          A=$(keytool -list -keystore app/haboox.p12 -storetype pkcs12 -storepass haboox123 | grep -i PrivateKeyEntry | head -1 | cut -d, -f1)
          echo "alias: $A"
          echo "KS_ALIAS=$A" >> "$GITHUB_ENV"

      - name: Build release APK
        run: |
          set -o pipefail
          gradle :app:assembleRelease --no-daemon --stacktrace 2>&1 | tee build.log
          cp app/build/outputs/apk/release/app-release.apk HaBoox.apk

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: HaBoox-apk
          path: HaBoox.apk

      - name: Publish release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v2.0.${{ github.run_number }}
          name: הבוקס 2.0.${{ github.run_number }}
          body: קובץ ההתקנה של הבוקס – מסך בית ל‑BOOX Go 6. הורידו את HaBoox.apk והתקינו.
          files: HaBoox.apk
          make_latest: true

      - name: Publish build log on failure
        if: failure()
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release delete build-log -y --cleanup-tag || true
          (grep -E "^e: |error:|FAILURE|What went wrong" -A3 build.log | head -c 50000; echo; echo ----; tail -c 20000 build.log) > build-log.txt 2>/dev/null || echo "no log" > build-log.txt
          gh release create build-log build-log.txt --prerelease --title "build log" --notes "failed build log"
````

---

## `app/build.gradle.kts`

````kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksAlias: String? = System.getenv("KS_ALIAS")
val buildNumber: Int = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.chdv.haboox"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chdv.haboox"
        minSdk = 26
        targetSdk = 30
        versionCode = 100 + buildNumber
        versionName = "2.0.$buildNumber"
    }

    signingConfigs {
        if (ksAlias != null) {
            create("fixed") {
                storeFile = file("haboox.p12")
                storeType = "pkcs12"
                storePassword = "haboox123"
                keyAlias = ksAlias
                keyPassword = "haboox123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksAlias != null) signingConfigs.getByName("fixed") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
````

---

## `app/src/main/AndroidManifest.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.EXPAND_STATUS_BAR" />
    <uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />

    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:mimeType="*/*" />
        </intent>
        <intent>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent>
    </queries>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:requestLegacyExternalStorage="true"
        android:theme="@style/Theme.HaBoox"
        tools:targetApi="30">

        <activity
            android:name=".HomeActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:clearTaskOnLaunch="true"
            android:stateNotNeeded="true"
            android:excludeFromRecents="false"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustPan"
            tools:ignore="DiscouragedApi,LockedOrientationActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <activity android:name=".FilesActivity" android:exported="false"
            android:screenOrientation="portrait" tools:ignore="DiscouragedApi,LockedOrientationActivity" />
        <activity android:name=".AppsActivity" android:exported="false"
            android:screenOrientation="portrait" tools:ignore="DiscouragedApi,LockedOrientationActivity" />
        <activity android:name=".BooksActivity" android:exported="false"
            android:screenOrientation="portrait" tools:ignore="DiscouragedApi,LockedOrientationActivity" />
        <activity android:name=".CalendarActivity" android:exported="false"
            android:screenOrientation="portrait" tools:ignore="DiscouragedApi,LockedOrientationActivity" />
        <activity android:name=".SettingsActivity" android:exported="false"
            android:screenOrientation="portrait" tools:ignore="DiscouragedApi,LockedOrientationActivity" />

        <service
            android:name=".ActionService"
            android:exported="false"
            android:label="@string/service_name"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.files"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
````

---

## `app/src/main/res/values/strings.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">היכל הספרים</string>
    <string name="service_name">היכל הספרים – פעולות מסך</string>
    <string name="service_desc">מאפשר להיכל הספרים לבצע פעולות חזרה ופתיחת חלונות אחרונים מתוך מרכז הבקרה. השירות אינו קורא תוכן מהמסך.</string>
</resources>
````

---

## `app/src/main/res/values/themes.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.HaBoox" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="colorPrimary">#111111</item>
        <item name="colorPrimaryDark">#FFFFFF</item>
        <item name="colorAccent">#111111</item>
        <item name="colorControlActivated">#111111</item>
        <item name="colorControlNormal">#555555</item>
        <item name="android:windowBackground">@android:color/white</item>
        <item name="android:statusBarColor">@android:color/white</item>
        <item name="android:navigationBarColor">@android:color/white</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowAnimationStyle">@null</item>
        <item name="android:windowDisablePreview">true</item>
    </style>
</resources>
````

---

## `app/src/main/res/xml/accessibility_config.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="1000"
    android:canRetrieveWindowContent="false"
    android:description="@string/service_desc" />
````

---

## `app/src/main/res/xml/file_paths.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="storage" path="." />
    <root-path name="root" path="." />
    <cache-path name="cache" path="." />
</paths>
````

---

## `app/src/main/java/com/chdv/haboox/AppsActivity.kt`

````kotlin
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
````

---

## `app/src/main/java/com/chdv/haboox/BooksActivity.kt`

````kotlin
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
````

---

## `app/src/main/java/com/chdv/haboox/CalendarActivity.kt`

````kotlin
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
````

---

## `app/src/main/java/com/chdv/haboox/Data.kt`

````kotlin
package com.chdv.haboox

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/* ================================================================ apps */

class AppEntry(val pkg: String, val cls: String, val label: String, val system: Boolean = false) {
    val key: String get() = "$pkg/$cls"
    val cn: ComponentName get() = ComponentName(pkg, cls)
}

object Apps {
    @Volatile
    var all: List<AppEntry> = emptyList()
    private val icons = LruCache<String, Drawable>(160)

    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val res = LinkedHashMap<String, AppEntry>()
        fun isSys(ai: android.content.pm.ApplicationInfo?): Boolean {
            if (ai == null) return false
            val sys = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val upd = (ai.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            return sys && !upd
        }
        // 1. LauncherApps – the official launcher API (covers apps on SD card and all profiles)
        try {
            val la = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
            for (u in la.profiles) {
                for (a in la.getActivityList(null, u)) {
                    val cn = a.componentName
                    if (cn.packageName == ctx.packageName) continue
                    val e = AppEntry(cn.packageName, cn.className, a.label.toString(), isSys(a.applicationInfo))
                    if (!res.containsKey(e.key)) res[e.key] = e
                }
            }
        } catch (e: Exception) { ignore(e) }
        // 2. PackageManager query – merged in, in case LauncherApps missed something
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            for (r in pm.queryIntentActivities(i, PackageManager.MATCH_ALL)) {
                val p = r.activityInfo.packageName
                if (p == ctx.packageName) continue
                val e = AppEntry(p, r.activityInfo.name, r.loadLabel(pm).toString(), isSys(r.activityInfo.applicationInfo))
                if (!res.containsKey(e.key)) res[e.key] = e
            }
        } catch (e: Exception) { ignore(e) }
        // 3. Any installed package that has a launch intent but no visible LAUNCHER activity
        try {
            val known = res.values.map { it.pkg }.toHashSet()
            for (pi in pm.getInstalledApplications(0)) {
                if (pi.packageName == ctx.packageName || pi.packageName in known) continue
                val li = pm.getLaunchIntentForPackage(pi.packageName) ?: continue
                val cn = li.component ?: continue
                val e = AppEntry(cn.packageName, cn.className, pm.getApplicationLabel(pi).toString(), isSys(pi))
                if (!res.containsKey(e.key)) res[e.key] = e
            }
        } catch (e: Exception) { ignore(e) }
        all = res.values.toList()
        return all
    }

    fun hidden(): Set<String> = Prefs.list("hidden").toSet()

    fun ordered(): List<AppEntry> {
        val hid = hidden()
        val order = Prefs.list("order")
        val map = all.associateBy { it.key }
        val res = ArrayList<AppEntry>()
        for (k in order) {
            val a = map[k]
            if (a != null && k !in hid) res.add(a)
        }
        val orderSet = order.toSet()
        val userFirst = Prefs.b("userFirst", true)
        res.addAll(all.filter { it.key !in orderSet && it.key !in hid }
            .sortedWith(compareBy<AppEntry> { if (userFirst && it.system) 1 else 0 }.thenBy { name(it).lowercase() }))
        return res
    }

    fun everything(): List<AppEntry> =
        all.sortedWith(compareBy<AppEntry> { if (it.system) 1 else 0 }.thenBy { name(it).lowercase() })

    fun isHidden(a: AppEntry): Boolean = a.key in hidden()

    fun setHidden(keys: Collection<String>) {
        Prefs.putList("hidden", keys.toList())
    }

    fun saveOrder(list: List<AppEntry>) = Prefs.putList("order", list.map { it.key })

    fun name(a: AppEntry): String = Prefs.s("name:" + a.key, "").ifEmpty { a.label }

    fun hide(a: AppEntry) {
        val l = Prefs.list("hidden"); if (a.key !in l) l.add(a.key); Prefs.putList("hidden", l)
    }

    fun unhide(key: String) {
        val l = Prefs.list("hidden"); l.remove(key); Prefs.putList("hidden", l)
    }

    fun byKey(key: String): AppEntry? = all.firstOrNull { it.key == key }

    fun origIcon(ctx: Context, key: String, cn: ComponentName): Drawable {
        val cached = icons.get(key)
        if (cached != null) return cached.constantState?.newDrawable()?.mutate() ?: cached
        val d = try {
            ctx.packageManager.getActivityIcon(cn)
        } catch (e: Exception) {
            ctx.packageManager.defaultActivityIcon
        }
        icons.put(key, d)
        return d.constantState?.newDrawable()?.mutate() ?: d
    }

    fun clearIcons() = icons.evictAll()

    fun launch(ctx: Context, a: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.cn)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (ctx.go(i)) Prefs.put("lastApp", a.key)
    }

    /** Builds the icon view for an app according to the user's customisation. */
    fun iconView(ctx: Context, a: AppEntry, sizeDp: Int, pal: Pal, index: Int): View {
        val spec = Prefs.s("icon:" + a.key, "orig")
        val tiled = Prefs.b("iconTiles", true) && !(spec == "letter" || spec.startsWith("emoji:"))
        if (tiled) {
            val outer = FrameLayout(ctx)
            val s = ctx.dp(sizeDp)
            outer.background = roundBg(
                if (pal.vivid) pal.card else if (pal.pastel) pal.tile(index) else pal.card,
                s * 0.26f, if (pal.vivid || pal.night) 0 else ctx.dp(1), pal.line
            )
            if (pal.vivid) outer.elevation = ctx.dp(2).toFloat()
            val inner = (sizeDp * 0.74f).toInt()
            outer.addView(rawIcon(ctx, a, inner, pal, index, spec), FrameLayout.LayoutParams(ctx.dp(inner), ctx.dp(inner), Gravity.CENTER))
            return outer
        }
        return rawIcon(ctx, a, sizeDp, pal, index, spec)
    }

    private fun rawIcon(ctx: Context, a: AppEntry, sizeDp: Int, pal: Pal, index: Int, spec: String): View {
        val size = ctx.dp(sizeDp)
        val frame = FrameLayout(ctx)
        when {
            spec == "letter" || spec.startsWith("emoji:") -> {
                val txt = if (spec == "letter") name(a).trim().take(1) else spec.removePrefix("emoji:")
                val tv = TextView(ctx).apply {
                    text = txt
                    gravity = Gravity.CENTER
                    textSize = sizeDp * (if (spec == "letter") 0.42f else 0.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    if (spec == "letter") {
                        setTextColor(if (pal.pastel) pal.ink else pal.onInk)
                        background = roundBg(if (pal.pastel) pal.tile(index) else pal.ink, size * 0.24f)
                    } else {
                        setTextColor(pal.ink)
                        background = roundBg(if (pal.pastel) pal.tile(index) else pal.card, size * 0.24f, ctx.dp(1), pal.line)
                    }
                }
                frame.addView(tv, FrameLayout.LayoutParams(size, size))
            }
            else -> {
                val src = if (spec.startsWith("app:")) spec.removePrefix("app:") else a.key
                val cn = ComponentName.unflattenFromString(src) ?: a.cn
                val iv = ImageView(ctx)
                iv.setImageDrawable(origIcon(ctx, src, cn))
                if (Look.grayIcons()) iv.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                frame.addView(iv, FrameLayout.LayoutParams(size, size))
            }
        }
        return frame
    }
}

/* ================================================================ storage */

object Store {
    lateinit var app: Context

    class Vol(val name: String, val dir: File, val removable: Boolean)

    /** Internal storage + every SD card / USB volume. */
    private var volCache: List<Vol> = emptyList()
    private var volAt = 0L

    fun volumes(): List<Vol> {
        val now = System.currentTimeMillis()
        if (volCache.isNotEmpty() && now - volAt < 5000) return volCache
        volCache = readVolumes()
        volAt = now
        return volCache
    }

    private fun readVolumes(): List<Vol> {
        val out = ArrayList<Vol>()
        out.add(Vol("אחסון פנימי", root(), false))
        try {
            val sm = app.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
            for (v in sm.storageVolumes) {
                if (v.isPrimary) continue
                if (v.state != Environment.MEDIA_MOUNTED && v.state != Environment.MEDIA_MOUNTED_READ_ONLY) continue
                val d: File? = (if (android.os.Build.VERSION.SDK_INT >= 30) v.directory else null)
                    ?: v.uuid?.let { File("/storage/$it") }
                if (d != null && d.exists() && out.none { it.dir.path == d.path }) {
                    out.add(Vol(if (v.isRemovable) "כרטיס זיכרון" else (v.getDescription(app) ?: d.name), d, v.isRemovable))
                }
            }
        } catch (e: Exception) { ignore(e) }
        try {
            File("/storage").listFiles()?.forEach { f ->
                if (f.name != "emulated" && f.name != "self" && f.isDirectory && f.canRead() && out.none { it.dir.path == f.path }) {
                    out.add(Vol("כרטיס זיכרון", f, true))
                }
            }
        } catch (e: Exception) { ignore(e) }
        val sds = out.filter { it.removable }
        if (sds.size > 1) {
            return out.map { v -> if (v.removable) Vol(v.name + " " + v.dir.name, v.dir, true) else v }
        }
        return out
    }

    fun volumeOf(path: String): Vol? = volumes().filter { path == it.dir.path || path.startsWith(it.dir.path + "/") }
        .maxByOrNull { it.dir.path.length }

    fun isVolumeRoot(path: String): Boolean = volumes().any { it.dir.path == path }

    fun prettyPath(path: String): String {
        val v = volumeOf(path) ?: return path
        return v.name + path.removePrefix(v.dir.path)
    }

    fun hasAccess(ctx: Context): Boolean =
        if (isAtLeastR()) Environment.isExternalStorageManager()
        else ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun askAccess(a: Activity) {
        if (isAtLeastR()) {
            a.go(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + a.packageName)),
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            )
        } else {
            a.requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 7
            )
        }
    }

    fun root(): File = Environment.getExternalStorageDirectory()

    fun mime(f: File): String {
        val ext = f.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "epub" -> "application/epub+zip"
            "mobi", "azw", "azw3", "prc" -> "application/x-mobipocket-ebook"
            "fb2" -> "application/x-fictionbook+xml"
            "djvu", "djv" -> "image/vnd.djvu"
            "cbz" -> "application/vnd.comicbook+zip"
            "cbr" -> "application/vnd.comicbook-rar"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }

    fun uri(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)

    fun open(ctx: Context, f: File, chooser: Boolean = false) {
        val isBook = f.extension.lowercase() in Books.exts
        val u = try { uri(ctx, f) } catch (e: Exception) { Uri.fromFile(f) }
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(u, mime(f))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isBook) Books.touch(f.path)
        if (chooser) {
            ctx.go(Intent.createChooser(i, "פתיחה באמצעות").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val reader = Prefs.s("reader", "")
        if (isBook && reader.isNotEmpty()) {
            if (safeOpenTry(ctx, Intent(i).setPackage(reader))) return
        }
        if (!safeOpenTry(ctx, i)) ctx.toast("אין במכשיר אפליקציה שיכולה לפתוח את הקובץ")
    }

    fun uniqueTarget(dir: File, name: String): File {
        var t = File(dir, name)
        if (!t.exists()) return t
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var n = 2
        while (t.exists()) { t = File(dir, "$base ($n)$ext"); n++ }
        return t
    }

    fun paste(src: File, dir: File, cut: Boolean): Boolean {
        if (dir.path.startsWith(src.path + "/") || dir.path == src.path && src.isDirectory) return false
        val target = uniqueTarget(dir, src.name)
        return try {
            if (cut && src.renameTo(target)) return true
            val ok = src.copyRecursively(target, overwrite = false)
            if (ok && cut) src.deleteRecursively()
            ok
        } catch (e: Exception) {
            false
        }
    }

    fun sizeStr(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> String.format("%.0f KB", b / 1024.0)
        b < 1024L * 1024 * 1024 -> String.format("%.1f MB", b / 1048576.0)
        else -> String.format("%.2f GB", b / 1073741824.0)
    }

    fun kindOf(f: File): String {
        if (f.isDirectory) return "folder"
        return when (f.extension.lowercase()) {
            in Books.exts -> "book"
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "image"
            "mp3", "m4a", "wav", "ogg", "flac", "aac", "opus" -> "music"
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> "video"
            "zip", "rar", "7z", "tar", "gz" -> "archive"
            "apk" -> "apk"
            else -> "file"
        }
    }

    // ------------- home folders
    fun homeFolders(): List<String> {
        if (!Prefs.has("folders")) {
            val r = root()
            val def = (listOf("Books", "Download", "Documents", "Pictures", "Music")
                .map { File(r, it) }.filter { it.isDirectory }.map { it.path } +
                volumes().filter { it.removable }.map { it.dir.path }).take(6)
            Prefs.putList("folders", def)
            return def
        }
        return Prefs.list("folders")
    }

    fun addHomeFolder(path: String) {
        val l = homeFolders().toMutableList(); if (path !in l) l.add(path); Prefs.putList("folders", l)
    }

    fun removeHomeFolder(path: String) {
        val l = homeFolders().toMutableList(); l.remove(path); Prefs.putList("folders", l)
    }

    fun folderName(path: String): String {
        val custom = Prefs.s("fname:$path", "")
        if (custom.isNotEmpty()) return custom
        val f = File(path)
        if (f.path == root().path) return "אחסון"
        volumes().firstOrNull { it.dir.path == f.path }?.let { return it.name }
        return when (f.name) {
            "Books" -> "ספרים"
            "Download" -> "הורדות"
            "Documents" -> "מסמכים"
            "Pictures" -> "תמונות"
            "Music" -> "מוזיקה"
            "Movies" -> "סרטים"
            "DCIM" -> "מצלמה"
            "Notes", "note" -> "פתקים"
            else -> f.name
        }
    }
}

/* ================================================================ books */

object Books {
    val exts = setOf(
        "pdf", "epub", "mobi", "azw", "azw3", "prc", "fb2", "djvu", "djv", "txt",
        "cbz", "cbr", "chm", "rtf", "doc", "docx"
    )

    @Volatile
    var cache: List<File> = emptyList()
    @Volatile
    var scannedAt = 0L

    fun scan(force: Boolean = false): List<File> {
        if (!force && cache.isNotEmpty() && System.currentTimeMillis() - scannedAt < 120_000) return cache
        val out = ArrayList<File>()
        fun walk(d: File, depth: Int) {
            if (depth > 6 || out.size > 4000) return
            val list = d.listFiles() ?: return
            for (f in list) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    if (depth == 0 && (f.name == "Android" || f.name == "DCIM")) continue
                    walk(f, depth + 1)
                } else {
                    val e = f.extension.lowercase()
                    if (e in exts && (e != "txt" || f.length() > 20_000)) out.add(f)
                }
            }
        }
        for (v in Store.volumes()) {
            try { walk(v.dir, 0) } catch (e: Exception) { ignore(e) }
        }
        cache = out
        scannedAt = System.currentTimeMillis()
        return out
    }

    fun history(): Map<String, Long> {
        val m = HashMap<String, Long>()
        for (line in Prefs.list("hist")) {
            val i = line.indexOf('|')
            if (i > 0) m[line.substring(i + 1)] = line.substring(0, i).toLongOrNull() ?: 0L
        }
        return m
    }

    fun touch(path: String) {
        val h = history().toMutableMap()
        h[path] = System.currentTimeMillis()
        val lines = h.entries.sortedByDescending { it.value }.take(60).map { "${it.value}|${it.key}" }
        Prefs.putList("hist", lines)
    }

    fun clearHistory() = Prefs.put("hist", null)

    fun recent(n: Int): List<File> {
        val h = history()
        val opened = h.entries.sortedByDescending { it.value }.map { File(it.key) }.filter { it.exists() }
        val rest = cache.filter { it.path !in h }.sortedByDescending { it.lastModified() }
        return (opened + rest).take(n)
    }

    fun sorted(mode: String): List<File> = when (mode) {
        "name" -> cache.sortedBy { it.nameWithoutExtension.lowercase() }
        else -> {
            val h = history()
            cache.sortedWith(compareByDescending<File> { h[it.path] ?: 0L }.thenByDescending { it.lastModified() })
        }
    }
}

/* ================================================================ dock */

object Dock {
    val builtIn: List<Pair<String, String>> = listOf(
        "wifi" to "Wi‑Fi", "files" to "קבצים", "books" to "ספרים", "settings" to "הגדרות",
        "apps" to "כל האפליקציות", "calendar" to "לוח שנה", "control" to "מרכז בקרה",
        "refresh" to "רענון מסך", "bt" to "בלוטות׳", "sd" to "כרטיס זיכרון", "android" to "הגדרות אנדרואיד",
        "addwin" to "חלון חדש"
    )

    fun items(): List<String> =
        if (Prefs.has("dock")) Prefs.list("dock") else listOf("wifi", "files", "books", "settings")

    fun save(l: List<String>) = Prefs.putList("dock", l)

    fun setSlot(i: Int, k: String) {
        val l = items().toMutableList()
        if (i in l.indices) l[i] = k else l.add(k)
        save(l)
    }

    fun addApp(key: String) {
        val l = items().toMutableList()
        val k = "app:$key"
        if (k in l) return
        val e = l.indexOf("empty")
        if (e >= 0) l[e] = k
        else { if (l.size >= 6) l.removeAt(l.size - 1); l.add(k) }
        save(l)
    }

    fun glyph(k: String): String = when {
        k.startsWith("folder:") -> "folder"
        k.startsWith("book:") -> "book"
        else -> when (k) {
            "wifi" -> "wifi"; "files" -> "folder"; "books" -> "book"; "settings" -> "gear"
            "apps" -> "grid"; "calendar" -> "calendar"; "control" -> "sliders"; "refresh" -> "refresh"
            "bt" -> "bt"; "sd" -> "sd"; "android" -> "screen"; "addwin" -> "plus"; else -> "dots"
        }
    }

    fun shortLabel(k: String): String = when {
        k.startsWith("folder:") -> Store.folderName(k.removePrefix("folder:"))
        k.startsWith("book:") -> File(k.removePrefix("book:")).nameWithoutExtension
        k.startsWith("app:") -> Apps.byKey(k.removePrefix("app:"))?.let { Apps.name(it) } ?: "אפליקציה"
        else -> when (k) {
            "wifi" -> "Wi‑Fi"; "files" -> "קבצים"; "books" -> "ספרים"; "settings" -> "הגדרות"
            "apps" -> "אפליקציות"; "calendar" -> "לוח"; "control" -> "בקרה"; "refresh" -> "רענון"
            "bt" -> "בלוטות׳"; "sd" -> "כרטיס"; "android" -> "אנדרואיד"; "addwin" -> "חלון"; "empty" -> ""
            else -> ""
        }
    }

    fun describe(k: String): String = when {
        k == "empty" -> "ריק"
        k.startsWith("folder:") -> "תיקייה · " + shortLabel(k)
        k.startsWith("book:") -> "ספר · " + shortLabel(k)
        k.startsWith("app:") -> "אפליקציה · " + shortLabel(k)
        else -> builtIn.firstOrNull { it.first == k }?.second ?: k
    }
}

/** Home screen windows (pages); each is an ordered list of blocks. */
object Pages {
    val coreBlocks: List<Pair<String, String>> = listOf(
        "hero" to "שעון ותאריך", "books" to "ספרים אחרונים", "apps" to "אפליקציות", "folders" to "תיקיות"
    )
    val gadgetBlocks: List<Pair<String, String>> = listOf(
        "g:calendar" to "לוח חודשי", "g:battery" to "סוללה ואחסון", "g:note" to "פתק",
        "g:book" to "הספר שאני קורא", "g:clock" to "שעון ענק"
    )

    fun load(): MutableList<MutableList<String>> {
        if (!Prefs.has("pages")) {
            val first = mutableListOf("hero")
            first.addAll(Prefs.list("gadgets"))
            first.addAll(listOf("books", "apps", "folders"))
            return mutableListOf(first)
        }
        val l = Prefs.s("pages", "").split('\n')
            .map { p -> p.split(',').filter { it.isNotEmpty() }.toMutableList() }.toMutableList()
        if (l.isEmpty()) l.add(mutableListOf())
        return l
    }

    fun save(p: List<List<String>>) = Prefs.put("pages", p.joinToString("\n") { it.joinToString(",") })

    fun name(k: String): String =
        (coreBlocks + gadgetBlocks).firstOrNull { it.first == k }?.second ?: "גאדג׳ט"

    fun pageOf(p: List<List<String>>, k: String): Int = p.indexOfFirst { k in it }
}
/* ================================================================ covers */

object Covers {
    private const val S = 300
    private val mem = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val exec = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun load(ctx: Context, f: File, iv: ImageView) {
        val key = f.path + "@" + f.lastModified()
        iv.tag = key
        val m = mem.get(key)
        if (m != null) { iv.setImageBitmap(m); return }
        iv.setImageDrawable(null)
        val app = ctx.applicationContext
        exec.execute {
            val b = try { make(app, f, key) } catch (t: Throwable) { null }
            main.post { if (iv.tag == key && b != null) iv.setImageBitmap(b) }
        }
    }

    fun clear(ctx: Context) {
        mem.evictAll()
        File(ctx.cacheDir, "covers").deleteRecursively()
    }

    private fun make(ctx: Context, f: File, key: String): Bitmap {
        val disk = File(ctx.cacheDir, "covers/" + Integer.toHexString(key.hashCode()) + ".jpg")
        if (disk.exists()) {
            val b = BitmapFactory.decodeFile(disk.path)
            if (b != null) { mem.put(key, b); return b }
        }
        val src: Bitmap? = try {
            when (f.extension.lowercase()) {
                "pdf" -> pdf(f)
                "epub" -> epub(f)
                "cbz" -> cbz(f)
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
        val out = if (src != null) squareTop(src) else generated(f)
        try {
            disk.parentFile?.mkdirs()
            FileOutputStream(disk).use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        } catch (e: Exception) { ignore(e) }
        mem.put(key, out)
        return out
    }

    private fun pdf(f: File): Bitmap? {
        val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(pfd)
        try {
            if (r.pageCount == 0) return null
            val p = r.openPage(0)
            try {
                val w = 420
                val h = (w.toFloat() * p.height / maxOf(1, p.width)).toInt().coerceIn(1, 1400)
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                b.eraseColor(Color.WHITE)
                p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return b
            } finally {
                p.close()
            }
        } finally {
            r.close()
            pfd.close()
        }
    }

    private fun isImg(n: String): Boolean {
        val l = n.lowercase()
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".gif") || l.endsWith(".webp")
    }

    private fun decode(z: ZipFile, e: ZipEntry): Bitmap? {
        val bytes = z.getInputStream(e).use { it.readBytes() }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= 420) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o2)
    }

    private fun text(z: ZipFile, e: ZipEntry): String = z.getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun attr(tag: String, a: String): String? =
        Regex("\\b" + a + "\\s*=\\s*[\"']([^\"']*)[\"']").find(tag)?.groupValues?.get(1)

    private fun epub(f: File): Bitmap? {
        return ZipFile(f).use { z ->
            var href: String? = null
            val container = z.getEntry("META-INF/container.xml")?.let { text(z, it) }
            val opfPath = container?.let { Regex("full-path\\s*=\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
            if (opfPath != null) {
                val opf = z.getEntry(opfPath)?.let { text(z, it) }
                if (opf != null) {
                    val base = opfPath.substringBeforeLast('/', "")
                    val items = Regex("<item\\s[^>]*>").findAll(opf).map { it.value }.toList()
                    var item = items.firstOrNull { (attr(it, "properties") ?: "").contains("cover-image") }
                    if (item == null) {
                        val meta = Regex("<meta\\s[^>]*name\\s*=\\s*\"cover\"[^>]*>").find(opf)?.value
                        val coverId = meta?.let { attr(it, "content") }
                        if (coverId != null) item = items.firstOrNull { attr(it, "id") == coverId }
                    }
                    if (item == null) item = items.firstOrNull {
                        (attr(it, "href") ?: "").contains("cover", true) && (attr(it, "media-type") ?: "").startsWith("image")
                    }
                    val h = item?.let { attr(it, "href") }
                    if (h != null) href = if (base.isEmpty()) h else "$base/$h"
                }
            }
            var entry: ZipEntry? = null
            if (href != null) {
                val dec = try { URLDecoder.decode(href, "UTF-8") } catch (e: Exception) { href }
                entry = z.getEntry(dec) ?: z.getEntry(href)
                if (entry == null) {
                    val tail = dec.substringAfterLast('/')
                    entry = z.entries().asSequence().firstOrNull { it.name.endsWith(tail) }
                }
            }
            if (entry == null) entry = z.entries().asSequence().firstOrNull { isImg(it.name) && it.name.contains("cover", true) }
            if (entry == null) entry = z.entries().asSequence().firstOrNull { isImg(it.name) }
            entry?.let { decode(z, it) }
        }
    }

    private fun cbz(f: File): Bitmap? {
        return ZipFile(f).use { z ->
            val e = z.entries().asSequence().filter { !it.isDirectory && isImg(it.name) }.sortedBy { it.name }.firstOrNull()
            e?.let { decode(z, it) }
        }
    }

    private fun squareTop(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val crop = Bitmap.createBitmap(src, x, 0, side, side)
        return Bitmap.createScaledBitmap(crop, S, S, true)
    }

    private fun generated(f: File): Bitmap {
        val b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF111111.toInt()
        c.drawRect(RectF(S * 0.86f, 0f, S.toFloat(), S.toFloat()), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f
        c.drawRoundRect(RectF(16f, 16f, S * 0.86f - 16f, S - 16f), 14f, 14f, p)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt(); textSize = 34f; typeface = Typeface.DEFAULT_BOLD
        }
        val title = f.nameWithoutExtension.replace('_', ' ')
        val w = (S * 0.86f - 64f).toInt()
        val sl = StaticLayout.Builder.obtain(title, 0, title.length, tp, w)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(4)
            .setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        c.save()
        c.translate(32f, (S - sl.height) / 2f - 14f)
        sl.draw(c)
        c.restore()
        val ext = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF555555.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText(f.extension.uppercase(), S * 0.43f, S - 42f, ext)
        return b
    }
}

/* ================================================================ accessibility helper */

class ActionService : AccessibilityService() {
    companion object {
        @Volatile
        var inst: ActionService? = null

        fun act(action: Int): Boolean = inst?.performGlobalAction(action) ?: false

        fun enabled(ctx: Context): Boolean {
            val s = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return s.contains(ctx.packageName + "/")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        inst = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        inst = null
        super.onDestroy()
    }
}
````

---

## `app/src/main/java/com/chdv/haboox/Editors.kt`

````kotlin
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
````

---

## `app/src/main/java/com/chdv/haboox/FilesActivity.kt`

````kotlin
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
````

---

## `app/src/main/java/com/chdv/haboox/Heb.kt`

````kotlin
package com.chdv.haboox

import android.icu.util.HebrewCalendar
import java.util.Locale

/** Offline Hebrew calendar: dates, numerals and holidays. */
object Heb {
    private val ICAL_YEAR = android.icu.util.Calendar.YEAR
    private val ICAL_MONTH = android.icu.util.Calendar.MONTH
    private val ICAL_DAY = android.icu.util.Calendar.DAY_OF_MONTH
    private val ICAL_DOW = android.icu.util.Calendar.DAY_OF_WEEK
    private val ICAL_JD = android.icu.util.Calendar.JULIAN_DAY

    const val SUN = 1
    const val MON = 2
    const val THU = 5
    const val FRI = 6
    const val SAT = 7

    private val months = arrayOf(
        "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר",
        "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    )
    val dayNames = arrayOf("", "ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
    val dayLetters = arrayOf("", "א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")

    class HD(val y: Int, val m: Int, val d: Int, val dow: Int, val jd: Int)

    fun isLeap(y: Int): Boolean = ((7 * y + 1) % 19) < 7

    fun monthName(y: Int, m: Int): String = when {
        m == 6 && isLeap(y) -> "אדר ב׳"
        m == 5 && !isLeap(y) -> "אדר"
        else -> months[m]
    }

    fun of(millis: Long): HD {
        val c = HebrewCalendar()
        c.timeInMillis = millis
        return HD(c.get(ICAL_YEAR), c.get(ICAL_MONTH), c.get(ICAL_DAY), c.get(ICAL_DOW), c.get(ICAL_JD))
    }

    private fun jdOf(y: Int, m: Int, d: Int): Int = HebrewCalendar(y, m, d).get(ICAL_JD)

    private fun dowOfJd(jd: Int): Int = ((jd + 1) % 7) + 1

    /** Hebrew numerals (gematria) with geresh / gershayim. */
    fun gem(num: Int): String {
        var x = num
        val sb = StringBuilder()
        val hundreds = intArrayOf(400, 300, 200, 100)
        val hl = arrayOf("ת", "ש", "ר", "ק")
        while (x >= 100) {
            for (i in hundreds.indices) {
                if (x >= hundreds[i]) { sb.append(hl[i]); x -= hundreds[i]; break }
            }
        }
        if (x == 15) { sb.append("טו"); x = 0 }
        if (x == 16) { sb.append("טז"); x = 0 }
        val tens = arrayOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
        val ones = arrayOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
        sb.append(tens[x / 10]).append(ones[x % 10])
        val s = sb.toString()
        return when {
            s.isEmpty() -> ""
            s.length == 1 -> "$s׳"
            else -> s.substring(0, s.length - 1) + "״" + s.last()
        }
    }

    fun yearStr(y: Int): String = gem(y % 1000)

    fun format(h: HD): String = "${gem(h.d)} ב${monthName(h.y, h.m)} ${yearStr(h.y)}"
    fun formatShort(h: HD): String = "${gem(h.d)} ${monthName(h.y, h.m)}"

    fun omer(h: HD): Int {
        val o = h.jd - jdOf(h.y, 7, 16) + 1
        return if (o in 1..49) o else 0
    }

    fun events(h: HD): List<String> {
        val r = ArrayList<String>()
        val y = h.y
        val m = h.m
        val d = h.d
        val dow = h.dow
        val leap = isLeap(y)
        when (m) {
            0 -> {
                when (d) {
                    1, 2 -> r.add("ראש השנה")
                    9 -> r.add("ערב יום כיפור")
                    10 -> r.add("יום כיפור")
                    14 -> r.add("ערב סוכות")
                    15 -> r.add("סוכות")
                    in 16..20 -> r.add("חול המועד סוכות")
                    21 -> r.add("הושענא רבה")
                    22 -> r.add("שמיני עצרת · שמחת תורה")
                }
                if ((d == 3 && dow != SAT) || (d == 4 && dow == SUN)) r.add("צום גדליה")
            }
            3 -> if (d == 10) r.add("עשרה בטבת")
            4 -> if (d == 15) r.add("ט״ו בשבט")
            5 -> if (leap) {
                if (d == 14) r.add("פורים קטן")
                if (d == 15) r.add("שושן פורים קטן")
            }
            6 -> {
                if ((d == 13 && dow != SAT) || (d == 11 && dow == THU)) r.add("תענית אסתר")
                if (d == 14) r.add("פורים")
                if (d == 15) r.add("שושן פורים")
            }
            7 -> {
                when (d) {
                    14 -> r.add("ערב פסח")
                    15 -> r.add("פסח")
                    in 16..20 -> r.add("חול המועד פסח")
                    21 -> r.add("שביעי של פסח")
                }
                if ((d == 27 && dow != FRI && dow != SUN) || (d == 26 && dow == THU) || (d == 28 && dow == MON))
                    r.add("יום השואה")
            }
            8 -> {
                val dw5 = dowOfJd(jdOf(y, 8, 5))
                val atz = when (dw5) { FRI -> 4; SAT -> 3; MON -> 6; else -> 5 }
                if (d == atz - 1) r.add("יום הזיכרון")
                if (d == atz) r.add("יום העצמאות")
                if (d == 14) r.add("פסח שני")
                if (d == 18) r.add("ל״ג בעומר")
                if (d == 28) r.add("יום ירושלים")
            }
            9 -> {
                if (d == 5) r.add("ערב שבועות")
                if (d == 6) r.add("שבועות")
            }
            10 -> if ((d == 17 && dow != SAT) || (d == 18 && dow == SUN)) r.add("צום י״ז בתמוז")
            11 -> {
                if ((d == 9 && dow != SAT) || (d == 10 && dow == SUN)) r.add("תשעה באב")
                if (d == 15) r.add("ט״ו באב")
            }
            12 -> if (d == 29) r.add("ערב ראש השנה")
        }
        val k = h.jd - jdOf(y, 2, 25)
        if (k in 0..7) r.add("חנוכה · נר ${gem(k + 1)}")
        if (d == 30) {
            var nm = m + 1
            if (nm == 5 && !leap) nm = 6
            if (nm <= 12) r.add("ראש חודש ${monthName(y, nm)}")
        }
        if (d == 1 && m != 0) r.add("ראש חודש ${monthName(y, m)}")
        return r
    }

    fun gregLong(millis: Long): String =
        java.text.SimpleDateFormat("EEEE, d בMMMM yyyy", Locale("he")).format(java.util.Date(millis))
}
````

---

## `app/src/main/java/com/chdv/haboox/HomeActivity.kt`

````kotlin
package com.chdv.haboox

import android.accessibilityservice.AccessibilityService
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Home screen made of "windows" (pages). Each window holds blocks: hero (clock), books, apps,
 * folders and gadgets. Apps that don't fit spill over into automatic extra windows.
 */
class HomeActivity : BaseActivity() {

    companion object {
        private const val HOST_ID = 4711
        private const val REQ_BIND = 41
        private const val REQ_CONF = 42
    }

    private class Ref(val user: Int, val appsFrom: Int)

    private lateinit var root: FrameLayout
    private lateinit var mainCol: LinearLayout
    private lateinit var pageHost: FrameLayout
    private lateinit var dots: LinearLayout
    private lateinit var dockBox: FrameLayout
    private lateinit var banner: LinearLayout
    private lateinit var statusBat: TextView
    private lateinit var statusBatIcon: ImageView
    private lateinit var statusWifi: ImageView
    private var panel: View? = null

    // hero references (only when the hero is on the visible window)
    private var clockTv: TextView? = null
    private var greetTv: TextView? = null
    private var hebTv: TextView? = null
    private var gregTv: TextView? = null
    private var eventTv: TextView? = null
    private val bigClocks = ArrayList<TextView>()

    private lateinit var host: AppWidgetHost
    private lateinit var awm: AppWidgetManager
    private val widgetViews = HashMap<Int, AppWidgetHostView>()
    private var pendingWidget = -1
    private var pendingPage = 0

    private var apps: List<AppEntry> = emptyList()
    private var userPages: MutableList<MutableList<String>> = mutableListOf()
    private var refs: List<Ref> = emptyList()
    private var cur = 0
    private val baseShown = HashMap<Int, Int>()
    private var fullPer = 0
    private var sig = ""
    private var lastBookCount = -1

    private var downX = 0f
    private var downY = 0f
    private var downT = 0L

    private val tick = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = updateClock()
    }
    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = reloadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sig = Look.sig()
        awm = AppWidgetManager.getInstance(this)
        host = AppWidgetHost(applicationContext, HOST_ID)
        userPages = Pages.load()
        build()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panel != null) hidePanel() else if (cur != 0) { cur = 0; renderPage() }
            }
        })
        val pf = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val sdf = IntentFilter().apply {
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
            addAction(Intent.ACTION_USER_UNLOCKED)
            addAction(Intent.ACTION_MEDIA_MOUNTED)
        }
        try { registerReceiver(pkgReceiver, pf) } catch (e: Exception) { ignore(e) }
        try { registerReceiver(pkgReceiver, sdf) } catch (e: Exception) { ignore(e) }
        handleExtras(intent)
    }

    override fun onDestroy() {
        try { unregisterReceiver(pkgReceiver) } catch (e: Exception) { ignore(e) }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        try { host.startListening() } catch (e: Exception) { ignore(e) }
    }

    override fun onStop() {
        super.onStop()
        try { host.stopListening() } catch (e: Exception) { ignore(e) }
    }

    override fun onResume() {
        super.onResume()
        if (Look.sig() != sig) { recreate(); return }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(tick, f)
        userPages = Pages.load()
        renderBanner()
        renderDock()
        renderPage()
        reloadApps()
        scanBooks()
        if (Prefs.b("autoUpdate", true)) Updater.dailyCheck(this)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(tick) } catch (e: Exception) { ignore(e) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        hidePanel()
        if (cur != 0) { cur = 0; renderPage() }
        handleExtras(intent)
    }

    private fun handleExtras(i: Intent?) {
        if (i != null && i.getBooleanExtra("gadgets", false)) {
            i.removeExtra("gadgets")
            root.post { addBlockMenu(curUser()) }
        }
    }

    private fun curUser(): Int = refs.getOrNull(cur)?.user ?: 0

    private fun savePages() = Pages.save(userPages)

    /* ------------------------------------------------------------ frame */

    private fun build() {
        root = FrameLayout(this).apply { setBackgroundColor(pal.bg) }
        mainCol = vbox().apply { setPadding(dp(14), dp(4), dp(14), dp(10)) }

        // wide control-center button (instead of a thin pull bar)
        val bar = hbox().apply {
            background = pressBg(pal.card, pal.card2, 20, if (pal.night || pal.vivid) 0 else dp(1), pal.line)
            setPadding(dp(14), dp(7), dp(12), dp(7))
            setOnClickListener { showPanel() }
        }
        bar.addView(glyphView("sliders", pal.ink, 20))
        bar.addView(label("מרכז בקרה", 14.5f, pal.text, true), lp(WRAP, WRAP).m(dp(8), 0, dp(4), 0))
        bar.addView(glyphView("chev_down", pal.sub, 16))
        bar.addView(View(this), lp(0, 1, 1f))
        statusWifi = ImageView(this)
        bar.addView(statusWifi, lp(dp(18), dp(18)).m(0, 0, dp(10), 0))
        statusBat = label("", 13.5f, pal.text, true)
        bar.addView(statusBat)
        statusBatIcon = ImageView(this)
        bar.addView(statusBatIcon, lp(dp(24), dp(24)).m(dp(4), 0, 0, 0))
        mainCol.addView(bar, lp(MATCH, WRAP))

        banner = vbox().apply { visibility = View.GONE }
        mainCol.addView(banner, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        pageHost = FrameLayout(this)
        pageHost.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) pageHost.post { computeFullPer(); renderPage() }
        }
        mainCol.addView(pageHost, lp(MATCH, 0, 1f))

        dots = hbox().apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) }
        mainCol.addView(dots, lp(MATCH, WRAP))

        dockBox = FrameLayout(this)
        mainCol.addView(dockBox, lp(MATCH, WRAP).m(0, dp(8), 0, 0))

        root.addView(mainCol, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun cellHeight(): Int {
        val labels = Prefs.b("labels", true)
        return dp(if (labels) (60 + 30 * Look.scale()).toInt() else 76)
    }

    private fun computeFullPer() {
        val h = pageHost.height - dp(40)
        if (h <= 0) return
        fullPer = maxOf(1, h / cellHeight()) * Prefs.i("cols", 4)
    }

    private fun computeRefs() {
        val out = ArrayList<Ref>()
        userPages.forEachIndexed { ui, blocks ->
            out.add(Ref(ui, -1))
            if ("apps" in blocks && fullPer > 0 && apps.isNotEmpty()) {
                var off = baseShown[ui] ?: (fullPer / 2)
                while (off < apps.size) { out.add(Ref(ui, off)); off += fullPer }
            }
        }
        refs = out
        cur = cur.coerceIn(0, maxOf(0, refs.size - 1))
    }

    private fun goToUserPage(ui: Int) {
        computeRefs()
        val i = refs.indexOfFirst { it.user == ui && it.appsFrom < 0 }
        if (i >= 0) cur = i
        renderPage()
    }

    /* ------------------------------------------------------------ rendering */

    private fun renderPage() {
        clockTv = null; greetTv = null; hebTv = null; gregTv = null; eventTv = null
        bigClocks.clear()
        if (userPages.isEmpty()) userPages.add(mutableListOf())
        computeRefs()
        pageHost.removeAllViews()
        val ref = refs.getOrNull(cur) ?: return
        val v = if (ref.appsFrom >= 0) appsOnlyPage(ref.appsFrom) else basePage(ref.user)
        pageHost.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        renderDots()
        updateClock()
    }

    private fun renderDots() {
        dots.removeAllViews()
        if (refs.size <= 1 && !Prefs.b("showAddWin", true)) { dots.visibility = View.GONE; return }
        dots.visibility = View.VISIBLE
        refs.forEachIndexed { i, r ->
            val active = i == cur
            val d = View(this).apply {
                background = if (active) roundBg(pal.ink, dp(4).toFloat())
                else if (r.appsFrom >= 0) roundBg(pal.sub, dp(1).toFloat())
                else roundBg(Color.TRANSPARENT, dp(4).toFloat(), dp(1), pal.sub)
            }
            val wrap = FrameLayout(this).apply {
                addView(d, FrameLayout.LayoutParams(dp(if (active) 20 else 8), dp(8), Gravity.CENTER))
                setOnClickListener { cur = i; renderPage() }
            }
            dots.addView(wrap, lp(dp(if (active) 28 else 18), dp(24)))
        }
        if (Prefs.b("showAddWin", true)) {
            val plus = FrameLayout(this).apply {
                addView(ImageView(context).apply { setImageDrawable(Glyph("plus", pal.sub, 2.2f)) },
                    FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
                setOnClickListener { newPage() }
            }
            dots.addView(plus, lp(dp(28), dp(24)).m(dp(6), 0, 0, 0))
        }
    }

    private fun basePage(ui: Int): View {
        val col = vbox().apply {
            isLongClickable = true
            setOnLongClickListener { homeMenu(); true }
        }
        val blocks = userPages[ui]
        if (blocks.isEmpty()) {
            val c = vbox().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                background = roundBg(pal.card, dp(22).toFloat(), if (pal.night) 0 else dp(1), pal.line)
                setPadding(dp(18), dp(22), dp(18), dp(22))
            }
            c.addView(glyphView("widget", pal.ink, 42))
            c.addView(label("חלון ריק", 19f, pal.text, true).apply { setPadding(0, dp(10), 0, 0) })
            c.addView(label("אפשר להוסיף לכאן שעון, ספרים, אפליקציות, תיקיות או גאדג׳טים. לחיצה ארוכה על המסך פותחת את האפשרויות.", 14f, pal.sub).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(14))
            })
            c.addView(pillButton("הוספת רכיב") { addBlockMenu(ui) })
            if (userPages.size > 1) c.addView(pillButton("סגירת החלון", filled = false) { closePage(ui) }, lp(WRAP, WRAP).m(0, dp(8), 0, 0))
            col.addView(c, lp(MATCH, WRAP).m(0, dp(10), 0, 0))
            col.addView(View(this), lp(MATCH, 0, 1f))
            return col
        }
        val flex = if ("apps" in blocks) "apps" else if ("books" in blocks) "books" else null
        blocks.forEachIndexed { bi, k ->
            val isFlex = k == flex
            val v = blockView(k, ui, bi, isFlex) ?: return@forEachIndexed
            col.addView(v, if (isFlex) lp(MATCH, 0, 1f).m(0, dp(10), 0, 0) else lp(MATCH, WRAP).m(0, dp(10), 0, 0))
        }
        if (flex == null) col.addView(View(this), lp(MATCH, 0, 1f))
        return col
    }

    private fun blockView(k: String, ui: Int, bi: Int, flex: Boolean): View? {
        val onLong = { blockMenu(k, ui, bi) }
        return when (k) {
            "hero" -> heroView(onLong)
            "books" -> booksBlock(flex, onLong)
            "apps" -> appsBlock(ui, onLong)
            "folders" -> foldersBlock(onLong)
            "g:calendar" -> miniCalendar(bi, onLong)
            "g:battery" -> batteryGadget(bi, onLong)
            "g:note" -> noteGadget(bi, onLong)
            "g:book" -> bookGadget(bi, onLong)
            "g:clock" -> clockGadget(bi, onLong)
            else -> if (k.startsWith("w:")) androidWidget(k, onLong) else null
        }
    }

    private fun sectionTitle(glyph: String, t: String, link: String?, onLong: () -> Unit, onLink: (() -> Unit)?): View {
        val r = hbox()
        val badge = FrameLayout(this).apply {
            background = roundBg(if (pal.pastel) pal.tile(t.length) else pal.ink, dp(8).toFloat())
            addView(ImageView(context).apply { setImageDrawable(Glyph(glyph, if (pal.pastel) pal.ink else pal.onInk, 2.1f)) },
                FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
        }
        r.addView(badge, lp(dp(26), dp(26)))
        r.addView(label(t, 17f, pal.text, true), lp(0, WRAP, 1f).m(dp(8), 0, 0, 0))
        if (link != null && onLink != null) {
            val l = hbox().apply {
                addView(label(link, 14.5f, pal.sub, true))
                addView(glyphView("chev_l", pal.sub, 16))
                setPadding(dp(10), dp(4), dp(6), dp(4))
                background = pressBg(pal.card, pal.card2, 14)
                setOnClickListener { onLink() }
            }
            r.addView(l, lp(WRAP, WRAP))
        }
        val more = FrameLayout(this).apply {
            addView(ImageView(context).apply { setImageDrawable(Glyph("dots", pal.sub)) }, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            background = pressBg(Color.TRANSPARENT, pal.card2, 14)
            setOnClickListener { onLong() }
        }
        r.addView(more, lp(dp(34), dp(30)).m(dp(4), 0, 0, 0))
        r.setPadding(0, 0, 0, dp(7))
        r.setOnLongClickListener { onLong(); true }
        return r
    }

    /* ------------------------------------------------------------ hero */

    private fun heroView(onLong: () -> Unit): View {
        val hero = vbox().apply {
            background = Look.heroBg(pal, dp(26).toFloat())
            clipToOutline = true
            setPadding(dp(20), dp(14), dp(20), dp(16))
            if (pal.vivid) elevation = dp(4).toFloat()
            setOnClickListener { go(Intent(context, CalendarActivity::class.java)) }
            setOnLongClickListener { onLong(); true }
        }
        val top = hbox()
        val g = label("", 15f, pal.heroSub, true)
        greetTv = g
        top.addView(g, lp(0, WRAP, 1f))
        top.addView(ImageView(this).apply { setImageDrawable(Glyph("calendar", pal.heroSub)) }, lp(dp(20), dp(20)))
        hero.addView(top, lp(MATCH, WRAP))
        val mid = hbox().apply { gravity = Gravity.BOTTOM }
        val c = TextView(this).apply {
            setTextColor(pal.heroText)
            textSize = Look.clockSize()
            typeface = Look.heavy
            includeFontPadding = false
            textDirection = View.TEXT_DIRECTION_LTR
            letterSpacing = -0.02f
        }
        clockTv = c
        mid.addView(c, lp(WRAP, WRAP))
        mid.addView(View(this), lp(0, 1, 1f))
        val dates = vbox().apply { gravity = Gravity.END }
        val h = label("", 20f, pal.heroText, true).oneLine().apply { gravity = Gravity.END }
        val gr = label("", 13.5f, pal.heroSub).oneLine().apply { gravity = Gravity.END }
        hebTv = h; gregTv = gr
        dates.addView(h, lp(WRAP, WRAP))
        dates.addView(gr, lp(WRAP, WRAP))
        mid.addView(dates, lp(WRAP, WRAP).m(dp(10), 0, 0, dp(8)))
        hero.addView(mid, lp(MATCH, WRAP).m(0, dp(2), 0, 0))
        val ev = label("", 13.5f, pal.heroText, true).apply {
            background = roundBg(if (Color.red(pal.heroText) > 128) 0x33FFFFFF else 0x1A000000, dp(12).toFloat())
            setPadding(dp(12), dp(4), dp(12), dp(5))
            visibility = View.GONE
        }
        eventTv = ev
        val evWrap = hbox().apply { addView(ev, lp(WRAP, WRAP)) }
        hero.addView(evWrap, lp(MATCH, WRAP).m(0, dp(8), 0, 0))
        return hero
    }

    private fun greeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "בוקר טוב"
            in 12..16 -> "צהריים טובים"
            in 17..20 -> "ערב טוב"
            else -> "לילה טוב"
        }
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val h = Heb.of(now)
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))
        clockTv?.let {
            it.visibility = if (Prefs.b("showClock", true)) View.VISIBLE else View.GONE
            it.text = time
        }
        greetTv?.text = greeting() + " · יום " + Heb.dayNames[h.dow]
        gregTv?.let {
            it.visibility = if (Prefs.b("showGreg", true)) View.VISIBLE else View.GONE
            it.text = SimpleDateFormat("d בMMMM yyyy", Locale("he")).format(Date(now))
        }
        hebTv?.let {
            it.visibility = if (Prefs.b("showHeb", true)) View.VISIBLE else View.GONE
            it.text = Heb.format(h)
        }
        eventTv?.let { evTv ->
            val ev = ArrayList(Heb.events(h))
            if (h.dow == Heb.SAT) ev.add(0, "שבת שלום")
            val om = Heb.omer(h)
            if (om > 0) ev.add("${Heb.gem(om)} לעומר")
            if (Prefs.b("showEvents", true) && ev.isNotEmpty()) {
                evTv.text = ev.joinToString(" · ")
                evTv.visibility = View.VISIBLE
            } else evTv.visibility = View.GONE
        }
        for (b in bigClocks) b.text = time
        val bat = batteryLevel()
        statusBat.text = if (bat >= 0) "$bat%" else ""
        statusBatIcon.setImageDrawable(Glyph("battery", pal.text).apply { fill = if (bat >= 0) bat / 100f else 1f })
        statusWifi.setImageDrawable(if (wifiOn()) Glyph("wifi", pal.text) else null)
    }

    private fun wifiOn(): Boolean = try {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } catch (e: Exception) { false }

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
        if (needFiles) parts.add("גישה לקבצים ולכרטיס הזיכרון")
        if (needHome) parts.add("קביעה כמסך הבית")
        txt.addView(label(parts.joinToString(" · "), 13f, pal.sub))
        c.addView(txt, lp(0, WRAP, 1f))
        c.addView(pillButton("השלמה") {
            if (needFiles) askFiles() else go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        }.apply { setTextSize(14f) })
        if (!needFiles) c.addView(iconButton("close") { Prefs.put("bannerHomeOff", true); renderBanner() })
        banner.addView(c, lp(MATCH, WRAP))
    }

    /* ------------------------------------------------------------ apps */

    private fun reloadApps() {
        Thread {
            Apps.load(this)
            val o = Apps.ordered()
            runOnUiThread {
                val changed = o.map { it.key } != apps.map { it.key }
                apps = o
                if (changed) renderPage()
                renderDock()
            }
        }.start()
    }

    private fun appsBlock(ui: Int, onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("grid", "אפליקציות", "הכול", onLong) { go(Intent(this, AppsActivity::class.java)) })
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) area.post {
                val shown = fillApps(area, 0)
                if (baseShown[ui] != shown) { baseShown[ui] = shown; computeRefs(); renderDots() }
            }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    private fun appsOnlyPage(offset: Int): View {
        val box = vbox().apply {
            isLongClickable = true
            setOnLongClickListener { homeMenu(); true }
        }
        val head = hbox().apply { setPadding(0, dp(10), 0, dp(6)) }
        head.addView(label("אפליקציות", 17f, pal.text, true), lp(0, WRAP, 1f))
        head.addView(label("${offset + 1}–${minOf(apps.size, offset + fullPer)} מתוך ${apps.size}", 13f, pal.sub, true))
        box.addView(head, lp(MATCH, WRAP))
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, _, t, _, b, _, ot, _, ob ->
            if (b - t != ob - ot) area.post { fillApps(area, offset) }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    /** Fills a grid with as many apps as fit; returns how many were shown. */
    private fun fillApps(area: FrameLayout, offset: Int): Int {
        val h = area.height
        if (h <= 0) return 0
        area.removeAllViews()
        if (apps.isEmpty()) {
            area.addView(label("טוען אפליקציות…", 15f, pal.sub).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(MATCH, MATCH))
            return 0
        }
        val cols = Prefs.i("cols", 4)
        val rows = maxOf(1, h / cellHeight())
        val iconSize = if (cols >= 5) 50 else if (cols == 3) 64 else 56
        val grid = vbox()
        var shown = 0
        for (r in 0 until rows) {
            val row = hbox().apply { gravity = Gravity.TOP }
            for (c in 0 until cols) {
                val idx = offset + r * cols + c
                val a = apps.getOrNull(idx)
                if (a == null) { row.addView(View(this), lp(0, MATCH, 1f)); continue }
                row.addView(appCell(a, idx, iconSize), lp(0, MATCH, 1f))
                shown++
            }
            grid.addView(row, lp(MATCH, 0, 1f))
        }
        area.addView(grid, FrameLayout.LayoutParams(MATCH, MATCH))
        return shown
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

    private fun moveApp(a: AppEntry, to: Int) {
        val l = apps.toMutableList()
        val from = l.indexOfFirst { it.key == a.key }
        if (from < 0) return
        l.removeAt(from)
        l.add(to.coerceIn(0, l.size), a)
        Apps.saveOrder(l)
        apps = l
        renderPage()
    }

    private fun appMenu(a: AppEntry) {
        val pos = apps.indexOfFirst { it.key == a.key }
        Sheet.actions(this, Apps.name(a), listOf(
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם חדש למסך הבית", Apps.name(a), a.label) { s ->
                    Prefs.put("name:" + a.key, if (s.isEmpty() || s == a.label) null else s)
                    apps = Apps.ordered(); renderPage(); renderDock()
                }
            },
            Sheet.Item("שינוי סמל") { iconMenu(a) },
            Sheet.Item("הזזה קדימה") { moveApp(a, pos - 1) },
            Sheet.Item("הזזה אחורה") { moveApp(a, pos + 1) },
            Sheet.Item("העברה לתחילה") { moveApp(a, 0) },
            Sheet.Item("העברה לסוף") { moveApp(a, apps.size) },
            Sheet.Item("הוספה לשורה התחתונה") { Dock.addApp(a.key); renderDock() },
            Sheet.Item("הסרה ממסך הבית") {
                Apps.hide(a); apps = Apps.ordered(); renderPage()
                toast("אפשר להחזיר מ'כל האפליקציות' או מההגדרות")
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
            Prefs.put("icon:" + a.key, v); renderPage(); renderDock()
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

    /* ------------------------------------------------------------ books */

    private fun scanBooks() {
        if (!hasFiles()) return
        Thread {
            Books.scan()
            val n = Books.cache.size
            runOnUiThread {
                if (n != lastBookCount) {
                    lastBookCount = n
                    if (userPages.getOrNull(curUser())?.any { it == "books" || it == "g:book" } == true) renderPage()
                }
            }
        }.start()
    }

    private fun booksBlock(flex: Boolean, onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("book", "ספרים אחרונים", "הכול", onLong) { go(Intent(this, BooksActivity::class.java)) })
        if (!hasFiles()) {
            box.addView(pillButton("אישור גישה לקבצים כדי להציג ספרים", filled = false) { askFiles() }.apply { setTextSize(14f) })
            return box
        }
        if (!flex) {
            val grid = vbox()
            drawBookRows(grid, 1)
            box.addView(grid, lp(MATCH, WRAP))
            return box
        }
        val area = FrameLayout(this)
        area.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, orr, ob ->
            if (b - t != ob - ot || r - l != orr - ol) area.post {
                val w = area.width
                val rowH = (w - dp(30)) / 4 + dp(26)
                val rows = maxOf(1, area.height / maxOf(1, rowH))
                area.removeAllViews()
                val grid = vbox()
                drawBookRows(grid, rows)
                area.addView(grid, FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT))
            }
        }
        box.addView(area, lp(MATCH, 0, 1f))
        return box
    }

    private fun drawBookRows(target: LinearLayout, rows: Int) {
        val list = Books.recent(rows * 4)
        if (list.isEmpty()) {
            target.addView(label("לא נמצאו עדיין ספרים במכשיר או בכרטיס הזיכרון", 13f, pal.sub))
            return
        }
        for (r in 0 until rows) {
            val row = hbox().apply { gravity = Gravity.TOP }
            for (i in 0 until 4) {
                val idx = r * 4 + i
                val f = list.getOrNull(idx)
                val cell = vbox()
                if (f != null) {
                    val sq = SquareFrame(this).apply {
                        background = roundBg(pal.card, dp(14).toFloat(), if (pal.vivid) 0 else dp(1), pal.line)
                        clipToOutline = true
                        if (pal.vivid) elevation = dp(3).toFloat()
                    }
                    val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                    Covers.load(this, f, iv)
                    if (idx == 0) {
                        val tag = label("ממשיכים", 10.5f, pal.onInk, true).apply {
                            background = roundBg(pal.ink, dp(8).toFloat())
                            setPadding(dp(7), dp(2), dp(7), dp(3))
                        }
                        sq.addView(tag, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
                            setMargins(dp(6), 0, dp(6), dp(6))
                        })
                    }
                    sq.setOnClickListener { Store.open(this, f) }
                    sq.setOnLongClickListener { bookMenu(f); true }
                    cell.addView(sq, lp(MATCH, WRAP))
                    cell.addView(label(f.nameWithoutExtension, 12f, pal.text).oneLine().apply { setPadding(dp(2), dp(5), dp(2), 0) })
                }
                row.addView(cell, lp(0, WRAP, 1f).m(if (i == 0) 0 else dp(5), 0, if (i == 3) 0 else dp(5), 0))
            }
            target.addView(row, lp(MATCH, WRAP).m(0, if (r == 0) 0 else dp(8), 0, 0))
        }
    }

    private fun bookMenu(f: File) {
        Sheet.actions(this, f.nameWithoutExtension, listOf(
            Sheet.Item("פתיחה") { Store.open(this, f) },
            Sheet.Item("פתיחה באמצעות…") { Store.open(this, f, chooser = true) },
            Sheet.Item("הצגה בתיקייה") { go(Intent(this, FilesActivity::class.java).putExtra("path", f.parent)) },
            Sheet.Item("שמירה בשורה התחתונה") { Dock.setSlot(Dock.items().indexOf("empty").let { if (it < 0) Dock.items().size else it }, "book:" + f.path); renderDock() },
            Sheet.Item("כל הספרים") { go(Intent(this, BooksActivity::class.java)) }
        ))
    }

    /* ------------------------------------------------------------ folders */

    private fun foldersBlock(onLong: () -> Unit): View {
        val box = vbox()
        box.addView(sectionTitle("folder", "תיקיות", "הוספה", onLong) { pickFolder() })
        val row = hbox()
        val all = Store.homeFolders()
        val visible = if (all.size > 4) all.take(3) else all
        visible.forEachIndexed { i, path -> row.addView(folderTile(path, i), lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0)) }
        if (all.size > 4) {
            row.addView(tile("dots", "עוד ${all.size - 3}", 3) { moreFolders(all) }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
        } else if (all.size < 4) {
            row.addView(tile("plus", "הוספה", all.size) { pickFolder() }, lp(0, WRAP, 1f).m(dp(4), 0, dp(4), 0))
            for (x in all.size + 1 until 4) row.addView(View(this), lp(0, 1, 1f).m(dp(4), 0, dp(4), 0))
        }
        box.addView(row, lp(MATCH, WRAP))
        return box
    }

    private fun tile(kind: String, text: String, i: Int, onClick: () -> Unit): LinearLayout = vbox().apply {
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(10), dp(4), dp(8))
        val bgc = if (pal.pastel) pal.tile(i + 2) else pal.card
        background = pressBg(bgc, pal.card2, 16)
        if (pal.vivid) elevation = dp(2).toFloat()
        addView(glyphView(kind, pal.ink, 28))
        addView(label(text, 12.5f, pal.text, pal.vivid).oneLine().apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }, lp(MATCH, WRAP))
        setOnClickListener { onClick() }
    }

    private fun openFolder(path: String) {
        val isVol = Store.isVolumeRoot(path) && path != Store.root().path
        if (isVol) go(Intent(this, FilesActivity::class.java).putExtra("volume", path))
        else go(Intent(this, FilesActivity::class.java).putExtra("path", path))
    }

    private fun folderTile(path: String, i: Int): View {
        val isVol = Store.isVolumeRoot(path) && path != Store.root().path
        val t = tile(if (isVol) "sd" else "folder", Store.folderName(path), i) { openFolder(path) }
        t.setOnLongClickListener { folderMenu(path); true }
        return t
    }

    private fun folderMenu(path: String) {
        Sheet.actions(this, Store.folderName(path), listOf(
            Sheet.Item("פתיחה") { openFolder(path) },
            Sheet.Item("שינוי שם") {
                Sheet.input(this, "שם התיקייה במסך הבית", Store.folderName(path), File(path).name) { s ->
                    Prefs.put("fname:$path", if (s.isEmpty()) null else s); renderPage()
                }
            },
            Sheet.Item("שמירה בשורה התחתונה") {
                Dock.setSlot(Dock.items().indexOf("empty").let { if (it < 0) Dock.items().size else it }, "folder:$path"); renderDock()
            },
            Sheet.Item("הסרה ממסך הבית", red = true) { Store.removeHomeFolder(path); renderPage() }
        ))
    }

    private fun moreFolders(all: List<String>) {
        Sheet.actions(this, "תיקיות", all.map { p ->
            Sheet.Item(Store.folderName(p)) { openFolder(p) }
        } + Sheet.Item("הוספת תיקייה…") { pickFolder() })
    }

    private fun pickFolder() {
        if (!hasFiles()) { askFiles(); return }
        go(Intent(this, FilesActivity::class.java).putExtra("pick", true))
    }

    /* ------------------------------------------------------------ dock */

    private fun renderDock() {
        dockBox.removeAllViews()
        val keys = Dock.items()
        if (keys.isEmpty() || !Prefs.b("dockOn", true)) { dockBox.visibility = View.GONE; return }
        dockBox.visibility = View.VISIBLE
        val d = hbox().apply {
            background = roundBg(pal.dockBg, dp(30).toFloat(), if (pal.dockBg == pal.card && !pal.night) dp(1) else 0, pal.line)
            if (pal.vivid) elevation = dp(6).toFloat()
            setPadding(dp(6), dp(9), dp(6), dp(9))
            isLongClickable = true
            setOnLongClickListener { DockEditor.open(this@HomeActivity) { renderDock() }; true }
        }
        keys.forEachIndexed { i, k ->
            if (k == "empty") { d.addView(View(this), lp(0, dp(10), 1f)); return@forEachIndexed }
            val v = vbox().apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, dp(3))
                background = pressBg(Color.TRANSPARENT, if (Color.red(pal.dockBg) > 200) pal.card2 else 0x33FFFFFF, 20)
            }
            when {
                k.startsWith("app:") -> {
                    val a = Apps.byKey(k.removePrefix("app:"))
                    if (a != null) v.addView(Apps.iconView(this, a, 32, pal, i), lp(dp(32), dp(32)))
                    else v.addView(glyphView("dots", pal.dockFg, 30))
                }
                k.startsWith("book:") -> {
                    val sq = FrameLayout(this).apply { background = roundBg(pal.card, dp(8).toFloat()); clipToOutline = true }
                    val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
                    val f = File(k.removePrefix("book:"))
                    if (f.exists()) Covers.load(this, f, iv) else iv.setImageDrawable(Glyph("book", pal.ink))
                    v.addView(sq, lp(dp(32), dp(32)))
                }
                else -> v.addView(glyphView(Dock.glyph(k), pal.dockFg, 30))
            }
            v.addView(label(Dock.shortLabel(k), 12f, pal.dockFg, true).oneLine().apply { gravity = Gravity.CENTER; setPadding(dp(2), dp(3), dp(2), 0) }, lp(MATCH, WRAP))
            v.setOnClickListener { dockAction(k) }
            v.setOnLongClickListener { DockEditor.slot(this@HomeActivity, i) { renderDock() }; true }
            d.addView(v, lp(0, WRAP, 1f))
        }
        dockBox.addView(d, FrameLayout.LayoutParams(MATCH, FrameLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun dockAction(k: String) {
        when {
            k.startsWith("app:") -> Apps.byKey(k.removePrefix("app:"))?.let { Apps.launch(this, it) } ?: toast("האפליקציה לא נמצאה")
            k.startsWith("folder:") -> openFolder(k.removePrefix("folder:"))
            k.startsWith("book:") -> {
                val f = File(k.removePrefix("book:"))
                if (f.exists()) Store.open(this, f) else toast("הספר לא נמצא")
            }
            else -> when (k) {
                "wifi" -> openWifi()
                "files" -> go(Intent(this, FilesActivity::class.java))
                "books" -> go(Intent(this, BooksActivity::class.java))
                "settings" -> go(Intent(this, SettingsActivity::class.java))
                "apps" -> go(Intent(this, AppsActivity::class.java))
                "calendar" -> go(Intent(this, CalendarActivity::class.java))
                "control" -> showPanel()
                "refresh" -> einkRefresh()
                "bt" -> go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                "addwin" -> newPage()
                "sd" -> {
                    val sd = Store.volumes().firstOrNull { it.removable }
                    if (sd == null) toast("לא זוהה כרטיס זיכרון")
                    else go(Intent(this, FilesActivity::class.java).putExtra("volume", sd.dir.path))
                }
                "android" -> go(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    /* ------------------------------------------------------------ windows & blocks */

    private fun homeMenu() {
        val ui = curUser()
        val items = ArrayList<Sheet.Item>()
        items.add(Sheet.Item("הוספת רכיב לחלון הזה") { addBlockMenu(ui) })
        items.add(Sheet.Item("חלון חדש") { newPage() })
        if (userPages.size > 1) {
            if (ui > 0) items.add(Sheet.Item("הזזת החלון ימינה") { swapPages(ui, ui - 1) })
            if (ui < userPages.size - 1) items.add(Sheet.Item("הזזת החלון שמאלה") { swapPages(ui, ui + 1) })
            items.add(Sheet.Item("סגירת החלון הזה", red = true) { closePage(ui) })
        }
        items.add(Sheet.Item("ערכת נושא") { themeMenu() })
        items.add(Sheet.Item("השורה התחתונה") { DockEditor.open(this) { renderDock() } })
        items.add(Sheet.Item("אילו אפליקציות יופיעו") { go(Intent(this, AppsActivity::class.java).putExtra("manage", true)) })
        items.add(Sheet.Item("הגדרות") { go(Intent(this, SettingsActivity::class.java)) })
        Sheet.actions(this, "חלון ${ui + 1} מתוך ${userPages.size}", items)
    }

    private fun themeMenu() {
        Sheet.choice(this, "ערכת נושא", Look.themes, Look.theme) { v ->
            Prefs.put("theme", v); Apps.clearIcons()
            if (v == "custom") ThemeEditor.open(this) { recreate() } else recreate()
        }
    }

    private fun newPage() {
        val ui = curUser()
        userPages.add(ui + 1, mutableListOf())
        savePages()
        goToUserPage(ui + 1)
        toast("נפתח חלון חדש")
    }

    private fun swapPages(a: Int, b: Int) {
        val t = userPages[a]; userPages[a] = userPages[b]; userPages[b] = t
        savePages(); goToUserPage(b)
    }

    private fun closePage(ui: Int) {
        Sheet.confirm(this, "סגירת החלון", "החלון ייסגר והרכיבים שבו יוסרו. אם יש בו אפליקציות או שעון, הם יועברו לחלון הראשון.", "סגירה") {
            val blocks = userPages[ui]
            userPages.removeAt(ui)
            if (userPages.isEmpty()) userPages.add(mutableListOf())
            for (k in blocks) {
                if (k == "apps" || k == "hero") { if (Pages.pageOf(userPages, k) < 0) userPages[0].add(if (k == "hero") 0 else userPages[0].size, k) }
                else if (k.startsWith("w:")) deleteWidget(k)
            }
            savePages(); cur = 0; renderPage()
        }
    }

    private fun deleteWidget(k: String) {
        val id = k.removePrefix("w:").toIntOrNull() ?: return
        try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        widgetViews.remove(id)
    }

    private fun addBlock(ui: Int, k: String) {
        val u = ui.coerceIn(0, userPages.size - 1)
        val existing = Pages.pageOf(userPages, k)
        if (existing >= 0 && !k.startsWith("w:")) { userPages[existing].remove(k) }
        if (k == "hero") userPages[u].add(0, k) else userPages[u].add(k)
        savePages()
        goToUserPage(u)
    }

    private fun addBlockMenu(ui: Int) {
        val items = ArrayList<Sheet.Item>()
        for ((k, n) in Pages.coreBlocks + Pages.gadgetBlocks) {
            val where = Pages.pageOf(userPages, k)
            val suffix = if (where == ui) " (כבר כאן)" else if (where >= 0) " (מחלון ${where + 1})" else ""
            if (where == ui) continue
            items.add(Sheet.Item(n + suffix) { addBlock(ui, k) })
        }
        items.add(Sheet.Item("גאדג׳ט של אפליקציה…") { widgetPicker(ui) })
        Sheet.actions(this, "הוספה לחלון ${ui + 1}", items)
    }

    private fun blockMenu(k: String, ui: Int, bi: Int) {
        val blocks = userPages[ui]
        val items = ArrayList<Sheet.Item>()
        if (bi > 0) items.add(Sheet.Item("הזזה למעלה") { blocks.removeAt(bi); blocks.add(bi - 1, k); savePages(); renderPage() })
        if (bi < blocks.size - 1) items.add(Sheet.Item("הזזה למטה") { blocks.removeAt(bi); blocks.add(bi + 1, k); savePages(); renderPage() })
        items.add(Sheet.Item("העברה לחלון אחר…") { moveBlockMenu(k, ui) })
        if (k == "g:note") items.add(Sheet.Item("עריכת הפתק") { editNote() })
        if (k == "books") items.add(Sheet.Item("כל הספרים") { go(Intent(this, BooksActivity::class.java)) })
        if (k == "apps") items.add(Sheet.Item("אילו אפליקציות יופיעו") { go(Intent(this, AppsActivity::class.java).putExtra("manage", true)) })
        items.add(Sheet.Item("הסרה מהחלון", red = true) {
            blocks.remove(k)
            if (k.startsWith("w:")) deleteWidget(k)
            savePages(); renderPage()
        })
        Sheet.actions(this, blockName(k), items)
    }

    private fun moveBlockMenu(k: String, ui: Int) {
        val items = ArrayList<Sheet.Item>()
        for (i in userPages.indices) {
            if (i == ui) continue
            items.add(Sheet.Item("חלון ${i + 1}" + if (userPages[i].isEmpty()) " (ריק)" else "") { moveBlock(k, ui, i) })
        }
        items.add(Sheet.Item("חלון חדש") {
            userPages.add(mutableListOf()); moveBlock(k, ui, userPages.size - 1)
        })
        Sheet.actions(this, "להעביר את \"${blockName(k)}\" אל", items)
    }

    private fun moveBlock(k: String, from: Int, to: Int) {
        userPages[from].remove(k)
        if (k == "hero") userPages[to].add(0, k) else userPages[to].add(k)
        savePages()
        goToUserPage(to)
    }

    private fun blockName(k: String): String {
        if (k.startsWith("w:")) {
            val id = k.removePrefix("w:").toIntOrNull()
            return id?.let { awm.getAppWidgetInfo(it) }?.loadLabel(packageManager) ?: "גאדג׳ט"
        }
        return Pages.name(k)
    }

    /* ------------------------------------------------------------ android widgets */

    private fun widgetPicker(ui: Int) {
        val providers = try { awm.installedProviders } catch (e: Exception) { emptyList<AppWidgetProviderInfo>() }
        if (providers.isEmpty()) { toast("אין במכשיר גאדג׳טים של אפליקציות"); return }
        val items = ArrayList<Sheet.Item>()
        for (p in providers.sortedBy { it.loadLabel(packageManager) ?: "" }) {
            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(p.provider.packageName, 0)).toString()
            } catch (e: Exception) { p.provider.packageName }
            items.add(Sheet.Item(appName + " · " + (p.loadLabel(packageManager) ?: "")) { addAndroidWidget(p, ui) })
        }
        Sheet.actions(this, "גאדג׳טים של אפליקציות", items)
    }

    private fun addAndroidWidget(p: AppWidgetProviderInfo, ui: Int) {
        val id = try { host.allocateAppWidgetId() } catch (e: Exception) { toast("לא ניתן להוסיף גאדג׳ט"); return }
        pendingWidget = id
        pendingPage = ui
        val ok = try { awm.bindAppWidgetIdIfAllowed(id, p.provider) } catch (e: Exception) { false }
        if (ok) configureWidget(id)
        else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(i, REQ_BIND)
            } catch (e: Exception) {
                host.deleteAppWidgetId(id); toast("המכשיר לא מאפשר להוסיף את הגאדג׳ט הזה")
            }
        }
    }

    private fun configureWidget(id: Int) {
        val info = awm.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONF, null)
                return
            } catch (e: Exception) { ignore(e) }
        }
        addBlock(pendingPage, "w:$id")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget) ?: pendingWidget
        if (requestCode == REQ_BIND) {
            if (resultCode == RESULT_OK) configureWidget(id) else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        } else if (requestCode == REQ_CONF) {
            if (resultCode == RESULT_OK) addBlock(pendingPage, "w:$id") else try { host.deleteAppWidgetId(id) } catch (e: Exception) { ignore(e) }
        }
    }

    private fun androidWidget(k: String, onLong: () -> Unit): View? {
        val id = k.removePrefix("w:").toIntOrNull() ?: return null
        val info = awm.getAppWidgetInfo(id) ?: return null
        val hv = widgetViews[id] ?: try {
            host.createView(applicationContext, id, info).also { widgetViews[id] = it }
        } catch (e: Exception) { return null }
        (hv.parent as? android.view.ViewGroup)?.removeView(hv)
        val box = vbox()
        val head = hbox()
        head.addView(label(blockName(k), 12.5f, pal.sub, true).oneLine(), lp(0, WRAP, 1f))
        head.addView(FrameLayout(this).apply {
            addView(ImageView(context).apply { setImageDrawable(Glyph("dots", pal.sub)) }, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
            setOnClickListener { onLong() }
        }, lp(dp(30), dp(24)))
        box.addView(head, lp(MATCH, WRAP))
        val card = FrameLayout(this).apply {
            background = roundBg(pal.card, dp(20).toFloat(), if (pal.night) 0 else dp(1), pal.line)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipToOutline = true
        }
        val h = info.minHeight.coerceIn(dp(70), dp(240))
        card.addView(hv, FrameLayout.LayoutParams(MATCH, h))
        box.addView(card, lp(MATCH, WRAP))
        return box
    }

    /* ------------------------------------------------------------ built-in gadgets */

    private fun gadgetCard(i: Int, onLong: () -> Unit): LinearLayout = vbox().apply {
        background = roundBg(if (pal.pastel && !pal.vivid) pal.tile(i + 4) else pal.card, dp(20).toFloat(),
            if (pal.vivid || pal.pastel || pal.night) 0 else dp(1), pal.line)
        if (pal.vivid) elevation = dp(3).toFloat()
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnLongClickListener { onLong(); true }
    }

    private fun miniCalendar(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        c.setOnClickListener { go(Intent(this, CalendarActivity::class.java)) }
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val hd = Heb.of(cal.timeInMillis)
        val head = hbox()
        head.addView(label(SimpleDateFormat("LLLL", Locale("he")).format(cal.time), 16f, pal.text, true), lp(0, WRAP, 1f))
        head.addView(label(Heb.monthName(hd.y, hd.m) + " " + Heb.yearStr(hd.y), 14f, pal.sub, true))
        c.addView(head, lp(MATCH, WRAP))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val offset = cal.get(Calendar.DAY_OF_WEEK) - 1
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val wk = hbox().apply { setPadding(0, dp(6), 0, dp(2)) }
        for (d in 1..7) wk.addView(label(Heb.dayLetters[d], 11f, pal.sub, true).apply { gravity = Gravity.CENTER }, lp(0, WRAP, 1f))
        c.addView(wk, lp(MATCH, WRAP))
        var day = 1
        for (w in 0 until 6) {
            if (day > days) break
            val row = hbox()
            for (col in 0 until 7) {
                val idx = w * 7 + col
                if (idx < offset || day > days) { row.addView(View(this), lp(0, dp(22), 1f)); continue }
                val isToday = day == today
                val t = label(day.toString(), 12.5f, if (isToday) pal.onInk else pal.text, isToday || col == 6).apply {
                    gravity = Gravity.CENTER
                    if (isToday) background = roundBg(pal.ink, dp(11).toFloat())
                }
                row.addView(t, lp(0, dp(22), 1f).m(dp(3), dp(1), dp(3), dp(1)))
                day++
            }
            c.addView(row, lp(MATCH, WRAP))
        }
        return c
    }

    private fun batteryGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val r = hbox()
        val bat = batteryLevel()
        r.addView(ImageView(this).apply { setImageDrawable(Glyph("battery", pal.ink, 1.6f).apply { fill = if (bat >= 0) bat / 100f else 1f }) }, lp(dp(54), dp(54)))
        val col = vbox()
        col.addView(label(if (bat >= 0) "$bat%" else "—", 28f, pal.text, true).apply { typeface = Look.heavy })
        col.addView(label("סוללה", 13f, pal.sub))
        r.addView(col, lp(WRAP, WRAP).m(dp(10), 0, dp(16), 0))
        val st = vbox()
        for (v in Store.volumes()) {
            st.addView(label(v.name, 13f, pal.text, true).oneLine())
            st.addView(label(Store.sizeStr(v.dir.freeSpace) + " פנויים", 12f, pal.sub).oneLine())
        }
        r.addView(st, lp(0, WRAP, 1f))
        c.addView(r)
        c.setOnClickListener { go(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_SETTINGS)) }
        return c
    }

    private fun editNote() {
        Sheet.input(this, "פתק", Prefs.s("note", ""), "מה לזכור?", multiline = true) { s -> Prefs.put("note", s); renderPage() }
    }

    private fun noteGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val head = hbox()
        head.addView(glyphView("note", pal.ink, 20))
        head.addView(label("פתק", 15f, pal.text, true), lp(0, WRAP, 1f).m(dp(8), 0, 0, 0))
        c.addView(head)
        val n = Prefs.s("note", "")
        c.addView(label(if (n.isEmpty()) "הקישו כדי לכתוב פתק" else n, 15f, if (n.isEmpty()) pal.sub else pal.text).apply {
            maxLines = 5; setPadding(0, dp(6), 0, 0)
        })
        c.setOnClickListener { editNote() }
        return c
    }

    private fun bookGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong)
        val f = Books.recent(1).firstOrNull()
        val r = hbox()
        val sq = SquareFrame(this).apply {
            background = roundBg(pal.card2, dp(10).toFloat()); clipToOutline = true
        }
        val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        sq.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
        if (f != null) Covers.load(this, f, iv) else iv.setImageDrawable(Glyph("book", pal.sub))
        r.addView(sq, lp(dp(78), dp(78)))
        val col = vbox()
        col.addView(label("ממשיכים לקרוא", 13f, pal.sub, true))
        col.addView(label(f?.nameWithoutExtension ?: "עדיין לא נפתח ספר", 17f, pal.text, true).apply { maxLines = 2 })
        if (f != null) col.addView(pillButton("המשך קריאה") { Store.open(this, f) }.apply {
            setTextSize(13.5f); setPadding(dp(14), dp(6), dp(14), dp(6))
        }, lp(WRAP, WRAP).m(0, dp(6), 0, 0))
        r.addView(col, lp(0, WRAP, 1f).m(dp(12), 0, 0, 0))
        c.addView(r)
        c.setOnClickListener { if (f != null) Store.open(this, f) else go(Intent(this, BooksActivity::class.java)) }
        return c
    }

    private fun clockGadget(i: Int, onLong: () -> Unit): View {
        val c = gadgetCard(i, onLong).apply { gravity = Gravity.CENTER_HORIZONTAL }
        val t = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            textSize = 92f; typeface = Look.heavy; setTextColor(pal.text); includeFontPadding = false
            gravity = Gravity.CENTER
        }
        bigClocks.add(t)
        c.addView(t, lp(MATCH, WRAP))
        c.addView(label(Heb.format(Heb.of(System.currentTimeMillis())), 16f, pal.sub, true).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP))
        return c
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
                    } else if (dy > th && abs(dy) > 1.4f * abs(dx) && Prefs.b("swipeDown", true)
                        && downY > dp(90) && !inWidget(downX, downY)) {
                        showPanel(); handled = true
                    } else if (abs(dx) > th && abs(dx) > 1.4f * abs(dy) && !inWidget(downX, downY) && inView(pageHost, downY)) {
                        if (dx > 0 && cur < refs.size - 1) { cur++; renderPage() }
                        else if (dx < 0 && cur > 0) { cur--; renderPage() }
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

    private fun inView(v: View, rawY: Float): Boolean {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return rawY >= loc[1] && rawY <= loc[1] + v.height
    }

    private fun inWidget(x: Float, y: Float): Boolean {
        for (hv in widgetViews.values) {
            if (hv.parent == null || !hv.isShown) continue
            val loc = IntArray(2)
            hv.getLocationOnScreen(loc)
            if (x >= loc[0] && x <= loc[0] + hv.width && y >= loc[1] && y <= loc[1] + hv.height) return true
        }
        return false
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
        Sheet.confirm(this, what, "כדי להשתמש בכפתור הזה יש להפעיל את השירות \"היכל הספרים – פעולות מסך\" בהגדרות הנגישות. השירות לא קורא שום תוכן מהמסך.", "פתיחת הגדרות נגישות") {
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
            setBackgroundColor(if (pal.night) 0xDD000000.toInt() else 0xDDFFFFFF.toInt())
            setOnClickListener { hidePanel() }
            isClickable = true
        }
        val sheet = vbox().apply {
            background = roundBg(pal.bg, dp(26).toFloat(), dp(2), pal.ink)
            setPadding(dp(16), dp(14), dp(16), dp(10))
            isClickable = true
        }
        val head = hbox()
        val hv = vbox()
        hv.addView(label("מרכז בקרה", 22f, pal.text, true))
        val bat = batteryLevel()
        hv.addView(label(SimpleDateFormat("HH:mm", Locale.US).format(Date()) + (if (bat >= 0) "  ·  סוללה $bat%" else ""), 14f, pal.sub))
        head.addView(hv, lp(0, WRAP, 1f))
        head.addView(iconButton("close") { hidePanel() })
        sheet.addView(head, lp(MATCH, WRAP))

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
                Sheet.confirm(this, "מעבר למסך הבית הרגיל", "בחרו במסך הבית של BOOX כברירת מחדל. אפשר לחזור להיכל הספרים בכל עת מאותו מקום.", "בחירת מסך בית") {
                    go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                }
            },
            T("calendar", "לוח שנה") { hidePanel(); go(Intent(this, CalendarActivity::class.java)) },
            T("widget", "הוספת רכיב") { hidePanel(); addBlockMenu(curUser()) },
            T("palette", "ערכת נושא") { hidePanel(); themeMenu() },
            T("plus", "חלון חדש") { hidePanel(); newPage() },
            T("folder", "קבצים") { hidePanel(); go(Intent(this, FilesActivity::class.java)) },
            T("grid", "אפליקציות") { hidePanel(); go(Intent(this, AppsActivity::class.java)) },
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

        val br = hbox().apply {
            background = roundBg(pal.card, dp(18).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        br.addView(glyphView("sun", pal.ink, 26))
        val canWrite = Settings.System.canWrite(this)
        val curB = try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }
        val seek = SeekBar(this).apply {
            max = 255
            progress = curB
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
        val close = pillButton("סגירה", filled = false) { hidePanel() }
        sheet.addView(close, lp(MATCH, WRAP).m(0, dp(12), 0, dp(2)))

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
````

---

## `app/src/main/java/com/chdv/haboox/SettingsActivity.kt`

````kotlin
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
        hv.addView(label("היכל הספרים", 22f, pal.text, true))
        hv.addView(label("מסך בית ל‑BOOX Go 6 · גרסה " + version(), 14f, pal.sub))
        hero.addView(hv, lp(0, WRAP, 1f).m(dp(14), 0, 0, 0))
        list.addView(hero, lp(MATCH, WRAP).m(0, dp(4), 0, 0))

        // display
        header("תצוגה")
        choice("palette", "ערכת נושא", "theme", "bw", Look.themes) {
            if (Look.theme == "custom") ThemeEditor.open(this) { restartLook() } else restartLook()
        }
        if (Look.theme == "custom") row("palette", "עריכת הערכה שלי", null) { ThemeEditor.open(this) { restartLook() } }
        toggle("star", "עיטורים בכרטיס השעון", "heroDeco", true) { restartLook() }
        choice("calendar", "גודל השעון", "clockSize", "large", listOf("normal" to "רגיל", "large" to "גדול", "huge" to "ענק")) { restartLook() }
        toggle("grid", "סמלים על אריח מעוגל", "iconTiles", true) { restartLook() }
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
        row("grid", "אילו אפליקציות יופיעו", if (hidden.isEmpty()) "כולן" else "${hidden.size} מוסתרות") {
            go(Intent(this, AppsActivity::class.java).putExtra("manage", true))
        }
        toggle("star", "אפליקציות שהתקנתי – ראשונות", "userFirst", true)
        row("sliders", "השורה התחתונה", Dock.items().size.toString() + " מקומות") { DockEditor.open(this) { build() } }
        row("widget", "הוספת רכיב או גאדג׳ט", Pages.load().size.toString() + " חלונות") {
            go(Intent(this, HomeActivity::class.java).putExtra("gadgets", true))
        }
        toggle("chev_down", "החלקה למטה פותחת מרכז בקרה", "swipeDown", true)
        toggle("plus", "כפתור + לחלון חדש", "showAddWin", true)
        row("refresh", "איפוס סידור החלונות", null) {
            Sheet.confirm(this, "איפוס חלונות", "כל החלונות יחזרו לחלון אחד עם שעון, ספרים, אפליקציות ותיקיות.", "איפוס") {
                Prefs.put("pages", null); Prefs.put("gadgets", null); toast("בוצע")
            }
        }
        row("grid", "רענון רשימת האפליקציות", null) {
            Thread { Apps.load(this); runOnUiThread { toast("נמצאו ${Apps.all.size} אפליקציות") } }.start()
        }
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
        val sd = Store.volumes().firstOrNull { it.removable }
        row("sd", "כרטיס זיכרון", if (sd != null) Store.sizeStr(sd.dir.freeSpace) + " פנויים" else "לא זוהה") {
            if (sd != null) go(Intent(this, FilesActivity::class.java).putExtra("volume", sd.dir.path))
            else toast("לא זוהה כרטיס זיכרון. ודאו שהוא מוכנס ושאושרה גישה לקבצים")
        }
        row("home", "מסך הבית של המכשיר", if (isDefaultHome()) "היכל הספרים" else "לא היכל הספרים") {
            go(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        }
        row("windows", "שירות פעולות (חזרה, חלונות)", if (ActionService.enabled(this)) "פעיל" else "כבוי") {
            go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        row("sun", "הרשאת שינוי בהירות", if (Settings.System.canWrite(this)) "מאושר" else "נדרש אישור") {
            go(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
        }
        row("info", "היכל הספרים בהגדרות אנדרואיד", null) {
            go(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        // separate Android settings button
        val androidBtn = pillButton("פתיחת הגדרות אנדרואיד", filled = true) { go(Intent(Settings.ACTION_SETTINGS)) }
        list.addView(androidBtn, lp(MATCH, WRAP).m(0, dp(14), 0, 0))

        // about
        header("אודות")
        row("refresh", "בדיקת עדכונים", "גרסה " + version()) { Updater.check(this, silent = false) }
        toggle("refresh", "בדיקת עדכונים אוטומטית (פעם ביום)", "autoUpdate", true)
        row("info", "גרסה", version()) { }
        row("edit", "פותח", "שניאור וילהלם") { }
        row("screen", "מותאם ל", "BOOX Go 6 · E‑ink") { }
        row("calendar", "לוח עברי", "אופליין, ללא אינטרנט") { }

        val foot = vbox().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(26), 0, 0) }
        foot.addView(ImageView(this).apply { setImageResource(R.drawable.logo_big) }, lp(dp(84), dp(84)))
        foot.addView(label("היכל הספרים", 18f, pal.text, true).apply { setPadding(0, dp(8), 0, 0) })
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

    private fun dockSettings() {
        if (Apps.all.isEmpty()) Apps.load(this)
        val opts = Dock.builtIn + Apps.everything().map { "app:" + it.key to "אפליקציה: " + Apps.name(it) }
        Sheet.multi(this, "מה יופיע בשורה התחתונה", opts, Dock.items(), 6) { sel -> Dock.save(sel); build() }
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
````

---

## `app/src/main/java/com/chdv/haboox/Ui.kt`

````kotlin
package com.chdv.haboox

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/* ---------------------------------------------------------------- prefs */

object Prefs {
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        if (sp == null) sp = c.applicationContext.getSharedPreferences("haboox", Context.MODE_PRIVATE)
        Store.app = c.applicationContext
    }

    private fun p(): SharedPreferences = sp!!

    fun b(k: String, d: Boolean): Boolean = p().getBoolean(k, d)
    fun s(k: String, d: String): String = p().getString(k, d) ?: d
    fun i(k: String, d: Int): Int = p().getInt(k, d)
    fun has(k: String): Boolean = p().contains(k)
    fun keys(): Set<String> = p().all.keys

    fun put(k: String, v: Any?) {
        val e = p().edit()
        when (v) {
            null -> e.remove(k)
            is Boolean -> e.putBoolean(k, v)
            is Int -> e.putInt(k, v)
            is Long -> e.putLong(k, v)
            else -> e.putString(k, v.toString())
        }
        e.apply()
    }

    fun list(k: String): MutableList<String> =
        s(k, "").split('\n').filter { it.isNotEmpty() }.toMutableList()

    fun putList(k: String, l: List<String>) = put(k, l.joinToString("\n"))
}

/* ---------------------------------------------------------------- palette */

/** pastel == "colorful" (pastel or vivid themes). */
class Pal(
    val bg: Int, val card: Int, val card2: Int, val text: Int, val sub: Int,
    val line: Int, val ink: Int, val onInk: Int, val tiles: IntArray, val pastel: Boolean,
    val hero: IntArray, val heroText: Int, val heroSub: Int,
    val dockBg: Int, val dockFg: Int, val night: Boolean = false, val vivid: Boolean = false
) {
    fun tile(i: Int): Int = tiles[Math.floorMod(i, tiles.size)]
}

object Look {
    val theme: String get() = Prefs.s("theme", "bw")
    val pastel: Boolean get() = theme !in setOf("bw", "contrast", "paper", "night")

    fun c(v: Long): Int = v.toInt()

    val themes: List<Pair<String, String>> = listOf(
        "bw" to "שחור‑לבן", "contrast" to "ניגודיות גבוהה", "paper" to "נייר",
        "vivid" to "חי וצבעוני", "pastel" to "פסטל", "ocean" to "אוקיינוס", "forest" to "יער",
        "sunset" to "שקיעה", "night" to "לילה (שחור)", "midnight" to "כהה צבעוני", "custom" to "ערכה משלי"
    )

    val swatches: List<Pair<String, Int>> = listOf(
        "סגול" to c(0xFF7C3AED), "כחול" to c(0xFF2563EB), "תכלת" to c(0xFF0EA5E9), "טורקיז" to c(0xFF14B8A6),
        "ירוק" to c(0xFF16A34A), "ליים" to c(0xFF84CC16), "צהוב" to c(0xFFEAB308), "כתום" to c(0xFFF97316),
        "אדום" to c(0xFFDC2626), "ורוד" to c(0xFFDB2777), "חום" to c(0xFF92400E), "אפור" to c(0xFF475569),
        "שחור" to c(0xFF111111)
    )

    fun mix(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * (1 - t) + Color.red(b) * t).toInt()
        val g = (Color.green(a) * (1 - t) + Color.green(b) * t).toInt()
        val bl = (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun lightColorful(bg: Long, card2: Long, text: Long, sub: Long, line: Long, tiles: IntArray, a: Int, b: Int): Pal = Pal(
        bg = c(bg), card = Color.WHITE, card2 = c(card2), text = c(text), sub = c(sub), line = c(line),
        ink = c(text), onInk = Color.WHITE, tiles = tiles, pastel = true,
        hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.15f),
        dockBg = c(text), dockFg = Color.WHITE, vivid = true
    )

    fun pal(): Pal = when (theme) {
        "pastel" -> Pal(
            bg = c(0xFFFBF8F3), card = c(0xFFFFFFFF), card2 = c(0xFFF1ECE4),
            text = c(0xFF1C1C1E), sub = c(0xFF6B6B70), line = c(0xFFE4DED4),
            ink = c(0xFF1C1C1E), onInk = Color.WHITE,
            tiles = intArrayOf(
                c(0xFFFFD9D6), c(0xFFD6E8FF), c(0xFFDDF2D8), c(0xFFFFF0C7),
                c(0xFFE8DDFF), c(0xFFFFE0F0), c(0xFFD5F3F0), c(0xFFFFE6CC)
            ), pastel = true,
            hero = intArrayOf(c(0xFFFFD9D6), c(0xFFD6E8FF)), heroText = c(0xFF1C1C1E), heroSub = c(0xFF5A5A60),
            dockBg = c(0xFFFFFFFF), dockFg = c(0xFF1C1C1E)
        )
        "vivid" -> lightColorful(0xFFF6F3EE, 0xFFEDE8E0, 0xFF16161A, 0xFF6A6A72, 0xFFE3DDD3, intArrayOf(
            c(0xFFFF8A7A), c(0xFF5EC8C0), c(0xFFFFC857), c(0xFF9B8CFF),
            c(0xFF6EB5FF), c(0xFF8BD17C), c(0xFFFF9FCB), c(0xFFFFA65C)
        ), c(0xFF4F46E5), c(0xFFC026D3))
        "ocean" -> lightColorful(0xFFF1F7FB, 0xFFE2EEF6, 0xFF0F2433, 0xFF557080, 0xFFD3E3EE, intArrayOf(
            c(0xFF8FD3FF), c(0xFF7FE0D0), c(0xFFB8C8FF), c(0xFFA5E8FF), c(0xFF9FD8C8), c(0xFFC7E3FF)
        ), c(0xFF0EA5E9), c(0xFF2563EB))
        "forest" -> lightColorful(0xFFF3F6EF, 0xFFE4EBDB, 0xFF1B2A17, 0xFF5F6F57, 0xFFD8E0CE, intArrayOf(
            c(0xFFB5E3A1), c(0xFFE8D98F), c(0xFF9CD3B4), c(0xFFD7E8A6), c(0xFFF2C79A), c(0xFFA8D8C0)
        ), c(0xFF16A34A), c(0xFF0D9488))
        "sunset" -> lightColorful(0xFFFFF6EF, 0xFFFBE7D8, 0xFF2B1A12, 0xFF7A5A4A, 0xFFF0DCCD, intArrayOf(
            c(0xFFFFB38A), c(0xFFFFD27A), c(0xFFFF9AA8), c(0xFFFFC2A0), c(0xFFF7A6D2), c(0xFFFFE08A)
        ), c(0xFFF97316), c(0xFFDB2777))
        "paper" -> Pal(
            bg = c(0xFFF5EFE3), card = c(0xFFFBF7EE), card2 = c(0xFFEDE4D3),
            text = c(0xFF3B2F22), sub = c(0xFF7D6B55), line = c(0xFFE0D5C1),
            ink = c(0xFF3B2F22), onInk = c(0xFFF5EFE3),
            tiles = intArrayOf(c(0xFFFBF7EE)), pastel = false,
            hero = intArrayOf(c(0xFF3B2F22)), heroText = c(0xFFF5EFE3), heroSub = c(0xFFCDBFA8),
            dockBg = c(0xFF3B2F22), dockFg = c(0xFFF5EFE3)
        )
        "contrast" -> Pal(
            bg = Color.WHITE, card = Color.WHITE, card2 = c(0xFFDDDDDD),
            text = Color.BLACK, sub = c(0xFF222222), line = Color.BLACK,
            ink = Color.BLACK, onInk = Color.WHITE,
            tiles = intArrayOf(Color.WHITE), pastel = false,
            hero = intArrayOf(Color.BLACK), heroText = Color.WHITE, heroSub = c(0xFFDDDDDD),
            dockBg = Color.BLACK, dockFg = Color.WHITE
        )
        "night" -> Pal(
            bg = Color.BLACK, card = c(0xFF1C1C1E), card2 = c(0xFF2C2C2E),
            text = Color.WHITE, sub = c(0xFFAAAAAA), line = c(0xFF3A3A3C),
            ink = Color.WHITE, onInk = Color.BLACK,
            tiles = intArrayOf(c(0xFF1C1C1E)), pastel = false,
            hero = intArrayOf(c(0xFF1C1C1E)), heroText = Color.WHITE, heroSub = c(0xFFAAAAAA),
            dockBg = c(0xFF1C1C1E), dockFg = Color.WHITE, night = true
        )
        "midnight" -> Pal(
            bg = c(0xFF0F1220), card = c(0xFF1A1F35), card2 = c(0xFF262C48),
            text = Color.WHITE, sub = c(0xFFA7B0D0), line = c(0xFF2C3352),
            ink = Color.WHITE, onInk = c(0xFF0F1220),
            tiles = intArrayOf(c(0xFF3B3F8F), c(0xFF0E6B6B), c(0xFF7A3B8F), c(0xFF8F4A2E), c(0xFF2E5C8F), c(0xFF2E7A45)),
            pastel = true,
            hero = intArrayOf(c(0xFF6366F1), c(0xFFEC4899)), heroText = Color.WHITE, heroSub = c(0xFFE0E0FF),
            dockBg = c(0xFF1A1F35), dockFg = Color.WHITE, night = true, vivid = true
        )
        "custom" -> custom()
        else -> Pal(
            bg = Color.WHITE, card = c(0xFFF2F2F2), card2 = c(0xFFE4E4E4),
            text = c(0xFF111111), sub = c(0xFF555555), line = c(0xFFCFCFCF),
            ink = c(0xFF111111), onInk = Color.WHITE,
            tiles = intArrayOf(c(0xFFF2F2F2)), pastel = false,
            hero = intArrayOf(c(0xFF111111)), heroText = Color.WHITE, heroSub = c(0xFFBDBDBD),
            dockBg = c(0xFF111111), dockFg = Color.WHITE
        )
    }

    private fun custom(): Pal {
        val a = Prefs.i("customA", c(0xFF7C3AED))
        val b = Prefs.i("customB", c(0xFF0EA5E9))
        val dark = Prefs.s("customBg", "light") == "dark"
        return if (dark) Pal(
            bg = mix(Color.BLACK, a, 0.08f), card = mix(Color.BLACK, a, 0.16f), card2 = mix(Color.BLACK, a, 0.26f),
            text = Color.WHITE, sub = mix(Color.WHITE, a, 0.3f), line = mix(Color.BLACK, a, 0.3f),
            ink = Color.WHITE, onInk = Color.BLACK,
            tiles = intArrayOf(mix(Color.BLACK, a, 0.45f), mix(Color.BLACK, b, 0.45f), mix(Color.BLACK, mix(a, b, 0.5f), 0.45f)),
            pastel = true, hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.2f),
            dockBg = mix(Color.BLACK, a, 0.16f), dockFg = Color.WHITE, night = true, vivid = true
        ) else Pal(
            bg = mix(Color.WHITE, a, 0.05f), card = Color.WHITE, card2 = mix(Color.WHITE, a, 0.14f),
            text = c(0xFF16161A), sub = c(0xFF62626C), line = mix(Color.WHITE, a, 0.18f),
            ink = c(0xFF16161A), onInk = Color.WHITE,
            tiles = intArrayOf(mix(Color.WHITE, a, 0.45f), mix(Color.WHITE, b, 0.45f), mix(Color.WHITE, mix(a, b, 0.5f), 0.45f)),
            pastel = true, hero = intArrayOf(a, b), heroText = Color.WHITE, heroSub = mix(Color.WHITE, a, 0.15f),
            dockBg = mix(c(0xFF16161A), a, 0.25f), dockFg = Color.WHITE, vivid = true
        )
    }

    fun heroBg(p: Pal, radius: Float): Drawable {
        val base: GradientDrawable =
            if (p.hero.size > 1) GradientDrawable(GradientDrawable.Orientation.TR_BL, p.hero).apply { cornerRadius = radius }
            else roundBg(p.hero[0], radius)
        if (!Prefs.b("heroDeco", true)) return base
        val light = Color.red(p.heroText) > 128
        return android.graphics.drawable.LayerDrawable(arrayOf<Drawable>(base, Deco(if (light) 0x22FFFFFF else 0x14000000)))
    }
    fun clockSize(): Float = when (Prefs.s("clockSize", "large")) {
        "normal" -> 50f
        "huge" -> 84f
        else -> 66f
    }

    val heavy: Typeface by lazy { Typeface.create("sans-serif-black", Typeface.NORMAL) }

    fun scale(): Float = when (Prefs.s("fontSize", "normal")) {
        "large" -> 1.15f
        "xlarge" -> 1.3f
        else -> 1f
    }

    fun bold(): Boolean = Prefs.b("boldText", false)
    fun grayIcons(): Boolean = Prefs.b("grayIcons", !pastel)
    fun sig(): String = Prefs.s("theme", "bw") + Prefs.s("fontSize", "normal") + bold() + grayIcons() +
        Prefs.s("clockSize", "large") + Prefs.b("iconTiles", true) + Prefs.b("heroDeco", true) +
        Prefs.i("customA", 0) + Prefs.i("customB", 0) + Prefs.s("customBg", "light")
}

/* ---------------------------------------------------------------- helpers */

fun Context.dp(v: Number): Int = (v.toFloat() * resources.displayMetrics.density + 0.5f).toInt()

fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

fun Context.go(i: Intent, fallback: Intent? = null): Boolean {
    if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(i); true
    } catch (e: Exception) {
        if (fallback != null) {
            try {
                if (this !is Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallback); true
            } catch (e2: Exception) {
                toast("הפעולה אינה זמינה במכשיר זה"); false
            }
        } else {
            toast("הפעולה אינה זמינה במכשיר זה"); false
        }
    }
}

fun roundBg(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

fun Context.pressBg(color: Int, pressed: Int, radiusDp: Int, stroke: Int = 0, strokeColor: Int = 0): Drawable {
    val r = dp(radiusDp).toFloat()
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), roundBg(pressed, r, stroke, strokeColor))
        addState(intArrayOf(), roundBg(color, r, stroke, strokeColor))
    }
}

fun Context.label(s: CharSequence, size: Float, color: Int, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size * Look.scale())
        setTextColor(color)
        typeface = if (bold || Look.bold()) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textDirection = View.TEXT_DIRECTION_RTL
    }

fun TextView.oneLine(): TextView {
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    return this
}

fun Context.vbox(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
fun Context.hbox(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h, weight)

fun LinearLayout.LayoutParams.m(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
    setMargins(l, t, r, b); return this
}

fun Context.glyphView(kind: String, color: Int, sizeDp: Int): ImageView = ImageView(this).apply {
    setImageDrawable(Glyph(kind, color))
    layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
}

class SquareFrame(c: Context) : FrameLayout(c) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}

/* ---------------------------------------------------------------- glyphs */

class Glyph(private val kind: String, private val color: Int, private val strokeW: Float = 1.9f) : Drawable() {
    var fill: Float = 1f
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    companion object {
        private val cache = HashMap<String, Path>()
        private val tok = Regex("[MLHVQCZ]|-?[0-9]*\\.?[0-9]+")

        fun path(d: String): Path {
            cache[d]?.let { return it }
            val p = Path()
            val t = tok.findAll(d).map { it.value }.toList()
            var i = 0
            var cmd = 'M'
            var x = 0f
            var y = 0f
            fun n(): Float = t[i++].toFloat()
            while (i < t.size) {
                val s = t[i]
                if (s[0].isLetter()) {
                    cmd = s[0]; i++
                    if (cmd == 'Z') { p.close(); continue }
                }
                when (cmd) {
                    'M' -> { x = n(); y = n(); p.moveTo(x, y); cmd = 'L' }
                    'L' -> { x = n(); y = n(); p.lineTo(x, y) }
                    'H' -> { x = n(); p.lineTo(x, y) }
                    'V' -> { y = n(); p.lineTo(x, y) }
                    'Q' -> { val a = n(); val b = n(); x = n(); y = n(); p.quadTo(a, b, x, y) }
                    'C' -> {
                        val a = n(); val b = n(); val c = n(); val e = n(); x = n(); y = n()
                        p.cubicTo(a, b, c, e, x, y)
                    }
                    else -> i++
                }
            }
            cache[d] = p
            return p
        }
    }

    private lateinit var cv: Canvas
    private fun st(d: String) = cv.drawPath(path(d), sp)
    private fun fl(d: String) = cv.drawPath(path(d), fp)
    private fun circ(x: Float, y: Float, r: Float, fill: Boolean = false) = cv.drawCircle(x, y, r, if (fill) fp else sp)
    private fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float, fill: Boolean = false) =
        cv.drawRoundRect(RectF(l, t, r, b), rad, rad, if (fill) fp else sp)

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()) / 24f
        sp.color = color; fp.color = color; sp.strokeWidth = strokeW
        cv = canvas
        canvas.save()
        canvas.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f)
        canvas.scale(s, s)
        when (kind) {
            "wifi" -> {
                for (r in floatArrayOf(4f, 8f, 12f)) {
                    canvas.drawArc(RectF(12f - r, 19.5f - r, 12f + r, 19.5f + r), 225f, 90f, false, sp)
                }
                circ(12f, 19.5f, 1.7f, true)
            }
            "folder" -> {
                st("M3 6.5 Q3 5 4.5 5 H9 L11 7 H19.5 Q21 7 21 8.5 V17.5 Q21 19 19.5 19 H4.5 Q3 19 3 17.5 Z")
                st("M3 9.5 H21")
            }
            "book" -> {
                st("M12 6.5 C10 5 6.5 4.8 3 5.5 V18.5 C6.5 17.8 10 18 12 19.5")
                st("M12 6.5 C14 5 17.5 4.8 21 5.5 V18.5 C17.5 17.8 14 18 12 19.5")
                st("M12 6.5 V19.5")
            }
            "gear" -> {
                circ(12f, 12f, 3f)
                circ(12f, 12f, 6.8f)
                val old = sp.strokeWidth
                sp.strokeWidth = 2.6f
                for (i in 0 until 8) {
                    canvas.save(); canvas.rotate(45f * i, 12f, 12f)
                    canvas.drawLine(12f, 2.6f, 12f, 5.0f, sp)
                    canvas.restore()
                }
                sp.strokeWidth = old
            }
            "bt" -> st("M7 7.5 L17 16.5 L12 21 V3 L17 7.5 L7 16.5")
            "battery" -> {
                rr(2.5f, 7.5f, 19.5f, 16.5f, 2f)
                rr(20.4f, 10.3f, 22f, 13.7f, 0.8f, true)
                val w = 13f * fill.coerceIn(0.05f, 1f)
                rr(4.5f, 9.5f, 4.5f + w, 14.5f, 1f, true)
            }
            "sun" -> {
                circ(12f, 12f, 3.6f)
                for (i in 0 until 8) {
                    canvas.save(); canvas.rotate(45f * i, 12f, 12f)
                    canvas.drawLine(12f, 2.8f, 12f, 5.2f, sp)
                    canvas.restore()
                }
            }
            "back" -> { st("M4 12 H19.5"); st("M13.5 6 L19.5 12 L13.5 18") }
            "chev_l" -> st("M15 5 L8 12 L15 19")
            "chev_r" -> st("M9 5 L16 12 L9 19")
            "chev_down" -> st("M5 9 L12 16 L19 9")
            "windows" -> {
                rr(3f, 8f, 15f, 20f, 2f)
                st("M7.5 8 V5.5 Q7.5 4 9 4 H19.5 Q21 4 21 5.5 V14.5 Q21 16 19.5 16 H15")
            }
            "refresh" -> {
                canvas.drawArc(RectF(4.5f, 4.5f, 19.5f, 19.5f), 320f, 300f, false, sp)
                st("M18.2 3.6 L18.3 8 L13.9 8.1")
            }
            "home" -> {
                st("M3.5 11 L12 4 L20.5 11"); st("M6 9.5 V20 H18 V9.5"); st("M10 20 V14.5 H14 V20")
            }
            "sliders" -> {
                st("M4 7 H20"); st("M4 12 H20"); st("M4 17 H20")
                circ(15f, 7f, 2.2f, true); circ(9f, 12f, 2.2f, true); circ(13.5f, 17f, 2.2f, true)
            }
            "calendar" -> {
                rr(3.5f, 5f, 20.5f, 20.5f, 2.5f)
                st("M3.5 9.5 H20.5"); st("M8 3 V7"); st("M16 3 V7")
                for (yy in floatArrayOf(13f, 16.8f)) for (xx in floatArrayOf(7.5f, 12f, 16.5f)) circ(xx, yy, 1.05f, true)
            }
            "plus" -> { st("M12 5 V19"); st("M5 12 H19") }
            "file" -> { st("M6 3 H14 L19 8 V21 H6 Z"); st("M14 3 V8 H19") }
            "image" -> {
                rr(3f, 5f, 21f, 19f, 2.5f); circ(8.5f, 9.5f, 1.6f, true)
                st("M3.5 17 L9 12 L13 15.5 L16 13 L20.5 16.5")
            }
            "music" -> { st("M9 17 V5.5 L19 3.5 V15"); circ(6.8f, 17f, 2.2f, true); circ(16.8f, 15f, 2.2f, true) }
            "video" -> { rr(3f, 5f, 21f, 19f, 2.5f); fl("M10 9 L15.5 12 L10 15 Z") }
            "archive" -> {
                rr(4f, 3f, 20f, 21f, 2.5f)
                st("M12 3 V5"); st("M12 7 V9"); st("M12 11 V13"); rr(10.5f, 14f, 13.5f, 18f, 1f)
            }
            "apk" -> {
                rr(5f, 9f, 19f, 20f, 3f); st("M8 9 Q8 4.5 12 4.5 Q16 4.5 16 9"); st("M7.5 4 L9 6"); st("M16.5 4 L15 6")
            }
            "sort" -> { st("M4 7 H20"); st("M7 12 H17"); st("M10 17 H14") }
            "dots" -> { circ(6f, 12f, 1.7f, true); circ(12f, 12f, 1.7f, true); circ(18f, 12f, 1.7f, true) }
            "paste" -> { rr(5f, 5f, 19f, 21f, 2.5f); rr(9f, 3f, 15f, 7f, 1.2f) }
            "search" -> { circ(10.5f, 10.5f, 6f); st("M15 15 L20.5 20.5") }
            "up" -> { st("M12 19.5 V5"); st("M6 11 L12 5 L18 11") }
            "check" -> st("M5 12.5 L10 17.5 L19.5 7")
            "close" -> { st("M6.5 6.5 L17.5 17.5"); st("M17.5 6.5 L6.5 17.5") }
            "trash" -> { st("M4 7 H20"); st("M9 7 V4.5 H15 V7"); st("M6 7 L7 20.5 H17 L18 7") }
            "edit" -> st("M4 20 L5 15.5 L15.5 5 L19 8.5 L8.5 19 Z")
            "copy" -> {
                rr(8f, 8f, 20f, 20f, 2.5f)
                st("M16 8 V5.5 Q16 4 14.5 4 H5.5 Q4 4 4 5.5 V14.5 Q4 16 5.5 16 H8")
            }
            "cut" -> { circ(7f, 17.5f, 2.6f); circ(17f, 17.5f, 2.6f); st("M8.8 15.6 L17 4"); st("M15.2 15.6 L7 4") }
            "info" -> { circ(12f, 12f, 9f); st("M12 11 V17"); circ(12f, 7.6f, 1.2f, true) }
            "screen" -> { rr(6f, 2.5f, 18f, 21.5f, 2.8f); st("M10 18.5 H14") }
            "hide" -> {
                st("M2.5 12 Q12 3 21.5 12 Q12 21 2.5 12 Z"); circ(12f, 12f, 3f); st("M4 4 L20 20")
            }
            "bell" -> {
                st("M6 17 V11 Q6 5 12 5 Q18 5 18 11 V17 Z"); st("M4.5 17 H19.5"); st("M10 20 Q12 21.5 14 20")
            }
            "grid" -> {
                rr(4f, 4f, 10.5f, 10.5f, 2f); rr(13.5f, 4f, 20f, 10.5f, 2f)
                rr(4f, 13.5f, 10.5f, 20f, 2f); rr(13.5f, 13.5f, 20f, 20f, 2f)
            }
            "text" -> { st("M5 19 L10 5 L15 19"); st("M7 14 H13"); st("M16.5 19 V13"); circ(18.5f, 16.8f, 2.2f) }
            "palette" -> {
                st("M12 3 Q21 3 21 11 Q21 15 17 15 H15 Q13 15 13.8 17 Q15 21 12 21 Q3 21 3 12 Q3 3 12 3 Z")
                circ(8f, 10f, 1.3f, true); circ(12f, 7f, 1.3f, true); circ(16.2f, 9.5f, 1.3f, true)
            }
            "note" -> { st("M5 3.5 H15 L19 7.5 V20.5 H5 Z"); st("M8 10 H16"); st("M8 13.5 H16"); st("M8 17 H13") }
            "sd" -> {
                st("M8 3 H18 Q19.5 3 19.5 4.5 V19.5 Q19.5 21 18 21 H6 Q4.5 21 4.5 19.5 V7 Z")
                st("M9.5 6 V9"); st("M12.5 6 V9"); st("M15.5 6 V9")
            }
            "widget" -> {
                rr(3.5f, 3.5f, 11f, 11f, 2f); rr(13f, 3.5f, 20.5f, 11f, 2f, true)
                rr(3.5f, 13f, 20.5f, 20.5f, 2f)
            }
            "star" ->st("M12 3.5 L14.6 9 L20.5 9.6 L16 13.6 L17.3 19.5 L12 16.5 L6.7 19.5 L8 13.6 L3.5 9.6 L9.4 9 Z")
            else -> circ(12f, 12f, 8f)
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        sp.alpha = alpha; fp.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        sp.colorFilter = colorFilter; fp.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = 72
    override fun getIntrinsicHeight(): Int = 72
}

/** Decorative translucent circles for the hero card. */
class Deco(private val color: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    override fun draw(canvas: Canvas) {
        val b = bounds
        p.color = color
        val w = b.width().toFloat(); val h = b.height().toFloat()
        canvas.save()
        canvas.clipRect(b)
        canvas.drawCircle(b.left + w * 0.1f, b.top + h * 0.05f, h * 0.75f, p)
        canvas.drawCircle(b.left + w * 0.34f, b.top + h * 1.05f, h * 0.42f, p)
        canvas.drawCircle(b.left + w * 0.92f, b.top + h * 0.18f, h * 0.12f, p)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { p.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/* ---------------------------------------------------------------- base activity */

open class BaseActivity : AppCompatActivity() {
    lateinit var pal: Pal

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.init(this)
        pal = Look.pal()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = pal.bg
        window.navigationBarColor = pal.bg
        window.setBackgroundDrawable(ColorDrawable(pal.bg))
        window.decorView.systemUiVisibility = if (pal.night) 0 else
            (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    fun iconButton(kind: String, onClick: () -> Unit): View = FrameLayout(this).apply {
        val g = ImageView(context).apply { setImageDrawable(Glyph(kind, pal.ink, 2.1f)) }
        addView(g, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        background = pressBg(Color.TRANSPARENT, pal.card2, 22)
        layoutParams = lp(dp(46), dp(46))
        setOnClickListener { onClick() }
    }

    /** iOS style screen: back link, large title, body. Returns the actions bar for later edits. */
    fun screen(title: String, body: View, actions: List<View> = emptyList(), onBack: (() -> Unit)? = null): LinearLayout {
        val root = vbox().apply { setBackgroundColor(pal.bg) }
        val bar = hbox().apply { setPadding(dp(8), dp(6), dp(8), 0) }
        val back = hbox().apply {
            addView(glyphView("chev_r", pal.ink, 22))
            addView(label("חזרה", 17f, pal.ink))
            setPadding(dp(6), dp(8), dp(12), dp(8))
            background = pressBg(Color.TRANSPARENT, pal.card2, 18)
            setOnClickListener { if (onBack != null) onBack() else finish() }
        }
        bar.addView(back, lp(WRAP, WRAP))
        bar.addView(View(this), lp(0, 1, 1f))
        val act = hbox()
        actions.forEach { act.addView(it) }
        bar.addView(act, lp(WRAP, WRAP))
        root.addView(bar, lp(MATCH, WRAP))
        val t = label(title, 30f, pal.text, true).oneLine()
        t.setPadding(dp(20), dp(2), dp(20), dp(10))
        t.tag = "title"
        root.addView(t, lp(MATCH, WRAP))
        root.addView(body, lp(MATCH, 0, 1f))
        setContentView(root)
        return act
    }

    fun setScreenTitle(s: String) {
        (window.decorView.findViewWithTag<TextView>("title"))?.text = s
    }

    fun hasFiles(): Boolean = Store.hasAccess(this)
    fun askFiles() = Store.askAccess(this)

    fun permissionCard(text: String): View {
        val c = vbox().apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundBg(pal.card, dp(18).toFloat())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        c.addView(glyphView("folder", pal.ink, 40))
        c.addView(label(text, 16f, pal.text).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(14)) })
        c.addView(pillButton("אישור גישה לקבצים") { askFiles() })
        return c
    }

    fun pillButton(text: String, filled: Boolean = true, onClick: () -> Unit): TextView =
        label(text, 16f, if (filled) pal.onInk else pal.ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(11), dp(22), dp(11))
            background = if (filled) pressBg(pal.ink, 0xFF444444.toInt(), 22)
            else pressBg(Color.TRANSPARENT, pal.card2, 22, dp(2), pal.ink)
            setOnClickListener { onClick() }
        }
}

/* ---------------------------------------------------------------- sheets (iOS action sheets) */

object Sheet {
    private fun make(ctx: Context, content: View, bottom: Boolean = true): Dialog {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(content)
        d.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(MATCH, WRAP)
            w.setGravity(if (bottom) Gravity.BOTTOM else Gravity.CENTER)
            w.setWindowAnimations(0)
            w.setDimAmount(0.3f)
            w.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        return d
    }

    private fun shell(ctx: Context): LinearLayout = ctx.vbox().apply {
        setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(14))
    }

    private fun card(ctx: Context, pal: Pal): LinearLayout = ctx.vbox().apply {
        background = roundBg(pal.card, ctx.dp(18).toFloat(), ctx.dp(1), pal.line)
    }

    private fun divider(ctx: Context, pal: Pal): View = View(ctx).apply {
        setBackgroundColor(pal.line)
        layoutParams = lp(MATCH, maxOf(1, ctx.dp(0.7f)))
    }

    private fun row(ctx: Context, pal: Pal, text: String, red: Boolean, bold: Boolean, onClick: () -> Unit): View =
        ctx.label(text, 18f, pal.ink, bold || red).apply {
            gravity = Gravity.CENTER
            setPadding(ctx.dp(16), ctx.dp(15), ctx.dp(16), ctx.dp(15))
            background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
            if (red) setTextColor(if (pal.pastel) 0xFFC0392B.toInt() else pal.ink)
            setOnClickListener { onClick() }
        }

    private fun limit(ctx: Context, inner: View, count: Int): View {
        val sv = ScrollView(ctx)
        sv.addView(inner)
        val h = (ctx.resources.displayMetrics.heightPixels * 0.62f).toInt()
        sv.layoutParams = lp(MATCH, if (count > 8) h else WRAP)
        return sv
    }

    class Item(val text: String, val red: Boolean = false, val action: () -> Unit)

    fun actions(ctx: Context, title: String?, items: List<Item>) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        if (title != null) {
            c.addView(ctx.label(title, 14f, pal.sub, true).apply {
                gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
            })
            c.addView(divider(ctx, pal))
        }
        val list = ctx.vbox()
        items.forEachIndexed { i, item ->
            if (i > 0) list.addView(divider(ctx, pal))
            list.addView(row(ctx, pal, item.text, item.red, false) { d.dismiss(); item.action() })
        }
        c.addView(limit(ctx, list, items.size))
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun choice(ctx: Context, title: String, options: List<Pair<String, String>>, current: String, onPick: (String) -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 14f, pal.sub, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        })
        c.addView(divider(ctx, pal))
        val list = ctx.vbox()
        options.forEachIndexed { i, (value, text) ->
            if (i > 0) list.addView(divider(ctx, pal))
            val r = ctx.hbox().apply {
                setPadding(ctx.dp(18), ctx.dp(14), ctx.dp(18), ctx.dp(14))
                background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
                addView(ctx.label(text, 17f, pal.ink, value == current), lp(0, WRAP, 1f))
                if (value == current) addView(ctx.glyphView("check", pal.ink, 22))
                setOnClickListener { d.dismiss(); onPick(value) }
            }
            list.addView(r)
        }
        c.addView(limit(ctx, list, options.size))
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun confirm(ctx: Context, title: String, msg: String, ok: String, onOk: () -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(4))
        })
        c.addView(ctx.label(msg, 15f, pal.sub).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(4), ctx.dp(18), ctx.dp(14))
        })
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, ok, true, true) { d.dismiss(); onOk() })
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        d = make(ctx, sh)
        d.show()
    }

    fun info(ctx: Context, title: String, msg: String) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(4))
        })
        c.addView(ctx.label(msg, 15f, pal.sub).apply {
            gravity = Gravity.START; setPadding(ctx.dp(18), ctx.dp(6), ctx.dp(18), ctx.dp(14))
        })
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, "סגירה", false, true) { d.dismiss() })
        sh.addView(c, lp(MATCH, WRAP))
        d = make(ctx, sh)
        d.show()
    }

    /** Multi-select with order kept by selection. */
    fun multi(ctx: Context, title: String, options: List<Pair<String, String>>, selected: List<String>, max: Int, onDone: (List<String>) -> Unit) {
        val pal = Look.pal()
        val sel = selected.filter { s -> options.any { it.first == s } }.toMutableList()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 14f, pal.sub, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(4))
        })
        val hint = ctx.label("", 12.5f, pal.sub).apply { gravity = Gravity.CENTER; setPadding(ctx.dp(16), 0, ctx.dp(16), ctx.dp(10)) }
        c.addView(hint)
        c.addView(divider(ctx, pal))
        val list = ctx.vbox()
        val marks = ArrayList<TextView>()
        fun refresh() {
            hint.text = "נבחרו ${sel.size} מתוך עד $max · הסדר לפי סדר הבחירה"
            options.forEachIndexed { i, (v, _) ->
                val n = sel.indexOf(v)
                marks[i].text = if (n >= 0) (n + 1).toString() else ""
                marks[i].background = if (n >= 0) roundBg(pal.ink, ctx.dp(13).toFloat())
                else roundBg(Color.TRANSPARENT, ctx.dp(13).toFloat(), ctx.dp(1), pal.sub)
            }
        }
        options.forEachIndexed { i, (value, text) ->
            if (i > 0) list.addView(divider(ctx, pal))
            val mark = ctx.label("", 13f, pal.onInk, true).apply { gravity = Gravity.CENTER }
            marks.add(mark)
            val r = ctx.hbox().apply {
                setPadding(ctx.dp(18), ctx.dp(12), ctx.dp(18), ctx.dp(12))
                background = ctx.pressBg(Color.TRANSPARENT, pal.card2, 0)
                addView(ctx.label(text, 17f, pal.ink), lp(0, WRAP, 1f))
                addView(mark, lp(ctx.dp(26), ctx.dp(26)))
                setOnClickListener {
                    if (value in sel) sel.remove(value)
                    else if (sel.size < max) sel.add(value)
                    else ctx.toast("אפשר לבחור עד $max")
                    refresh()
                }
            }
            list.addView(r)
        }
        c.addView(limit(ctx, list, options.size))
        c.addView(divider(ctx, pal))
        c.addView(row(ctx, pal, "שמירה", false, true) { d.dismiss(); onDone(sel) })
        sh.addView(c, lp(MATCH, WRAP))
        val cancel = card(ctx, pal)
        cancel.addView(row(ctx, pal, "ביטול", false, true) { d.dismiss() })
        sh.addView(cancel, lp(MATCH, WRAP).m(0, ctx.dp(8), 0, 0))
        refresh()
        d = make(ctx, sh)
        d.show()
    }

    fun input(ctx: Context, title: String, initial: String, hint: String, multiline: Boolean = false, onOk: (String) -> Unit) {
        val pal = Look.pal()
        val sh = shell(ctx)
        val c = card(ctx, pal)
        lateinit var d: Dialog
        c.addView(ctx.label(title, 18f, pal.text, true).apply {
            gravity = Gravity.CENTER; setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(10))
        })
        val et = EditText(ctx).apply {
            setText(initial)
            setHint(hint)
            if (multiline) {
                setSingleLine(false); minLines = 3; maxLines = 8; gravity = Gravity.TOP or Gravity.START
            } else setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(pal.text)
            setHintTextColor(pal.sub)
            textDirection = View.TEXT_DIRECTION_RTL
            background = roundBg(pal.bg, ctx.dp(12).toFloat(), ctx.dp(1), pal.line)
            setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
            setSelectAllOnFocus(true)
        }
        c.addView(et, lp(MATCH, WRAP).m(ctx.dp(16), 0, ctx.dp(16), ctx.dp(16)))
        c.addView(divider(ctx, pal))
        val btns = ctx.hbox()
        btns.addView(row(ctx, pal, "ביטול", false, false) { d.dismiss() }, lp(0, WRAP, 1f))
        btns.addView(View(ctx).apply { setBackgroundColor(pal.line) }, lp(maxOf(1, ctx.dp(0.7f)), MATCH))
        btns.addView(row(ctx, pal, "שמירה", false, true) {
            d.dismiss(); onOk(et.text.toString().trim())
        }, lp(0, WRAP, 1f))
        c.addView(btns)
        sh.addView(c, lp(MATCH, WRAP))
        d = make(ctx, sh, bottom = false)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        et.requestFocus()
        et.postDelayed({
            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
    }
}

/* ---------------------------------------------------------------- misc */

fun isAtLeastR(): Boolean = Build.VERSION.SDK_INT >= 30

fun ignore(e: Throwable?) {}

fun safeOpenTry(ctx: Context, i: Intent): Boolean = try {
    ctx.startActivity(i); true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
````

---

## `app/src/main/java/com/chdv/haboox/Updater.kt`

````kotlin
package com.chdv.haboox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from GitHub Releases. Every successful build publishes a release tagged v2.0.<run>
 * with HaBoox.apk attached; all builds are signed with the same key, so an update installs over
 * the existing app and keeps its settings.
 */
object Updater {
    private const val REPO = "chdv770-ctrl/my-app"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    @Volatile
    private var busy = false

    private fun build(v: String): Int = v.trim().removePrefix("v").substringAfterLast('.').toIntOrNull() ?: 0

    private fun localVersion(a: Activity): String = try {
        a.packageManager.getPackageInfo(a.packageName, 0).versionName ?: "0"
    } catch (e: Exception) { "0" }

    fun dailyCheck(a: Activity) {
        val last = try { Prefs.s("lastUpdateCheck", "0").toLong() } catch (e: Exception) { 0L }
        if (System.currentTimeMillis() - last < 24L * 3600 * 1000) return
        Prefs.put("lastUpdateCheck", System.currentTimeMillis().toString())
        check(a, silent = true)
    }

    private fun open(u: String): HttpURLConnection {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.instanceFollowRedirects = false
        c.setRequestProperty("User-Agent", "HaBoox-Updater")
        c.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, */*")
        return c
    }

    /** GET that follows redirects across hosts (GitHub sends downloads to another domain). */
    private fun get(url: String): HttpURLConnection {
        var u = url
        repeat(6) {
            val c = open(u)
            val code = c.responseCode
            if (code in 300..399) {
                val loc = c.getHeaderField("Location")
                c.disconnect()
                if (loc != null) u = URL(URL(u), loc).toString()
                return@repeat
            }
            return c
        }
        return open(u)
    }
    fun check(a: Activity, silent: Boolean) {
        if (busy) return
        busy = true
        if (!silent) a.toast("בודק אם יש עדכון…")
        Thread {
            try {
                val c = get(API)
                val body = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                val apk = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"").find(body)?.groupValues?.get(1)
                val local = localVersion(a)
                val newer = build(tag) > build(local)
                a.runOnUiThread {
                    busy = false
                    if (newer && apk != null) {
                        Sheet.confirm(a, "יש עדכון זמין",
                            "גרסה חדשה: ${tag.removePrefix("v")}\nהגרסה שלך: $local\n\nההורדה דרך האינטרנט (Wi‑Fi), וההגדרות שלך יישמרו.",
                            "הורדה והתקנה") { download(a, apk) }
                    } else if (!silent) {
                        Sheet.info(a, "אין עדכון חדש", "הגרסה שלך ($local) היא העדכנית ביותר.")
                    }
                }
            } catch (e: Exception) {
                a.runOnUiThread {
                    busy = false
                    if (!silent) Sheet.info(a, "לא ניתן לבדוק עדכונים", "ודאו שהמכשיר מחובר לאינטרנט (Wi‑Fi) ונסו שוב.")
                }
            }
        }.start()
    }

    private fun download(a: Activity, url: String) {
        if (Build.VERSION.SDK_INT >= 26 && !a.packageManager.canRequestPackageInstalls()) {
            Sheet.confirm(a, "צריך אישור חד-פעמי",
                "כדי להתקין עדכונים יש לאשר להיכל הספרים להתקין אפליקציות. אחרי האישור חזרו ולחצו שוב על \"בדיקת עדכונים\".",
                "פתיחת ההגדרה") {
                a.go(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName)))
            }
            return
        }
        a.toast("מוריד את העדכון…")
        Thread {
            try {
                val dir = File(a.cacheDir, "update").apply { mkdirs() }
                val out = File(dir, "HaBoox.apk")
                val c = get(url)
                val total = c.contentLength.toLong()
                var done = 0L
                var lastPct = -1
                c.inputStream.use { input ->
                    FileOutputStream(out).use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct / 25 != lastPct / 25) {
                                    lastPct = pct
                                    a.runOnUiThread { a.toast("מוריד… $pct%") }
                                }
                            }
                        }
                    }
                }
                c.disconnect()
                a.runOnUiThread { install(a, out) }
            } catch (e: Exception) {
                a.runOnUiThread { Sheet.info(a, "ההורדה נכשלה", "בדקו את החיבור לאינטרנט ונסו שוב.") }
            }
        }.start()
    }

    private fun install(a: Activity, f: File) {
        val uri = FileProvider.getUriForFile(a, a.packageName + ".files", f)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!safeOpenTry(a, i)) a.toast("לא ניתן לפתוח את מתקין האפליקציות")
    }
}
````

