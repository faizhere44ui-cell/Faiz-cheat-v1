package com.example.overlay

import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import kotlin.math.roundToInt

private val MonoBlack = Color(0xFF08080A)
private val MonoSurface = Color(0xFF131317)
private val MonoWhite = Color(0xFFFFFFFF)
private val MonoOffWhite = Color(0xFFE5E5EA)
private val MonoMuted = Color(0xFF8C8C96)
private val MonoBorder = Color(0x3DFFFFFF)
private val MonoDivider = Color(0x22FFFFFF)

@Composable
fun FloatingOverlayContent(
    state: OverlaySharedState,
    onToggleExpanded: () -> Unit,
    onToggleAimbot: () -> Unit,
    onToggleEsp: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    onDragDelta: (dx: Int, dy: Int) -> Unit
) {
    val hostView = LocalView.current

    AnimatedContent(
        targetState = state.isExpanded,
        transitionSpec = {
            (fadeIn(tween(180)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180))) togetherWith
                (fadeOut(tween(140)) + scaleOut(targetScale = 0.88f, animationSpec = tween(140)))
        },
        label = "overlay_expand_collapse"
    ) { expanded ->
        if (!expanded) {
            FloatingAyanokoujiIconBubble(
                hasActiveToggle = state.aimbotEnabled || state.locationEspEnabled,
                hostView = hostView,
                onClick = onToggleExpanded,
                onDragDelta = onDragDelta
            )
        } else {
            FloatingMinimalPanelCard(
                state = state,
                hostView = hostView,
                onMinimize = onToggleExpanded,
                onToggleAimbot = onToggleAimbot,
                onToggleEsp = onToggleEsp,
                onReset = onReset,
                onClose = onClose,
                onDragDelta = onDragDelta
            )
        }
    }
}

@Composable
private fun FloatingAyanokoujiIconBubble(
    hasActiveToggle: Boolean,
    hostView: View,
    onClick: () -> Unit,
    onDragDelta: (dx: Int, dy: Int) -> Unit
) {
    val cdBubble = stringResource(R.string.cd_overlay_bubble)
    val loc = remember { IntArray(2) }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(56.dp)
            .pointerInput(Unit) {
                var lastScreenX = 0f
                var lastScreenY = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        hostView.getLocationOnScreen(loc)
                        lastScreenX = loc[0] + offset.x
                        lastScreenY = loc[1] + offset.y
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        hostView.getLocationOnScreen(loc)
                        val currentScreenX = loc[0] + change.position.x
                        val currentScreenY = loc[1] + change.position.y
                        val dx = (currentScreenX - lastScreenX).roundToInt()
                        val dy = (currentScreenY - lastScreenY).roundToInt()
                        if (dx != 0 || dy != 0) {
                            lastScreenX += dx
                            lastScreenY += dy
                            onDragDelta(dx, dy)
                        }
                    }
                )
            }
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(MonoBlack)
            .border(
                width = if (hasActiveToggle) 2.dp else 1.5.dp,
                color = if (hasActiveToggle) MonoWhite else MonoWhite.copy(alpha = 0.72f),
                shape = CircleShape
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = cdBubble
            }
            .testTag("floating_bubble_icon")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_ayanokouji_portrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
        )

        if (hasActiveToggle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(MonoWhite)
                    .border(1.5.dp, MonoBlack, CircleShape)
            )
        }
    }
}

@Composable
private fun FloatingMinimalPanelCard(
    state: OverlaySharedState,
    hostView: View,
    onMinimize: () -> Unit,
    onToggleAimbot: () -> Unit,
    onToggleEsp: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    onDragDelta: (dx: Int, dy: Int) -> Unit
) {
    val loc = remember { IntArray(2) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .padding(4.dp)
            .width(244.dp)
            .shadow(16.dp, shape)
            .clip(shape)
            .background(MonoBlack.copy(alpha = 0.96f))
            .border(1.dp, MonoBorder, shape)
            .padding(12.dp)
            .testTag("floating_expanded_panel")
    ) {
        // Draggable Minimal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var lastScreenX = 0f
                    var lastScreenY = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            hostView.getLocationOnScreen(loc)
                            lastScreenX = loc[0] + offset.x
                            lastScreenY = loc[1] + offset.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            hostView.getLocationOnScreen(loc)
                            val currentScreenX = loc[0] + change.position.x
                            val currentScreenY = loc[1] + change.position.y
                            val dx = (currentScreenX - lastScreenX).roundToInt()
                            val dy = (currentScreenY - lastScreenY).roundToInt()
                            if (dx != 0 || dy != 0) {
                                lastScreenX += dx
                                lastScreenY += dy
                                onDragDelta(dx, dy)
                            }
                        }
                    )
                }
        ) {
            // Tap icon + title to minimize back to floating bubble
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onMinimize)
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ayanokouji_portrait),
                    contentDescription = stringResource(R.string.cd_overlay_minimize),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, MonoWhite.copy(alpha = 0.8f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.overlay_brand_compact),
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.6.sp,
                        color = MonoWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = null,
                    tint = MonoMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayHeaderIconButton(
                    onClick = onMinimize,
                    contentDescription = stringResource(R.string.cd_overlay_minimize),
                    testTag = "overlay_minimize_button"
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = null,
                        tint = MonoOffWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OverlayHeaderIconButton(
                    onClick = onClose,
                    contentDescription = stringResource(R.string.cd_overlay_close),
                    testTag = "overlay_close_button"
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MonoWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MonoDivider)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // AIMBOT Visual Toggle
        MonoToggleRow(
            label = stringResource(R.string.option_aimbot_title),
            checked = state.aimbotEnabled,
            testTag = "overlay_aimbot_row",
            onClick = onToggleAimbot
        )

        Spacer(modifier = Modifier.height(8.dp))

        // LOCATION ESP Visual Toggle
        MonoToggleRow(
            label = stringResource(R.string.option_esp_title),
            checked = state.locationEspEnabled,
            testTag = "overlay_esp_row",
            onClick = onToggleEsp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Minimal Footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.overlay_demo_tag),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = MonoMuted
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MonoSurface)
                    .border(1.dp, MonoDivider, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("overlay_reset_button")
            ) {
                Text(
                    text = stringResource(R.string.overlay_reset_compact),
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    color = MonoWhite
                )
            }
        }
    }
}

@Composable
private fun OverlayHeaderIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    icon: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MonoSurface)
            .border(1.dp, MonoDivider, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MonoWhite, bounded = true),
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    ) {
        icon()
    }
}

@Composable
private fun MonoToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val rowShape = RoundedCornerShape(10.dp)
    val borderColor by animateColorAsState(
        targetValue = if (checked) MonoWhite.copy(alpha = 0.65f) else MonoDivider,
        animationSpec = tween(220),
        label = "mono_row_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF1A1A20) else MonoSurface,
        animationSpec = tween(220),
        label = "mono_row_bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(bgColor)
            .border(1.dp, borderColor, rowShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MonoWhite),
                role = Role.Switch,
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.2.sp,
                color = MonoWhite
            )
            Text(
                text = if (checked) stringResource(R.string.state_on) else stringResource(R.string.state_off),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                color = if (checked) MonoWhite else MonoMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        MonoPillSwitch(checked = checked)
    }
}

@Composable
private fun MonoPillSwitch(checked: Boolean) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) MonoWhite else Color(0xFF1D1D24),
        animationSpec = tween(240),
        label = "mono_track_bg"
    )
    val trackBorder by animateColorAsState(
        targetValue = if (checked) MonoWhite else MonoWhite.copy(alpha = 0.35f),
        animationSpec = tween(240),
        label = "mono_track_border"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MonoBlack else MonoWhite,
        animationSpec = tween(240),
        label = "mono_thumb_color"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mono_thumb_offset"
    )

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(CircleShape)
            .background(trackBg)
            .border(1.dp, trackBorder, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .align(Alignment.CenterStart)
                .size(18.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
