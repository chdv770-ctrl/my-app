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
