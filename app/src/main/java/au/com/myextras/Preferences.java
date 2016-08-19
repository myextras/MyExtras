package au.com.myextras;

import android.content.Context;
import android.preference.PreferenceManager;

public class Preferences {

    private static final String PREF_CODE = "code";
    private static final String PREF_TITLE = "title";

    public static String getCode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_CODE, null);
    }

    public static void setCode(Context context, String code) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_CODE, code)
                .apply();
    }

    public static String getTitle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_TITLE, null);
    }

    public static void setTitle(Context context, String title) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_TITLE, title)
                .apply();
    }

}
