package com.example.ui.panel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.overlay.FloatingPanelService
import com.example.overlay.OverlayPanelStateHolder
import com.example.ui.theme.FaizPanelTheme
import com.example.ui.theme.LocalPanelColors

@Composable
fun PanelRoute(
    viewModel: PanelViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOverlayRationaleDialog by remember { mutableStateOf(false) }
    var launchAfterPermissionReturn by remember { mutableStateOf(false) }

    val startOverlayService = remember(context) {
        {
            OverlayPanelStateHolder.setExpanded(false)
            FloatingPanelService.start(context)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Start floating overlay regardless; FGS runs cleanly even if notification is hidden
        if (Settings.canDrawOverlays(context)) {
            startOverlayService()
        }
    }

    val launchWithNotificationCheck = remember(context, startOverlayService) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startOverlayService()
            }
        }
    }

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        viewModel.updateOverlayPermission(granted)
        if (granted && launchAfterPermissionReturn) {
            launchAfterPermissionReturn = false
            launchWithNotificationCheck()
        } else {
            launchAfterPermissionReturn = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateOverlayPermission(Settings.canDrawOverlays(context))
    }

    if (showOverlayRationaleDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayRationaleDialog = false
                launchAfterPermissionReturn = true
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlaySettingsLauncher.launch(intent)
            },
            onDismiss = {
                showOverlayRationaleDialog = false
            }
        )
    }

    PanelScreen(
        uiState = uiState,
        onToggleAimbot = viewModel::toggleAimbot,
        onToggleEsp = viewModel::toggleLocationEsp,
        onToggleOverlay = {
            if (uiState.isOverlayRunning) {
                FloatingPanelService.stop(context)
            } else if (Settings.canDrawOverlays(context)) {
                launchWithNotificationCheck()
            } else {
                showOverlayRationaleDialog = true
            }
        },
        onReset = viewModel::resetPanel
    )
}

@Composable
private fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val primaryRed = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panelColors.panelTop,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = panelColors.pillText,
        title = {
            Text(
                text = stringResource(R.string.overlay_dialog_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(R.string.overlay_dialog_body),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = primaryRed)
            ) {
                Text(
                    text = stringResource(R.string.overlay_dialog_confirm),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = panelColors.mutedText)
            ) {
                Text(
                    text = stringResource(R.string.overlay_dialog_cancel),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    )
}

@Composable
fun PanelScreen(
    uiState: PanelUiState,
    onToggleAimbot: () -> Unit,
    onToggleEsp: () -> Unit,
    onToggleOverlay: () -> Unit,
    onReset: () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val primaryRed = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    val entryAlpha = remember { Animatable(0f) }
    val entryOffset = remember { Animatable(18f) }
    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        entryOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
    }

    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(bgColor)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryRed.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.12f),
                            radius = size.maxDimension * 0.52f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E1E2A).copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.92f),
                            radius = size.maxDimension * 0.55f
                        )
                    )
                }
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                PanelCardContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .alpha(entryAlpha.value)
                        .offset(y = entryOffset.value.dp)
                ) {
                    PanelHeader()

                    CrimsonGradientDivider(
                        modifier = Modifier.padding(top = 20.dp, bottom = 14.dp)
                    )

                    ActivityFeedbackBar(
                        bannerKind = uiState.bannerKind,
                        isPositiveActivation = uiState.isPositiveActivation,
                        sweepTriggerId = uiState.sweepTriggerId
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToggleOptionCard(
                            title = stringResource(R.string.option_aimbot_title),
                            isEnabled = uiState.aimbotEnabled,
                            popTrigger = uiState.aimbotPopTrigger,
                            contentDescription = stringResource(R.string.cd_toggle_aimbot),
                            cardTestTag = "aimbot_card",
                            switchTestTag = "aimbot_switch",
                            onToggle = onToggleAimbot
                        )

                        ToggleOptionCard(
                            title = stringResource(R.string.option_esp_title),
                            isEnabled = uiState.locationEspEnabled,
                            popTrigger = uiState.espPopTrigger,
                            contentDescription = stringResource(R.string.cd_toggle_esp),
                            cardTestTag = "esp_card",
                            switchTestTag = "esp_switch",
                            onToggle = onToggleEsp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FloatingOverlayControlCard(
                        hasOverlayPermission = uiState.hasOverlayPermission,
                        isOverlayRunning = uiState.isOverlayRunning,
                        onToggleOverlay = onToggleOverlay
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ResetPanelButton(
                        resetSpinTrigger = uiState.resetSpinTrigger,
                        onReset = onReset
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.footer_disclaimer),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = panelColors.footerText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingOverlayControlCard(
    hasOverlayPermission: Boolean,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val shape = MaterialTheme.shapes.medium

    val statusText = when {
        isOverlayRunning -> stringResource(R.string.overlay_status_active)
        hasOverlayPermission -> stringResource(R.string.overlay_status_ready)
        else -> stringResource(R.string.overlay_status_permission_needed)
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            isOverlayRunning -> panelColors.activeGreen
            hasOverlayPermission -> panelColors.pillText
            else -> panelColors.inactiveRed
        },
        animationSpec = tween(250),
        label = "overlay_status_color"
    )

    val buttonLabel = when {
        isOverlayRunning -> stringResource(R.string.overlay_stop_button)
        hasOverlayPermission -> stringResource(R.string.overlay_launch_button)
        else -> stringResource(R.string.overlay_grant_button)
    }

    val cardBorder by animateColorAsState(
        targetValue = if (isOverlayRunning) Color.White.copy(alpha = 0.48f) else panelColors.subtleBorder,
        animationSpec = tween(250),
        label = "overlay_card_border"
    )

    val cdOverlayLaunch = stringResource(R.string.cd_overlay_launch)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(panelColors.itemSurface)
            .border(1.dp, cardBorder, shape)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.overlay_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = statusColor
                )
            }

            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = null,
                tint = if (isOverlayRunning) Color.White else panelColors.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }

        val btnShape = RoundedCornerShape(10.dp)
        val btnBg by animateColorAsState(
            targetValue = if (isOverlayRunning) Color(0xFF1F1F26) else Color.White,
            animationSpec = tween(240),
            label = "overlay_btn_bg"
        )
        val btnTextColor by animateColorAsState(
            targetValue = if (isOverlayRunning) Color.White else Color(0xFF060608),
            animationSpec = tween(240),
            label = "overlay_btn_text"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(btnShape)
                .background(btnBg)
                .border(
                    width = 1.dp,
                    color = if (isOverlayRunning) Color.White.copy(alpha = 0.35f) else Color.White,
                    shape = btnShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = if (isOverlayRunning) Color.White else Color.Black),
                    role = Role.Button,
                    onClick = onToggleOverlay
                )
                .semantics { contentDescription = cdOverlayLaunch }
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .testTag("overlay_launch_button")
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    letterSpacing = 1.4.sp
                ),
                color = btnTextColor
            )
        }
    }
}

@Composable
private fun PanelCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(panelColors.panelTop, panelColors.panelBottom),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 1200f)
                )
            )
            .border(
                width = 1.dp,
                color = panelColors.subtleBorder,
                shape = shape
            )
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
private fun PanelHeader() {
    val panelColors = LocalPanelColors.current
    val primaryRed = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val infiniteTransition = rememberInfiniteTransition(label = "header_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot_scale"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot_alpha"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portrait_halo_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(106.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryRed.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.62f
                        )
                    }
                    .border(
                        width = 1.dp,
                        color = primaryRed.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryRed,
                                Color(0xFF272730),
                                primaryRed
                            )
                        )
                    )
                    .padding(3.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ayanokouji_portrait),
                    contentDescription = stringResource(R.string.portrait_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF0D0D11))
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val brandTitle = buildAnnotatedString {
                append(stringResource(R.string.brand_prefix))
                append(" ")
                withStyle(SpanStyle(color = primaryRed)) {
                    append(stringResource(R.string.brand_accent))
                }
                append(" ")
                append(stringResource(R.string.brand_version))
            }

            Text(
                text = brandTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = CircleShape,
                color = Color(0x0AFFFFFF),
                border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                modifier = Modifier.testTag("status_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dotScale)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(primaryRed)
                    )
                    Text(
                        text = stringResource(R.string.status_panel_demo_active).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        ),
                        color = panelColors.pillText
                    )
                }
            }
        }
    }
}

@Composable
private fun CrimsonGradientDivider(modifier: Modifier = Modifier) {
    val primaryRed = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryRed.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun ActivityFeedbackBar(
    bannerKind: PanelBannerKind,
    isPositiveActivation: Boolean,
    sweepTriggerId: Int
) {
    val panelColors = LocalPanelColors.current
    val shape = MaterialTheme.shapes.small

    val sweepProgress = remember { Animatable(-0.6f) }
    LaunchedEffect(sweepTriggerId) {
        if (sweepTriggerId > 0) {
            sweepProgress.snapTo(-0.6f)
            sweepProgress.animateTo(
                targetValue = 1.6f,
                animationSpec = tween(durationMillis = 650, easing = LinearEasing)
            )
        }
    }

    val barBackground by animateColorAsState(
        targetValue = if (isPositiveActivation) {
            panelColors.activeGreen.copy(alpha = 0.08f)
        } else {
            Color(0x07FFFFFF)
        },
        animationSpec = tween(280),
        label = "activity_bar_bg"
    )
    val barBorder by animateColorAsState(
        targetValue = if (isPositiveActivation) {
            panelColors.activeGreen.copy(alpha = 0.42f)
        } else {
            panelColors.subtleBorder
        },
        animationSpec = tween(280),
        label = "activity_bar_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isPositiveActivation) {
            panelColors.activeGreen
        } else {
            panelColors.mutedText
        },
        animationSpec = tween(280),
        label = "activity_bar_text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(barBackground)
            .border(1.dp, barBorder, shape)
            .drawBehind {
                if (isPositiveActivation && sweepProgress.value in -0.55f..1.55f) {
                    val startX = size.width * sweepProgress.value
                    val bandWidth = size.width * 0.45f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                panelColors.activeGreen.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startX = startX,
                            endX = startX + bandWidth
                        )
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("activity_bar")
    ) {
        AnimatedContent(
            targetState = bannerKind,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(150))
            },
            label = "activity_text_anim"
        ) { kind ->
            Text(
                text = stringResource(kind.messageRes),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ToggleOptionCard(
    title: String,
    isEnabled: Boolean,
    popTrigger: Int,
    contentDescription: String,
    cardTestTag: String,
    switchTestTag: String,
    onToggle: () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val shape = MaterialTheme.shapes.medium

    val popScale = remember { Animatable(1f) }
    LaunchedEffect(popTrigger) {
        if (popTrigger > 0) {
            popScale.snapTo(1f)
            popScale.animateTo(
                targetValue = 1.025f,
                animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) {
            panelColors.activeGreen.copy(alpha = 0.42f)
        } else {
            panelColors.subtleBorder
        },
        animationSpec = tween(320),
        label = "card_border"
    )

    val stateLabel = if (isEnabled) stringResource(R.string.state_on) else stringResource(R.string.state_off)
    val stateColor by animateColorAsState(
        targetValue = if (isEnabled) panelColors.activeGreen else panelColors.inactiveRed,
        animationSpec = tween(280),
        label = "state_text_color"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .scale(popScale.value)
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(panelColors.itemSurface)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .drawBehind {
                if (isEnabled) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                panelColors.activeGreen.copy(alpha = 0.06f)
                            )
                        )
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = if (isEnabled) panelColors.activeGreen else panelColors.inactiveRed
                ),
                role = Role.Switch,
                onClick = onToggle
            )
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = stateLabel
                this.role = Role.Switch
            }
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .testTag(cardTestTag)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedContent(
                targetState = isEnabled,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                },
                label = "on_off_label"
            ) { enabled ->
                Text(
                    text = if (enabled) stringResource(R.string.state_on) else stringResource(R.string.state_off),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp
                    ),
                    color = stateColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        AnimatedTacticalSwitch(
            checked = isEnabled,
            modifier = Modifier.testTag(switchTestTag)
        )
    }
}

@Composable
private fun AnimatedTacticalSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    val panelColors = LocalPanelColors.current

    val trackColor by animateColorAsState(
        targetValue = if (checked) panelColors.activeGreenTrack else panelColors.inactiveRedTrack,
        animationSpec = tween(300),
        label = "switch_track_bg"
    )
    val trackBorder by animateColorAsState(
        targetValue = if (checked) {
            panelColors.activeGreen.copy(alpha = 0.58f)
        } else {
            panelColors.inactiveRed.copy(alpha = 0.45f)
        },
        animationSpec = tween(300),
        label = "switch_track_border"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) panelColors.activeGreen else panelColors.inactiveRed,
        animationSpec = tween(300),
        label = "switch_thumb_color"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switch_thumb_offset"
    )

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 30.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(width = 1.dp, color = trackBorder, shape = CircleShape)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .align(Alignment.CenterStart)
                .size(22.dp)
                .drawBehind {
                    drawCircle(
                        color = thumbColor.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.72f
                    )
                }
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
private fun ResetPanelButton(
    resetSpinTrigger: Int,
    onReset: () -> Unit
) {
    val panelColors = LocalPanelColors.current
    val primaryRed = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)

    val rotation by animateFloatAsState(
        targetValue = resetSpinTrigger * -360f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "reset_icon_spin"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val cdReset = stringResource(R.string.cd_reset_panel)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(panelColors.inactiveRedSurface)
            .border(
                width = 1.dp,
                color = primaryRed.copy(alpha = 0.38f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = primaryRed),
                role = Role.Button,
                onClick = onReset
            )
            .semantics {
                contentDescription = cdReset
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("reset_button")
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = primaryRed,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_reset_panel),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060608)
@Composable
private fun PanelScreenPreview() {
    FaizPanelTheme {
        PanelScreen(
            uiState = PanelUiState(
                aimbotEnabled = true,
                locationEspEnabled = false,
                hasOverlayPermission = true,
                isOverlayRunning = false,
                bannerKind = PanelBannerKind.ONE_ACTIVE
            ),
            onToggleAimbot = {},
            onToggleEsp = {},
            onToggleOverlay = {},
            onReset = {}
        )
    }
}
