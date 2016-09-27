package com.derekmorrison.networkmusicplayer.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.ui.GlobalApp;

/**
 * Created by Derek on 9/22/2016.
 */
public class AlbumArt {

    private String TAG = "AlbumArt";
    private static AlbumArt instance = null;
    private static Bitmap mBitmap;
    private static int mSongId = 0;
    private int[] colorBacks = new int[9];

    private AlbumArt() {
        setupArray();
    }

    public static AlbumArt getInstance() {
        if (instance == null)
            instance = new AlbumArt();

        return instance;
    }

    private void setupArray() {
        colorBacks[0] = R.drawable.blue_cover;
        colorBacks[1] = R.drawable.red_cover;
        colorBacks[2] = R.drawable.yellow_cover;
        colorBacks[3] = R.drawable.purple_cover;
        colorBacks[4] = R.drawable.pink_cover;
        colorBacks[5] = R.drawable.brown_cover;
        colorBacks[6] = R.drawable.teal_cover;
        colorBacks[7] = R.drawable.indigo_cover;
        colorBacks[8] = R.drawable.gray_cover;
    }

    public int getPlaceHolderId(int songDbId){
        return colorBacks[mod(songDbId, 9)];
    }

    public Bitmap getArtWork(int songId, int albumId) {

        if (songId == mSongId) {
            return mBitmap;
        }

        mSongId = songId;

        Context appContext = GlobalApp.getContext();

        Bitmap albumBitmap = null;

        final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
        Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumId);

        //Log.d(TAG, "getArtWork - albumId: " + albumId + " albumArtUri: " + albumArtUri);

        try {
            albumBitmap = MediaStore.Images.Media.getBitmap(appContext.getContentResolver(), albumArtUri);
        } catch (Exception exception) {
            // log error
        }

        if (null == albumBitmap) {
            //Log.d(TAG, "getArtWork - albumId: " + albumId + " not found, loading default");
            int choice = mod(songId, 9);
            Log.d(TAG, "getArtWork - albumId: " + albumId + " not found, loading default: " + choice);
            albumBitmap = BitmapFactory.decodeResource(appContext.getResources(), colorBacks[choice]);
        }

        mBitmap = albumBitmap;
        return albumBitmap;
    }

    private int mod(int x, int y)
    {
        int result = x % y;
        return result < 0 ? result + y : result;
    }
}
