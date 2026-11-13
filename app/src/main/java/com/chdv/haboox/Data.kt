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
        "refresh" to "רענון מסך", "bt" to "בלוטות׳", "sd" to "כרטיס זיכרון", "android" to "הגדרות אנדרואיד"
    )

    fun items(): List<String> =
        if (Prefs.has("dock")) Prefs.list("dock") else listOf("wifi", "files", "books", "settings")

    fun save(l: List<String>) = Prefs.putList("dock", l)

    fun addApp(key: String) {
        val l = items().toMutableList()
        val k = "app:$key"
        if (k in l) return
        if (l.size >= 6) l.removeAt(l.size - 1)
        l.add(k); save(l)
    }

    fun glyph(k: String): String = when (k) {
        "wifi" -> "wifi"; "files" -> "folder"; "books" -> "book"; "settings" -> "gear"
        "apps" -> "grid"; "calendar" -> "calendar"; "control" -> "sliders"; "refresh" -> "refresh"
        "bt" -> "bt"; "sd" -> "sd"; "android" -> "screen"; else -> "dots"
    }

    fun shortLabel(k: String): String = when (k) {
        "wifi" -> "Wi‑Fi"; "files" -> "קבצים"; "books" -> "ספרים"; "settings" -> "הגדרות"
        "apps" -> "אפליקציות"; "calendar" -> "לוח"; "control" -> "בקרה"; "refresh" -> "רענון"
        "bt" -> "בלוטות׳"; "sd" -> "כרטיס"; "android" -> "אנדרואיד"
        else -> Apps.byKey(k.removePrefix("app:"))?.let { Apps.name(it) } ?: "אפליקציה"
    }
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
