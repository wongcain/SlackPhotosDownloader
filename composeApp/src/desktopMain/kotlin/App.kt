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
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

@OptIn(ExperimentalSerializationApi::class)
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

        val httpClient = remember {
            HttpClient(Java)
        }
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
                            val tmpDir =
                                File(exportDir + File.separator + exportFile.nameWithoutExtension).apply { mkdirs() }
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

                            File(exportDir).walk().forEach { jsonFile ->
                                val channel =
                                    if (jsonFile.parent == exportDir) "no_channel" else jsonFile.parentFile.name
                                if (jsonFile.extension == "json") {
                                    FileInputStream(jsonFile).use {
                                        try {
                                            val items: JsonArray = Json.decodeFromStream(it)
                                            items.forEach { item ->
                                                item.jsonObject["files"]?.also { files ->
                                                    files.jsonArray.forEach { file ->
                                                        file.jsonObject.also {
                                                            val photoname =
                                                                it["name"]?.jsonPrimitive?.content
                                                            val photourl =
                                                                it["url_private_download"]?.jsonPrimitive?.content
                                                            val mimeType =
                                                                it["mimetype"]?.jsonPrimitive?.content
                                                            if (photourl != null && mimeType != null && (mimeType.startsWith(
                                                                    "image"
                                                                ) || mimeType.startsWith("video"))
                                                            ) {
                                                                val destDir =
                                                                    File(downloadDirectory + File.separator + exportFile.nameWithoutExtension + " - Photos" + File.separator + channel).apply { mkdirs() }
                                                                log = "Downloading $photourl..."
                                                                httpClient.get(photourl) {
                                                                    onDownload { bytesSentTotal, contentLength ->
                                                                        log =
                                                                            "Downloading $photourl... $bytesSentTotal of $contentLength"
                                                                    }
                                                                }.bodyAsChannel().copyAndClose(
                                                                    File(
                                                                        destDir,
                                                                        photoname!!
                                                                    ).writeChannel()
                                                                )

                                                            }
                                                        }
                                                    }

                                                }
                                            }
                                        } catch (e: Throwable) {
                                            println("@@ error: $e")
                                        }
                                    }
                                }
                            }

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