package au.com.myextras;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import au.com.myextras.rss.FeedParser;
import au.com.myextras.rss.FeedParserException;

public class SyncService extends IntentService {

    private static final String ACTION_SYNC_REQUESTED = BuildConfig.APPLICATION_ID + "intent.action.SYNC_REQUESTED";
    private static final String ACTION_SYNC_STATUS = BuildConfig.APPLICATION_ID + "intent.action.SYNC_STATUS";

    public static final String ACTION_SYNC_STARTED = BuildConfig.APPLICATION_ID + "intent.action.SYNC_STARTED";
    public static final String ACTION_SYNC_FINISHED = BuildConfig.APPLICATION_ID + "intent.action.SYNC_FINISHED";
    public static final String ACTION_SYNC_FAILED = BuildConfig.APPLICATION_ID + "intent.action.SYNC_FAILED";

    private boolean syncing;

    public SyncService() {
        super(SyncService.class.getSimpleName());
    }

    public static void requestSync(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_SYNC_REQUESTED);
        context.startService(intent);
    }

    public static void requestStatus(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_SYNC_STATUS);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_SYNC_STATUS.equals(action)) {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(syncing ? ACTION_SYNC_STARTED : ACTION_SYNC_FINISHED));
        }

        if (!ACTION_SYNC_REQUESTED.equals(action) || syncing) {
            // ignore intent
            intent = null;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Log.i(getClass().getName(), "Syncing");

        try {
            syncing = true;

            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(ACTION_SYNC_STARTED));

            String code = Preferences.getCode(this);
            FeedParser.Result result = FeedParser.parse("https://www.myextras.com.au/rssb/" + code);

            if (result.status != HttpsURLConnection.HTTP_OK) {
                throw new FeedParserException("Unexpected status: " + result.status);
            }

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

            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(ACTION_SYNC_FAILED));
        } finally {
            syncing = false;

            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(ACTION_SYNC_FINISHED));

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
            Intent intent = new Intent(this, SyncService.class)
                    .setAction(SyncService.ACTION_SYNC_REQUESTED);
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
