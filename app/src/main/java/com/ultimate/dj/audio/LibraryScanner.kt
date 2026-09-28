package com.ultimate.dj.audio
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val isVideo: Boolean
)
object LibraryScanner {
    fun scan(context: Context, query: String = ""): List<Track> {
        val out = mutableListOf<Track>()
        val audioProj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        try {
            val sel = if (query.isBlank()) null else "${MediaStore.Audio.Media.TITLE} LIKE ?"
            val args = if (query.isBlank()) null else arrayOf("%$query%")
            val c: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioProj, sel, args,
                "${MediaStore.Audio.Media.TITLE} ASC")
            c?.use {
                val idI = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tiI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val arI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val duI = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (it.moveToNext() && out.size < 200) {
                    val id = it.getLong(idI)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    out.add(Track(id, uri, it.getString(tiI) ?: "Unknown",
                        it.getString(arI) ?: "Unknown", it.getLong(duI), false))
                }
            }
        } catch (_: Throwable) {}
        return out
    }
}
