package com.derekmorrison.networkmusicplayer.ui;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * Created by Derek on 9/8/2016.
 */
public class AllPlaylistAdapter extends RecyclerView.Adapter<AllPlaylistAdapter.ViewHolder>  {

    private static final String TAG = "AllPlaylistAdapter";
    //private static ClickListener clickListener;
    private Cursor mCursor;
    private static MenuClickListener menuClickListener;

    public AllPlaylistAdapter() {}

    public interface MenuClickListener {
        void onMenuClick(int position, View v);
    }

    public void setOnMenuClickListener(MenuClickListener clickListener) {
        AllPlaylistAdapter.menuClickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder { // implements View.OnClickListener {
        private final TextView playlistView;
        private final ImageView menuView;

        public ViewHolder(View v) {
            super(v);

            playlistView = (TextView) v.findViewById(R.id.playlistName);
            menuView = (ImageView) v.findViewById(R.id.playlistMoreMenu);

            menuView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (null != menuClickListener) {
                        menuClickListener.onMenuClick(getAdapterPosition(), v);
                    }
                }
            });

        }

        public TextView getPlaylistView() {
            return playlistView;
        }

//        public LinearLayout getLayoutView() {
//            return shareLayout;
//        }


/*
        @Override
        public void onClick(View v) {
            if (null != clickListener) {
                clickListener.onItemClick(getAdapterPosition(), v);
            }
        }
*/
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_playlist_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        mCursor.moveToPosition(position);

        String listName = mCursor.getString(NMPContract.PlaylistEntry.COL_PLAYLIST_NAME);
        viewHolder.getPlaylistView().setText(listName);

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
