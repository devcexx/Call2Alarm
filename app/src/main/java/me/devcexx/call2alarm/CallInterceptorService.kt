/*
 * This file is part of Call2Alarm.
 *
 * Call2Alarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Call2Alarm is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Call2Alarm. If not, see <https://www.gnu.org/licenses/>.
 */

package me.devcexx.call2alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import me.devcexx.call2alarm.preferences.RingtoneVolumeSetting
import kotlin.math.ceil
import kotlin.math.round


class CallInterceptorService: Service() {
    companion object {
        private const val NOTIF_CHANNEL_ID = "com.devcexx.call2alarm"
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var vibratorManager: VibratorManager
    private var lastKnownRingtone: Ringtone? = null
    private var lastKnownAlarmVolume: Int? = null


    val telephonyCallback: TelephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            this@CallInterceptorService.logi("Call state changed")
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    handlePhoneRinging()
                    this@CallInterceptorService.logi("Incoming call")

                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    this@CallInterceptorService.logi("Call state idle")
                    stopRinging()
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    this@CallInterceptorService.logi("Call state off hook")
                    stopRinging()
                }
                else -> {
                    this@CallInterceptorService.logi("Entered unknown call state: $state")
                }
            }
        }
    }

    val shouldInterceptCall: Boolean get() {
        // TODO This should have a more complex logic for preventing DND config to let the call
        //  go through, but for now is enough to assume that all calls are blocked while in DND.
        // TODO There're more DND modes than PRIORITY, but OnePlus uses that one.
        return !app.preferences.interceptionOnlyInZenMode ||
                notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    val shouldMuteRingtoneAudio: Boolean get() {
        if (!app.preferences.ringtoneHonorSilentModes) {
            return false
        }

        // Interception should honor vibration / silent mode.
        val ringerMode = audioManager.ringerMode
        val ringerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        // Not trusting ringer mode SILENT because it appears to be always silent when DND is enabled,
        // preferring checking the ringer volume value.
        logi("Ringer mode: $ringerMode")
        logi("Ringer volume: $ringerVolume")

        return ringerVolume == 0 ||
                ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    private fun setAlarmVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
        } catch (e: SecurityException) {
            loge("Failed to set alarm volume", e)
        }
    }

    private fun readVolumePercent(audioStream: Int): Float {
        val min = audioManager.getStreamMinVolume(audioStream)
        val max = audioManager.getStreamMaxVolume(audioStream)

        return (audioManager.getStreamVolume(audioStream) - min).toFloat() / (max - min).toFloat()
    }

    private fun adjustPercent(min: Int, max: Int, percent: Float): Int {
        return round(((max - min).toFloat() * percent) + min).toInt()
    }

    fun handlePhoneRinging() {
        lastKnownRingtone?.stop()
        if (!shouldInterceptCall) {
            logi("Call not intercepted: $shouldMuteRingtoneAudio")
            return
        }

        val uri = RingtoneManager.getActualDefaultRingtoneUri(
            applicationContext,
            RingtoneManager.TYPE_RINGTONE
        )
        val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Will manually generate a vibration
        ringtone.isHapticGeneratorEnabled = false
        lastKnownAlarmVolume = null
        if (shouldMuteRingtoneAudio) {
            logi("Ringtone will be played muted")
            ringtone.volume = 0.0f
         } else {
            val customVolumePercent: Float? = when (app.preferences.ringtoneVolumeSetting) {
                RingtoneVolumeSetting.SYSTEM_ALARM_VOLUME -> null // No need to change volume
                RingtoneVolumeSetting.SYSTEM_RING_VOLUME -> readVolumePercent(AudioManager.STREAM_RING)
                RingtoneVolumeSetting.CUSTOM_VOLUME -> app.preferences.ringtoneCustomVolume
            }

            if (customVolumePercent != null) {
                val customVolume = adjustPercent(
                    audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    customVolumePercent
                )

                logi("Will set alarm volume to ${customVolumePercent * 100.0f} ($customVolume)")
                lastKnownAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                setAlarmVolume(customVolume)
            }
        }

        ringtone.play()

        if (app.preferences.ringtoneEnableVibration) {
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 1500, 1000), 1)
            )
        }
        lastKnownRingtone = ringtone
        logi("Playing ringtone")
    }

    fun stopRinging() {
        logi("Ringtone stopped")
        vibratorManager.cancel()
        lastKnownRingtone?.stop()
        lastKnownRingtone = null
        lastKnownAlarmVolume?.let(::setAlarmVolume)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
        logi("Service stopped")
    }

    fun startForeground() {
        logi("Started foreground service")

        val channelName = "Call Interception Service"
        val chan = NotificationChannel(
            NOTIF_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(chan)

        val notification: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setOngoing(true)
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.call_interceptor_service_notification_title))
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!app.preferences.interceptionEnabled) {
            logi("Background service not initialized because call interception is not enabled")
            stopSelf()
            return START_NOT_STICKY
        }

        logi("Background service started!")
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            logi("Service has permissions to proceed.")
        } else {
            logi("Service doesn't have required permissions. Aborted.")
            stopSelf()
            return START_NOT_STICKY
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback)
        logi("Telephony callbacks installed")

        startForeground()
        logi("Initiated foreground service")

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}