package com.teinproductions.tein.papyrosprogress;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;


public class MainActivity extends AppCompatActivity {

    public static final String URL = "https://api.github.com/repos/papyros/papyros-shell/milestones";
    public static final String MILESTONE_TITLE = "title";
    public static final String OPEN_ISSUES = "open_issues";
    public static final String CLOSED_ISSUES = "closed_issues";
    public static final String STATE = "state";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DUE_ON = "due_on";
    public static final String CLOSED_AT = "closed_at";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }
}