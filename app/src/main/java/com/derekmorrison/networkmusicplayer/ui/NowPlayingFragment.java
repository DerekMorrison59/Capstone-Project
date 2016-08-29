package com.derekmorrison.networkmusicplayer.ui;


import android.app.ProgressDialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;

import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 */
public class NowPlayingFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "NowPlayingFragment";
    protected NodeNavigator mNodeNavigator;
    private static final int FILE_LOADER = 1;

    TextView mArtistText;
    TextView mTitleText;
    TextView mAlbumText;
    TextView mDurationText;
    ImageView mAlbumCover;
    private ProgressDialog mProgress;

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this is required for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.directory_frag, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_up_level) {
            goUpLevel();

/*
            //DirNode parent = mNodeNavigator.goBack();

            // now move to the folder navigation screen
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DirectoryFragment fragment = new DirectoryFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
*/

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void goUpLevel() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DirectoryFragment fragment = new DirectoryFragment();
        transaction.replace(R.id.sample_content_fragment, fragment);
        transaction.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView" );

        // Inflate the layout for this fragment
        View npView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();
        MainActivity.setToolbarTitle("Now Playing");

        // has this song been deep scanned?
        if (0 == mNodeNavigator.getSongNode().getDeepScan()) {
            int currentSongId = mNodeNavigator.getSongNode().getId();
            String filePath = mNodeNavigator.getSongNode().getFilePath();
            CopyFileService.startCopyFile(getContext(), currentSongId, filePath);

            mProgress = new ProgressDialog(getContext());
            mProgress.setTitle("Loading");
            mProgress.setMessage("Copying Song to Phone...");
            mProgress.show();
        }

        // get references for the various TextViews
        mArtistText = (TextView)npView.findViewById(R.id.artistTV);
        mTitleText = (TextView)npView.findViewById(R.id.titleTV);
        mAlbumText = (TextView)npView.findViewById(R.id.albumTV);
        mDurationText = (TextView)npView.findViewById(R.id.durationTV);
        mAlbumCover = (ImageView)npView.findViewById(R.id.albumImageView);

        updateScreen();

        npView.setFocusableInTouchMode(true);
        npView.requestFocus();
        npView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey" );
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

    private void updateScreen() {
        mArtistText.setText(mNodeNavigator.getSongNode().getArtist());
        mTitleText.setText(mNodeNavigator.getSongNode().getTitle());
        mAlbumText.setText(mNodeNavigator.getSongNode().getAlbum());
        mDurationText.setText(convertMsToMinutes(mNodeNavigator.getSongNode().getDuration()));

        String artUrl = mNodeNavigator.getSongNode().getArtUrl();

        if (null != artUrl && artUrl.contains("content:")) {
            //final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
            //Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumId);
            Uri albumArtUri = Uri.parse(artUrl);

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), albumArtUri);
            } catch (Exception exception) {
                // log error
            }
            if (null != bitmap) {
                mAlbumCover.setImageBitmap(bitmap);
            } // todo get image from last.fm
        }
    }

    private String convertMsToMinutes(int duration) {
        String minAndSec = "N/A";
        long length = duration;
        if (duration > 0) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
            length = length - (minutes * 60000);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(length);

            minAndSec = String.valueOf(minutes) + ":" + String.format("%02d", seconds);
        }

        return minAndSec;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated ");
        getLoaderManager().initLoader(FILE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG, "onCreateLoader LOADER: " + i);
/*
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String currentId = sp.getString("CurrentId", "0");
        mCurrentNodeId = Integer.valueOf(currentId);
        Log.d(TAG, "onCreateLoader ******>>>  CurrentId from SharedPrefs = " + currentId);
*/
        String dirColumns = NMPContract.SongEntry.COLUMN_SONG_ID + "=?";
        int currentNodeId = mNodeNavigator.getSongNode().getId();
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

        // use the cursor to update the UI
        if (true == data.moveToFirst()) {

//            int songId = data.getInt(NMPContract.SongEntry.COL_SONG_ID);
//            int parentId = data.getInt(NMPContract.SongEntry.COL_PARENT_ID);
//            String songFileName = data.getString(NMPContract.SongEntry.COL_FILE_NAME);

            SongNode sNode = mNodeNavigator.getSongNode();

            sNode.setDeepScan(data.getInt(NMPContract.SongEntry.COL_SONG_DEEP_SCAN));

            if (1 == sNode.getDeepScan()) {
                sNode.setDuration(data.getInt(NMPContract.SongEntry.COL_SONG_DURATION));
                sNode.setLastPlayed(data.getInt(NMPContract.SongEntry.COL_SONG_LAST_PLAYED));
                sNode.setPlayCount(data.getInt(NMPContract.SongEntry.COL_SONG_PLAY_COUNT));
                sNode.setTrack(data.getInt(NMPContract.SongEntry.COL_SONG_TRACK));
                sNode.setAlbum(data.getString(NMPContract.SongEntry.COL_SONG_ALBUM));
                sNode.setArtUrl(data.getString(NMPContract.SongEntry.COL_SONG_ART_URL));
                sNode.setArtist(data.getString(NMPContract.SongEntry.COL_SONG_ARTIST));
                sNode.setGenre(data.getString(NMPContract.SongEntry.COL_SONG_GENRE));
                sNode.setTitle(data.getString(NMPContract.SongEntry.COL_SONG_TITLE));

                if (null != mProgress && mProgress.isShowing()) {
                    mProgress.dismiss();
                }

            }
            updateScreen();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {


    }

}
