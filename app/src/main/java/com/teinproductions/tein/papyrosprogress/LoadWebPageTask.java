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

@Deprecated
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
        return IOUtils.loadPage(LoadWebPageTask.this.url);
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
