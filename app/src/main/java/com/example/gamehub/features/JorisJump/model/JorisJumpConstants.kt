package com.example.gamehub.features.JorisJump.model

// Game constants for Joris Jump
const val PLAYER_WIDTH_DP = 50f
const val PLAYER_HEIGHT_DP = 75f
const val ACCELEROMETER_SENSITIVITY = 4.0f
const val GRAVITY = 0.4f
const val INITIAL_JUMP_VELOCITY = -11.5f
const val PLATFORM_HEIGHT_DP = 15f
const val PLATFORM_WIDTH_DP = 30f
const val SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR = 0.65f
const val MAX_PLATFORMS_ON_SCREEN = 10
const val INITIAL_PLATFORM_COUNT = 5
const val SCORE_POINTS_PER_DP_WORLD_Y = 0.04f
const val DEBUG_SHOW_HITBOXES = false

// Moving Platforms
const val PLATFORM_BASE_MOVE_SPEED = 1.6f
const val PLATFORM_MOVE_SPEED_VARIATION = 0.6f

// Spring Power-up
const val SPRING_VISUAL_WIDTH_FACTOR = 0.6f
const val SPRING_VISUAL_HEIGHT_FACTOR = 1.8f

// Enemies
const val ENEMY_WIDTH_DP = 40f
const val ENEMY_HEIGHT_DP = 40f
const val ENEMY_SPAWN_CHANCE_PER_PLATFORM_ROW = 0.10f
const val MAX_ENEMIES_ON_SCREEN = 3
const val ENEMY_TWITCH_AMOUNT_DP = 1f
const val ENEMY_TWITCH_SPEED_FACTOR = 0.05f 