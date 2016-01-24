package com.teinproductions.tein.papyrosprogress;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        findPreference("rate_in_play_store").setOnPreferenceClickListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.NOTIFICATION_PREFERENCE.equals(key)) {
            if (sharedPreferences.getBoolean(key, true)) {
                // Schedule alarm
                AlarmUtils.setAlarm(getActivity());
            } else {
                // Cancel alarm
                AlarmUtils.reconsiderSettingAlarm(getActivity());
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if ("rate_in_play_store".equals(preference.getKey())) {
            try {
                MainActivity.sendEventHit(getActivity(), Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Rate in Play Store", null);
            } catch (Exception ignored) {/*ignored*/ }
        }
        return true;
    }
}
