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
 * Created by Derek on 8/24/2016.
 */
public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {
    private static final String TAG = "ServerAdapter";
    private static ClickListener clickListener;
    private Cursor mCursor;
    private Context mContext;

    public ServerAdapter(Context context) { mContext = context; }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        ServerAdapter.clickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView serverView;
        private final LinearLayout serverLayout;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(this);

            serverView = (TextView) v.findViewById(R.id.serverName);
            serverLayout = (LinearLayout) v.findViewById(R.id.serverNodeLayout);
        }

        public TextView getServerView() {
            return serverView;
        }
        public LinearLayout getLayoutView() {
            return serverLayout;
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
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.server_node, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        mCursor.moveToPosition(position);

        String serverName = mCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME);
        viewHolder.getServerView().setText(serverName);

    }

    @Override
    public int getItemCount() {
        if ( null == mCursor ) return 0;
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
