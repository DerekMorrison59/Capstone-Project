package com.derekmorrison.networkmusicplayer.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;

import java.util.ArrayList;

/**
 * Created by Derek on 9/6/2016.
 */
public class BreadCrumbAdapter extends RecyclerView.Adapter<BreadCrumbAdapter.ViewHolder> {
    private static final String TAG = "BreadCrumbAdapter";

    private ArrayList<DirNode> mDirNode;
    private static ClickListener clickListener;

    public BreadCrumbAdapter(ArrayList<DirNode> dirList) {
        super();

        mDirNode = new ArrayList<>(dirList);
    }

    @Override
    public int getItemCount() {
        return mDirNode.size();
    }

    public void updateNodes(ArrayList<DirNode> newNodes){
        mDirNode.clear();
        mDirNode = new ArrayList<>(newNodes);
        notifyDataSetChanged();
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    public void setOnClickListener(ClickListener clickListener) {
        BreadCrumbAdapter.clickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView crumb_folder;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            crumb_folder = (TextView) v.findViewById(R.id.crumb_folder);
        }

        public TextView getCrumbFolder() { return crumb_folder; }

        @Override
        public void onClick(View v) {
            if (null != clickListener) {
                clickListener.onItemClick(getAdapterPosition(), v);
            }
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.bread_crumb, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        String nodeName = mDirNode.get(position).getNodeName();
        nodeName = nodeName.replace("/", "");
        viewHolder.getCrumbFolder().setText(nodeName);
    }

}