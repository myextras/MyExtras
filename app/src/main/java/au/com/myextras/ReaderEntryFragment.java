package au.com.myextras;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A {@link Fragment} that shows an entry in a {@link WebView}.
 */
public class ReaderEntryFragment extends Fragment {

    public static final String ARG_CURSOR_POSITION = "cursor_position";

    private static String entryHtml;

    private int cursorPosition;

    private DataSetObserver dataSetObserver;

    private long entryId;
    private String entryTitle;
    private String entryLink;
    private String entryContent;
    private long entryPublished;
    private long entryAccessed;

    private WebView descriptionWebView;
    private ProgressBar progressBar;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
    }

    private Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int progress = message.arg1;
            if (progress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    public ReaderEntryFragment() {
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if (!menuVisible && descriptionWebView != null) {
            // reset the scroll state of the #aggregator-content element
            descriptionWebView.reload();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        ReaderActivity readerActivity = (ReaderActivity) getActivity();
        if (readerActivity != null && readerActivity.getActiveCursorPosition() == cursorPosition) {
            markAccessed();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load template
        if (entryHtml == null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.entry)));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                entryHtml = builder.toString();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load entry template", exception);
            }
        }

        cursorPosition = getArguments().getInt(ARG_CURSOR_POSITION);

        setHasOptionsMenu(true);

        final ReaderActivity activity = (ReaderActivity) getActivity();
        Log.i(getClass().getName(), "activity: " + activity);
        activity.adapter.registerDataSetObserver(dataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                Cursor cursor = activity.cursor;
                if (cursor.moveToPosition(cursorPosition)) {
                    entryId = cursor.getLong(ReaderActivity.ENTRY_ID);
                    String newEntryTitle = cursor.getString(ReaderActivity.ENTRY_TITLE);
                    String newEntryLink = cursor.getString(ReaderActivity.ENTRY_LINK);
                    long newEntryPublished = cursor.getLong(ReaderActivity.ENTRY_PUBLISHED);
                    long newEntryAccessed = cursor.getLong(ReaderActivity.ENTRY_ACCESSED);
                    String newEntryContent = cursor.getString(ReaderActivity.ENTRY_CONTENT);

                    if (newEntryPublished != entryPublished
                            || (newEntryTitle != null ? !newEntryTitle.equals(entryTitle) : entryTitle != null)
                            || (newEntryLink != null ? !newEntryLink.equals(entryLink) : entryLink != null)
                            || (newEntryContent != null ? !newEntryContent.equals(entryContent) : entryContent != null)) {
                        entryTitle = newEntryTitle;
                        entryLink = newEntryLink;
                        entryContent = newEntryContent;
                        entryPublished = newEntryPublished;

                        // show new content
                        String content = prepareContent();
                        String base64 = Base64.encodeToString(content.getBytes(), Base64.DEFAULT);
                        descriptionWebView.loadData(base64, "text/html; charset=utf-8", "base64");
                    }

                    if (entryAccessed != newEntryAccessed) {
                        entryAccessed = newEntryAccessed;

                        activity.supportInvalidateOptionsMenu();
                    }

                    if (getUserVisibleHint() && activity.getActiveCursorPosition() == cursorPosition) {
                        markAccessed();
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.reader_entry, container, false);

        progressBar = (ProgressBar) fragmentView.findViewById(R.id.progress);

        WebView descriptionWebView = this.descriptionWebView = (WebView) fragmentView.findViewById(R.id.description);
        descriptionWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressHandler.sendMessage(progressHandler.obtainMessage(0, newProgress, 0));
            }
        });
        descriptionWebView.getSettings().setJavaScriptEnabled(false);
        descriptionWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        descriptionWebView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.background, null));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                descriptionWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }

        registerForContextMenu(descriptionWebView);

        // force load
        ReaderActivity activity = (ReaderActivity) getActivity();
        if (activity.cursor != null) {
            dataSetObserver.onChanged();
        }

        return fragmentView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ReaderActivity activity = (ReaderActivity) getActivity();
        activity.adapter.unregisterDataSetObserver(dataSetObserver);
    }

    private static final int DATE_FORMAT = DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME;

    private String prepareContent() {
        return entryHtml
                .replace("{{ reader.theme }}", "light")
                .replace("{{ entry.link }}", nullSafe(entryLink, ""))
                .replace("{{ entry.date }}", DateUtils.formatDateTime(getActivity(), entryPublished, DATE_FORMAT))
                .replace("{{ entry.title }}", nullSafe(entryTitle, entryTitle))
                .replace("{{ entry.content }}", nullSafe(entryContent, entryContent));
    }

    private String nullSafe(String value, String nullValue) {
        return value != null ? value : nullValue;
    }

    private void markAccessed() {
        if (entryId > 0 && entryAccessed != entryPublished) {
            new SetEntryAccessedTask(getContext()).execute(entryId, entryPublished);
        }
    }

    private static class SetEntryAccessedTask extends AsyncTask<Object, Void, Void> {

        private Context context;

        private SetEntryAccessedTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Object... params) {
            Long entryId = (Long) params[0];
            Long accessed = (Long) params[1];

            ContentResolver contentResolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(Entry.Columns.ACCESSED, accessed);
            contentResolver.update(ContentUris.withAppendedId(Entry.CONTENT_URI, entryId), values, Entry.Columns.ACCESSED + " != " + accessed, null);

            return null;
        }

    }

}