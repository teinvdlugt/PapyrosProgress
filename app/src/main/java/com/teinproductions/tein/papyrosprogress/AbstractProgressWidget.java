package com.teinproductions.tein.papyrosprogress;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public abstract class AbstractProgressWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        AlarmUtils.reconsiderSettingAlarm(context);
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

    public static int[] getAppWidgetLargeIds(Context context, AppWidgetManager awManager) {
        ComponentName componentName = ComponentName.createRelative(context, ProgressWidgetLarge.class.getName());
        return awManager.getAppWidgetIds(componentName);
    }

    public static int[] getAppWidgetSmallIds(Context context, AppWidgetManager awManager) {
        ComponentName componentName = ComponentName.createRelative(context, ProgressWidgetSmall.class.getName());
        return awManager.getAppWidgetIds(componentName);
    }

    public static boolean areAppWidgetsEnabled(Context context) {
        AppWidgetManager awManager = AppWidgetManager.getInstance(context);
        int[] appWidgetLargeIds = getAppWidgetLargeIds(context, awManager);
        int[] appWidgetSmallIds = getAppWidgetSmallIds(context, awManager);

        return !(appWidgetLargeIds.length == 0 && appWidgetSmallIds.length == 0);
    }
}
