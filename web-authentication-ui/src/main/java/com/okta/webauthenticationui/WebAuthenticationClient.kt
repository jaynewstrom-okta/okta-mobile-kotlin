/*
 * Copyright 2021-Present Okta, Inc.
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
package com.okta.webauthenticationui

import android.content.Context
import android.net.Uri
import android.app.Activity
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.oauth2.AuthorizationCodeFlow
import com.okta.oauth2.AuthorizationCodeFlow.Companion.authorizationCodeFlow
import com.okta.oauth2.RedirectEndSessionFlow
import com.okta.oauth2.RedirectEndSessionFlow.Companion.redirectEndSessionFlow

/**
 * Authentication coordinator that simplifies signing users in using browser-based OIDC authentication flows.
 *
 * This simple class encapsulates the details of launching the browser, and coordinating with OAuth2 endpoints.
 *
 * To customize the authentication flow, please read more about the underlying flows: [AuthorizationCodeFlow] and
 * [RedirectEndSessionFlow].
 */
class WebAuthenticationClient private constructor(
    private val oidcClient: OidcClient,
    private val webAuthenticationProvider: WebAuthenticationProvider,
) {
    companion object {
        /**
         * Initializes a web authentication client using the [OidcClient].
         *
         * @receiver the [OidcClient] used to perform the low level OIDC requests, as well as with which to use the configuration from.
         * @param webAuthenticationProvider the [WebAuthenticationProvider] which will be used to show the UI when performing the
         * redirect flows.
         */
        fun OidcClient.webAuthenticationClient(
            webAuthenticationProvider: WebAuthenticationProvider = DefaultWebAuthenticationProvider(configuration.eventCoordinator)
        ): WebAuthenticationClient {
            return WebAuthenticationClient(this, webAuthenticationProvider)
        }
    }

    /**
     * A model representing all possible states of a [WebAuthenticationClient.login] call.
     */
    sealed class LoginResult {
        /**
         * A successful result, indicating the flow should continue via the [AuthorizationCodeFlow.Context].
         */
        class Success internal constructor(
            /**
             * The [AuthorizationCodeFlow.Context] associated with a successful launch of the login flow.
             */
            val flowContext: AuthorizationCodeFlow.Context,
        ) : LoginResult()

        /**
         * An error resulting from [WebAuthenticationProvider.launch].
         */
        class Error internal constructor(
            /**
             * The exception, if available which caused the error.
             */
            val exception: Exception? = null,
        ) : LoginResult()
    }

    /**
     * A model representing all possible states of a [WebAuthenticationClient.logout] call.
     */
    sealed class LogoutResult {
        /**
         * A successful result, indicating the flow should continue via the [RedirectEndSessionFlow.Context].
         */
        class Success internal constructor(
            /**
             * The [RedirectEndSessionFlow.Context] associated with a successful launch of the Logout flow.
             */
            val flowContext: RedirectEndSessionFlow.Context,
        ) : LogoutResult()

        /**
         * An error resulting from [WebAuthenticationProvider.launch].
         */
        class Error internal constructor(
            /**
             * The exception, if available which caused the error.
             */
            val exception: Exception? = null,
        ) : LogoutResult()
    }

    private val authorizationCodeFlow: AuthorizationCodeFlow = oidcClient.authorizationCodeFlow()
    private val redirectEndSessionFlow: RedirectEndSessionFlow = oidcClient.redirectEndSessionFlow()

    /**
     * Initiates the OIDC Authorization Code redirect flow.
     *
     * See [WebAuthenticationClient.resume] for completing the flow.
     *
     * @param context the Android [Activity] [Context] which is used to display the login flow via the configured
     * [WebAuthenticationProvider].
     * @param scopes the scopes to request during sign in. Defaults to the configured [OidcClient] [OidcConfiguration.defaultScopes].
     */
    fun login(
        context: Context,
        scopes: Set<String> = oidcClient.configuration.defaultScopes,
    ): LoginResult {
        val flowContext = authorizationCodeFlow.start(scopes)
        when (val exception = webAuthenticationProvider.launch(context, flowContext.url)) {
            null -> {
                return LoginResult.Success(flowContext)
            }
            else -> {
                return LoginResult.Error(exception)
            }
        }
    }

    /**
     * Resumes the OIDC Authorization Code redirect flow.
     *
     * @param uri the redirect [Uri] which includes the authorization code to complete the flow.
     * @param flowContext the [AuthorizationCodeFlow.Context] used internally to maintain state.
     */
    suspend fun resume(
        uri: Uri,
        flowContext: AuthorizationCodeFlow.Context,
    ): AuthorizationCodeFlow.Result {
        return authorizationCodeFlow.resume(uri, flowContext)
    }

    /**
     * Initiates the OIDC logout redirect flow.
     *
     * See [WebAuthenticationClient.resume] for completing the flow.
     *
     * @param context the Android [Activity] [Context] which is used to display the logout flow via the configured
     * [WebAuthenticationProvider].
     * @param idToken the token used to identify the session to log the user out of.
     */
    fun logout(context: Context, idToken: String): LogoutResult {
        val flowContext = redirectEndSessionFlow.start(idToken)
        when (val exception = webAuthenticationProvider.launch(context, flowContext.url)) {
            null -> {
                return LogoutResult.Success(flowContext)
            }
            else -> {
                return LogoutResult.Error(exception)
            }
        }
    }

    /**
     * Resumes the OIDC logout redirect flow.
     *
     * @param uri the redirect [Uri] to complete the flow.
     * @param flowContext the [RedirectEndSessionFlow.Context] used internally to maintain state.
     */
    fun resume(uri: Uri, flowContext: RedirectEndSessionFlow.Context): RedirectEndSessionFlow.Result {
        return redirectEndSessionFlow.resume(uri, flowContext)
    }
}
