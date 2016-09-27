package com.derekmorrison.networkmusicplayer.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.ui.SongNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Derek on 9/15/2016.
 *
 * Container for 1 playlist that includes:
 *
 * playlist name
 * playlist ID
 * a list of songs (SongDbId) that are contained in the playlist
 *
 * Load Playlist from Db
 * Save Playlist to Db
 * Add Playlist to the SongList
 *
 */
public class Playlist {

    private String TAG = "Playlist";
    private String mPlaylistName = "Temporary";
    private int mPlaylistId = -1;
    private List<SongNode> mListMember = new ArrayList<>();
    private boolean dbLoaded = false;

    public Playlist(){

    }

    public String getPlayListName() {
        return mPlaylistName;
    }

    public void setPlayListName(String mPlayListName) {
        this.mPlaylistName = mPlayListName;
    }

    public int getPlaylistId() {
        return mPlaylistId;
    }

    public void setPlaylistId(int playlistId, Context context) {
        this.mPlaylistId = playlistId;
        loadFromDb(context, playlistId);
    }

    public List<SongNode> getListMembers() {
        return mListMember;
    }

    public void clear() {
        if (null != mListMember && 0 < mListMember.size()) {
            mListMember.clear();
            dbLoaded = false;
        }
        mPlaylistName = "cleared";
        mPlaylistId = -1;
    }

    // the int value is stored as a long inside the MediaMetadataCompat
    // so it is safe to cast it
//    public void addSong(long longSongId, Context context) {
//        addSong((int) longSongId, context);
//    }

    public void addSong(int songId, Context context) {
        addSong(songId, "", "", 1, context);
    }

    // adds new songId to the Playlist and returns it's position
    // does not allow duplicate songId entries
    public int addSong(int songId, String artist, String title, int albumId, Context context) {
        Log.d(TAG, "addSong song ID: " + songId + " artist: " + artist + " title: " + title);

        // do not allow a duplicate
        if (false == mListMember.contains(songId)) {
            SongNode s = new SongNode();
            s.setId(songId);
            s.setArtist(artist);
            s.setTitle(title);
            s.setTrack(albumId);

            mListMember.add(s);
            saveToDb(context);
        }

        return mListMember.size()-1;
    }

    public void deleteSong(int position) {

        int size = mListMember.size();
        if (size > 0 &&  size > position) {
            mListMember.remove(position);
        }
    }

    public void loadFromDb(Context context, int playlistId) {
        Log.d(TAG, "loadFromDb playlist ID: " + playlistId);

        // get the songs in this playlist
        String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + "=?";
        String[] args = { String.valueOf(playlistId) };

        Cursor playlistCursor = context.getContentResolver().query(
                NMPContract.PlaylistEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        String playlistName = "No Name " + String.valueOf(playlistId);
        if (null != playlistCursor && true == playlistCursor.moveToFirst()) {
            playlistName = playlistCursor.getString(NMPContract.PlaylistEntry.COL_PLAYLIST_NAME);
        }
        Log.d(TAG, "loadFromDb playlist Name: " + playlistName);

        playlistCursor.close();
        playlistCursor = null;

        loadFromDb(context, playlistId, playlistName);
    }

    public void loadFromDb(Context context, int playlistId, String playlistName) {
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;

        if (true == dbLoaded) {
            Log.d(TAG, "loadFromDb already loaded! ! !");
            return;
        }

        Log.d(TAG, "loadFromDb playlist ID: " + playlistId + " name: " + playlistName);

        // get the songs in this playlist
        String selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";
        String[] args = { String.valueOf(playlistId) };

        Cursor songCursor = context.getContentResolver().query(
                NMPContract.PlaylistItemEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        if (null == songCursor) {
            // todo complain
            return;
        }

        mListMember.clear();
        while (songCursor.moveToNext()) {
            int songDbId = songCursor.getInt(NMPContract.PlaylistItemEntry.COL_PLAYLIST_ITEM_SONG_ID);
            SongNode s = new SongNode();
            s.setId(songDbId);
            mListMember.add(s);
            Log.d(TAG, "loadFromDb - retrieved songDbId: " + songDbId);
        }

        loadDetailsFromDb(context);
    }

    public void saveToDb(Context context) {

        // if playlist is less than 1 then get a new playlist ID
        if (1 > mPlaylistId) {
            mPlaylistId = SharedPrefUtils.getInstance().getNextPlaylistId();
        }

        // if this list exists then update the PLAYLIST record, delete any existing members
        // and save the new list

        String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + "=?";
        String[] args = {String.valueOf(mPlaylistId)};
        Cursor idCursor = context.getContentResolver().query(
                NMPContract.PlaylistEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        ContentValues listValues = new ContentValues();
        listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, mPlaylistName);

        Log.d(TAG, "saveToDb - Trying to update PlayListEntry with new NAME: " + mPlaylistName);

        if (null != idCursor && idCursor.moveToFirst() ) {
            // the playlist exists and must be updated to ensure the name is correct
            context.getContentResolver().update(
                NMPContract.PlaylistEntry.CONTENT_URI,
                    listValues,
                selection,
                args
            );

            // remove all existing members (song entries) from the playlist
            selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";
            args[0] = String.valueOf(mPlaylistId);
            context.getContentResolver().delete(
                    NMPContract.PlaylistItemEntry.CONTENT_URI,
                    selection,
                    args
                    );
        } else {
            // the playlist does not exist and the new Id and Name must be inserted
            listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID, mPlaylistId);
            context.getContentResolver().insert(
                    NMPContract.PlaylistEntry.CONTENT_URI,
                    listValues
            );
        }

        if (null != idCursor) {
            idCursor.close();
            idCursor = null;
        }

        // loop through all songs in mListMember
        List<ContentValues> songItems = new ArrayList<ContentValues>();

        for (SongNode songNode : mListMember) {
            ContentValues songValues = new ContentValues();
            songValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, mPlaylistId);
            songValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, songNode.getId());
            songItems.add(songValues);
        }

        int songCount = songItems.size();

        if (songCount > 0) {
            ContentValues[] songArray = new ContentValues[songCount];
            songArray = songItems.toArray(songArray);
            context.getContentResolver().bulkInsert(
                    NMPContract.PlaylistItemEntry.CONTENT_URI,
                    songArray
            );
            Log.d(TAG, "Saved Playlist: " + mPlaylistName + " songs contained: " + songCount);
        }

    }

    public void loadDetailsFromDb(Context context) {

        String artist = "";
        String title = "";
        String album = "";

        // go through the list of SongNodes and use the ID to get the song record from the database
        for (SongNode songNode : mListMember) {

            // get the song details
            String selection = NMPContract.SongEntry.COLUMN_SONG_ID + "=?";
            String[] args = {String.valueOf(songNode.getId())};

            Cursor songCursor = context.getContentResolver().query(
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    selection,
                    args,
                    null
            );

            if (null != songCursor && songCursor.moveToFirst()) {
                artist = songCursor.getString(NMPContract.SongEntry.COL_SONG_ARTIST);
                songNode.setArtist(artist);
                title = songCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE);

                // if this song has not been deep scanned yet then show the file name
                if (null == title) {
                    title = songCursor.getString(NMPContract.SongEntry.COL_FILE_NAME);
                }
                //songNode.setFileName();

                songNode.setTitle(title);
                album = songCursor.getString(NMPContract.SongEntry.COL_SONG_ALBUM);
                songNode.setAlbum(album);
                songNode.setTrack(songCursor.getInt(NMPContract.SongEntry.COL_SONG_TRACK));
                songNode.setArtUrl(songCursor.getString(NMPContract.SongEntry.COL_SONG_ART_URL));
                songNode.setDeepScan(songCursor.getInt(NMPContract.SongEntry.COL_SONG_DEEP_SCAN));

                Log.d(TAG, "loadDetailsFromDb adding song: " + title + " by: " + artist);

                songCursor.close();
            } else {
                // todo - complain can't find song in DB
            }

        }
        dbLoaded = true;
    }
/*
    public void loadDetailsFromDb(Context context, int playlistId, String playlistName) {
        Log.d(TAG, "loadDetailsFromDb playlist ID: " + playlistId + " name: " + playlistName);

        // has the list been loaded from the database?
        if (1 > mListMember.size()) {
            loadFromDb(context, playlistId, playlistName);
        }

        // go through the list of SongNodes and use the ID to get the song record from the database
        for (SongNode songNode : mListMember) {

            // get the song details
            String selection = NMPContract.SongEntry.COLUMN_SONG_ID + "=?";
            String[] args = {String.valueOf(songNode.getId())};

            Cursor songCursor = context.getContentResolver().query(
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    selection,
                    args,
                    null
            );

            if (null != songCursor && songCursor.moveToFirst()) {
                songNode.setArtist(songCursor.getString(NMPContract.SongEntry.COL_SONG_ARTIST));
                songNode.setTitle(songCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE));
                songNode.setAlbum(songCursor.getString(NMPContract.SongEntry.COL_SONG_ALBUM));

                songCursor.close();
            } else {
                // todo - complain can't find song in DB
            }

        }
    }
*/

}
