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
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devcexx.call2alarm.preferences.RingtoneVolumeSetting
import me.devcexx.call2alarm.preferences.ui.PreferenceSet
import me.devcexx.call2alarm.ui.theme.Call2AlarmTheme

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val interceptionEnabled by lazy { (app.preferences::interceptionEnabled).asState }
    private val interceptionOnlyInZenMode by lazy { (app.preferences::interceptionOnlyInZenMode).asState }
    private val startOnBoot by lazy { (app.preferences::startOnBoot).asState }
    private val ringtoneEnableVibration by lazy { (app.preferences::ringtoneEnableVibration).asState }
    private val ringtoneHonorSilentModes by lazy { (app.preferences::ringtoneHonorSilentModes).asState }
    private val ringtoneVolumeSetting by lazy { (app.preferences::ringtoneVolumeSetting).asState }
    private val ringtoneCustomVolume by lazy { (app.preferences::ringtoneCustomVolume).asState }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Call2AlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val context = LocalContext.current

                    val permissionsRequester =
                        rememberMultiplePermissionsState(permissions = allPermissions.toList()) { permissions ->
                            val hasPermissions =
                                mandatoryPermissions.all { permissions[it] ?: false }
                            context.onPermissionsProvided(hasPermissions)
                        }

                    CheckForPermissions(permissionsRequester)
                    MainContent(permissionsRequester)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainContent(permissionsRequest: MultiplePermissionsState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .overscroll(ScrollableDefaults.overscrollEffect()),
            verticalArrangement = Arrangement.Top
        ) {

            Preferences(permissionsRequest)
        }
    }

    @Composable
    fun Preferences(permissionsRequest: MultiplePermissionsState) {
        val (interceptionIsEnabled, _) = interceptionEnabled
        val (ringtoneVolumeSettingValue, _) = ringtoneVolumeSetting
        PreferenceSet(context = applicationContext) {
            PreferenceGroup(titleId = R.string.pref_group_global) {
                val context = LocalContext.current
                SwitchPreference(
                    titleId = R.string.pref_enable_call_handling_title,
                    subtitleId = R.string.pref_enable_call_handling_subtitle,
                    state = interceptionEnabled,
                    onChange = {
                        val hasPermissions = permissionsRequest.checkMandatoryPermissions()
                        context.onPermissionsProvided(hasPermissions && it)
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


    @Composable
    private fun CheckForPermissions(permissionsRequester: MultiplePermissionsState) {
        val context = LocalContext.current


        val mandatoryPermissionsGranted by remember {
            derivedStateOf {
                permissionsRequester.checkMandatoryPermissions()
            }
        }

        LaunchedEffect(mandatoryPermissionsGranted, context, interceptionEnabled.value, block = {
            if (!mandatoryPermissionsGranted) {
                permissionsRequester.launchMultiplePermissionRequest()
            }
            context.onPermissionsProvided(mandatoryPermissionsGranted && interceptionEnabled.value)
        })
    }

    private fun MultiplePermissionsState.checkMandatoryPermissions(): Boolean {
        val permissionMap =
            this.permissions.associateBy { it.permission }
        return mandatoryPermissions.all { permission ->
            permissionMap[permission]?.status?.isGranted ?: false
        }
    }

}


private val mandatoryPermissions =
    listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_NOTIFICATION_POLICY)

private val optionalPermissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

private val allPermissions = mandatoryPermissions + optionalPermissions


private fun Context.onPermissionsProvided(hasPermissions: Boolean) {
    val intent = Intent(this, CallInterceptorService::class.java)
    if (hasPermissions) {
        startForegroundService(intent)
    } else {
        stopService(intent)
    }
}