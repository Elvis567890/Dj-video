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
    onPickTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(query) {
        tracks = withContext(Dispatchers.IO) {
            try { LibraryScanner.scan(context, query) } catch (_: Throwable) { emptyList() }
        }
    }
    Column(modifier = modifier.fillMaxWidth().height(200.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Neon.PANEL_GLASS)
        .border(1.dp, Neon.CYAN.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(8.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search library", color = Neon.TEXT_DIM, fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
        Spacer(Modifier.height(6.dp))
        if (tracks.isEmpty()) {
            Text("No tracks found — grant permission or add music",
                color = Neon.TEXT_FAINT, fontSize = 9.sp)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(tracks) { track ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { onPickTrack(track) }
                        .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(track.title.take(40), color = Neon.TEXT, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(track.artist.take(20), color = Neon.TEXT_DIM, fontSize = 9.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("${track.durationMs / 1000 / 60}:${"%02d".format((track.durationMs / 1000) % 60)}",
                            color = Neon.CYAN, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
