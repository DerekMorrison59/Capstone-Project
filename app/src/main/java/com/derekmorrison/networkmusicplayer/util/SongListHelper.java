package com.derekmorrison.networkmusicplayer.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;
import com.derekmorrison.networkmusicplayer.sync.ScanFileService;
import com.derekmorrison.networkmusicplayer.ui.GlobalApp;

import java.io.File;

/**
 * Created by Derek on 9/3/2016.
 */
public class SongListHelper {
    private String TAG = "SongListHelper";

    public final static int SONG_LIST_NONE = 0;
    public final static int SONG_LIST_CURRENT = 1;
    public final static int SONG_LIST_TEMP = 2;


    public void getMetadataForId(Context context, int songDbId, int listId) {
        MediaMetadataCompat metadata = null;

        String songColumns = NMPContract.SongEntry.COLUMN_SONG_ID + " = ? ";
        String[] songIds = {String.valueOf(songDbId)};

        Cursor songCursor = context.getContentResolver().query(
                NMPContract.SongEntry.CONTENT_URI,
                null,
                songColumns,
                songIds,
                null
        );

        // just call CopyFileService and pass the listId so that ScanFileService knows where to put the results
        if (null != songCursor && songCursor.moveToFirst()) {

            Log.d(TAG, "getMetadataForId - found song in Db");

            String fileName = songCursor.getString(NMPContract.SongEntry.COL_FILE_NAME);

            int deepScan = songCursor.getInt(NMPContract.SongEntry.COL_SONG_DEEP_SCAN);

            if (1 == deepScan) {
                Log.d(TAG, "getMetadataForId - song already Deep Scanned: " + fileName);
                return;
            }


            //int songId = songCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);

            // create the file path using the parent node and the filename
            int parentId = songCursor.getInt(NMPContract.SongEntry.COL_PARENT_ID);

            String nodeColumns = NMPContract.NodeEntry.COLUMN_NODE_ID + " = ? ";
            String nodeIds[] = {String.valueOf(parentId)};

            Cursor nodeCursor = context.getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    nodeColumns,
                    nodeIds,
                    null
            );

            if (null != nodeCursor && nodeCursor.moveToFirst()) {
                String path = nodeCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH);
                CopyFileService.startCopyFile(context, songDbId, path + fileName, listId);
                Log.d(TAG, "getMetadataForId - found parent in Db: " + path + fileName);
            }


            if (null != songCursor) {
                songCursor.close();
                songCursor = null;
            }
            if (null != nodeCursor) {
                nodeCursor.close();
                nodeCursor = null;
            }



            // startCopyFile(Context context, int songId, String filePath, int listId)


/*
            String artist = songCursor.getString(NMPContract.SongEntry.COL_SONG_ARTIST);
            String title = songCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE);
            String album = songCursor.getString(NMPContract.SongEntry.COL_SONG_ALBUM);
            String genre = songCursor.getString(NMPContract.SongEntry.COL_SONG_GENRE);
            String artUrl = songCursor.getString(NMPContract.SongEntry.COL_SONG_ART_URL);




            @SuppressWarnings("WrongConstant")
                    metadata = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                    .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    //.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, year)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUrl)
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 0)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, songDbId)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track)
                    .build();


            // if the file is local then grab the 2 other tidbits
            if (true == fileOnDevice(fileName)) {

                String selection = MediaStore.Audio.Media.TITLE + " = ? AND " +
                        MediaStore.Audio.Media.ARTIST + " = ?";

                String[] selectionArgs = {title, artist};

                ContentResolver musicResolver = context.getContentResolver();
                Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                Cursor musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);

                if (null != musicCursor && musicCursor.moveToFirst()) {
                    int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);

                    long thisId = musicCursor.getLong(idColumn);
                    Uri trackUri = ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId);

                    String trackPath = ScanFileService.getFilePathFromContentUri(trackUri, musicResolver);


                }
            }

*/
        }

    }




    public void convertCursorToSongList(Context context, Cursor songCursor, int listId) {
        // this should be a cursor with the currently viewable songs (DirectoryFragment)

        if (null == songCursor) {
            return;
        }

        int songDbId = 0;
        int start = songCursor.getPosition();

        // add the songs that follow this one
        while (songCursor.moveToNext()) {
            songDbId = songCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
            getMetadataForId(context, songDbId, listId);
        }


        songCursor.moveToFirst();
        while (start > songCursor.getPosition()) {
            songDbId = songCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
            getMetadataForId(context, songDbId, listId);
            songCursor.moveToNext();
        }

        if (null != songCursor) {
            songCursor.close();
        }

    }


}
