/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.intune.samples.taskr.authentication

import android.app.Activity
import android.content.Context
import androidx.annotation.WorkerThread
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.microsoft.intune.samples.taskr.R
import java.util.Arrays
import java.util.logging.Logger

/**
 * A utility class for methods required by MSAL.
 */
object MSALUtil {
    private val LOGGER: Logger = Logger.getLogger(MSALUtil::class.java.name)

    private var mMsalClientApplication: IPublicClientApplication? = null

    /**
     * Acquire a token for the requested scopes.  Will be interactive.
     *
     * @param fromActivity
     * the Activity from which the auth request is made.
     * @param scopes
     * Scopes for the requested token.
     * @param loginHint
     * a prompt for the login dialog, can be null if unused.
     * @param callback
     * callback to receive the result of the auth attempt.
     *
     * @throws MsalException
     * MSAL error occurred.
     * @throws InterruptedException
     * Thread was interrupted.
     */
    @WorkerThread
    @Throws(MsalException::class, InterruptedException::class)
    fun acquireToken(
        fromActivity: Activity,
        scopes: Array<String>,
        loginHint: String?,
        callback: AuthenticationCallback
    ) {
        initializeMsalClientApplication(fromActivity.applicationContext)

        val params = AcquireTokenParameters.Builder()
            .withScopes(Arrays.asList(*scopes))
            .withCallback(callback)
            .startAuthorizationFromActivity(fromActivity)
            .withLoginHint(loginHint)
            .build()

        mMsalClientApplication!!.acquireToken(params)
    }

    /**
     * Acquire a token for the requested scopes.  Will not be interactive.
     *
     * @param appContext
     * A Context used to initialize the MSAL context, if needed.
     * @param aadId
     * Id of the user.
     * @param scopes
     * Scopes for the requested token.
     * @param callback
     * callback to receive the result of the auth attempt.
     *
     * @throws MsalException
     * MSAL error occurred.
     * @throws InterruptedException
     * Thread was interrupted.
     */
    @WorkerThread
    @Throws(MsalException::class, InterruptedException::class)
    fun acquireTokenSilent(
        appContext: Context,
        aadId: String,
        scopes: Array<String?>,
        callback: AuthenticationCallback
    ) {
        initializeMsalClientApplication(appContext.applicationContext)

        val account = getAccount(aadId)
        if (account == null) {
            LOGGER.severe("Failed to acquire token: no account found for $aadId")
            callback.onError(
                MsalUiRequiredException(
                    MsalUiRequiredException.NO_ACCOUNT_FOUND,
                    "no account found for $aadId"
                )
            )
            return
        }

        val params = AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(account.authority)
            .withScopes(Arrays.asList(*scopes))
            .withCallback(callback)
            .build()

        mMsalClientApplication!!.acquireTokenSilentAsync(params)
    }

    /**
     * Acquire a token silently for the given resource and user.  This is synchronous.
     *
     * @param appContext
     * the application context.
     * @param aadId
     * Id of the user.
     * @param scopes
     * Scopes to request the token for.
     *
     * @return the authentication result, or null if it fails.
     *
     * @throws MsalException
     * MSAL error occurred.
     * @throws InterruptedException
     * Thread was interrupted.
     */
    @WorkerThread
    @Throws(MsalException::class, InterruptedException::class)
    fun acquireTokenSilentSync(
        appContext: Context,
        aadId: String,
        scopes: Array<String>
    ): IAuthenticationResult {
        initializeMsalClientApplication(appContext)
        val account = getAccount(aadId)
        if (account == null) {
            LOGGER.severe("Failed to acquire token: no account found for $aadId")
            throw MsalUiRequiredException(
                MsalUiRequiredException.NO_ACCOUNT_FOUND,
                "no account found for $aadId"
            )
        }

        val params =
            AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .withScopes(Arrays.asList(*scopes))
                .build()

        return mMsalClientApplication!!.acquireTokenSilent(params)
    }

    /**
     * Sign out the given account from MSAL.
     *
     * @param appContext
     * the application context.
     * @param aadId
     * Id of the user.
     *
     * @throws MsalException
     * MSAL error occurred.
     * @throws InterruptedException
     * Thread was interrupted.
     */
    @Throws(MsalException::class, InterruptedException::class)
    fun signOutAccount(appContext: Context, aadId: String) {
        initializeMsalClientApplication(appContext)
        val account = getAccount(aadId)

        if (account == null) {
            LOGGER.warning("Failed to sign out account: No account found for $aadId")
            return
        }

        if (mMsalClientApplication is IMultipleAccountPublicClientApplication) {
            val multiAccountPCA =
                mMsalClientApplication as IMultipleAccountPublicClientApplication?

            multiAccountPCA!!.removeAccount(account)
        } else {
            val singleAccountPCA =
                mMsalClientApplication as ISingleAccountPublicClientApplication?

            singleAccountPCA!!.signOut()
        }
    }

    @Throws(InterruptedException::class, MsalException::class)
    private fun getAccount(aadId: String): IAccount? {
        var account: IAccount? = null

        if (mMsalClientApplication is IMultipleAccountPublicClientApplication) {
            val multiAccountPCA =
                mMsalClientApplication as IMultipleAccountPublicClientApplication?

            account = multiAccountPCA!!.getAccount(aadId)
        } else {
            val singleAccountPCA =
                mMsalClientApplication as ISingleAccountPublicClientApplication?

            val accountResult = singleAccountPCA!!.currentAccount
            if (accountResult != null) {
                account = accountResult.currentAccount
                // make sure this is the correct user
                if (account != null && account.id != aadId) account = null
            }
        }
        return account
    }

    @Synchronized
    @Throws(MsalException::class, InterruptedException::class)
    private fun initializeMsalClientApplication(appContext: Context) {
        if (mMsalClientApplication == null) {
            val msalLogger = com.microsoft.identity.client.Logger.getInstance()
            msalLogger.setEnableLogcatLog(true)
            msalLogger.setLogLevel(com.microsoft.identity.client.Logger.LogLevel.VERBOSE)
            msalLogger.setEnablePII(true)

            mMsalClientApplication = PublicClientApplication.create(appContext, R.raw.auth_config)
        }
    }
}
