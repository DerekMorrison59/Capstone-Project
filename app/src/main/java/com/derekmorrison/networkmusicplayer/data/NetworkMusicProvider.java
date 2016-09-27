package com.derekmorrison.networkmusicplayer.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by Derek on 8/22/2016.
 *
 * Patterned after the Sunshine app
 */
public class NetworkMusicProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private NMPDbHelper mOpenHelper;

    static final int NODE = 100;
    static final int SONG = 200;
    static final int PLAYLIST = 300;
    static final int PLAYLIST_ITEM = 400;
    static final int PLAYLIST_ITEM_SONG = 500;


    static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = NMPContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, NMPContract.PATH_NODE, NODE);
        matcher.addURI(authority, NMPContract.PATH_SONG, SONG);
        matcher.addURI(authority, NMPContract.PATH_PLAYLIST, PLAYLIST);
        matcher.addURI(authority, NMPContract.PATH_PLAYLIST_ITEM, PLAYLIST_ITEM);
        matcher.addURI(authority, NMPContract.PATH_PLAYLIST_ITEM_SONG, PLAYLIST_ITEM_SONG);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new NMPDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case NODE:
                return NMPContract.NodeEntry.CONTENT_ITEM_TYPE;
            case SONG:
                return NMPContract.SongEntry.CONTENT_TYPE;
            case PLAYLIST:
                return NMPContract.PlaylistEntry.CONTENT_TYPE;
            case PLAYLIST_ITEM:
                return NMPContract.PlaylistItemEntry.CONTENT_TYPE;
            case PLAYLIST_ITEM_SONG:
                return NMPContract.PLAYLIST_ITEM_CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case NODE:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        NMPContract.NodeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case SONG: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        NMPContract.SongEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case PLAYLIST: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        NMPContract.PlaylistEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case PLAYLIST_ITEM: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        NMPContract.PlaylistItemEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case PLAYLIST_ITEM_SONG: {

                String rawQuery = "SELECT * FROM " + NMPContract.PlaylistItemEntry.TABLE_NAME
                        + " INNER JOIN " + NMPContract.SongEntry.TABLE_NAME
                        + " ON " + NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID
                        + " = " + NMPContract.SongEntry.COLUMN_SONG_ID
                        + " WHERE " + NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + " = "
                        +  selection;

                final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                retCursor = db.rawQuery(
                        rawQuery,
                        null
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case NODE: {
                long _id = db.insert(NMPContract.NodeEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = NMPContract.NodeEntry.buildNodeUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case SONG: {
                long _id = db.insert(NMPContract.SongEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = NMPContract.SongEntry.buildSongUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case PLAYLIST: {
                long _id = db.insert(NMPContract.PlaylistEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = NMPContract.PlaylistEntry.buildPlaylistUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case PLAYLIST_ITEM: {

                // do not insert a song into a playlist a second time
                int song_id = values.getAsInteger(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);
                int playlist_id = values.getAsInteger(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID);
                returnUri = null;

                if (false == songExists(db, playlist_id, song_id)) {


                    long _id = db.insert(NMPContract.PlaylistItemEntry.TABLE_NAME, null, values);
                    if (_id > 0)
                        returnUri = NMPContract.PlaylistItemEntry.buildPlaylistItemUri(_id);
                    else
                        throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case NODE:
                rowsDeleted = db.delete(
                        NMPContract.NodeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case SONG:
                rowsDeleted = db.delete(
                        NMPContract.SongEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PLAYLIST:
                rowsDeleted = db.delete(
                        NMPContract.PlaylistEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PLAYLIST_ITEM:
                rowsDeleted = db.delete(
                        NMPContract.PlaylistItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case NODE:
                rowsUpdated = db.update(
                        NMPContract.NodeEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case SONG:
                rowsUpdated = db.update(
                        NMPContract.SongEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case PLAYLIST:
                rowsUpdated = db.update(
                        NMPContract.PlaylistEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case PLAYLIST_ITEM:
                rowsUpdated = db.update(
                        NMPContract.PlaylistItemEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    private boolean dirExists(SQLiteDatabase db, int node_id){
        boolean exists = false;

        String dirColumns = NMPContract.NodeEntry.COLUMN_NODE_ID + "=?";
        String[] dirIds = {String.valueOf(node_id)};
        Cursor dirCursor = db.query(
                NMPContract.NodeEntry.TABLE_NAME,
                null,
                dirColumns,
                dirIds,
                null,
                null,
                null
        );

        // todo what if the count is > 1
        if (null != dirCursor && dirCursor.getCount() > 0) {
            exists = true;
        }
        if (null != dirCursor) {
            dirCursor.close();
        }

        return exists;
    }

    private boolean fileExists(SQLiteDatabase db, int song_id){
        boolean exists = false;

        String dirColumns = NMPContract.SongEntry.COLUMN_SONG_ID + "=?";
        String[] dirIds = {String.valueOf(song_id)};
        Cursor fileCursor = db.query(
                NMPContract.SongEntry.TABLE_NAME,
                null,
                dirColumns,
                dirIds,
                null,
                null,
                null
        );

        // todo what if the count is > 1
        if (null != fileCursor && fileCursor.getCount() > 0) {
            exists = true;
        }
        if (null != fileCursor) {
            fileCursor.close();
        }

        return exists;
    }

    private boolean songExists(SQLiteDatabase db, int playlist_id, int song_id){
        boolean exists = false;

        String dirColumns = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID + "=? " +
                " AND " + NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";
        String[] dirIds = {String.valueOf(song_id), String.valueOf(playlist_id)};
        Cursor fileCursor = db.query(
                NMPContract.PlaylistItemEntry.TABLE_NAME,
                null,
                dirColumns,
                dirIds,
                null,
                null,
                null
        );

        // todo what if the count is > 1
        if (null != fileCursor && fileCursor.getCount() > 0) {
            exists = true;
        }
        if (null != fileCursor) {
            fileCursor.close();
        }

        return exists;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount = 0;

        switch (match) {
            case NODE:
                db.beginTransaction();

                try {
                    for (ContentValues value : values) {
                        int node_id = value.getAsInteger(NMPContract.NodeEntry.COLUMN_NODE_ID);

                        if (false == dirExists(db, node_id)) {
                            long _id = db.insert(NMPContract.NodeEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case SONG:
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        int song_id = value.getAsInteger(NMPContract.SongEntry.COLUMN_SONG_ID);

                        if (false == fileExists(db, song_id)) {
                            long _id = db.insert(NMPContract.SongEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case PLAYLIST_ITEM:
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        int song_id = value.getAsInteger(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);
                        int playlist_id = value.getAsInteger(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID);

                        if (false == songExists(db, playlist_id, song_id)) {
                            long _id = db.insert(NMPContract.PlaylistItemEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                return super.bulkInsert(uri, values);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }


    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

}
