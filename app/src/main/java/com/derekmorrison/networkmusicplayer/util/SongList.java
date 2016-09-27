package com.derekmorrison.networkmusicplayer.util;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPContract.PlaylistItemEntry;
import com.derekmorrison.networkmusicplayer.playback.MediaNotificationManager;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;
import com.derekmorrison.networkmusicplayer.sync.ScanFileService;
import com.derekmorrison.networkmusicplayer.ui.GlobalApp;
import com.derekmorrison.networkmusicplayer.ui.SongNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.derekmorrison.networkmusicplayer.data.NMPContract.*;

/**
 * Created by Derek on 9/1/2016.
 */
public class SongList {
    private String TAG = "SongList";

    private ArrayList<MediaMetadataCompat> songList = new ArrayList<>();
    private int currentSong = 0;
    private ChangeListener mChangeListener = null;
    private int mPlaylistId;
    private String mPlaylistName;
    private Context mContext;

    //private Playlist mPlaylist = new Playlist();

    public interface ChangeListener {
        void onItemChanged();
    }

    public void setOnItemChangedListener(ChangeListener changeListener) {
        mChangeListener = changeListener;
    }

    public SongList(){}

    public void setPlaylistId(Context context, int newPlaylistId, String newPlaylistName) {

        // don't reload the same list
        if (newPlaylistId == mPlaylistId) {
//            Log.d(TAG, "setPlaylistId - playlist already loaded, don't do it again");
            return;
        }
        mContext = context;
        mPlaylistId = newPlaylistId;
        mPlaylistName = newPlaylistName;
        loadPlaylist();
        SharedPrefUtils.getInstance().saveLastSong(currentSong);
    }

    public int getPlaylistId() {
        return mPlaylistId;
    }

    public void relaodPlaylist() {
        loadPlaylist();
    }

    private void loadPlaylist() {

        songList.clear();
        currentSong = 0;

//        Log.d(TAG, "loadNewPlaylist Playlist ID: " + mPlaylistId);

        if (0 > mPlaylistId) {
            Log.d(TAG, "loadNewPlaylist Cannot load Playlist: " + mPlaylistId);
            return;
        }

        // get all the members of the playlist
        Cursor songCursor = mContext.getContentResolver().query(
                PLAYLIST_ITEM_SONG_CONTENT_URI,
                null,
                String.valueOf(mPlaylistId),
                null,
                null
        );

        // get the column numbers from the cursor
        // COLUMN_SONG_TRACK, albumId
        int col_songId = songCursor.getColumnIndex(PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);
        int col_songFile = songCursor.getColumnIndex(SongEntry.COLUMN_FILE_NAME);
        int col_deepScan = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_DEEP_SCAN);
        int col_artist = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_ARTIST);
        int col_title = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_TITLE);
        int col_album = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_ALBUM);
        int col_track = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_TRACK);
        int col_albumId = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_TRACK);
        int col_duration = songCursor.getColumnIndex(SongEntry.COLUMN_SONG_DURATION);
        int col_parent = songCursor.getColumnIndex(SongEntry.COLUMN_PARENT_ID);

        int deepScan;
        int songDbId;
        String filename;

        if (null != songCursor && 0 < songCursor.getCount()) {

            // METADATA_KEY_DISC_NUMBER stores the songDbId
            // METADATA_KEY_NUM_TRACKS  stores the AlbumId to get artwork

            // /storage/emulated/0/Music/NMP/Father and Son.mp3
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            //File newDir = new File(path.toString() + "/NMP");

            String dirPath = path.getAbsolutePath() + "/NMP/";
            String filePath; // = dirPath + songCursor.getString(col_songFile);

            while (songCursor.moveToNext()){

                // get the deepScan flag and if it is false then call for the FileCopy
                deepScan = songCursor.getInt(col_deepScan);
                songDbId = songCursor.getInt(col_songId);
                filename = songCursor.getString(col_songFile);
//                Log.d(TAG, "loadNewPlaylist songDb ID: " + songDbId + " filename: " + filename + " deepScan: " + deepScan);

                if (0 == deepScan) {
                    int parentId = songCursor.getInt(col_parent);
                    String selection = NodeEntry.COLUMN_NODE_ID + "=?";
                    String[] args = {String.valueOf(parentId)};
                    Cursor nodeCursor = mContext.getContentResolver().query(
                            NodeEntry.CONTENT_URI,
                            null,
                            selection,
                            args,
                            null
                    );

                    if (null != nodeCursor && nodeCursor.moveToFirst()) {
                        String parentPath = nodeCursor.getString(NodeEntry.COL_FILE_PATH);
                        CopyFileService.startCopyFile(mContext, songDbId, parentPath + filename, SongListHelper.SONG_LIST_CURRENT);
                    }

                    if (null != nodeCursor) {
                        nodeCursor.close();
                        nodeCursor = null;
                    }
                }

                filePath = dirPath + filename;

//                Log.d(TAG, "loadPlaylist filePath: " + filePath
//                        + " title: " + songCursor.getString(col_title)
//                        + " songId: " + songCursor.getInt(col_songId));

                @SuppressWarnings("WrongConstant")
                MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                        .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, filePath)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, songCursor.getString(col_album))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songCursor.getString(col_artist))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songCursor.getString(col_title))
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, songCursor.getInt(col_albumId))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, songDbId)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, songCursor.getInt(col_duration))
                        .build();

                songList.add(newTrack);
            }
        }

        if (null != songCursor) {
            songCursor.close();
            songCursor = null;
        }

//        Intent metaDataUpdatedIntent = new Intent(MediaNotificationManager.ACTION_METADATA_UPDATED);
//        metaDataUpdatedIntent.putExtra("SOURCE", " NEW SongList ");
//        mContext.sendBroadcast(metaDataUpdatedIntent);

    }

    public void addSong(int songDbId){
        SongParams songParams = new SongParams(songDbId);
        LoadNewSongTask lnst = new LoadNewSongTask(mContext);
        lnst.execute(songParams);
    }






    private static class SongParams {
        int mSongId;

        SongParams(int songId) {
            mSongId = songId;
        }
    }

    public void processFinish(MediaMetadataCompat output) {

    }

    private class LoadNewSongTask extends AsyncTask<SongParams, Void, MediaMetadataCompat> {
        private Context mContext;

//        public interface AsyncResponse {
//            void processFinish(MediaMetadataCompat output);
//        }

//        public AsyncResponse delegate = null;

        public LoadNewSongTask(Context context){
            mContext = context;
        }

        @Override
        protected MediaMetadataCompat doInBackground(SongParams... params) {

            MediaMetadataCompat newTrack = null;

            final int songId = params[0].mSongId;

            // look for this song in the database
            String selection = SongEntry.COLUMN_SONG_ID + "=?";
            String[] args = {String.valueOf(songId)};

            Cursor songCursor = mContext.getContentResolver().query(
                    SongEntry.CONTENT_URI,
                    null,
                    selection,
                    args,
                    null
            );

            // the song was found - now load data into a MediaMetadataCompat
            if (null != songCursor && songCursor.moveToFirst()){

                int deepScan = songCursor.getInt(SongEntry.COL_SONG_DEEP_SCAN);
                int songDbId = songCursor.getInt(SongEntry.COL_SONG_ID);
                String songFilename = songCursor.getString(SongEntry.COL_FILE_NAME);

                if (0 == deepScan) {
                    int parentId = songCursor.getInt(SongEntry.COL_PARENT_ID);
                    selection = NodeEntry.COLUMN_NODE_ID + "=?";
                    args[0] = String.valueOf(parentId);
                    Cursor nodeCursor = mContext.getContentResolver().query(
                            NodeEntry.CONTENT_URI,
                            null,
                            selection,
                            args,
                            null
                    );

                    if (null != nodeCursor && nodeCursor.moveToFirst()) {
                        String parentPath = nodeCursor.getString(NodeEntry.COL_FILE_PATH);
                        CopyFileService.startCopyFile(mContext, songDbId, parentPath + songFilename, SongListHelper.SONG_LIST_CURRENT);
                    }

                    if (null != nodeCursor) {
                        nodeCursor.close();
                        nodeCursor = null;
                    }
                }

                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                String dirPath = path.getAbsolutePath() + "/NMP/";

                //@SuppressWarnings("WrongConstant")
                newTrack = new MediaMetadataCompat.Builder()
                        .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, dirPath + songFilename)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, songCursor.getString(SongEntry.COL_SONG_ALBUM))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songCursor.getString(SongEntry.COL_SONG_ARTIST))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songCursor.getString(SongEntry.COL_SONG_TITLE))
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, songCursor.getInt(SongEntry.COL_SONG_TRACK))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, songDbId)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, songCursor.getInt(SongEntry.COL_SONG_DURATION))
                        .build();
            }

            return newTrack;
        }

        @Override
        protected void onPostExecute(MediaMetadataCompat result){
            super.onPostExecute(result);
            songList.add(result);
//            if (null != delegate) {
//                delegate.processFinish(result);
//            }
        }
    }




//    public String getPlayListName() {
//        return mPlaylist.getPlayListName();
//    }
//
//    public void setPlayListName(String name) {
//        mPlaylist.setPlayListName(name);
//    }
//
//    public int getPlaylistId() {
//        return mPlaylist.getPlaylistId();
//    }
//
//    public Playlist getPlaylist() {
//        return mPlaylist;
//    }

    public MediaMetadataCompat getCurrentSong() {
        MediaMetadataCompat current = null;
        if (songList.size() > 0 && currentSong >= 0 && currentSong < songList.size()) {
            current = songList.get(currentSong);
        }
        return current;
    }

    public void checkNextSong() {
        int nextSong = currentSong + 1;
        if (nextSong >= songList.size()) {
            nextSong = 0;
        }
        int songDbId = (int) songList.get(nextSong).getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
//        Log.d(TAG, "checkNextSong  songDbId: " + songDbId + " name: " + songList.get(nextSong).getString(MediaMetadataCompat.METADATA_KEY_TITLE));

        String selection = SongEntry.COLUMN_SONG_ID + "=?";
        String[] args = {String.valueOf(songDbId)};
        Cursor songCursor = mContext.getContentResolver().query(
                SongEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        if (null != songCursor && songCursor.moveToFirst()) {
            String filename = songCursor.getString(SongEntry.COL_FILE_NAME);
            if (false == fileOnDevice(filename)) {
//                Log.d(TAG, "checkNextSong  song not on device: calling getMetadataForId ");
                SongListHelper helper = new SongListHelper();
                helper.getMetadataForId(mContext, songDbId, SongListHelper.SONG_LIST_CURRENT);
            }
        }

        if (null != songCursor) {
            songCursor.close();
            songCursor = null;
        }
    }

    public void moveToNextSong() {
        currentSong++;
        if (currentSong >= songList.size()) {
            currentSong = 0;
        }
        SharedPrefUtils.getInstance().saveLastSong(currentSong);
    }

    public void moveToPreviousSong() {
        currentSong--;
        if (currentSong < 0 ) {
            currentSong = songList.size() - 1;
        }
        SharedPrefUtils.getInstance().saveLastSong(currentSong);
    }

    public void gotoSong(int songIndex) {
        if (songIndex >= 0 && songIndex < songList.size()){
            currentSong = songIndex;
            if (null != mChangeListener) {
                mChangeListener.onItemChanged();
            }
        }
        SharedPrefUtils.getInstance().saveLastSong(currentSong);
    }

    public void updateSong(MediaMetadataCompat newSong) {
//        Log.d(TAG, "updateSong  name: " + newSong.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
//        Log.d(TAG, "updateSong  artist: " + newSong.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
//        Log.d(TAG, "updateSong  album: " + newSong.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));

        // find the index of the newSong in the existing songList
        int index = findSongIndex(newSong);

        if (0 < index) {
            // create a replacement MediaMetadataCompat
            @SuppressWarnings("WrongConstant")
            MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, newSong.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
                    .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, newSong.getString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,newSong.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, newSong.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, newSong.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
                    .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, newSong.getLong(MediaMetadataCompat.METADATA_KEY_YEAR))
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, newSong.getString(MediaMetadataCompat.METADATA_KEY_GENRE))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, newSong.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, newSong.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, newSong.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, newSong.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, newSong.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
                    .build();

            // replace the old with the new
           String title = songList.get(index).getString(MediaMetadataCompat.METADATA_KEY_TITLE);

//            Log.d(TAG, "updateSong AAAA index: " + index + " title: " + title + " size: " + songList.size());
            songList.set(index, newTrack);
            title = songList.get(index).getString(MediaMetadataCompat.METADATA_KEY_TITLE);
//            Log.d(TAG, "updateSong BBBB index: " + index + " title: " + title + " size: " + songList.size());
        }
    }



/*

        if (-1 == index) {
            songList.add(newSong);
            String artist = newSong.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            String title = newSong.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            int songId = (int) newSong.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
            int albumId = (int) newSong.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
            //String albumPath = newSong.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            mPlaylist.addSong(songId, artist, title, albumId, context);
            if (null != mChangeListener) {
                mChangeListener.onItemChanged();
            }
        } else {
            // update the existing song data
            MediaMetadataCompat mmc = songList.get(index);

            @SuppressWarnings("WrongConstant")
            MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mmc.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
                    .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, mmc.getString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,mmc.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mmc.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mmc.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
                    .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, mmc.getLong(MediaMetadataCompat.METADATA_KEY_YEAR))
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, mmc.getString(MediaMetadataCompat.METADATA_KEY_GENRE))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, mmc.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mmc.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mmc.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mmc.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, mmc.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
                    .build();


            songList.set(index, newTrack);
        }
*/

/*
    public void deleteSong(int songIndex, Context context) {
        if (songIndex >= 0 && songIndex < songList.size()){

            songList.remove(songIndex);
            mPlaylist.deleteSong(songIndex);
            mPlaylist.saveToDb(context);

            if (currentSong >= songList.size()) {
                currentSong = 0;
            }

            if (null != mChangeListener) {
                mChangeListener.onItemChanged();
            }
        }
    }
*/

    public ArrayList<MediaMetadataCompat> getSongList(){
        return songList;
    }

    public void clear() {
        songList.clear();
        currentSong = 0;
        mPlaylistId = -1;
        mPlaylistName = "temp";
        SharedPrefUtils.getInstance().saveLastSong(currentSong);

        if (null != mChangeListener) {
            mChangeListener.onItemChanged();
        }
    }

    public int findSongIndex(MediaMetadataCompat newSong) {
        int index = -1;

        int sSongDbId;
        int songDbId = (int) newSong.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
        //Log.d(TAG, "findSongIndex  songDbId: " + songDbId);

        for (int i = 0; i < songList.size(); i++) {

            sSongDbId = (int) songList.get(i).getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
            //Log.d(TAG, "findSongIndex  sSongDbId: " + sSongDbId);

            if (songDbId == sSongDbId) {
                index = i;
                break;
            }
        }

        //Log.d(TAG, "findSongIndex  index: " + index);

        return index;
    }

    // find out if the file is on the device
    public boolean fileOnDevice(String fileName){
        boolean itExists = false;

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File newDir = new File(path.toString() + "/NMP/" + fileName);

        itExists = newDir.exists();
        return itExists;
    }


/*
    public void buildFromLocalSongs(Context context) {
        this.clear();

        ContentResolver musicResolver = context.getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (null == musicCursor) {
            return;
        }

        // get important column numbers
        int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int isMusicColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
        int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int yearColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
        int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int trackColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int genreColumn = musicCursor.getColumnIndex("genre_name");

        while (musicCursor.moveToNext()) {

            // only add Music to this list (not ring tones, notifications, etc.)
            String isMusic = musicCursor.getString(isMusicColumn);

            if (Integer.valueOf(isMusic) == 0) {
                continue;
            }

            long thisId = musicCursor.getLong(idColumn);
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId);

            String trackPath = getFilePathFromContentUri(trackUri, musicResolver);

            long albumId = musicCursor.getLong(albumIdColumn);
//            String albumArt = (ContentUris.withAppendedId(artworkUri, albumId)).toString();

            String albumPath = getFilePathFromContentUri(ContentUris.withAppendedId(artworkUri, albumId), musicResolver);

            long duration = musicCursor.getInt(durationColumn);
            String yearS = musicCursor.getString(yearColumn);
            long year = 1900;
            if (null != yearS && yearS.length() > 0) year = Long.parseLong(yearS);

            long track = 1;
            String trackNumberS = musicCursor.getString(trackColumn);
            if (trackNumberS.length() > 0) track = Long.parseLong(trackNumberS);

            String artist = musicCursor.getString(artistColumn);
            String mediaId = String.valueOf(musicCursor.getLong(idColumn));
            String album = musicCursor.getString(albumColumn);
            String genre = musicCursor.getString(genreColumn);
            String title = musicCursor.getString(titleColumn);


            // specify the where clause
            String nodeColumns = SongEntry.COLUMN_SONG_ARTIST + " = ? " +
                    "AND " + SongEntry.COLUMN_SONG_TITLE + " = ?";

            // specify the node ID
            String[] nodeIds = {artist, title};


            Cursor songCursor = context.getContentResolver().query(
                    SongEntry.CONTENT_URI,
                    null,
                    nodeColumns,
                    nodeIds,
                    null
            );

            int songDbId = 0;

            if (null != songCursor) {
                if (songCursor.moveToFirst()) {
                    songDbId = songCursor.getInt(SongEntry.COL_SONG_ID);
                } else {

                    nodeColumns = SongEntry.COLUMN_SONG_TITLE + " = ? ";
                    String[] songIds = {title};

                    if (null != songCursor) {
                        songCursor.close();
                    }

                    songCursor = context.getContentResolver().query(
                            SongEntry.CONTENT_URI,
                            null,
                            nodeColumns,
                            songIds,
                            null
                    );

                    if (null != songCursor && songCursor.moveToFirst()) {
                        songDbId = songCursor.getInt(SongEntry.COL_SONG_ID);
                        Log.d(TAG, "buildFromLocalSongs retrieved songDbId: " + songDbId);
                    }
                }
            }

            if (null != songCursor) {
                songCursor.close();
            }

            @SuppressWarnings("WrongConstant")
            MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(musicCursor.getLong(idColumn)))
                    .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, trackPath)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicCursor.getString(albumColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicCursor.getString(artistColumn))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, year)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, musicCursor.getString(genreColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, albumId)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, songDbId)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicCursor.getString(titleColumn))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track)
                    .build();

            this.addSong(newTrack, context);
            }

        //int localCount = songList.size();

        if (null != musicCursor) {
            musicCursor.close();
        }

        if (null != mChangeListener) {
            mChangeListener.onItemChanged();
        }
    }
*/

    // filePath example: /storage/emulated/0/Music/NMP/Boston - Smokin'.mp3
/*
    private String getFilePathFromContentUri(Uri selectedUri,
                                             ContentResolver contentResolver) {
        String filePath="";
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedUri, filePathColumn, null, null, null);
        try {

            if (null != cursor) {
                cursor.moveToFirst();

                if (cursor.getColumnCount() > 0) {
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    int colType = cursor.getType(columnIndex);
                    int strType = cursor.FIELD_TYPE_STRING;
                    if (colType == strType) {
                        filePath = cursor.getString(columnIndex);
                    }
                }
            }
        }catch(Exception e){
            // todo log this problem and deal with it properly
        }

        cursor.close();

        return filePath;
    }
*/

/*
    public void loadFromPlaylist(Playlist playlist, GlobalApp globalApp) {

        // get rid of current playlist
        this.clear();
        mPlaylist = playlist;

        // expose the first song for other screens
        if (0 < mPlaylist.getListMembers().size()) {
            globalApp.setTransientSongDbId(mPlaylist.getListMembers().get(0).getId());
        }

        SongListHelper listHelper = new SongListHelper();

        int songDbId;
        for (SongNode songNode : playlist.getListMembers()) {
            songDbId = songNode.getId();
            Log.d(TAG, "requesting songDbId: " + songDbId);






            // add the current selection as the first song on the new list
            listHelper.getMetadataForId(globalApp.getApplicationContext(), songDbId, SongListHelper.SONG_LIST_CURRENT);
        }
    }
*/

    public void loadCheck(Context context) {

        SongListHelper listHelper = new SongListHelper();

        int songDbId;

        for (MediaMetadataCompat song:  songList) {
            songDbId = (int) song.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
//            Log.d(TAG, "requesting Meta Data update for songDbId: " + songDbId);
            listHelper.getMetadataForId(context, songDbId, SongListHelper.SONG_LIST_CURRENT);
        }
    }

//    public void savePlaylistToDb(Context context) {
//        mPlaylist.saveToDb(context);
//    }
/*
    public void buildFromPlaylist(int playlistId, String playlistName, GlobalApp globalApp) {

        mPlaylistId = playlistId;
        mPlaylistName = playlistName;

        // clear out the existing Current Playlist
        globalApp.getSongList().clear();
        Log.d(TAG, "new playlist ID: " + playlistId + " name: " + playlistName);

        // save the new playlist number and name
        SharedPrefUtils.getInstance().saveLastPlaylist(playlistId, playlistName);

        // get the songs in this playlist
        String selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";
        String[] args = { String.valueOf(playlistId) };

        Cursor songCursor = globalApp.getApplicationContext().getContentResolver().query(
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

        SongListHelper listHelper = new SongListHelper();
        // add each song on the list to the current play list
        // make sure that it has been copied to the phone
        // and grab the media details
        boolean firstSaved = false;
        while (songCursor.moveToNext()) {
            int songDbId = songCursor.getInt(NMPContract.PlaylistItemEntry.COL_PLAYLIST_ITEM_SONG_ID);

            if (false == firstSaved){
                firstSaved = true;
                globalApp.setTransientSongDbId(songDbId);
            }
            Log.d(TAG, "requesting songDbId: " + songDbId);

            // add the current selection as the first song on the new list
            listHelper.getMetadataForId(globalApp.getApplicationContext(), songDbId, SongListHelper.SONG_LIST_CURRENT);
        }
    }

    public void savePlaylist(Context context, int newPlayListId) {

        mPlaylistId = newPlayListId;

        // if playlist is -1 then get a new playlist ID
        if (-1 == mPlaylistId) {
            mPlaylistId = SharedPrefUtils.getInstance().getNextPlaylistId();
        }
        // loop through all songs in the list


        // save the playlist items
        List<ContentValues> songItems = new ArrayList<ContentValues>();

        for (MediaMetadataCompat song : songList) {
            ContentValues songValues = new ContentValues();
            songValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, mPlaylistId);
            songValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, song.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER));
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
            Log.d("SongList", "Total songs in playlist: " + mPlaylistId + "  : " + songCount);
        }

    }
*/
}
