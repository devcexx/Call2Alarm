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

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Deprecated("Use accompanist permissions instead")
class PermissionRequester(activity: ComponentActivity) {
    private var requestPermissionContinuation: Continuation<Boolean>? = null
    private val activityResultLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        requestPermissionContinuation?.resume(granted.values.fold(true) { l, r -> l && r })
    }


    suspend fun requestPermission(vararg permissions: String) = suspendCoroutine { cont ->
        val perms: Array<String> = permissions.map { it }.toTypedArray()
        requestPermissionContinuation = cont
        activityResultLauncher.launch(perms)
    }
}