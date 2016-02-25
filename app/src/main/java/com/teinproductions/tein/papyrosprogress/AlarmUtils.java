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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AlarmUtils {

    public static void reconsiderSettingAlarm(Context context) {
        // Check if notifications are enabled:
        SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = defPref.getBoolean(Constants.NOTIFICATION_PREFERENCE, true);

        // Check if app widgets are present:
        boolean appWidgets = AbstractProgressWidget.areAppWidgetsEnabled(context);

        if (notifications || appWidgets) {
            // Set alarm
            setAlarm(context);
        } else {
            cancelAlarm(context);
        }
    }

    public static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pIntent = getPendingIntent(context);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pIntent);
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    private static PendingIntent getPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context, 1, new Intent(context, UpdateCheckReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
