package com.teinproductions.tein.papyrosprogress;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractProgressWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        AlarmUtils.setAlarm(context);
        // TODO Update app widgets
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateFromCache(context);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmUtils.reconsiderSettingAlarm(context);
    }

    public static void updateAppWidgets(final Context context, int progress) {
        AppWidgetManager awManager = AppWidgetManager.getInstance(context);
        int[] appWidgetLargeIds = getAppWidgetLargeIds(context, awManager);
        int[] appWidgetSmallIds = getAppWidgetSmallIds(context, awManager);

        for (int appWidgetId : appWidgetLargeIds) {
            RemoteViews views = ProgressWidgetLarge.getRemoteViews(context, appWidgetId, progress);
            if (views != null) {
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            }
        }
        for (int appWidgetSmallId : appWidgetSmallIds) {
            RemoteViews views = ProgressWidgetSmall.getRemoteViews(context, appWidgetSmallId, progress);
            if (views != null) {
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetSmallId, views);
            }
        }
    }

    private static int[] getAppWidgetLargeIds(Context context, AppWidgetManager awManager) {
        ComponentName componentName = new ComponentName(context, ProgressWidgetLarge.class);
        return awManager.getAppWidgetIds(componentName);
    }

    private static int[] getAppWidgetSmallIds(Context context, AppWidgetManager awManager) {
        ComponentName componentName = new ComponentName(context, ProgressWidgetSmall.class);
        return awManager.getAppWidgetIds(componentName);
    }

    public static boolean areAppWidgetsEnabled(Context context) {
        AppWidgetManager awManager = AppWidgetManager.getInstance(context);
        int[] appWidgetLargeIds = getAppWidgetLargeIds(context, awManager);
        int[] appWidgetSmallIds = getAppWidgetSmallIds(context, awManager);

        return !(appWidgetLargeIds.length == 0 && appWidgetSmallIds.length == 0);
    }

    /**
     * Update all app widgets without loading the progress from the Internet.
     * Used for example when the text size of a widget is updated, or when a widget
     * is first instantiated.
     */
    public static void updateFromCache(Context context) {
        int progress = getCachedProgress(context);
        updateAppWidgets(context, progress);
    }

    private static int getCachedProgress(Context context) {
        try {
            String cache = MainActivity.getCache(context);
            JSONObject jsonObject = new JSONArray(cache).getJSONObject(0);

            int open = jsonObject.getInt(MainActivity.OPEN_ISSUES);
            int closed = jsonObject.getInt(MainActivity.CLOSED_ISSUES);

            return closed * 100 / (open + closed);
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
