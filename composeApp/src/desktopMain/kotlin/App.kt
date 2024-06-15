import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var log: String? by remember {
            mutableStateOf(null)
        }
        var busy by remember {
            mutableStateOf(false)
        }
        var exportFilePath: String? by remember {
            mutableStateOf(null)
        }
        var downloadDirectory: String? by remember {
            mutableStateOf(null)
        }
        var showFilePicker by remember { mutableStateOf(false) }
        var showDirectoryPicker by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Slack Photo Downloader",
                style = MaterialTheme.typography.h5,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Select Slack export file:",
                style = MaterialTheme.typography.body1,
            )
            exportFilePath?.also {
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Button(
                onClick = { showFilePicker = true },
                enabled = !busy,
            ) {
                Text("Select")
            }

            Divider()

            Text(
                text = "2. Select photo download destination:",
                style = MaterialTheme.typography.body1,
            )
            downloadDirectory?.also {
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Button(
                onClick = { showDirectoryPicker = true },
                enabled = !busy,
            ) {
                Text(
                    text = "Select",
                )
            }

            Divider()

            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        log = "Extracting archive file..."
                    }
                },
                enabled = !busy && exportFilePath != null && downloadDirectory != null
            ) {
                Text(
                    text = "Download Photos",
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }
                log?.also {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }

        }
        FilePicker(show = showFilePicker, fileExtensions = listOf("zip")) { file ->
            showFilePicker = false
            file?.also { exportFilePath = it.path }
        }
        DirectoryPicker(showDirectoryPicker) { dir ->
            showDirectoryPicker = false
            dir?.also { downloadDirectory = it }
        }
    }
}