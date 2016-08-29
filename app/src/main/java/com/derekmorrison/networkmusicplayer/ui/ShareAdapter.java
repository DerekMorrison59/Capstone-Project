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

/**
 * Created by Derek on 8/25/2016.
 */
public class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ViewHolder> {
    private static final String TAG = "ShareAdapter";
    private static ClickListener clickListener;
    private Cursor mCursor;
    private Context mContext;

    public ShareAdapter(Context context) {
        mContext = context;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        ShareAdapter.clickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView shareView;
        private final LinearLayout shareLayout;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(this);

            shareView = (TextView) v.findViewById(R.id.shareName);
            shareLayout = (LinearLayout) v.findViewById(R.id.shareNodeLayout);
        }

        public TextView getShareView() {
            return shareView;
        }

        public LinearLayout getLayoutView() {
            return shareLayout;
        }


        @Override
        public void onClick(View v) {
            if (null != clickListener) {
                clickListener.onItemClick(getAdapterPosition(), v);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.share_node, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        mCursor.moveToPosition(position);

        String serverName = mCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME);
        viewHolder.getShareView().setText(serverName);

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
