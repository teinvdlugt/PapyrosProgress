package com.teinproductions.tein.papyrosprogress;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


class LoadWebPageTask extends AsyncTask<Void, Void, LoadWebPageTask.Response> {

    private OnLoadedListener listener;
    private final String url;

    public LoadWebPageTask(OnLoadedListener onLoadedListener) {
        this(Constants.MILESTONES_URL, onLoadedListener);
    }

    public LoadWebPageTask(String url, OnLoadedListener onLoadedListener) {
        this.url = url;
        this.listener = onLoadedListener;
    }

    @Override
    protected Response doInBackground(Void... params) {
        try {
            URL url = new URL(LoadWebPageTask.this.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(20000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int responseCode = conn.getResponseCode();

            if (responseCode < 200 || responseCode >= 400) {
                return new Response(responseCode, null);
            }

            InputStream is = conn.getInputStream();
            return new Response(responseCode, read(is));
        } catch (IOException e) {
            e.printStackTrace();
            return new Response(666, null);
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
    protected void onPostExecute(Response r) {
        listener.onLoaded(r);
    }

    public static class Response {
        public final int responseCode;
        public final String content;

        public Response(int responseCode, String content) {
            this.responseCode = responseCode;
            this.content = content;
        }
    }

    interface OnLoadedListener {
        void onLoaded(Response result);
    }
}
