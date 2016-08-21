package au.com.myextras;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SyncJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        startService(new Intent(this, SyncService.class));

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

}
