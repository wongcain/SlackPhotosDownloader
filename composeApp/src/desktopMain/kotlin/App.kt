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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

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
                        withContext(Dispatchers.IO) {
                            val exportFile = File(exportFilePath!!)
                            val exportDir = exportFile.parent
                            val tmpDir = File(exportDir + File.separator + exportFile.nameWithoutExtension).apply { mkdirs() }
                            ZipFile(exportFilePath!!).use { zip ->
                                zip.entries().asSequence().forEach { entry ->
                                    zip.getInputStream(entry).use { input ->
                                        val filePath = tmpDir.path + File.separator + entry.name
                                        log = "Extracting archive file...${filePath}"
                                        if (!entry.isDirectory) {
                                            // if the entry is a file, extracts it
                                            extractFile(input, filePath)
                                        } else {
                                            // if the entry is a directory, make the directory
                                            val dir = File(filePath)
                                            dir.mkdir()
                                        }
                                    }
                                }
                            }

                            // TODO

                            log = "DONE!"
                            busy = false
                        }
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

fun extractFile(inputStream: InputStream, destFilePath: String) {
    val bos = BufferedOutputStream(FileOutputStream(destFilePath))
    val bytesIn = ByteArray(4096)
    var read: Int
    while (inputStream.read(bytesIn).also { read = it } != -1) {
        bos.write(bytesIn, 0, read)
    }
    bos.close()
}