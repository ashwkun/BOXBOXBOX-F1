package com.f1tracker.ui.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.max

/**
 * A data class to hold parallax offset values (-1f to 1f range)
 */
data class ParallaxState(
    val horizontalBias: Float = 0f,
    val verticalBias: Float = 0f
)

/**
 * Remembers a ParallaxState derived from device rotation sensors.
 * 
 * @param maxBias The maximum output value for bias (clamped between -1 and 1 usually, but here scaled)
 * @param smoothingFactor How much smoothing to apply (0.0 - 1.0)
 */
@Composable
fun rememberParallaxState(
    maxRotationDegrees: Float = 10f
): State<ParallaxState> {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    
    val parallaxState = remember { mutableStateOf(ParallaxState()) }
    
    if (sensor == null) return parallaxState // Fallback for no sensor (emulator etc)

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        
                        // orientation[1] is pitch (x-axis tilt), orientation[2] is roll (y-axis tilt)
                        // values are in radians
                        
                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        
                        // Normalize and clamp
                        // We clamp to a small range (e.g. +/- 10 degrees) so the user doesn't have to tilt wildly
                        val normPitch = (pitch.coerceIn(-maxRotationDegrees, maxRotationDegrees) / maxRotationDegrees)
                        val normRoll = (roll.coerceIn(-maxRotationDegrees, maxRotationDegrees) / maxRotationDegrees)
                        
                        // For landscape/portrait awareness, we might need to swap, but usually 
                        // Roll drives horizontal parallax in portrait mode.
                        
                        // Inverted roll for natural "window" feel (move phone right -> look left)
                        // Or non-inverted for "layer" feel (move phone right -> layer slides left)
                        
                        parallaxState.value = ParallaxState(
                            horizontalBias = normRoll, 
                            verticalBias = normPitch // orientation[1] is negative when tilting top back
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    return parallaxState
}
