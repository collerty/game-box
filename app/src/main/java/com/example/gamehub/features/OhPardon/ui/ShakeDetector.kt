package com.example.gamehub.features.OhPardon

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

class ShakeDetector(
    private val shakeThreshold: Float = 12.0f,
    private val shakeCooldown: Long = 1000L,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastShakeTime: Long = 0

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (magnitude > shakeThreshold && now - lastShakeTime > shakeCooldown) {
            lastShakeTime = now
            onShake() // Trigger callback
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}