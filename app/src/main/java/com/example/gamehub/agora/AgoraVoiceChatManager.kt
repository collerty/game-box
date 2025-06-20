package com.example.gamehub.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class AgoraVoiceChatManager(
    private val context: Context,
    private val appId: String,
    private val onJoinChannelSuccess: (String, Int, Int) -> Unit,
    private val onUserJoined: (Int, Int) -> Unit,
    private val onUserOffline: (Int, Int) -> Unit,
    private val onLeaveChannel: (IRtcEngineEventHandler.RtcStats) -> Unit
) {

    private var rtcEngine: RtcEngine? = null

    private val rtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.i("AgoraVoiceChatManager", "onJoinChannelSuccess: $channel, $uid, $elapsed")
            if (channel != null) {
                this@AgoraVoiceChatManager.onJoinChannelSuccess(channel, uid, elapsed)
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.i("AgoraVoiceChatManager", "onUserJoined: $uid, $elapsed")
            this@AgoraVoiceChatManager.onUserJoined(uid, elapsed)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Log.i("AgoraVoiceChatManager", "onUserOffline: $uid, $reason")
            this@AgoraVoiceChatManager.onUserOffline(uid, reason)
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
            Log.i("AgoraVoiceChatManager", "onLeaveChannel: $stats")
            if (stats != null) {
                this@AgoraVoiceChatManager.onLeaveChannel(stats)
            }
        }
    }

    fun initializeAndJoinChannel(channelName: String, token: String?, uid: Int) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = rtcEngineEventHandler
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            rtcEngine?.joinChannel(token, channelName, null, uid)
            Log.d("AgoraVoiceChatManager", "Attempting to join channel: $channelName with UID: $uid")
        } catch (e: Exception) {
            Log.e("AgoraVoiceChatManager", "Error initializing Agora RtcEngine: ${e.message}")
        }
    }

    fun leaveChannel() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        Log.d("AgoraVoiceChatManager", "Left channel and destroyed RtcEngine")
    }

    fun muteLocalAudioStream(mute: Boolean) {
        rtcEngine?.muteLocalAudioStream(mute)
        Log.d("AgoraVoiceChatManager", "Local audio muted: $mute")
    }

    fun enableLocalAudio(enabled: Boolean) {
        rtcEngine?.enableLocalAudio(enabled)
        Log.d("AgoraVoiceChatManager", "Local audio enabled: $enabled")
    }

    fun getRtcEngine(): RtcEngine? {
        return rtcEngine
    }
} 