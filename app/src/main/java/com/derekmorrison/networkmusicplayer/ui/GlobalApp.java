package com.derekmorrison.networkmusicplayer.ui;

import android.app.Application;
import android.database.Cursor;

import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * Created by Derek on 8/24/2016.
 */
public class GlobalApp extends Application {

    private NodeNavigator gNodeNavigator = null;

    public NodeNavigator getNodeNavigator() {
        if (null == gNodeNavigator){
            gNodeNavigator = new NodeNavigator(getApplicationContext(), getRootNode());
        }
        return gNodeNavigator;
    }

    public void setgNodeNavigator(NodeNavigator gNodeNavigator) {
        this.gNodeNavigator = gNodeNavigator;
    }


    // set
    // ((MyApplication) this.getApplication()).setSomeVariable("foo");

    // get
    // String s = ((MyApplication) this.getApplication()).getSomeVariable();



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

        return rootNode;
    }
}
