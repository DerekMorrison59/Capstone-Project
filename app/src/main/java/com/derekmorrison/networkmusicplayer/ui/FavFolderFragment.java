package com.derekmorrison.networkmusicplayer.ui;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavFolderFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
    FavFolderAdapter.ClickListener {

    private static final String TAG = "FavFolderFragment";

    private static final int DIR_LOADER = 1;
    private Cursor mDirCursor = null;


    protected LinearLayoutManager mDirLayoutManager;
    protected RecyclerView mDirRecyclerView;
    private FavFolderAdapter mFavFolderAdapter;
    private NodeNavigator mNodeNavigator;


    public FavFolderFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
//        Log.d(TAG, "onActivityCreated ");
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(DIR_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View favView = inflater.inflate(R.layout.fragment_fav_folder, container, false);

        MainActivity.setToolbarTitle("Favorite Folders");

        mDirRecyclerView = (RecyclerView) favView.findViewById(R.id.favFolderRecyclerView);
        mFavFolderAdapter = new FavFolderAdapter(getContext());
        mDirRecyclerView.setAdapter(mFavFolderAdapter);

        mDirLayoutManager = new LinearLayoutManager(getActivity());
        mDirRecyclerView.setLayoutManager(mDirLayoutManager);

        mFavFolderAdapter.setOnItemClickListener(this);
        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();

        return favView;
    }

    @Override
    public void onItemClick(int position, View v) {
        Log.d(TAG, "onItemClick position: " + position);

        if (null != mDirCursor && mDirCursor.moveToPosition(position)) {
            int nodeId = mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID);

            Log.d(TAG, "onItemClick nodeId: " + nodeId);


            DirNode dn = new DirNode(nodeId,
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                    mDirCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                    mDirCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                    mDirCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
            );

            mNodeNavigator.moveToNewNode(dn);
            MainActivity.setFragment(MainActivity.FRAGMENT_DIRECTORY);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        Log.d(TAG, "onCreateLoader LOADER: " + i);

        String dirColumns = NMPContract.NodeEntry.COLUMN_NODE_IS_FAV + "=?";
        String[] dirIds = {String.valueOf(1)};
        String sort = NMPContract.NodeEntry.COLUMN_NODE_NAME + " collate nocase asc";

        Loader<Cursor> loader = new CursorLoader(getActivity(),
                NMPContract.NodeEntry.CONTENT_URI,
                null,
                dirColumns,
                dirIds,
                sort
        );

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mDirCursor = data;
        mFavFolderAdapter.swapCursor(data);

//        String count = " (" + String.valueOf(data.getCount()) + ")";
//        if (DIR_LOADER ==  loader.getId()) {
//            mDirCursor = data;
//            mDirectoryAdapter.swapCursor(data);
//            mDirRadioButton.setText(getResources().getString(R.string.folders_rb_label) + count);
////            Log.d(TAG, "onLoadFinished : FOLDERS" + count + " mDirCountDown: " + mDirCountDown );
//
//            // To dismiss the dialog
//            mDirCountDown--;
//            if (mDirCountDown < 0 && null != mProgress && mProgress.isShowing()) {
//                mProgress.dismiss();
//            }
//            if (mDirCountDown < -1) { mDirCountDown = -1; }
//        }
//
//        if (FILE_LOADER ==  loader.getId()) {
//            mFileCursor = data;
//            mFileAdapter.swapCursor(data);
//            mFileRadioButton.setText(getResources().getString(R.string.songs_rb_label) + count);
////            Log.d(TAG, "onLoadFinished : SONGS" + count + " mFileCountDown: " + mFileCountDown);
//
//            if (null != mProgress) {
////                Log.d(TAG, "onLoadFinished : showing: " + mProgress.isShowing());
//            }
//
//            // To dismiss the dialog
//            mFileCountDown--;
//            if (mFileCountDown < 0 && null != mProgress && mProgress.isShowing()) {
//                mProgress.dismiss();
//            }
//            if (mFileCountDown < -1) { mFileCountDown = -1; }
//        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFavFolderAdapter.swapCursor(null);
    }
}
