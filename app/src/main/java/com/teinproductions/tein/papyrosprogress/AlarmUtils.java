package com.teinproductions.tein.papyrosprogress;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AlarmUtils {

    public static void reconsiderSettingAlarm(Context context) {
        // Check if notifications are enabled:
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
        boolean notifications = pref.getBoolean(MainActivity.NOTIFICATION_PREFERENCE, true);

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
