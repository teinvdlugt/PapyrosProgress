package com.teinproductions.tein.papyrosprogress;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;


public class MainActivity extends AppCompatActivity implements LoadWebPageTask.OnLoadedListener {

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

    public static final String CACHE_FILE = "papyros_cache";

    private RecyclerView recyclerView;
    private TextView errorTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        errorTextView = (TextView) findViewById(R.id.noNetwork_textView);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        load();
    }

    private void load() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new LoadWebPageTask(this).execute();
        } else {
            String cache = getCache(this);
            if (cache != null) {
                onLoaded(cache);
                showNetworkSnackbar();
            } else {
                progressBar.setVisibility(View.GONE);
                errorTextView.setText(R.string.no_network);
                errorTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaded(String json) {
        if ("403".equals(json) || "404".equals(json)) {
            String cache = getCache(this);
            if (cache != null && !cache.equals(json)) { // To prevent infinite recursion
                onLoaded(cache);
                showNetworkSnackbar();
            } else {
                errorTextView.setText("403".equals(json) ? R.string.error403 : R.string.error404);
                errorTextView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        try {
            JSONArray jArray = new JSONArray(json);
            JSONObject[] jObjects = new JSONObject[jArray.length()];
            for (int i = 0; i < jObjects.length; i++) {
                jObjects[i] = jArray.getJSONObject(i);
            }

            // Set list adapter
            Log.d("papyrosprogress", "set recycler adapter");
            recyclerView.setAdapter(new PapyrosRecyclerAdapter(jObjects, 33, this));

            // Fade out progress bar and fade in listView, if API >= 14
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            if (Build.VERSION.SDK_INT >= 14) {
                progressBar.animate().alpha(0f).setDuration(duration)
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        }).start();
                recyclerView.setAlpha(0f);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.animate().alpha(1f).setDuration(duration).start();
                Log.d("papyrosprogress", "recyclerView faded in");
            } else {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            // Nothing went wrong, so the web page contents are correct and can be cached
            saveCache(this, json);

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            // This means the retrieved web page was not the right one
            String cache = getCache(this);
            if (cache != null && !cache.equals(json)) { // To prevent infinite recursion
                onLoaded(cache);
                showNetworkSnackbar();
            } else {
                errorTextView.setText(R.string.error404);
                errorTextView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void showNetworkSnackbar() {
        Snackbar.make(recyclerView, getString(R.string.offline_snackbar), Snackbar.LENGTH_LONG).show();
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
                return true;
            case R.id.visitGithub:
                openWebPage(this, "https://github.com/papyros");
                return true;
        }

        return false;
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
            Log.d("cachethedata", "cached");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}