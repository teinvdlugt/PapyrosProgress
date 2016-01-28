package com.teinproductions.tein.papyrosprogress;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;

import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements LoadWebPageTask.OnLoadedListener, SwipeRefreshLayout.OnRefreshListener,
        PapyrosRecyclerAdapter.OnTextSizeButtonClickListener {
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 1;
    public static final String EXTRA_SMALL_WIDGET = "small_widget";

    private RecyclerView recyclerView;
    private PapyrosRecyclerAdapter adapter;
    private SwipeRefreshLayout srLayout;

    private int appWidgetId;
    private String errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ((GAApplication) getApplication()).startTracking();

        checkNotificationsAsked();

        sendBroadcast(new Intent(this, UpdateCheckReceiver.class));

        srLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        srLayout.setOnRefreshListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PapyrosRecyclerAdapter(this, new Milestone[0], this);
        recyclerView.setAdapter(adapter);

        restoreAppWidgetStuff();
        onRefresh();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restoreAppWidgetStuff();
    }

    private void checkNotificationsAsked() {
        final SharedPreferences oldSharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        final SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (oldSharedPref.contains(Constants.NOTIFICATION_ASKED_PREFERENCE)) {
            // The old preferences have to be copied over to the new default shared preferences
            SharedPreferences.Editor editor = defaultSharedPref.edit();
            editor.putBoolean(Constants.NOTIFICATION_PREFERENCE,
                    oldSharedPref.getBoolean(Constants.NOTIFICATION_PREFERENCE, true));
            editor.putInt(Constants.CACHED_BLOG_AMOUNT,
                    oldSharedPref.getInt(Constants.CACHED_BLOG_AMOUNT, 0));

            for (int id : AbstractProgressWidget.getAppWidgetLargeIds(this, AppWidgetManager.getInstance(this))) {
                editor.putInt(Constants.TEXT_SIZE_PREFERENCE + id,
                        oldSharedPref.getInt(Constants.TEXT_SIZE_PREFERENCE + id, 24));
                editor.putString(Constants.MILESTONE_WIDGET_PREFERENCE + id,
                        oldSharedPref.getString(Constants.MILESTONE_WIDGET_PREFERENCE + id, "Version 0.1"));
            }
            for (int id : AbstractProgressWidget.getAppWidgetSmallIds(this, AppWidgetManager.getInstance(this))) {
                editor.putInt(Constants.TEXT_SIZE_PREFERENCE + id,
                        oldSharedPref.getInt(Constants.TEXT_SIZE_PREFERENCE + id, 24));
                editor.putString(Constants.MILESTONE_WIDGET_PREFERENCE + id,
                        oldSharedPref.getString(Constants.MILESTONE_WIDGET_PREFERENCE + id, "Version 0.1"));
            }
            editor.apply();

            // Clear the old shared preferences
            oldSharedPref.edit().clear().apply();
        } else if (!defaultSharedPref.contains(Constants.NOTIFICATION_PREFERENCE)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.notification_dialog_title))
                    .setMessage(getString(R.string.notification_dialog_message))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            defaultSharedPref.edit().putBoolean(Constants.NOTIFICATION_PREFERENCE, true).apply();
                            AlarmUtils.setAlarm(MainActivity.this);
                            invalidateOptionsMenu();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            defaultSharedPref.edit().putBoolean(Constants.NOTIFICATION_PREFERENCE, false).apply();
                            AlarmUtils.reconsiderSettingAlarm(MainActivity.this);
                            invalidateOptionsMenu();
                        }
                    }).create().show();
        }
    }

    private void restoreAppWidgetStuff() {
        if (Build.VERSION.SDK_INT < 16) return;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            adapter.setTextSize(pref.getInt(Constants.TEXT_SIZE_PREFERENCE + appWidgetId, 24));
            adapter.setWidgetMilestoneTitle(pref.getString(Constants.MILESTONE_WIDGET_PREFERENCE + appWidgetId, null));
        } else {
            adapter.setTextSize(PapyrosRecyclerAdapter.DONT_SHOW_TEXT_SIZE_TILE);
        }
    }

    @Override
    public void onRefresh() {
        // First try with cache:
        String cache = getFile(this, Constants.MILESTONES_CACHE_FILE);
        if (cache != null) {
            try {
                adapter.setMilestones(JSONUtils.getMilestones(cache));
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }

        // Now try from the web:
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            srLayout.setRefreshing(true);
            new LoadWebPageTask(this).execute();
        } else {
            errorMessage = getString(R.string.offline_snackbar);
            showErrorMessage();
            srLayout.setRefreshing(false);
        }
    }

    @Override
    public void onLoaded(LoadWebPageTask.Response response) {
        srLayout.setRefreshing(false);
        if (response.content == null) {
            if (response.responseCode == 403) {
                errorMessage = getString(R.string.error403);
            } else {
                errorMessage = getString(R.string.error404, response.responseCode);
            }

            showErrorMessage();
            return;
        }

        try {
            adapter.setMilestones(JSONUtils.getMilestones(response.content));

            // Nothing went wrong, so the web page contents are correct and can be cached
            saveFile(this, response.content, Constants.MILESTONES_CACHE_FILE);
        } catch (JSONException | NullPointerException | ParseException e) {
            e.printStackTrace();
            // This means the retrieved web page was not the right one
            errorMessage = getString(R.string.error404);
        }
    }

    @Override
    public void onClickApply(int progress, String milestoneTitle) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        editor.putInt(Constants.TEXT_SIZE_PREFERENCE + appWidgetId, progress).apply();
        editor.putString(Constants.MILESTONE_WIDGET_PREFERENCE + appWidgetId, milestoneTitle).apply();

        AbstractProgressWidget.updateFromCache(this);
    }

    private void showErrorMessage() {
        if (errorMessage != null) {
            Snackbar.make(recyclerView, errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.visitHomepage:
                openWebPage(this, "http://papyros.io");

                try {
                    sendEventHit(this, Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Visit papyros.io", null);
                } catch (Exception e) {
                    // I don't want to cause this any errors,
                    // because that would seem weird to the user
                }
                return true;
            case R.id.visitGithub:
                openWebPage(this, "https://github.com/papyros");

                try {
                    sendEventHit(this, Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Visit Github page", null);
                } catch (Exception ignored) { /*ignored*/ }

                return true;
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_ACTIVITY_REQUEST_CODE);
            default:
                return false;
        }
    }

    public static void sendEventHit(Activity activity, String category, String action, String label) {
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
        if (category != null) builder.setCategory(category);
        if (action != null) builder.setAction(action);
        if (label != null) builder.setLabel(label);
        ((GAApplication) activity.getApplication()).getTracker().send(builder.build());
    }

    public static void openWebPage(Context context, String URL) {
        if (URLUtil.isValidUrl(URL)) {
            Uri webPage = Uri.parse(URL);
            Intent intent = new Intent(Intent.ACTION_VIEW, webPage);

            // Check if the web intent is safe (if a browser is installed)
            PackageManager packageManager = context.getPackageManager();
            List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (activities.size() > 0) {
                context.startActivity(intent);
            }
        }
    }

    public static String getFile(Context context, String fileName) {
        StringBuilder sb;

        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader buffReader = new BufferedReader(isr);

            sb = new StringBuilder();
            String line;
            while ((line = buffReader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveFile(Context context, String fileContent, String fileName) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
