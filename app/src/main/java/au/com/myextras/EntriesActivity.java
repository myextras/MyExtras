package au.com.myextras;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EntriesActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static final int LOADER_ENTRIES = 1;
    private static final int LOADER_NEW_ENTRIES = 2;

    private static final String[] ENTRY_PROJECTION = {
            Entry.Columns.ID,
            Entry.Columns.TITLE,
            Entry.Columns.PUBLISHED,
            Entry.Columns.ACCESSED,
            Entry.Columns.IMPORTANT,
    };
    private static final String ENTRY_SORT_ORDER = Entry.Columns.PUBLISHED + " DESC";
    private static final int ENTRY_ID = 0;
    private static final int ENTRY_TITLE = 1;
    private static final int ENTRY_PUBLISHED = 2;
    private static final int ENTRY_ACCESSED = 3;
    private static final int ENTRY_IMPORTANT = 4;

    private EntriesAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View doneAllButton;

    private LocalBroadcastManager localBroadcastManager;

    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SyncService.ACTION_SYNC_STARTED:
                    swipeRefreshLayout.setRefreshing(true);
                    break;
                case SyncService.ACTION_SYNC_FINISHED:
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case SyncService.ACTION_SYNC_FAILED:
                    Snackbar.make(swipeRefreshLayout, R.string.sync_failed, Snackbar.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.entries);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.branding_blue);
        swipeRefreshLayout.setOnRefreshListener(this);

        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout.findViewById(R.id.entries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            final int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            final int bottomPadding = getResources().getDimensionPixelSize(R.dimen.fab_size) + getResources().getDimensionPixelSize(R.dimen.fab_margin) * 2;

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int lastPosition = parent.getAdapter().getItemCount() - 1;

                outRect.set(spacing, position == 0 ? spacing : 0, spacing, position == lastPosition ? bottomPadding : spacing);
            }
        });
        recyclerView.setAdapter(adapter = new EntriesAdapter(this));

        doneAllButton = findViewById(R.id.done_all);
        doneAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Object, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Object... params) {
                        ContentResolver contentResolver = (ContentResolver) params[0];

                        contentResolver.update(Entry.CONTENT_URI, null, null, null);

                        return Boolean.TRUE;
                    }
                }.execute(getContentResolver());
            }
        });

        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(LOADER_ENTRIES, null, this);
        loaderManager.initLoader(LOADER_NEW_ENTRIES, null, this);


        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        final String hintProperty = "show_refresh_hint";
        if (preferences.getBoolean(hintProperty, true)) {
            Snackbar.make(swipeRefreshLayout, R.string.entries_swipe_hint, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            preferences.edit()
                                    .putBoolean(hintProperty, false)
                                    .apply();
                        }
                    })
                    .show();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.entries, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ENTRIES:
                return new CursorLoader(this, Entry.CONTENT_URI, ENTRY_PROJECTION, null, null, ENTRY_SORT_ORDER);
            case LOADER_NEW_ENTRIES:
                return new CursorLoader(this, Entry.CONTENT_URI, ENTRY_PROJECTION, Entry.Columns.PUBLISHED + " != " + Entry.Columns.ACCESSED, null, null);
        }

        throw new IllegalArgumentException("Unsupported loader: " + id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ENTRIES:
                adapter.setHeader(Preferences.getTitle(this));
                adapter.setCursor(cursor);
                break;
            case LOADER_NEW_ENTRIES:
                doneAllButton.setVisibility(cursor.getCount() == 0 ? View.INVISIBLE : View.VISIBLE);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ENTRIES:
                adapter.setCursor(null);
                break;
        }
    }

    @Override
    public void onRefresh() {
        SyncService.requestSync(this);
    }

    private abstract class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

    }

    private class HeaderViewHolder extends ViewHolder {

        final TextView schoolTextView;
        final TextView teacherTextView;
        final TextView dateTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);

            schoolTextView = (TextView) itemView.findViewById(R.id.school);
            teacherTextView = (TextView) itemView.findViewById(R.id.teacher);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
        }

    }

    private class EntryViewHolder extends ViewHolder implements View.OnClickListener {

        final TextView titleTextView;
        final TextView dateTextView;
        final View importantView;

        public EntryViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.title);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
            importantView = itemView.findViewById(R.id.important);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(EntriesActivity.this, ReaderActivity.class)
                    .putExtra(ReaderActivity.EXTRA_CURSOR_POSITION, getAdapterPosition() - 1);
            startActivity(intent);
        }

    }

    private class EntriesAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int VIEW_TYPE_HEADER = 0;
        private final int VIEW_TYPE_ENTRY = 1;

        private Context context;

        private String header;
        private Cursor cursor;

        public EntriesAdapter(Context context) {
            this.context = context;

            setHasStableIds(true);
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;

            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = getLayoutInflater();
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    return new HeaderViewHolder(layoutInflater.inflate(R.layout.entries_header, parent, false));
                default:
                    return new EntryViewHolder(layoutInflater.inflate(R.layout.entries_item, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            if (position == 0) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;

                if (header != null) {
                    String[] header = this.header.split("@");
                    String school = header[0].trim();
                    String teacher = header.length > 1 ? header[1].trim() : null;

                    headerViewHolder.schoolTextView.setText(school);
                    headerViewHolder.teacherTextView.setText(teacher);
                    headerViewHolder.teacherTextView.setVisibility(teacher != null ? View.VISIBLE : View.GONE);
                }

                long lastUpdate = Preferences.getLastUpdate(context);
                headerViewHolder.dateTextView.setText(getString(R.string.last_check, DateUtils.formatDateTime(context, lastUpdate, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME)));
            } else {
                EntryViewHolder entryViewHolder = (EntryViewHolder) viewHolder;

                cursor.moveToPosition(position - 1);

                entryViewHolder.titleTextView.setText(cursor.getString(ENTRY_TITLE));
                entryViewHolder.titleTextView.setTypeface(cursor.getLong(ENTRY_PUBLISHED) != cursor.getLong(ENTRY_ACCESSED) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                entryViewHolder.dateTextView.setText(getString(R.string.last_update, DateUtils.formatDateTime(context, cursor.getLong(ENTRY_PUBLISHED), DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME)));
                entryViewHolder.importantView.setVisibility(cursor.getInt(ENTRY_IMPORTANT) != 0 ? View.VISIBLE : View.GONE);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ENTRY;
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return -1;
            }

            cursor.moveToPosition(position - 1);

            return cursor.getLong(ENTRY_ID);
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() + 1 : 0;
        }

    }

}

