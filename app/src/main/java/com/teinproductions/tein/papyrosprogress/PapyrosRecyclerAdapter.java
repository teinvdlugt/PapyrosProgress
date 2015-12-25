package com.teinproductions.tein.papyrosprogress;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;


class PapyrosRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int DONT_SHOW_TEXT_SIZE_TILE = -1;

    private static final int ITEM_VIEW_TYPE_TEXT_SIZE = 0;
    private static final int ITEM_VIEW_TYPE_MILESTONE = 1;

    private JSONObject[] milestones;
    private Context context;
    private int textSize = DONT_SHOW_TEXT_SIZE_TILE;
    private OnTextSizeButtonClickListener listener;
    private boolean useOldProgressBar = false;

    public PapyrosRecyclerAdapter(Context context, JSONObject[] milestones, OnTextSizeButtonClickListener listener) {
        this.context = context;
        this.milestones = milestones;
        this.listener = listener;

        this.useOldProgressBar = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.OLD_PROGRESS_BAR_PREFERENCE, false);
    }

    public void setMilestones(JSONObject[] milestones) {
        sortByCreatedDate(milestones);
        this.milestones = milestones;
        notifyDataSetChanged();
    }

    private static void sortByCreatedDate(JSONObject[] milestones) {
        // First, get the create-dates of all milestones
        long[] createdDates = new long[milestones.length];
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(MileStoneViewHolder.JSON_DATE_FORMAT);
        for (int i = 0; i < createdDates.length; i++) {
            createdDates[i] = 0;
            try {
                String formattedDate = milestones[i].getString(MainActivity.CREATED_AT);
                createdDates[i] = dateFormat.parse(formattedDate).getTime();
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }

        // Sort by created date in ascending order TODO Move closed milestones to bottom of list
        // Uses bubble sort algorithm
        for (int i = milestones.length - 1; i > 1; i--) {
            for (int j = 0; j < i; j++) {
                if (createdDates[j] > createdDates[j + 1]) {
                    JSONObject temp = milestones[j];
                    milestones[j] = milestones[j + 1];
                    milestones[j + 1] = temp;

                    long temp2 = createdDates[j];
                    createdDates[j] = createdDates[j + 1];
                    createdDates[j + 1] = temp2;
                }
            }
        }
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (textSize != -1 && position == 0) {
            return ITEM_VIEW_TYPE_TEXT_SIZE;
        } else {
            return ITEM_VIEW_TYPE_MILESTONE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_MILESTONE:
                View itemViewMilestone = LayoutInflater.from(context).inflate(R.layout.list_item_milestone, viewGroup, false);
                return new MileStoneViewHolder(itemViewMilestone);
            case ITEM_VIEW_TYPE_TEXT_SIZE:
                View itemViewTextSize = LayoutInflater.from(context).inflate(R.layout.list_item_text_size, viewGroup, false);
                return new TextSizeViewHolder(itemViewTextSize, listener);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_MILESTONE:
                if (textSize != -1) position--;
                ((MileStoneViewHolder) viewHolder).showData(context, milestones[position], useOldProgressBar);
                break;
            case ITEM_VIEW_TYPE_TEXT_SIZE:
                ((TextSizeViewHolder) viewHolder).showData(context, textSize);
        }
    }

    @Override
    public int getItemCount() {
        if (textSize == -1) {
            return milestones.length;
        } else {
            return milestones.length + 1;
        }
    }

    public void setUseOldProgressBar(boolean useOldProgressBar) {
        this.useOldProgressBar = useOldProgressBar;
        notifyDataSetChanged();
    }

    interface OnTextSizeButtonClickListener {
        void onClickApply(int progress);
    }
}


class MileStoneViewHolder extends RecyclerView.ViewHolder {
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private TextView titleTV, openIssuesTV, closedIssuesTV, progressTV, stateTV,
            createdAtTV, updatedAtTV, dueOnTV, closedAtTV;
    private ProgressBar oldProgressBar;
    private PapyrosProgressBar progressBar;
    private Button githubButton;

    public MileStoneViewHolder(View itemView) {
        super(itemView);

        titleTV = (TextView) itemView.findViewById(R.id.name_textView);
        openIssuesTV = (TextView) itemView.findViewById(R.id.open_issues_textView);
        closedIssuesTV = (TextView) itemView.findViewById(R.id.closed_issues_textView);
        progressTV = (TextView) itemView.findViewById(R.id.progress_textView);
        progressBar = (PapyrosProgressBar) itemView.findViewById(R.id.listItem_progressBar);
        oldProgressBar = (ProgressBar) itemView.findViewById(R.id.oldProgressBar);
        stateTV = (TextView) itemView.findViewById(R.id.state_textView);
        createdAtTV = (TextView) itemView.findViewById(R.id.createdAt);
        updatedAtTV = (TextView) itemView.findViewById(R.id.updatedAt);
        dueOnTV = (TextView) itemView.findViewById(R.id.dueOn);
        closedAtTV = (TextView) itemView.findViewById(R.id.closedAt);
        githubButton = (Button) itemView.findViewById(R.id.github_button);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void showData(final Context context, JSONObject data, boolean useOldProgressBar) {
        if (useOldProgressBar) {
            progressBar.setVisibility(View.GONE);
            oldProgressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            oldProgressBar.setVisibility(View.GONE);
        }


        String name = context.getString(R.string.unknown);
        String state = context.getString(R.string.unknown);
        String createdAt = null, updatedAt = null, dueOn = null, closedAt = null, githubURL = null;
        int openIssues = 0, closedIssues = 0, progress;

        try {
            name = data.getString(MainActivity.MILESTONE_TITLE);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            openIssues = data.getInt(MainActivity.OPEN_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            closedIssues = data.getInt(MainActivity.CLOSED_ISSUES);
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            state = data.getString(MainActivity.STATE);
        } catch (JSONException ignored) { /*ignore*/ }
        if (state == null) state = "null";
        try {
            if (!data.isNull(MainActivity.CREATED_AT)) {
                createdAt = data.getString(MainActivity.CREATED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data.isNull(MainActivity.UPDATED_AT)) {
                updatedAt = data.getString(MainActivity.UPDATED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data.isNull(MainActivity.DUE_ON)) {
                dueOn = data.getString(MainActivity.DUE_ON);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data.isNull(MainActivity.CLOSED_AT)) {
                closedAt = data.getString(MainActivity.CLOSED_AT);
            }
        } catch (JSONException ignored) { /*ignore*/ }
        try {
            if (!data.isNull(MainActivity.GITHUB_URL)) {
                githubURL = data.getString(MainActivity.GITHUB_URL);
            }
        } catch (JSONException ignored) { /*ignore*/ }

        progress = closedIssues * 100 / (openIssues + closedIssues);

        titleTV.setText(name);
        openIssuesTV.setText(context.getString(R.string.open_issues) + " " + openIssues);
        closedIssuesTV.setText(context.getString(R.string.closed_issues) + " " + closedIssues);
        progressBar.setProgress(progress);
        oldProgressBar.setProgress(progress);
        progressTV.setText(context.getString(R.string.progress) + " " + progress + "%");
        stateTV.setText(context.getString(R.string.state) + " " + state);

        @SuppressLint("SimpleDateFormat") DateFormat oldFormat = new SimpleDateFormat(JSON_DATE_FORMAT);
        DateFormat newFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
        if (createdAt == null) createdAt = context.getString(R.string.unknown);
        createdAtTV.setText(context.getString(R.string.created_at) + " " + reformatDate(oldFormat, newFormat, createdAt));
        if (updatedAt == null) updatedAtTV.setVisibility(View.GONE);
        else
            updatedAtTV.setText(context.getString(R.string.updated_at) + " " + reformatDate(oldFormat, newFormat, updatedAt));
        if (dueOn == null) dueOnTV.setVisibility(View.GONE);
        else
            dueOnTV.setText(context.getString(R.string.due_on) + " " + reformatDate(oldFormat, newFormat, dueOn));
        if (closedAt == null) closedAtTV.setVisibility(View.GONE);

        if (githubURL == null) {
            githubButton.setOnClickListener(null);
            githubButton.setVisibility(View.GONE); // TODO doesn't look pretty but should not happen very often
        } else {
            final String finalGithubURL = githubURL;
            githubButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.openWebPage(context, finalGithubURL);
                }
            });
        }
    }

    private static String reformatDate(DateFormat oldFormat, DateFormat newFormat, String dateStr) {
        try {
            Date date = oldFormat.parse(dateStr);
            return newFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr;
        }
    }
}


class TextSizeViewHolder extends RecyclerView.ViewHolder {
    private SeekBar seekBar;
    private TextView textSizeTextView;
    private PapyrosRecyclerAdapter.OnTextSizeButtonClickListener mListener;

    public TextSizeViewHolder(View itemView, PapyrosRecyclerAdapter.OnTextSizeButtonClickListener listener) {
        super(itemView);
        this.mListener = listener;

        seekBar = (SeekBar) itemView.findViewById(R.id.textSize_SeekBar);
        textSizeTextView = (TextView) itemView.findViewById(R.id.textSize_textView);
        Button applyButton = (Button) itemView.findViewById(R.id.okButton);

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onClickApply(seekBar.getProgress());
            }
        });
    }

    public void showData(final Context context, int textSize) {
        seekBar.setProgress(textSize);
        textSizeTextView.setText(context.getString(R.string.text_size) + " " + textSize);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSizeTextView.setText(context.getString(R.string.text_size) + " " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {/*ignored*/}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {/*ignored*/}
        });
    }
}