package au.com.myextras;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
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
            String code = Preferences.getCode(this);
            FeedParser.Result result = FeedParser.parse("https://www.myextras.com.au/rssb/" + code);

            if (result.feed.title != null) {
                ContentResolver contentResolver = getContentResolver();

                Preferences.setTitle(this, result.feed.title);

                for (FeedParser.Result.Feed.Entry entry : result.feed.entries) {
                    ContentValues values = new ContentValues();
                    values.put(Bulletin.Column.CODE, code);
                    values.put(Bulletin.Column.GUID, entry.guid);
                    values.put(Bulletin.Column.TITLE, entry.title);
                    values.put(Bulletin.Column.LINK, entry.link);
                    values.put(Bulletin.Column.CONTENT, entry.content);
                    values.put(Bulletin.Column.PUBLISHED, entry.publishedTimestamp.getTime());

                    contentResolver.insert(Bulletin.CONTENT_URI, values);
                }

                contentResolver.notifyChange(Bulletin.CONTENT_URI, null);
            }
        } catch (FeedParserException exception) {
            Log.e(getClass().getName(), "Sync failed", exception);
        }
    }

}
