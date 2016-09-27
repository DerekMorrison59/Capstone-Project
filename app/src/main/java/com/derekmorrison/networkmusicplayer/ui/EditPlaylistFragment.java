package com.derekmorrison.networkmusicplayer.ui;


import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;
import com.derekmorrison.networkmusicplayer.sync.ScanFileService;
import com.derekmorrison.networkmusicplayer.util.Playlist;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.SongListHelper;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * A simple {@link Fragment} subclass.
 */
public class EditPlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        EditPlaylistAdapter.MenuClickListener {

    private static final String TAG = "EditPlaylistFragment";
    private static final int LIST_LOADER = 1;

    private int mPlaylistId;
    private String mPlaylistName = "NONE";
    private static String mNewPlaylist = "";

    private EditPlaylistAdapter mEditPlaylistAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;

    private Cursor mCursor;
    private Tracker mTracker;

    public EditPlaylistFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View editPlaylist =  inflater.inflate(R.layout.fragment_edit_playlist, container, false);

        // this is required for this fragment to handle menu events
        setHasOptionsMenu(true);

        mEditPlaylistAdapter = new EditPlaylistAdapter(getContext());
        mEditPlaylistAdapter.setOnMenuClickListener(this);

        mRecyclerView = (RecyclerView) editPlaylist.findViewById(R.id.editPlaylistRecyclerView);
        mRecyclerView.setAdapter(mEditPlaylistAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mTracker = ((GlobalApp) getActivity().getApplication()).getDefaultTracker();
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // capture the 'back' key and direct the user to the All Playlist screen
        editPlaylist.setFocusableInTouchMode(true);
        editPlaylist.requestFocus();
        editPlaylist.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey -> go to ALL_PLAYLIST" );

                if( keyCode == KeyEvent.KEYCODE_BACK ){
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        MainActivity.setFragment(MainActivity.FRAGMENT_ALL_PLAYLIST);
                    }
                    return true;
                }
                return false;
            }
        });

        return editPlaylist;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.current_playlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle save playlist action here.
        int id = item.getItemId();

        // play this playlist
        if (id == R.id.action_e_play_playlist) {
            GlobalApp globalApp = ((GlobalApp) getActivity().getApplication());
            MediaControllerCompat mMediaController = globalApp.getMediaController();

            Log.d(TAG, "Play Playlist: " + mPlaylistId + mPlaylistId + " mPlaylistName: " + mPlaylistName );

            // if the selected playlist already loaded?
            if (mPlaylistId != globalApp.getSongList().getPlaylistId()) {
                Log.d(TAG, "Play Playlist: NEW PLAYLIST");

                // stop current song
                if (null != mMediaController) {
                    // pause, move back to the beginning of the song, stop
                    mMediaController.getTransportControls().pause();
                    mMediaController.getTransportControls().seekTo(0);
                    mMediaController.getTransportControls().stop();
                }

                // build the new current playlist from the selected playlist

                // tell SongList which playlist is 'current'
                globalApp.getSongList().setPlaylistId(getContext(), mPlaylistId, mPlaylistName);


//                Playlist selectedPlaylist = new Playlist();
//                selectedPlaylist.loadFromDb(getContext(), mPlaylistId, mPlaylistName);
//                globalApp.getSongList().loadFromPlaylist(selectedPlaylist, globalApp);

                // remember this as the last playlist played
                SharedPrefUtils.getInstance().saveLastPlaylist(mPlaylistId, mPlaylistName);

//                SharedPrefUtils.getInstance().saveLastPlaylistId(mPlaylistId);
//                SharedPrefUtils.getInstance().saveLastPlaylistName(mPlaylistName);
            } else {
                Log.d(TAG, "Play Playlist: RELOAD PLAYLIST");

                //globalApp.getSongList().loadCheck(getContext());
                globalApp.getSongList().relaodPlaylist();
            }

            // switch to Now Playing
            //globalApp.getSongList().gotoSong(0);
            //mMediaController.getTransportControls().play();
            MainActivity.setFragment(MainActivity.FRAGMENT_NOW_PLAYING);

            return true;
        } else if (id == R.id.action_e_save_playlist_as) {

            // pop up a dialog and ask for a playlist name
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("New Playlist");
            View view = LayoutInflater.from(getContext()).inflate(R.layout.text_input_dialog, (ViewGroup) getView(), false);
            //View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.text_input_dialog, (ViewGroup) this.findViewById(android.R.id.content), false);

            // Set up the input
            final EditText input = (EditText) view.findViewById(R.id.input);

            // Specify the type of input expected
            builder.setView(view);

            // Set up the buttons
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mNewPlaylist = input.getText().toString();
                    saveNewPlaylist(mNewPlaylist);
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
        } else if (id == R.id.action_e_copy_playlist) {

            int col_song_id = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_ID);
            int col_file_name = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_FILE_NAME);
            int col_parent_id = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_PARENT_ID);

            int size = mCursor.getCount();
            mCursor.moveToFirst();
            for (int i = 0; i < size; i++) {

                // get the songId and the parentId
                int songDbId = mCursor.getInt(col_song_id);
                int parentId = mCursor.getInt(col_parent_id);
                String filename = mCursor.getString(col_file_name);

                // get the path from the parent
                String selection = NMPContract.NodeEntry.COLUMN_NODE_ID + "=?";
                String[] args = {String.valueOf(parentId)};
                Cursor nodeCursor = getContext().getContentResolver().query(
                        NMPContract.NodeEntry.CONTENT_URI,
                        null,
                        selection,
                        args,
                        null
                );

                String path = "";
                if (null != nodeCursor && true == nodeCursor.moveToFirst()) {
                    path = nodeCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH);
                }

                CopyFileService.startCopyFile(getContext(), songDbId, path + filename, SongListHelper.SONG_LIST_NONE);
                mCursor.moveToNext();
            }
            return true;

/*
                GlobalApp globalApp = ((GlobalApp) getActivity().getApplication());
            MediaControllerCompat mMediaController = globalApp.getMediaController();

            // if the selected playlist already loaded?
            if (mPlaylistId != globalApp.getSongList().getPlaylistId()) {

                // stop current song
                if (null != mMediaController) {
                    // pause, move back to the beginning of the song, stop
                    mMediaController.getTransportControls().pause();
                    mMediaController.getTransportControls().seekTo(0);
                    mMediaController.getTransportControls().stop();
                }

                // add all songs from this playlist to the SongList
                Playlist selectedPlaylist = new Playlist();
                selectedPlaylist.loadFromDb(getContext(), mPlaylistId, mPlaylistName);
                globalApp.getSongList().loadFromPlaylist(selectedPlaylist, globalApp);
*/

/*
                globalApp.getSongList().clear();

                // get the column numbers of the important items
                int col_album = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_ALBUM);
                int col_artist = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_ARTIST);
                int col_title = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_TITLE);
                int col_duration = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_DURATION);
                int col_year = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_YEAR);
                int col_track = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_TRACK);
                int col_genre = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_GENRE);
                int col_art_uri = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_ART_URL);


                int size = mCursor.getCount();
                mCursor.moveToFirst();
                for (int i = 0; i < size; i++) {
                    // copy data from cursor to MediaMetadataCompat newSong
                    @SuppressWarnings("WrongConstant")
                    MediaMetadataCompat newTrack = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                            .putString(ScanFileService.CUSTOM_METADATA_TRACK_SOURCE, "")
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mCursor.getString(col_album))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mCursor.getString(col_artist))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mCursor.getInt(col_duration))
                            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, mCursor.getInt(col_year))
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, mCursor.getString(col_genre))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,  mCursor.getString(col_art_uri))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  mCursor.getString(col_title))
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,  mCursor.getInt(col_track))
                            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mCursor.getInt(col_track))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, mCursor.getInt(col_track))
                            .build();

                    globalApp.getSongList().addSong(newTrack, getContext());
                }

                SharedPrefUtils.getInstance().saveLastPlaylistId(mPlaylistId);
            }

            // switch to Now Playing
            //globalApp.getSongList().gotoSong(0);
            //mMediaController.getTransportControls().play();
            MainActivity.setFragment(MainActivity.FRAGMENT_NOW_PLAYING);

            return true;
*/
        } else if (id == R.id.action_e_delete_playlist) {
            // delete this playlist
            AllPlaylistFragment.deletePlaylist(mPlaylistId, getActivity());
            MainActivity.setFragment(MainActivity.FRAGMENT_ALL_PLAYLIST);
            return true;
        }

        //


        return super.onOptionsItemSelected(item);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated ");
        getLoaderManager().initLoader(LIST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void saveNewPlaylist(String playlistName) {

        String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + "=?";
        String[] args = {playlistName};
        Cursor listNameCursor = getContext().getContentResolver().query(
                NMPContract.PlaylistEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        Playlist playlist = new Playlist();
        playlist.setPlayListName(playlistName);

        int playlistID;

        if (null != listNameCursor && listNameCursor.moveToFirst()) {
            // name already exists
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setTitle("Error: Playlist '" + playlistName + "' already exists.");
            alertDialogBuilder.setMessage("Tap OK to overwrite existing playlist");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.button_ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            alertDialogBuilder.setNegativeButton(
                    getResources().getString(R.string.button_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            return;
                        }
                    });
            AlertDialog alertDialogError = alertDialogBuilder.create();
            alertDialogError.show();

            // get the existing playlist ID
            playlistID = listNameCursor.getInt(NMPContract.PlaylistEntry.COL_PLAYLIST_ID);

            if (null != listNameCursor) { listNameCursor.close(); }

            // update the playlist name
//            selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + "=?";
//            args[0] = String.valueOf(playlistID);
//            ContentValues listValues = new ContentValues(1);
//            listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, playlistName);
//
//            getContext().getContentResolver().update(
//                    NMPContract.PlaylistEntry.CONTENT_URI,
//                    listValues,
//                    selection,
//                    args
//            );

            // delete the existing members of the playlist
            selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + "=?";
            args[0] = String.valueOf(playlistID);
            getContext().getContentResolver().delete(
                    NMPContract.PlaylistItemEntry.CONTENT_URI,
                    selection,
                    args
            );

        } else {
            // get new playlist ID
            playlistID = SharedPrefUtils.getInstance().getNextPlaylistId();
        }

        // save the playlist items
        playlist.setPlaylistId(playlistID, getContext());
        playlist.saveToDb(getContext());

        if (null != listNameCursor) { listNameCursor.close(); }
    }


    public void onMenuClick(int position, View v) {
        Log.d(TAG, "onMenuClick position: " + position);

        final int mPosition = position;

        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.playlist_item_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toast.makeText(getContext(), "You selected the action : " + item.getTitle(), Toast.LENGTH_SHORT).show();

                GlobalApp globalApp = ((GlobalApp) getActivity().getApplication());
                MediaControllerCompat mMediaController = globalApp.getMediaController();

                int id = item.getItemId();
                if (id == R.id.action_remove_song) {

                    // if this playlist is currently playing then stop it
                    //int currentPlaylistId =  globalApp.getSongList().getPlaylistId();

                    if (mPlaylistId == globalApp.getSongList().getPlaylistId()) {
                        // pause, move back to the beginning of the song, stop
                        mMediaController.getTransportControls().pause();
                        mMediaController.getTransportControls().seekTo(0);
                        mMediaController.getTransportControls().stop();
                    }

                    // remove the song from the playlist
                    mCursor.moveToPosition(mPosition);
                    int col_songId = mCursor.getColumnIndex(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);
                    int songDbId = mCursor.getInt(col_songId);

                    String selection = NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID + " =? AND "
                            + NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID + " =?";
                    String[] args = {String.valueOf(mPlaylistId), String.valueOf(songDbId)};

                    getContext().getContentResolver().delete(
                            NMPContract.PlaylistItemEntry.CONTENT_URI,
                            selection,
                            args
                    );

                    // make sure the songList stays up to date
                    if (mPlaylistId == globalApp.getSongList().getPlaylistId()) {
                        globalApp.getSongList().relaodPlaylist();
                    }

                    getLoaderManager().restartLoader(LIST_LOADER, null, EditPlaylistFragment.this);

                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        mPlaylistId = SharedPrefUtils.getInstance().getEditingPlaylistId();
        Log.d(TAG, "onCreateLoader Playlist ID: " + mPlaylistId);

        // get all the members of the playlist
        return new CursorLoader(getActivity(),
                NMPContract.PLAYLIST_ITEM_SONG_CONTENT_URI,
                null,
                String.valueOf(mPlaylistId),
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        mEditPlaylistAdapter.swapCursor(data);

        if ("NONE" == mPlaylistName || 1 > mPlaylistName.length()) {

            // get the playlist name from the PlayList table
            String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID + "=?";
            String[] args = {String.valueOf(mPlaylistId)};

            Cursor playlistCursor = getContext().getContentResolver().query(
                    NMPContract.PlaylistEntry.CONTENT_URI,
                    null,
                    selection,
                    args,
                    null
            );

            String playlistName = "No Name " + String.valueOf(mPlaylistId);
            if (null != playlistCursor && true == playlistCursor.moveToFirst()) {
                playlistName = playlistCursor.getString(NMPContract.PlaylistEntry.COL_PLAYLIST_NAME);
            }

            mPlaylistName = playlistName;
        }

        MainActivity.setToolbarTitle(mPlaylistName);

        Log.d(TAG, "onLoadFinished playlist Name: " + mPlaylistName);

/*
        if (null != mCursor && 0 <  mCursor.getCount()) {

            int size = mCursor.getCount();

            int col_songId = mCursor.getColumnIndex(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);;
            int col_songFile = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_FILE_NAME);
            int col_deepScan = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_DEEP_SCAN);

            int songId;
            String fileName;
            int deepScan;

            while (mCursor.moveToNext()) {
                songId = mCursor.getInt(col_songId);
                fileName = mCursor.getString(col_songFile);
                deepScan = mCursor.getInt(col_deepScan);
            }
        }
*/

        // use mPlaylistId to get the playlist name

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEditPlaylistAdapter.swapCursor(null);
    }


}
