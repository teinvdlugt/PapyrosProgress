package com.teinproductions.tein.papyrosprogress;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ProgressWidget extends AppWidgetProvider {


    static int[] tempAppWidgetIds;
    static Context tempContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAppWidgets(context, appWidgetIds);
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

                //title = jObject.getString(MILESTONE_TITLE);
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

    static void updateAppWidgets(Context context, int[] appWidgetIds) {
        tempContext = context;
        tempAppWidgetIds = appWidgetIds;

        new UpdateAppWidgetsTask().onPostExecute(null);

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new UpdateAppWidgetsTask().execute();
        }
    }

    static class UpdateAppWidgetsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                java.net.URL url = new URL(MainActivity.URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                int response = conn.getResponseCode();
                Log.d("CRAZY PASTA", "Response is " + response);
                InputStream is = conn.getInputStream();

                return read(is);
            } catch (IOException e) {
                return "Error in doInBackground";
            }
        }

        private String read(InputStream inputStream) throws IOException {
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            for (int appWidgetId : tempAppWidgetIds) {
                RemoteViews views = updateAppWidget(tempContext, appWidgetId, s);
                if (views != null) {
                    AppWidgetManager.getInstance(tempContext).updateAppWidget(appWidgetId, views);
                }
            }
        }
    }
}