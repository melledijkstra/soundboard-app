package nl.melledijkstra.mellesoundboard.network;

import android.content.Context;
import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import nl.melledijkstra.mellesoundboard.Config;
import nl.melledijkstra.mellesoundboard.Sound;

/**
 * This Task makes a request to the server to get Sound Changes
 * Created by melle on 15-10-2016.
 */

public class GetChangesTask extends AsyncTask<Integer, Void, String> {

    private static final String TAG = GetChangesTask.class.getSimpleName();
    private final onChangesListener listener;

    private int status;
    private boolean errorCaught;
    private StringBuilder buffer;

    public GetChangesTask(@Nullable onChangesListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Integer... timestamps) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        int timestamp = timestamps[0];
        try {
            URL url = new URL(Config.getApiUrl()+Sound.MODEL_NAME+"/changes/"+timestamp);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();

            status = connection.getResponseCode();

            InputStream input;
            if(status < HttpURLConnection.HTTP_BAD_REQUEST)
                input = connection.getInputStream();
            else
                input = connection.getErrorStream();

            reader = new BufferedReader(new InputStreamReader(input));
            buffer = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            return buffer.toString();
        } catch (Exception e) {
            Log.d(TAG,"Something went wrong - "+e.getMessage());
            errorCaught = true;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        /*if (result != null && !result.isEmpty()) {
            Log.d(TAG, result);
            try {
                JSONArray json = new JSONArray(result);
                if (listener != null) {
                    listener.onHttpSuccess(json);
                }
            } catch (JSONException e) {
                Log.d(TAG, "Could not create JSON from: "+result);
                e.printStackTrace();
            }
        }*/
        if(result != null && !result.isEmpty())
            Log.d(TAG, result);
        if(listener == null)
            return;
        if(status >= 300 || errorCaught)
        {
            listener.onHttpFailed(status, result);
            Log.e(TAG, "HTTP Connection Fault  " + (buffer != null ? buffer.toString() : ""));
        }
        else
            try {
                JSONArray json = new JSONArray(result);
                listener.onHttpSuccess(json);
            } catch (JSONException e) {
                Log.d(TAG, "Could not create JSON from: "+result);
                e.printStackTrace();
            }
    }

    public interface onChangesListener {
        /**
         * This runs when the request for new changes completed correctly
         * @param new_sounds The new sounds which need to be downloaded and stored in database
         */
        void onHttpSuccess(JSONArray new_sounds);

        /**
         * This runs when the request for new changes failed
         * @param status The status given by the server
         * @param result The string result of the server
         */
        void onHttpFailed(int status, String result);
    }

}
