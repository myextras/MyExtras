package au.com.myextras;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BulletinsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_BULLETINS = 1;

    private static final String[] BULLETINS_PROJECTION = {
            Bulletin.Column.ID,
            Bulletin.Column.TITLE,
            Bulletin.Column.PUBLISHED,
            Bulletin.Column.ACCESSED,
    };
    private static final String BULLETINS_SELECTION = Bulletin.Column.CODE + " = ?";
    private static final String BULLETINS_SORT_ORDER = Bulletin.Column.PUBLISHED + " DESC";
    private static final int BULLETIN_ID = 0;
    private static final int BULLETIN_TITLE = 1;
    private static final int BULLETIN_PUBLISHED = 2;
    private static final int BULLETIN_ACCESSED = 3;

    private BulletinAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bulletins_activity);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.bulletins);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            final int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);

                outRect.set(spacing, position == 0 ? spacing : 0, spacing, spacing);
            }
        });
        recyclerView.setAdapter(adapter = new BulletinAdapter(this));

        final View actionButton = findViewById(R.id.done_all);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionButton.setVisibility(View.GONE);
            }
        });

        getSupportLoaderManager().initLoader(LOADER_BULLETINS, null, this);

        startService(new Intent(this, SyncService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bulletins_activity, menu);

        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] selectionArgs = { Preferences.getCode(this) };
        return new CursorLoader(this, Bulletin.CONTENT_URI, BULLETINS_PROJECTION, BULLETINS_SELECTION, selectionArgs, BULLETINS_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(Preferences.getTitle(this));
        }

        adapter.setCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setCursor(null);
    }

    private class BulletinViewHolder extends RecyclerView.ViewHolder {

        final TextView titleTextView;
        final TextView dateTextView;

        public BulletinViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.title);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
        }

    }

    private class BulletinAdapter extends RecyclerView.Adapter<BulletinViewHolder> {

        private Context context;

        private Cursor cursor;

        public BulletinAdapter(Context context) {
            this.context = context;

            setHasStableIds(true);
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;

            notifyDataSetChanged();
        }

        @Override
        public BulletinViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.bulletin_item, parent, false);
            return new BulletinViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BulletinViewHolder holder, int position) {
            cursor.moveToPosition(position);

            holder.titleTextView.setText(cursor.getString(BULLETIN_TITLE));
            holder.titleTextView.setTypeface(cursor.getLong(BULLETIN_PUBLISHED) != cursor.getLong(BULLETIN_ACCESSED) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            holder.dateTextView.setText(getString(R.string.last_update, DateUtils.formatDateTime(context, cursor.getLong(BULLETIN_PUBLISHED), DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME)));
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);

            return cursor.getLong(BULLETIN_ID);
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }

}

