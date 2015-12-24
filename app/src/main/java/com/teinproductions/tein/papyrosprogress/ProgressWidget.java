package com.teinproductions.tein.papyrosprogress;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ProgressWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAppWidgets(context, appWidgetIds, 0, true);
    }

    private static RemoteViews updateAppWidget(Context context, int appWidgetId, int progress) {
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

    /**
     * Set new progress to app widgets, and load new progress from the web when {@code loadFromWeb} is set to true.
     *
     * @param context      Context
     * @param appWidgetIds App widget ids for this app widget
     * @param progress     Progress to set to progress bars. This parameter is only used when {@code loadFromWeb} is set to false
     * @param loadFromWeb  Specifies whether to try to retrieve new progress from the web, or to use the specified {@code progress}
     *                     parameter.
     */
    static void updateAppWidgets(final Context context, final int[] appWidgetIds, int progress, boolean loadFromWeb) {
        if (loadFromWeb) {
            // Already update widget to show other changes (i.e. text size) before progress data is loaded from web
            onWebPageLoaded(context, appWidgetIds, null);

            // Check for internet connection
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                // If connected to internet, load progress data from internet
                new LoadWebPageTask(new LoadWebPageTask.OnLoadedListener() {
                    @Override
                    public void onLoaded(String json) {
                        onWebPageLoaded(context, appWidgetIds, json);
                    }
                }).execute();
            }
        } else {
            updateAppWidgetProgress(context, appWidgetIds, progress);
        }
    }

    private static void onWebPageLoaded(Context context, int[] appWidgetIds, String json) {
        if (appWidgetIds == null) return;

        if ("404".equals(json) || "403".equals(json)) {
            return;
        }

        int progress = 0;
        if (json == null) {
            json = MainActivity.getCache(context);
        }
        if (json != null) {
            try {
                JSONArray jArray = new JSONArray(json);
                JSONObject jObject = jArray.getJSONObject(0);

                int openIssues = jObject.getInt(MainActivity.OPEN_ISSUES);
                int closedIssues = jObject.getInt(MainActivity.CLOSED_ISSUES);

                progress = closedIssues * 100 / (openIssues + closedIssues);

                // Nothing went wrong, so the web page contents are correct and can be cached
                MainActivity.saveCache(context, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        updateAppWidgetProgress(context, appWidgetIds, progress);
    }

    private static void updateAppWidgetProgress(Context context, int[] appWidgetIds, int progress) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = updateAppWidget(context, appWidgetId, progress);
            if (views != null) {
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            }
        }
    }
}