package com.teinproductions.tein.papyrosprogress;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


/**
 * Subclass of {@code android.app.Application}, needed for Google Analytics tracking.
 * Note {@code AndroidManifest.xml}, where the {@code android:name} attribute of the
 * {@code <application>} tag has been set to {@code .GAApplication}.
 */
public class GAApplication extends Application {

    public Tracker mTracker;

    public void startTracking() {
        if (mTracker == null) {
            GoogleAnalytics ga = GoogleAnalytics.getInstance(this);

            mTracker = ga.newTracker(R.xml.tracker_config);
            ga.enableAutoActivityReports(this);
        }
    }

    public Tracker getTracker() {
        startTracking();
        return mTracker;
    }
}
