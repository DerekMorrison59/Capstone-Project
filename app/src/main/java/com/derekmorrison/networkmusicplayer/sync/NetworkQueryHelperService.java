package com.derekmorrison.networkmusicplayer.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.util.SharedPrefUtils;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class NetworkQueryHelperService extends IntentService {

    private static Context mContext;
    private static final String ACTION_SCAN_CHILDREN = "com.derekmorrison.networkmusicplayer.sync.action.SCAN_CHILDREN";


    public NetworkQueryHelperService() {
        super("NetworkQueryHelperService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionScanChildren(Context context, int node_id,
                                               int scan_depth, int node_type) {
        mContext = context;
        Intent intent = new Intent(context, NetworkQueryHelperService.class);
        intent.setAction(ACTION_SCAN_CHILDREN);
        intent.putExtra(NetworkQueryService.EXTRA_NODE_ID, node_id);
        intent.putExtra(NetworkQueryService.EXTRA_SCAN_DEPTH, scan_depth);
        intent.putExtra(NetworkQueryService.EXTRA_NODE_TYPE, node_type);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SCAN_CHILDREN.equals(action)) {
                final int parent_node_id = intent.getIntExtra(NetworkQueryService.EXTRA_NODE_ID, 0);
                final int scan_depth = intent.getIntExtra(NetworkQueryService.EXTRA_SCAN_DEPTH, 0);
                final int node_type = intent.getIntExtra(NetworkQueryService.EXTRA_NODE_TYPE, NMPDbHelper.NODE_TYPE_DIRECTORY);
                handleActionScanChildren(parent_node_id, scan_depth, node_type);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionScanChildren(int parent_node_id, int scan_depth, int node_type) {


        // get all the child directories of the node passed in
        Cursor directoryCursor = null;

        String directoryColumns = NMPContract.NodeEntry.COLUMN_PARENT_ID + " = ?";
        String[] directoryIds = {String.valueOf(parent_node_id)};

        directoryCursor = getContentResolver().query(
                NMPContract.NodeEntry.CONTENT_URI,
                null,
                directoryColumns,
                directoryIds,
                null
        );

        // call NetworkQueryService to scan each of the directories found
        if (null != directoryCursor && directoryCursor.getCount() > 0) {
            while(directoryCursor.moveToNext()) {

                // if this is the final scan then count it
                if (1 == scan_depth) {
                    SharedPrefUtils.getInstance().incrementScanStarts();
                }

                // only request a scan if the directory has not been scanned before
//                if (NMPDbHelper.NODE_NOT_SCANNED == directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)) {
                    NetworkQueryService.startActionScanNode(mContext,
                            directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID),
                            directoryCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                            scan_depth, node_type);

//                }
            }
        }

        if (null != directoryCursor) {
            directoryCursor.close();
        }
    }
}
