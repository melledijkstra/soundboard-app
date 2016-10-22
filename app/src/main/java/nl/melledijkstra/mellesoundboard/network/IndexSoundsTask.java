package nl.melledijkstra.mellesoundboard.network;

import android.os.AsyncTask;
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

/**
 * This Task does makes a GET request to the sound server to get sounds information
 * which is needed for synchronizing with the server
 * Created by melle on 10-10-2016.
 */

public class IndexSoundsTask extends AsyncTask<String, Void, String> {

    private static final String TAG = IndexSoundsTask.class.getSimpleName();
    private final onDoneListener listener;

    private String url;

    public IndexSoundsTask(onDoneListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... urls) {
        this.url = urls[0];
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urls[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream input = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder buffer = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            Log.d(TAG, buffer.toString());
            return buffer.toString();
        } catch (IOException e) {
            Log.d(TAG,"Something went wrong - "+e.getMessage());
            e.printStackTrace();
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
        try {
            JSONArray json = new JSONArray(result);
            listener.onDone(json);
        } catch (JSONException e) {
            Log.d(TAG, "Could not create JSON from: "+result);
            e.printStackTrace();
        }
    }

    public interface onDoneListener {
        void onDone(JSONArray sounds_info);
    }

}
