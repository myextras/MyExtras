package au.com.myextras;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader);

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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CURSOR_POSITION, resultData.getIntExtra(EXTRA_CURSOR_POSITION, 0));

        super.onSaveInstanceState(outState);
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