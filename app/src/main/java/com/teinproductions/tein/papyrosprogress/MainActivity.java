package com.teinproductions.tein.papyrosprogress;

import android.animation.Animator;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    public static final String URL = "https://api.github.com/repos/papyros/papyros-shell/milestones";
    public static final String MILESTONE_TITLE = "title";
    public static final String OPEN_ISSUES = "open_issues";
    public static final String CLOSED_ISSUES = "closed_issues";

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new LoadFromTheWebTask().execute();
        } else {
            findViewById(R.id.progress_bar).setVisibility(View.GONE);
            findViewById(R.id.noNetwork_textView).setVisibility(View.VISIBLE);
        }
    }


    private class MilestoneAdapter extends ArrayAdapter<JSONObject> {

        JSONObject[] data;

        public MilestoneAdapter(Context context, JSONObject[] data) {
            super(context, R.layout.list_item);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View theView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);

            String name = getString(R.string.unknown_version_name);
            int openIssues = 0, closedIssues = 0, progress = 0;
            try {
                name = data[position].getString(MILESTONE_TITLE);
            } catch (JSONException ignored) { /*ignore*/ }
            try {
                openIssues = data[position].getInt(OPEN_ISSUES);
            } catch (JSONException ignored) { /*ignore*/ }
            try {
                closedIssues = data[position].getInt(CLOSED_ISSUES);
            } catch (JSONException ignored) { /*ignore*/ }

            progress = closedIssues * 100 / (openIssues + closedIssues);

            ((TextView) theView.findViewById(R.id.name_textView)).setText(name);
            ((TextView) theView.findViewById(R.id.open_issues_textView))
                    .setText(getString(R.string.open_issues) + " " + openIssues);
            ((TextView) theView.findViewById(R.id.closed_issues_textView))
                    .setText(getString(R.string.closed_issues) + " " + closedIssues);
            ((ProgressBar) theView.findViewById(R.id.listItem_progressBar)).setProgress(progress);
            ((TextView) theView.findViewById(R.id.progress_textView))
                    .setText(getString(R.string.progress) + " " + progress + "%");

            return theView;
        }
    }


    private class LoadFromTheWebTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(URL);
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
                e.printStackTrace();
                return null;
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
        protected void onPostExecute(String json) {
            try {
                JSONArray jArray = new JSONArray(json);
                JSONObject[] jObjects = new JSONObject[jArray.length()];
                for (int i = 0; i < jObjects.length; i++) {
                    jObjects[i] = jArray.getJSONObject(i);
                }

                // Set list adapter
                listView.setAdapter(new MilestoneAdapter(MainActivity.this, jObjects));

                // Fade out progress bar and fade in listView, if API >= 14
                View progressBar = findViewById(R.id.progress_bar);
                int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);

                if (Build.VERSION.SDK_INT >= 14) {
                    progressBar.animate()
                            .alpha(0f)
                            .setDuration(duration)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            }).start();
                    listView.setAlpha(0f);
                    listView.setVisibility(View.VISIBLE);
                    listView.animate().alpha(1f).setDuration(duration).start();
                } else {
                    progressBar.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }

            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
}
