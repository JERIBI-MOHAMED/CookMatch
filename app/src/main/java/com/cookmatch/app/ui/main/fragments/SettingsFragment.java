package com.cookmatch.app.ui.main.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cookmatch.app.databinding.FragmentSettingsBinding;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsFragment extends Fragment {

    private static final String PREF_NOTIFICATIONS = "notifications_enabled";
    private static final String PREF_OFFLINE = "offline_mode";

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences(
            "cookmatch_prefs", android.content.Context.MODE_PRIVATE);

        binding.notificationsSwitch.setChecked(prefs.getBoolean(PREF_NOTIFICATIONS, true));
        binding.offlineSwitch.setChecked(prefs.getBoolean(PREF_OFFLINE, false));

        binding.notificationsSwitch.setOnCheckedChangeListener((btn, checked) ->
        {
            prefs.edit().putBoolean(PREF_NOTIFICATIONS, checked).apply();
            if (checked) {
                FirebaseMessaging.getInstance().subscribeToTopic("community");
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("community");
            }
        });

        binding.offlineSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(PREF_OFFLINE, checked).apply());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
