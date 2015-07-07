package com.teinproductions.tein.papyrosprogress;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class LoadWebPageTask extends AsyncTask<Void, Void, String> {

    private String url = MainActivity.URL;
    private OnLoadedListener listener;
    private Context context;

    public LoadWebPageTask(Context context, String url, OnLoadedListener onLoadedListener) {
        this.url = url;
        this.listener = onLoadedListener;
        this.context = context;
    }

    public LoadWebPageTask(Context context, OnLoadedListener onLoadedListener) {
        this.context = context;
        this.listener = onLoadedListener;
    }

    public void setListener(OnLoadedListener listener) {
        this.listener = listener;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    @Override
    protected String doInBackground(Void... params) {
        try {
            URL url = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(20000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            //Log.d("COFFEE", "Response is " + response);

            if (response == 403) return "403";
            if (response == 404) return "404";

            InputStream is = conn.getInputStream();
            String result = read(is);
            MainActivity.saveCache(context, result);
            return result;
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
    protected void onPostExecute(String s) {
        listener.onLoaded(s);
    }

    interface OnLoadedListener {
        void onLoaded(String result);
    }
}
