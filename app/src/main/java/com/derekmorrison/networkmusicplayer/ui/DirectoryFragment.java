package com.derekmorrison.networkmusicplayer.ui;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ListViewAutoScrollHelper;
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
import android.widget.RadioButton;
import android.widget.Toast;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.CopyFileService;
import com.derekmorrison.networkmusicplayer.sync.NetworkQueryService;
import com.derekmorrison.networkmusicplayer.util.Playlist;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.SongListHelper;
import com.derekmorrison.networkmusicplayer.util.Utility;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * A simple {@link Fragment} subclass.
 */
public class DirectoryFragment extends Fragment  implements LoaderManager.LoaderCallbacks<Cursor>,
        DirectoryAdapter.ClickListener, FileAdapter.ClickListener, FileAdapter.MenuClickListener,
        BreadCrumbFragment.CrumbClickListener {

    public static final int UPDATE_NO_CHANGE = 0;
    public static final int UPDATE_SHOW_FOLDERS = 1;
    public static final int UPDATE_SHOW_FILES = 2;

    private static final String TAG = "DirectoryFragment";
    private static final int DIR_LOADER = 0;
    private static final int FILE_LOADER = 1;
    private Cursor mDirCursor = null;
    private Cursor mFileCursor = null;

    private String FILE_VISIBLE = "file_visible";
    private String FILE_LIST_KEY = "file_list_key";
    private String DIR_LIST_KEY = "dir_list_key";
    private Parcelable mFileListState;
    private Parcelable mDirListState;

    protected RecyclerView mDirRecyclerView;
    protected RecyclerView mFileRecyclerView;

    protected DirectoryAdapter mDirectoryAdapter;
    protected FileAdapter mFileAdapter;

    protected LinearLayoutManager mDirLayoutManager;
    protected LinearLayoutManager mFileLayoutManager;

    protected NodeNavigator mNodeNavigator;
    protected RadioButton mDirRadioButton;
    protected RadioButton mFileRadioButton;
    protected View mDirHighlight;
    protected View mFileHighlight;
    private BreadCrumbFragment mBreadCrumbFragment;
    private View mDirectoryView;

    private ProgressDialog mProgress;
    private int mDirCountDown = 1;
    private int mFileCountDown = 1;
    private int[] mPlaylistIds;
    private String newPlaylistName;
    private ProgressDialog mAllProgress;
    private int mSelectedPlaylistId = 0;
    private Tracker mTracker;


    public DirectoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Log.d(TAG, "onCreateView" );

        // Inflate the layout for this fragment
        View directoryView = inflater.inflate(R.layout.fragment_directory, container, false);
        mDirectoryView = directoryView;

        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();
        MainActivity.setToolbarTitle("Folder Contents");

        mDirRecyclerView = (RecyclerView) directoryView.findViewById(R.id.directoryRecyclerView);
        mDirectoryAdapter = new DirectoryAdapter(getContext());
        mDirRecyclerView.setAdapter(mDirectoryAdapter);

        mDirLayoutManager = new LinearLayoutManager(getActivity());
        mDirRecyclerView.setLayoutManager(mDirLayoutManager);

        mDirectoryAdapter.setOnItemClickListener(this);


        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        mBreadCrumbFragment = new BreadCrumbFragment();
        transaction.replace(R.id.placeholder_fragment, mBreadCrumbFragment);
        transaction.commit();
        mBreadCrumbFragment.setOnCrumbClickListener(this);

        mTracker = ((GlobalApp) getActivity().getApplication()).getDefaultTracker();
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        mFileRecyclerView = (RecyclerView) directoryView.findViewById(R.id.fileRecyclerView);
        mFileAdapter = new FileAdapter(getContext());
        mFileRecyclerView.setAdapter(mFileAdapter);

        mFileLayoutManager = new LinearLayoutManager(getActivity());
        mFileRecyclerView.setLayoutManager(mFileLayoutManager);

        mFileAdapter.setOnItemClickListener(this);
        mFileAdapter.setOnMenuClickListener(this);

        directoryView.setFocusableInTouchMode(true);
        directoryView.requestFocus();
        directoryView.setOnKeyListener(new View.OnKeyListener() {
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

        mDirRadioButton = (RadioButton) directoryView.findViewById(R.id.folderButton);
        mFileRadioButton = (RadioButton) directoryView.findViewById(R.id.songButton);
        mDirHighlight = directoryView.findViewById(R.id.hFolder);
        mFileHighlight = directoryView.findViewById(R.id.hSong);

        mDirRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                foldersAreVisible();
                SharedPrefUtils.getInstance().saveFoldersDisplayed(true);
            }
        });

        mFileRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filesAreVisible();
                SharedPrefUtils.getInstance().saveFoldersDisplayed(false);
            }
        });

        if (true == SharedPrefUtils.getInstance().getFoldersDisplayed()) {
            foldersAreVisible();
        } else {
            filesAreVisible();
        }

        return directoryView;
    }

    private void foldersAreVisible(){
//        Log.d(TAG, "foldersAreVisible" );

        mDirRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text));
        mFileRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text_unfocused));
        mDirHighlight.setVisibility(View.VISIBLE);
        mFileHighlight.setVisibility(View.INVISIBLE);
        mDirRecyclerView.setVisibility(View.VISIBLE);
        mFileRecyclerView.setVisibility(View.GONE);
    }

    private void filesAreVisible() {
//        Log.d(TAG, "filesAreVisible" );

        mDirRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text_unfocused));
        mFileRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text));
        mDirHighlight.setVisibility(View.INVISIBLE);
        mFileHighlight.setVisibility(View.VISIBLE);
        mDirRecyclerView.setVisibility(View.GONE);
        mFileRecyclerView.setVisibility(View.VISIBLE);
    }


    @Override
    public void onPause() {
        super.onPause();

        // get the current positions of the dir and file lists
        int filePosition = mFileLayoutManager.findFirstVisibleItemPosition();
        int dirPosition = mDirLayoutManager.findFirstVisibleItemPosition();

        // save the positions in shared prefs
        SharedPrefUtils.getInstance().saveFileListPosition(filePosition);
        SharedPrefUtils.getInstance().saveDirListPosition(dirPosition);
//        Log.d(TAG, "onPause: dir position: " + dirPosition + "  file position: " + filePosition);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        Boolean files = mFileRecyclerView.getVisibility() == View.VISIBLE;
//        Log.d(TAG, "onSaveInstanceState, files visible: " + files);

        // either folders or files are visible, this saves a boolean for FILE_VISIBLE
        state.putBoolean(FILE_VISIBLE, files);

        // save the list state for files and folders
//        int filePosition = mFileLayoutManager.findFirstVisibleItemPosition();
//        int dirPosition = mDirLayoutManager.findFirstVisibleItemPosition();
//
//        SharedPrefUtils.getInstance().saveFileListPosition(filePosition);
//        Log.d(TAG, "onSaveInstanceState, position: " + filePosition);
//
//        mFileListState = mFileLayoutManager.onSaveInstanceState();
//        state.putParcelable(FILE_LIST_KEY, mFileListState);
//        mDirListState = mDirLayoutManager.onSaveInstanceState();
//        state.putParcelable(DIR_LIST_KEY, mDirListState);
    }

    private void goUpLevel() {
        DirNode parent = mNodeNavigator.goBack();
/*
        mBreadCrumbFragment.updateNodes();

        Log.d(TAG, "RRRR goUpLevel restarting Loaders" );

        getLoaderManager().restartLoader(DIR_LOADER, null, this);
        getLoaderManager().restartLoader(FILE_LOADER, null, this);
*/
        restartLoadersUpdateBreadCrumbs(UPDATE_NO_CHANGE);
        resetListPositions();
    }

    public void restartLoadersUpdateBreadCrumbs(int showFolders) {
//        Log.d(TAG, "RRRR restartLoadersUpdateBreadCrumbs" );
        mBreadCrumbFragment.updateNodes();

        if (UPDATE_SHOW_FOLDERS == showFolders) {
            foldersAreVisible();
            SharedPrefUtils.getInstance().saveFoldersDisplayed(true);
        } else if (UPDATE_SHOW_FILES == showFolders) {
            filesAreVisible();
            SharedPrefUtils.getInstance().saveFoldersDisplayed(false);
        }

        getLoaderManager().restartLoader(DIR_LOADER, null, this);
        getLoaderManager().restartLoader(FILE_LOADER, null, this);

    }
    private void resetListPositions() {
        // changing levels means resetting the list positions
//        Log.d(TAG, "resetListPositions");
        SharedPrefUtils.getInstance().saveFileListPosition(0);
        SharedPrefUtils.getInstance().saveDirListPosition(0);
    }

    @Override
    public void onCrumbClick(DirNode node, int position, View v) {
        // change the current node in NodeNavigator
        mNodeNavigator.gotoNode(position);
        restartLoadersUpdateBreadCrumbs(UPDATE_NO_CHANGE);

/*
        // update the BreadCrumbFragment
        mBreadCrumbFragment.updateNodes();

        // update this screen
        Log.d(TAG, "RRRR onCrumbClick restarting Loaders" );
        getLoaderManager().restartLoader(DIR_LOADER, null, this);
        getLoaderManager().restartLoader(FILE_LOADER, null, this);
*/
    }

    // this method is called when the pop-up menu (3 dots) is tapped on a song
    @Override
    public void onMenuClick(int position, View v) {
//        Log.d(TAG, "onMenuClick position: " + position);
        //Toast.makeText(getContext(), "You selected menu at position : " + position, Toast.LENGTH_SHORT).show();

        mFileCursor.moveToPosition(position);
        final int songDbId = mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
        ((GlobalApp) getActivity().getApplication()).setTransientSongDbId(songDbId);

        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.song_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toast.makeText(getContext(), "You selected the action : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                SongListHelper listHelper = new SongListHelper();
                int id = item.getItemId();
                if (id == R.id.play_now) {

                    // clear out the existing Current Playlist
                    //((GlobalApp) getActivity().getApplication()).getSongList().clear();

                    // make a new 'temp' playlist and add this song to it

                    // get new playlist ID
                    int newPlaylistId = SharedPrefUtils.getInstance().getNextPlaylistId();

                    // create new name temp + ID
                    String tempPlaylistName = "Temp " + String.valueOf(newPlaylistId);

                    // save the playlist using the new ID and Name
                    ContentValues listValues = new ContentValues();
                    listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, tempPlaylistName);
                    listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID, newPlaylistId);

                    getContext().getContentResolver().insert(
                            NMPContract.PlaylistEntry.CONTENT_URI,
                            listValues
                    );

                    // save the single playlist item using the songDbId
                    listValues = new ContentValues();
                    listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, newPlaylistId);
                    listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, songDbId);

                    getContext().getContentResolver().insert(
                            NMPContract.PlaylistItemEntry.CONTENT_URI,
                            listValues
                    );

                    // then set new playlist on the global songlist
                    ((GlobalApp) getActivity().getApplication()).getSongList().setPlaylistId(getContext(), newPlaylistId, tempPlaylistName);



                    // add the current selection as the first song on the new list
                    listHelper.getMetadataForId(getContext(), songDbId, SongListHelper.SONG_LIST_CURRENT);

//                    Log.d(TAG, "onMenuItemClick PLAY NOW created new playlist: " + tempPlaylistName + " ID: " + newPlaylistId);

                    // add all other songs from the current folder
                    //listHelper.convertCursorToSongList(getContext(), mFileCursor, SongListHelper.SONG_LIST_CURRENT);

                    // switch to Now Playing  Delay and then start playing?
                    MainActivity.setFragment(MainActivity.FRAGMENT_NOW_PLAYING);

                } else if (id == R.id.add_to_playlist) {
                    final String[] items = getListOfPlaylists();

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Add Song to Playlist")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String playlistName = items[which];
                                    addSongToPlaylist(songDbId, which, playlistName);
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                } else if (id == R.id.add_to_new_playlist) {
//                    final String[] items = getListOfPlaylists();
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                    builder.setTitle("Add Song to Playlist")
//                            .setItems(items, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    String playlistName = items[which];
//                                    addSongToPlaylist(songDbId, which, playlistName);
//                                }
//                            });
//
//                    AlertDialog alertDialog = builder.create();
//                    alertDialog.show();


                    // pop up a dialog and ask for a playlist name
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("New Playlist");
                    View view = LayoutInflater.from(getContext()).inflate(R.layout.text_input_dialog, (ViewGroup) getView(), false);

                    // Set up the input
                    final EditText input = (EditText) view.findViewById(R.id.input);

                    // Specify the type of input expected
                    builder.setView(view);

                    // Set up the buttons
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            newPlaylistName = input.getText().toString();
                            saveToNewPlaylist(songDbId, newPlaylistName);
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();


                } else if (id == R.id.add_all_to_playlist) {
                    //listHelper.getMetadataForId(getContext(), songDbId, SongListHelper.SONG_LIST_CURRENT);
                    final String[] items = getListOfPlaylists();

                    if (null == items || 0 == items.length) {
                        return true;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Add Song to Playlist")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String playlistName = items[which];
                                    addAllSongsToPlaylist(which, playlistName);
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                } else if (id == R.id.copy_to_local) {
                    listHelper.getMetadataForId(getContext(), songDbId, SongListHelper.SONG_LIST_NONE);
                }
                return true;
            }
        });
        popup.show();
    }

    private String[] getListOfPlaylists() {
        String[] playlists = null;

        String sort = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + " collate nocase asc";
        // get playlists from Db
        Cursor playlistCursor = getContext().getContentResolver().query(
                NMPContract.PlaylistEntry.CONTENT_URI,
                null,
                null,
                null,
                sort
        );

        if (null != playlistCursor & playlistCursor.getCount() > 0) {

            int size = playlistCursor.getCount();
            playlists = new String[size];
            mPlaylistIds = new int[size];
            for (int i = 0; i < size; i++) {
                playlistCursor.moveToNext();
                playlists[i] = playlistCursor.getString(NMPContract.PlaylistEntry.COL_PLAYLIST_NAME);
                mPlaylistIds[i] = playlistCursor.getInt(NMPContract.PlaylistEntry.COL_PLAYLIST_ID);
            }
        }

        return playlists;
    }

    private void addSongToPlaylist(int songDbId, int selected, String playlistName) {
//        Log.d(TAG, "addSongToPlaylist songDbId: " + songDbId + " selected: " + selected + " playlist: " + playlistName);
        if (null == mPlaylistIds || 0 == mPlaylistIds.length) {
            return;
        }

        int selectedPlaylistId = mPlaylistIds[selected];

        ContentValues listValues = new ContentValues();
        listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, selectedPlaylistId);
        listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, songDbId);

        getContext().getContentResolver().insert(
                NMPContract.PlaylistItemEntry.CONTENT_URI,
                listValues
        );

        // if this is the current playlist then add it to the current SongList
        if (((GlobalApp) getActivity().getApplication()).getSongList().getPlaylistId() == selectedPlaylistId) {
//            Log.d(TAG, "addSongToPlaylist *** adding song to SongList *** songDbId: " + songDbId);
            ((GlobalApp) getActivity().getApplication()).getSongList().addSong(songDbId);
        }

//        Playlist selectedPlaylist = new Playlist();
//        selectedPlaylist.setPlaylistId(mPlaylistIds[selected], getContext());
//        selectedPlaylist.addSong(songDbId, getContext());
//        selectedPlaylist.saveToDb(getContext());

        Snackbar.make(mDirectoryView, "Song added to playlist",
                Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();


    }

    private class AddAllSongsToPlaylist extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... playlistName) {

            if (null == mFileCursor || 0 >= mFileCursor.getCount()) {
                return "";
            }

            String songTitle;
            String fileName;
            int songCount = mFileCursor.getCount();
            int songDbId;
            ContentValues listValues;

//            Log.d(TAG, "doInBackground - mSelectedPlaylistId: " + mSelectedPlaylistId);

            mFileCursor.moveToFirst();

            for (int i = 0; i < songCount; i++) {
                songDbId = mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
                songTitle = mFileCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE);
                fileName = mFileCursor.getString(NMPContract.SongEntry.COL_FILE_NAME);
//                Log.d(TAG, "doInBackground - filename: " + fileName + " title: " + songTitle + " songId: " + songDbId);

                listValues = new ContentValues();
                listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, mSelectedPlaylistId);
                listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, songDbId);

                getContext().getContentResolver().insert(
                        NMPContract.PlaylistItemEntry.CONTENT_URI,
                        listValues
                );

                // if this is the current playlist then add it to the current SongList
                if (((GlobalApp) getActivity().getApplication()).getSongList().getPlaylistId() == mSelectedPlaylistId) {
//                    Log.d(TAG, "AddAllSongsToPlaylist  doInBackground *** adding song to SongList *** songDbId: " + songDbId);
                    ((GlobalApp) getActivity().getApplication()).getSongList().addSong(songDbId);
                }

                listValues.clear();
                listValues = null;
                mFileCursor.moveToNext();
            }


/*

            Playlist selectedPlaylist = new Playlist();
            selectedPlaylist.setPlaylistId(mSelectedPlaylistId, getContext());
            selectedPlaylist.loadFromDb(getContext(), mSelectedPlaylistId); //, playlistName);

            int songDbId;
            int songCount = 0;

            if (null != mFileCursor && true == mFileCursor.moveToFirst()) {

                songCount = mFileCursor.getCount();

                for (int i = 0; i < songCount; i++) {

                    songDbId = mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_ID);
                    selectedPlaylist.addSong(songDbId, getContext());
                    Log.d(TAG, "Adding songDbId: " + songDbId);
                    mFileCursor.moveToNext();
                }
            }

            selectedPlaylist.saveToDb(getContext());
*/

            return "";
        }

        @Override
        protected void onPostExecute(String result) {

            if (null != mAllProgress && mAllProgress.isShowing()) {
//                Log.d(TAG, " >>>>>>> addAllSongsToPlaylist Dialog Dismissed - all songs added ");
                mAllProgress.dismiss();
            }

            Snackbar.make(mDirectoryView, "All songs added to playlist",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        }
    }

    private void addAllSongsToPlaylist(int selected, String playlistName) {
        mSelectedPlaylistId = mPlaylistIds[selected];
//        Log.d(TAG, "addAllSongsToPlaylist selected: " + selected + " playlist: " + playlistName + " playlistId: " + mSelectedPlaylistId);

        mAllProgress = new ProgressDialog(getContext());
        mAllProgress.setTitle("Adding");
        mAllProgress.setMessage("Adding All Songs to Playlist");
        mAllProgress.show();

        AddAllSongsToPlaylist task = new AddAllSongsToPlaylist();
        task.execute("bob");
    }

    private void saveToNewPlaylist(int songDbId, String playlistName){
        // check for duplicate Name
        String selection = NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + "=?";
        String[] args = {playlistName};
        Cursor listNameCursor = getContext().getContentResolver().query(
                NMPContract.PlaylistEntry.CONTENT_URI,
                null,
                selection,
                args,
                null
        );

        int playlistID;
//        Playlist playlist = new Playlist();

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

//            // update the playlist name
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

            // now delete the existing members of the playlist
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

            // create a new playlist
            ContentValues listValues = new ContentValues();
            listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_ID, playlistID);
            listValues.put(NMPContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, playlistName);

            getContext().getContentResolver().insert(
                    NMPContract.PlaylistEntry.CONTENT_URI,
                    listValues
            );
        }

        // insert single record to new or empty playlist
        ContentValues listValues = new ContentValues();
        listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_LIST_ID, playlistID);
        listValues.put(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID, songDbId);

        getContext().getContentResolver().insert(
                NMPContract.PlaylistItemEntry.CONTENT_URI,
                listValues
        );

        // make sure the file gets copied from the network
        SongListHelper listHelper = new SongListHelper();
        listHelper.getMetadataForId(getContext(), songDbId, SongListHelper.SONG_LIST_CURRENT);

        if (null != listNameCursor) { listNameCursor.close(); }

    }

    // this method is called when a Folder or Song is tapped
    @Override
    public void onItemClick(int nodeType, int position, View v) {
//        Log.d(TAG, "onItemClick nodeType: " + nodeType + " position: " + position);

        if (NMPDbHelper.NODE_TYPE_DIRECTORY == nodeType) {

            if (false == Utility.getInstance().isWiFiConnected()) {

                Snackbar.make(MainActivity.getReferenceView(), "WiFi Network is not connected!",
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                return;
            }

            // grab the info from the selected node and create a DirNode to store it
            mDirCursor.moveToPosition(position);

            int nodeStatus = mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS);

            DirNode newNode = new DirNode(
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                    mDirCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                    mDirCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                    nodeStatus
            );

            // has this directory been scanned?
            if (NMPDbHelper.NODE_SCANNED != nodeStatus) {
                // this node needs to be scanned
                NetworkQueryService.startActionScanNode(
                        getContext(),
                        mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                        mDirCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                        1,
                        mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE)
                        );

                mProgress = new ProgressDialog(getContext());
                mProgress.setTitle("Loading");
                mProgress.setMessage("Scanning Folder...");
                mProgress.show();
                mDirCountDown = 1;
                mFileCountDown = 1;
            }

            DirNode dest = mNodeNavigator.addNode(newNode);
            //mBreadCrumbFragment.updateNodes();
            resetListPositions();
        }

/*
        // just refresh with the new node
        Log.d(TAG, "RRRR onItemClick restarting Loaders" );
        getLoaderManager().restartLoader(DIR_LOADER, null, this);
        getLoaderManager().restartLoader(FILE_LOADER, null, this);
*/
        restartLoadersUpdateBreadCrumbs(UPDATE_NO_CHANGE);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
//        Log.d(TAG, "onActivityCreated ");
        super.onActivityCreated(savedInstanceState);

/*
        if (null != savedInstanceState) {

            mFileListState = savedInstanceState.getParcelable(FILE_LIST_KEY);
            mDirListState = savedInstanceState.getParcelable(DIR_LIST_KEY);

            if (true == savedInstanceState.getBoolean(FILE_VISIBLE)) {
                filesAreVisible();
            } else {
                foldersAreVisible();
            }
        }
*/
//        Log.d(TAG, "RRRR onActivityCreated restarting Loaders" );

        DirNode here = mNodeNavigator.getCurrentNode();

        // has this directory been scanned?
        if (here.getNodeStatus() != NMPDbHelper.NODE_SCANNED) {
            // this node needs to be scanned
            NetworkQueryService.startActionScanNode(
                    getContext(),
                    here.getId(),
                    here.getFilePath(),
                    1,
                    here.getNodeType()
            );

            mProgress = new ProgressDialog(getContext());
            mProgress.setTitle("Loading");
            mProgress.setMessage("Scanning Folder...");
            mProgress.show();
            mDirCountDown = 1;
            mFileCountDown = 1;
        }


        getLoaderManager().initLoader(DIR_LOADER, null, this);
        getLoaderManager().initLoader(FILE_LOADER, null, this);

    }

    @Override
    public void onResume() {
        super.onResume();

/*
        if (null != mFileListState) {
            Log.d(TAG, "onResume restoring File LayoutManager");
            mFileLayoutManager.onRestoreInstanceState(mFileListState);
        }
        if (null != mDirListState) {
            Log.d(TAG, "onResume restoring DIR LayoutManager");
            mDirLayoutManager.onRestoreInstanceState(mDirListState);
        }
*/

        int position = SharedPrefUtils.getInstance().getFileListPosition();
        mFileLayoutManager.scrollToPosition(position);
        int dirPosition = SharedPrefUtils.getInstance().getDirListPosition();
        mDirLayoutManager.scrollToPosition(dirPosition);
//        Log.d(TAG, "onResume File LayoutManager: " + position + " Dir: " + dirPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> loader = null;
//        Log.d(TAG, "onCreateLoader LOADER: " + i);

        int currentNodeId = mNodeNavigator.getCurrentNode().getId();

        if (DIR_LOADER == i) {
            String dirColumns = NMPContract.NodeEntry.COLUMN_PARENT_ID + "=?";
            String[] dirIds = {String.valueOf(currentNodeId)};
            String sort = NMPContract.NodeEntry.COLUMN_NODE_NAME + " collate nocase asc";
            loader = new CursorLoader(getActivity(),
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    dirColumns,
                    dirIds,
                    sort
            );
        }
        if (FILE_LOADER == i) {
            String dirColumns = NMPContract.SongEntry.COLUMN_PARENT_ID + "=?";
            String[] dirIds = {String.valueOf(currentNodeId)};
            String sort = NMPContract.SongEntry.COLUMN_FILE_NAME + " collate nocase asc";
            loader = new CursorLoader(getActivity(),
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    dirColumns,
                    dirIds,
                    sort
            );
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        String count = " (" + String.valueOf(data.getCount()) + ")";
        if (DIR_LOADER ==  loader.getId()) {
            mDirCursor = data;
            mDirectoryAdapter.swapCursor(data);
            mDirRadioButton.setText(getResources().getString(R.string.folders_rb_label) + count);
//            Log.d(TAG, "onLoadFinished : FOLDERS" + count + " mDirCountDown: " + mDirCountDown );

            // To dismiss the dialog
            mDirCountDown--;
            if (mDirCountDown < 0 && null != mProgress && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            if (mDirCountDown < -1) { mDirCountDown = -1; }
        }

        if (FILE_LOADER ==  loader.getId()) {
            mFileCursor = data;
            mFileAdapter.swapCursor(data);
            mFileRadioButton.setText(getResources().getString(R.string.songs_rb_label) + count);
//            Log.d(TAG, "onLoadFinished : SONGS" + count + " mFileCountDown: " + mFileCountDown);

            if (null != mProgress) {
//                Log.d(TAG, "onLoadFinished : showing: " + mProgress.isShowing());
            }

            // To dismiss the dialog
            mFileCountDown--;
            if (mFileCountDown < 0 && null != mProgress && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            if (mFileCountDown < -1) { mFileCountDown = -1; }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDirectoryAdapter.swapCursor(null);
    }
}
