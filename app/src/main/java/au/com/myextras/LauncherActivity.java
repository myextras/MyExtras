package au.com.myextras;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Shows splash screen and initial configuration.
 */
public class LauncherActivity extends AppCompatActivity implements View.OnClickListener {

    private static final long SPLASH_TIME_LONG = 1500;
    private static final long SPLASH_TIME_SHORT = 500;

    private static boolean firstLaunch = true;

    private View configurationView;
    private EditText codeEditText;
    private View doneButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launcher);

        configurationView = findViewById(R.id.configuration);

        codeEditText = (EditText) findViewById(R.id.code);
        codeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Preferences.setCode(view.getContext(), view.getText().toString());

                    startMainActivity();
                }

                return false;
            }
        });

        doneButton = findViewById(R.id.done);
        doneButton.setOnClickListener(this);

        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                onSplashTimeout();
                return true;
            }
        });
        handler.sendEmptyMessageDelayed(0, firstLaunch ? SPLASH_TIME_LONG : SPLASH_TIME_SHORT);
    }

    private void onSplashTimeout() {
        firstLaunch = false;

        if (Preferences.getCode(this) == null) {
            configurationView.setVisibility(View.VISIBLE);
        } else {
            startMainActivity();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == doneButton) {
            Preferences.setCode(view.getContext(), codeEditText.getText().toString());

            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, EntriesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        startService(new Intent(this, SyncService.class));

        finish();
    }

}
