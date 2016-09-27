package com.derekmorrison.networkmusicplayer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.derekmorrison.networkmusicplayer.data.NMPContract.NodeEntry;
import com.derekmorrison.networkmusicplayer.data.NMPContract.SongEntry;
import com.derekmorrison.networkmusicplayer.data.NMPContract.PlaylistEntry;
import com.derekmorrison.networkmusicplayer.data.NMPContract.PlaylistItemEntry;


/**
 * Created by Derek on 8/22/2016.
 */
public class NMPDbHelper extends SQLiteOpenHelper {

    // constants that define the Node Types
    public final static int NODE_TYPE_FILE = 10;
    public final static int NODE_TYPE_DIRECTORY = 20;
    public final static int NODE_TYPE_SHARE = 30;
    public final static int NODE_TYPE_SERVER = 40;
    public final static int NODE_TYPE_DOMAIN = 50;
    public final static int NODE_TYPE_START = 60;

    // constants that define the Node Status
    public final static int NODE_NOT_SCANNED = 10;
    //public final static int NODE_SCANNING = 20;
    public final static int NODE_SCANNED = 30;


    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "nmp.db";

    public NMPDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold the node info retrieved from scanning the local network.
        final String SQL_CREATE_NODE_TABLE = "CREATE TABLE " + NodeEntry.TABLE_NAME + " (" +
                NodeEntry._ID + " INTEGER PRIMARY KEY," +
                NodeEntry.COLUMN_NODE_ID + " INTEGER UNIQUE NOT NULL, " +
                NodeEntry.COLUMN_PARENT_ID + " INTEGER NOT NULL, " +
                NodeEntry.COLUMN_NODE_NAME + " TEXT NOT NULL, " +
                NodeEntry.COLUMN_FILE_PATH + " TEXT NOT NULL, " +
                NodeEntry.COLUMN_NODE_TYPE + " INTEGER NOT NULL, " +
                NodeEntry.COLUMN_NODE_STATUS + " INTEGER NOT NULL, " +
                NodeEntry.COLUMN_NODE_IS_FAV + " INTEGER NOT NULL " +
                " );";

        final String SQL_CREATE_SONG_TABLE = "CREATE TABLE " + SongEntry.TABLE_NAME + " (" +
                SongEntry._ID + " INTEGER PRIMARY KEY," +
                SongEntry.COLUMN_SONG_ID + " INTEGER UNIQUE NOT NULL, " +
                SongEntry.COLUMN_PARENT_ID + " INTEGER NOT NULL, " +
                SongEntry.COLUMN_FILE_NAME + " TEXT NOT NULL, " +
                SongEntry.COLUMN_SONG_TITLE + " TEXT, " +
                SongEntry.COLUMN_SONG_ARTIST + " TEXT, " +
                SongEntry.COLUMN_SONG_ALBUM + " TEXT, " +
                SongEntry.COLUMN_SONG_GENRE + " TEXT, " +
                SongEntry.COLUMN_SONG_ART_URL + " TEXT, " +
                SongEntry.COLUMN_SONG_TRACK + " INTEGER, " +
                SongEntry.COLUMN_SONG_LAST_PLAYED + " INTEGER, " +
                SongEntry.COLUMN_SONG_PLAY_COUNT + " INTEGER, " +
                SongEntry.COLUMN_SONG_DEEP_SCAN + " INTEGER, " +
                SongEntry.COLUMN_SONG_DURATION + " INTEGER, " +
                SongEntry.COLUMN_SONG_YEAR + " INTEGER, " +
                SongEntry.COLUMN_SONG_IS_FAV + " INTEGER " +
                " );";

        final String SQL_CREATE_PLAYLIST_TABLE = "CREATE TABLE " + PlaylistEntry.TABLE_NAME + " (" +
                PlaylistEntry._ID + " INTEGER PRIMARY KEY," +
                PlaylistEntry.COLUMN_PLAYLIST_ID + " INTEGER UNIQUE NOT NULL, " +
                PlaylistEntry.COLUMN_PLAYLIST_NAME + " TEXT UNIQUE NOT NULL " +
                " );";

        final String SQL_CREATE_PLAYLIST_ITEM_TABLE = "CREATE TABLE " + PlaylistItemEntry.TABLE_NAME + " (" +
                PlaylistItemEntry._ID + " INTEGER PRIMARY KEY," +
                PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + " INTEGER NOT NULL, " +
                PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID + " INTEGER NOT NULL " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_NODE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_SONG_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PLAYLIST_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PLAYLIST_ITEM_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NodeEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SongEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PlaylistEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PlaylistItemEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
