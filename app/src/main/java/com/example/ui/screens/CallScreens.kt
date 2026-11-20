package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.ActiveCallUiState
import com.example.data.model.CallSession
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PitchEmeraldDark
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import java.util.Locale

@Composable
fun rememberCallPermissionLauncher(
    onPermissionDenied: (String) -> Unit
): (callType: String, onGranted: () -> Unit) -> Unit {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val action = pendingAction
        pendingAction = null
        if (action == null) return@rememberLauncherForActivityResult
        val (callType, onGranted) = action
        val micOk = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camOk = if (callType == "video") {
            results[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else true

        if (micOk && camOk) {
            onGranted()
        } else {
            val msg = if (callType == "video") {
                "Video Call ခေါ်ဆိုရန် မိုက်ခရိုဖုန်းနှင့် ကင်မရာ ခွင့်ပြုချက် လိုအပ်ပါသည်"
            } else {
                "Voice Call ခေါ်ဆိုရန် မိုက်ခရိုဖုန်း ခွင့်ပြုချက် လိုအပ်ပါသည်"
            }
            onPermissionDenied(msg)
        }
    }

    return remember(context, launcher) {
        { callType, onGranted ->
            val needed = if (callType == "video") {
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            } else {
                arrayOf(Manifest.permission.RECORD_AUDIO)
            }
            val allGranted = needed.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                onGranted()
            } else {
                pendingAction = callType to onGranted
                launcher.launch(needed)
            }
        }
    }
}

@Composable
fun IncomingCallDialog(
    session: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isVideo = session.callType == "video"
    val isFromAdmin = session.callerRole == "admin"
    val infiniteTransition = rememberInfiniteTransition(label = "incoming_ring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "incoming_pulse"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.5.dp, ChampionshipGold),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incoming_call_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = if (isVideo) Color(0xFF0C4A6E) else PitchEmeraldDark,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = null,
                                tint = ChampionshipGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isVideo) "Video Call အဝင်" else "Voice Call အဝင်",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .scale(pulse)
                            .background(ChampionshipGold.copy(alpha = 0.16f), CircleShape)
                            .border(2.dp, ChampionshipGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFromAdmin) Icons.Default.SupportAgent else Icons.Default.Person,
                            contentDescription = null,
                            tint = ChampionshipGold,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = session.callerName.ifBlank { "Member" },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isFromAdmin) "Htet Thu Ya Admin မှ ခေါ်ဆိုနေပါသည်..."
                            else "Customer မှ ခေါ်ဆိုနေပါသည်...",
                            color = Color(0xFFC5D7CE),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AdminCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("incoming_call_decline_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ငြင်းမည်", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("incoming_call_accept_button")
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("လက်ခံမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    callState: ActiveCallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onHangup: () -> Unit,
    onAttachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachLocalRenderer: (SurfaceViewRenderer) -> Unit,
    onAttachRemoteRenderer: (SurfaceViewRenderer) -> Unit,
    onDetachRemoteRenderer: (SurfaceViewRenderer) -> Unit
) {
    BackHandler {
        onHangup()
    }

    var elapsedSeconds by remember(callState.callId, callState.connectedAtEpochMs) {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(callState.callId, callState.connectedAtEpochMs) {
        val start = callState.connectedAtEpochMs
        if (start != null) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - start) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    val statusSubtitle = when {
        callState.connectedAtEpochMs != null -> formatCallDuration(elapsedSeconds)
        callState.status == "calling" -> "ခေါ်ဆိုနေသည်..."
        callState.status == "connecting" -> "ချိတ်ဆက်နေသည်..."
        else -> "ပြီးဆုံး"
    }

    val isVideo = callState.callType == "video"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF04100B), DeepPitchBg, DeepPitchSurface)
                )
            )
            .testTag("active_call_screen")
    ) {
        if (isVideo) {
            // Remote full-screen video
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also(onAttachRemoteRenderer)
                },
                onRelease = { renderer ->
                    onDetachRemoteRenderer(renderer)
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!callState.hasRemoteVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC06140E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .background(ChampionshipGold.copy(alpha = 0.16f), CircleShape)
                                .border(2.dp, ChampionshipGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (callState.peerRole == "admin") Icons.Default.SupportAgent else Icons.Default.Person,
                                contentDescription = null,
                                tint = ChampionshipGold,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Text(
                            text = callState.peerName,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (callState.connectedAtEpochMs != null) "တစ်ဖက် Video စောင့်ဆိုင်းနေသည်..." else statusSubtitle,
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Top translucent status bar + PiP self preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (callState.peerRole == "admin") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = ChampionshipGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = callState.peerName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Video Call • $statusSubtitle",
                            color = Color(0xFFD1FAE5),
                            fontFamily = if (callState.connectedAtEpochMs != null) JetBrainsMonoFontFamily else MaterialTheme.typography.bodySmall.fontFamily,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Local PiP Preview
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 152.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B1F17))
                        .border(1.5.dp, ChampionshipGold, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState.isCameraEnabled) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).also(onAttachLocalRenderer)
                            },
                            onRelease = { renderer ->
                                onDetachLocalRenderer(renderer)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Camera Off",
                            tint = Color(0xFF9FB4A9),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        } else {
            // Voice Call Center Hero
            val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.07f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "voice_avatar_pulse"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Surface(
                    color = PitchEmeraldDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Voice Call", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(48.dp))

                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(if (callState.connectedAtEpochMs == null) scale else 1f)
                        .background(ChampionshipGold.copy(alpha = 0.14f), CircleShape)
                        .border(2.5.dp, ChampionshipGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (callState.peerRole == "admin") Icons.Default.SupportAgent else Icons.Default.Person,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = callState.peerName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (callState.peerRole == "admin") "Htet Thu Ya Admin" else "Customer",
                    color = Color(0xFFB4C8BE),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = Color(0xFF103224),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusSubtitle,
                        color = ChampionshipGold,
                        fontFamily = if (callState.connectedAtEpochMs != null) JetBrainsMonoFontFamily else MaterialTheme.typography.titleMedium.fontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Bottom Call Controls Bar
        Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlCircleButton(
                    icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (callState.isMuted) "Unmute" else "Mute",
                    active = callState.isMuted,
                    onClick = onToggleMute,
                    testTag = "call_toggle_mute"
                )

                if (isVideo) {
                    CallControlCircleButton(
                        icon = if (callState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = "Cam",
                        active = !callState.isCameraEnabled,
                        onClick = onToggleCamera,
                        testTag = "call_toggle_camera"
                    )
                    CallControlCircleButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip",
                        active = false,
                        onClick = onSwitchCamera,
                        testTag = "call_switch_camera"
                    )
                }

                CallControlCircleButton(
                    icon = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    label = "Speaker",
                    active = callState.isSpeakerOn,
                    onClick = onToggleSpeaker,
                    testTag = "call_toggle_speaker"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onHangup,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(AdminCrimson)
                            .testTag("call_hangup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "ဖုန်းချမည်",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("ချမည်", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CallControlCircleButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) ChampionshipGold else Color(0xFF1F382D))
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) InkDark else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFD7EAE1), style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatCallDuration(totalSeconds: Long): String {
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
