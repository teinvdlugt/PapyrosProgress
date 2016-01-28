package com.teinproductions.tein.papyrosprogress;


public class Constants {
    // URLs
    public static final String PAPYROS_BLOG_API_URL = "https://api.github.com/repos/papyros/papyros.github.io/contents/_posts";
    public static final String MILESTONES_URL = "https://api.github.com/repos/papyros/papyros-shell/milestones";
    public static final String PAPYROS_BLOG_URL = "http://papyros.io/blog/";

    // Settings
    public static final String NOTIFICATION_PREFERENCE = "notifications";
    public static final String NOTIFICATION_SOUND_PREF = "notification_sound";
    public static final String NOTIFICATION_VIBRATE_PREF = "notification_vibrate";
    public static final String NOTIFICATION_LIGHT_PREF = "notification_light";

    // Other preference keys
    public static final String NOTIFICATION_ASKED_PREFERENCE = "notification_asked";
    public static final String TEXT_SIZE_PREFERENCE = "text_size";
    public static final String MILESTONE_WIDGET_PREFERENCE = "milestone_";
    public static final String CACHED_BLOG_AMOUNT = "cached_blog_amount";

    // File/SharedPreferences names
    public static final String MILESTONES_CACHE_FILE = "papyros_cache";
    @Deprecated public static final String SHARED_PREFERENCES = "shared_preferences";

    // Google Analytics
    public static final String GA_EXTERNAL_LINKS_EVENT_CATEGORY = "External links";
    public static final String GA_PREFERENCES_EVENT_CATEGORY = "Preferences";
}
