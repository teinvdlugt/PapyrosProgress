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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

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
        this.context = context;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        // Check Internet connection
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) return;

        // Check if progress has to be checked
        boolean notifications = pref.getBoolean(Constants.NOTIFICATION_PREFERENCE, true);
        boolean appWidgets = AbstractProgressWidget.areAppWidgetsEnabled(context);
        if (notifications || appWidgets) checkProgress();

        // Check if blog has to be checked
        boolean extra = intent.getBooleanExtra(CHECK_BLOGS_EXTRA, true);
        if (extra && notifications) checkBlog();
    }

    // PROGRESS NOTIFICATIONS

    private void checkProgress() {
        try {
            // Parse the cache
            String cache = IOUtils.getFile(context, Constants.MILESTONES_CACHE_FILE);
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
                IOUtils.saveFile(context, result.content, Constants.MILESTONES_CACHE_FILE);

                boolean sendNotification = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(Constants.NOTIFICATION_PREFERENCE, false);
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

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sound = pref.getBoolean(Constants.NOTIFICATION_SOUND_PREF, true);
        boolean vibrate = pref.getBoolean(Constants.NOTIFICATION_VIBRATE_PREF, true);
        boolean light = pref.getBoolean(Constants.NOTIFICATION_LIGHT_PREF, true);
        int defaults = 0;
        if (sound) defaults = defaults | Notification.DEFAULT_SOUND;
        if (vibrate) defaults = defaults | Notification.DEFAULT_VIBRATE;
        if (light) defaults = defaults | Notification.DEFAULT_LIGHTS;

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.mipmap.notification_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setDefaults(defaults)
                .setAutoCancel(true);

        // Issue the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
    }

    // BLOG NOTIFICATIONS

    private void checkBlog() {
        // The amount of blog posts that were found when last checked
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final int cachePostAmount = pref.getInt(Constants.CACHED_BLOG_AMOUNT, -1);

        // Load the GitHub API page containing information about the amount of blog posts
        new LoadWebPageTask(Constants.PAPYROS_BLOG_API_URL, new LoadWebPageTask.OnLoadedListener() {
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

                    pref.edit().putInt(Constants.CACHED_BLOG_AMOUNT, newPostAmount).apply();
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
                    new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PAPYROS_BLOG_URL)),
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
    }
}
