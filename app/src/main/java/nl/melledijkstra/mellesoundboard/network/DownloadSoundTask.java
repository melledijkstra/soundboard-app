package nl.melledijkstra.mellesoundboard.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import nl.melledijkstra.mellesoundboard.Sound;
import nl.melledijkstra.mellesoundboard.SoundManager;

/**
 * This Class Downloads a sound by specifying a Sound Object
 * Created by melle on 5-10-2016.
 */

public class DownloadSoundTask extends AsyncTask<Sound, Integer, String> {

    private static final String TAG = DownloadSoundTask.class.getSimpleName();

    private OnDownloadDone listener;

    // The context who calls the DownloadTask
    private Context context;
    // The optional dialog for feedback to the user
    @Nullable
    private ProgressDialog mProgressDialog;
    private Sound sound;

    private PowerManager.WakeLock mWakeLock;

    public DownloadSoundTask(Context context, @Nullable ProgressDialog dialog, OnDownloadDone listener) {
        this.listener = listener;
        this.context = context;
        this.mProgressDialog = dialog;
    }

    @Override
    protected String doInBackground(Sound... sounds) {
        this.sound = sounds[0];
        // For downloading
        InputStream input = null;
        // For writing to file
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(sounds[0].downloadLink);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int soundLength = connection.getContentLength();

            if(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
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
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress...
                    if(soundLength > 0)
                        publishProgress((int) (total * 100 / soundLength));
                    output.write(data, 0, count);
                }

            } else {
                Log.d(TAG, "External storage not available, so cannot write sound file");
            }

        } catch (Exception e) {
            Log.d(TAG, "download failed: "+e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Cleanup
                if(output != null) output.close();
                if(input != null) input.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Could not cleanup!!! - "+e.getMessage());
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
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if(result != null) {
            Toast.makeText(context, "Download Error: "+result, Toast.LENGTH_SHORT).show();
        } else {
            listener.onDownloadDone(sound);
            Toast.makeText(context, String.format("Sound \"%s\" Downloaded", sound.name), Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnDownloadDone {
        void onDownloadDone(Sound sound);
    }
}
