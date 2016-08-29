package com.derekmorrison.networkmusicplayer.ui;


import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.NetworkQueryService;

/**
 * A simple {@link Fragment} subclass.
 */
public class DirectoryFragment extends Fragment  implements LoaderManager.LoaderCallbacks<Cursor>,
        DirectoryAdapter.ClickListener, FileAdapter.ClickListener {
    private static final String TAG = "DirectoryFragment";
    private static final int DIR_LOADER = 0;
    private static final int FILE_LOADER = 1;
    private Cursor mDirCursor = null;
    private Cursor mFileCursor = null;

    protected RecyclerView mDirRecyclerView;
    protected RecyclerView mFileRecyclerView;

    protected DirectoryAdapter mDirectoryAdapter;
    protected FileAdapter mFileAdapter;

    protected RecyclerView.LayoutManager mDirLayoutManager;
    protected RecyclerView.LayoutManager mFileLayoutManager;

    protected NodeNavigator mNodeNavigator;
    protected RadioButton mDirRadioButton;
    protected RadioButton mFileRadioButton;
    protected View mDirHighlight;
    protected View mFileHighlight;

    private ProgressDialog mProgress;
    private int mDirCountDown = 1;
    private int mFileCountDown = 1;

    public DirectoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this is required for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView" );

        // Inflate the layout for this fragment
        View directoryView = inflater.inflate(R.layout.fragment_directory, container, false);

        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();
        MainActivity.setToolbarTitle("Folder Contents");

        mDirRecyclerView = (RecyclerView) directoryView.findViewById(R.id.directoryRecyclerView);
        mDirectoryAdapter = new DirectoryAdapter(getContext());
        mDirRecyclerView.setAdapter(mDirectoryAdapter);

        mDirLayoutManager = new LinearLayoutManager(getActivity());
        mDirRecyclerView.setLayoutManager(mDirLayoutManager);

        mDirectoryAdapter.setOnItemClickListener(this);



        mFileRecyclerView = (RecyclerView) directoryView.findViewById(R.id.fileRecyclerView);
        mFileAdapter = new FileAdapter(getContext());
        mFileRecyclerView.setAdapter(mFileAdapter);

        mFileLayoutManager = new LinearLayoutManager(getActivity());
        mFileRecyclerView.setLayoutManager(mFileLayoutManager);

        mFileAdapter.setOnItemClickListener(this);

        directoryView.setFocusableInTouchMode(true);
        directoryView.requestFocus();
        directoryView.setOnKeyListener(new View.OnKeyListener() {
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

        mDirRadioButton = (RadioButton) directoryView.findViewById(R.id.folderButton);
        mFileRadioButton = (RadioButton) directoryView.findViewById(R.id.songButton);
        mDirHighlight = directoryView.findViewById(R.id.hFolder);
        mFileHighlight = directoryView.findViewById(R.id.hSong);

        mDirRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text));
                mFileRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text_unfocused));
                mDirHighlight.setVisibility(View.VISIBLE);
                mFileHighlight.setVisibility(View.INVISIBLE);
                mDirRecyclerView.setVisibility(View.VISIBLE);
                mFileRecyclerView.setVisibility(View.GONE);
            }
        });

        mFileRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text_unfocused));
                mFileRadioButton.setTextColor(ContextCompat.getColor(getContext(), R.color.tab_text));
                mDirHighlight.setVisibility(View.INVISIBLE);
                mFileHighlight.setVisibility(View.VISIBLE);
                mDirRecyclerView.setVisibility(View.GONE);
                mFileRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        return directoryView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.directory_frag, menu);
    }

    private void goUpLevel() {
        DirNode parent = mNodeNavigator.goBack();
        if (NMPDbHelper.NODE_TYPE_SERVER == parent.getNodeType()) {
            // go to share
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            ShareFragment fragment = new ShareFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else {

            getLoaderManager().restartLoader(DIR_LOADER, null, this);
            getLoaderManager().restartLoader(FILE_LOADER, null, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_up_level) {
            //goUpDirectory();
            //int cNodeType = mNodeNavigator.getCurrentNode().getNodeType();

            goUpLevel();

/*
            DirNode parent = mNodeNavigator.goBack();
            if (NMPDbHelper.NODE_TYPE_SERVER == parent.getNodeType()) {
                // go to share
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                ShareFragment fragment = new ShareFragment();
                transaction.replace(R.id.sample_content_fragment, fragment);
                transaction.commit();
            }

            getLoaderManager().restartLoader(DIR_LOADER, null, this);
            getLoaderManager().restartLoader(FILE_LOADER, null, this);
*/

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(int nodeType, int position, View v) {
        Log.d(TAG, "onItemClick nodeType: " + nodeType + " position: " + position);

        if (NMPDbHelper.NODE_TYPE_DIRECTORY == nodeType) {

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
        }

        if (NMPDbHelper.NODE_TYPE_FILE == nodeType) {

            // grab the info from the selected node and create a DirNode to store it
            mFileCursor.moveToPosition(position);
            SongNode sNode = new SongNode();

            sNode.setId(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_ID));
            sNode.setParentId(mFileCursor.getInt(NMPContract.SongEntry.COL_PARENT_ID));

            String fileName = mFileCursor.getString(NMPContract.SongEntry.COL_FILE_NAME);
            sNode.setFileName(fileName);

            String filePath = mNodeNavigator.getCurrentNode().getFilePath() + fileName;
            sNode.setFilePath(filePath);

            // there's no point in getting more data if the file has not been deep scanned yet
            sNode.setDeepScan(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_DEEP_SCAN));

            if (1 == sNode.getDeepScan()) {
                sNode.setDuration(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_DURATION));
                sNode.setLastPlayed(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_LAST_PLAYED));
                sNode.setPlayCount(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_PLAY_COUNT));
                sNode.setTrack(mFileCursor.getInt(NMPContract.SongEntry.COL_SONG_TRACK));
                sNode.setAlbum(mFileCursor.getString(NMPContract.SongEntry.COL_SONG_ALBUM));
                sNode.setArtUrl(mFileCursor.getString(NMPContract.SongEntry.COL_SONG_ART_URL));
                sNode.setArtist(mFileCursor.getString(NMPContract.SongEntry.COL_SONG_ARTIST));
                sNode.setGenre(mFileCursor.getString(NMPContract.SongEntry.COL_SONG_GENRE));
                sNode.setTitle(mFileCursor.getString(NMPContract.SongEntry.COL_SONG_TITLE));
            }

            mNodeNavigator.setSongNode(sNode);

            // todo go to the Now Playing screen
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            NowPlayingFragment fragment = new NowPlayingFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();

        }

        // just refresh with the new node
        getLoaderManager().restartLoader(DIR_LOADER, null, this);
        getLoaderManager().restartLoader(FILE_LOADER, null, this);




/*
        if (null != dest && dest.getNodeType() == NMPDbHelper.NODE_TYPE_SHARE) {
            // now move to the folder navigation screen
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DirectoryFragment fragment = new DirectoryFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else {
            // todo error - server children should be shares
        }
*/
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated ");
        getLoaderManager().initLoader(DIR_LOADER, null, this);
        getLoaderManager().initLoader(FILE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> loader = null;
        Log.d(TAG, "onCreateLoader LOADER: " + i);
/*
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String currentId = sp.getString("CurrentId", "0");
        mCurrentNodeId = Integer.valueOf(currentId);
        Log.d(TAG, "onCreateLoader ******>>>  CurrentId from SharedPrefs = " + currentId);
*/
        if (DIR_LOADER == i) {
            String dirColumns = NMPContract.NodeEntry.COLUMN_PARENT_ID + "=?";
            int currentNodeId = mNodeNavigator.getCurrentNode().getId();
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
            String dirColumns = NMPContract.NodeEntry.COLUMN_PARENT_ID + "=?";
            int currentNodeId = mNodeNavigator.getCurrentNode().getId();
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

        //if (null == data) { return; }

        String count = " (" + String.valueOf(data.getCount()) + ")";
        if (DIR_LOADER ==  loader.getId()) {
            mDirCursor = data;
            mDirectoryAdapter.swapCursor(data);
            mDirRadioButton.setText(getResources().getString(R.string.folders_rb_label) + count);
            Log.d(TAG, "onLoadFinished : FOLDERS" + count + " mDirCountDown: " + mDirCountDown );

            // To dismiss the dialog
            mDirCountDown--;
            if (mDirCountDown < 0 && null != mProgress && mProgress.isShowing()) {
                mProgress.dismiss();
            }
        }

        if (FILE_LOADER ==  loader.getId()) {
            mFileCursor = data;
            mFileAdapter.swapCursor(data);
            mFileRadioButton.setText(getResources().getString(R.string.songs_rb_label) + count);
            Log.d(TAG, "onLoadFinished : SONGS" + count + " mFileCountDown: " + mFileCountDown);

            if (null != mProgress) {
                Log.d(TAG, "onLoadFinished : showing: " + mProgress.isShowing());
            }

            // To dismiss the dialog
            mFileCountDown--;
            if (mFileCountDown < 0 && null != mProgress && mProgress.isShowing()) {
                mProgress.dismiss();
            }

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDirectoryAdapter.swapCursor(null);

    }
}
