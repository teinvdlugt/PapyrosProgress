/*
 * Papyros Progress: An Android application showing the development progress of Papyros
 * Copyright (C) 2016  Tein van der Lugt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.teinproductions.tein.papyrosprogress;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;

import org.json.JSONException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener,
        PapyrosRecyclerAdapter.OnTextSizeButtonClickListener,
        LoaderManager.LoaderCallbacks<MilestoneLoader.Result> {
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 1;
    public static final String EXTRA_SMALL_WIDGET = "small_widget";
    private static final int LOADER_ID = 0;

    private RecyclerView recyclerView;
    private PapyrosRecyclerAdapter adapter;
    private SwipeRefreshLayout srLayout;

    private int appWidgetId;

    private CustomTabsHelper tabsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ((GAApplication) getApplication()).startTracking();
        tabsHelper = new CustomTabsHelper();

        checkNotificationsAsked();
        sendBroadcast(new Intent(this, UpdateCheckReceiver.class));

        srLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        if (srLayout != null) srLayout.setOnRefreshListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PapyrosRecyclerAdapter(this, new ArrayList<Milestone>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                addListPadding();
            }
        });

        restoreAppWidgetStuff();
        //onRefresh();
        loadCache();
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        tabsHelper.bindService(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restoreAppWidgetStuff();
    }

    @Override
    protected void onStop() {
        super.onStop();
        tabsHelper.unbindService(this);
    }

    private void addListPadding() {
        float maxListWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 620, getResources().getDisplayMetrics());
        if (recyclerView.getWidth() > maxListWidth) {
            int leftRightPadding = (int) ((recyclerView.getWidth() - maxListWidth) / 2);
            int topPadding = recyclerView.getPaddingTop();
            int bottomPadding = recyclerView.getPaddingBottom();
            recyclerView.setPadding(leftRightPadding, topPadding, leftRightPadding, bottomPadding);
        }
    }

    private void checkNotificationsAsked() {
        @SuppressWarnings("deprecation")
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
                    .setTitle(getString(R.string.notifications))
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

    private void loadCache() {
        // TODO Move this functionality into MilestoneLoader?
        String cache = IOUtils.getFile(this, Constants.MILESTONES_CACHE_FILE);
        if (cache != null) {
            try {
                adapter.setMilestones(JSONUtils.getMilestones(cache));
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRefresh() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<MilestoneLoader.Result> onCreateLoader(int id, Bundle args) {
        return new MilestoneLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<MilestoneLoader.Result> loader, MilestoneLoader.Result data) {
        if (loader.getId() == LOADER_ID) {
            srLayout.setRefreshing(false);
            switch (data.getError()) {
                case MilestoneLoader.Result.NO_ERROR:
                    if (data.getData() != null)
                        adapter.setMilestones(data.getData());
                    if (data.getStrData() != null)
                        IOUtils.saveFile(this, data.getStrData(), Constants.MILESTONES_CACHE_FILE);
                    break;
                case MilestoneLoader.Result.SOCKET_TIMEOUT:
                case MilestoneLoader.Result.UNKNOWN_ERROR:
                case MilestoneLoader.Result.JSON_PARSE_ERROR:
                    // TODO show special snackbar?
                case MilestoneLoader.Result.NO_INTERNET_CONNECTION:
                    showSnackbar(getString(R.string.offline_snackbar));
                    break;
                case MilestoneLoader.Result.ERROR_CODE:
                    if (data.getErrorCode() == 403) {
                        showSnackbar(getString(R.string.error403));
                    } else {
                        showSnackbar(getString(R.string.error404, data.getErrorCode()));
                    }
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<MilestoneLoader.Result> loader) {
        adapter.setMilestones(null); // TODO ?
    }

    @Override
    public void onClickApply(int progress, String milestoneTitle) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        editor.putInt(Constants.TEXT_SIZE_PREFERENCE + appWidgetId, progress).apply();
        editor.putString(Constants.MILESTONE_WIDGET_PREFERENCE + appWidgetId, milestoneTitle).apply();

        AbstractProgressWidget.updateFromCache(this);
    }

    private void showSnackbar(String message) {
        if (message != null && recyclerView != null) {
            Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG).show();
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
                    IOUtils.sendGAEventHit(this, Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Visit papyros.io", null);
                } catch (Exception e) {
                    // I don't want to cause this any errors,
                    // because that would seem weird to the user
                }
                return true;
            case R.id.visitGithub:
                openWebPage(this, "https://github.com/papyros");

                try {
                    IOUtils.sendGAEventHit(this, Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY, "Visit Github page", null);
                } catch (Exception ignored) { /*ignored*/ }

                return true;
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_ACTIVITY_REQUEST_CODE);
            default:
                return false;
        }
    }

    public static void openWebPage(MainActivity activity, String URL) {
        if (URLUtil.isValidUrl(URL)) {
            if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("use_custom_tabs", true)) {
                activity.tabsHelper.openURL(activity, URL);
            } else {
                Uri webPage = Uri.parse(URL);
                Intent intent = new Intent(Intent.ACTION_VIEW, webPage);

                PackageManager packageManager = activity.getPackageManager();
                List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                if (activities.size() > 0) {
                    activity.startActivity(intent);
                }
            }
        }
    }
}
