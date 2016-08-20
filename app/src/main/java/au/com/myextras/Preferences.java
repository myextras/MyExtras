package au.com.myextras;

import android.content.Context;
import android.preference.PreferenceManager;

public class Preferences {

    public static final String CODE = "code";
    public static final String TITLE = "title";

    public static String getCode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(CODE, null);
    }

    public static void setCode(Context context, String code) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(CODE, code)
                .apply();
    }

    public static String getTitle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(TITLE, null);
    }

    public static void setTitle(Context context, String title) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(TITLE, title)
                .apply();
    }

}
