package com.derekmorrison.networkmusicplayer.ui;


import android.app.ProgressDialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;
import com.derekmorrison.networkmusicplayer.sync.ScanFileService;
import com.derekmorrison.networkmusicplayer.util.AlbumArt;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A simple {@link Fragment} subclass.
 */
public class NowPlayingFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "NowPlayingFragment";

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    protected NodeNavigator mNodeNavigator;
    private static final int FILE_LOADER = 1;

    private TextView mArtistText;
    private TextView mTitleText;
    private TextView mAlbumText;
    private TextView mStart;
    private TextView mDurationText;
    private ImageView mAlbumCover;
    private SeekBar mSeekbar;
    private ProgressDialog mProgress;

    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private Cursor mCursor;

    private MediaSessionCompat.Token mToken;
    private MediaControllerCompat mMediaController = null;
    private ScheduledFuture<?> mScheduleFuture;

    private PlaybackStateCompat mLastPlaybackState;
    private GlobalApp mGlobalApp;
    private Tracker mTracker;

    private final Handler mHandler = new Handler();

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
//            Log.d(TAG, "onPlaybackstate changed: " + state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            updateScreen();
/*
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
*/
        }
    };

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGlobalApp = ((GlobalApp) getActivity().getApplication());

        mTracker = mGlobalApp.getDefaultTracker();
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        mPauseDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_pause_circle_filled_black_48dp);
        mPlayDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_play_circle_filled_black_48dp);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.directory_frag, menu);
    }

    private void goUpLevel() {
        MainActivity.setFragment(MainActivity.FRAGMENT_DIRECTORY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        View npView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mNodeNavigator = mGlobalApp.getNodeNavigator();
        MainActivity.setToolbarTitle("Now Playing");

        // get references for the various TextViews
        mArtistText = (TextView) npView.findViewById(R.id.artistTV);
        mTitleText = (TextView) npView.findViewById(R.id.titleTV);
        mAlbumText = (TextView) npView.findViewById(R.id.albumTV);
        mStart = (TextView) npView.findViewById(R.id.startTV);
        mDurationText = (TextView) npView.findViewById(R.id.durationTV);
        mAlbumCover = (ImageView) npView.findViewById(R.id.albumImageView);

        mSeekbar = (SeekBar) npView.findViewById(R.id.songSeekBar);
        mPlayPause = (ImageView) npView.findViewById(R.id.play_pause);
        mSkipNext = (ImageView) npView.findViewById(R.id.next);
        mSkipPrev = (ImageView) npView.findViewById(R.id.prev);

        mToken = mGlobalApp.getMediaSessionCompatToken();

        if (null != mToken) {
            try {
                mMediaController = new MediaControllerCompat(getContext(), mToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (null != mMediaController) {
            mMediaController.registerCallback(mCallback);
        }

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        mMediaController.getTransportControls();
                controls.skipToNext();

//                Log.d(TAG, "Tracker: HitBuilders - Action - SkipToNext");
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("SkipToNext")
                        .build());
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        mMediaController.getTransportControls();
                controls.skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = mMediaController.getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls =
                            mMediaController.getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING:
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_NONE:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
//                            Log.d(TAG, "onClick with state " + state.getState());
                    }
                }
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMediaController.getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        int state = PlaybackStateCompat.STATE_NONE;
        if (null != mMediaController) {
            if (PlaybackStateCompat.STATE_NONE == mMediaController.getPlaybackState().getState()) {
                mMediaController.getTransportControls().pause();
            }
            state = mMediaController.getPlaybackState().getState();
        }
        updatePlaybackControls(state);
        if (null != mMediaController) {
            updatePlaybackState(mMediaController.getPlaybackState());
        }
        updateScreen();
        updateProgress();

        MediaMetadataCompat currentSong = mGlobalApp.getSongList().getCurrentSong();

        // check for artist name
        String artist = currentSong.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

        if (null == artist || artist.length() < 1) {
            //Log.d(TAG, "  onCreateView - no song available - it must be loading...");

            mProgress = new ProgressDialog(getContext());
            mProgress.setTitle("Loading");
            mProgress.setMessage("Copying Song to Phone...");
            mProgress.show();
        }

        npView.setFocusableInTouchMode(true);
        npView.requestFocus();
        npView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //Log.d(TAG, "onKey" );
                if( keyCode == KeyEvent.KEYCODE_BACK ){
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        goUpLevel();
                    }
                    return true;
                }
                return false;
            }
        });

        return npView;
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void updateProgress() {

        long currentPosition = 0;

        if (mLastPlaybackState != null) {
            currentPosition = mLastPlaybackState.getPosition();
        }

        //Log.d(TAG, "updateProgress - currentPosition " + currentPosition);

        if (mLastPlaybackState != null && mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }

        // update the progress of the seekbar
        mSeekbar.setProgress((int) currentPosition);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
        //Log.d(TAG, "    [[[[[[[ onDestroy ]]]]]]]]   Duh Duh Duh Duuuuuhhhhh");
    }

    private void updateScreen() {
/*
        mArtistText.setText(mNodeNavigator.getSongNode().getArtist());
        mTitleText.setText(mNodeNavigator.getSongNode().getTitle());
        mAlbumText.setText(mNodeNavigator.getSongNode().getAlbum());
        mDurationText.setText(convertMsToMinutes(mNodeNavigator.getSongNode().getDuration()));
*/
        //Log.d(TAG, "  updateScreen ");

        MediaMetadataCompat source = mGlobalApp.getSongList().getCurrentSong();

        String artist = "";
        String title = "";
        String album = "";
        int duration = 0;
        int albumId = 1;
        int songDbId = 0;

        if (null == source) {
            if (null == mCursor) {
                Log.d(TAG, "  updateScreen - no song available");
                return;
            } else if (true == mCursor.moveToFirst()){
                //Log.d(TAG, "  updateScreen from mCursor");
                artist = mCursor.getString(NMPContract.SongEntry.COL_SONG_ARTIST);
                title = mCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE);
                album = mCursor.getString(NMPContract.SongEntry.COL_SONG_ALBUM);
                duration = mCursor.getInt(NMPContract.SongEntry.COL_SONG_DURATION);
                albumId = mCursor.getInt(NMPContract.SongEntry.COL_SONG_TRACK);
                songDbId = mCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
            }
        } else {
            //Log.d(TAG, "  updateScreen from SongList.getCurrentSong");
            artist = source.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            title = source.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            album = source.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            duration = (int) source.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            albumId = (int) source.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
            songDbId = (int) source.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
        }

        mArtistText.setText(artist);
        mTitleText.setText(title);
        mAlbumText.setText(album);
        mSeekbar.setMax(duration);

        mDurationText.setText(DateUtils.formatElapsedTime(duration / 1000));

        final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
        Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumId);
        //Log.d(TAG, " ^ ^ ^ updateScreen - albumId: " + albumId + " albumArtUri " + albumArtUri);

        // use picasso to load the album cover (or the placeholder)
        Picasso.with(getContext())
                .load(albumArtUri)
                .placeholder(AlbumArt.getInstance().getPlaceHolderId(songDbId))
                .into(mAlbumCover);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
//        Log.d(TAG, "onActivityCreated ");
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FILE_LOADER, null, this);
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            Log.d(TAG, "  updatePlaybackState STATE is null ");
            return;
        }
        mLastPlaybackState = state;

        if (PlaybackStateCompat.STATE_ERROR == state.getState()) {
            Log.d(TAG, "  updatePlaybackState ERROR STATE: " + state.getErrorMessage());
        }

        updatePlaybackControls(state.getState());

        mSkipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE );
        mSkipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE );
    }

    public void updatePlaybackControls(int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                Log.d(TAG, "updatePlaybackControls: Unhandled state " + state);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //Log.d(TAG, "onCreateLoader LOADER: " + i);

        String dirColumns = NMPContract.SongEntry.COLUMN_SONG_ID + "=?";
        MediaMetadataCompat cSong = mGlobalApp.getSongList().getCurrentSong();

        long currentNodeId = 0;
        if (null != cSong) {
            currentNodeId = cSong.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER);
        } else {
            currentNodeId = mGlobalApp.getTransientSongDbId();
        }

        //Log.d(TAG, " < < < < < < < onCreateLoader -  songDbId: " + currentNodeId);
        String[] dirIds = {String.valueOf(currentNodeId)};
        return new CursorLoader(getActivity(),
                NMPContract.SongEntry.CONTENT_URI,
                null,
                dirColumns,
                dirIds,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;

        //int results = data.getCount();
        //Log.d(TAG, " >>>>>>> onLoadFinished - rows returned: " + results + "  id: " + loader.getId());

        // use the cursor to update the UI
        if (true == data.moveToFirst()) {
            //Log.d(TAG, " >>>>>>> onLoadFinished  ++++ Data Was Returned");

            int dScan = data.getInt(NMPContract.SongEntry.COL_SONG_DEEP_SCAN);
            if (dScan > 0) {
                if (null != mProgress && mProgress.isShowing()) {
                    //Log.d(TAG, " >>>>>>> onLoadFinished Dialog Dismissed because DEEP SCAN!");
                    mProgress.dismiss();
                }
            }
        }

        updateScreen();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // nothing to do because there is no adapter
    }

}
