package au.com.myextras;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
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

    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences preferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            preferences = getPreferenceManager().getSharedPreferences();

            onSharedPreferenceChanged(preferences, Preferences.CODE);
        }

        @Override
        public void onStart() {
            super.onStart();

            preferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            preferences.unregisterOnSharedPreferenceChangeListener(this);

            super.onStop();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            switch (key) {
                case Preferences.CODE:
                    Preference preference = findPreference(Preferences.CODE);
                    preference.setSummary(preferences.getString(Preferences.CODE, null));

                    preferences.edit()
                            .remove(Preferences.TITLE)
                            .apply();

                    Context context = preference.getContext();
                    context.getContentResolver().notifyChange(Entry.CONTENT_URI, null);
                    context.startService(new Intent(context, SyncService.class));

                    break;
            }
        }

    }

}
