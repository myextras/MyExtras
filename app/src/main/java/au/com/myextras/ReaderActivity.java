package au.com.myextras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ReaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener {

    public static final String EXTRA_CURSOR_POSITION = "cursor_position";

    private static final String[] ENTRY_PROJECTION = {
            Entry.Columns.ID,
            Entry.Columns.TITLE,
            Entry.Columns.LINK,
            Entry.Columns.CONTENT,
            Entry.Columns.PUBLISHED,
            Entry.Columns.ACCESSED,
    };
    private static final String ENTRY_ORDER = Entry.Columns.PUBLISHED + " DESC";
    protected static final int ENTRY_ID = 0;
    protected static final int ENTRY_TITLE = 1;
    protected static final int ENTRY_LINK = 2;
    protected static final int ENTRY_CONTENT = 3;
    protected static final int ENTRY_PUBLISHED = 4;
    protected static final int ENTRY_ACCESSED = 5;

    private ViewPager pager;
    protected Adapter adapter = new Adapter();
    protected Cursor cursor;

    private Intent resultData;

    private MenuItem refreshMenuItem;

    private LocalBroadcastManager localBroadcastManager;

    private boolean syncing;
    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SyncService.ACTION_SYNC_STARTED:
                    syncing = true;
                    if (refreshMenuItem != null) {
                        refreshMenuItem.setActionView(R.layout.refresh_action);
                    }
                    break;
                case SyncService.ACTION_SYNC_FINISHED:
                    syncing = false;
                    if (refreshMenuItem != null) {
                        refreshMenuItem.setActionView(null);
                    }
                    break;
                case SyncService.ACTION_SYNC_FAILED:
                    Snackbar.make(pager, R.string.sync_failed, Snackbar.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        pager = (ViewPager) findViewById(R.id.pager);
        assert pager != null;
        pager.addOnPageChangeListener(this);
        pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.reader_pager_margin));
        pager.setAdapter(adapter);

        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(0, null, this);

        resultData = new Intent().putExtra(EXTRA_CURSOR_POSITION, getIntent().getIntExtra(EXTRA_CURSOR_POSITION, 0));
        setResult(RESULT_OK, resultData);

        if (savedInstanceState != null) {
            resultData.putExtra(EXTRA_CURSOR_POSITION, savedInstanceState.getInt(EXTRA_CURSOR_POSITION));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter syncReceiverFilter = new IntentFilter();
        syncReceiverFilter.addAction(SyncService.ACTION_SYNC_STARTED);
        syncReceiverFilter.addAction(SyncService.ACTION_SYNC_FINISHED);
        syncReceiverFilter.addAction(SyncService.ACTION_SYNC_FAILED);
        localBroadcastManager.registerReceiver(syncReceiver, syncReceiverFilter);

        SyncService.requestStatus(this);
    }

    @Override
    protected void onPause() {
        localBroadcastManager.unregisterReceiver(syncReceiver);

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CURSOR_POSITION, resultData.getIntExtra(EXTRA_CURSOR_POSITION, 0));

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        refreshMenuItem = menu.findItem(R.id.refresh);
        if (syncing) {
            refreshMenuItem.setActionView(R.layout.refresh_action);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                SyncService.requestSync(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Entry.CONTENT_URI, ENTRY_PROJECTION, null, null, ENTRY_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.cursor = cursor;
        adapter.notifyDataSetChanged();

        pager.setCurrentItem(resultData.getIntExtra(EXTRA_CURSOR_POSITION, 0), false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursor = null;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // nothing to do here
    }

    @Override
    public void onPageSelected(int position) {
        resultData.putExtra(EXTRA_CURSOR_POSITION, position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // nothing to do here
    }

    protected class Adapter extends FragmentStatePagerAdapter {

        public Adapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            cursor.moveToPosition(position);
            Bundle arguments = new Bundle();
            arguments.putInt(ReaderEntryFragment.ARG_CURSOR_POSITION, position);
            return Fragment.instantiate(ReaderActivity.this, ReaderEntryFragment.class.getName(), arguments);
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }

}