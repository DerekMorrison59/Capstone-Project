/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.derekmorrison.networkmusicplayer.playback;

import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.ui.GlobalApp;
import com.derekmorrison.networkmusicplayer.ui.SongService;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = "PlaybackManager";

//    private MusicProvider mMusicProvider;
//    private QueueManager mQueueManager;
    private Resources mResources;
    private Playback mPlayback;
    private PlaybackServiceCallback mServiceCallback;
    private MediaSessionCallback mMediaSessionCallback;
    private GlobalApp mGlobalApp = null;

    public PlaybackManager(PlaybackServiceCallback serviceCallback, Resources resources,
//                           MusicProvider musicProvider, QueueManager queueManager,
                           Playback playback) {
//        mMusicProvider = musicProvider;
        mServiceCallback = serviceCallback;
        mResources = resources;
//        mQueueManager = queueManager;
        mMediaSessionCallback = new MediaSessionCallback();
        mPlayback = playback;
        mPlayback.setCallback(this);

        mGlobalApp = ((GlobalApp) ((SongService) serviceCallback).getApplication());
    }

    public Playback getPlayback() {
        return mPlayback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mMediaSessionCallback;
    }

    /**
     * Handle a request to play music
     */
    public void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState = " + mPlayback.getState());


//        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();

/*
        MediaSessionCompat.QueueItem currentMusic = null;
        if (currentMusic != null) {
            mServiceCallback.onPlaybackStart();
            mPlayback.play(currentMusic);
        }
*/
        mServiceCallback.onPlaybackStart();
        mPlayback.play(null);
        mServiceCallback.onUpdateMetadata(mGlobalApp.getSongList().getCurrentSong());
        mGlobalApp.getSongList().checkNextSong();
    }

    /**
     * Handle a request to pause music
     */
    public void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        if (mPlayback.isPlaying()) {
            mPlayback.pause();
            mServiceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error= " + withError);
        mPlayback.stop(true);
        mServiceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state = " + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        Log.d(TAG, "updatePlaybackState, position = " + position);

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);
        int state = mPlayback.getState();

        Log.d(TAG, "updatePlaybackState, state = " + state);

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.


        MediaMetadataCompat currentSong = mGlobalApp.getSongList().getCurrentSong();

        MediaSessionCompat.QueueItem currentMusic = null;
        MediaDescriptionCompat currentDescription;

        if (null != currentSong) {
            Log.d(TAG, "updatePlaybackState, currentSong title = " + currentSong.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();
            currentDescription = builder.setMediaId(currentSong.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
                    .setTitle(currentSong.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .setSubtitle(currentSong.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                    .build();

            currentMusic = new MediaSessionCompat.QueueItem(currentDescription, 1);
        }
//        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();

        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());
        mServiceCallback.onUpdateMetadata(currentSong);

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired();
        }
    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
// TODO get the current music selection from the NodeNavigator
        MediaSessionCompat.QueueItem currentMusic = null;
//        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic == null) {
            return;
        }
        // Set appropriate "Favorite" icon on Custom action:
        String mediaId = currentMusic.getDescription().getMediaId();
        if (mediaId == null) {
            return;
        }
/*
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
        int favoriteIcon = mMusicProvider.isFavorite(musicId) ?
                R.drawable.ic_star_on : R.drawable.ic_star_off;
        Log.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
                musicId, " current favorite=", mMusicProvider.isFavorite(musicId));
        Bundle customActionExtras = new Bundle();
        WearHelper.setShowCustomActionOnWear(customActionExtras, true);
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon)
                .setExtras(customActionExtras)
                .build());
*/
    }

    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
/*
        if (mQueueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            mQueueManager.updateMetadata();
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
*/
        //handleStopRequest(null);
        mGlobalApp.getSongList().moveToNextSong();
        //mServiceCallback.onUpdateMetadata(mGlobalApp.getSongList().getCurrentSong());
        handlePlayRequest();
    }

    @Override
    public void onPlaybackStatusChanged(int state) {

        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        Log.d(TAG, "setCurrentMediaId " + mediaId);
        //mQueueManager.setQueueFromMusic(mediaId);
    }


    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    public void switchToPlayback(Playback playback, boolean resumePlaying) {
        if (playback == null) {
            throw new IllegalArgumentException("Playback cannot be null");
        }
        // suspend the current one.
        int oldState = mPlayback.getState();
        int pos = mPlayback.getCurrentStreamPosition();
        String currentMediaId = mPlayback.getCurrentMediaId();
        mPlayback.stop(false);
        playback.setCallback(this);
        playback.setCurrentStreamPosition(pos < 0 ? 0 : pos);
        playback.setCurrentMediaId(currentMediaId);
        playback.start();
        // finally swap the instance
        mPlayback = playback;
        switch (oldState) {
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayback.pause();
                break;
            case PlaybackStateCompat.STATE_PLAYING:

                // TODO get the current music selection from the NodeNavigator

                MediaMetadataCompat newSong = mGlobalApp.getSongList().getCurrentSong();
                //MediaSessionCompat.QueueItem currentMusic = null;
//                MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
                if (resumePlaying && newSong != null) {
                    //mPlayback.play(currentMusic);
                    mPlayback.play(null);
                } else if (!resumePlaying) {
                    mPlayback.pause();
                } else {
                    mPlayback.stop(true);
                }
                break;
            case PlaybackStateCompat.STATE_NONE:
                break;
            default:
                Log.d(TAG, "Default called. Old state is " + oldState);
        }
    }


    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "MediaSessionCompat.Callback onPlay");
//            if (mQueueManager.getCurrentMusic() == null) {
//                mQueueManager.setRandomQueue();
//            }
            //mServiceCallback.onUpdateMetadata(mGlobalApp.getSongList().getCurrentSong());
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "MediaSessionCompat.Callback OnSkipToQueueItem:" + queueId);
//            mQueueManager.setCurrentQueueItem(queueId);
            handlePlayRequest();
//            mQueueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "MediaSessionCompat.Callback onSeekTo:" + position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "MediaSessionCompat.Callback playFromMediaId mediaId: " + mediaId + "  extras= " + extras);
//            mQueueManager.setQueueFromMusic(mediaId);
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            Log.d(TAG, "MediaSessionCompat.Callback pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "MediaSessionCompat.Callback stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "MediaSessionCompat.Callback skipToNext");
/*
            if (mQueueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            mQueueManager.updateMetadata();
*/
            mGlobalApp.getSongList().moveToNextSong();
            //mServiceCallback.onUpdateMetadata(mGlobalApp.getSongList().getCurrentSong());
            handlePlayRequest();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "MediaSessionCompat.Callback onSkipToPrevious");
/*
            if (mQueueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            mQueueManager.updateMetadata();
*/
            mGlobalApp.getSongList().moveToPreviousSong();
            //mServiceCallback.onUpdateMetadata(mGlobalApp.getSongList().getCurrentSong());
            handlePlayRequest();
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
/*
            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                LogHelper.i(TAG, "onCustomAction: favorite for current track");
                MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
                if (currentMusic != null) {
                    String mediaId = currentMusic.getDescription().getMediaId();
                    if (mediaId != null) {
                        String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
                        mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                    }
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null);
            } else {
                LogHelper.e(TAG, "Unsupported action: ", action);
            }
*/
        }

        /**
         * Handle free and contextual searches.
         * <p/>
         * All voice searches on Android Auto are sent to this method through a connected
         * {@link android.support.v4.media.session.MediaControllerCompat}.
         * <p/>
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         * <p/>
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an {@link AsyncTask} as we do here).
         **/
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            Log.d(TAG, "playFromSearch  query= " + query + " extras= " + extras);

            mPlayback.setState(PlaybackStateCompat.STATE_CONNECTING);
/*
            boolean successSearch = mQueueManager.setQueueFromSearch(query, extras);
            if (successSearch) {
                handlePlayRequest();
                mQueueManager.updateMetadata();
            } else {
                updatePlaybackState("Could not find music");
            }
*/
            updatePlaybackState("Could not find music");
        }
    }


    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);

        void onUpdateMetadata(MediaMetadataCompat metaData);
    }
}
