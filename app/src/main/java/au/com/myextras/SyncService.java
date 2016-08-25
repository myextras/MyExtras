package au.com.myextras;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
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
                    values.put(Entry.Columns.BULLETIN, code);
                    values.put(Entry.Columns.GUID, entry.guid);
                    values.put(Entry.Columns.TITLE, entry.title);
                    values.put(Entry.Columns.LINK, entry.link);
                    values.put(Entry.Columns.CONTENT, entry.content);
                    values.put(Entry.Columns.PUBLISHED, entry.publishedTimestamp.getTime());
                    values.put(Entry.Columns.IMPORTANT, "important".equals(entry.category));

                    contentResolver.insert(Entry.CONTENT_URI, values);
                }

                contentResolver.notifyChange(Entry.CONTENT_URI, null);
            }
        } catch (FeedParserException exception) {
            Log.e(getClass().getName(), "Sync failed", exception);
        }

        scheduleNextSync();
    }


    private void scheduleNextSync() {
        // on work days:
        //   every 15 minutes from 7:00 to 10:00
        //   every hour from 10:00 to 23:00
        // saturday:
        //   no updates
        // sunday:
        //   every hour from 18:00 to 23:00

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) / 15 * 15);

        int hour;
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour < 18) {
                    calendar.set(Calendar.HOUR_OF_DAY, 18);
                } else if (hour < 23) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                } else {
                    calendar.add(Calendar.DATE, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 7);
                }
                calendar.set(Calendar.MINUTE, 0);
                break;
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:
            case Calendar.FRIDAY:
                hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour < 7) {
                    calendar.set(Calendar.HOUR_OF_DAY, 7);
                    calendar.set(Calendar.MINUTE, 0);
                } else if (hour < 10) {
                    calendar.add(Calendar.MINUTE, 15);
                } else if (hour < 23) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                    calendar.set(Calendar.MINUTE, 0);
                } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    calendar.add(Calendar.DATE, 2);
                    calendar.set(Calendar.HOUR_OF_DAY, 18);
                    calendar.set(Calendar.MINUTE, 0);
                } else {
                    calendar.add(Calendar.DATE, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 7);
                    calendar.set(Calendar.MINUTE, 0);
                }
                break;
            case Calendar.SATURDAY:
                calendar.add(Calendar.DATE, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 18);
                calendar.set(Calendar.MINUTE, 0);
                break;
        }

        Date nextSyncDate = calendar.getTime();
        Log.d(getClass().getName(), "Next sync: " + nextSyncDate);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(this, SyncService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, nextSyncDate.getTime(), pendingIntent);
        } else {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

            JobInfo jobInfo = new JobInfo.Builder(R.id.sync_job, new ComponentName(this, SyncJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(nextSyncDate.getTime() - System.currentTimeMillis())
                    .build();

            jobScheduler.schedule(jobInfo);
        }
    }

}
