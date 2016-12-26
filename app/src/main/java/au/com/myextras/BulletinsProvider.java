package au.com.myextras;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.NotificationCompat;

import com.tughi.android.database.sqlite.DatabaseOpenHelper;

public class BulletinsProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".bulletins";

    private static final int URI_ENTRIES = 1;
    private static final int URI_ENTRY = 2;

    private static final String TABLE_ENTRIES = "entries";

    private UriMatcher uriMatcher;

    private SQLiteOpenHelper sqlite;

    private Context context;
    private ContentResolver contentResolver;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "entries", URI_ENTRIES);
        uriMatcher.addURI(AUTHORITY, "entries/#", URI_ENTRY);

        context = getContext();
        assert context != null;

        sqlite = new DatabaseOpenHelper(context, "bulletins.db", 1);

        contentResolver = context.getContentResolver();

        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case URI_ENTRIES:
                return queryEntries(projection, selection, selectionArgs, sortOrder);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private Cursor queryEntries(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = sqlite.getReadableDatabase();

        if (selection == null) {
            selection = Entry.Columns.BULLETIN + " = ?";
        } else {
            selection = Entry.Columns.BULLETIN + " = ? AND (" + selection + ")";
        }

        if (selectionArgs == null || selectionArgs.length == 0) {
            selectionArgs = new String[] { Preferences.getCode(context) };
        } else {
            String[] newSelectionArgs = new String[selectionArgs.length + 1];
            newSelectionArgs[0] = Preferences.getCode(context);
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
            selectionArgs = newSelectionArgs;
        }

        Cursor cursor = database.query(TABLE_ENTRIES, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(contentResolver, Entry.CONTENT_URI);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {
            case URI_ENTRIES:
                return insertEntry(values);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private Uri insertEntry(ContentValues values) {
        SQLiteDatabase database = sqlite.getWritableDatabase();

        try {
            database.insertOrThrow(TABLE_ENTRIES, null, values);
        } catch (SQLiteConstraintException exception) {
            String selection = Entry.Columns.BULLETIN + " = ? AND " + Entry.Columns.GUID + " = ?";
            String[] selectionArgs = { values.getAsString(Entry.Columns.BULLETIN), values.getAsString(Entry.Columns.GUID) };
            database.update(TABLE_ENTRIES, values, selection, selectionArgs);
        }

        updateNotification(database);

        return null;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_ENTRIES:
                return updateEntries(values, selection, selectionArgs);
            case URI_ENTRY:
                if (selection != null) {
                    selection = Entry.Columns.ID + " = " + uri.getLastPathSegment() + " AND (" + selection + ")";
                } else {
                    selection = Entry.Columns.ID + " = " + uri.getLastPathSegment();
                }
                return updateEntries(values, selection, selectionArgs);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private int updateEntries(ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = sqlite.getWritableDatabase();

        int affectedRows;
        if (values == null) {
            SQLiteStatement statement = database.compileStatement("UPDATE " + TABLE_ENTRIES + " SET " + Entry.Columns.ACCESSED + " = " + Entry.Columns.PUBLISHED + " WHERE " + Entry.Columns.BULLETIN + " = ? AND " + Entry.Columns.ACCESSED + " != " + Entry.Columns.PUBLISHED);
            statement.bindAllArgsAsStrings(new String[] { Preferences.getCode(context) });
            affectedRows = statement.executeUpdateDelete();
        } else {
            affectedRows = database.update(TABLE_ENTRIES, values, selection, selectionArgs);
        }

        if (affectedRows > 0) {
            contentResolver.notifyChange(Entry.CONTENT_URI, null);
        }

        updateNotification(database);

        return affectedRows;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private static final String[] NOTIFICATION_ENTRY_PROJECTION = {
            Entry.Columns.ID,
            Entry.Columns.TITLE,
            Entry.Columns.PUBLISHED,
            Entry.Columns.ACCESSED,
            Entry.Columns.IMPORTANT,
    };
    private static final String NOTIFICATION_ENTRY_SELECTION = Entry.Columns.BULLETIN + " = ?";
    private static final String NOTIFICATION_ENTRY_SORT_ORDER = Entry.Columns.PUBLISHED + " DESC";
    private static final int NOTIFICATION_ENTRY_ID = 0;
    private static final int NOTIFICATION_ENTRY_TITLE = 1;
    private static final int NOTIFICATION_ENTRY_PUBLISHED = 2;
    private static final int NOTIFICATION_ENTRY_ACCESSED = 3;
    private static final int NOTIFICATION_ENTRY_IMPORTANT = 4;

    @WorkerThread
    public void updateNotification(SQLiteDatabase database) {
        String code = Preferences.getCode(context);
        if (code != null) {
            String[] selectionArgs = { code };
            Cursor cursor = database.query(TABLE_ENTRIES, NOTIFICATION_ENTRY_PROJECTION, NOTIFICATION_ENTRY_SELECTION, selectionArgs, null, null, NOTIFICATION_ENTRY_SORT_ORDER, "1");

            if (cursor != null) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (cursor.moveToFirst()
                        && cursor.getLong(NOTIFICATION_ENTRY_PUBLISHED) != cursor.getLong(NOTIFICATION_ENTRY_ACCESSED)
                        && cursor.getInt(NOTIFICATION_ENTRY_IMPORTANT) != 0) {
                    Uri uri = ContentUris.withAppendedId(Entry.CONTENT_URI, cursor.getLong(NOTIFICATION_ENTRY_ID));

                    Intent intent = new Intent(context, EntriesActivity.class);
                    intent.setData(uri);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Notification notification = new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(Preferences.getTitle(context))
                            .setContentText(cursor.getString(NOTIFICATION_ENTRY_TITLE))
                            .setContentIntent(pendingIntent)
                            .setColor(ResourcesCompat.getColor(context.getResources(), R.color.branding_blue, null))
                            .setAutoCancel(true)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .build();

                    notificationManager.notify(R.id.notification, notification);
                } else {
                    notificationManager.cancel(R.id.notification);
                }

                cursor.close();
            }
        }
    }


}
