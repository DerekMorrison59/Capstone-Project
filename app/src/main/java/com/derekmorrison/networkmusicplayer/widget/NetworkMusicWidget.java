package com.derekmorrison.networkmusicplayer.widget;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.playback.MediaNotificationManager;
import com.derekmorrison.networkmusicplayer.ui.GlobalApp;
import com.derekmorrison.networkmusicplayer.ui.MainActivity;
import com.derekmorrison.networkmusicplayer.ui.NowPlayingFragment;
import com.derekmorrison.networkmusicplayer.util.AlbumArt;

/**
 * Implementation of App Widget functionality.
 */
public class NetworkMusicWidget extends AppWidgetProvider {
    private final String TAG = "NetworkMusicWidget";
    private static Bitmap mPauseDrawable;
    private static Bitmap mPlayDrawable;
    private static String pkg;
    private int mState = -13;

    private static PendingIntent mPauseIntent;
    private static PendingIntent mPlayIntent;
    private static PendingIntent mNextIntent;
    private static PendingIntent mPreviousIntent;
    private static  PendingIntent mLaunchApp;


    // REQUEST CODES
    public static final int WIDGET_REQUEST_CODE_PAUSE = 200;
    public static final int WIDGET_REQUEST_CODE_PLAY = 201;
    public static final int WIDGET_REQUEST_CODE_PREVIOUS = 202;
    public static final int WIDGET_REQUEST_CODE_NEXT = 203;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        GlobalApp globalApp = (GlobalApp) GlobalApp.getContext();
        MediaMetadataCompat song = globalApp.getSongList().getCurrentSong();

        String songName = "song name here";
        String artistName = "artist name here";
        int albumId = 0;
        int songDbId = 0;

        if (null != song) {
            songName = song.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            artistName = song.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            albumId = (int) song.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
            songDbId = (int) song.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
        }

        //Log.d(TAG, "updateAppWidget - albumId: " + albumId + " songDbId: " + songDbId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.network_music_widget);
        views.setTextViewText(R.id.widget_title, songName);
        views.setTextViewText(R.id.widget_artist, artistName);
        views.setImageViewBitmap(R.id.widget_image, AlbumArt.getInstance().getArtWork(songDbId, albumId));
        views.setOnClickPendingIntent(R.id.widgetPrevious, mPreviousIntent);
        views.setOnClickPendingIntent(R.id.widgetNext, mNextIntent);
        views.setOnClickPendingIntent(R.id.widget_item_layout, mLaunchApp);

        //Log.d(TAG, "updateAppWidget - mState: " + mState);

        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            views.setOnClickPendingIntent(R.id.widgetPlayPause, mPauseIntent);
            views.setImageViewBitmap(R.id.widgetPlayPause, mPauseDrawable);
        } else {
            views.setOnClickPendingIntent(R.id.widgetPlayPause, mPlayIntent);
            views.setImageViewBitmap(R.id.widgetPlayPause, mPlayDrawable);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (MediaNotificationManager.ACTION_METADATA_UPDATED.equals(intent.getAction())) {
            //String source = intent.getStringExtra("SOURCE");
            mState = intent.getIntExtra("STATE", -1);
            //Log.d(TAG, "onReceive - METADATA - source: " + source + "  mState: " + mState);
        } else if (MediaNotificationManager.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            //String source = intent.getStringExtra("SOURCE");
            mState = intent.getIntExtra("STATE", -1);
            //Log.d(TAG, "onReceive - STATE - source: " + source + "  mState: " + mState);
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                NetworkMusicWidget.class));

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Log.d(TAG, "UUUUUUUUUUUUUUUUUUUUU       onUpdate  ");
        mPauseDrawable = BitmapFactory.decodeResource(context.getResources(), R.drawable.uamp_ic_pause_white_24dp);
        mPlayDrawable = BitmapFactory.decodeResource(context.getResources(), R.drawable.uamp_ic_play_arrow_white_24dp);

        pkg = context.getPackageName();

        mPauseIntent = PendingIntent.getBroadcast(context, WIDGET_REQUEST_CODE_PAUSE,
                new Intent(MediaNotificationManager.ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(context, WIDGET_REQUEST_CODE_PLAY,
                new Intent(MediaNotificationManager.ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(context, WIDGET_REQUEST_CODE_PREVIOUS,
                new Intent(MediaNotificationManager.ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(context, WIDGET_REQUEST_CODE_NEXT,
                new Intent(MediaNotificationManager.ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        Intent mainAppIntent = new Intent(context, MainActivity.class);
        mLaunchApp = PendingIntent.getActivity(context, 98, mainAppIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

