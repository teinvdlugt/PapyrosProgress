/*
 * Papyros Progress: An Android application showing the development progress of Papyros
 * Copyright (C) 2016  Tein van der Lugt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.teinproductions.tein.papyrosprogress;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.teinproductions.tein.papyrosprogress"));
            startActivity(intent);

            try {
                MainActivity.sendEventHit(getActivity(), Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Rate in Play Store", null);
            } catch (Exception ignored) {/*ignored*/ }
        }
        return true;
    }
}
