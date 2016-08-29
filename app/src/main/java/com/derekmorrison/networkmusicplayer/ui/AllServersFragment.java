package com.derekmorrison.networkmusicplayer.ui;


import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllServersFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ServerAdapter.ClickListener {
    private static final String TAG = "AllServersFragment";
    private static final int DIR_LOADER = 0;
    private Cursor mCursor = null;

    protected RecyclerView mRecyclerView;
    protected ServerAdapter mServerAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected NodeNavigator mNodeNavigator;

    public AllServersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView" );

        // Inflate the layout for this fragment
        View allServersView = inflater.inflate(R.layout.fragment_all_servers, container, false);

        setUpNodeNavigator();
        MainActivity.setToolbarTitle("Servers");

        mRecyclerView = (RecyclerView) allServersView.findViewById(R.id.serverRecyclerView);
        mServerAdapter = new ServerAdapter(getContext());
        mRecyclerView.setAdapter(mServerAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mServerAdapter.setOnItemClickListener(this);

        allServersView.setFocusableInTouchMode(true);
        allServersView.requestFocus();
        allServersView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey" );
                if( keyCode == KeyEvent.KEYCODE_BACK ){
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        InitialScanFragment fragment = new InitialScanFragment();
                        transaction.replace(R.id.sample_content_fragment, fragment);
                        transaction.commit();
                    }
                    return true;
                }
                return false;
            }
        });

        return allServersView;
    }

    private void setUpNodeNavigator() {
        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();

        // add the domain node to the navigator
        Cursor directoryCursor = null;

        String directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
        String[] directoryIds = {String.valueOf(NMPDbHelper.NODE_TYPE_DOMAIN)};

        directoryCursor = getActivity().getContentResolver().query(
                NMPContract.NodeEntry.CONTENT_URI,
                null,
                directoryColumns,
                directoryIds,
                null
        );

        // get the details for the DOMAIN node
        if (null != directoryCursor && directoryCursor.getCount() > 0) {
            directoryCursor.moveToFirst();
            DirNode newNode = new DirNode(
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
            );

            mNodeNavigator.resetHead(newNode);

            if (null != directoryCursor) {
                directoryCursor.close();
            }
        }
}

    @Override
    public void onItemClick(int position, View v) {
        Log.d(TAG, "onItemClick position: " + position);

        // grab the info from the selected node and create a DirNode to store it
        mCursor.moveToPosition(position);

        DirNode newNode = new DirNode(
                mCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                mCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                mCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                mCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                mCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                mCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
        );

        DirNode dest = mNodeNavigator.addNode(newNode);

        if (null != dest && dest.getNodeType() == NMPDbHelper.NODE_TYPE_SERVER) {
            // now move to the shares screen
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            ShareFragment fragment = new ShareFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        } else {
            // todo error - server children should be shares
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated ");
        getLoaderManager().initLoader(DIR_LOADER, null, this);
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

        String dirColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + "=?";
        String[] dirIds = {String.valueOf(NMPDbHelper.NODE_TYPE_SERVER)};
        String sort = NMPContract.NodeEntry.COLUMN_NODE_NAME + " COLLATE NOCASE ASC";
        return new CursorLoader(getActivity(),
                NMPContract.NodeEntry.CONTENT_URI,
                null,
                dirColumns,
                dirIds,
                sort
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
/*
        // get the latest parent ID
        if (null != mCursor && mCursor.moveToNext()) {

            mCurrentParentId = data.getInt(SmbFileDbHelper.COL_NODE_ID);
            Log.d(TAG, "onLoadFinished ******  mCurrentParentId = " + mCurrentParentId);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("CurrentParentId", String.valueOf(mCurrentParentId));
            editor.apply();

        }
*/
        mServerAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mServerAdapter.swapCursor(null);
    }


}