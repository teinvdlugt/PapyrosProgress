package com.teinproductions.tein.papyrosprogress;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;


class PapyrosRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int DONT_SHOW_TEXT_SIZE_TILE = -1;

    private static final int ITEM_VIEW_TYPE_TEXT_SIZE = 0;
    private static final int ITEM_VIEW_TYPE_MILESTONE = 1;

    private Activity activity; // Needed for Google Analytics event reporting
    private Milestone[] milestones;
    private int textSize = DONT_SHOW_TEXT_SIZE_TILE;
    private String widgetMilestoneTitle;
    private OnTextSizeButtonClickListener listener;
    private boolean useOldProgressBar = false;

    public PapyrosRecyclerAdapter(Activity activity, Milestone[] milestones, OnTextSizeButtonClickListener listener) {
        this.activity = activity;
        this.milestones = milestones;
        this.listener = listener;

        this.useOldProgressBar = activity.getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.OLD_PROGRESS_BAR_PREFERENCE, false);
    }

    public void setMilestones(Milestone[] milestones) {
        sortByCreatedDate(milestones);
        this.milestones = milestones;
        notifyDataSetChanged();
    }

    private static void sortByCreatedDate(Milestone[] milestones) {
        // Sort by created date in ascending order TODO Move closed milestones to bottom of list
        // Uses bubble sort algorithm

        for (int i = milestones.length - 1; i > 1; i--) {
            for (int j = 0; j < i; j++) {
                if (milestones[j].getCreatedAt() > milestones[j + 1].getCreatedAt()) {
                    Milestone temp = milestones[j];
                    milestones[j] = milestones[j + 1];
                    milestones[j + 1] = temp;
                }
            }
        }
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        notifyDataSetChanged();
    }

    public void setWidgetMilestoneTitle(String widgetMilestoneTitle) {
        this.widgetMilestoneTitle = widgetMilestoneTitle;
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
                View itemViewMilestone = LayoutInflater.from(activity).inflate(R.layout.list_item_milestone, viewGroup, false);
                return new MileStoneViewHolder(itemViewMilestone);
            case ITEM_VIEW_TYPE_TEXT_SIZE:
                View itemViewTextSize = LayoutInflater.from(activity).inflate(R.layout.list_item_text_size, viewGroup, false);
                return new TextSizeViewHolder(itemViewTextSize, listener);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_MILESTONE:
                if (textSize != -1) position--;
                ((MileStoneViewHolder) viewHolder).showData(activity, milestones[position], useOldProgressBar);
                break;
            case ITEM_VIEW_TYPE_TEXT_SIZE:
                String[] milestoneTitles = new String[milestones.length];
                for (int i = 0; i < milestoneTitles.length; i++) {
                    milestoneTitles[i] = milestones[i].getTitle();
                }
                int selectedItemPosition = 0;
                if (widgetMilestoneTitle != null)
                    for (int i = 0; i < milestoneTitles.length; i++)
                        if (widgetMilestoneTitle.equals(milestoneTitles[i])) {
                            selectedItemPosition = i;
                            break;
                        }
                ((TextSizeViewHolder) viewHolder).showData(activity, textSize, milestoneTitles, selectedItemPosition);
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
        void onClickApply(int progress, String milestoneTitle);
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
    public void showData(final Activity context, final Milestone milestone, boolean useOldProgressBar) {
        if (useOldProgressBar) {
            progressBar.setVisibility(View.GONE);
            oldProgressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            oldProgressBar.setVisibility(View.GONE);
        }

        int progress = milestone.getProgress();

        // Set texts
        titleTV.setText(milestone.getTitle());
        openIssuesTV.setText(context.getString(R.string.open_issues, milestone.getOpenIssues()));
        closedIssuesTV.setText(context.getString(R.string.closed_issues, milestone.getClosedIssues()));
        progressBar.setProgress(progress);
        oldProgressBar.setProgress(progress);
        progressTV.setText(context.getString(R.string.progress, progress));
        stateTV.setText(context.getString(R.string.state, milestone.getState()));

        // Set date texts
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
        if (milestone.getCreatedAt() == -1)
            createdAtTV.setText(context.getString(R.string.unknown));
        else
            createdAtTV.setText(context.getString(R.string.created_at, format.format(new Date(milestone.getCreatedAt()))));
        if (milestone.getUpdatedAt() == -1) updatedAtTV.setVisibility(View.GONE);
        else {
            updatedAtTV.setVisibility(View.VISIBLE);
            updatedAtTV.setText(context.getString(R.string.updated_at, format.format(new Date(milestone.getUpdatedAt()))));
        }
        if (milestone.getDueOn() == -1) dueOnTV.setVisibility(View.GONE);
        else {
            dueOnTV.setVisibility(View.VISIBLE);
            dueOnTV.setText(context.getString(R.string.due_on, format.format(new Date(milestone.getDueOn()))));
        }
        if (milestone.getClosedAt() == -1) closedAtTV.setVisibility(View.GONE);
        else {
            closedAtTV.setVisibility(View.VISIBLE);
            closedAtTV.setText(context.getString(R.string.closed_at, format.format(new Date(milestone.getClosedAt()))));
        }

        // Set github url text
        if (milestone.getGithubUrl() == null) {
            githubButton.setOnClickListener(null);
            githubButton.setVisibility(View.GONE);
        } else {
            githubButton.setVisibility(View.VISIBLE);
            githubButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.sendEventHit(context, MainActivity.GA_EXTERNAL_LINKS_EVENT_CATEGORY,
                            "View on github", milestone.getTitle());
                    MainActivity.openWebPage(context, milestone.getGithubUrl());
                }
            });
        }
    }
}


class TextSizeViewHolder extends RecyclerView.ViewHolder {
    private SeekBar seekBar;
    private TextView textSizeTextView;
    private Spinner spinner;
    private String[] milestoneTitles;
    private PapyrosRecyclerAdapter.OnTextSizeButtonClickListener mListener;

    public TextSizeViewHolder(View itemView, PapyrosRecyclerAdapter.OnTextSizeButtonClickListener listener) {
        super(itemView);
        this.mListener = listener;

        seekBar = (SeekBar) itemView.findViewById(R.id.textSize_SeekBar);
        textSizeTextView = (TextView) itemView.findViewById(R.id.textSize_textView);
        spinner = (Spinner) itemView.findViewById(R.id.milestone_spinner);
        Button applyButton = (Button) itemView.findViewById(R.id.okButton);

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onClickApply(seekBar.getProgress(), milestoneTitles[spinner.getSelectedItemPosition()]);
            }
        });
    }

    public void showData(final Context context, int textSize, String[] milestoneTitles, int selectedItem) {
        seekBar.setProgress(textSize);
        textSizeTextView.setText(context.getString(R.string.text_size, textSize));

        this.milestoneTitles = milestoneTitles;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                milestoneTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedItem);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSizeTextView.setText(context.getString(R.string.text_size, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {/*ignored*/}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {/*ignored*/}
        });
    }
}