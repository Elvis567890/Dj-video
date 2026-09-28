package com.ultimate.dj.ui
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultimate.dj.Neon
import com.ultimate.dj.audio.LibraryScanner
import com.ultimate.dj.audio.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun LibraryBrowser(
    context: Context,
    targetDeckName: String,
    onPickTrack: (Track) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(query) {
        tracks = withContext(Dispatchers.IO) {
            try { LibraryScanner.scan(context, query) } catch (_: Throwable) { emptyList() }
        }
    }
    Column(modifier = modifier.fillMaxWidth().fillMaxHeight()
        .clip(RoundedCornerShape(8.dp))
        .background(Neon.PANEL_GLASS)
        .border(0.5.dp, Neon.BLUE.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("LIBRARY", color = Neon.BLUE, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.width(12.dp))
            Text("Load into $targetDeckName",
                color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(Neon.BTN_BG)
                .border(0.5.dp, Neon.TEXT_FAINT.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { onClose() }
                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("CLOSE", color = Neon.TEXT_DIM, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search by title or artist...", color = Neon.TEXT_FAINT, fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No music or video found on this device.",
                    color = Neon.TEXT_FAINT, fontSize = 10.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(tracks) { track ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { onPickTrack(track) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (track.isVideo) Neon.AMBER.copy(alpha = 0.15f) else Neon.BLUE.copy(alpha = 0.15f))
                            .border(0.5.dp, if (track.isVideo) Neon.AMBER.copy(alpha = 0.6f) else Neon.BLUE.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(if (track.isVideo) "V" else "♪",
                                color = if (track.isVideo) Neon.AMBER else Neon.BLUE,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title.take(60), color = Neon.TEXT, fontSize = 11.sp,
                                fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(track.artist.take(30), color = Neon.TEXT_DIM, fontSize = 9.sp, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${track.durationMs / 1000 / 60}:${"%02d".format((track.durationMs / 1000) % 60)}",
                            color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
