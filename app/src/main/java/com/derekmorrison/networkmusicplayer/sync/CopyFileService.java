package com.derekmorrison.networkmusicplayer.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.derekmorrison.networkmusicplayer.ui.MainActivity;
import com.derekmorrison.networkmusicplayer.util.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class CopyFileService extends IntentService {

    static Context mContext;
    private static CountDownLatch scanLatch = new CountDownLatch(1);
    private static boolean gateLatch = true;

    private static final String ACTION_COPY_FILE = "com.derekmorrison.networkmusicplayer.sync.action.COPY_FILE";

    private static final String EXTRA_SONG_ID = "com.derekmorrison.networkmusicplayer.sync.extra.SONG_ID";
    private static final String EXTRA_FILE_PATH = "com.derekmorrison.networkmusicplayer.sync.extra.FILE_PATH";
    private static final String EXTRA_LIST_ID = "com.derekmorrison.networkmusicplayer.sync.extra.LIST_ID";

    private int mSongId;
    private int mListId;

    public CopyFileService() {
        super("CopyFileService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startCopyFile(Context context, int songId, String filePath, int listId) {
        mContext = context;
        Intent intent = new Intent(context, CopyFileService.class);
        intent.setAction(ACTION_COPY_FILE);
        intent.putExtra(EXTRA_SONG_ID, songId);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_LIST_ID, listId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final int songId = intent.getIntExtra(EXTRA_SONG_ID, 0);
            final String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            final int listId = intent.getIntExtra(EXTRA_LIST_ID, 0);
            handleActionFileCopy(songId, filePath, listId);
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFileCopy(final int songId, String filePath, final int listId) {

        mSongId = songId;
        mListId = listId;

        Log.d("CopyFileService", "handleActionFileCopy - songId: " + songId + " filePath: " + filePath);

        if (false == Utility.getInstance().isWiFiConnected()) {

            Snackbar.make(MainActivity.getReferenceView(), "WiFi Network is not connected!",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return;
        }

        // use the file path to copy the music file from the network
        // use the songId to update the metadata in the database once the file has arrived

        SmbFile source = null;
        NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication("", "guest", ""); // domain, user, password
        try {
            jcifs.Config.setProperty("jcifs.resolveOrder", "LMHOSTS,BCAST,DNS");
            source = new SmbFile(filePath, authentication);
        } catch(MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File newDir = new File(path.toString() + "/NMP");
        boolean success = true;
        if (false == newDir.exists()) {
            success = newDir.mkdir();
        }

        // if the "/NMP" directory does not exist then quit, there's nowhere to store a local copy
        if (false == success) {
            // todo log error
            return;
        }

        // make the directory R/W
        newDir.setReadable(true);
        newDir.setWritable(true);

        path = newDir;

        String destFilename;
        FileOutputStream fileOutputStream = null;
        InputStream fileInputStream = null;
        byte[] buf;
        int len;
        if (null != source) {
            destFilename = source.getName();
            //output(sb, logger, "         copying " + smbFile.getName());
            try {
                //fileOutputStream = new FileOutputStream(destFilename);
                File file = new File(path, destFilename);
                if (file.exists()) {
                    Log.d("CopyFileService", "file already exists: " + destFilename);
                } else {
                    Log.d("CopyFileService", "copy file from network: " + path + "/" + destFilename);
                    fileOutputStream = new FileOutputStream(file);

                    fileInputStream = source.getInputStream();
                    buf = new byte[16 * 1024 * 1024];
                    while ((len = fileInputStream.read(buf)) > 0) {
                        Log.d("CopyFileService", "writing buffer to file: " + destFilename);
                        fileOutputStream.write(buf, 0, len);
                        Log.d("CopyFileService", "finished writing buffer of length: " + len);
                    }

                    if (null != fileInputStream) {
                        fileInputStream.close();
                    }
                    if (null != fileOutputStream) {
                        fileOutputStream.close();
                    }
                    buf = null;
                }

                gateLatch = true;

                ScanParams scanParams = new ScanParams(file.toString(), mSongId, mListId);
                ScanMediaTask smt = new ScanMediaTask();
                smt.execute(scanParams);
/*
                MediaScannerConnection.scanFile(this,
                        new String[]{file.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("***** MediaScanner", "ExternalStorage Scanned " + path + ":");
                                Log.i("CopyFileService", "-> uri=" + uri);

                                // start the service that updates the metadata from the file
                                ScanFileService.startFileScan(mContext, path, uri, songId, listId);

                                // release the main thread (the 'await' statement below)
                                //scanLatch.countDown();
                                //Date ended = new Date();
                                //Log.d(" TTTTT CopyFileService", "latch.countDown() called at " + ended.toString());
                                gateLatch = false;
                            }
                        });
*/

                try {
                    //Date started = new Date();
                    int countDown = 10;
                    while (true == gateLatch) {
                        //Log.d(" TTTTT CopyFileService", "starting the latch.await now: " + started.toString());
                        scanLatch.await(100, TimeUnit.MILLISECONDS);
                        countDown--;
                        if (countDown < 1) { gateLatch = false; }
                    }
                    //scanLatch.await(100, TimeUnit.MILLISECONDS);
                    //Date ended = new Date();
                    //Log.d(" TTTTT CopyFileService", "latch.await done " + ended.toString());
                } catch (InterruptedException e) {
                    Log.e("GetFileService", "interrupted!");
                    e.printStackTrace();
                }


            } catch (SmbException e) {
                Log.d("GetFileService", "Exception during stream to output, SMP issue: " + e.getMessage());
                e.printStackTrace();
                return; // false;
            } catch (FileNotFoundException e) {
                Log.d("GetFileService", "Exception during stream to output, file not found: " + e.getMessage());
                e.printStackTrace();
                return; // false;
            } catch (IOException e) {
                Log.d("GetFileService", "Exception during stream to output, IO problem: " + e.getMessage());
                e.printStackTrace();
                return; // false;
            }
        }
    }

    private static class ScanParams {
        String mFilename;
        int mSongId;
        int mListId;

        ScanParams(String filename, int songId, int listId) {
            mFilename = filename;
            mSongId = songId;
            mListId = listId;
        }
    }

    private class ScanMediaTask extends AsyncTask<ScanParams, Void, Void> {
        @Override
        protected Void doInBackground(ScanParams... params) {

            String[] filename = {params[0].mFilename};
            final int songId = params[0].mSongId;
            final int listId = params[0].mListId;

            MediaScannerConnection.scanFile(mContext,
                    filename, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("***** MediaScanner", "ExternalStorage Scanned " + path + ":");
                            Log.i("CopyFileService", "-> uri=" + uri);

                            // start the service that updates the metadata from the file
                            ScanFileService.startFileScan(mContext, path, uri, songId, listId);

                            scanLatch.countDown();
                            //Date ended = new Date();
                            //Log.d(" TTTTT CopyFileService", "latch.countDown() called at " + ended.toString());
                            //gateLatch = false;
                        }
                    });
            try {
                Thread.sleep(200);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
