package com.teinproductions.tein.papyrosprogress;


import android.annotation.SuppressLint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONUtils {

    public static final String MILESTONE_TITLE = "title";
    public static final String OPEN_ISSUES = "open_issues";
    public static final String CLOSED_ISSUES = "closed_issues";
    public static final String STATE = "state";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DUE_ON = "due_on";
    public static final String CLOSED_AT = "closed_at";
    public static final String GITHUB_URL = "html_url";

    public static Map<String, Integer> getProgressMap(String json) {
        try {
            JSONArray jArray = new JSONArray(json);

            Map<String, Integer> progress = new HashMap<>();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject milestone = jArray.getJSONObject(i);
                progress.put(JSONUtils.getTitle(milestone), JSONUtils.getProgress(milestone));
            }

            return progress;
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static String getTitle(JSONObject milestone) throws JSONException {
        return milestone.getString(MILESTONE_TITLE);
    }

    public static int getProgress(JSONObject milestone) throws JSONException {
        int openIssues = milestone.getInt(OPEN_ISSUES);
        int closedIssues = milestone.getInt(CLOSED_ISSUES);
        return closedIssues * 100 / (openIssues + closedIssues);
    }

    public static List<Milestone> getMilestones(String json) throws JSONException, ParseException {
        JSONArray jArray = new JSONArray(json);
        List<Milestone> result = new ArrayList<>();

        for (int i = 0; i < jArray.length(); i++) {
            result.add(getMilestone(jArray.getJSONObject(i)));
        }

        return result;
    }

    public static Milestone getMilestone(JSONObject jObject) throws JSONException, ParseException {
        // Define variables
        String title = null;
        String state = null;
        String githubURL = null;
        long createdAt = -1, updatedAt = -1, dueOn = -1, closedAt = -1;
        int openIssues = 0, closedIssues = 0;

        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat(MileStoneViewHolder.JSON_DATE_FORMAT);

        // Extract data from json
        try {
            title = jObject.getString(MILESTONE_TITLE);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            openIssues = jObject.getInt(OPEN_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            closedIssues = jObject.getInt(CLOSED_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            state = jObject.getString(STATE);
        } catch (JSONException ignored) { /*ignore*/ }
        if (state == null) state = "null";
        try {
            if (!jObject.isNull(CREATED_AT)) {
                createdAt = format.parse(jObject.getString(CREATED_AT)).getTime();
            }
        } catch (JSONException | ParseException ignored) { /*ignore*/ }
        try {
            if (!jObject.isNull(UPDATED_AT)) {
                updatedAt = format.parse(jObject.getString(UPDATED_AT)).getTime();
            }
        } catch (JSONException | ParseException ignored) { /*ignore*/ }
        try {
            if (!jObject.isNull(DUE_ON)) {
                dueOn = format.parse(jObject.getString(DUE_ON)).getTime();
            }
        } catch (JSONException | ParseException ignored) { /*ignore*/ }
        try {
            if (!jObject.isNull(CLOSED_AT)) {
                closedAt = format.parse(jObject.getString(CLOSED_AT)).getTime();
            }
        } catch (JSONException | ParseException ignored) { /*ignore*/ }
        try {
            if (!jObject.isNull(GITHUB_URL)) {
                githubURL = jObject.getString(GITHUB_URL);
            }
        } catch (JSONException ignored) { /*ignore*/ }

        // Construct Milestone object
        return new Milestone(title, openIssues, closedIssues, state, createdAt, updatedAt, dueOn, closedAt, githubURL);
    }
}
