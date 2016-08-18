package au.com.myextras;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import au.com.myextras.rss.FeedParser;
import au.com.myextras.rss.FeedParserException;

public class SyncService extends IntentService {

    public SyncService() {
        super(SyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            FeedParser.Result result = FeedParser.parse("https://www.myextras.com.au/rssb/" + Preferences.getSchoolCode(this));

            Log.d(getClass().getName(), "result: " + result);
        } catch (FeedParserException exception) {
            Log.e(getClass().getName(), "Sync failed", exception);
        }
    }

}
