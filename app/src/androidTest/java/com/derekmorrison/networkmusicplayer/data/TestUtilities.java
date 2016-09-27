package com.derekmorrison.networkmusicplayer.data;

import android.content.ContentValues;
import android.test.AndroidTestCase;

/**
 * Created by Derek on 8/22/2016.
 */
public class TestUtilities extends AndroidTestCase {

    public static ContentValues createNodeValues() {

/*
        // Table name
        public static final String TABLE_NAME = "node";

        public static final String COLUMN_NODE_ID = "node_id";
        public static final String COLUMN_PARENT_ID = "parent_id";
        public static final String COLUMN_NODE_NAME = "node_name";
        public static final String COLUMN_FILE_PATH = "file_path";
        public static final String COLUMN_NODE_TYPE = "node_type";
        public static final String COLUMN_NODE_STATUS = "node_status";
        public static final String COLUMN_NODE_IS_FAV = "node_is_fav";
*/

        String path = "smb://WORKGROUP";
        ContentValues values = new ContentValues();
        values.put(NMPContract.NodeEntry.COLUMN_PARENT_ID, 0);

        values.put(NMPContract.NodeEntry.COLUMN_NODE_ID, path.hashCode());
        values.put(NMPContract.NodeEntry.COLUMN_FILE_PATH, path);
        values.put(NMPContract.NodeEntry.COLUMN_NODE_NAME, "WORKGROUP");
        values.put(NMPContract.NodeEntry.COLUMN_NODE_TYPE, NMPDbHelper.NODE_TYPE_DOMAIN);
        values.put(NMPContract.NodeEntry.COLUMN_NODE_STATUS, NMPDbHelper.NODE_NOT_SCANNED);
        values.put(NMPContract.NodeEntry.COLUMN_NODE_IS_FAV, 0);

        return values;
    }
}
