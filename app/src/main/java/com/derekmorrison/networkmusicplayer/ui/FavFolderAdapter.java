package com.derekmorrison.networkmusicplayer.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * Created by Derek on 9/26/2016.
 */
public class FavFolderAdapter extends RecyclerView.Adapter<FavFolderAdapter.ViewHolder> {
    private static final String TAG = "DirectoryAdapter";
    private static ClickListener clickListener;

    private Cursor mCursor;
    private Context mContext;
    private Bitmap mColorFav;
    private Bitmap mGrayFav;

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        FavFolderAdapter.clickListener = clickListener;
    }

    public FavFolderAdapter(Context context) {
        mContext = context;

        mColorFav = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.color_favorite);
        mGrayFav = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.gray_favorite);

    }
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView dirView;
        private final LinearLayout dirLayout;
        private final ImageView dirFav;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(this);

            dirView = (TextView) v.findViewById(R.id.directoryName);
            dirLayout = (LinearLayout) v.findViewById(R.id.directoryNodeLayout);
            dirFav = (ImageView) v.findViewById(R.id.favImageView);

            dirFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //mCursor.moveToPosition(getAdapterPosition());
                    if (null != mCursor && mCursor.moveToPosition(getAdapterPosition())) {

                        int isFav = mCursor.getInt(NMPContract.NodeEntry.COL_NODE_IS_FAV);
                        if (1 == isFav) {
                            // change to NOT fav
                            isFav = 0;
                        } else {
                            // change to favorite
                            isFav = 1;
                        }

                        ContentValues values = new ContentValues();
                        values.put(NMPContract.NodeEntry.COLUMN_NODE_IS_FAV, isFav);

                        int nodeId = mCursor.getInt(NMPContract.NodeEntry.COL_NODE_ID);
                        String selection = NMPContract.NodeEntry.COLUMN_NODE_ID + "=?";
                        String[] args = {String.valueOf(nodeId)};

                        mContext.getContentResolver().update(
                                NMPContract.NodeEntry.CONTENT_URI,
                                values,
                                selection,
                                args
                        );
                    }
                }
            });
        }

        public TextView getDirectoryView() {
            return dirView;
        }
        public ImageView getFavImage() {
            return dirFav;
        }

        public LinearLayout getLayoutView() {
            return dirLayout;
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
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.directory_node, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        mCursor.moveToPosition(position);

        String folderName = mCursor.getString(NMPContract.NodeEntry.COL_NODE_NAME);
        viewHolder.getDirectoryView().setText(folderName);

        int isFav = mCursor.getInt(NMPContract.NodeEntry.COL_NODE_IS_FAV);
        if (1 == isFav) {
            // show colored icon
            viewHolder.getFavImage().setImageBitmap(mColorFav);
        } else {
            // show gray icon
            viewHolder.getFavImage().setImageBitmap(mGrayFav);
        }
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
