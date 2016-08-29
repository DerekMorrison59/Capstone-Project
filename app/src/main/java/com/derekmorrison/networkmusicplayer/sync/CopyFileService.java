package com.derekmorrison.networkmusicplayer.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

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
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CopyFileService extends IntentService {

    static Context mContext;
    CountDownLatch scanLatch = new CountDownLatch(1);

    private static final String ACTION_COPY_FILE = "com.derekmorrison.networkmusicplayer.sync.action.COPY_FILE";

    // TODO: Rename parameters
    private static final String EXTRA_SONG_ID = "com.derekmorrison.networkmusicplayer.sync.extra.SONG_ID";
    private static final String EXTRA_FILE_PATH = "com.derekmorrison.networkmusicplayer.sync.extra.FILE_PATH";

    public CopyFileService() {
        super("CopyFileService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startCopyFile(Context context, int songId, String filePath) {
        mContext = context;
        Intent intent = new Intent(context, CopyFileService.class);
        intent.setAction(ACTION_COPY_FILE);
        intent.putExtra(EXTRA_SONG_ID, songId);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final int songId = intent.getIntExtra(EXTRA_SONG_ID, 0);
            final String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            handleActionFoo(songId, filePath);
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(final int songId, String filePath) {

        // use the file path to copy the music file from the network
        // use the songId to update the metadata in the database once the file has arrived

        SmbFile source = null;
        NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication("", "guest", ""); // domain, user, password
        try {
            jcifs.Config.setProperty("jcifs.resolveOrder", "LMHOSTS,BCAST,DNS");
            source = new SmbFile(filePath, authentication);
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File newDir = new File(path.toString() + "/NMP");
        boolean success = true;
        if (false == newDir.exists()) {
            success = newDir.mkdir();
        }
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
                    Log.d("GetFileService", "file already exists: " + destFilename);
                } else {
                    fileOutputStream = new FileOutputStream(file);

                    fileInputStream = source.getInputStream();
                    buf = new byte[16 * 1024 * 1024];
                    while ((len = fileInputStream.read(buf)) > 0) {
                        Log.d("CopyFileService", "writing buffer to file: " + destFilename);
                        fileOutputStream.write(buf, 0, len);
                        Log.d("CopyFileService", "finished writing buffer of length: " + len);
                    }
                }
                MediaScannerConnection.scanFile(this,
                        new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("CopyFileService", "ExternalStorage Scanned " + path + ":");
                                Log.i("CopyFileService", "-> uri=" + uri);

                                // start the service that updates the metadata from the file
                                ScanFileService.startFileScan(mContext, path, uri, songId);

                                // release the main thread (the 'await' statement below)
                                scanLatch.countDown();

                            }
                        });

                try {
                    Log.d("CopyFileService", "starting the latch.await now");
                    scanLatch.await(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Log.e("GetFileService", "interrupted!");
                    e.printStackTrace();
                }



                if (null != fileInputStream) {
                    fileInputStream.close();
                }
                if (null != fileOutputStream) {
                    fileOutputStream.close();
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
}
