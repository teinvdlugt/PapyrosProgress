package com.teinproductions.tein.papyrosprogress;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
        Log.d("notification", "broadcast received");
        this.context = context;
        issueNotification(0, 0);
        /*try {
            // Parse cache
            String cache = MainActivity.getCache(context);
            parseOld(cache);

            // Parse web page
            new LoadWebPageTask(this).execute();
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onLoaded(String result) {
        try {
            parseNew(result);

            int progressOld = closedOld / (closedOld + openOld) * 100;
            int progressNew = closedNew / (closedNew + openNew) * 100;

            if (milestoneTitleNew.equals(milestoneTitleOld) && progressOld != progressNew) {
                issueNotification(progressOld, progressNew);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void issueNotification(int progressOld, int progressNew) {
        String title = "Title";
        String message = "Message";

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
