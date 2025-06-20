package com.example.gamehub.features.codenames.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.Constants

class AgoraTestActivity : ComponentActivity() {
    private var agoraEngine: RtcEngine? = null
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        setupAgoraEngine()

        setContent {
            AgoraTestScreen(
                onJoinChannel = { channelName ->
                    joinChannel(channelName)
                },
                onLeaveChannel = {
                    leaveChannel()
                }
            )
        }
    }

    private fun setupAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = "98c698ae05f4483fbd1526156480d930" // Replace with your Agora App ID
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Successfully joined channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Remote user joined", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Remote user left", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joinChannel(channelName: String) {
        val token = "" // Replace with your token if you have token authentication enabled
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        }
        agoraEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun leaveChannel() {
        agoraEngine?.leaveChannel()
    }

    private fun checkSelfPermission(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.leaveChannel()
        RtcEngine.destroy()
    }
}

@Composable
fun AgoraTestScreen(
    onJoinChannel: (String) -> Unit,
    onLeaveChannel: () -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var isJoined by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = channelName,
            onValueChange = { channelName = it },
            label = { Text("Channel Name") },
            enabled = !isJoined,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isJoined) {
                    onLeaveChannel()
                    isJoined = false
                } else {
                    if (channelName.isNotBlank()) {
                        onJoinChannel(channelName)
                        isJoined = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isJoined) "Leave Channel" else "Join Channel")
        }
    }
} 