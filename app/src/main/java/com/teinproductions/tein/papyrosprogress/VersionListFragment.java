package com.teinproductions.tein.papyrosprogress;


import android.animation.Animator;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VersionListFragment extends Fragment implements LoadWebPageTask.OnLoadedListener {

    RecyclerView recyclerView;
    TextView errorTextView;
    ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View theView = inflater.inflate(R.layout.fragment_version_list, container, false);

        errorTextView = (TextView) theView.findViewById(R.id.noNetwork_textView);
        progressBar = (ProgressBar) theView.findViewById(R.id.progress_bar);
        recyclerView = (RecyclerView) theView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ConnectivityManager connManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new LoadWebPageTask(this).execute();
        } else {
            theView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
            errorTextView.setText(R.string.no_network);
            errorTextView.setVisibility(View.VISIBLE);
        }

        return theView;
    }

    @Override
    public void onLoaded(String json) {
        if ("403".equals(json) || "404".equals(json)) {
            errorTextView.setText("403".equals(json) ? R.string.error403 : R.string.error404);
            errorTextView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        try {
            JSONArray jArray = new JSONArray(json);
            JSONObject[] jObjects = new JSONObject[jArray.length()];
            for (int i = 0; i < jObjects.length; i++) {
                jObjects[i] = jArray.getJSONObject(i);
            }

            // Set list adapter
            recyclerView.setAdapter(new MilestoneRecyclerAdapter(jObjects));

            // Fade out progress bar and fade in listView, if API >= 14
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            if (Build.VERSION.SDK_INT >= 14) {
                progressBar.animate().alpha(0f).setDuration(duration)
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        }).start();
                recyclerView.setAlpha(0f);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.animate().alpha(1f).setDuration(duration).start();
            } else {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }


    private class MilestoneRecyclerAdapter extends RecyclerView.Adapter<MileStoneViewHolder> {
        JSONObject[] data;

        public MilestoneRecyclerAdapter(JSONObject[] data) {
            this.data = data;
        }

        @Override
        public MileStoneViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item, viewGroup, false);
            return new MileStoneViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MileStoneViewHolder viewHolder, int i) {
            String name = getString(R.string.unknown_version_name);
            int openIssues = 0, closedIssues = 0, progress;
            try { name = data[i].getString(MainActivity.MILESTONE_TITLE);
            } catch (JSONException ignored) { /*ignore*/ }
            try { openIssues = data[i].getInt(MainActivity.OPEN_ISSUES);
            } catch (JSONException ignored) { /*ignore*/ }
            try { closedIssues = data[i].getInt(MainActivity.CLOSED_ISSUES);
            } catch (JSONException ignored) { /*ignore*/ }

            progress = closedIssues * 100 / (openIssues + closedIssues);

            viewHolder.title.setText(name);
            viewHolder.openIssues.setText(getString(R.string.open_issues) + " " + openIssues);
            viewHolder.closedIssues.setText(getString(R.string.closed_issues) + " " + closedIssues);
            viewHolder.progressBar.setProgress(progress);
            viewHolder.progressTV.setText(getString(R.string.progress) + " " + progress + "%");
        }

        @Override
        public int getItemCount() {
            return data.length;
        }
    }

    private class MileStoneViewHolder extends RecyclerView.ViewHolder {
        TextView title, openIssues, closedIssues, progressTV;
        ProgressBar progressBar;

        public MileStoneViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.name_textView);
            openIssues = (TextView) itemView.findViewById(R.id.open_issues_textView);
            closedIssues = (TextView) itemView.findViewById(R.id.closed_issues_textView);
            progressTV = (TextView) itemView.findViewById(R.id.progress_textView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.listItem_progressBar);
        }
    }
}
