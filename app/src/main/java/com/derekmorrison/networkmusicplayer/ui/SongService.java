package com.derekmorrison.networkmusicplayer.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.playback.LocalPlayback;
import com.derekmorrison.networkmusicplayer.playback.MediaNotificationManager;
import com.derekmorrison.networkmusicplayer.playback.PlaybackManager;

public class SongService extends Service implements
        PlaybackManager.PlaybackServiceCallback {

    private String TAG = "SongService";

    private NotificationManager mNM;
    private MediaMetadataCompat mMetaData;
    private PlaybackStateCompat mPlaybackState;
    private MediaControllerCompat mMediaController = null;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    //private int NOTIFICATION = R.string.song_service_started;

    private Context mContext = null;

    private LocalPlayback mLocalPlayback = null;
    private PlaybackManager mPlaybackManager = null;
    private MediaSessionCompat mSession = null;
    //private MediaControllerCompat mController;

    private GlobalApp mGlobalApp;
    private MediaSessionCompat.Token mSessionToken;
    private NotificationManagerCompat mNotificationManager;
    private MediaNotificationManager mMediaNotificationManager;

    public class LocalBinder extends Binder {
        SongService getService() {
            return SongService.this;
        }
    }

    @Override
    public void onCreate() {
//        Log.i("SongService", " onCreate");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mNotificationManager = NotificationManagerCompat.from(this);
        mGlobalApp = ((GlobalApp) this.getApplication());

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "SongService");
        mSessionToken = mSession.getSessionToken();
        mGlobalApp.setMediaSessionCompatToken(mSessionToken);

        if (null != mSessionToken) {
            try {
                mMediaController = new MediaControllerCompat(getApplicationContext(), mSessionToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();

        try {
            mMediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.i("SongService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        //mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.song_service_stopped, Toast.LENGTH_SHORT).show();

        mMediaNotificationManager.stopNotification();
//        if (null != mMediaPlayer) {
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    public boolean hasContext() {
        return mContext != null;
    }

    public void setUp(Context context) {
//        Log.i("SongService", " **** setUp - creating new LocalPlayback, PlaybackManager");

        mContext = context;

        mLocalPlayback = new LocalPlayback(context, this.getApplication());

        mPlaybackManager = new PlaybackManager(this, getResources(), mLocalPlayback);

        // send Media Session callbacks to the Playback Manager
        mSession.setCallback(mPlaybackManager.getMediaSessionCallback());

        // tell the Media Session which events will be handled by Playback Manager
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent intent = new Intent(mContext, NowPlayingFragment.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        mPlaybackManager.updatePlaybackState(null);
    }

//    public void playSong() {
//        mLocalPlayback.play();
//    }

    // PlaybackManager Callback Methods
    //      onPlaybackStart
    //      onPlaybackStop
    //      onNotificationRequired
    //      onPlaybackStateUpdated
    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    @Override
    public void onPlaybackStart() {
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        //mDelayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.

//        Log.i("SongService", " onPlaybackStart: Starting SongService =========================================");

        //startService(new Intent(getApplicationContext(), SongService.class));
    }


    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    @Override
    public void onPlaybackStop() {
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        //mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }

    @Override
    public void onNotificationRequired() {

        mMediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
//        Log.d(TAG, " onPlaybackStateUpdated: state: " + newState.getState());
        mPlaybackState = newState;
        mSession.setPlaybackState(newState);

//        Intent stateChangedIntent = new Intent(MediaNotificationManager.ACTION_STATE_CHANGED);
//        stateChangedIntent.putExtra("SOURCE", "SongService");
//        stateChangedIntent.putExtra("STATE", newState.getState());
//        sendBroadcast(stateChangedIntent);
    }

    @Override
    public void onUpdateMetadata(MediaMetadataCompat metaData){
//        Log.d(TAG, " onUpdateMetadata");
        mMetaData = metaData;
        mSession.setMetadata(metaData);

        Intent metaDataUpdatedIntent = new Intent(MediaNotificationManager.ACTION_METADATA_UPDATED);
        metaDataUpdatedIntent.putExtra("SOURCE", "SongService");
        metaDataUpdatedIntent.putExtra("STATE", mPlaybackState.getState());
        sendBroadcast(metaDataUpdatedIntent);

    }


}
