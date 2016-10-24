package nl.melledijkstra.mellesoundboard.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.jar.Manifest;

import nl.melledijkstra.mellesoundboard.Sound;
import nl.melledijkstra.mellesoundboard.SoundManager;

/**
 * This Class Downloads a sound by specifying a Sound Object
 * Created by melle on 5-10-2016.
 */

public class DownloadSoundTask extends AsyncTask<Sound, Integer, String> {

    private static final String TAG = DownloadSoundTask.class.getSimpleName();
    private final Context context;

    private downloadTaskListener listener;

    private int status;
    // The optional dialog for feedback to the user
    @Nullable
    private ProgressDialog mProgressDialog;
    private Sound sound;
    private boolean errorCaught;

    // The wakelock makes sure the download doesn't stop when going to sleep
    private PowerManager.WakeLock mWakeLock;

    public DownloadSoundTask(Context context,downloadTaskListener listener, @Nullable ProgressDialog dialog) {
        this.context = context;
        this.listener = listener;
        this.mProgressDialog = dialog;
    }

    @Override
    @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    protected String doInBackground(Sound... sounds) {
        this.sound = sounds[0];
        // For downloading
        InputStream input = null;
        // For writing to file
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(sound.downloadLink);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            status = connection.getResponseCode();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (status != HttpURLConnection.HTTP_OK) {
                errorCaught = true;
                return "Server returned HTTP " + status
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int soundLength = connection.getContentLength();

            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // download the actual sound
                input = connection.getInputStream();
                output = new FileOutputStream(SoundManager.MEDIA_PATH+sound.getRemoteFileName());

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                // Loop through downloading sound file
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if(isCancelled()) {
                        mWakeLock.release();
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress...
                    if(soundLength > 0)
                        publishProgress((int) (total * 100 / soundLength));
                    output.write(data, 0, count);
                }

                sound.setLocalFileName(sound.getRemoteFileName());
                sound.setDownloaded(true);

            } else {
                errorCaught = true;
                Log.d(TAG, "External storage not available, so cannot write sound file");
                return "Could not save file to external storage";
            }

        } catch (Exception e) {
            Log.d(TAG, "download failed: "+e.getMessage());
            errorCaught = true;
            e.printStackTrace();
        } finally {
            try {
                // Cleanup
                if(output != null) output.close();
                if(input != null) input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        if (mProgressDialog != null) {
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        mWakeLock.release();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if(status == 404) {
            listener.onSoundNotFound(sound);
        } else if(status >= 300 || errorCaught) {
            listener.onDownloadFailed(status);
            Log.e(TAG, "Download failed with status: "+status+", url: "+sound.downloadLink);
        } else {
            listener.onDownloadDone(sound);
        }
    }

    public interface downloadTaskListener {
        /**
         * Runs when download went fine
         * @param sound The sound file with updated data like the localfile name. This object should be stored in database again
         */
        void onDownloadDone(Sound sound);

        /**
         * Runs when download failed
         * @param status The status of the server, or 0 if server didn't even give a status
         */
        void onDownloadFailed(int status);

        /**
         * This method runs when the server responded with a 404 Not Found, which means this sound should not exist
         * @param sound The sound which couldn't be found and should be deleted
         */
        void onSoundNotFound(Sound sound);
    }
}
