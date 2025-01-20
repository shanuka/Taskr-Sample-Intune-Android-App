/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.intune.samples.taskr

import android.content.Context
import android.util.Log
import com.microsoft.intune.mam.client.app.MAMApplication
import com.microsoft.intune.mam.client.app.MAMComponents
import com.microsoft.intune.mam.client.identity.MAMPolicyManager
import com.microsoft.intune.mam.client.notification.MAMNotificationReceiverRegistry
import com.microsoft.intune.mam.policy.MAMEnrollmentManager
import com.microsoft.intune.mam.policy.notification.MAMEnrollmentNotification
import com.microsoft.intune.mam.policy.notification.MAMNotification
import com.microsoft.intune.mam.policy.notification.MAMNotificationType
import com.microsoft.intune.samples.taskr.authentication.AuthenticationCallback
import org.json.JSONObject

//import com.microsoft.intune.samples.taskr.room.RoomManager;
/**
 * Specifies what happens when the app is launched and terminated.
 *
 * Registers an authentication callback for MAM.
 */
class TaskrApplication : MAMApplication() {
    override fun onMAMCreate() {
        super.onMAMCreate()

        // Initialize the tasks database
//        RoomManager.initRoom(getApplicationContext());

        // Registers a MAMAuthenticationCallback, which will try to acquire access tokens for MAM.
        // This is necessary for proper MAM integration.
        val mgr = MAMComponents.get(
            MAMEnrollmentManager::class.java
        )
        mgr!!.registerAuthenticationCallback(
            AuthenticationCallback(
                applicationContext
            )
        )

        /* This section shows how to register a MAMNotificationReceiver, so you can perform custom
         * actions based on MAM enrollment notifications.
         * More information is available here:
         * https://docs.microsoft.com/en-us/intune/app-sdk-android#types-of-notifications */
        MAMComponents.get(MAMNotificationReceiverRegistry::class.java)!!
            .registerReceiver(
                { notification: MAMNotification? ->
                    if (notification is MAMEnrollmentNotification) {
                        val result =
                            notification.enrollmentResult
                        when (result) {
                            MAMEnrollmentManager.Result.AUTHORIZATION_NEEDED, MAMEnrollmentManager.Result.NOT_LICENSED, MAMEnrollmentManager.Result.ENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.ENROLLMENT_FAILED, MAMEnrollmentManager.Result.WRONG_USER, MAMEnrollmentManager.Result.UNENROLLMENT_SUCCEEDED, MAMEnrollmentManager.Result.UNENROLLMENT_FAILED, MAMEnrollmentManager.Result.PENDING, MAMEnrollmentManager.Result.COMPANY_PORTAL_REQUIRED -> Log.d(
                                "Enrollment Receiver",
                                result.name
                            )

                            else -> Log.d("Enrollment Receiver", result.name)
                        }
                    } else {
                        Log.d("Enrollment Receiver", "Unexpected notification type received")
                    }
                    true
                }, MAMNotificationType.MAM_ENROLLMENT_RESULT
            )


        //   MAMAppConfigManager configManager = MAMComponents.get(MAMAppConfigManager.class);
// Retrieve the configuration value
        // Retrieve the app configuration for the current user
        // Get the app configuration for the current user
//        MAMAppConfig appConfig = configManager.getAppConfig(null); // Pass null for default user
//
//        // Define the configuration key
//        String key = "your_app_config_key"; // Replace with actual key
//
//        // Retrieve the value for the key
//        String configValue = appConfig.getStringForKey("serverURLManagedRestriction", MAMAppConfig.StringQueryType.Any);
//        Log.d("MAM Config", "Value for "+configValue.toString());
//
//        // Log the configuration value
//        if (configValue != null) {
//            Log.d("MAM Config", "Value for " + key + ": " + configValue);
//        } else {
//            Log.d("MAM Config", "No value found for key: " + key);
//        }

//        MAMAppConfigManager configManager = MAMComponents.get(MAMAppConfigManager.class);
//        String oid = "<OID of user account>";
//        MAMAppConfig appConfig = configManager.getAppConfig(null);
//        String fooValue = appConfig.getStringForKey("serverURLManagedRestriction", MAMAppConfig.StringQueryType.Any);
//        Log.d("MAM Config", "No value found for key: " + fooValue);

//        MAMAppConfigManager configManager = MAMComponents.get(MAMAppConfigManager.class);
//        String userId = mUserAccount.getAADID();
//        MAMAppConfig appConfig = configManager.getAppConfig(userId);
//        String configValue = appConfig.getStringForKey("serverURLManagedRestriction", MAMAppConfig.StringQueryType.Any);
//
//        Log.d("MAM Config", "No value found for key: " + configValue);
        readMAMConfig(applicationContext)

        //        // Register the notification handler
//        MAMNotificationReceiver receiver = new MAMNotificationReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                MAMNotificationType MAMNotification = null;
//                if (MAMNotification.REFRESH_APP_CONFIG.equals(intent.getAction())) {
//                    // Handle the configuration refresh
//                }
//            }
//        };
        initializeMAM()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Close the database connection to prevent memory leaks
//        RoomManager.closeRoom();
    }

    private var mamEnrollmentManager: MAMEnrollmentManager? = null

    private fun initializeMAM() {
        // Get the enrollment manager
        mamEnrollmentManager = MAMComponents.get(MAMEnrollmentManager::class.java)

        // Set authentication callback
        mamEnrollmentManager!!.registerAuthenticationCallback { upn, aadId, resourceId -> // Implement your authentication logic here
            // This should integrate with your organization's authentication flow
            Log.d("registerAuthenticationCallback", aadId)
            null
        }
    }

    fun readMAMConfig(context: Context?) {
        val configuration = JSONObject()

        try {
            // Get the MAM policy manager instance
            val policyManager = MAMComponents.get(
                MAMPolicyManager::class.java
            )

            // Check if app is managed
            if (policyManager != null) {
                val currentPolicy = MAMPolicyManager.getPolicy(context!!)

                if (currentPolicy != null) {
                    // Get the app configuration
                    //  String appConfig = currentPolicy.getAppConfig();

//                    if (appConfig != null && !appConfig.isEmpty()) {
//                        configuration = new JSONObject(appConfig);
//                    }

                    // Get policy settings specific to SDK 11.0

                    configuration.put("isManaged", true)

                    // configuration.put("isPolicyApplied", currentPolicy.isActive());
                    // configuration.put("isSaveToLocationAllowed", currentPolicy.getIsSaveToPersonalAllowed(context));
                    // configuration.put("isOpenFromLocationAllowed", currentPolicy.isOpenFromLocationAllowed(context));
                    //configuration.put("allowedSharingLevel", currentPolicy.getSharingPolicy());

                    // Add Identity info if available
//                    String upn = currentPolicy.getu();
//                    if (upn != null) {
//                        configuration.put("userPrincipalName", upn);
//                    }
                }
            } else {
                configuration.put("isManaged", false)
                configuration.put("message", "App is not managed by Intune")
            }
        } catch (e: Exception) {
            try {
                configuration.put("error", e.message)
                configuration.put("isManaged", false)
                Log.e("IntuneMAM", "Error reading MAM config: " + e.message)
            } catch (jsonError: Exception) {
                Log.e("IntuneMAM", "Error creating error JSON: " + jsonError.message)
            }
        }
    }
}
