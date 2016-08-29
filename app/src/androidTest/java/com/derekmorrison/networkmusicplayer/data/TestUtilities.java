package com.derekmorrison.networkmusicplayer.data;

import android.content.ContentValues;
import android.test.AndroidTestCase;

/**
 * Created by Derek on 8/22/2016.
 */
public class TestUtilities extends AndroidTestCase {

    public static ContentValues createNodeValues() {

        String path = "smb://WORKGROUP";
        ContentValues values = new ContentValues();
        values.put(NMPContract.NodeEntry.COLUMN_PARENT_ID, 0);

        values.put(NMPContract.NodeEntry.COLUMN_NODE_ID, path.hashCode());
        values.put(NMPContract.NodeEntry.COLUMN_FILE_PATH, path);
        values.put(NMPContract.NodeEntry.COLUMN_NODE_NAME, "WORKGROUP");
        values.put(NMPContract.NodeEntry.COLUMN_NODE_TYPE, NMPDbHelper.NODE_TYPE_DOMAIN);
        values.put(NMPContract.NodeEntry.COLUMN_NODE_STATUS, NMPDbHelper.NODE_NOT_SCANNED);

        return values;
    }
}
