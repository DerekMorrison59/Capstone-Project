package com.derekmorrison.networkmusicplayer.ui;

import android.content.Context;
import java.util.ArrayList;

/**
 * Created by Derek on 8/24/2016.
 */
public class NodeNavigator {

    private Context mContext;
    private ArrayList<DirNode> dirNodes = new ArrayList<DirNode>();

    private SongNode songNode = null;

    // the end of the list is the current position

    public NodeNavigator(Context context) {
        mContext = context;
    }

    public NodeNavigator(Context context, DirNode first) {
        mContext = context;
        dirNodes.add(first);
    }

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
        if (getCurrentNode().equals(newNode)) {
            return null;
        }
        dirNodes.add(newNode);
        return newNode;
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
        return getCurrentNode();
    }

    public DirNode resetHead(DirNode newHead){
        dirNodes.clear();
        dirNodes.add(newHead);
        return newHead;
    }


    public void setSongNode(SongNode newSong) {
        songNode = newSong;
    }

    public SongNode getSongNode() {
        if (null == songNode) {
            songNode = new SongNode();
        }
        return songNode;
    }
}
