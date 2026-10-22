package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.KrunkerGameScreen
import com.example.ui.theme.KrunkerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Keep display awake during FPS gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Enable edge to edge and apply sticky immersive full screen
        enableEdgeToEdge()

        // 3. Remove notch / display cutout padding completely so game renders edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 4. Unlock Maximum Display Refresh Rate (90Hz / 120Hz / 144Hz) for Unlimited FPS
        unlockHighRefreshRate()

        // 5. Clean up any corrupted or empty WebView variations seed to prevent seed signature error
        cleanupCorruptedWebViewSeed()

        applyFullScreenImmersive()

        setContent {
            KrunkerTheme {
                KrunkerGameScreen(
                    onTriggerImmersive = { applyFullScreenImmersive() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyFullScreenImmersive()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullScreenImmersive()
        }
    }

    private fun applyFullScreenImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
    }

    private fun unlockHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { maxMode ->
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = window.windowManager.defaultDisplay.supportedModes
                modes?.maxByOrNull { it.refreshRate }?.let { maxMode ->
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun cleanupCorruptedWebViewSeed() {
        try {
            val appDataDir = applicationInfo.dataDir?.let { java.io.File(it) } ?: filesDir.parentFile
            if (appDataDir != null && appDataDir.exists()) {
                val webViewDir = java.io.File(appDataDir, "app_webview")
                if (webViewDir.exists()) {
                    listOf("variations_seed", "variations_seed_new", "variations_stamp").forEach { fileName ->
                        val file = java.io.File(webViewDir, fileName)
                        if (file.exists() && file.length() < 16L) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }
}

