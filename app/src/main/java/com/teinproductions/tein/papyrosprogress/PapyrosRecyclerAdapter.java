package com.teinproductions.tein.papyrosprogress;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;


class PapyrosRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int DONT_SHOW_TEXT_SIZE_TILE = -1;

    private static final int ITEM_VIEW_TYPE_TEXT_SIZE = 0;
    private static final int ITEM_VIEW_TYPE_MILESTONE = 1;

    private Activity activity; // Needed for Google Analytics event reporting
    private List<Milestone> milestones;
    private int textSize = DONT_SHOW_TEXT_SIZE_TILE;
    private String widgetMilestoneTitle;
    private OnTextSizeButtonClickListener listener;

    public PapyrosRecyclerAdapter(Activity activity, List<Milestone> milestones, OnTextSizeButtonClickListener listener) {
        this.activity = activity;
        this.milestones = milestones;
        this.listener = listener;
    }

    public void setMilestones(List<Milestone> milestones) {
        if (milestones.size() > 1) sortByCreatedDate(milestones);
        this.milestones = milestones;
        notifyDataSetChanged();
    }

    private static void sortByCreatedDate(List<Milestone> milestones) {
        // Sort by created date in ascending order TODO Move closed milestones to bottom of list
        // Uses bubble sort algorithm

        for (int i = milestones.size() - 1; i > 1; i--) {
            for (int j = 0; j < i; j++) {
                if (milestones.get(j).getCreatedAt() > milestones.get(j + 1).getCreatedAt()) {
                    Milestone temp = milestones.get(j);
                    milestones.set(j, milestones.get(j + 1));
                    milestones.set(j + 1, temp);
                }
            }
        }

        // Move closed milestones to bottom of list
        int amountMoved = 0;
        for (int i = 0; i < milestones.size(); i++) {
            if (milestones.get(i - amountMoved).getClosedAt() != -1) {
                milestones.add(milestones.remove(i - amountMoved));
                amountMoved++;
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
                ((MileStoneViewHolder) viewHolder).showData(activity, milestones.get(position));
                break;
            case ITEM_VIEW_TYPE_TEXT_SIZE:
                String[] milestoneTitles = new String[milestones.size()];
                for (int i = 0; i < milestoneTitles.length; i++) {
                    milestoneTitles[i] = milestones.get(i).getTitle();
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
            return milestones.size();
        } else {
            return milestones.size() + 1;
        }
    }

    interface OnTextSizeButtonClickListener {
        void onClickApply(int progress, String milestoneTitle);
    }
}


class MileStoneViewHolder extends RecyclerView.ViewHolder {
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String MILESTONE_COLLAPSED_PREFERENCE = "milestone_collapsed_"; // Append the title of the Milestone

    private TextView titleTV, progressAbbrTV, milestoneInfoTV;
    private PapyrosProgressBar progressBar;
    private Button githubButton;
    private ImageButton collapseButton;
    private ViewGroup milestoneInfoContainer;

    private Milestone milestone;
    private Activity context;

    public MileStoneViewHolder(View itemView) {
        super(itemView);

        titleTV = (TextView) itemView.findViewById(R.id.name_textView);
        progressAbbrTV = (TextView) itemView.findViewById(R.id.progressAbbr_textView);
        collapseButton = (ImageButton) itemView.findViewById(R.id.collapse_imageButton);
        milestoneInfoContainer = (ViewGroup) itemView.findViewById(R.id.milestone_content_container);
        milestoneInfoTV = (TextView) itemView.findViewById(R.id.milestoneInfo_textView);
        progressBar = (PapyrosProgressBar) itemView.findViewById(R.id.listItem_progressBar);
        githubButton = (Button) itemView.findViewById(R.id.github_button);

        itemView.findViewById(R.id.collapse_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickCollapse();
            }
        });
    }

    private void onClickCollapse() {
        boolean collapse; // Value to save to preferences
        if (milestoneInfoContainer.getVisibility() == View.VISIBLE) {
            collapse();
            collapse = true;
        } else {
            expand();
            collapse = false;
        }
        if (context != null)
            context.getPreferences(Context.MODE_PRIVATE).edit()
                    .putBoolean(MILESTONE_COLLAPSED_PREFERENCE + milestone.getTitle(), collapse).apply();
    }

    private void collapse() {
        collapseButton.setImageResource(R.mipmap.ic_keyboard_arrow_down_black_24dp);
        final int initialHeight = milestoneInfoContainer.getMeasuredHeight();

        progressAbbrTV.setAlpha(0f);
        progressAbbrTV.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    milestoneInfoContainer.setVisibility(View.GONE);
                } else {
                    milestoneInfoContainer.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    milestoneInfoContainer.requestLayout();
                }
                progressAbbrTV.setAlpha(interpolatedTime);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
        milestoneInfoContainer.startAnimation(a);
    }

    private void expand() {
        collapseButton.setImageResource(R.mipmap.ic_keyboard_arrow_up_black_24dp);
        milestoneInfoContainer.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = milestoneInfoContainer.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        milestoneInfoContainer.getLayoutParams().height = 1;
        milestoneInfoContainer.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                milestoneInfoContainer.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                milestoneInfoContainer.requestLayout();

                if (interpolatedTime == 1) progressAbbrTV.setVisibility(View.GONE);
                else progressAbbrTV.setAlpha(1 - interpolatedTime);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime));
        milestoneInfoContainer.startAnimation(a);
    }

    private void collapseOrExpandInstant(boolean collapse) {
        // (Instant means without animation)
        if (collapse) {
            // Collapse the milestone
            milestoneInfoContainer.setVisibility(View.GONE);
            collapseButton.setImageResource(R.mipmap.ic_keyboard_arrow_down_black_24dp);
            progressAbbrTV.setVisibility(View.VISIBLE);
        } else {
            // Expand the milestone
            milestoneInfoContainer.setVisibility(View.VISIBLE);
            collapseButton.setImageResource(R.mipmap.ic_keyboard_arrow_up_black_24dp);
            progressAbbrTV.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void showData(Activity context, Milestone milestone) {
        this.milestone = milestone;
        this.context = context;

        int progress = milestone.getProgress();

        // Set texts
        titleTV.setText(milestone.getTitle());
        progressBar.setProgress(progress);
        progressAbbrTV.setText(context.getString(R.string.collapsed_progress_text, progress));

        StringBuilder milestoneInfo = new StringBuilder();
        milestoneInfo.append(context.getString(R.string.open_issues, milestone.getOpenIssues()));
        milestoneInfo.append("\n").append(context.getString(R.string.closed_issues, milestone.getClosedIssues()));
        milestoneInfo.append("\n").append(context.getString(R.string.progress, progress));
        milestoneInfo.append("\n").append(context.getString(R.string.state, getStateText(context, milestone)));

        // Set date texts
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
        if (milestone.getCreatedAt() == -1)
            milestoneInfo.append("\n").append(context.getString(R.string.unknown));
        else
            milestoneInfo.append("\n").append(context.getString(R.string.created_at, format.format(new Date(milestone.getCreatedAt()))));
        if (milestone.getUpdatedAt() != -1)
            milestoneInfo.append("\n").append(context.getString(R.string.updated_at, format.format(new Date(milestone.getUpdatedAt()))));
        if (milestone.getDueOn() != -1)
            milestoneInfo.append("\n").append(context.getString(R.string.due_on, format.format(new Date(milestone.getDueOn()))));
        if (milestone.getClosedAt() != -1)
            milestoneInfo.append("\n").append(context.getString(R.string.closed_at, format.format(new Date(milestone.getClosedAt()))));

        milestoneInfoTV.setText(milestoneInfo);

        // Set github url text
        if (milestone.getGithubUrl() == null) {
            githubButton.setOnClickListener(null);
            githubButton.setVisibility(View.GONE);
        } else {
            githubButton.setVisibility(View.VISIBLE);
            githubButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.sendEventHit(MileStoneViewHolder.this.context, Constants.GA_EXTERNAL_LINKS_EVENT_CATEGORY,
                            "View on github", MileStoneViewHolder.this.milestone.getTitle());
                    MainActivity.openWebPage(MileStoneViewHolder.this.context, MileStoneViewHolder.this.milestone.getGithubUrl());
                }
            });
        }

        boolean collapse = context.getPreferences(Context.MODE_PRIVATE)
                .getBoolean(MILESTONE_COLLAPSED_PREFERENCE + milestone.getTitle(), false);
        collapseOrExpandInstant(collapse);
    }

    private String getStateText(Context context, Milestone milestone) {
        String state = milestone.getState();
        if ("open".equals(state)) return context.getString(R.string.state_open);
        if ("closed".equals(state)) return context.getString(R.string.state_closed);
        if ("all".equals(state)) return context.getString(R.string.state_all);
        return state;
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