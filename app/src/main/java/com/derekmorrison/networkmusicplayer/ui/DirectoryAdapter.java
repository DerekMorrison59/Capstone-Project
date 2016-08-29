package com.derekmorrison.networkmusicplayer.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;

/**
 * Created by Derek on 8/25/2016.
 */
public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {
    private static final String TAG = "DirectoryAdapter";
    private static ClickListener clickListener;
    private Cursor mCursor;
    private Context mContext;
    private int mNodeType = NMPDbHelper.NODE_TYPE_DIRECTORY;

    public DirectoryAdapter(Context context) {
        mContext = context;
    }

    public interface ClickListener {
        void onItemClick(int nodeType, int position, View v);
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        DirectoryAdapter.clickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView dirView;
        private final LinearLayout dirLayout;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(this);

            dirView = (TextView) v.findViewById(R.id.directoryName);
            dirLayout = (LinearLayout) v.findViewById(R.id.directoryNodeLayout);
        }

        public TextView getDirectoryView() {
            return dirView;
        }

        public LinearLayout getLayoutView() {
            return dirLayout;
        }


        @Override
        public void onClick(View v) {
            if (null != clickListener) {
                clickListener.onItemClick(mNodeType, getAdapterPosition(), v);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.directory_node, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        mCursor.moveToPosition(position);

        String serverName = mCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME);
        viewHolder.getDirectoryView().setText(serverName);

    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return mCursor;
    }
}
