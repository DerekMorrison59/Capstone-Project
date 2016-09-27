package com.derekmorrison.networkmusicplayer.sync;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContentResolverCompat;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.ui.MainActivity;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;
import com.derekmorrison.networkmusicplayer.util.Utility;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NetworkQueryService extends IntentService {

    private static Context mContext;

    public final String LOG_TAG = NetworkQueryService.class.getSimpleName();
    private static final String ACTION_SCAN_NODE = "com.derekmorrison.networkmusicplayer.sync.action.SCAN_NODE";

    public static final String EXTRA_NODE_ID = "com.derekmorrison.networkmusicplayer.sync.extra.NODE_ID";
    public static final String EXTRA_NODE_PATH = "com.derekmorrison.networkmusicplayer.sync.extra.NODE_PATH";
    public static final String EXTRA_SCAN_DEPTH = "com.derekmorrison.networkmusicplayer.sync.extra.SCAN_DEPTH";
    public static final String EXTRA_NODE_TYPE = "com.derekmorrison.networkmusicplayer.sync.extra.NODE_TYPE";

    // todo decide if more of these song extenstions should be included
    //private static final String[] audioExt=new String[]{"aif","iff","m3u","m4a","mid","mp3","mpa","ra","wav","wma"};
    private static final String[] audioExt = new String[] {"m4a","mp3"};

    public NetworkQueryService() {
        super("NetworkQueryService");
        Log.d(LOG_TAG, "NetworkQueryService Constructor");
    }

    /**
     * Starts this service to perform action ScanNode with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // startActionScanNode uses the 'node_id' as the parent node and scans the network
    // to find all child nodes. Each child is evaluated, directories and music files are
    // added to the database. The parameter 'scan_depth' indicates how many levels of the
    // directory tree should be scanned below the current one
    public static void startActionScanNode(Context context, int node_id, String node_path,
                                           int scan_depth, int node_type) {
        mContext = context;
        Intent intent = new Intent(context, NetworkQueryService.class);
        intent.setAction(ACTION_SCAN_NODE);
        intent.putExtra(EXTRA_NODE_ID, node_id);
        intent.putExtra(EXTRA_NODE_PATH, node_path);
        intent.putExtra(EXTRA_SCAN_DEPTH, scan_depth);
        intent.putExtra(EXTRA_NODE_TYPE, node_type);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SCAN_NODE.equals(action)) {
                final int parent_node_id = intent.getIntExtra(EXTRA_NODE_ID, 0);
                final String node_path = intent.getStringExtra(EXTRA_NODE_PATH);
                final int scan_depth = intent.getIntExtra(EXTRA_SCAN_DEPTH, 0);
                final int node_type = intent.getIntExtra(EXTRA_NODE_TYPE, NMPDbHelper.NODE_TYPE_DOMAIN);
                handleActionScanNode(parent_node_id, node_path, scan_depth, node_type);
            }
        }
    }

    /**
     * Handle action ScanNode in the provided background thread with the provided
     * parameters.
     */
    private void handleActionScanNode(int parent_node_id, String parent_node_path,
                                      int scan_depth, int parent_node_type) {

        Log.d(LOG_TAG, "handleActionScanNode for path: " + parent_node_path);

        Date scanStartTime = new Date();

        final String nDomain = "";
        final String nUser = "guest";
        final String nPassword = "";

        SmbFile directory = null;
        SmbFile[] children = null;

        // sanity check - start at the root if there is no other path provided
        if (null == parent_node_path) {
            parent_node_path = "smb://";
            parent_node_id = 0;
            scan_depth = 3;
            parent_node_type = NMPDbHelper.NODE_TYPE_DOMAIN;
        }

        try {
            jcifs.Config.setProperty("jjcifs.smb.lmCompatibility", "2");
            jcifs.Config.setProperty("jcifs.smb.client.useExtendedSecurity", "false");
            jcifs.Config.setProperty("jcifs.smb.client.soTimeout", "35000");
            jcifs.Config.setProperty("jcifs.resolveOrder", "LMHOSTS,BCAST,DNS");

            NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(nDomain, nUser, nPassword);

            // look for this directory on the network
            directory = new SmbFile(parent_node_path, authentication);

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        // the directory exists, now request the child nodes
        try {
            Date date = new Date();
            Log.d(LOG_TAG, "about to call for children: " + date.toString());

            children = directory.listFiles();

            date = new Date();
            Log.d(LOG_TAG, "finished call for children: " + date.toString());

        }catch (SmbException e) {
            // TODO Auto-generated catch block

            Log.d(LOG_TAG, "** $$ !! can not read directory: " + directory.getName());
            SharedPrefUtils.getInstance().incrementScanEnds();

            //e.printStackTrace();
            //children = null;
            return;
        }

        // todo do something with the start and end scan times
        Date scanEndTime = new Date();
        if (1 == scan_depth) {
            SharedPrefUtils.getInstance().incrementScanEnds();
        }


        // update the parentNodeId record and mark it as 'scanned' because the children were retrieved

        // specify the column to use
        String nodeColumns = NMPContract.NodeEntry.COLUMN_NODE_ID + " = ? ";

        // specify the node ID
        String[] nodeIds = {"0"};
        nodeIds[0] = String.valueOf(parent_node_id);

        // specify which column to update and the new value
        ContentValues values = new ContentValues();
        values.put(NMPContract.NodeEntry.COLUMN_NODE_STATUS, NMPDbHelper.NODE_SCANNED);

        int updatedRows = getContentResolver().update(
                NMPContract.NodeEntry.CONTENT_URI,
                values,
                nodeColumns,
                nodeIds
        );

        // updatedRows should always be 1 because a directory should never be duplicated

        // if no children were found then there is nothing to add to the database
        if (null != children) {
            int childrenFound = children.length;

            int nodeType = NMPDbHelper.NODE_TYPE_FILE;
            int nodeStatus = 0;

            List<ContentValues> newDirectories = new ArrayList<ContentValues>();
            List<ContentValues> newFiles = new ArrayList<ContentValues>();


            for (int i = 0; i < childrenFound; i++) {
                try {
                    String filename = children[i].getName();

                    if (true == children[i].isDirectory()) {
                        Log.d(LOG_TAG, "found directory: " + filename);

                        nodeType = getChildNodeType(parent_node_type);
                        nodeStatus = NMPDbHelper.NODE_NOT_SCANNED;

                        ContentValues dirValues = new ContentValues();
                        dirValues.put(NMPContract.NodeEntry.COLUMN_PARENT_ID, parent_node_id);
                        dirValues.put(NMPContract.NodeEntry.COLUMN_NODE_ID, children[i].getPath().hashCode());
                        dirValues.put(NMPContract.NodeEntry.COLUMN_NODE_NAME, filename);
                        dirValues.put(NMPContract.NodeEntry.COLUMN_FILE_PATH, children[i].getPath());
                        dirValues.put(NMPContract.NodeEntry.COLUMN_NODE_TYPE, nodeType);
                        dirValues.put(NMPContract.NodeEntry.COLUMN_NODE_STATUS, nodeStatus);
                        dirValues.put(NMPContract.NodeEntry.COLUMN_NODE_IS_FAV, 0);

                        newDirectories.add(dirValues);

                    } else {
                        // get the file extension and see if it's a music file
                        String extension = getExtension(filename);

                        if (extension.equals("mp3") || extension.equals("m4a")) {
                            Log.d(LOG_TAG, "found song: " + filename);
                            ContentValues fileValues = new ContentValues();
                            fileValues.put(NMPContract.SongEntry.COLUMN_PARENT_ID, parent_node_id);
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_ID, children[i].getPath().hashCode());
                            fileValues.put(NMPContract.SongEntry.COLUMN_FILE_NAME, filename);
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_TRACK, 0);         // there is no image ID
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_LAST_PLAYED, 0);   // just added == no last play
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_PLAY_COUNT, 0);    // just added == never played
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_DEEP_SCAN, 0);     // 0 == false
                            fileValues.put(NMPContract.SongEntry.COLUMN_SONG_IS_FAV, 0);        // 0 == false

                            newFiles.add(fileValues);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // todo post an update containing start and end times

            }

            // todo  figure out when all scans have completed
            // add new song files to the database
            int fileCount = newFiles.size();
            if (fileCount > 0){
                ContentValues[] fileArray = new ContentValues[fileCount];
                fileArray = newFiles.toArray(fileArray);
                getContentResolver().bulkInsert(NMPContract.SongEntry.CONTENT_URI, fileArray);
                Log.d(LOG_TAG, "Songs added to the database: " + fileCount);
            }

            // add new directories to the database
            int directoryCount = newDirectories.size();
            if (directoryCount > 0){
                ContentValues[] directoryArray = new ContentValues[directoryCount];
                directoryArray = newDirectories.toArray(directoryArray);
                getContentResolver().bulkInsert(NMPContract.NodeEntry.CONTENT_URI, directoryArray);
                Log.d(LOG_TAG, "Directories added to the database: " + directoryCount);

                // now check to see if the scanning should continue to the child directories
                scan_depth--;
                // if levels to go > 0 then send intent to the follow-up service with parentNodeId for the parent
                if (scan_depth > 0) {
                    Log.d(LOG_TAG, "scan_depth: " + scan_depth);

                    NetworkQueryHelperService.startActionScanChildren(mContext, parent_node_id,
                            scan_depth, getChildNodeType(parent_node_type));

                }
            }
        }
    }

    private String getExtension(String fileName) {
        String ext ="";
        int i = fileName.lastIndexOf('.');
        if (i > 0 &&  i < fileName.length() - 1) {
            ext = fileName.substring(i+1).toLowerCase();
        }
        return ext;
    }

    public static int getParentNodeType(int nodeType) {

        int parentNodeType = Math.abs(nodeType) + 10;

        if (parentNodeType > NMPDbHelper.NODE_TYPE_DOMAIN) {
            parentNodeType = NMPDbHelper.NODE_TYPE_DOMAIN;
        }

        return parentNodeType;
    }

    // this is used to determine various levels of Directory from DOMAIN to DIRECTORY
    public static int getChildNodeType(int nodeType) {

        int childNodeType = Math.abs(nodeType) - 10;

        if (childNodeType < NMPDbHelper.NODE_TYPE_DIRECTORY) {
            childNodeType = NMPDbHelper.NODE_TYPE_DIRECTORY;
        }

        return childNodeType;
    }


}
