package com.derekmorrison.networkmusicplayer.ui;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.SongList;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
/**
 * Created by Derek on 8/24/2016.
 */
public class GlobalApp extends Application {

    private NodeNavigator gNodeNavigator = null;
    private MediaSessionCompat.Token gMediaSessionCompatToken = null;
    private MediaControllerCompat gMediaController = null;
    private SongList gSongList = new SongList();
    private int TransientSongDbId = 0;
    private static Context mContext;
    private Tracker mTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker("UA-84733018-1");
        }
        return mTracker;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPrefUtils.getInstance().init(getApplicationContext());
        getNodeNavigator();
        this.mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }

    public int getTransientSongDbId() {
        return TransientSongDbId;
    }

    public void setTransientSongDbId(int transientSongDbId) {
        TransientSongDbId = transientSongDbId;
    }

    public NodeNavigator getNodeNavigator() {
        if (null == gNodeNavigator && false == SharedPrefUtils.getInstance().isFirstTime()){

            // this will automatically restore from shared prefs
            gNodeNavigator = new NodeNavigator(getApplicationContext());

            // if there was nothing in shared prefs then add the root node
            if (null == gNodeNavigator.getCurrentNode()) {
                DirNode root = getRootNode();
                if (null == root) {
                    // wtf?
                }
                gNodeNavigator.addNode(root);
            }
        }
        return gNodeNavigator;
    }

    public void setNodeNavigator(NodeNavigator gNodeNavigator) {
        this.gNodeNavigator = gNodeNavigator;
    }

    public MediaSessionCompat.Token getMediaSessionCompatToken() {
        return gMediaSessionCompatToken;
    }

    public void setMediaSessionCompatToken(MediaSessionCompat.Token token) {
        gMediaSessionCompatToken = token;

        // use the token to connect to the MediaControllerCompat
        try {
            gMediaController = new MediaControllerCompat(getApplicationContext(), gMediaSessionCompatToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public MediaControllerCompat getMediaController() {
        return gMediaController;
    }

    public SongList getSongList() {
        return gSongList;
    }


    private DirNode getRootNode() {

        DirNode rootNode = null;

        // get all the child directories of the node passed in
        Cursor directoryCursor = null;

        String directoryColumns = NMPContract.NodeEntry.COLUMN_PARENT_ID + " = ?";
        String[] directoryIds = {"0"};

        directoryCursor = getContentResolver().query(
                NMPContract.NodeEntry.CONTENT_URI,
                null,
                directoryColumns,
                directoryIds,
                null
        );

        if (null != directoryCursor && directoryCursor.moveToFirst()) {

            rootNode = new DirNode(
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
            );
        }

        if (null == rootNode) {
            // initialization failed - try again

        }


/*
        int bob = 0;

        if (null == directoryCursor) { bob = 1; }
        if (directoryCursor.getCount() == 0) { bob = 2; }
*/

        if (null != directoryCursor) {
            directoryCursor.close();
            directoryCursor = null;
        }

/*
        if (null == rootNode) {
            // try again

            directoryColumns = NMPContract.NodeEntry._ID + " = ?";
            directoryIds[0] = "1";

            directoryCursor = getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    directoryColumns,
                    directoryIds,
                    null
            );
        }

        // call NetworkQueryService to scan each of the directories found
        if (null != directoryCursor && directoryCursor.getCount() > 0) {
            directoryCursor.moveToNext();

            rootNode = new DirNode(
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                    directoryCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                    directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
            );
        }
*/

        return rootNode;
    }
}
