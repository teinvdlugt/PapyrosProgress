package com.teinproductions.tein.papyrosprogress;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.RemoteViews;


public class ProgressWidgetSmall extends AbstractProgressWidget {

    public static RemoteViews getRemoteViews(Context context, int appWidgetId, int progress) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.progress_widget_small);

        // Set text size of text view
        if (Build.VERSION.SDK_INT >= 16) {
            int textSize = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(Constants.TEXT_SIZE_PREFERENCE + appWidgetId, 24);
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, textSize);
        }

        // Set on click listener
        Intent configIntent = new Intent(context, MainActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.putExtra(MainActivity.EXTRA_SMALL_WIDGET, true);
        configIntent.setData(Uri.parse("CONFIGURE_THE_WIDGET://widget/id" + appWidgetId)); // Some rubbish to make the intent unique
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
        views.setOnClickPendingIntent(R.id.root, pendingIntent);

        // Set progress
        views.setTextViewText(R.id.appwidget_text, "" + progress);

        return views;
    }
}