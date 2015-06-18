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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ProgressWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAppWidgets(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        Toast.makeText(context, context.getString(R.string.click_on_the_widget_message), Toast.LENGTH_LONG).show();
    }

    static RemoteViews updateAppWidget(Context context,
                                       int appWidgetId, String json) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.progress_widget);

        // Set text size of text view
        if (Build.VERSION.SDK_INT >= 16) {
            int textSize = context.getSharedPreferences(ConfigurationActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .getInt(ConfigurationActivity.TEXT_SIZE_PREFERENCE + appWidgetId, 24);
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, textSize);
        }

        // Set on click listener
        Intent configIntent = new Intent(context, ConfigurationActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.setData(Uri.parse("CONFIGURE_THE_WIDGET://widget/id" + appWidgetId)); // Some rubbish to make the intent unique:
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
        views.setOnClickPendingIntent(R.id.root, pendingIntent);

        // Set progress
        int progress = 0;

        if (json == null) {
            progress = context.getSharedPreferences(ConfigurationActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .getInt(ConfigurationActivity.LAST_UPDATED_PROGRESS, 0);
        } else {
            try {
                JSONArray jArray = new JSONArray(json);
                JSONObject jObject = jArray.getJSONObject(0);

                int openIssues = jObject.getInt(MainActivity.OPEN_ISSUES);
                int closedIssues = jObject.getInt(MainActivity.CLOSED_ISSUES);

                progress = closedIssues * 100 / (openIssues + closedIssues);
                context.getSharedPreferences(ConfigurationActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putInt(ConfigurationActivity.LAST_UPDATED_PROGRESS, progress).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        CharSequence widgetText = progress + "%";

        views.setTextViewText(R.id.appwidget_text, widgetText);
        views.setProgressBar(R.id.progress_bar, 100, progress, false);

        return views;
    }

    static void updateAppWidgets(final Context context, final int[] appWidgetIds) {
        onLoaded(context, appWidgetIds, null);

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new LoadWebPageTask(new LoadWebPageTask.OnLoadedListener() {
                @Override
                public void onLoaded(String json) {
                    ProgressWidget.onLoaded(context, appWidgetIds, json);
                }
            }).execute();
        }
    }

    static void onLoaded(Context context, int[] appWidgetIds, String json) {
        if (appWidgetIds == null) return;

        if ("404".equals(json) || "403".equals(json)) {
            return;
        }

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = updateAppWidget(context, appWidgetId, json);
            if (views != null) {
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            }
        }
    }
}