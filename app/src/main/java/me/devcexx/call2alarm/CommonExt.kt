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

import android.app.Service
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

val ComponentActivity.app: App get() = application as App
val Service.app: App get() = application as App
val <A> KMutableProperty0<A>.asState get(): MutableState<A> {
    val state = mutableStateOf(this.get())
    return object : MutableState<A> by state {
        override var value: A
            get() = get()
            set(value) {
                logi("Set value")
                set(value)
                state.value = value
            }

        override fun component2(): (A) -> Unit {
            val f = state.component2()
            return {
                set(it)
                f(it)
            }
        }
    }
}