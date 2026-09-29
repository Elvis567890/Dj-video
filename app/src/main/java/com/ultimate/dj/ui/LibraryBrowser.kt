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
private val FILTERS = listOf("ALL", "AUDIO", "VIDEO", "RECENT")
@Composable
fun LibraryBrowser(
    context: Context,
    targetDeckName: String,
    onPickTrack: (Track) -> Unit,
    onPickForOtherDeck: (Track) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selected by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(query, filter) {
        tracks = withContext(Dispatchers.IO) {
            try { LibraryScanner.scan(context, query, filter) } catch (_: Throwable) { emptyList() }
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
            Text("${tracks.size} tracks", color = Neon.TEXT_FAINT, fontSize = 9.sp)
            Spacer(Modifier.width(12.dp))
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
            placeholder = { Text("Search title or artist...", color = Neon.TEXT_FAINT, fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(46.dp)
        )

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (f in FILTERS) {
                val sel = f == filter
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Neon.BLUE.copy(alpha = 0.20f) else Neon.BTN_BG)
                    .border(0.5.dp, Neon.BLUE.copy(alpha = if (sel) 0.9f else 0.20f), RoundedCornerShape(4.dp))
                    .clickable { filter = f }
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(f, color = if (sel) Neon.BLUE else Neon.TEXT_DIM,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            Text("TITLE", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(2.2f))
            Text("ARTIST", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1.4f))
            Text("BPM", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.width(44.dp))
            Text("KEY", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.width(36.dp))
            Text("TIME", color = Neon.TEXT_FAINT, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.width(44.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Neon.BORDER_SOFT))

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No tracks found.\nGrant storage permission and add music/videos.",
                    color = Neon.TEXT_FAINT, fontSize = 10.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(tracks) { track ->
                    val isSel = selected?.uri == track.uri
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(if (isSel) Neon.BLUE.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { selected = track }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (track.isVideo) Neon.AMBER.copy(alpha = 0.18f) else Neon.BLUE.copy(alpha = 0.18f))
                            .border(0.5.dp, if (track.isVideo) Neon.AMBER.copy(alpha = 0.7f) else Neon.BLUE.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(if (track.isVideo) "V" else "♪",
                                color = if (track.isVideo) Neon.AMBER else Neon.BLUE,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(track.title.take(45), color = Neon.TEXT, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(2.2f), maxLines = 1)
                        Text(track.artist.take(20), color = Neon.TEXT_DIM, fontSize = 9.sp,
                            modifier = Modifier.weight(1.4f), maxLines = 1)
                        Text(if (track.bpm > 0) "%.0f".format(track.bpm) else "—",
                            color = Neon.TEXT_DIM, fontSize = 9.sp, modifier = Modifier.width(44.dp))
                        Text(if (track.key.isNotEmpty()) track.key else "—",
                            color = Neon.TEXT_DIM, fontSize = 9.sp, modifier = Modifier.width(36.dp))
                        Text("${track.durationMs / 1000 / 60}:${"%02d".format((track.durationMs / 1000) % 60)}",
                            color = Neon.TEXT_DIM, fontSize = 9.sp, modifier = Modifier.width(44.dp))
                    }
                }
            }
        }

        if (selected != null) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Neon.PANEL_DARK)
                .border(0.5.dp, Neon.BLUE.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(selected!!.title, color = Neon.TEXT, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(selected!!.artist, color = Neon.TEXT_DIM, fontSize = 9.sp, maxLines = 1)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Neon.BLUE.copy(alpha = 0.20f))
                    .border(0.5.dp, Neon.BLUE, RoundedCornerShape(4.dp))
                    .clickable { onPickTrack(selected!!) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("LOAD $targetDeckName", color = Neon.BLUE, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Neon.BTN_BG)
                    .border(0.5.dp, Neon.TEXT_FAINT.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .clickable { onPickForOtherDeck(selected!!) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("OTHER DECK", color = Neon.TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
