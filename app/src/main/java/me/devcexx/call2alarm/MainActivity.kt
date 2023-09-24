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
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devcexx.call2alarm.preferences.RingtoneVolumeSetting
import me.devcexx.call2alarm.preferences.ui.PreferenceSet
import me.devcexx.call2alarm.ui.theme.Call2AlarmTheme


class MainActivity : ComponentActivity() {
    private val permissionRequester = PermissionRequester(this)

    private val interceptionEnabled by lazy { (app.preferences::interceptionEnabled).asState }
    private val interceptionOnlyInZenMode by lazy { (app.preferences::interceptionOnlyInZenMode).asState }
    private val startOnBoot by lazy { (app.preferences::startOnBoot).asState }
    private val ringtoneEnableVibration by lazy { (app.preferences::ringtoneEnableVibration).asState }
    private val ringtoneHonorSilentModes by lazy { (app.preferences::ringtoneHonorSilentModes).asState }
    private val ringtoneVolumeSetting by lazy { (app.preferences::ringtoneVolumeSetting).asState }
    private val ringtoneCustomVolume by lazy { (app.preferences::ringtoneCustomVolume).asState }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureServiceInCorrectState()
        setContent {
            Call2AlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainContent() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .overscroll(ScrollableDefaults.overscrollEffect()),
            verticalArrangement = Arrangement.Top
        ) {

            Preferences()
        }
    }

    @Composable
    fun Preferences() {
        val (interceptionIsEnabled, _) = interceptionEnabled
        val (ringtoneVolumeSettingValue, _) = ringtoneVolumeSetting
        PreferenceSet(context = applicationContext) {
            PreferenceGroup(titleId = R.string.pref_group_global) {
                SwitchPreference(
                    titleId = R.string.pref_enable_call_handling_title,
                    subtitleId = R.string.pref_enable_call_handling_subtitle,
                    state = interceptionEnabled,
                    onChange = {
                        ensureServiceInCorrectState()
                    })
            }

            PreferenceSet(enabled = interceptionIsEnabled) {
                PreferenceGroup(titleId = R.string.pref_group_general) {
                    SwitchPreference(
                        titleId = R.string.pref_handle_while_dnd_title,
                        subtitleId = null,
                        state = interceptionOnlyInZenMode
                    )
                    SwitchPreference(
                        titleId = R.string.pref_start_on_boot_title,
                        subtitleId = R.string.pref_start_on_boot_subtitle,
                        state = startOnBoot
                    )
                }
                PreferenceGroup(titleId = R.string.pref_group_ringtone) {
                    SwitchPreference(
                        titleId = R.string.pref_ringtone_enable_vibration_title,
                        subtitleId = null,
                        state = ringtoneEnableVibration
                    )

                    SwitchPreference(
                        titleId = R.string.pref_ringtone_honor_silent_modes_title,
                        subtitleId = R.string.pref_ringtone_honor_silent_modes_subtitle,
                        state = ringtoneHonorSilentModes
                    )
                    ComboPreference(
                        titleId = R.string.pref_ringtone_volume_title,
                        ringtoneVolumeSetting,
                        RingtoneVolumeSetting
                    )

                    if (ringtoneVolumeSettingValue == RingtoneVolumeSetting.CUSTOM_VOLUME) {
                        SliderPreference(
                            titleId = R.string.pref_ringtone_custom_volume_title,
                            ringtoneCustomVolume
                        )
                    }
                }
            }
        }
    }

    private fun ensureServiceInCorrectState() {
        GlobalScope.launch(Dispatchers.Main) {
            logi(Thread.currentThread().toString())
            val intent = Intent(baseContext, CallInterceptorService::class.java)
            if (interceptionEnabled.value) {
                if (permissionRequester.requestPermission(Manifest.permission.READ_PHONE_STATE) &&
                    permissionRequester.requestPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
                    permissionRequester.requestPermission(Manifest.permission.POST_NOTIFICATIONS) // Optional permission
                    startForegroundService(intent)
                } else {
                    //interceptionEnabledState.value = false
                    interceptionEnabled.value = false
                    stopService(intent)
                }
            } else {
                stopService(intent)
            }
        }

    }
}