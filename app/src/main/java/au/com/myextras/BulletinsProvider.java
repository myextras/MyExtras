package au.com.myextras;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tughi.android.database.sqlite.DatabaseOpenHelper;

public class BulletinsProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".bulletins";

    private static final int URI_BULLETINS = 1;
    private static final int URI_BULLETIN = 2;

    private static final String TABLE_BULLETINS = "bulletins";

    private UriMatcher uriMatcher;

    private SQLiteOpenHelper sqlite;

    private ContentResolver contentResolver;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "bulletins", URI_BULLETINS);
        uriMatcher.addURI(AUTHORITY, "bulletins/#", URI_BULLETIN);

        Context context = getContext();
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
            case URI_BULLETINS:
                return queryBulletins(projection, selection, selectionArgs, sortOrder);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private Cursor queryBulletins(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = sqlite.getReadableDatabase();

        Cursor cursor = database.query(TABLE_BULLETINS, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(contentResolver, Bulletin.CONTENT_URI);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {
            case URI_BULLETINS:
                return insertBulletin(values);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private Uri insertBulletin(ContentValues values) {
        SQLiteDatabase database = sqlite.getWritableDatabase();

        try {
            database.insertOrThrow(TABLE_BULLETINS, null, values);
        } catch (SQLiteConstraintException exception) {
            String selection = Bulletin.Column.GUID + " = ?";
            String[] selectionArgs = { values.getAsString(Bulletin.Column.GUID) };
            database.update(TABLE_BULLETINS, values, selection, selectionArgs);
        }

        return null;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_BULLETINS:
                return updateBulletins(values, selection, selectionArgs);
        }

        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

    private int updateBulletins(ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = sqlite.getWritableDatabase();

        int affectedRows;
        if (values == null) {
            SQLiteStatement statement = database.compileStatement("UPDATE " + TABLE_BULLETINS + " SET " + Bulletin.Column.ACCESSED + " = " + Bulletin.Column.PUBLISHED + " WHERE " + Bulletin.Column.ACCESSED + " != " + Bulletin.Column.PUBLISHED);
            affectedRows = statement.executeUpdateDelete();
        } else {
            affectedRows = database.update(TABLE_BULLETINS, values, selection, selectionArgs);
        }

        if (affectedRows > 0) {
            contentResolver.notifyChange(Bulletin.CONTENT_URI, null);
        }

        return affectedRows;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported URI: " + uri);
    }

}
