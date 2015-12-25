package com.teinproductions.tein.papyrosprogress;

import android.annotation.SuppressLint;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class UpdateCheckReceiver extends BroadcastReceiver implements LoadWebPageTask.OnLoadedListener {
    private static final int NOTIFICATION_ID = 1;

    private Map<String, Integer> oldMilestones = new HashMap<>();
    private Map<String, Integer> newMilestones = new HashMap<>();
    private String firstCreated; // Title of first created milestone
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        // Check Internet connection
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // Connected to the Internet!
            try {
                // Parse the cache
                String cache = MainActivity.getCache(context);
                if (cache == null) {
                    // We have nothing to compare the new progress to
                    return;
                }

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

            // Check if milestones have been added
            Set<String> addedMilestones = new HashSet<>();
            for (Iterator<String> it = newMilestones.keySet().iterator(); it.hasNext();) {
                String title = it.next();
                if (!oldMilestones.containsKey(title)) {
                    addedMilestones.add(title);
                    it.remove();
                }
            }

            // Check if milestones have been removed
            Set<String> removedMilestones = new HashSet<>();
            for (Iterator<String> it = oldMilestones.keySet().iterator(); it.hasNext();) {
                String title = it.next();
                if (!newMilestones.containsKey(title)) {
                    removedMilestones.add(title);
                    it.remove();
                }
            }

            // Compare progresses of milestones which are in both sets
            Map<String, int[]> changedProgresses = new HashMap<>();
            Set<String> titles = newMilestones.keySet();
            for (String title : titles) {
                if (!Objects.equals(oldMilestones.get(title), newMilestones.get(title))) {
                    changedProgresses.put(title, new int[]{oldMilestones.get(title), newMilestones.get(title)});
                }
            }

            // Check if there are any changes
            if (addedMilestones.size() > 0 || removedMilestones.size() > 0 || changedProgresses.size() > 0) {
                // Save cache
                MainActivity.saveCache(context, result);

                progressChanged(addedMilestones, removedMilestones, changedProgresses);
            }
        } catch (JSONException | NullPointerException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void parseOld(String json) throws JSONException {
        JSONArray jArray = new JSONArray(json);
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject milestone = jArray.getJSONObject(i);
            oldMilestones.put(getTitle(milestone), getProgress(milestone));
        }
    }

    private void parseNew(String json) throws JSONException, ParseException {
        // While doing this, also check for first created milestone (to display progress from in app widget)
        long firstCreatedDate = Long.MAX_VALUE;
        @SuppressLint("SimpleDateFormat")
        DateFormat dateFormat = new SimpleDateFormat(MileStoneViewHolder.JSON_DATE_FORMAT);

        JSONArray jArray = new JSONArray(json);
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject milestone = jArray.getJSONObject(i);
            String title = getTitle(milestone);
            newMilestones.put(title, getProgress(milestone));

            long created = dateFormat.parse(milestone.getString(MainActivity.CREATED_AT)).getTime();
            if (created < firstCreatedDate) {
                firstCreatedDate = created;
                firstCreated = title;
            }
        }
    }

    private static String getTitle(JSONObject milestone) throws JSONException {
        return milestone.getString(MainActivity.MILESTONE_TITLE);
    }

    private static int getProgress(JSONObject milestone) throws JSONException {
        int openIssues = milestone.getInt(MainActivity.OPEN_ISSUES);
        int closedIssues = milestone.getInt(MainActivity.CLOSED_ISSUES);
        return closedIssues * 100 / (openIssues + closedIssues);
    }

    private void progressChanged(Set<String> addedMilestones, Set<String> removedMilestones,
                                 Map<String, int[]> changedProgresses) {
        // Send notification
        boolean sendNotification = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.NOTIFICATION_PREFERENCE, false);
        if (sendNotification)
            issueNotification(context, addedMilestones, removedMilestones, changedProgresses);

        // Notify the app widgets
        Integer progress = newMilestones.get(firstCreated);
        if (progress != null) {
            AbstractProgressWidget.updateAppWidgets(context, progress);
        }
    }

    private static void issueNotification(Context context, Set<String> addedMilestones,
                                          Set<String> removedMilestones, Map<String, int[]> changedProgresses) {
        String title = context.getString(R.string.notification_title);
        StringBuilder message = new StringBuilder();

        // Changed progresses
        for (String milestoneTitle : changedProgresses.keySet()) {
            int oldProgress = changedProgresses.get(milestoneTitle)[0];
            int newProgress = changedProgresses.get(milestoneTitle)[1];
            message.append("\n").append(String.format(context.getString(R.string.notific_msg_text_format),
                    milestoneTitle, oldProgress, newProgress));
        }

        // Added milestones
        for (String milestoneTitle : addedMilestones) {
            message.append("\n").append(context.getString(R.string.milestone_added_notification, milestoneTitle));
        }

        // Removed milestones
        for (String milestoneTitle : removedMilestones) {
            message.append("\n").append(context.getString(R.string.milestone_removed_notification, milestoneTitle));
        }

        // Remove first newline
        message.delete(0, 1);

        // Create PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.mipmap.notification_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        // Issue the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
