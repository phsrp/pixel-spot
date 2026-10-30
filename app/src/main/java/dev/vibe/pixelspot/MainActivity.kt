package dev.vibe.pixelspot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import dev.vibe.pixelspot.data.SpotifyAuth
import dev.vibe.pixelspot.lock.LockScreenService
import dev.vibe.pixelspot.ui.PixelSpotApp

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            PixelSpotApp(vm, onLogin = { clientId ->
                CustomTabsIntent.Builder().build().launchUrl(this, vm.loginUri(clientId))
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        vm.connectPlayer(this)
        LockScreenService.sync(this)
    }

    override fun onStop() {
        super.onStop()
        // Spotify recommends letting go of the connection while the app isn't visible.
        if (!isChangingConfigurations) vm.player.disconnect()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.toString().startsWith(SpotifyAuth.REDIRECT_URI)) {
            vm.handleRedirect(data)
            intent.data = null
        }
    }
}
