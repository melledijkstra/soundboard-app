package nl.melledijkstra.mellesoundboard.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import nl.melledijkstra.mellesoundboard.Config;
import nl.melledijkstra.mellesoundboard.Sound;

/**
 * This Task deletes a Sound from the api
 * Created by melle on 10-10-2016.
 */

public class DeleteSoundTask extends AsyncTask<Sound, Void, String> {

    private static final String TAG = DeleteSoundTask.class.getSimpleName();

    @Nullable
    private Context context;

    private Sound sound;

    private OnDeletedListener listener;
    private int status;

    public DeleteSoundTask(OnDeletedListener listener, @Nullable Context context) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Sound... sounds) {
        this.sound = sounds[0];
        try{
            URL url = new URL(Config.getApiUrl()+Sound.MODEL_NAME+'/'+sound.remote_id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Set correct HTTP method
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("DELETE");
            connection.connect();

            // TODO: check response code if request went fine
            status = connection.getResponseCode();

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                return stringBuilder.toString();
            }
            finally{
                connection.disconnect();
            }
        } catch (IOException e) {
            Log.d(TAG, "Could not make request: "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "Something went wrong from request to "+Config.getApiUrl()+Sound.MODEL_NAME+'/'+sound.remote_id);
        if (result == null && context != null) {
            Toast.makeText(context, "Something went wrong from request to "+Config.getApiUrl()+Sound.MODEL_NAME+'/'+sound.remote_id, Toast.LENGTH_SHORT).show();
        } else {
            listener.onDeleted(sound);
        }
    }

    public interface OnDeletedListener {
        /**
         * This method gets called when the task is done and server deleted the sound model
         */
        void onDeleted(Sound sound);
    }

}
