package com.derekmorrison.networkmusicplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * Created by Derek on 9/9/2016.
 */
public class SharedPrefUtils {
    private final String LOG = "SharedPrefUtils";
    private static SharedPrefUtils instance = null;
    private static Context mContext = null;

    private String PLAYLIST_ID = "playlist_id";
    private String NEXT_PLAYLIST_ID = "next_playlist_id";

    private String SYSTEM_SETTINGS = "system_settings";
    private String FIRST_RUN = "first_run";
    private String LAST_SCREEN = "last_screen";
    private String LAST_SELECTED_PLAYLIST = "last_playlist";
    private String LAST_SELECTED_PLAYLIST_NAME = "last_playlist_name";
    private String FOLDERS_SELECTED = "folders_selected";
    private String FILE_LIST_POSITION = "file_list_position";
    private String DIR_LIST_POSITION = "dir_list_position";
    private String LAST_SONG = "last_song";

    private String SCAN_STARTS = "scan_starts";
    private String SCAN_ENDS = "scan_ends";
    private String SCAN_START_TIME = "scan_start_time";
    private String SCAN_END_TIME = "scan_end_time";

    private String SELECTED_PLAYLIST_ID = "selected_playlist_id";
    private String EDITING_PLAYLIST_ID = "editing_playlist_id";

    private SharedPrefUtils() {}

    public static SharedPrefUtils getInstance() {
        if(instance == null)
            instance = new SharedPrefUtils();

        return instance;
    }

    public void init(Context context) {
        mContext = context;
    }

    public int getNextPlaylistId() {
        int playlistId;
        SharedPreferences sp = mContext.getSharedPreferences(PLAYLIST_ID, Context.MODE_PRIVATE);
        playlistId = sp.getInt(NEXT_PLAYLIST_ID, -1);

        // check for the highest playlist Id in the database
        if (-1 == playlistId) {
//            Log.d(LOG, "getNextPlaylistId - no ID stored in Shared Prefs");

            String[] max = {" MAX(" + NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + ") "};

            Cursor directoryCursor = mContext.getContentResolver().query(
                    NMPContract.PlaylistEntry.CONTENT_URI,
                    max,
                    null,
                    null,
                    null
            );

            // if there are no playlists then start at 0 and move to 1 below
            playlistId = 0;

            if (null != directoryCursor && directoryCursor.getCount() > 0 && directoryCursor.moveToFirst()) {
                // the result is a single value - 1 row & 1 column
                playlistId = directoryCursor.getInt(0);
//                Log.d(LOG, "getNextPlaylistId - the highest PLAYLIST_ID found: " + playlistId);
            }

            // move to the next available ID number
            playlistId++;

            if (null != directoryCursor) {
                directoryCursor.close();
            }


        }

        // save the next ID back into shared prefs
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(NEXT_PLAYLIST_ID, playlistId+1);
        editor.apply();

//        Log.d(LOG, "getNextPlaylistId - PLAYLIST_ID returned: " + playlistId);

        return playlistId;
    }

    public boolean isFirstTime() {
        boolean isFirst;
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        isFirst = sp.getBoolean(FIRST_RUN, true);
        return isFirst;
    }

    public void firstTimeCompleted() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putBoolean(FIRST_RUN, false);
        editor.apply();
    }

    // LAST_SELECTED_PLAYLIST
    public int getLastSong() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(LAST_SONG, -1);
    }

    public void saveLastSong(int songDbId) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(LAST_SONG, songDbId);
        editor.apply();
    }

    public int getLastPlaylistId() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(LAST_SELECTED_PLAYLIST, -1);
    }

    public void saveLastPlaylistId(int playlistId) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(LAST_SELECTED_PLAYLIST, playlistId);
        editor.apply();
    }

    public String getLastPlaylistName() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getString(LAST_SELECTED_PLAYLIST_NAME, "unknown");
    }

    public void saveLastPlaylistName(String playlistName) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putString(LAST_SELECTED_PLAYLIST_NAME, playlistName);
        editor.apply();
    }

    public void saveLastPlaylist(int playlistId, String playlistName) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(LAST_SELECTED_PLAYLIST, playlistId);
        editor.putString(LAST_SELECTED_PLAYLIST_NAME, playlistName);
        editor.apply();
    }

    public int getEditingPlaylistId() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(EDITING_PLAYLIST_ID, -1);
    }

    public void saveEditingPlaylistId(int playlistId) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(EDITING_PLAYLIST_ID, playlistId);
        editor.apply();
    }

    public boolean getFoldersDisplayed() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getBoolean(FOLDERS_SELECTED, true);
    }

    public void saveFoldersDisplayed(boolean foldersDisplayed) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putBoolean(FOLDERS_SELECTED, foldersDisplayed);
        editor.apply();
    }

    public int getFileListPosition() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(FILE_LIST_POSITION, 0);
    }

    public void saveFileListPosition(int fileListPosition) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(FILE_LIST_POSITION, fileListPosition);
        editor.apply();
    }

    public int getDirListPosition() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(DIR_LIST_POSITION, 0);
    }

    public void saveDirListPosition(int dirListPosition) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(DIR_LIST_POSITION, dirListPosition);
        editor.apply();
    }

    public int getScanStarts() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(SCAN_STARTS, 0);
    }

    public void incrementScanStarts() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        int starts = sp.getInt(SCAN_STARTS, 0);
        starts++;
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(SCAN_STARTS, starts);
        editor.apply();
    }

    public int getScanEnds() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(SCAN_ENDS, 0);
    }

    public void incrementScanEnds() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        int ends = sp.getInt(SCAN_ENDS, 0);
        ends++;
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(SCAN_ENDS, ends);
        editor.apply();
    }

    public void setScanStartTime(long start) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putLong(SCAN_START_TIME, start);
        editor.apply();
    }

    public long getScanStartTime() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getLong(SCAN_START_TIME, 0);
    }

    public void setScanEndTime(long end) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putLong(SCAN_END_TIME, end);
        editor.apply();
    }

    public long getScanEndTime() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getLong(SCAN_END_TIME, 0);
    }


    public void setPlaylistId(int playlistId) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(SELECTED_PLAYLIST_ID, playlistId);
        editor.apply();
    }

    public int getPlaylistId() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(SELECTED_PLAYLIST_ID, 0);
    }

    public void setLastScreen(int screenId) {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  sp.edit();
        editor.putInt(LAST_SCREEN, screenId);
        editor.apply();
    }

    public int getLastScreen() {
        SharedPreferences sp = mContext.getSharedPreferences(SYSTEM_SETTINGS, Context.MODE_PRIVATE);
        return sp.getInt(LAST_SCREEN, 0);
    }

}
