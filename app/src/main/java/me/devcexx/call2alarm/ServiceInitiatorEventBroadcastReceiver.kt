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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceInitiatorEventBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        logi("Received boot notification")

        val prefs = (context.applicationContext as App).preferences
        if (prefs.interceptionEnabled && prefs.startOnBoot) {
            context.startForegroundService(Intent(context, CallInterceptorService::class.java))
        } else {
            logi("Service not started. Interception enabled: ${prefs.interceptionEnabled}; " +
                    "start on boot: ${prefs.startOnBoot}")
        }
    }

}