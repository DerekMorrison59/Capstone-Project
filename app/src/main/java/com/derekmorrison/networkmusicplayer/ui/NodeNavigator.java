package com.derekmorrison.networkmusicplayer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.View;

import com.derekmorrison.networkmusicplayer.data.NMPContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Derek on 8/24/2016.
 */
public class NodeNavigator {

    private Context mContext;
    private ArrayList<DirNode> dirNodes = new ArrayList<DirNode>();

    private final String NODE_LIST = "node_list";
    private final String NODE_COUNT = "node_count";

    // the end of the list is the current position

    public NodeNavigator(Context context) {
        mContext = context;
        restoreNodeList();
    }

    public NodeNavigator(Context context, DirNode first) {
        mContext = context;
        dirNodes.add(first);
    }

/*
* Store current list in prefs
* re-store current list from prefs
* goto specific node
* */



/*
    public void moveToNewNode(AppCompatActivity activity) {
        // newNode Type         Destination Screen

        // NODE_TYPE_DOMAIN     AllServersFragment
        // NODE_TYPE_SERVER     SharesFragment
        // NODE_TYPE_SHARE      DirectoryFragment
        // NODE_TYPE_DIRECTORY  FilesFragment
        // NODE_TYPE_FILE
        FragmentTransaction transaction =  activity.getSupportFragmentManager().beginTransaction();
        OnboardingFragment fragment = new OnboardingFragment();
        transaction.replace(R.id.sample_content_fragment, fragment);
        transaction.commit();

    }
*/

    // todo what are the rules about adding a node?
    // it will always be a lower number until it is a directory
    // directory cannot go up to a share only to another directory
    //
    // must check all existing nodes to make sure a node is not added a second time
    public DirNode addNode(DirNode newNode) {
        if (null == newNode || (dirNodes.size() > 0 && getCurrentNode().equals(newNode))) {
            return null;
        }
        dirNodes.add(newNode);
        saveNodeList();
        return newNode;
    }

    public ArrayList<DirNode> getDirNodeList() {
        return dirNodes;
    }

    public DirNode getCurrentNode() {
        DirNode current = null;
        if (dirNodes.size() > 0) {
            current = dirNodes.get(dirNodes.size() - 1);
        }
        return current;
    }

    public DirNode goBack() {
        int size = dirNodes.size();
        if (size > 1) {
            dirNodes.remove(size - 1);
        }
        saveNodeList();
        return getCurrentNode();
    }

    public DirNode gotoNode(int nodeNumber) {
        DirNode newPosition = getCurrentNode();

        // make sure the nodeNumber is within the current set of nodes
        int size = dirNodes.size();
        if (nodeNumber >= 0 && nodeNumber < size && nodeNumber != size-1) {
            while (dirNodes.size()-1 > nodeNumber) {
                newPosition = goBack();
            }
        }

        return newPosition;
    }

    public DirNode resetHead(DirNode newHead){
        dirNodes.clear();
        dirNodes.add(newHead);
        return newHead;
    }

    public void saveNodeList() {
        SharedPreferences sp = mContext.getSharedPreferences(NODE_LIST, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();

        editor.putInt(NODE_COUNT, dirNodes.size());

        for (int i = 0; i < dirNodes.size(); i++){
            // use 'i' as the key and save the node Db Id as the value
            editor.putInt(String.valueOf(i), dirNodes.get(i).getId());
        }
        editor.apply();
    }

    public void restoreNodeList() {

        dirNodes.clear();

        SharedPreferences sp = mContext.getSharedPreferences(NODE_LIST, Context.MODE_PRIVATE);

        int nodeCount = sp.getInt(NODE_COUNT, 0);

        // special case




        Cursor directoryCursor = null;

        for (int i = 0; i < nodeCount; i++) {
            int nodeId = sp.getInt(String.valueOf(i), 0);

            String directoryColumns = NMPContract.NodeEntry.COLUMN_NODE_ID + " = ?";
            String[] directoryIds = {String.valueOf(nodeId)};

            directoryCursor = mContext.getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    directoryColumns,
                    directoryIds,
                    null
            );

            // call NetworkQueryService to scan each of the directories found
            if (null != directoryCursor && directoryCursor.moveToFirst()) {
                DirNode dn = new DirNode(nodeId,
                        directoryCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                        directoryCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                        directoryCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                        directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                        directoryCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
                        );

                addNode(dn);

                directoryCursor.close();
                directoryCursor = null;
            }

        }
    }



    public void moveToNewNode(DirNode selectedNode) {

        // first build a list of all nodes from this node back to the root node
        ArrayList<DirNode> newNodes = new ArrayList<DirNode>();

        newNodes.add(selectedNode);

        int currentNode = selectedNode.getId();
        int parentId = selectedNode.getParentId();
        boolean listComplete = false;
        if (0 == currentNode || 0 == parentId) listComplete = true;

        String selection = NMPContract.NodeEntry.COLUMN_NODE_ID + "=?";
        String[] args = {String.valueOf(parentId)};

        while (false == listComplete) {

            Cursor nodeCursor = mContext.getContentResolver().query(
                    NMPContract.NodeEntry.CONTENT_URI,
                    null,
                    selection,
                    args,
                    null
            );

            if (null != nodeCursor && nodeCursor.moveToFirst()) {
                DirNode dn = new DirNode(parentId,
                        nodeCursor.getInt(NMPContract.NodeEntry.COL_PARENT_ID),
                        nodeCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME),
                        nodeCursor.getString(NMPContract.NodeEntry.COL_FILE_PATH),
                        nodeCursor.getInt(NMPContract.NodeEntry.COL_NODE_TYPE),
                        nodeCursor.getInt(NMPContract.NodeEntry.COL_NODE_STATUS)
                );

                newNodes.add(dn);
                parentId = dn.getParentId();
                if (0 == parentId) {
                    listComplete = true;
                } else {
                    args[0] = String.valueOf(parentId);
                }
            } else {
                listComplete = true;
            }

            nodeCursor.close();
            nodeCursor = null;
        }

        // then add DirNodes from the root back to the selected Node
        dirNodes.clear();
        int index = newNodes.size() - 1;

        for (int i = 0; i < newNodes.size(); i++) {
            dirNodes.add(newNodes.get(index - i));
        }
    }
}
