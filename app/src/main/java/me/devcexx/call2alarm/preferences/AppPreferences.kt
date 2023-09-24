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

package me.devcexx.call2alarm.preferences

import android.content.SharedPreferences
import me.devcexx.call2alarm.logi
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AppPreferences(private val preferences: SharedPreferences) {
    class PreferenceStoreDelegate<V>(
        private val prefName: String,
        private val defaultValue: V,
        private val prefGetter: (SharedPreferences, String, V) -> V,
        private val prefSetter: (SharedPreferences.Editor, String, V) -> Unit
    ): ReadWriteProperty<AppPreferences, V> {

        override fun getValue(thisRef: AppPreferences, property: KProperty<*>): V {
            val value = prefGetter(thisRef.preferences, prefName, defaultValue)
            thisRef.logi("Read preference: $prefName = $value")
            return value
        }

        override fun setValue(thisRef: AppPreferences, property: KProperty<*>, value: V) =
            thisRef.preferences.edit().also {
                prefSetter(it, prefName, value)
                thisRef.logi("Write preference: $prefName = $value")
            }.apply()

        fun <B> map(fromInner: (V) -> B, intoInner: (B) -> V): PreferenceStoreDelegate<B> = PreferenceStoreDelegate(
            prefName,
            fromInner(defaultValue),
            { sharedPreferences, prefName, b: B ->
                fromInner(prefGetter(sharedPreferences, prefName, intoInner(b)))
            },
            { editor: SharedPreferences.Editor, s: String, b: B ->
                prefSetter(editor, s, intoInner(b))
            }
        )
    }

    private fun booleanPreference(
        prefName: String, defaultValue: Boolean
    ): PreferenceStoreDelegate<Boolean> =
        PreferenceStoreDelegate(
            prefName, defaultValue,
            SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean
        )

    private fun floatPreference(
        prefName: String, defaultValue: Float
    ): PreferenceStoreDelegate<Float> =
        PreferenceStoreDelegate(
            prefName, defaultValue,
            SharedPreferences::getFloat, SharedPreferences.Editor::putFloat
        )

    private fun <A: PreferenceEnum> enumPreference(
        prefName: String, options: PreferenceEnumVariants<A>, defaultValue: A
    ): PreferenceStoreDelegate<A> {
        return PreferenceStoreDelegate(
            prefName, defaultValue.propertyValue,
            SharedPreferences::getString, SharedPreferences.Editor::putString
        ).map({ stringValue ->
            options.all.find { it.propertyValue == stringValue } ?: defaultValue
        }, { enumValue ->
            enumValue.propertyValue
        })
    }

    var interceptionEnabled: Boolean by booleanPreference("interception_enabled", false)
    var interceptionOnlyInZenMode: Boolean by booleanPreference("only_dnd_mode", true)
    var startOnBoot: Boolean by booleanPreference("start_on_boot", true)
    var ringtoneEnableVibration: Boolean by booleanPreference("ringtone_vibration", true)
    var ringtoneHonorSilentModes: Boolean by booleanPreference("ringtone_honor_silent_modes", true)
    var ringtoneVolumeSetting: RingtoneVolumeSetting by enumPreference("ringtone_volume_setting", RingtoneVolumeSetting, RingtoneVolumeSetting.SYSTEM_ALARM_VOLUME)
    var ringtoneCustomVolume: Float by floatPreference("ringtone_custom_volume", 0.5f)
}