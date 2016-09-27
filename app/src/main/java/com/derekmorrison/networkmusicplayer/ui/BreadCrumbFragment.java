package com.derekmorrison.networkmusicplayer.ui;


import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class BreadCrumbFragment extends Fragment implements BreadCrumbAdapter.ClickListener {

    private NodeNavigator mNodeNavigator;
    private BreadCrumbAdapter mCrumbAdapter;
    private ArrayList<DirNode> mDirNode;
    protected RecyclerView mNodeRecyclerView;
    private CrumbClickListener mCrumbClickListener = null;

    public BreadCrumbFragment() {
        // Required empty public constructor
    }

    public interface CrumbClickListener {
        void onCrumbClick(DirNode node, int position, View v);
    }

    public void setOnCrumbClickListener(CrumbClickListener clickListener) {
        mCrumbClickListener = clickListener;
    }

    public void updateNodes(){
        mCrumbAdapter.updateNodes(mNodeNavigator.getDirNodeList());
        mNodeRecyclerView.scrollToPosition(mDirNode.size() - 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View crumbs = inflater.inflate(R.layout.fragment_bread_crumb, container, false);
        mNodeNavigator = ((GlobalApp) getActivity().getApplication()).getNodeNavigator();


        mCrumbAdapter = new BreadCrumbAdapter(mNodeNavigator.getDirNodeList());
        mCrumbAdapter.setOnClickListener(this);
        mDirNode = mNodeNavigator.getDirNodeList();

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        layoutManager.setStackFromEnd(true);

        mNodeRecyclerView = (RecyclerView) crumbs.findViewById(R.id.bread_crumb_list);

        mNodeRecyclerView.setLayoutManager(layoutManager);
        mNodeRecyclerView.setAdapter(mCrumbAdapter);

        return crumbs;
    }

    public void onItemClick(int position, View v) {
        String nodeName = mDirNode.get(position).getNodeName();
        String nodePath = mDirNode.get(position).getFilePath();

        if (null != mCrumbClickListener) {
            mCrumbClickListener.onCrumbClick(mDirNode.get(position), position, v);
        }
    }
}
