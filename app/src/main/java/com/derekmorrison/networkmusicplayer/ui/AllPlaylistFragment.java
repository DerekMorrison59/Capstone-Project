package com.derekmorrison.networkmusicplayer.ui;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.util.Playlist;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.SongListHelper;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllPlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        AllPlaylistAdapter.MenuClickListener {

    private static final String TAG = "AllPlaylistFragment";
    private static final int LIST_LOADER = 1;
    private Cursor mCursor = null;

    protected RecyclerView mRecyclerView;
    protected AllPlaylistAdapter mListAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    private int mSelectedPlaylistId = 0;
    private String mSelectedPlaylistName = "";
    private Tracker mTracker;

    public AllPlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView" );

        // Inflate the layout for this fragment
        View playlistView = inflater.inflate(R.layout.fragment_all_playlist, container, false);

        MainActivity.setToolbarTitle("Playlists");

        mRecyclerView = (RecyclerView) playlistView.findViewById(R.id.playlistRecyclerView);
        mListAdapter = new AllPlaylistAdapter();
        mListAdapter.setOnMenuClickListener(this);
        mRecyclerView.setAdapter(mListAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mTracker = ((GlobalApp) getActivity().getApplication()).getDefaultTracker();
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

//        mListAdapter.setOnItemClickListener(this);

        playlistView.setFocusableInTouchMode(true);
        playlistView.requestFocus();
        playlistView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey" );

                // go back on the stack?
/*
                if( keyCode == KeyEvent.KEYCODE_BACK ){
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        AllServersFragment fragment = new AllServersFragment();
                        transaction.replace(R.id.sample_content_fragment, fragment);
                        transaction.commit();
                    }
                    return true;
                }
*/
                return false;
            }
        });

        return playlistView;

    }

    public void onMenuClick(int position, View v){
        Log.d(TAG, "onMenuClick position: " + position);

        mCursor.moveToPosition(position);
        mSelectedPlaylistId = mCursor.getInt(NMPContract.PlaylistEntry.COL_PLAYLIST_ID);
        mSelectedPlaylistName = mCursor.getString(NMPContract.PlaylistEntry.COL_PLAYLIST_NAME);
        Log.d(TAG, "onMenuClick mSelectedPlaylistId: " + mSelectedPlaylistId + " mSelectedPlaylistName " + mSelectedPlaylistName);

        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.playlist_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toast.makeText(getContext(), "You selected the action : " + item.getTitle(), Toast.LENGTH_SHORT).show();

                int id = item.getItemId();
                if (id == R.id.action_play_playlist) {

                    GlobalApp globalApp = ((GlobalApp) getActivity().getApplication());
                    MediaControllerCompat mMediaController = globalApp.getMediaController();

                    // if the selected playlist already loaded?
                    if (mSelectedPlaylistId != globalApp.getSongList().getPlaylistId()) {

                        // stop current song
                        if (null != mMediaController) {
                            // pause, move back to the beginning of the song, stop
                            mMediaController.getTransportControls().pause();
                            mMediaController.getTransportControls().seekTo(0);
                            mMediaController.getTransportControls().stop();
                        }

                        // build the new current playlist from the selected playlist
                        globalApp.getSongList().setPlaylistId(getContext(), mSelectedPlaylistId, mSelectedPlaylistName);
                        SharedPrefUtils.getInstance().saveLastPlaylist(mSelectedPlaylistId, mSelectedPlaylistName);
                    } else {
                        //globalApp.getSongList().loadCheck(getContext());
                        globalApp.getSongList().relaodPlaylist();
                    }
                    // switch to Now Playing
                    //globalApp.getSongList().gotoSong(0);
                    //mMediaController.getTransportControls().play();
                    MainActivity.setFragment(MainActivity.FRAGMENT_NOW_PLAYING);
                    return true;

                } else if (id == R.id.action_edit_playlist) {
                    // switch to Playlist Edit page
                    SharedPrefUtils.getInstance().saveEditingPlaylistId(mSelectedPlaylistId);
                    MainActivity.setFragment(MainActivity.FRAGMENT_PLAYLIST);
                    return true;

                } else if (id == R.id.action_delete_playlist) {

                    // pop up a dialog and confirm delete
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Delete Playlist");
                    View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.confirm_playlist_delete, (ViewGroup) getView(), false);
                    //View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.text_input_dialog, (ViewGroup) this.findViewById(android.R.id.content), false);

                    // Set up the input
                    final TextView deleteMessage = (TextView) viewInflated.findViewById(R.id.delete_message);
                    deleteMessage.setText("Deleting Playlist " + mSelectedPlaylistName);
                    // Specify the type of input expected
                    builder.setView(viewInflated);

                    // Set up the buttons
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            deletePlaylist(mSelectedPlaylistId, getActivity());
                            // update the screen to show that the playlist is gone
                            getLoaderManager().initLoader(LIST_LOADER, null, AllPlaylistFragment.this);
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();

                    return true;
                }
                return true;
            }
        });
        popup.show();
    }

    public static void deletePlaylist(int playlistId, Activity activity) {

        // check to see if this is the current playlist
        GlobalApp globalApp = ((GlobalApp) activity.getApplication());
        if (playlistId == globalApp.getSongList().getPlaylistId()) {
            MediaControllerCompat mMediaController = globalApp.getMediaController();

            // stop current song
            if (null != mMediaController) {
                // pause, move back to the beginning of the song, stop
                mMediaController.getTransportControls().pause();
                mMediaController.getTransportControls().seekTo(0);
                mMediaController.getTransportControls().stop();
            }

            // remove the current playlist because it's about to be deleted
            globalApp.getSongList().clear();
        }


        String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + "=?";
        String[] args = {String.valueOf(playlistId)};

        // delete the playlist
        activity.getContentResolver().delete(
                NMPContract.PlaylistEntry.CONTENT_URI,
                selection,
                args
        );

        selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";

        // delete the playlist items
        activity.getContentResolver().delete(
                NMPContract.PlaylistItemEntry.CONTENT_URI,
                selection,
                args
        );
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated ");
        getLoaderManager().initLoader(LIST_LOADER, null, this);
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

        // get all the playlists sorted by name
        String sort = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + " collate nocase asc";
        return new CursorLoader(getActivity(),
            NMPContract.PlaylistEntry.CONTENT_URI,
            null,
            null,
            null,
            sort
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        mListAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.swapCursor(null);
    }

}
