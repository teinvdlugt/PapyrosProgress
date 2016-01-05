package com.teinproductions.tein.papyrosprogress;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class UpdateCheckReceiver extends BroadcastReceiver implements LoadWebPageTask.OnLoadedListener {
    private static final int PROGRESS_NOTIFICATION_ID = 1;
    private static final int BLOG_NOTIFICATION_ID = 2;

    /**
     * Boolean Intent extra which specifies whether to also check
     * for blog updates. Default is true;
     */
    public static final String CHECK_BLOGS_EXTRA = "check_blogs";

    private Map<String, Integer> oldMilestones = new HashMap<>();
    private Map<String, Integer> newMilestones = new HashMap<>();
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TAG", "onReceive");

        this.context = context;
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFERENCES,
                Context.MODE_PRIVATE);

        // Check Internet connection
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) return;

        // Check if progress has to be checked
        boolean progressNotifications = pref.getBoolean(MainActivity.NOTIFICATION_PREFERENCE, true);
        boolean appWidgets = AbstractProgressWidget.areAppWidgetsEnabled(context);
        if (progressNotifications || appWidgets) checkProgress();

        // Check if blog has to be checked
        boolean extra = intent.getBooleanExtra(CHECK_BLOGS_EXTRA, true);
        boolean blogNotifications = pref.getBoolean(MainActivity.BLOG_NOTIFICATION_PREFERENCE, true);
        if (extra && blogNotifications) checkBlog();
    }

    // PROGRESS NOTIFICATIONS

    private void checkProgress() {
        try {
            // Parse the cache
            String cache = MainActivity.getFile(context, MainActivity.MILESTONES_CACHE_FILE);
            if (cache == null) {
                // We have nothing to compare the new progress to
                throw new NullPointerException("There was no saved progress cache");
            }

            parseOld(cache);

            // Parse web page
            new LoadWebPageTask(this).execute();
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaded(LoadWebPageTask.Response result) {
        try {
            parseNew(result.content);

            // Check if milestones have been added
            Set<String> addedMilestones = new HashSet<>();
            for (Iterator<String> it = newMilestones.keySet().iterator(); it.hasNext(); ) {
                String title = it.next();
                if (!oldMilestones.containsKey(title)) {
                    addedMilestones.add(title);
                    it.remove();
                }
            }

            // Check if milestones have been removed
            Set<String> removedMilestones = new HashSet<>();
            for (Iterator<String> it = oldMilestones.keySet().iterator(); it.hasNext(); ) {
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
                if (!oldMilestones.get(title).equals(newMilestones.get(title))) {
                    changedProgresses.put(title, new int[]{oldMilestones.get(title), newMilestones.get(title)});
                }
            }

            // Check if there are any changes
            if (addedMilestones.size() > 0 || removedMilestones.size() > 0 || changedProgresses.size() > 0) {
                // Save cache
                MainActivity.saveFile(context, result.content, MainActivity.MILESTONES_CACHE_FILE);

                boolean sendNotification = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                        .getBoolean(MainActivity.NOTIFICATION_PREFERENCE, false);
                if (sendNotification)
                    issueNotification(context, addedMilestones, removedMilestones, changedProgresses);
            }

            // I have experienced some problems with the app widgets, so update them
            // frequently, even when there is no change in progress
            AbstractProgressWidget.updateAppWidgets(context, newMilestones);
        } catch (JSONException | NullPointerException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void parseOld(String json) throws JSONException {
        oldMilestones.clear();
        JSONArray jArray = new JSONArray(json);
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject milestone = jArray.getJSONObject(i);
            oldMilestones.put(JSONUtils.getTitle(milestone), JSONUtils.getProgress(milestone));
        }
    }

    private void parseNew(String json) throws JSONException, ParseException {
        newMilestones.clear();
        JSONArray jArray = new JSONArray(json);
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject milestone = jArray.getJSONObject(i);
            newMilestones.put(JSONUtils.getTitle(milestone), JSONUtils.getProgress(milestone));
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
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
    }

    // BLOG NOTIFICATIONS

    private void checkBlog() {
        Log.d("TAG", "checkBlog() called");
        // The amount of blog posts that were found when last checked
        final SharedPreferences pref = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        final int cachePostAmount = pref.getInt(MainActivity.CACHED_BLOG_AMOUNT, -1);

        // Load the GitHub API page containing information about the amount of blog posts
        new LoadWebPageTask(MainActivity.PAPYROS_BLOG_API_URL, new LoadWebPageTask.OnLoadedListener() {
            @Override
            public void onLoaded(LoadWebPageTask.Response result) {
                if (result.content == null) return;

                try {
                    final int newPostAmount = new JSONArray(result.content).length();

                    // If there was a cachePostAmount present in storage, compare it to
                    // new amount
                    if (cachePostAmount != -1 && newPostAmount > cachePostAmount) {
                        // A blog post has been added
                        issueBlogNotification(UpdateCheckReceiver.this.context);
                    }

                    pref.edit().putInt(MainActivity.CACHED_BLOG_AMOUNT, newPostAmount).apply();
                } catch (JSONException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }).execute();
    }

    private static void issueBlogNotification(Context context) {
        String title = context.getString(R.string.notification_title);
        String message = context.getString(R.string.blog_notification_content);

        PendingIntent pendingIntent;
        try {
            pendingIntent = PendingIntent.getActivity(context, 0,
                    new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.PAPYROS_BLOG_URL)),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.notification_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(BLOG_NOTIFICATION_ID, builder.build());

        Log.d("TAG", "notification issued");
    }
}
