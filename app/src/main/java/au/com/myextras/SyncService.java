package au.com.myextras;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import au.com.myextras.rss.FeedParser;
import au.com.myextras.rss.FeedParserException;

public class SyncService extends IntentService {

    public SyncService() {
        super(SyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(getClass().getName(), "Syncing");

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

        scheduleNextSync();
    }


    private void scheduleNextSync() {
        // every 15 minutes from 7:00 to 9:00
        // every hour from 10:00 to 23:00
        // no updates on weekends

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) / 15 * 15);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SATURDAY:
                calendar.add(Calendar.DATE, 1);
                // break omitted intentionally
            case Calendar.SUNDAY:
                calendar.add(Calendar.DATE, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 7);
                calendar.set(Calendar.MINUTE, 0);
                break;
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:
            case Calendar.FRIDAY:
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour < 7) {
                    calendar.set(Calendar.HOUR_OF_DAY, 7);
                    calendar.set(Calendar.MINUTE, 0);
                } else if (hour < 9) {
                    calendar.add(Calendar.MINUTE, 15);
                } else if (hour < 23) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                    calendar.set(Calendar.MINUTE, 0);
                } else {
                    calendar.add(Calendar.DATE, calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY ? 3 : 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 7);
                    calendar.set(Calendar.MINUTE, 0);
                }
                break;
        }

        Date nextSyncDate = calendar.getTime();
        Log.d(getClass().getName(), "Next sync: " + nextSyncDate);

        Intent intent = new Intent(this, SyncService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, nextSyncDate.getTime(), pendingIntent);
    }

}
