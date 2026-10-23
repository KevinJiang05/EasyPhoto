package com.kevin.easyphoto.ui.feedback

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View

object UserFeedback {
    fun tap(view: View, enabled: Boolean) {
        if (enabled) {
            vibrate(view, longArrayOf(0L, 35L))
        }
    }

    fun captureSuccess(view: View, hapticEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticEnabled) {
            vibrate(view, longArrayOf(0L, 65L, 55L, 90L))
        }
        if (soundEnabled) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 260)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(view: View, pattern: LongArray) {
        val vibrator = view.context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }
}
