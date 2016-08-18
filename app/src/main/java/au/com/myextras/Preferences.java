package au.com.myextras;

import android.content.Context;
import android.preference.PreferenceManager;

public class Preferences {

    private static final String PREF_SCHOOL_CODE = "school_code";

    public static String getSchoolCode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SCHOOL_CODE, null);
    }

    public static void setSchoolCode(Context context, String schoolCode) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SCHOOL_CODE, schoolCode)
                .apply();
    }

}
