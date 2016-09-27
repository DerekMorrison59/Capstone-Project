package com.derekmorrison.networkmusicplayer.sync;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ScanFileService extends IntentService {
    private static final String TAG = "ScanFileService";
    private static Context mContext;

    public String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    private static final String ACTION_DEEP_SCAN = "com.derekmorrison.networkmusicplayer.sync.action.DEEP_SCAN";
    private static final String EXTRA_FILE_PATH = "com.derekmorrison.networkmusicplayer.sync.extra.FILE_PATH";
    private static final String EXTRA_FILE_URI = "com.derekmorrison.networkmusicplayer.sync.extra.FILE_URI";
    private static final String EXTRA_SONG_ID = "com.derekmorrison.networkmusicplayer.sync.extra.SONG_ID";

    public ScanFileService() {
        super("ScanFileService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startFileScan(Context context, String filePath, Uri fileUri, int songId) {
        mContext = context;
        Intent intent = new Intent(context, ScanFileService.class);
        intent.setAction(ACTION_DEEP_SCAN);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_FILE_URI, fileUri.toString());
        intent.putExtra(EXTRA_SONG_ID, songId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DEEP_SCAN.equals(action)) {
                final String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                final String fileUri = intent.getStringExtra(EXTRA_FILE_URI);
                final int songId = intent.getIntExtra(EXTRA_SONG_ID, 0);
                handleActionFileScan(filePath, fileUri, songId);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFileScan(String filePath, String fileUri, int songId) {

        Log.d(TAG, "filePath: " + filePath);
        Log.d(TAG, "fileUri: " + fileUri);
        Log.d(TAG, "songId: " + songId);
        Uri uri = Uri.parse(fileUri);
        Log.d(TAG, "uri: " + uri);

        // grab data from MediaScannerConnection
        // update database

        ContentResolver musicResolver = mContext.getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

        // get the last part of the uri --> that's the ID
        String musicId = getIdFromUri(fileUri);
        String selection = MediaStore.Audio.Media._ID + " = ? ";;
        String[] selectionArgs = {musicId};

        Cursor musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);

        // if the cursor doesn't contain data then bail out now
        if (null == musicCursor) {
            //if (null == musicCursor || false == musicCursor.moveToFirst()) {
            return;
        }

        while (musicCursor.moveToNext()) {

            // sampling_rate

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

            String isMusic = musicCursor.getString(isMusicColumn);

            long thisId = musicCursor.getLong(idColumn);
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId);

            String trackPath = getFilePathFromContentUri(trackUri, musicResolver);

            long albumId = musicCursor.getLong(albumIdColumn);
            String albumArt = (ContentUris.withAppendedId(artworkUri, albumId)).toString();

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


            MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(musicCursor.getLong(idColumn)))
                    .putString(CUSTOM_METADATA_TRACK_SOURCE, trackPath)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicCursor.getString(albumColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicCursor.getString(artistColumn))
//                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (musicCursor.getInt(durationColumn)))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
//                            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, Long.parseLong(musicCursor.getString(yearColumn)))
                    .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, year)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, musicCursor.getString(genreColumn))

                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)

                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicCursor.getString(titleColumn))
//                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Long.parseLong(musicCursor.getString(trackColumn)))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track)
                    //.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                    .build();


            // don't forget to set 'deepScan' to 1

            // specify the column to use
            String nodeColumns = NMPContract.SongEntry.COLUMN_SONG_ID + " = ? ";

            // specify the node ID
            String[] nodeIds = {String.valueOf(songId)};

//            public static final String COLUMN_SONG_LAST_PLAYED = "last_played";
//            public static final String COLUMN_SONG_PLAY_COUNT = "play_count";
            //values.put(NMPContract.SongEntry.COLUMN_, yearS);



            // specify which column to update and the new value
            ContentValues values = new ContentValues();
            values.put(NMPContract.SongEntry.COLUMN_SONG_DEEP_SCAN, "1");
            values.put(NMPContract.SongEntry.COLUMN_SONG_ARTIST, artist);
            values.put(NMPContract.SongEntry.COLUMN_SONG_TITLE, title);

            values.put(NMPContract.SongEntry.COLUMN_SONG_ALBUM, album);
            values.put(NMPContract.SongEntry.COLUMN_SONG_DURATION, duration);
            values.put(NMPContract.SongEntry.COLUMN_SONG_TRACK, track);
            values.put(NMPContract.SongEntry.COLUMN_SONG_GENRE, genre);
            values.put(NMPContract.SongEntry.COLUMN_SONG_ART_URL, albumArt);


            int updatedRows = getContentResolver().update(
                    NMPContract.SongEntry.CONTENT_URI,
                    values,
                    nodeColumns,
                    nodeIds
            );
        }

    }

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

    private String getIdFromUri(String uri) {
        String id ="";
        int i = uri.lastIndexOf('/');
        if (i > 0 &&  i < uri.length() - 1) {
            id = uri.substring(i+1).toLowerCase();
        }
        return id;
    }

}