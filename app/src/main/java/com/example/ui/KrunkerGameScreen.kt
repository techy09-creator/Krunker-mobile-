package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.GamingDark
import com.example.ui.theme.GamingSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val KRUNKER_URL = "https://krunker.io"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KrunkerGameScreen(
    onTriggerImmersive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customViewContainer by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var isMenuOpen by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var hudOpacity by remember { mutableFloatStateOf(0.35f) }
    var isCustomControlsEnabled by remember { mutableStateOf(true) }
    var isAutoControlsEnabled by remember { mutableStateOf(true) }
    var isInGameMatch by remember { mutableStateOf(false) }
    var isEditControlsMode by remember { mutableStateOf(false) }

    // Intercept Back button: confirm exit to avoid dropping mid-match
    BackHandler {
        showExitDialog = true
    }

    // Sync automatic controls visibility with Krunker's in-game / menu status
    LaunchedEffect(webViewRef, isAutoControlsEnabled) {
        if (!isAutoControlsEnabled) return@LaunchedEffect
        while (true) {
            val js = """
                (function() {
                    try {
                        var menuHolder = document.getElementById('menuHolder');
                        var instructions = document.getElementById('instructions');
                        var endUI = document.getElementById('endUI');

                        var isMenuOpen = (menuHolder && (menuHolder.style.display !== 'none' && menuHolder.offsetHeight > 0)) ||
                                         (instructions && (instructions.style.display !== 'none' && instructions.offsetHeight > 0)) ||
                                         (endUI && (endUI.style.display !== 'none' && endUI.offsetHeight > 0));

                        if (window.control && typeof window.control.inGame === 'boolean') {
                            return (window.control.inGame && !isMenuOpen) ? '1' : '0';
                        }
                        return (!isMenuOpen) ? '1' : '0';
                    } catch(e) {
                        return '0';
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js) { result ->
                val cleaned = result?.trim('"', ' ', '\'')
                isInGameMatch = (cleaned == "1")
            }
            kotlinx.coroutines.delay(350)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("krunker_main_container")
    ) {
        // Embed the Fullscreen WebView covering 100% of the display including notch cutout area
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("krunker_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Native window hardware acceleration without view layer overhead
                    setBackgroundColor(android.graphics.Color.BLACK)

                    // Critical WebView settings for 3D gaming
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = false
                        useWideViewPort = false
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        mediaPlaybackRequiresUserGesture = false
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        blockNetworkLoads = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        // Remove '; wv' and 'Version/4.0' so Krunker enables full WebGL2 engine path
                        val defaultUa = userAgentString
                        val cleanChromeUa = defaultUa
                            .replace("; wv", "")
                            .replace("Version/4.0 ", "")

                        userAgentString = cleanChromeUa
                    }

                    // Enable ServiceWorker disk caching for instant load of 3D assets & maps
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            android.webkit.ServiceWorkerController.getInstance().serviceWorkerWebSettings.apply {
                                cacheMode = WebSettings.LOAD_DEFAULT
                                allowContentAccess = true
                                allowFileAccess = true
                            }
                        } catch (_: Throwable) {
                        }
                    }

                    // Enable cookies for session/login persistence
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadProgress = newProgress
                            if (newProgress >= 25) {
                                isLoading = false
                            }
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            val req = request ?: return
                            val grantedResources = mutableListOf<String>()
                            for (res in req.resources) {
                                if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                                    val hasMicPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasMicPermission) {
                                        grantedResources.add(res)
                                    }
                                } else {
                                    grantedResources.add(res)
                                }
                            }
                            if (grantedResources.isNotEmpty()) {
                                req.grant(grantedResources.toTypedArray())
                            } else {
                                req.deny()
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            customViewContainer = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            customViewContainer = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            hasError = false
                            onTriggerImmersive()
                            injectUnlimitedFpsAndZeroDrawBypass(view)
                            injectHideInGameControls(view)
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            // First frame is ready, instantly show game canvas
                            isLoading = false
                            onTriggerImmersive()
                            injectUnlimitedFpsAndZeroDrawBypass(view)
                            injectHideInGameControls(view)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            onTriggerImmersive()
                            injectHideInGameControls(view)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                errorMessage = error?.description?.toString() ?: "Failed to connect"
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url ?: return false
                            val scheme = url.scheme ?: return false
                            // Allow all HTTP/HTTPS traffic (Krunker game servers, FRVR, CDNs, websockets) to load directly
                            if (scheme == "http" || scheme == "https") {
                                return false
                            }
                            return try {
                                val intent = Intent(Intent.ACTION_VIEW, url)
                                ctx.startActivity(intent)
                                true
                            } catch (_: Exception) {
                                true
                            }
                        }
                    }

                    loadUrl(KRUNKER_URL)
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
            }
        )

        // Custom HTML5 Fullscreen view container if triggered by web canvas
        if (customViewContainer != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    FrameLayout(it).apply {
                        addView(
                            customViewContainer,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }

        // Customizable On-Screen Gaming Controls Layer (with Auto In-Game Visibility Sync)
        val shouldShowControls = if (isEditControlsMode) {
            true
        } else if (isAutoControlsEnabled) {
            isInGameMatch
        } else {
            isCustomControlsEnabled
        }

        if (shouldShowControls && !isLoading) {
            CustomMobileControls(
                webView = webViewRef,
                isEditMode = isEditControlsMode,
                onExitEditMode = {
                    isEditControlsMode = false
                    onTriggerImmersive()
                }
            )
        }

        // Loading Splash Indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Game Logo
                    Image(
                        painter = painterResource(R.drawable.krunker_logo_custom),
                        contentDescription = "Krunker Logo",
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { loadProgress / 100f },
                            modifier = Modifier.size(56.dp),
                            color = Color.White,
                            strokeWidth = 3.5.dp,
                            trackColor = Color(0xFF2B2B2B)
                        )
                        Text(
                            text = "$loadProgress%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.loading_game),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Removing notch padding & launching full-screen arena…",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .width(220.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color(0xFF2B2B2B)
                    )
                }
            }
        }

        // Connection Error / Offline Screen
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(420.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.Gray))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.connection_error_title),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (errorMessage.isNotBlank()) errorMessage else stringResource(R.string.connection_error_desc),
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    hasError = false
                                    webViewRef?.loadUrl(KRUNKER_URL)
                                },
                                modifier = Modifier.testTag("retry_button")
                            ) {
                                Text(stringResource(R.string.retry_button))
                            }

                            Button(
                                onClick = {
                                    hasError = false
                                    webViewRef?.reload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.testTag("reload_button")
                            ) {
                                Text("Reload Page", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Floating Home Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .alpha(if (isMenuOpen) 1f else hudOpacity)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isMenuOpen = !isMenuOpen
                        hudOpacity = if (isMenuOpen) 1f else 0.35f
                        onTriggerImmersive()
                    }
                    .testTag("home_button_trigger"),
                color = Color(0xFF111111).copy(alpha = 0.90f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f))
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMenuOpen) Icons.Default.Close else Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Gaming Quick HUD Menu (Overlay drawer)
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101010).copy(alpha = 0.96f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.7f), Color(0xFF333333)))
                ),
                modifier = Modifier
                    .width(520.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("hud_overlay_menu")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.krunker_logo_custom),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Krunker Arena Tools",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                isMenuOpen = false
                                hudOpacity = 0.35f
                                onTriggerImmersive()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Integrated UI Tools & Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reload Match button
                        HudActionButton(
                            icon = Icons.Default.Refresh,
                            label = stringResource(R.string.reload_page),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isMenuOpen = false
                                hudOpacity = 0.35f
                                webViewRef?.reload()
                                onTriggerImmersive()
                            }
                        )

                        // Re-lock Fullscreen / Notch elimination
                        HudActionButton(
                            icon = Icons.Default.Fullscreen,
                            label = "Notch Lock",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isMenuOpen = false
                                hudOpacity = 0.35f
                                onTriggerImmersive()
                            }
                        )

                        // Edit Controls Layout (Drag & Move Buttons Anywhere)
                        HudActionButton(
                            icon = Icons.Default.TouchApp,
                            label = "Move Buttons",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isMenuOpen = false
                                isCustomControlsEnabled = true
                                isEditControlsMode = true
                                hudOpacity = 0.35f
                                onTriggerImmersive()
                            }
                        )

                        // Auto Controls / Manual Controls Cycler
                        HudActionButton(
                            icon = Icons.Default.Gamepad,
                            label = when {
                                isAutoControlsEnabled -> "Auto HUD"
                                isCustomControlsEnabled -> "Controls: ON"
                                else -> "Controls: OFF"
                            },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when {
                                    isAutoControlsEnabled -> {
                                        // Switch to Manual ON
                                        isAutoControlsEnabled = false
                                        isCustomControlsEnabled = true
                                        injectHideInGameControls(webViewRef)
                                    }
                                    isCustomControlsEnabled -> {
                                        // Switch to Manual OFF
                                        isCustomControlsEnabled = false
                                        val showAllCss = "var s = document.getElementById('__krunker_hide_duplicate_controls'); if (s) s.remove();"
                                        webViewRef?.evaluateJavascript(showAllCss, null)
                                    }
                                    else -> {
                                        // Switch back to Auto Sync
                                        isAutoControlsEnabled = true
                                        isCustomControlsEnabled = true
                                        injectHideInGameControls(webViewRef)
                                    }
                                }
                                onTriggerImmersive()
                            }
                        )

                        // Exit button
                        HudActionButton(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            label = "Exit",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isMenuOpen = false
                                hudOpacity = 0.35f
                                showExitDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAutoControlsEnabled) {
                            "Auto HUD Active • Controls hide in menu, appear upon joining match"
                        } else {
                            "Full-screen mode active • Display cutout / notch fully bypassed"
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Safe Exit Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = {
                    showExitDialog = false
                    onTriggerImmersive()
                },
                title = {
                    Text(
                        text = stringResource(R.string.exit_dialog_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.exit_dialog_desc),
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            activity?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_exit_button")
                    ) {
                        Text(stringResource(R.string.exit_confirm), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onTriggerImmersive()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier.testTag("cancel_exit_button")
                    ) {
                        Text(stringResource(R.string.exit_cancel))
                    }
                },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun HudActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF1C1C1C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Uncaps FPS engine, guarantees full 3D asset/skin loading,
 * and syncs real-time FPS 100% with Krunker's in-game FPS counter.
 */
private fun injectUnlimitedFpsAndZeroDrawBypass(webView: WebView?) {
    val js = """
        (function() {
            if (window.__krunker_fps_uncapped) return;
            window.__krunker_fps_uncapped = true;

            // WebGL Texture Completeness & NPOT Filtering Patch
            try {
                function patchGL(proto) {
                    if (!proto || proto.__krunker_patched) return;
                    proto.__krunker_patched = true;

                    var origBindTexture = proto.bindTexture;
                    var origTexImage2D = proto.texImage2D;
                    var origTexParameteri = proto.texParameteri;
                    var origDrawElements = proto.drawElements;
                    var origDrawArrays = proto.drawArrays;

                    var dummyPixel = new Uint8Array([0, 0, 0, 0]);

                    function isPot(x) {
                        return x > 0 && (x & (x - 1)) === 0;
                    }

                    proto.bindTexture = function(target, texture) {
                        origBindTexture.call(this, target, texture);
                        if (texture && !texture.__krunker_init) {
                            texture.__krunker_init = true;
                            try {
                                if (target === 0x0DE1 /* TEXTURE_2D */) {
                                    origTexImage2D.call(this, 0x0DE1, 0, 0x1908 /* RGBA */, 1, 1, 0, 0x1908 /* RGBA */, 0x1401 /* UNSIGNED_BYTE */, dummyPixel);
                                    origTexParameteri.call(this, 0x0DE1, 0x2802 /* TEXTURE_WRAP_S */, 0x812F /* CLAMP_TO_EDGE */);
                                    origTexParameteri.call(this, 0x0DE1, 0x2803 /* TEXTURE_WRAP_T */, 0x812F /* CLAMP_TO_EDGE */);
                                    origTexParameteri.call(this, 0x0DE1, 0x2801 /* TEXTURE_MIN_FILTER */, 0x2601 /* LINEAR */);
                                }
                            } catch(e) {}
                        }
                    };

                    proto.texImage2D = function(target, level, internalformat, width, height, border, format, type, pixels) {
                        var res = origTexImage2D.apply(this, arguments);
                        try {
                            var w = 0, h = 0;
                            if (typeof width === 'number' && typeof height === 'number') {
                                w = width; h = height;
                            } else if (width && width.width && width.height) {
                                w = width.width; h = width.height;
                            }
                            if (w > 0 && h > 0 && target === 0x0DE1) {
                                if (!isPot(w) || !isPot(h)) {
                                    origTexParameteri.call(this, 0x0DE1, 0x2802 /* TEXTURE_WRAP_S */, 0x812F /* CLAMP_TO_EDGE */);
                                    origTexParameteri.call(this, 0x0DE1, 0x2803 /* TEXTURE_WRAP_T */, 0x812F /* CLAMP_TO_EDGE */);
                                    origTexParameteri.call(this, 0x0DE1, 0x2801 /* TEXTURE_MIN_FILTER */, 0x2601 /* LINEAR */);
                                }
                            }
                        } catch(e) {}
                        return res;
                    };

                    proto.drawElements = function(mode, count, type, offset) {
                        if (count <= 0) return;
                        return origDrawElements.call(this, mode, count, type, offset);
                    };

                    proto.drawArrays = function(mode, first, count) {
                        if (count <= 0) return;
                        return origDrawArrays.call(this, mode, first, count);
                    };
                }

                if (window.WebGLRenderingContext) patchGL(window.WebGLRenderingContext.prototype);
                if (window.WebGL2RenderingContext) patchGL(window.WebGL2RenderingContext.prototype);
            } catch(e) {}

            try {
                localStorage.setItem('showFPS', '0');
                localStorage.removeItem('showFPS');
                localStorage.removeItem('krunker_fps_uncap');
                localStorage.removeItem('krunker_fps');

                // Comprehensive Shoe, Cosmetic, and 3D Asset Loader
                function enableAllCosmeticsAndShoes() {
                    try {
                        // 1. Update Krunker's primary krunker_settings JSON
                        var raw = localStorage.getItem('krunker_settings');
                        var s = raw ? JSON.parse(raw) : {};
                        s.showShoes = 1;
                        s.showLegs = 1;
                        s.showBody = 1;
                        s.showHats = 1;
                        s.showWaist = 1;
                        s.showSkins = 1;
                        s.showWeapons = 1;
                        s.showAttachments = 1;
                        s.loadMods = 1;
                        s.autoLoadMods = 1;
                        s.highSpec = 1;
                        s.lowSpec = 0;
                        s.showUnowned = 1;
                        s.assetCache = 1;
                        s.disableSkins = 0;
                        s.disableTextures = 0;
                        s.showFPS = 0;
                        localStorage.setItem('krunker_settings', JSON.stringify(s));

                        // 2. Individual persistent keys
                        localStorage.setItem('showShoes', '1');
                        localStorage.setItem('showLegs', '1');
                        localStorage.setItem('showBody', '1');
                        localStorage.setItem('showHats', '1');
                        localStorage.setItem('showWaist', '1');
                        localStorage.setItem('showSkins', '1');
                        localStorage.setItem('showWeapons', '1');
                        localStorage.setItem('loadMods', '1');
                        localStorage.setItem('autoLoadMods', '1');
                        localStorage.setItem('highSpec', '1');
                        localStorage.setItem('assetCache', '1');
                        localStorage.setItem('disableSkins', '0');
                        localStorage.setItem('disableTextures', '0');
                        localStorage.setItem('showFPS', '0');
                        localStorage.removeItem('lowSpec');

                        // 3. Runtime engine config object
                        if (window.config) {
                            window.config.showFPS = false;
                            window.config.lowSpec = false;
                            window.config.showShoes = true;
                            window.config.showLegs = true;
                            window.config.showBody = true;
                            window.config.showHats = true;
                            window.config.showWaist = true;
                            window.config.showSkins = true;
                            window.config.showWeapons = true;
                            window.config.loadMods = true;
                            window.config.autoLoadMods = true;
                            window.config.disableSkins = false;
                        }

                        // 4. In-memory Krunker settings window if loaded
                        if (window.windows && window.windows[0] && window.windows[0].settings) {
                            var ws = window.windows[0].settings;
                            if (ws.showShoes) ws.showShoes.val = 1;
                            if (ws.showLegs) ws.showLegs.val = 1;
                            if (ws.showBody) ws.showBody.val = 1;
                            if (ws.showHats) ws.showHats.val = 1;
                            if (ws.showWaist) ws.showWaist.val = 1;
                            if (ws.showSkins) ws.showSkins.val = 1;
                            if (ws.showWeapons) ws.showWeapons.val = 1;
                            if (ws.loadMods) ws.loadMods.val = 1;
                            if (ws.autoLoadMods) ws.autoLoadMods.val = 1;
                            if (ws.lowSpec) ws.lowSpec.val = 0;
                            if (ws.showFPS) ws.showFPS.val = 0;
                        }

                        // Maintain suppression of duplicate controls & in-game FPS counter, while enabling original game fire buttons
                        var cssId = '__krunker_hide_duplicate_controls';
                        if (!document.getElementById(cssId) && document.head) {
                            var s = document.createElement('style');
                            s.id = cssId;
                            s.innerHTML = '#fpsDisplay, #curFPS, #ingameFPS, #fps, .fps-display { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; } #mobileShoot, #mobileAimShoot, #mobileShoot2, #mobileFire, #uiBase_shoot, .mobileShoot, .mobileAimShoot, .mobileFire { opacity: 1 !important; visibility: visible !important; pointer-events: auto !important; z-index: 99 !important; } #mobileScope, #mobileAim, #uiBase_aim, .mobileScope, .mobileAim, [id*="mobileScope" i], [id*="mobileAim" i], [class*="mobileScope" i], [class*="mobileAim" i], #mobileSwap, #uiBase_swap, .mobileSwap, [id*="mobileSwap" i], [id*="swap" i], [class*="mobileSwap" i], [class*="swap" i], #mobileReload, #uiBase_reload, .mobileReload, [id*="mobileReload" i], [id*="reload" i], [class*="mobileReload" i], #mobileJump, #uiBase_jump, .mobileJump, [id*="mobileJump" i], [id*="jump" i], [class*="mobileJump" i], #mobileCrouch, #uiBase_crouch, .mobileCrouch, [id*="mobileCrouch" i], [id*="crouch" i], [id*="slide" i], [class*="mobileCrouch" i], [class*="slide" i], #mobileMelee, #uiBase_melee, .mobileMelee, [id*="mobileMelee" i], [id*="melee" i], [id*="knife" i], [class*="mobileMelee" i], [class*="melee" i], #mobileJoystick, #uiBase_leftStick, .mobileJoystick, #joystickHolder, [id*="mobileJoystick" i], [id*="joystick" i], [class*="mobileJoystick" i] { opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; }';
                            document.head.appendChild(s);
                        }
                    } catch(e) {}
                }

                enableAllCosmeticsAndShoes();
                setInterval(enableAllCosmeticsAndShoes, 2500);
            } catch(e) {}
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

/**
 * Injects CSS into the game webview to hide ONLY the 8 specified controls in the normal game,
 * plus any in-game FPS counter:
 * 1. Both fire buttons (#mobileShoot, #mobileAimShoot, etc.)
 * 2. Scope / Aim (#mobileScope, #mobileAim)
 * 3. Swap (#mobileSwap)
 * 4. Reload (#mobileReload)
 * 5. Jump (#mobileJump)
 * 6. Crouch / Slide (#mobileCrouch)
 * 7. Melee (#mobileMelee)
 * 8. Joystick (#mobileJoystick)
 *
 * Leaves all other in-game components (Esc/settings, Chat, scoreboard, health, ammo, crosshair, etc.) as is.
 */
private fun injectHideInGameControls(webView: WebView?) {
    val js = """
        (function() {
            var cssId = '__krunker_hide_duplicate_controls';
            var s = document.getElementById(cssId);
            if (!s) {
                s = document.createElement('style');
                s.id = cssId;
                document.head.appendChild(s);
            }
            s.innerHTML = '#fpsDisplay, #curFPS, #ingameFPS, #fps, .fps-display { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; } #mobileShoot, #mobileAimShoot, #mobileShoot2, #mobileFire, #uiBase_shoot, .mobileShoot, .mobileAimShoot, .mobileFire { opacity: 1 !important; visibility: visible !important; pointer-events: auto !important; z-index: 99 !important; } #mobileScope, #mobileAim, #uiBase_aim, .mobileScope, .mobileAim, [id*="mobileScope" i], [id*="mobileAim" i], [class*="mobileScope" i], [class*="mobileAim" i], #mobileSwap, #uiBase_swap, .mobileSwap, [id*="mobileSwap" i], [id*="swap" i], [class*="mobileSwap" i], [class*="swap" i], #mobileReload, #uiBase_reload, .mobileReload, [id*="mobileReload" i], [id*="reload" i], [class*="mobileReload" i], #mobileJump, #uiBase_jump, .mobileJump, [id*="mobileJump" i], [id*="jump" i], [class*="mobileJump" i], #mobileCrouch, #uiBase_crouch, .mobileCrouch, [id*="mobileCrouch" i], [id*="crouch" i], [id*="slide" i], [class*="mobileCrouch" i], [class*="slide" i], #mobileMelee, #uiBase_melee, .mobileMelee, [id*="mobileMelee" i], [id*="melee" i], [id*="knife" i], [class*="mobileMelee" i], [class*="melee" i], #mobileJoystick, #uiBase_leftStick, .mobileJoystick, #joystickHolder, [id*="mobileJoystick" i], [id*="joystick" i], [class*="mobileJoystick" i] { opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; }';
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}
