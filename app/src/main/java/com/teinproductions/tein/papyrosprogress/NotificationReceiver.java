package com.teinproductions.tein.papyrosprogress;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationReceiver extends BroadcastReceiver implements LoadWebPageTask.OnLoadedListener {

    public static final int NOTIFICATION_ID = 1;

    private int closedOld, closedNew, openOld, openNew;
    private String milestoneTitleOld, milestoneTitleNew;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            boolean notifications = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES,
                    Context.MODE_PRIVATE).getBoolean(MainActivity.NOTIFICATION_PREFERENCE, false);
            if (notifications) MainActivity.setOrCancelAlarm(context, true);
        }

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                // Parse cache
                String cache = MainActivity.getCache(context);
                parseOld(cache);

                // Parse web page
                new LoadWebPageTask(this).execute();
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLoaded(String result) {
        try {
            parseNew(result);

            int progressOld = closedOld * 100 / (openOld + closedOld);
            int progressNew = closedNew * 100 / (openNew + closedNew);

            if (milestoneTitleNew.equals(milestoneTitleOld) && progressOld != progressNew) {
                issueNotification(progressOld, progressNew);
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void issueNotification(int progressOld, int progressNew) {
        String title = context.getString(R.string.app_name);
        String message = String.format(context.getString(R.string.notific_msg_text_format),
                milestoneTitleNew, progressOld, progressNew);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.notification_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void parseOld(String json) throws JSONException {
        JSONObject jsonObject = new JSONArray(json).getJSONObject(0);
        milestoneTitleOld = jsonObject.getString(MainActivity.MILESTONE_TITLE);
        openOld = jsonObject.getInt(MainActivity.OPEN_ISSUES);
        closedOld = jsonObject.getInt(MainActivity.CLOSED_ISSUES);
    }

    private void parseNew(String json) throws JSONException {
        JSONObject jsonObject = new JSONArray(json).getJSONObject(0);
        milestoneTitleNew = jsonObject.getString(MainActivity.MILESTONE_TITLE);
        openNew = jsonObject.getInt(MainActivity.OPEN_ISSUES);
        closedNew = jsonObject.getInt(MainActivity.CLOSED_ISSUES);
    }
}
