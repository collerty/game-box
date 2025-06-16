package com.example.gamehub.features.test

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine


private const val AGORA_APP_ID = "6f014a9226ca454f80aef61ea7416ecc"
private const val AGORA_CHANNEL_NAME = "YourVoiceChannel" // desired channel name
private const val AGORA_TOKEN = "" // empty in test mode

// Global variable for Agora RtcEngine
private var mRtcEngine: RtcEngine? = null

@Composable
fun VoiceChatTestPage() {
    val context = LocalContext.current
    var isJoined by remember { mutableStateOf(false) }
    val TAG = "VoiceChatTestPage"

    // Callback for Agora RtcEngine events
    val mRtcEventHandler = remember {
        object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.i(TAG, "Joined channel $channel with UID $uid")
                isJoined = true
                Toast.makeText(context, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.i(TAG, "User $uid joined")
                Toast.makeText(context, "User $uid joined", Toast.LENGTH_SHORT).show()
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.i(TAG, "User $uid offline, reason: $reason")
                Toast.makeText(context, "User $uid offline", Toast.LENGTH_SHORT).show()
            }

            override fun onLeaveChannel(stats: RtcStats?) {
                Log.i(TAG, "Left channel")
                isJoined = false
                Toast.makeText(context, "Left channel", Toast.LENGTH_SHORT).show()
            }

            override fun onError(err: Int) {
                Log.e(TAG, "Agora Error: $err")
                Toast.makeText(context, "Agora Error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Request permissions launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true &&
            permissions[Manifest.permission.INTERNET] == true &&
            permissions[Manifest.permission.ACCESS_NETWORK_STATE] == true
        ) {
            Log.d(TAG, "Permissions granted: RECORD_AUDIO, INTERNET, ACCESS_NETWORK_STATE")
            initializeAgoraEngine(context, mRtcEventHandler)
        } else {
            Log.e(TAG, "Permissions denied")
            Toast.makeText(context, "Permissions required for voice chat", Toast.LENGTH_LONG).show()
        }
    }

    // Initialize Agora engine when the Composable is first launched
    LaunchedEffect(Unit) {
        if (!checkPermissions(context)) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE
                )
            )
        } else {
            Log.d(TAG, "Permissions already granted. Initializing Agora engine.")
            initializeAgoraEngine(context, mRtcEventHandler)
        }
    }

    // Clean up Agora engine when the Composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
            Log.d(TAG, "Agora RtcEngine destroyed on dispose.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Voice Chat Test Page",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (mRtcEngine == null) {
            CircularProgressIndicator()
            Text("Initializing Agora Engine...")
        } else {
            Button(
                onClick = {
                    if (!isJoined) {
                        joinChannel()
                    } else {
                        Toast.makeText(context, "Already joined channel!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = mRtcEngine != null && !isJoined,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join Voice Chat")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { leaveChannel() },
                enabled = isJoined,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Voice Chat")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isJoined) "Status: Connected to ${AGORA_CHANNEL_NAME}" else "Status: Disconnected",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Checks if the required permissions are granted.
 */
private fun checkPermissions(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
}

/**
 * Initializes the Agora RtcEngine.
 */
private fun initializeAgoraEngine(context: android.content.Context, handler: IRtcEngineEventHandler) {
    if (AGORA_APP_ID == "YOUR_AGORA_APP_ID") {
        Toast.makeText(context, "Please set your Agora App ID in VoiceChatTestPage.kt", Toast.LENGTH_LONG).show()
        return
    }

    try {
        mRtcEngine = RtcEngine.create(context, AGORA_APP_ID, handler)
        Log.d("VoiceChatTestPage", "Agora RtcEngine initialized successfully.")
    } catch (e: Exception) {
        Log.e("VoiceChatTestPage", "Failed to initialize Agora RtcEngine: ${e.message}")
        Toast.makeText(context, "Failed to initialize Agora Engine: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Joins the Agora voice channel.
 */
private fun joinChannel() {
    if (mRtcEngine == null) {
        Log.e("VoiceChatTestPage", "RtcEngine is not initialized.")
        return
    }
    // Set the channel profile to communication for voice chat
    mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
    // Join the channel
    val result = mRtcEngine?.joinChannel(AGORA_TOKEN, AGORA_CHANNEL_NAME, null, 0)
    if (result == 0) {
        Log.d("VoiceChatTestPage", "Attempting to join channel $AGORA_CHANNEL_NAME")
    } else {
        Log.e("VoiceChatTestPage", "Failed to join channel: $result")
    }
}

/**
 * Leaves the Agora voice channel.
 */
private fun leaveChannel() {
    mRtcEngine?.leaveChannel()
    Log.d("VoiceChatTestPage", "Left channel.")
}
