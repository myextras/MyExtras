package au.com.myextras;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentById(android.R.id.content) == null) {
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, Fragment.instantiate(this, PreferencesFragment.class.getName()))
                    .commit();
        }
    }

    public static class PreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        private SharedPreferences preferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            preferences = getPreferenceManager().getSharedPreferences();

            Preference codePreference = findPreference(Preferences.CODE);
            codePreference.setSummary(preferences.getString(Preferences.CODE, null));
            codePreference.setOnPreferenceChangeListener(this);

            Preference versionPreference = findPreference("version");
            versionPreference.setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case Preferences.CODE:
                    String oldValue = preferences.getString(Preferences.CODE, null);
                    if (oldValue != null && oldValue.equals(newValue)) {
                        return false;
                    }

                    preference.setSummary(newValue.toString());

                    preferences.edit()
                            .remove(Preferences.TITLE)
                            .apply();

                    Context context = preference.getContext();
                    context.getContentResolver().notifyChange(Entry.CONTENT_URI, null);
                    SyncService.requestSync(context);

                    break;
            }

            return true;
        }

    }

}
