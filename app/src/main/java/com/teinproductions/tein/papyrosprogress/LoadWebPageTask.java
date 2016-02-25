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
