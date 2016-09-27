package com.derekmorrison.networkmusicplayer.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPContract;
import com.derekmorrison.networkmusicplayer.util.AlbumArt;
import com.squareup.picasso.Picasso;

/**
 * Created by Derek on 9/17/2016.
 */
public class EditPlaylistAdapter extends RecyclerView.Adapter<EditPlaylistAdapter.ViewHolder> {


    private static final String TAG = "EditPlaylistAdapter";
    private final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");

    private Cursor mCursor;
    private static MenuClickListener menuClickListener;
    private Context mContext;

    public EditPlaylistAdapter(Context context) {
        mContext = context;
    }

    public interface MenuClickListener {
        void onMenuClick(int position, View v);
    }

    public void setOnMenuClickListener(MenuClickListener clickListener) {
        EditPlaylistAdapter.menuClickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder { // implements View.OnClickListener {
        public TextView display_title;
        public TextView display_artist;
        public ImageView display_image;
        public ImageView menu_image;

        public ViewHolder(View view) {
            super(view);

            display_title = (TextView) view.findViewById(R.id.playlist_title);
            display_artist = (TextView) view.findViewById(R.id.playlist_artist);
            display_image = (ImageView) view.findViewById(R.id.playlist_image);
            menu_image = (ImageView) view.findViewById(R.id.playlistItemMoreMenu);

            menu_image.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (null != menuClickListener) {
                        menuClickListener.onMenuClick(getAdapterPosition(), v);
                    }
                }
            });

        }

        public TextView getTitleView() {
            return display_title;
        }
        public TextView getArtistView() {
            return display_artist;
        }
        public ImageView getImageView() {
            return display_image;
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.playlist_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, final int position) {

        mCursor.moveToPosition(position);

        int col_songId = mCursor.getColumnIndex(NMPContract.PlaylistItemEntry.COLUMN_PLAYLIST_ITEM_SONG_ID);
        int col_songFile = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_FILE_NAME);
        int col_deepScan = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_DEEP_SCAN);
        int col_artist = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_ARTIST);
        int col_title = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_TITLE);
        int col_track = mCursor.getColumnIndex(NMPContract.SongEntry.COLUMN_SONG_TRACK);

        int songId = mCursor.getInt(col_songId);
        boolean deepScan = (1 == mCursor.getInt(col_deepScan));

        String artist = "";
        String title = "";

        if (false == deepScan) {
            title = mCursor.getString(col_songFile);
        } else {
            artist = mCursor.getString(col_artist);
            title = mCursor.getString(col_title);
        }
        vh.getTitleView().setText(title);
        vh.getArtistView().setText(artist);

        int albumId = mCursor.getInt(col_track);
        Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumId);
//        Log.d(TAG, "onBindViewHolder with Art Uri: " + albumArtUri);
//        Log.d(TAG, "onBindViewHolder deepScan: " + deepScan + " title: " + title);


        if (false == deepScan) {
            Picasso.with(mContext).load(R.drawable.generic_cover).into(vh.display_image);

        } else {
            // use picasso to load the album cover (or the placeholder)
            Picasso.with(mContext)
                    .load(albumArtUri)
                    .placeholder(AlbumArt.getInstance().getPlaceHolderId(songId))
                    .into(vh.display_image);
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
