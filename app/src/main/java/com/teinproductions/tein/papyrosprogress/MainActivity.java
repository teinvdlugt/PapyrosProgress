package com.teinproductions.tein.papyrosprogress;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements LoadWebPageTask.OnLoadedListener, SwipeRefreshLayout.OnRefreshListener,
        PapyrosRecyclerAdapter.OnTextSizeButtonClickListener {

    public static final String URL = "https://api.github.com/repos/papyros/papyros-shell/milestones";
    public static final String MILESTONE_TITLE = "title";
    public static final String OPEN_ISSUES = "open_issues";
    public static final String CLOSED_ISSUES = "closed_issues";
    public static final String STATE = "state";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DUE_ON = "due_on";
    public static final String CLOSED_AT = "closed_at";
    public static final String GITHUB_URL = "html_url";

    public static final String EXTRA_SMALL_WIDGET = "small_widget";
    private static final String CACHE_FILE = "papyros_cache";
    public static final String SHARED_PREFERENCES = "shared_preferences";
    public static final String TEXT_SIZE_PREFERENCE = "text_size";
    public static final String NOTIFICATION_PREFERENCE = "notifications";
    private static final String NOTIFICATION_ASKED_PREFERENCE = "notification_asked";
    public static final String OLD_PROGRESS_BAR_PREFERENCE = "old_progress_bar";

    private RecyclerView recyclerView;
    private PapyrosRecyclerAdapter adapter;
    private SwipeRefreshLayout srLayout;

    private int appWidgetId;
    private JSONObject[] data = new JSONObject[0];
    private String errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        srLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        srLayout.setOnRefreshListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PapyrosRecyclerAdapter(this, data, this);
        recyclerView.setAdapter(adapter);

        restoreAppWidgetStuff();
        onRefresh();

        checkNotificationsAsked();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restoreAppWidgetStuff();
    }

    private void checkNotificationsAsked() {
        final SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        if (!preferences.getBoolean(NOTIFICATION_ASKED_PREFERENCE, false)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.notification_dialog_title))
                    .setMessage(getString(R.string.notification_dialog_message))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putBoolean(NOTIFICATION_PREFERENCE, true).apply();
                            setOrCancelAlarm(MainActivity.this, true);
                            invalidateOptionsMenu();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putBoolean(NOTIFICATION_PREFERENCE, false).apply();
                            invalidateOptionsMenu();
                        }
                    }).create().show();
            preferences.edit().putBoolean(NOTIFICATION_ASKED_PREFERENCE, true).apply();
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
            adapter.setTextSize(getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .getInt(TEXT_SIZE_PREFERENCE + appWidgetId, 24));
        } else {
            adapter.setTextSize(PapyrosRecyclerAdapter.DONT_SHOW_TEXT_SIZE_TILE);
        }
    }

    @Override
    public void onRefresh() {
        // First try with cache:
        String cache = getCache(this);
        if (cache != null) {
            try {
                parseJSON(cache);
                adapter.setMilestones(data);
            } catch (JSONException e) {
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
    public void onLoaded(String json) {
        srLayout.setRefreshing(false);
        if ("403".equals(json) || "404".equals(json)) {
            errorMessage = getString("403".equals(json) ? R.string.error403 : R.string.error404);
            showErrorMessage();
            return;
        }

        try {
            parseJSON(json);
            adapter.setMilestones(data);

            // Nothing went wrong, so the web page contents are correct and can be cached
            saveCache(this, json);
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            // This means the retrieved web page was not the right one
            errorMessage = getString(R.string.error404);
        }
    }

    private void parseJSON(String json) throws JSONException {
        JSONArray jArray = new JSONArray(json);
        data = new JSONObject[jArray.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = jArray.getJSONObject(i);
        }
    }

    @Override
    public void onClickApply(int progress) {
        getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit()
                .putInt(TEXT_SIZE_PREFERENCE + appWidgetId, progress).apply();

        boolean smallWidget = getIntent().getBooleanExtra(EXTRA_SMALL_WIDGET, false);
        if (smallWidget) ProgressWidgetSmall.updateAppWidgets(this, new int[]{appWidgetId});
        else ProgressWidget.updateAppWidgets(this, new int[]{appWidgetId});
    }

    private void showErrorMessage() {
        if (errorMessage != null) {
            Snackbar.make(recyclerView, errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        boolean notifications = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).getBoolean(NOTIFICATION_PREFERENCE, false);
        menu.findItem(R.id.notification).setChecked(notifications);

        boolean oldProgressBar = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).getBoolean(OLD_PROGRESS_BAR_PREFERENCE, false);
        menu.findItem(R.id.oldProgressBar).setChecked(oldProgressBar);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.visitHomepage:
                openWebPage(this, "http://papyros.io");
                return true;
            case R.id.visitGithub:
                openWebPage(this, "https://github.com/papyros");
                return true;
            case R.id.notification:
                if (item.isChecked()) {
                    // Cancel alarm
                    item.setChecked(false);
                    setOrCancelAlarm(this, false);
                } else {
                    // Schedule alarm
                    item.setChecked(true);
                    setOrCancelAlarm(this, true);
                }

                // Save preference
                getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit()
                        .putBoolean(NOTIFICATION_PREFERENCE, item.isChecked()).apply();
                return true;
            case R.id.oldProgressBar:
                item.setChecked(!item.isChecked());
                adapter.setUseOldProgressBar(item.isChecked());
                getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit()
                        .putBoolean(OLD_PROGRESS_BAR_PREFERENCE, item.isChecked()).apply();
                return true;
            case R.id.rate_app:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.teinproductions.tein.papyrosprogress"));
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    public static void setOrCancelAlarm(Context context, boolean set) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1, new Intent(context, NotificationReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        if (set) alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
        else alarmManager.cancel(pendingIntent);

        ComponentName receiver = new ComponentName(context, NotificationReceiver.class);
        PackageManager pm = context.getPackageManager();
        int receiverEnabled = set ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver,
                receiverEnabled,
                PackageManager.DONT_KILL_APP);
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

    public static String getCache(Context context) {
        StringBuilder sb;

        try {
            FileInputStream fis = context.openFileInput(CACHE_FILE);
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

    public static void saveCache(Context context, String cache) {
        try {
            FileOutputStream fos = context.openFileOutput(CACHE_FILE, MODE_PRIVATE);
            fos.write(cache.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}