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

package me.devcexx.call2alarm.preferences.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.devcexx.call2alarm.preferences.PreferenceEnum
import me.devcexx.call2alarm.preferences.PreferenceEnumVariants
import me.devcexx.call2alarm.ui.theme.DarkBlue
import me.devcexx.call2alarm.ui.theme.NeutralVariant30


@Composable
fun PreferenceSet(
    context: Context,
    enabled: Boolean = true,
    content: @Composable PreferenceSetContext.() -> Unit
) {
    content(PreferenceSetContext(context, enabled))
}

class PreferenceSetContext(private val context: Context, private val enabled: Boolean) {
    @Composable
    fun PreferenceSet(
        enabled: Boolean = true,
        content: @Composable PreferenceSetContext.() -> Unit
    ) {
        content(PreferenceSetContext(context, this.enabled && enabled))
    }

    @Composable
    fun PreferenceGroup(@StringRes titleId: Int, content: @Composable () -> Unit) {
        Text(
            text = context.getString(titleId),
            style = MaterialTheme.typography.titleMedium.copy(color = NeutralVariant30),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        content()
    }

    @Composable
    fun PreferenceTitle(
        @StringRes titleId: Int,
        @StringRes subtitleId: Int? = null,
        subtitleColor: Color = NeutralVariant30,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Text(
                text = context.getString(titleId),
                style = MaterialTheme.typography.bodyLarge,
            )
            subtitleId?.let {
                Text(
                    text = context.getString(it),
                    style = MaterialTheme.typography.bodyMedium.copy(color = subtitleColor)
                )
            }
        }
    }

    @Composable
    fun PreferenceBox(
        onSurfaceClick: (() -> Unit)? = null,
        startDp: Dp = 16.dp,
        endDp: Dp = 24.dp,
        content: @Composable () -> Unit
    ) {
        Row(modifier = (if (onSurfaceClick != null && enabled) Modifier.clickable {
            onSurfaceClick()
        } else Modifier).fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .padding(start = startDp, top = 8.dp, bottom = 8.dp, end = endDp)
            ) {
                content()
            }
        }
    }

    @Composable
    fun PreferenceContainer(
        preferenceTitle: @Composable () -> Unit,
        preferenceControl: @Composable () -> Unit,
        onSurfaceClick: (() -> Unit)? = null
    ) {
        PreferenceBox(onSurfaceClick = onSurfaceClick) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    preferenceTitle()
                }
                Spacer(modifier = Modifier.width(16.dp))
                preferenceControl()
            }
        }
    }

    @Composable
    fun SwitchPreference(
        @StringRes titleId: Int,
        @StringRes subtitleId: Int?,
        state: MutableState<Boolean>,
        onChange: ((Boolean) -> Unit)? = null
    ) {
        val (currentValue, setter) = state
        PreferenceContainer(preferenceTitle = {
            PreferenceTitle(titleId = titleId, subtitleId = subtitleId)
        }, preferenceControl = {
            Switch(
                checked = currentValue,
                enabled = enabled,
                onCheckedChange = {
                    setter(it)
                    onChange?.invoke(it)
                }
            )
        }, onSurfaceClick = {
            val new = !currentValue
            setter(new)
            onChange?.invoke(new)
        })
    }

    @Composable
    fun <T : PreferenceEnum> ComboPreference(
        @StringRes titleId: Int,
        state: MutableState<T>,
        options: PreferenceEnumVariants<T>
    ) {

        val (currentValue, setter) = state
        var expanded by remember { mutableStateOf(false) }
        PreferenceContainer(
            preferenceTitle = {
                PreferenceTitle(
                    titleId = titleId,
                    subtitleId = currentValue.nameResId,
                    subtitleColor = if (enabled) DarkBlue else NeutralVariant30
                )
            }, preferenceControl = {
                Box(contentAlignment = Alignment.BottomEnd) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        options.all.forEach {
                            DropdownMenuItem(
                                text = { Text(text = context.getString(it.nameResId)) },
                                onClick = {
                                    expanded = false
                                    setter(it)
                                })
                        }
                    }
                }
            }, onSurfaceClick = if (enabled) {
                {
                    expanded = true
                }
            } else null)
    }


    @Composable
    fun SliderPreference(@StringRes titleId: Int, state: MutableState<Float>) {
        val (currentValue, setter) = state

        PreferenceBox(startDp = 0.dp, endDp = 0.dp) {
            PreferenceTitle(titleId = titleId, modifier = Modifier.padding(start = 16.dp))
            Slider(
                value = currentValue,
                onValueChange = { setter(it) },
                enabled = enabled,
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp, end = 24.dp)
                    .fillMaxWidth()
            )
        }
    }
}