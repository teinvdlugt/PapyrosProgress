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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONException;

import java.text.ParseException;
import java.util.List;

public class MilestoneLoader extends AsyncTaskLoader<MilestoneLoader.Result> {

    private Result data;

    public MilestoneLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (data != null)
            deliverResult(data);
        else forceLoad();
    }

    @Override
    public Result loadInBackground() {
        ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            Result result = new Result();
            result.setError(Result.NO_INTERNET_CONNECTION);
            return result;
        }

        Result result = IOUtils.loadPage(Constants.MILESTONES_URL);
        if (result.getError() == Result.NO_ERROR) {
            try {
                result.setData(JSONUtils.getMilestones(result.getStrData()));
            } catch (ParseException | JSONException | NullPointerException e) {
                e.printStackTrace();
                result.setError(Result.JSON_PARSE_ERROR);
            }
        }
        return result;
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();
        data = null;
    }

    @Override
    public void deliverResult(Result data) {
        if (isReset()) return;
        this.data = data;
        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    public static class Result {
        public static final int NO_ERROR = -1;
        public static final int NO_INTERNET_CONNECTION = 0;
        public static final int SOCKET_TIMEOUT = 1;
        public static final int UNKNOWN_ERROR = 2;
        public static final int ERROR_CODE = 3; // error code will be stored in errorCode
        public static final int JSON_PARSE_ERROR = 4;

        private int error = NO_ERROR;    // One of NO_ERROR, NO_INTERNET_CONNECTION etc.
        private int errorCode;           // 404, 403 etc.
        private List<Milestone> data;    // Either the data or strData should be used
        private String strData;          // (IOUtils.loadPage() returns a Result with strData, MilestoneLoader returns a Result with List<Milestone> data)

        public Result() {}

        public Result(int error) {
            this.error = error;
        }

        public Result(String strData) {
            this.strData = strData;
        }

        public int getError() {
            return error;
        }

        public void setError(int error) {
            this.error = error;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public List<Milestone> getData() {
            return data;
        }

        public void setData(List<Milestone> data) {
            this.data = data;
        }

        public String getStrData() {
            return strData;
        }

        public void setStrData(String strData) {
            this.strData = strData;
        }
    }
}
