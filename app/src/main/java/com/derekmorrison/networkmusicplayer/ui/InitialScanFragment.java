package com.derekmorrison.networkmusicplayer.ui;


import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.NetworkQueryService;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.Date;

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
    private TextView mStartTimeTV;
    private TextView mEndTimeTV;
//    private TextView mBranchEndsTV;
    ProgressBar mProgressBar;

    private Button mServersButton;

    private View mInitialScan;
    private Tracker mTracker;

    public InitialScanFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View initialScan = inflater.inflate(R.layout.fragment_initial_scan, container, false);
        mInitialScan = initialScan;

        mDomainTV = (TextView)initialScan.findViewById(R.id.domainCountTV);
        mServerTV = (TextView)initialScan.findViewById(R.id.serverCountTV);
        mShareTV = (TextView)initialScan.findViewById(R.id.shareCountTV);
        mFolderTV = (TextView)initialScan.findViewById(R.id.folderCountTV);
        mSongTV = (TextView)initialScan.findViewById(R.id.songCountTV);
        mScanTV = (TextView)initialScan.findViewById(R.id.scanCountTV);
        mScanTV.setText(String.valueOf(OnboardingFragment.ONBOARDING_SCAN_DEPTH));

        mStartTimeTV = (TextView)initialScan.findViewById(R.id.startTimeTV);
        mEndTimeTV = (TextView)initialScan.findViewById(R.id.endTimeTV);

        TextView scanLabel = (TextView)initialScan.findViewById(R.id.scanLabelTV);
        mProgressBar = (ProgressBar)initialScan.findViewById(R.id.progressBar);

        mServersButton = (Button)initialScan.findViewById(R.id.all_servers_button);

        String title = "Discovery Status";
        if (true == SharedPrefUtils.getInstance().isFirstTime()) {
            mScanTV.setVisibility(View.VISIBLE);
            mServersButton.setVisibility(View.INVISIBLE);
            mServersButton.setEnabled(false);
            scanLabel.setVisibility(View.VISIBLE);

            title = "Initial Network Scan";
            mServersButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.setFragment(MainActivity.FRAGMENT_ALL_SERVERS);
                }
            });
        } else {
            mScanTV.setVisibility(View.INVISIBLE);
            mServersButton.setVisibility(View.INVISIBLE);
            scanLabel.setVisibility(View.INVISIBLE);
        }

        mTracker = ((GlobalApp) getActivity().getApplication()).getDefaultTracker();
        mTracker.setScreenName("Image~" + title);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        MainActivity.setToolbarTitle(title);

        initialScan.setFocusableInTouchMode(true);
        initialScan.requestFocus();
        initialScan.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                Log.d(TAG, "onKey" );
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
        super.onActivityCreated(savedInstanceState);

//        Log.d(TAG, "onActivityCreated " );

        getLoaderManager().initLoader(DIR_LOADER, null, this);
        getLoaderManager().initLoader(FILE_LOADER, null, this);
        //if (mStartCount > 0) getLoaderManager().restartLoader(FILE_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        Log.d(TAG, "onCreateLoader LOADER: " + i);

        Loader<Cursor> theLoader = null;

        if (DIR_LOADER == i) {
            theLoader = new CursorLoader(GlobalApp.getContext(),
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (FILE_LOADER == i) {
            theLoader = new CursorLoader(GlobalApp.getContext(),
                    NMPContract.SongEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        return theLoader;
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
            }

            //mSmbAdapter.swapCursor(data);

            //Log.d(TAG, "onLoadFinished and dirCount = " + dirCount);
            mFolderTV.setText(String.valueOf(dirCount));

            String domainName = "Domain";
            String directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            String[] directoryIds = {String.valueOf(NMPDbHelper.NODE_TYPE_DOMAIN)};

            directoryCursor = GlobalApp.getContext().getContentResolver().query(
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
//            Log.d(TAG, "onLoadFinished and domainCount = " + domainCount + " name: " + domainName);
            mDomainTV.setText(String.valueOf(domainCount));
            if (null != directoryCursor) directoryCursor.close();


            //directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            directoryIds[0] = String.valueOf(NMPDbHelper.NODE_TYPE_SERVER);

            directoryCursor = GlobalApp.getContext().getContentResolver().query(
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
//            Log.d(TAG, "onLoadFinished and serverCount = " + serverCount);
            mServerTV.setText(String.valueOf(serverCount));
            if (null != directoryCursor) directoryCursor.close();

            //directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_TYPE + " = ?";
            directoryIds[0] = String.valueOf(NMPDbHelper.NODE_TYPE_SHARE);

            directoryCursor = GlobalApp.getContext().getContentResolver().query(
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
//            Log.d(TAG, "onLoadFinished and shareCount = " + shareCount);
            mShareTV.setText(String.valueOf(shareCount));
            if (null != directoryCursor) directoryCursor.close();

            // test to see if the initial scan has completed
            if (SharedPrefUtils.getInstance().getScanEnds() > SharedPrefUtils.getInstance().getScanStarts()) {

                if (true == SharedPrefUtils.getInstance().isFirstTime()) {
                    //setScanStartTime
                    Date now = new Date();
                    SharedPrefUtils.getInstance().setScanEndTime(now.getTime());

                    Snackbar.make(mInitialScan, "Initial Scan is Complete!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }

                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                mStartTimeTV.setText(fmt.format(SharedPrefUtils.getInstance().getScanStartTime()));
                mEndTimeTV.setText(fmt.format(SharedPrefUtils.getInstance().getScanEndTime()));

                // show and enable the button
                mServersButton.setVisibility(View.VISIBLE);
                mServersButton.setEnabled(true);

                // hide the progress bar now
                mProgressBar.setVisibility(View.INVISIBLE);
                SharedPrefUtils.getInstance().firstTimeCompleted();
            }
        }

        if (loader.getId() == FILE_LOADER) {
            int songCount = 0;

            if (null != data && data.getCount() > 0) {
                songCount = data.getCount();
            }

//            Log.d(TAG, "onLoadFinished and songCount = " + songCount);
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
