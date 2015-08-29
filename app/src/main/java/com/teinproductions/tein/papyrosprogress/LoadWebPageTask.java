package com.teinproductions.tein.papyrosprogress;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


class LoadWebPageTask extends AsyncTask<Void, Void, String> {

    private String url = MainActivity.URL;
    private OnLoadedListener listener;

    public LoadWebPageTask(OnLoadedListener onLoadedListener) {
        this.listener = onLoadedListener;
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

            if (response == 403) return "403";
            if (response == 404) return "404";

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
    protected void onPostExecute(String s) {
        listener.onLoaded(s);
    }

    interface OnLoadedListener {
        void onLoaded(String result);
    }
}
