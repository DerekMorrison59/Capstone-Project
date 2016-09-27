package com.derekmorrison.networkmusicplayer.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Derek on 8/22/2016.
 */
public class NMPContract {

    public static final String CONTENT_AUTHORITY = "com.derekmorrison.networkmusicplayer";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_NODE = "node";
    public static final String PATH_SONG = "song";
    public static final String PATH_PLAYLIST = "playlist";
    public static final String PATH_PLAYLIST_ITEM = "playlist_item";
    public static final String PATH_PLAYLIST_ITEM_SONG = "playlist_item_song";

    public static final Uri PLAYLIST_ITEM_SONG_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYLIST_ITEM_SONG).build();

    public static final String PLAYLIST_ITEM_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PLAYLIST_ITEM_SONG;

    /* Inner class that defines the table contents of the node table */
    public static final class NodeEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_NODE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NODE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NODE;

        // Table name
        public static final String TABLE_NAME = "node";

        public static final String COLUMN_NODE_ID = "node_id";
        public static final String COLUMN_PARENT_ID = "parent_id";
        public static final String COLUMN_NODE_NAME = "node_name";
        public static final String COLUMN_FILE_PATH = "file_path";
        public static final String COLUMN_NODE_TYPE = "node_type";
        public static final String COLUMN_NODE_STATUS = "node_status";
        public static final String COLUMN_NODE_IS_FAV = "node_is_fav";

        public static Uri buildNodeUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static long getNodeIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        // Each column in the above table has an index that is listed below
        // If there are any changes to the table these indices must be updated to match
        public static final int COL_ID = 0;
        public static final int COL_NODE_ID = 1;
        public static final int COL_PARENT_ID = 2;
        public static final int COL_NODE_NAME = 3;
        public static final int COL_FILE_PATH = 4;
        public static final int COL_NODE_TYPE = 5;
        public static final int COL_NODE_STATUS = 6;
        public static final int COL_NODE_IS_FAV = 7;
    }

    /* Inner class that defines the table contents of the song table */
    public static final class SongEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SONG).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SONG;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SONG;

        // Table name
        public static final String TABLE_NAME = "song";

        public static final String COLUMN_SONG_ID = "song_id";
        public static final String COLUMN_PARENT_ID = "parent_id";
        public static final String COLUMN_FILE_NAME = "file_name";
        public static final String COLUMN_SONG_TITLE = "title";
        public static final String COLUMN_SONG_ARTIST = "artist";
        public static final String COLUMN_SONG_ALBUM = "album";
        public static final String COLUMN_SONG_GENRE = "genre";
        public static final String COLUMN_SONG_ART_URL = "art_url";
        public static final String COLUMN_SONG_TRACK = "track";
        public static final String COLUMN_SONG_LAST_PLAYED = "last_played";
        public static final String COLUMN_SONG_PLAY_COUNT = "play_count";
        public static final String COLUMN_SONG_DEEP_SCAN = "deep_scan";
        public static final String COLUMN_SONG_DURATION = "duration";
        public static final String COLUMN_SONG_YEAR = "year";
        public static final String COLUMN_SONG_IS_FAV = "is_fav";

        public static Uri buildSongUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static long getSongIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        public static final int COL_ID = 0;
        public static final int COL_SONG_ID = 1;
        public static final int COL_PARENT_ID = 2;
        public static final int COL_FILE_NAME = 3;
        public static final int COL_SONG_TITLE = 4;
        public static final int COL_SONG_ARTIST = 5;
        public static final int COL_SONG_ALBUM = 6;
        public static final int COL_SONG_GENRE = 7;
        public static final int COL_SONG_ART_URL = 8;
        public static final int COL_SONG_TRACK = 9;
        public static final int COL_SONG_LAST_PLAYED = 10;
        public static final int COL_SONG_PLAY_COUNT = 11;
        public static final int COL_SONG_DEEP_SCAN = 12;
        public static final int COL_SONG_DURATION = 13;
        public static final int COL_SONG_YEAR = 14;
        public static final int COL_SONG_IS_FAV = 15;
    }

    /* Inner class that defines the table contents of the playlist table */
    public static final class PlaylistEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYLIST).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PLAYLIST;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PLAYLIST;

        // Table name
        public static final String TABLE_NAME = "playlist";

        public static final String COLUMN_PLAYLIST_ID = "playlist_id";
        public static final String COLUMN_PLAYLIST_NAME = "playlist_name";

        public static Uri buildPlaylistUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static long getPlaylistIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        public static final int COL_ID = 0;
        public static final int COL_PLAYLIST_ID = 1;
        public static final int COL_PLAYLIST_NAME = 2;
    }

    /* Inner class that defines the table contents of the playlist_item table */
    public static final class PlaylistItemEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYLIST_ITEM).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PLAYLIST_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PLAYLIST_ITEM;

        // Table name
        public static final String TABLE_NAME = "playlist_item";

        public static final String COLUMN_PLAYLIST_LIST_ID = "list_id";
        public static final String COLUMN_PLAYLIST_ITEM_SONG_ID = "ext_song_id";

        public static Uri buildPlaylistItemUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

/*
        public static long getPlaylistItemIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
*/
        public static final int COL_ID = 0;
        public static final int COL_PLAYLIST_LIST_ID = 1;
        public static final int COL_PLAYLIST_ITEM_SONG_ID = 2;
    }

}