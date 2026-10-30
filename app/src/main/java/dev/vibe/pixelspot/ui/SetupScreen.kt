package dev.vibe.pixelspot.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import dev.vibe.pixelspot.data.SpotifyAuth
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded
import java.security.MessageDigest

/** First screen: explains the Spotify dashboard setup and starts the login. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupScreen(savedClientId: String?, error: String?, onLogin: (String) -> Unit) {
    val context = LocalContext.current
    var clientId by rememberSaveable { mutableStateOf(savedClientId.orEmpty()) }
    val sha1 = remember { signingSha1(context) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primary)
        )
        Text("Pixel Spot", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded)
        Text(
            "A PixelPlayer-style remote for Spotify. The Spotify app plays the music in the background; this app is the remote.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text("1. In your Spotify developer dashboard, open your app → Settings → Edit, and add:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        CopyRow("Redirect URI", SpotifyAuth.REDIRECT_URI, context)
        CopyRow("Android package", context.packageName, context)
        CopyRow("SHA-1 fingerprint", sha1, context)
        Text("Tick \"Android\" and \"Web API\" under APIs used, then Save.", style = MaterialTheme.typography.bodyMedium)

        Text("2. Paste your app's Client ID:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it.trim() },
            label = { Text("Client ID") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(20.dp)) {
                Text(error, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        Button(
            onClick = { onLogin(clientId) },
            enabled = clientId.length >= 20,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
        ) { Text("3. Log in with Spotify", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CopyRow(label: String, value: String, context: Context) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
            IconButton(onClick = {
                val cm = context.getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.ContentCopy, "Copy $label") }
        }
    }
}

/** The SHA-1 of the key this APK was signed with, as Spotify's dashboard wants it. */
fun signingSha1(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val cert = info.signingInfo!!.apkContentsSigners.first().toByteArray()
    MessageDigest.getInstance("SHA-1").digest(cert).joinToString(":") { "%02X".format(it) }
}.getOrDefault("unknown")
