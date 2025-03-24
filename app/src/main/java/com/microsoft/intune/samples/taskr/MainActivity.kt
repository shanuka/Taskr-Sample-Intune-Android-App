/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.intune.samples.taskr

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalIntuneAppProtectionPolicyRequiredException
import com.microsoft.identity.client.exception.MsalUserCancelException
import com.microsoft.intune.mam.client.app.MAMComponents
import com.microsoft.intune.mam.client.notification.MAMNotificationReceiver
import com.microsoft.intune.mam.client.notification.MAMNotificationReceiverRegistry
import com.microsoft.intune.mam.policy.MAMEnrollmentManager
import com.microsoft.intune.mam.policy.appconfig.MAMAppConfigManager
import com.microsoft.intune.mam.policy.notification.MAMEnrollmentNotification
import com.microsoft.intune.mam.policy.notification.MAMNotification
import com.microsoft.intune.mam.policy.notification.MAMNotificationType
import com.microsoft.intune.samples.taskr.authentication.AppAccount
import com.microsoft.intune.samples.taskr.authentication.AppSettings
import com.microsoft.intune.samples.taskr.authentication.MSALUtil
import com.microsoft.intune.samples.taskr.fragments.AboutFragment
import com.microsoft.intune.samples.taskr.fragments.SubmitFragment
import com.microsoft.intune.samples.taskr.fragments.TasksFragment
import com.microsoft.intune.samples.taskr.trustedroots.ui.TrustedRootsFragment
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * The main activity of the app - runs when the app starts.
 *
 * Handles authentication, explicitly interacting with MSAL and implicitly with MAM.
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mUserAccount: AppAccount? = null
    private var mEnrollmentManager: MAMEnrollmentManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mEnrollmentManager = MAMComponents.get(MAMEnrollmentManager::class.java)

        // Get the account info from the app settings.
        // If a user is not signed in, the account will be null.
        mUserAccount = AppSettings.getAccount(applicationContext)

        if (mUserAccount == null) {
            displaySignInView()

            //String currentUser = AppSettings.getAccount(this).getAADID();
            val configManager = MAMComponents.get(
                MAMAppConfigManager::class.java
            )
            val appConfig = configManager!!.getAppConfig("serverURL")
            val configText = findViewById<TextView>(R.id.about_nav_config_text)
            configText.text = if (appConfig == null) getString(R.string.err_unset)
            else getString(R.string.about_nav_config_text, appConfig.getAllStringsForKey("serverURL"))

            MAMComponents.get(MAMNotificationReceiverRegistry::class.java)!!
                .registerReceiver(
                    { notification: MAMNotification? ->
                        if (notification is MAMEnrollmentNotification) {
                            val result =
                                notification.enrollmentResult
                            when (result) {
                                MAMEnrollmentManager.Result.AUTHORIZATION_NEEDED, MAMEnrollmentManager.Result.NOT_LICENSED, MAMEnrollmentManager.Result.ENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.ENROLLMENT_FAILED, MAMEnrollmentManager.Result.WRONG_USER, MAMEnrollmentManager.Result.UNENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.UNENROLLMENT_FAILED, MAMEnrollmentManager.Result.PENDING, MAMEnrollmentManager.Result.COMPANY_PORTAL_REQUIRED -> {
                                    val notifi1 = findViewById<TextView>(R.id.notifi1)
                                    notifi1.text = "REFRESH_APP_CONFIG" + result.name
                                    Log.d("Enrollment Receiver", result.name)
                                }

                                else -> {
                                    val notifi1 = findViewById<TextView>(R.id.notifi1)
                                    notifi1.text = "REFRESH_APP_CONFIG" + result.name
                                    Log.d("Enrollment Receiver", result.name)
                                }
                            }
                        } else {
                            Log.d("Enrollment Receiver", "Unexpected notification type received")
                        }
                        true
                    }, MAMNotificationType.REFRESH_APP_CONFIG
                )

            MAMComponents.get(MAMNotificationReceiverRegistry::class.java)!!
                .registerReceiver(
                    { notification: MAMNotification? ->
                        if (notification is MAMEnrollmentNotification) {
                            val result =
                                notification.enrollmentResult
                            when (result) {
                                MAMEnrollmentManager.Result.AUTHORIZATION_NEEDED, MAMEnrollmentManager.Result.NOT_LICENSED, MAMEnrollmentManager.Result.ENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.ENROLLMENT_FAILED, MAMEnrollmentManager.Result.WRONG_USER, MAMEnrollmentManager.Result.UNENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.UNENROLLMENT_FAILED, MAMEnrollmentManager.Result.PENDING, MAMEnrollmentManager.Result.COMPANY_PORTAL_REQUIRED -> {
                                    Log.d("Enrollment Receiver", result.name)
                                    val notifi1 = findViewById<TextView>(R.id.notifi2)
                                    notifi1.text = "REFRESH_POLICY" + result.name
                                }

                                else -> {
                                    Log.d("Enrollment Receiver", result.name)
                                    val notifi1 = findViewById<TextView>(R.id.notifi2)
                                    notifi1.text = "REFRESH_POLICY" + result.name
                                }
                            }
                        } else {
                            Log.d("Enrollment Receiver", "Unexpected notification type received")
                        }
                        true
                    }, MAMNotificationType.REFRESH_POLICY
                )
        } else {
            displayMainView()
        }

        try {
            if (Build.VERSION.SDK_INT >= 28) {
                @SuppressLint("WrongConstant") val packageInfo = this.packageManager.getPackageInfo(
                    this.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )

                val signatures = packageInfo.signingInfo.apkContentsSigners
                val md = MessageDigest.getInstance("SHA")
                if (signatures != null) {
                    for (signature in signatures) {
                        md.update(signature.toByteArray())
                        val signatureBase64 = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                        Log.d("Signature Base64 Sha", signatureBase64)
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    private fun displaySignInView() {
        setContentView(R.layout.sign_in)
    }

    private fun displayMainView() {
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        changeNavigationView(R.id.nav_submit)
        Toast.makeText(this, R.string.auth_success, Toast.LENGTH_SHORT).show()
    }


    inner class AppConfigNotificationReceiver : MAMNotificationReceiver {
        override fun onReceive(notification: MAMNotification): Boolean {
            if (notification.type == MAMNotificationType.REFRESH_APP_CONFIG) {
                // Handle app configuration change
                println("App configuration has changed.")
                // Retrieve updated configuration or perform necessary actions
            }
            return false
        }
    }

    fun onClickSignIn(view: View?) {
        // initiate the MSAL authentication on a background thread
        val thread = Thread {
            LOGGER.info("Starting interactive auth")
            try {
                var loginHint: String? = null
                if (mUserAccount != null) {
                    loginHint = mUserAccount!!.upn
                }
                MSALUtil.acquireToken(this@MainActivity, MSAL_SCOPES, loginHint, AuthCallback())
            } catch (e: MsalException) {
                LOGGER.log(Level.SEVERE, getString(R.string.err_auth), e)
                showMessage("Authentication exception occurred - check logcat for more details.")
            } catch (e: InterruptedException) {
                LOGGER.log(Level.SEVERE, getString(R.string.err_auth), e)
                showMessage("Authentication exception occurred - check logcat for more details.")
            }
        }
        thread.start()
    }

    private fun signOutUser() {
        // Initiate an MSAL sign out on a background thread.
        val effectiveAccount = mUserAccount

        val thread = Thread {
            try {
                MSALUtil.signOutAccount(this, effectiveAccount!!.aadid)
            } catch (e: MsalException) {
                LOGGER.log(Level.SEVERE, "Failed to sign out user " + effectiveAccount!!.aadid, e)
            } catch (e: InterruptedException) {
                LOGGER.log(Level.SEVERE, "Failed to sign out user " + effectiveAccount!!.aadid, e)
            }
            mEnrollmentManager!!.unregisterAccountForMAM(
                effectiveAccount!!.upn,
                effectiveAccount.aadid
            )
            AppSettings.clearAccount(applicationContext)
            mUserAccount = null
            runOnUiThread { this.displaySignInView() }
        }
        thread.start()
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return changeNavigationView(item.itemId)
    }

    /**
     * Changes the sidebar view of the app. Used when a user clicks on a menu item or to
     * manually change the view
     *
     * @param id the id of the fragment that should be displayed
     */
    private fun changeNavigationView(id: Int): Boolean {
        var frag: Fragment? = null

        when (id) {
            R.id.nav_tasks -> frag = TasksFragment()
            R.id.nav_about -> frag = AboutFragment()
            R.id.nav_trusted_roots -> frag = TrustedRootsFragment()
            R.id.nav_sign_out -> signOutUser()
            R.id.nav_submit -> frag = SubmitFragment()
            else -> frag = SubmitFragment()
        }
        var didChangeView = frag != null
        if (didChangeView) {
            try {
                // Display the fragment
                val fragManager = supportFragmentManager
                fragManager.beginTransaction().replace(R.id.flContent, frag!!).commit()
            } catch (e: NullPointerException) {
                e.printStackTrace()
                didChangeView = false
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        return didChangeView
    }

    private fun showMessage(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    private inner class AuthCallback : AuthenticationCallback {
        override fun onError(exc: MsalException) {
            LOGGER.log(Level.SEVERE, "authentication failed", exc)

            if (exc is MsalIntuneAppProtectionPolicyRequiredException) {
                val appException = exc

                // Note: An app that has enabled APP CA with Policy Assurance would need to pass these values to `remediateCompliance`.
                // For more information, see https://docs.microsoft.com/en-us/mem/intune/developer/app-sdk-android#app-ca-with-policy-assurance
                val upn = appException.accountUpn
                val aadid = appException.accountUserId
                val tenantId = appException.tenantId
                val authorityURL = appException.authorityUrl

                // The user cannot be considered "signed in" at this point, so don't save it to the settings.
                mUserAccount = AppAccount(upn, aadid, tenantId, authorityURL)

                val message = "Intune App Protection Policy required."
                showMessage(message)

                LOGGER.info("MsalIntuneAppProtectionPolicyRequiredException received.")
                LOGGER.info(
                    String.format(
                        "Data from broker: UPN: %s; AAD ID: %s; Tenant ID: %s; Authority: %s",
                        upn, aadid, tenantId, authorityURL
                    )
                )
            } else if (exc is MsalUserCancelException) {
                showMessage("User cancelled sign-in request")
            } else {
                showMessage("Exception occurred - check logcat")
            }
        }

        override fun onSuccess(result: IAuthenticationResult) {
            val account = result.account

            val upn = account.username
            val aadId = account.id
            val tenantId = account.tenantId
            val authorityURL = account.authority
            account.id
            val message = "Authentication succeeded for user $upn"
            LOGGER.info(message)

            // Save the user account in the settings, since the user is now "signed in".
            mUserAccount = AppAccount(upn, aadId, tenantId, authorityURL)
            AppSettings.saveAccount(applicationContext, mUserAccount!!)

            // Register the account for MAM.
            mEnrollmentManager!!.registerAccountForMAM(upn, aadId, tenantId, authorityURL)

            displayMainView()
        }

        override fun onCancel() {
            showMessage("User cancelled auth attempt")
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(
            MainActivity::class.java.name
        )

        val MSAL_SCOPES: Array<String> = arrayOf("https://graph.microsoft.com/User.Read")
    }
}
