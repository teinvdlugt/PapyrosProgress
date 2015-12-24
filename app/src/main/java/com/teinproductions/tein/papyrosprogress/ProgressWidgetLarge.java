package com.teinproductions.tein.papyrosprogress;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.widget.RemoteViews;


public class ProgressWidgetLarge extends AbstractProgressWidget {

    public static RemoteViews getRemoteViews(Context context, int appWidgetId, int progress) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.progress_widget);
        //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_progress_widget);

        // Set text size of text view
        if (Build.VERSION.SDK_INT >= 16) {
            int textSize = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .getInt(MainActivity.TEXT_SIZE_PREFERENCE + appWidgetId, 24);
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, textSize);
        }

        // Set on click listener
        Intent configIntent = new Intent(context, MainActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.putExtra(MainActivity.EXTRA_SMALL_WIDGET, false);
        configIntent.setData(Uri.parse("CONFIGURE_THE_WIDGET://widget/id" + appWidgetId)); // Some rubbish to make the intent unique
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
        views.setOnClickPendingIntent(R.id.root, pendingIntent);

        // Set progress
        CharSequence widgetText = progress + "%";
        views.setTextViewText(R.id.appwidget_text, widgetText);
        views.setProgressBar(R.id.progress_bar, 100, progress, false);

        /*PapyrosProgressBar progressBar = new PapyrosProgressBar(context);
        progressBar.measure(150, 150);
        progressBar.layout(0, 0, 150, 150);
        progressBar.setProgress(progress);
        progressBar.setDrawingCacheEnabled(true);
        views.setImageViewBitmap(R.id.imageView, progressBar.getDrawingCache());*/

        return views;
    }
}