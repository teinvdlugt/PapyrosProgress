package com.teinproductions.tein.papyrosprogress;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PapyrosRecyclerAdapter extends RecyclerView.Adapter<MileStoneViewHolder> {
    JSONObject[] data;
    Context context;

    public PapyrosRecyclerAdapter(JSONObject[] data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public MileStoneViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
        return new MileStoneViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MileStoneViewHolder viewHolder, int i) {
        String name = context.getString(R.string.unknown);
        String state = context.getString(R.string.unknown);
        String createdAt = null, updatedAt = null, dueOn = null, closedAt = null, githubURL = null;
        int openIssues = 0, closedIssues = 0, progress;

        try {
            name = data[i].getString(MainActivity.MILESTONE_TITLE);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            openIssues = data[i].getInt(MainActivity.OPEN_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            closedIssues = data[i].getInt(MainActivity.CLOSED_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            state = data[i].getString(MainActivity.STATE);
        } catch (JSONException ignored) { /*ignore*/ }
        if (state == null) state = "null";
        try {
            if (!data[i].isNull(MainActivity.CREATED_AT)) {
                createdAt = data[i].getString(MainActivity.CREATED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data[i].isNull(MainActivity.UPDATED_AT)) {
                updatedAt = data[i].getString(MainActivity.UPDATED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data[i].isNull(MainActivity.DUE_ON)) {
                dueOn = data[i].getString(MainActivity.DUE_ON);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data[i].isNull(MainActivity.CLOSED_AT)) {
                closedAt = data[i].getString(MainActivity.CLOSED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data[i].isNull(MainActivity.GITHUB_URL)) {
                githubURL = data[i].getString(MainActivity.GITHUB_URL);
            }
        } catch (JSONException ignored) { /*ignore*/ }

        progress = closedIssues * 100 / (openIssues + closedIssues);

        viewHolder.title.setText(name);
        viewHolder.openIssues.setText(context.getString(R.string.open_issues) + " " + openIssues);
        viewHolder.closedIssues.setText(context.getString(R.string.closed_issues) + " " + closedIssues);
        viewHolder.progressBar.setProgress(progress);
        viewHolder.progressTV.setText(context.getString(R.string.progress) + " " + progress + "%");
        viewHolder.state.setText(context.getString(R.string.state) + " " + state);

        DateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateFormat newFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
        if (createdAt == null) createdAt = context.getString(R.string.unknown);
        viewHolder.createdAt.setText(context.getString(R.string.created_at) + " " + reformatDate(oldFormat, newFormat, createdAt));
        if (updatedAt == null) viewHolder.updatedAt.setVisibility(View.GONE);
        else
            viewHolder.updatedAt.setText(context.getString(R.string.updated_at) + " " + reformatDate(oldFormat, newFormat, updatedAt));
        if (dueOn == null) viewHolder.dueOn.setVisibility(View.GONE);
        else
            viewHolder.dueOn.setText(context.getString(R.string.due_on) + " " + reformatDate(oldFormat, newFormat, dueOn));
        if (closedAt == null) viewHolder.closedAt.setVisibility(View.GONE);

        if (githubURL == null) {
            viewHolder.githubButton.setOnClickListener(null);
            viewHolder.githubButton.setVisibility(View.GONE); // TODO doesn't look pretty but should not happen very often
        } else {
            final String finalGithubURL = githubURL;
            viewHolder.githubButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.openWebPage(context, finalGithubURL);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    public static String reformatDate(DateFormat oldFormat, DateFormat newFormat, String dateStr) {
        try {
            Date date = oldFormat.parse(dateStr);
            return newFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr;
        }
    }
}


class MileStoneViewHolder extends RecyclerView.ViewHolder {
    TextView title, openIssues, closedIssues, progressTV, state,
            createdAt, updatedAt, dueOn, closedAt;
    ProgressBar progressBar;
    Button githubButton;

    public MileStoneViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.name_textView);
        openIssues = (TextView) itemView.findViewById(R.id.open_issues_textView);
        closedIssues = (TextView) itemView.findViewById(R.id.closed_issues_textView);
        progressTV = (TextView) itemView.findViewById(R.id.progress_textView);
        progressBar = (ProgressBar) itemView.findViewById(R.id.listItem_progressBar);
        state = (TextView) itemView.findViewById(R.id.state_textView);
        createdAt = (TextView) itemView.findViewById(R.id.createdAt);
        updatedAt = (TextView) itemView.findViewById(R.id.updatedAt);
        dueOn = (TextView) itemView.findViewById(R.id.dueOn);
        closedAt = (TextView) itemView.findViewById(R.id.closedAt);
        githubButton = (Button) itemView.findViewById(R.id.github_button);
    }
}
