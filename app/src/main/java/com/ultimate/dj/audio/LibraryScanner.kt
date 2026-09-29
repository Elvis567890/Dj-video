package com.ultimate.dj.audio
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val bpm: Float = 0f,
    val key: String = "",
    val dateAdded: Long = 0L
)
object LibraryScanner {
    fun scan(context: Context, query: String = "", filter: String = "ALL"): List<Track> {
        val out = mutableListOf<Track>()
        if (filter == "ALL" || filter == "AUDIO" || filter == "RECENT") scanAudio(context, query, out)
        if (filter == "ALL" || filter == "VIDEO") scanVideo(context, query, out)
        val sorted = out.sortedBy { it.title.lowercase() }
        return if (filter == "RECENT") sorted.sortedByDescending { it.dateAdded }.take(100) else sorted
    }
    private fun scanAudio(context: Context, query: String, out: MutableList<Track>) {
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        try {
            val sel = if (query.isBlank()) null else "${MediaStore.Audio.Media.TITLE} LIKE ?"
            val args = if (query.isBlank()) null else arrayOf("%$query%")
            val c: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, args,
                "${MediaStore.Audio.Media.TITLE} ASC")
            c?.use {
                val idI = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tiI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val arI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val alI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val duI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val daI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (it.moveToNext() && out.size < 500) {
                    val id = it.getLong(idI)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    out.add(Track(id, uri, it.getString(tiI) ?: "Unknown",
                        it.getString(arI) ?: "Unknown", it.getString(alI) ?: "",
                        it.getLong(duI), false, 0f, "", it.getLong(daI)))
                }
            }
        } catch (_: Throwable) {}
    }
    private fun scanVideo(context: Context, query: String, out: MutableList<Track>) {
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        try {
            val sel = if (query.isBlank()) null else "${MediaStore.Video.Media.TITLE} LIKE ?"
            val args = if (query.isBlank()) null else arrayOf("%$query%")
            val c: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, sel, args,
                "${MediaStore.Video.Media.TITLE} ASC")
            c?.use {
                val idI = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val tiI = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val duI = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val daI = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (it.moveToNext() && out.size < 500) {
                    val id = it.getLong(idI)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                    out.add(Track(id, uri, it.getString(tiI) ?: "Video",
                        "Video", "", it.getLong(duI), true, 0f, "", it.getLong(daI)))
                }
            }
        } catch (_: Throwable) {}
    }
}
