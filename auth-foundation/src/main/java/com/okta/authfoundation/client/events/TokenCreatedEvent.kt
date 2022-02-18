/*
 * Copyright 2022-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.authfoundation.client.events

import com.okta.authfoundation.credential.Token

class TokenCreatedEvent internal constructor(
    val token: Token,
) {
    private val followUpTasks: MutableList<suspend () -> Unit> = mutableListOf()

    fun addFollowUpTask(task: suspend () -> Unit) {
        followUpTasks += task
    }

    internal suspend fun runFollowUpTasks() {
        for (followUpTask in followUpTasks) {
            followUpTask()
        }
    }
}