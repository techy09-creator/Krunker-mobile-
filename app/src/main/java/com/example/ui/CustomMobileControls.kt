package com.example.ui

import android.content.Context
import android.view.KeyEvent
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Button specification for Krunker Pro Mobile Gaming HUD.
 */
data class MobileButtonSpec(
    val id: String,
    val name: String,
    val iconResId: Int? = null,
    val defaultNormX: Float, // Normalized 0..1 coordinates
    val defaultNormY: Float,
    val baseSizeDp: Int = 64,
    val isPrimary: Boolean = false
)

private val DEFAULT_BUTTON_SPECS = listOf(
    // Right Side: Combat & Movement
    // M1 Primary Action Button (Hand Palm Symbol - Left Click / Mouse 1)
    MobileButtonSpec(
        id = "btn_m1",
        name = "M1",
        iconResId = R.drawable.ic_hand_palm,
        defaultNormX = 0.88f,
        defaultNormY = 0.70f,
        baseSizeDp = 68,
        isPrimary = true
    ),
    // Scope / Aim (Scope Symbol)
    MobileButtonSpec(
        id = "btn_aim",
        name = "AIM",
        iconResId = R.drawable.ic_scope,
        defaultNormX = 0.88f,
        defaultNormY = 0.38f,
        baseSizeDp = 62
    ),
    // Jump (Arrow Up Symbol)
    MobileButtonSpec(
        id = "btn_jump",
        name = "JUMP",
        iconResId = R.drawable.ic_arrow_up,
        defaultNormX = 0.76f,
        defaultNormY = 0.76f,
        baseSizeDp = 64
    ),
    // Slide (Arrow Down Symbol)
    MobileButtonSpec(
        id = "btn_slide",
        name = "SLIDE",
        iconResId = R.drawable.ic_arrow_down,
        defaultNormX = 0.76f,
        defaultNormY = 0.54f,
        baseSizeDp = 62
    ),
    // Melee (Knife Symbol)
    MobileButtonSpec(
        id = "btn_melee",
        name = "MELEE",
        iconResId = R.drawable.ic_knife,
        defaultNormX = 0.65f,
        defaultNormY = 0.66f,
        baseSizeDp = 54
    ),
    // In-Game Spray (Spray Can Symbol)
    MobileButtonSpec(
        id = "btn_spray",
        name = "SPRAY",
        iconResId = R.drawable.ic_spray,
        defaultNormX = 0.65f,
        defaultNormY = 0.48f,
        baseSizeDp = 50
    ),
    // Reload (Magazine Symbol)
    MobileButtonSpec(
        id = "btn_reload",
        name = "RELOAD",
        iconResId = R.drawable.ic_magazine,
        defaultNormX = 0.88f,
        defaultNormY = 0.16f,
        baseSizeDp = 50
    ),
    // Swap (Gun Swap Symbol)
    MobileButtonSpec(
        id = "btn_swap",
        name = "SWAP",
        iconResId = R.drawable.ic_gun_swap,
        defaultNormX = 0.76f,
        defaultNormY = 0.30f,
        baseSizeDp = 50
    ),
    // Left Side: Analog Virtual Joystick
    MobileButtonSpec(
        id = "btn_joystick",
        name = "MOVE",
        iconResId = null,
        defaultNormX = 0.14f,
        defaultNormY = 0.70f,
        baseSizeDp = 120
    )
)

private const val PREFS_NAME = "krunker_controls_prefs"
private const val KEY_SCALE = "controls_scale"
private const val KEY_OPACITY = "controls_opacity"

@Composable
fun CustomMobileControls(
    webView: WebView?,
    isEditMode: Boolean,
    onExitEditMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Preferences for scaling and opacity
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var buttonScale by remember { mutableFloatStateOf(prefs.getFloat(KEY_SCALE, 1.0f)) }
    var buttonOpacity by remember { mutableFloatStateOf(prefs.getFloat(KEY_OPACITY, 0.80f)) }

    // Map storing normalized offsets (0..1)
    val buttonPositions = remember { mutableStateMapOf<String, Offset>() }
    var draggedButtonId by remember { mutableStateOf<String?>(null) }

    // Load saved positions
    LaunchedEffect(Unit) {
        DEFAULT_BUTTON_SPECS.forEach { spec ->
            val normX = prefs.getFloat("pos_x_${spec.id}", spec.defaultNormX)
            val normY = prefs.getFloat("pos_y_${spec.id}", spec.defaultNormY)
            buttonPositions[spec.id] = Offset(normX, normY)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Edit Mode Background Indicator
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // Top Toolbar when in Edit Mode (Always on top with zIndex 200f)
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .zIndex(200f)
        ) {
            Surface(
                color = Color(0xFF101010).copy(alpha = 0.96f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Color.White.copy(alpha = 0.85f)
                ),
                shadowElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✏️ Drag controls to ANY position",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Button Size Cycler (80% -> 100% -> 115% -> 130%)
                    Button(
                        onClick = {
                            val nextScale = when {
                                buttonScale < 0.9f -> 1.0f
                                buttonScale < 1.1f -> 1.15f
                                buttonScale < 1.25f -> 1.30f
                                else -> 0.85f
                            }
                            buttonScale = nextScale
                            prefs.edit().putFloat(KEY_SCALE, nextScale).apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202020), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(buttonScale * 100).roundToInt()}%",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    // Opacity Cycler (60% -> 80% -> 100%)
                    Button(
                        onClick = {
                            val nextOpacity = when {
                                buttonOpacity < 0.7f -> 0.80f
                                buttonOpacity < 0.9f -> 1.0f
                                else -> 0.60f
                            }
                            buttonOpacity = nextOpacity
                            prefs.edit().putFloat(KEY_OPACITY, nextOpacity).apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202020), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(buttonOpacity * 100).roundToInt()}%",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    // Reset button
                    Button(
                        onClick = {
                            val editor = prefs.edit()
                            DEFAULT_BUTTON_SPECS.forEach { spec ->
                                buttonPositions[spec.id] = Offset(spec.defaultNormX, spec.defaultNormY)
                                editor.remove("pos_x_${spec.id}")
                                editor.remove("pos_y_${spec.id}")
                            }
                            editor.apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp, color = Color.White)
                    }

                    // Save & Play button
                    Button(
                        onClick = {
                            val editor = prefs.edit()
                            buttonPositions.forEach { (id, normOffset) ->
                                editor.putFloat("pos_x_$id", normOffset.x)
                                editor.putFloat("pos_y_$id", normOffset.y)
                            }
                            editor.apply()
                            onExitEditMode()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save & Play",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Open background allows direct touch interaction with the original game's fire button and canvas

        // Render All Custom Control Buttons with full-screen movement freedom
        DEFAULT_BUTTON_SPECS.forEach { spec ->
            val normOffset = buttonPositions[spec.id] ?: Offset(spec.defaultNormX, spec.defaultNormY)
            val currentSizeDp = (spec.baseSizeDp * buttonScale).dp
            val sizePx = with(density) { currentSizeDp.toPx() }

            val maxX = (screenWidthPx - sizePx).coerceAtLeast(1f)
            val maxY = (screenHeightPx - sizePx).coerceAtLeast(1f)

            val pixelX = (normOffset.x * maxX).coerceIn(0f, maxX)
            val pixelY = (normOffset.y * maxY).coerceIn(0f, maxY)

            val isBeingDragged = isEditMode && (draggedButtonId == spec.id)

            val dragModifier = if (isEditMode) {
                Modifier.pointerInput(spec.id, screenWidthPx, screenHeightPx, sizePx) {
                    detectDragGestures(
                        onDragStart = {
                            draggedButtonId = spec.id
                        },
                        onDragEnd = {
                            draggedButtonId = null
                            val pos = buttonPositions[spec.id]
                            if (pos != null) {
                                prefs.edit()
                                    .putFloat("pos_x_${spec.id}", pos.x)
                                    .putFloat("pos_y_${spec.id}", pos.y)
                                    .apply()
                            }
                        },
                        onDragCancel = {
                            draggedButtonId = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curNorm = buttonPositions[spec.id] ?: Offset(spec.defaultNormX, spec.defaultNormY)
                            val curX = curNorm.x * maxX
                            val curY = curNorm.y * maxY
                            val newX = (curX + dragAmount.x).coerceIn(0f, maxX)
                            val newY = (curY + dragAmount.y).coerceIn(0f, maxY)
                            buttonPositions[spec.id] = Offset(newX / maxX, newY / maxY)
                        }
                    )
                }
            } else Modifier

            val buttonModifier = Modifier
                .zIndex(if (isBeingDragged) 100f else if (isEditMode) 10f else 1f)
                .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
                .then(
                    if (isBeingDragged) Modifier.scale(1.08f) else Modifier
                )
                .then(dragModifier)

            if (spec.id == "btn_joystick") {
                // Analog Movement Joystick (freely movable to any position on display)
                VirtualAnalogJoystick(
                    webView = webView,
                    isEditMode = isEditMode,
                    isBeingDragged = isBeingDragged,
                    sizeDp = currentSizeDp,
                    opacity = buttonOpacity,
                    modifier = buttonModifier
                )
            } else {
                // Action Buttons (Fire, Aim, Jump, Slide, Reload, Swap, Melee)
                ActionTouchButton(
                    spec = spec,
                    webView = webView,
                    isEditMode = isEditMode,
                    isBeingDragged = isBeingDragged,
                    sizeDp = currentSizeDp,
                    opacity = buttonOpacity,
                    modifier = buttonModifier
                )
            }
        }
    }
}

@Composable
private fun ActionTouchButton(
    spec: MobileButtonSpec,
    webView: WebView?,
    isEditMode: Boolean,
    isBeingDragged: Boolean,
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    val isToggleAim = spec.id == "btn_aim"
    val isM1Button = spec.id == "btn_m1"
    var isAimActive by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    var activePointerId by remember { mutableStateOf<PointerId?>(null) }

    val isActive = if (isToggleAim) isAimActive else isPressed

    val baseColor = when {
        isBeingDragged -> Color(0xFF242424)
        isActive -> Color.White
        else -> Color(0xFF0F0F0F)
    }

    val contentColor = when {
        isActive -> Color.Black
        else -> Color.White
    }

    val borderColor = when {
        isBeingDragged -> Color.White
        isEditMode -> Color.White.copy(alpha = 0.85f)
        isActive -> Color.White
        else -> Color.White.copy(alpha = 0.40f)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(baseColor.copy(alpha = if (isActive || isBeingDragged) 0.95f else opacity))
            .border(
                width = if (isBeingDragged) 3.5.dp else if (isEditMode || isActive) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(spec.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)

                                // Safety check: if our tracked pointer was released or lifted, release action immediately
                                if (isPressed && activePointerId != null && event.changes.none { it.id == activePointerId && it.pressed }) {
                                    activePointerId = null
                                    if (!isToggleAim) {
                                        isPressed = false
                                        dispatchGameAction(webView, spec.id, isDown = false)
                                    }
                                }

                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val downChange = event.changes.firstOrNull { it.pressed }
                                        if (downChange != null) {
                                            activePointerId = downChange.id
                                            downChange.consume()
                                            if (isToggleAim) {
                                                isAimActive = !isAimActive
                                                dispatchGameAction(webView, spec.id, isDown = isAimActive)
                                            } else {
                                                isPressed = true
                                                dispatchGameAction(webView, spec.id, isDown = true)
                                            }
                                        }
                                    }
                                    PointerEventType.Move -> {
                                        // Free 360-degree drag aiming while holding M1!
                                        if (isM1Button && isPressed && activePointerId != null) {
                                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                            if (change != null) {
                                                val dragAmount = change.position - change.previousPosition
                                                if (dragAmount.x != 0f || dragAmount.y != 0f) {
                                                    change.consume()
                                                    dispatchCameraLook(webView, dragAmount.x, dragAmount.y)
                                                }
                                            }
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        val releasedChange = if (activePointerId != null) {
                                            event.changes.firstOrNull { it.id == activePointerId }
                                        } else {
                                            event.changes.firstOrNull()
                                        }
                                        if (releasedChange == null || !releasedChange.pressed) {
                                            releasedChange?.consume()
                                            activePointerId = null
                                            if (!isToggleAim) {
                                                isPressed = false
                                                dispatchGameAction(webView, spec.id, isDown = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            )
            .testTag(spec.id),
        contentAlignment = Alignment.Center
    ) {
        if (spec.iconResId != null) {
            Icon(
                painter = painterResource(spec.iconResId),
                contentDescription = spec.name,
                tint = contentColor,
                modifier = Modifier.size((sizeDp.value * 0.52f).dp)
            )
        }
    }
}

/**
 * Analog Virtual Joystick sending real WASD movement vectors into Krunker's engine.
 * Fully movable anywhere on the display in Edit Mode.
 */
@Composable
private fun VirtualAnalogJoystick(
    webView: WebView?,
    isEditMode: Boolean,
    isBeingDragged: Boolean,
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val totalSizePx = with(density) { sizeDp.toPx() }
    val maxRadius = totalSizePx * 0.35f

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var activeDirection by remember { mutableStateOf<String?>(null) }

    val borderColor = when {
        isBeingDragged -> Color.White
        isEditMode -> Color.White.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF0A0A0A).copy(alpha = opacity * 0.75f))
            .border(
                width = if (isBeingDragged) 3.5.dp else if (isEditMode) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(totalSizePx / 2f, totalSizePx / 2f)
                                val delta = offset - center
                                val distance = sqrt(delta.x * delta.x + delta.y * delta.y)
                                val clampedDist = distance.coerceAtMost(maxRadius)
                                val angle = atan2(delta.y, delta.x)
                                thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                                activeDirection = dispatchJoystickMovement(webView, thumbOffset, maxRadius, activeDirection)
                            },
                            onDragEnd = {
                                thumbOffset = Offset.Zero
                                activeDirection = dispatchJoystickMovement(webView, Offset.Zero, maxRadius, activeDirection)
                            },
                            onDragCancel = {
                                thumbOffset = Offset.Zero
                                activeDirection = dispatchJoystickMovement(webView, Offset.Zero, maxRadius, activeDirection)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = thumbOffset + dragAmount
                                val distance = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                                val clampedDist = distance.coerceAtMost(maxRadius)
                                val angle = atan2(newOffset.y, newOffset.x)
                                thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                                activeDirection = dispatchJoystickMovement(webView, thumbOffset, maxRadius, activeDirection)
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner Thumb Pad (Black and White with Center Precision Dot)
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size((sizeDp.value * 0.42f).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = opacity))
                .border(2.dp, Color.Black.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }
    }
}

/**
 * Dispatches instant low-latency keyboard/mouse actions into Krunker's HTML5 canvas engine.
 */
private fun dispatchGameAction(webView: WebView?, buttonId: String, isDown: Boolean) {
    if (webView == null) return

    val type = if (isDown) "keydown" else "keyup"

    when (buttonId) {
        "btn_m1" -> {
            // M1 Key Action (Mouse 1 / Left Mouse Button click & hold)
            val js = """
                (function() {
                    var isDown = ${if (isDown) "true" else "false"};
                    var targets = [
                        document.getElementById('game-overlay'),
                        document.querySelector('canvas'),
                        document.body,
                        document,
                        window
                    ];
                    var cx = window.innerWidth / 2;
                    var cy = window.innerHeight / 2;

                    for (var i = 0; i < targets.length; i++) {
                        var t = targets[i];
                        if (!t) continue;
                        try {
                            var pe = new PointerEvent(isDown ? 'pointerdown' : 'pointerup', {
                                bubbles: true, cancelable: true, view: window,
                                button: 0, buttons: isDown ? 1 : 0, pointerId: 1, pointerType: 'mouse', isPrimary: true,
                                clientX: cx, clientY: cy
                            });
                            t.dispatchEvent(pe);

                            var me = new MouseEvent(isDown ? 'mousedown' : 'mouseup', {
                                bubbles: true, cancelable: true, view: window,
                                button: 0, buttons: isDown ? 1 : 0,
                                clientX: cx, clientY: cy
                            });
                            try { Object.defineProperty(me, 'which', { value: isDown ? 1 : 0 }); } catch(eW) {}
                            t.dispatchEvent(me);

                            if (isDown && typeof t.click === 'function') {
                                t.click();
                            }
                        } catch(e) {}
                    }

                    if (window.control) {
                        try {
                            if (window.control.keys && window.control.binds) {
                                var m1Key = window.control.binds.shoot || window.control.binds.fire || 0;
                                if (m1Key) window.control.keys[m1Key] = isDown ? 1 : 0;
                            }
                            if (window.control.inputs) {
                                window.control.inputs.lMouse = isDown ? 1 : 0;
                            }
                        } catch(e) {}
                    }
                    if (window.controls && window.controls.inputs) {
                        try {
                            window.controls.inputs.lMouse = isDown ? 1 : 0;
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
        "btn_aim" -> {
            // Right Click / Scope / ADS ONLY - NEVER FIRE!
            val js = """
                (function() {
                    var isDown = ${if (isDown) "true" else "false"};

                    // 1. Locate and trigger pure Scope / Aim elements in Krunker Mobile DOM
                    // STRICT: NEVER query or touch #mobileAimShoot, .mobileAimShoot, or any shoot element!
                    var scopeSelectors = [
                        '#mobileScope',
                        '#mobileAim',
                        '#uiBase_scope',
                        '#uiBase_aim',
                        '#mobileScopeBtn',
                        '#mobileAimBtn',
                        '.mobileScope',
                        '.mobileAim'
                    ];

                    for (var i = 0; i < scopeSelectors.length; i++) {
                        try {
                            var el = document.querySelector(scopeSelectors[i]);
                            if (!el) continue;

                            var rect = el.getBoundingClientRect();
                            var cx = (rect.width > 0) ? (rect.left + rect.width / 2) : (window.innerWidth * 0.85);
                            var cy = (rect.height > 0) ? (rect.top + rect.height / 2) : (window.innerHeight * 0.4);

                            // TouchEvent with button 2 (right click ADS)
                            try {
                                var touch = new Touch({
                                    identifier: 9998,
                                    target: el,
                                    clientX: cx,
                                    clientY: cy,
                                    pageX: cx,
                                    pageY: cy,
                                    screenX: cx,
                                    screenY: cy,
                                    radiusX: 10,
                                    radiusY: 10
                                });
                                var te = new TouchEvent(isDown ? 'touchstart' : 'touchend', {
                                    bubbles: true,
                                    cancelable: true,
                                    view: window,
                                    touches: isDown ? [touch] : [],
                                    targetTouches: isDown ? [touch] : [],
                                    changedTouches: [touch]
                                });
                                el.dispatchEvent(te);
                            } catch(eT) {}

                            // PointerEvent: button 2 (Right Click / ADS)
                            try {
                                var pe = new PointerEvent(isDown ? 'pointerdown' : 'pointerup', {
                                    bubbles: true,
                                    cancelable: true,
                                    view: window,
                                    pointerId: 9998,
                                    pointerType: 'touch',
                                    isPrimary: true,
                                    clientX: cx,
                                    clientY: cy,
                                    button: 2,
                                    buttons: isDown ? 2 : 0
                                });
                                el.dispatchEvent(pe);
                            } catch(eP) {}

                            // MouseEvent: button 2 (Right Click / ADS)
                            try {
                                var me = new MouseEvent(isDown ? 'mousedown' : 'mouseup', {
                                    bubbles: true,
                                    cancelable: true,
                                    view: window,
                                    clientX: cx,
                                    clientY: cy,
                                    button: 2,
                                    buttons: isDown ? 2 : 0
                                });
                                try { Object.defineProperty(me, 'which', { value: isDown ? 3 : 0 }); } catch(eW) {}
                                el.dispatchEvent(me);
                            } catch(eM) {}

                            // Mobile scope buttons toggle on tap (invoke on both transitions so Krunker mobile ADS toggles on and off)
                            try {
                                if (typeof el.click === 'function') {
                                    el.click();
                                }
                            } catch(eClk) {}
                        } catch(e) {}
                    }

                    // 2. Dispatch Right Click (Mouse button 2, which: 3) to Canvas & Game Container
                    var targets = [
                        document.querySelector('canvas'),
                        document.getElementById('game-overlay'),
                        document.body,
                        document,
                        window
                    ];

                    for (var k = 0; k < targets.length; k++) {
                        var target = targets[k];
                        if (!target) continue;
                        try {
                            var mx = window.innerWidth / 2;
                            var my = window.innerHeight / 2;

                            var pe = new PointerEvent(isDown ? 'pointerdown' : 'pointerup', {
                                bubbles: true,
                                cancelable: true,
                                view: window,
                                pointerId: 2,
                                pointerType: 'mouse',
                                isPrimary: true,
                                clientX: mx,
                                clientY: my,
                                button: 2,
                                buttons: isDown ? 2 : 0
                            });
                            try { Object.defineProperty(pe, 'which', { value: isDown ? 3 : 0 }); } catch(eW) {}
                            target.dispatchEvent(pe);

                            var me = new MouseEvent(isDown ? 'mousedown' : 'mouseup', {
                                bubbles: true,
                                cancelable: true,
                                view: window,
                                clientX: mx,
                                clientY: my,
                                button: 2,
                                buttons: isDown ? 2 : 0
                            });
                            try { Object.defineProperty(me, 'which', { value: isDown ? 3 : 0 }); } catch(eW) {}
                            target.dispatchEvent(me);
                        } catch(e) {}
                    }

                    // 3. Direct Krunker internal controller object if exposed - NEVER fire!
                    if (window.control) {
                        try {
                            if (window.control.keys && window.control.binds) {
                                var aimKey = window.control.binds.aim || window.control.binds.scope || window.control.binds.zoom;
                                if (aimKey !== undefined) window.control.keys[aimKey] = isDown ? 1 : 0;
                            }
                            if (window.control.inputs) {
                                window.control.inputs.rMouse = isDown ? 1 : 0;
                                window.control.inputs.aim = isDown ? 1 : 0;
                                window.control.inputs.scope = isDown ? 1 : 0;
                                // CRITICAL: Guarantee never fire when aiming!
                                window.control.inputs.lMouse = 0;
                            }
                            if (typeof window.control.setAim === 'function') {
                                window.control.setAim(isDown);
                            }
                            if (typeof window.control.setScope === 'function') {
                                window.control.setScope(isDown);
                            }
                        } catch(e) {}
                    }
                    if (window.controls) {
                        try {
                            if (window.controls.keys) {
                                if (window.controls.keys[2] !== undefined) window.controls.keys[2] = isDown ? 1 : 0;
                                if (window.controls.keys[3] !== undefined) window.controls.keys[3] = isDown ? 1 : 0;
                            }
                            if (window.controls.inputs) {
                                window.controls.inputs.rMouse = isDown ? 1 : 0;
                                window.controls.inputs.aim = isDown ? 1 : 0;
                                window.controls.inputs.scope = isDown ? 1 : 0;
                                // CRITICAL: Guarantee never fire when aiming!
                                window.controls.inputs.lMouse = 0;
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
        "btn_jump" -> {
            // Spacebar (Jump / Bhop)
            dispatchKey(webView, type, "Space", 32)
            triggerMobileElement(webView, "mobileJump", isDown)
        }
        "btn_slide" -> {
            // ShiftLeft / KeyC (Crouch / Slide)
            dispatchKey(webView, type, "ShiftLeft", 16)
            dispatchKey(webView, type, "KeyC", 67)
            triggerMobileElement(webView, "mobileCrouch", isDown)
        }
        "btn_melee" -> {
            // Melee: Knife in Krunker default is KeyQ (81)
            dispatchKey(webView, type, "KeyQ", 81)
            triggerMobileElement(webView, "mobileMelee", isDown)
        }
        "btn_swap" -> {
            // Swap: Secondary Weapon in Krunker default is KeyE (69)
            dispatchKey(webView, type, "KeyE", 69)
            triggerMobileElement(webView, "mobileSwap", isDown)
        }
        "btn_reload" -> {
            // KeyR (Reload)
            dispatchKey(webView, type, "KeyR", 82)
            triggerMobileElement(webView, "mobileReload", isDown)
        }
        "btn_spray" -> {
            // KeyF (Spray / Graffiti in Krunker default)
            dispatchKey(webView, type, "KeyF", 70)
            triggerMobileElement(webView, "mobileSpray", isDown)
            val js = """
                (function() {
                    var isDown = ${if (isDown) "true" else "false"};
                    if (window.control) {
                        try {
                            if (window.control.keys && window.control.binds) {
                                var sprayKey = window.control.binds.spray || window.control.binds.graffiti || 70;
                                window.control.keys[sprayKey] = isDown ? 1 : 0;
                            }
                            if (isDown && typeof window.control.spray === 'function') {
                                window.control.spray();
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }
}

private fun triggerMobileElement(webView: WebView, elementId: String, isDown: Boolean) {
    val js = """
        (function() {
            var el = document.getElementById('$elementId') || document.querySelector('[id*="$elementId" i]');
            if (el) {
                try {
                    var pe = new PointerEvent('${if (isDown) "pointerdown" else "pointerup"}', {
                        bubbles: true, cancelable: true, view: window, button: 0, buttons: ${if (isDown) 1 else 0}
                    });
                    el.dispatchEvent(pe);
                    var me = new MouseEvent('${if (isDown) "mousedown" else "mouseup"}', {
                        bubbles: true, cancelable: true, view: window, button: 0, buttons: ${if (isDown) 1 else 0}
                    });
                    el.dispatchEvent(me);
                    if (${if (isDown) "true" else "false"} && typeof el.click === 'function') {
                        el.click();
                    }
                } catch(e) {}
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

private fun dispatchKey(webView: WebView, eventType: String, code: String, keyCode: Int) {
    val js = """
        (function() {
            var evt = new KeyboardEvent('$eventType', {
                bubbles: true,
                cancelable: true,
                key: '${if (code == "Space") " " else code}',
                code: '$code',
                keyCode: $keyCode,
                which: $keyCode,
                view: window
            });
            window.dispatchEvent(evt);
            document.dispatchEvent(evt);
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

/**
 * Converts analog joystick angle & magnitude into digital WASD vectors with 8-way directional movement.
 */
private fun dispatchJoystickMovement(
    webView: WebView?,
    offset: Offset,
    maxRadius: Float,
    currentDirection: String?
): String? {
    if (webView == null) return null

    val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
    val threshold = maxRadius * 0.25f

    val newDir = if (distance < threshold) {
        null
    } else {
        val angleDeg = Math.toDegrees(atan2(offset.y.toDouble(), offset.x.toDouble()))
        when {
            angleDeg in -22.5..22.5 -> "D" // Right
            angleDeg in 22.5..67.5 -> "SD" // Down-Right
            angleDeg in 67.5..112.5 -> "S" // Down
            angleDeg in 112.5..157.5 -> "SA" // Down-Left
            angleDeg > 157.5 || angleDeg < -157.5 -> "A" // Left
            angleDeg in -157.5..-112.5 -> "WA" // Up-Left
            angleDeg in -112.5..-67.5 -> "W" // Up
            angleDeg in -67.5..-22.5 -> "WD" // Up-Right
            else -> null
        }
    }

    if (newDir != currentDirection) {
        // Release previous keys
        currentDirection?.forEach { char ->
            when (char) {
                'W' -> dispatchKey(webView, "keyup", "KeyW", 87)
                'A' -> dispatchKey(webView, "keyup", "KeyA", 65)
                'S' -> dispatchKey(webView, "keyup", "KeyS", 83)
                'D' -> dispatchKey(webView, "keyup", "KeyD", 68)
            }
        }
        // Press new keys
        newDir?.forEach { char ->
            when (char) {
                'W' -> dispatchKey(webView, "keydown", "KeyW", 87)
                'A' -> dispatchKey(webView, "keydown", "KeyA", 65)
                'S' -> dispatchKey(webView, "keydown", "KeyS", 83)
                'D' -> dispatchKey(webView, "keydown", "KeyD", 68)
            }
        }
    }

    return newDir
}

/**
 * Dispatches smooth mousemove & pointermove camera rotation vectors into Krunker's 3D canvas and engine.
 * Provides completely free 360-degree aim dragging for both background gestures and fire-button dragging.
 */
private fun dispatchCameraLook(webView: WebView?, deltaX: Float, deltaY: Float) {
    if (webView == null || (deltaX == 0f && deltaY == 0f)) return
    val js = """
        (function() {
            var dx = $deltaX;
            var dy = $deltaY;
            var sens = 0.0075; // Highly responsive 360-degree mobile FPS aiming
            var canvas = document.querySelector('canvas') || document.getElementById('game-overlay') || document.body;

            // 1. Dispatch mousemove & pointermove with movementX and movementY deltas across canvas, document, window
            var evt = new MouseEvent('mousemove', {
                bubbles: true,
                cancelable: true,
                movementX: dx,
                movementY: dy,
                clientX: window.innerWidth / 2,
                clientY: window.innerHeight / 2
            });
            if (canvas) canvas.dispatchEvent(evt);
            document.dispatchEvent(evt);
            window.dispatchEvent(evt);

            var pe = new PointerEvent('pointermove', {
                bubbles: true,
                cancelable: true,
                movementX: dx,
                movementY: dy,
                clientX: window.innerWidth / 2,
                clientY: window.innerHeight / 2
            });
            if (canvas) canvas.dispatchEvent(pe);
            document.dispatchEvent(pe);
            window.dispatchEvent(pe);

            // 2. Direct hook into Krunker player camera target rotation (pitch & yaw)
            if (window.control) {
                try {
                    if (window.control.target && window.control.target.rot) {
                        window.control.target.rot[1] -= dx * sens; // yaw (left/right 360 degrees)
                        window.control.target.rot[0] -= dy * sens; // pitch (up/down)
                        var halfPi = Math.PI / 2 - 0.01;
                        if (window.control.target.rot[0] > halfPi) window.control.target.rot[0] = halfPi;
                        if (window.control.target.rot[0] < -halfPi) window.control.target.rot[0] = -halfPi;
                    }
                    if (typeof window.control.xDr === 'number') {
                        window.control.xDr += dx * sens;
                    }
                    if (typeof window.control.yDr === 'number') {
                        window.control.yDr += dy * sens;
                    }
                    if (window.control.cam && window.control.cam.rot) {
                        window.control.cam.rot.y -= dx * sens;
                        window.control.cam.rot.x -= dy * sens;
                    }
                } catch(e) {}
            }
            if (window.controls) {
                try {
                    if (window.controls.target && window.controls.target.rot) {
                        window.controls.target.rot[1] -= dx * sens;
                        window.controls.target.rot[0] -= dy * sens;
                        var halfPi = Math.PI / 2 - 0.01;
                        if (window.controls.target.rot[0] > halfPi) window.controls.target.rot[0] = halfPi;
                        if (window.controls.target.rot[0] < -halfPi) window.controls.target.rot[0] = -halfPi;
                    }
                } catch(e) {}
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}
