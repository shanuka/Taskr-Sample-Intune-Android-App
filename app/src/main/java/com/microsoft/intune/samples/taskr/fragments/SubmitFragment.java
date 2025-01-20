/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.intune.samples.taskr.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.intune.mam.client.app.MAMComponents;
import com.microsoft.intune.mam.policy.appconfig.MAMAppConfig;
import com.microsoft.intune.mam.policy.appconfig.MAMAppConfigManager;
import com.microsoft.intune.samples.taskr.R;
import com.microsoft.intune.samples.taskr.authentication.AppAccount;
import com.microsoft.intune.samples.taskr.authentication.AppSettings;
import com.microsoft.intune.samples.taskr.room.Task;
import com.microsoft.intune.samples.taskr.room.RoomManager;

import java.util.List;
import java.util.Map;


/**
 * A {@link Fragment} subclass that handles the creation of a view of the submit screen.
 */
public class SubmitFragment extends Fragment {

    private String concat ="";

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_submit, container, false);
        Activity activity = getActivity();
        if (activity == null) {
            return rootView;
        }

        // This ensures that the keyboard doesn't automatically pop up when the app starts
        Window window = activity.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        // Watch the submit button and run submitListener if it's clicked
        rootView.findViewById(R.id.submit_nav_submit).setOnClickListener(submitListener);

        // Check if the app was sent an intent, if it's valid set the description field
        Intent intent = activity.getIntent();
        String type = intent.getType();
        if (type != null) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!type.equals("text/plain") || text == null) {
                Toast.makeText(activity, R.string.err_bad_intent, Toast.LENGTH_LONG).show();
            } else {
                EditText description = rootView.findViewById(R.id.submit_nav_description_text);
                description.setText(text);
            }
        }


        AppAccount mUserAccount = AppSettings.getAccount(getContext());

        MAMAppConfigManager configManager = MAMComponents.get(MAMAppConfigManager.class);
//        // Step 2: Get the user's UPN (User Principal Name) - This is required to access the user's app config
//        val userPrincipalName = enrollmentManager.primaryUser // Automatically retrieves the primary user
//
//        // Step 3: Retrieve application configuration for the current user
//        val appConfig = userPrincipalName?.let { enrollmentManager.getApplicationConfig(it) }
        String aadid = mUserAccount.getAADID();
        MAMAppConfig appConfig = configManager.getAppConfigForOID(aadid);
        // Step 4: Access specific configuration settings from the appConfig map
        String apiEndpoint = appConfig.getStringForKey("registerAuthenticationCallback", MAMAppConfig.StringQueryType.Any);
        String enableFeatureX = appConfig.getStringForKey("serverURLManagedRestriction", MAMAppConfig.StringQueryType.Any);
      //  Log.d("apiEndpoint", apiEndpoint);
        List<Map<String, String>> list = appConfig.getFullData();

        for (int i = 0; i < list.size(); i++) {
            System.out.println("Map " + (i + 1) + ":");
            Map<String, String> map = list.get(i);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                String key = entry.getKey();
                String value = entry.getValue();
                concat = "key "+ key+ " value "+ value;
            }
        }
        TextView description = rootView.findViewById(R.id.textView);
        description.setText("MAMAppConfig "+concat);

        return rootView;
    }

    private final View.OnClickListener submitListener = (final View view) -> {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // Hide the keyboard for convenience
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Get the text the user entered
        EditText descriptionField = activity.findViewById(R.id.submit_nav_description_text);
        String description = descriptionField.getText().toString();

        // Confirm the fields look valid
        int toastMessage;
        if (description.isEmpty()) {
            toastMessage = R.string.submit_nav_no_description;
        } else {
            // We know the user input is valid, submit it
//            RoomManager.insertTask(new Task(description));
            // Now clear the input field
            descriptionField.setText("");
            toastMessage = R.string.submit_nav_submitted;
        }
        Toast.makeText(view.getContext(), toastMessage, Toast.LENGTH_LONG).show();
    };
}
