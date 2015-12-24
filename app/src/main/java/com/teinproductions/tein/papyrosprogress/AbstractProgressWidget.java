package com.teinproductions.tein.papyrosprogress;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public abstract class AbstractProgressWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        // TODO AlarmUtils.setAlarm();
    }

    @Override
    public void onDisabled(Context context) {
        // TODO AlarmUtils.cancelAlarm();
    }

    static void updateAppWidgets(final Context context, int progress) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentNameLarge = ComponentName.createRelative(context, ProgressWidgetLarge.class.getName());
        ComponentName componentNameSmall = ComponentName.createRelative(context, ProgressWidgetSmall.class.getName());
        int[] appWidgetLargeIds = manager.getAppWidgetIds(componentNameLarge);
        int[] appWidgetSmallIds = manager.getAppWidgetIds(componentNameSmall);

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
}
