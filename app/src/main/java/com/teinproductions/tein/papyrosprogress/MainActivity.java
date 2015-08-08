package com.teinproductions.tein.papyrosprogress;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;


public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
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