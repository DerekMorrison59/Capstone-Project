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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.NetworkQueryService;

/**
 * A simple {@link Fragment} subclass.
 */
public class InitialScanFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>  {

    private static final String TAG = "InitialScanFragment";
    private static final int DIR_LOADER = 0;
    private static final int FILE_LOADER = 1;
    private Cursor mCursor = null;

    private TextView mDomainTV;
    private TextView mServerTV;
    private TextView mShareTV;
    private TextView mFolderTV;
    private TextView mSongTV;
    private TextView mScanTV;
    private Button mServersButton;

    public InitialScanFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View initialScan = inflater.inflate(R.layout.fragment_initial_scan, container, false);

        mDomainTV = (TextView)initialScan.findViewById(R.id.domainCountTV);
        mServerTV = (TextView)initialScan.findViewById(R.id.serverCountTV);
        mShareTV = (TextView)initialScan.findViewById(R.id.shareCountTV);
        mFolderTV = (TextView)initialScan.findViewById(R.id.folderCountTV);
        mSongTV = (TextView)initialScan.findViewById(R.id.songCountTV);
        mScanTV = (TextView)initialScan.findViewById(R.id.scanCountTV);
        mScanTV.setText(String.valueOf(OnboardingFragment.ONBOARDING_SCAN_DEPTH));

        MainActivity.setToolbarTitle("Initial Network Scan");

        mServersButton = (Button)initialScan.findViewById(R.id.all_servers_button);
        mServersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                AllServersFragment fragment = new AllServersFragment();
                transaction.replace(R.id.sample_content_fragment, fragment);
                transaction.commit();
            }
        });

        initialScan.setFocusableInTouchMode(true);
        initialScan.requestFocus();
        initialScan.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey" );
                if( keyCode == KeyEvent.KEYCODE_BACK ){
                    // just stay here
/*
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        InitialScanFragment fragment = new InitialScanFragment();
                        transaction.replace(R.id.sample_content_fragment, fragment);
                        transaction.commit();
                    }
*/
                    return true;
                }
                return false;
            }
        });

        return initialScan;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated " );

        getLoaderManager().initLoader(DIR_LOADER, null, this);
        getLoaderManager().initLoader(FILE_LOADER, null, this);
        //if (mStartCount > 0) getLoaderManager().restartLoader(FILE_LOADER, null, this);
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

        Loader<Cursor> theLoader = null;

        if (DIR_LOADER == i) {
            theLoader = new CursorLoader(getActivity(),
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (FILE_LOADER == i) {
            theLoader = new CursorLoader(getActivity(),
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        return theLoader;

/*
        String smbFileColumns = SmbFileContract.SmbFileEntry.COLUMN_PARENT_ID + "=?";
        String[] smbFileIds = {String.valueOf(currentId)};

        return new CursorLoader(getActivity(),
        SmbFileContract.SmbFileEntry.CONTENT_URI,
        null,
        smbFileColumns,
        smbFileIds,
        null
        );
*/

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Cursor directoryCursor = null;

        if (loader.getId() == DIR_LOADER) {


            mCursor = data;
            int dirCount = 0;

            // get the latest parent ID
            if (null != mCursor && mCursor.moveToNext()) {
                dirCount = mCursor.getCount();

//            mCurrentParentId = data.getInt(SmbFileDbHelper.COL_NODE_ID);
//            Log.d(TAG, "onLoadFinished ******  mCurrentParentId = " + mCurrentParentId);
//
//            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
//            SharedPreferences.Editor editor = sp.edit();
//            editor.putString("CurrentParentId", String.valueOf(mCurrentParentId));
//            editor.apply();

            }

            //mSmbAdapter.swapCursor(data);

            Log.d(TAG, "onLoadFinished and dirCount = " + dirCount);
            mFolderTV.setText(String.valueOf(dirCount));

            String domainName = "Domain";
            String directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            String[] directoryIds = {String.valueOf(NMPDbHelper.NODE_TYPE_DOMAIN)};

            directoryCursor = getActivity().getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    directoryColumns,
                    directoryIds,
                    null
            );

            int domainCount = 0;
            // call NetworkQueryService to scan each of the directories found
            if (null != directoryCursor && directoryCursor.getCount() > 0) {
                domainCount = directoryCursor.getCount();
                directoryCursor.moveToFirst();
                domainName = directoryCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME);
            }
            Log.d(TAG, "onLoadFinished and domainCount = " + domainCount + " name: " + domainName);
            mDomainTV.setText(String.valueOf(domainCount));
            if (null != directoryCursor) directoryCursor.close();


            //directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            directoryIds[0] = String.valueOf(NMPDbHelper.NODE_TYPE_SERVER);

            directoryCursor = getActivity().getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    directoryColumns,
                    directoryIds,
                    null
            );

            int serverCount = 0;
            // call NetworkQueryService to scan each of the directories found
            if (null != directoryCursor && directoryCursor.getCount() > 0) {
                serverCount = directoryCursor.getCount();
            }
            Log.d(TAG, "onLoadFinished and serverCount = " + serverCount);
            mServerTV.setText(String.valueOf(serverCount));
            if (null != directoryCursor) directoryCursor.close();

            //directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            directoryIds[0] = String.valueOf(NMPDbHelper.NODE_TYPE_SHARE);

            directoryCursor = getActivity().getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    directoryColumns,
                    directoryIds,
                    null
            );

            int shareCount = 0;
            // call NetworkQueryService to scan each of the directories found
            if (null != directoryCursor && directoryCursor.getCount() > 0) {
                shareCount = directoryCursor.getCount();
            }
            Log.d(TAG, "onLoadFinished and shareCount = " + shareCount);
            mShareTV.setText(String.valueOf(shareCount));
            if (null != directoryCursor) directoryCursor.close();

            // get all songs
            directoryCursor = getActivity().getContentResolver().query(
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    null, //    directoryColumns,
                    null, //    directoryIds,
                    null
            );
        }

        if (loader.getId() == FILE_LOADER) {
            int songCount = 0;

            if (null != data && data.getCount() > 0) {
                songCount = data.getCount();
            }

            Log.d(TAG, "onLoadFinished and songCount = " + songCount);
            mSongTV.setText(String.valueOf(songCount));

        }

        if (null != directoryCursor) {
            directoryCursor.close();
        }
    }

        @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
    }


}
