package com.jadzumi.video

import android.app.Application
import androidx.core.net.toUri
import androidx.core.database.sqlite.transaction
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract as Docs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class LocalFolder(val uri: String, val name: String, val status: String)
data class LocalVideo(
    val uri: String, val folder: String, val name: String, val mime: String,
    val bytes: Long, val modified: Long, val duration: Long, val width: Int, val height: Int,
    val parentDir: String = ""
) {
    val identity: String get() {
        val parsed = uri.toUri()
        return "${parsed.authority}:${Docs.getDocumentId(parsed)}"
    }
    val episode: EpisodeHint? by lazy { parseEpisode(name) }
    val title: String by lazy { name.substringBeforeLast('.', name).replace('_', ' ') }
    val dirId: String get() = if (parentDir.isNotEmpty()) parentDir else try {
        Docs.getDocumentId(uri.toUri()).substringBeforeLast('/', "")
    } catch (_: Exception) { "" }
}

data class LocalLibraryState(
    val folders: List<LocalFolder> = emptyList(),
    val videos: List<LocalVideo> = emptyList(),
    val sidecars: Map<String, List<LocalSidecar>> = emptyMap(),
    val tvSeries: List<TvSeries> = emptyList(),
    val busy: Boolean = true,
    val message: String? = null
)

data class EpisodeHint(val season: Int, val episode: Int)
fun parseEpisode(name: String): EpisodeHint? {
    val info = EpisodePatterns.parse(name)
    if (info != null) return EpisodeHint(info.season, info.episode)
    val match = Regex("(?i)(?:^|[ ._\\-])s(\\d{1,2})e(\\d{1,3})(?=$|[^0-9])").find(name)
        ?: Regex("(?i)(?:^|[ ._\\-])(\\d{1,2})x(\\d{1,3})(?=$|[^0-9])").find(name)
    return match?.let { EpisodeHint(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
}

fun isVideoFile(name: String, mime: String): Boolean = mime.startsWith("video/") ||
        name.substringAfterLast('.', "").lowercase() in setOf(
            "mkv", "mp4", "m4v", "webm", "avi", "mov", "ts", "m2ts", "mpeg", "mpg", "3gp", "vob"
        )

fun isSidecarFile(name: String, mime: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("jpg", "jpeg", "png", "webp", "nfo") ||
            mime.startsWith("image/") ||
            (mime == "text/plain" && ext == "nfo")
}

private class LibraryDb(context: Context) : SQLiteOpenHelper(context, "library.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE folders(uri TEXT PRIMARY KEY, name TEXT NOT NULL, status TEXT NOT NULL)")
        db.execSQL("CREATE TABLE videos(uri TEXT NOT NULL, folder TEXT NOT NULL, name TEXT NOT NULL, mime TEXT NOT NULL, bytes INTEGER, modified INTEGER, duration INTEGER, width INTEGER, height INTEGER, parent_dir TEXT DEFAULT '', PRIMARY KEY(uri,folder))")
        db.execSQL("CREATE TABLE IF NOT EXISTS sidecars(uri TEXT PRIMARY KEY, folder TEXT NOT NULL, parent_dir TEXT NOT NULL, name TEXT NOT NULL, mime TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS sidecars")
            db.execSQL("CREATE TABLE IF NOT EXISTS sidecars(uri TEXT PRIMARY KEY, folder TEXT NOT NULL, parent_dir TEXT NOT NULL, name TEXT NOT NULL, mime TEXT NOT NULL)")
            runCatching { db.execSQL("ALTER TABLE videos ADD COLUMN parent_dir TEXT DEFAULT ''") }
        }
    }
}

class LocalLibrary internal constructor(private val context: Context) {
    private val database = LibraryDb(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(LocalLibraryState())
    val state = mutable.asStateFlow()
    private val stopped = AtomicBoolean(false)
    private var lastAutoRefreshTime = 0L

    init {
        scope.launch {
            try {
                refresh()
                // Auto-rescan if folders are connected but sidecars have not yet been indexed
                if (mutable.value.folders.isNotEmpty() && mutable.value.sidecars.isEmpty()) {
                    rescan()
                }
            } catch (_: Exception) {
                mutable.value = LocalLibraryState(busy = false, message = "Could not open library. Restart to retry.")
            }
        }
    }

    private fun refresh(message: String? = null) {
        val db = database.readableDatabase
        val folders = mutableListOf<LocalFolder>()
        db.rawQuery("SELECT uri,name,status FROM folders ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) folders += LocalFolder(c.getString(0), c.getString(1), c.getString(2))
        }
        val videos = mutableListOf<LocalVideo>()
        db.rawQuery("SELECT uri,folder,name,mime,bytes,modified,duration,width,height,parent_dir FROM videos ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) videos += LocalVideo(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                c.getLong(4), c.getLong(5), c.getLong(6), c.getInt(7), c.getInt(8),
                c.getString(9) ?: ""
            )
        }
        val sidecarsMap = mutableMapOf<String, MutableList<LocalSidecar>>()
        try {
            db.rawQuery("SELECT folder,name,mime,uri,parent_dir FROM sidecars", null).use { c ->
                while (c.moveToNext()) {
                    val folder = c.getString(0)
                    val name = c.getString(1)
                    val mime = c.getString(2)
                    val uri = c.getString(3)
                    val parentDir = c.getString(4) ?: ""
                    sidecarsMap.getOrPut(folder) { mutableListOf() }.add(
                        LocalSidecar(folder, name, mime, uri, parentDir)
                    )
                }
            }
        } catch (_: Exception) { }
        val series = clusterVideosIntoSeries(videos, sidecarsMap, context)
        mutable.value = LocalLibraryState(folders, videos, sidecarsMap, series, false, message)
    }

    private fun work(silent: Boolean = false, block: () -> String?) {
        if (mutable.value.busy) return
        if (!silent) {
            mutable.value = mutable.value.copy(busy = true, message = "Working on your library…")
        } else {
            mutable.value = mutable.value.copy(busy = true)
        }
        stopped.set(false)
        scope.launch {
            val message = try {
                block()
            } catch (_: Exception) {
                "Operation could not finish. Your existing library has been kept."
            }
            try {
                refresh(message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(busy = false, message = "Could not reload library.")
            }
        }
    }

    fun cancel() { stopped.set(true) }
    fun report(message: String) { mutable.value = mutable.value.copy(message = message) }

    fun add(uri: Uri) = work {
        val root = Docs.buildDocumentUriUsingTree(uri, Docs.getTreeDocumentId(uri))
        val name = runCatching {
            context.contentResolver.query(root, arrayOf(Docs.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: "Selected folder"

        val db = database.writableDatabase
        db.insertWithOnConflict("folders", null, ContentValues().apply {
            put("uri", uri.toString())
            put("name", name)
            put("status", "Not scanned")
        }, SQLiteDatabase.CONFLICT_IGNORE)
        scan(LocalFolder(uri.toString(), name, "Not scanned"), force = false)
    }

    fun rescan() = work {
        val messages = mutable.value.folders.map { if (stopped.get()) "Scan cancelled" else scan(it, force = false) }
        if (messages.any { it != null }) "Some folders were not fully scanned. Check folder status." else "Library is up to date."
    }

    fun fullRescan() = work {
        val messages = mutable.value.folders.map { if (stopped.get()) "Scan cancelled" else scan(it, force = true) }
        if (messages.any { it != null }) "Some folders had issues during full rescan." else "Full library rescan complete."
    }

    fun autoRefreshOnResume() {
        val now = System.currentTimeMillis()
        if (now - lastAutoRefreshTime > 30_000 && !mutable.value.busy && mutable.value.folders.isNotEmpty()) {
            lastAutoRefreshTime = now
            work(silent = true) {
                mutable.value.folders.forEach { if (!stopped.get()) scan(it, force = false) }
                null
            }
        }
    }

    /** User-initiated permanent deletion using scoped SAF Document API */
    fun deleteVideo(video: LocalVideo) = work {
        val docUri = video.uri.toUri()
        val deleted = try {
            Docs.deleteDocument(context.contentResolver, docUri)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }

        if (deleted) {
            val db = database.writableDatabase
            db.transaction {
                delete("videos", "uri=?", arrayOf(video.uri))
            }
            "Deleted \"${video.name}\" from storage."
        } else {
            "Could not delete \"${video.name}\". If this folder was added before, remove and re-add the folder to grant write access."
        }
    }

    fun remove(folder: LocalFolder) = work {
        val db = database.writableDatabase
        db.transaction {
            delete("videos", "folder=?", arrayOf(folder.uri))
            delete("sidecars", "folder=?", arrayOf(folder.uri))
            delete("folders", "uri=?", arrayOf(folder.uri))
        }
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                folder.uri.toUri(),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        "Folder removed from library. Your files were not changed."
    }

    private fun scan(folder: LocalFolder, force: Boolean): String? {
        val db = database.writableDatabase
        val tree = folder.uri.toUri()
        var status = "Ready"
        try {
            if (context.contentResolver.persistedUriPermissions.none { it.uri == tree && it.isReadPermission }) throw SecurityException()
            val queue = ArrayDeque<String>()
            val visited = HashSet<String>()
            val found = mutableListOf<LocalVideo>()
            val foundSidecars = mutableListOf<LocalSidecar>()
            val previous = mutable.value.videos.associateBy { it.uri }
            queue.add(Docs.getTreeDocumentId(tree))
            var entries = 0
            while (queue.isNotEmpty()) {
                if (stopped.get()) throw CancellationException()
                val parent = queue.removeFirst()
                if (!visited.add(parent)) continue
                val children = Docs.buildChildDocumentsUriUsingTree(tree, parent)
                context.contentResolver.query(
                    children,
                    arrayOf(
                        Docs.Document.COLUMN_DOCUMENT_ID,
                        Docs.Document.COLUMN_DISPLAY_NAME,
                        Docs.Document.COLUMN_MIME_TYPE,
                        Docs.Document.COLUMN_SIZE,
                        Docs.Document.COLUMN_LAST_MODIFIED,
                        Docs.Document.COLUMN_FLAGS
                    ),
                    null, null, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        if (stopped.get()) throw CancellationException()
                        if (++entries > 50000) throw IllegalStateException("Folder too large")
                        val id = c.getString(0)
                        val name = c.getString(1) ?: "Untitled"
                        val mime = c.getString(2) ?: ""
                        if (mime == Docs.Document.MIME_TYPE_DIR) {
                            queue.add(id)
                        } else if (isVideoFile(name, mime)) {
                            if (c.getInt(5) and Docs.Document.FLAG_VIRTUAL_DOCUMENT != 0) continue
                            val uri = Docs.buildDocumentUriUsingTree(tree, id)
                            val size = if (c.isNull(3)) -1 else c.getLong(3)
                            val modified = if (c.isNull(4)) 0 else c.getLong(4)
                            val old = previous[uri.toString()]

                            val video = if (!force && old != null && modified > 0 && old.modified == modified && old.bytes == size) {
                                old.copy(folder = folder.uri, name = name, mime = mime, parentDir = parent)
                            } else {
                                metadata(LocalVideo(uri.toString(), folder.uri, name, mime, size, modified, 0, 0, 0, parentDir = parent))
                            }
                            found += video
                            if (found.size % 20 == 0) {
                                mutable.value = mutable.value.copy(message = "Indexing ${folder.name} · ${found.size} videos")
                            }
                        } else if (isSidecarFile(name, mime)) {
                            val uri = Docs.buildDocumentUriUsingTree(tree, id).toString()
                            foundSidecars += LocalSidecar(folder.uri, name, mime, uri, parentDir = parent)
                        }
                    }
                    if (c.extras.containsKey(Docs.EXTRA_ERROR)) throw IllegalStateException("Provider error")
                    if (c.extras.getBoolean(Docs.EXTRA_LOADING, false)) throw IllegalStateException("Provider still loading")
                } ?: throw IllegalStateException("Folder unavailable")
            }
            if (stopped.get()) throw CancellationException()
            db.transaction {
                db.delete("videos", "folder=?", arrayOf(folder.uri))
                db.delete("sidecars", "folder=?", arrayOf(folder.uri))
                found.forEach { v ->
                    db.insertOrThrow("videos", null, ContentValues().apply {
                        put("uri", v.uri)
                        put("folder", v.folder)
                        put("name", v.name)
                        put("mime", v.mime)
                        put("bytes", v.bytes)
                        put("modified", v.modified)
                        put("duration", v.duration)
                        put("width", v.width)
                        put("height", v.height)
                        put("parent_dir", v.parentDir)
                    })
                }
                foundSidecars.forEach { s ->
                    db.insertWithOnConflict("sidecars", null, ContentValues().apply {
                        put("uri", s.uri)
                        put("folder", s.parentId)
                        put("parent_dir", s.parentDir)
                        put("name", s.name)
                        put("mime", s.mime)
                    }, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
        } catch (_: CancellationException) {
            status = "Cancelled · previous index kept"
        } catch (_: SecurityException) {
            status = "Access lost · add folder again"
        } catch (_: Exception) {
            status = "Unavailable or incomplete · reconnect storage and refresh, or select a smaller folder"
        }
        db.update("folders", ContentValues().apply { put("status", status) }, "uri=?", arrayOf(folder.uri))
        return if (status == "Ready") null else status
    }

    private fun metadata(video: LocalVideo): LocalVideo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, video.uri.toUri())
            video.copy(
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            )
        } catch (_: Exception) {
            video
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        fun get(context: Context): LocalLibrary = (context.applicationContext as JadzumiApplication).library
    }
}

class JadzumiApplication : Application() {
    val library: LocalLibrary by lazy { LocalLibrary(this) }
}
