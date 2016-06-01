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

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class IOUtils {
    public static String getFile(Context context, String fileName) {
        StringBuilder sb;

        try {
            FileInputStream fis = context.openFileInput(fileName);
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

    public static void saveFile(Context context, String fileContent, String fileName) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendGAEventHit(Activity activity, String category, String action, String label) {
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
        if (category != null) builder.setCategory(category);
        if (action != null) builder.setAction(action);
        if (label != null) builder.setLabel(label);
        ((GAApplication) activity.getApplication()).getTracker().send(builder.build());
    }

    public static MilestoneLoader.Result loadPage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(20000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int responseCode = conn.getResponseCode();

            if (responseCode < 200 || responseCode >= 400) {
                MilestoneLoader.Result result = new MilestoneLoader.Result(MilestoneLoader.Result.ERROR_CODE);
                result.setErrorCode(responseCode);
                return result;
            }

            InputStream is = conn.getInputStream();
            String read = read(is);
            return new MilestoneLoader.Result(read);
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            return new MilestoneLoader.Result(MilestoneLoader.Result.SOCKET_TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
            return new MilestoneLoader.Result(MilestoneLoader.Result.UNKNOWN_ERROR);
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }
}
