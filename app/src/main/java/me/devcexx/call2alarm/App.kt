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

import android.app.Application
import android.content.Context
import me.devcexx.call2alarm.preferences.AppPreferences

class App: Application() {
    val preferences: AppPreferences get() {
        // TODO This is working right now but is not the best way of storing preferences and sharing
        //  them across processes.
        return AppPreferences(getSharedPreferences("prefs", Context.MODE_MULTI_PROCESS))
    }
}