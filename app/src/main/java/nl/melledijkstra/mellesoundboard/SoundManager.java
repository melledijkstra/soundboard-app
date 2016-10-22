package nl.melledijkstra.mellesoundboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import nl.melledijkstra.mellesoundboard.network.DeleteSoundTask;
import nl.melledijkstra.mellesoundboard.network.DownloadSoundTask;
import nl.melledijkstra.mellesoundboard.network.GetChangesTask;

/**
 * The SoundManager knows all about the sounds.
 * It knows:
 * - How to download the files
 * - Which sound need to be downloaded
 * - Create CRUD functionality local and on API
 * Created by melle on 5-10-2016.
 */
public class SoundManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        DeleteSoundTask.OnDeletedListener,
        GetChangesTask.onChangesListener,
        DownloadSoundTask.downloadTaskListener {

    /** The Place where sounds are stored */
    public static final String MEDIA_PATH = Environment.getExternalStorageDirectory().getPath() + "/mellesoundboard/";
    public static String[] allowedExtensions = new String[] {".mp3", ".wav", ".3gp", ".aac"};

    private static final String TAG = SoundManager.class.getSimpleName();

    private onSoundsArrayUpdateListener listener;

    private Context context;

    /** The Android MediaPlayer to play the sounds */
    private MediaPlayer mp;

    private SoundsDatabaseHelper soundsDB;

    /** The sounds for the soundboard */
    public ArrayList<Sound> sounds;

    public SoundManager(Context context, onSoundsArrayUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnCompletionListener(this);
        sounds = new ArrayList<>();
        soundsDB = new SoundsDatabaseHelper(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "Latest sync time: "+prefs.getInt(Config.Preferences.LAST_SYNC_TIME,0));
    }

    /**
     * This method synchronizes the SoundManager with the local files
     * It reads the directory and checks for sounds
     * After this method you can work with the sounds
     */
    public void syncLocalSounds() {
        // Clear the sounds ArrayList otherwise it would add duplicate sounds with every sync
        sounds.clear();
        sounds.addAll(soundsDB.getAllSounds());
        listener.soundsRenewed();
        /*// Get sound directory
        File soundsDir = new File(SoundManager.MEDIA_PATH);
        Log.i(TAG, "Checking "+SoundManager.MEDIA_PATH+" for sound files");
        File[] files = soundsDir.listFiles();
        if(files == null || files.length == 0) return;
        Log.i(TAG, "Files: "+ Arrays.toString(files));
        // Get default sound cover, TODO: create dynamic cover functionality
        Bitmap defaultImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.default_cover);
        for(File file : files) {
            if(Utils.stringContainsItemFromList(file.getName(),allowedExtensions)) {
                sounds.add(new Sound(file.getName(),file, defaultImage));
            }
        }*/
    }

    public Sound getSound(int id) {
        return sounds.get(id);
    }

    public ArrayList<Sound> getSounds() {
        return sounds;
    }

    public void playSound(int position) {
        final Sound sound = sounds.get(position);
        if(sound.isDownloaded() && sound.getSoundFile().exists()) {
            Uri uri = Uri.fromFile(sound.getSoundFile());
            try {
                Log.d(TAG,"playing: "+uri.getPath());
                if(mp.isPlaying()) {
                    mp.stop();
                    mp.reset();
                }
                mp.setDataSource(context,uri);
                mp.prepare();
            } catch (IOException e) {
                Log.e(TAG, "Could not play sound: "+e.getMessage());
                e.printStackTrace();
            }
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Download Sound")
                    .setMessage("This sound is not yet downloaded.\nDo you want to download \""+sound.name+"\"?")
                    .setPositiveButton("Hell Ya!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadSound(sound);
                        }
                    })
                    .setIcon(R.drawable.ic_action_download)
                    .show();
        }
    }

    private void downloadSound(Sound sound) {
        Log.d(TAG, "Sound to be downloaded - " + sound.downloadLink);
        ProgressDialog downloadDialog = new ProgressDialog(context);
        downloadDialog.setMessage("Downloading "+sound.name);
        downloadDialog.setIndeterminate(true);
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadDialog.setCancelable(true);

        final DownloadSoundTask downloadSoundTask = new DownloadSoundTask(context, this, downloadDialog);

        downloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadSoundTask.cancel(true);
            }
        });
    }

    public void syncWithServer() {
        if(Utils.deviceHasInternet(context)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int timestamp = (prefs.getInt(Config.Preferences.LAST_SYNC_TIME, 0));
            Log.d(TAG, "Latest sync time: "+timestamp);
            Log.d(TAG, "Starting synchronization");
            new GetChangesTask(this).execute(timestamp);
        } else {
            Toast.makeText(context, "Get some internet first you dummy! ;p", Toast.LENGTH_SHORT).show();
        }
    }

    public void destroy() {
        if(mp != null) {
            mp.stop();
            mp.release();
        }
    }

    public void deleteSound(int position) {
        // Make DELETE request to server to delete the sound, and if server deleted then handle onDeleted
        new DeleteSoundTask(this, context).execute(sounds.get(position));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
    }

    @Override
    public void onDeleted(Sound sound) {
        // The sound is deleted on server. Now delete locally from db and the file itself
        soundsDB.deleteSound(sound.id);
        sound.deleteFile();
        syncLocalSounds();
        Toast.makeText(context, "Deleted "+sound.name, Toast.LENGTH_SHORT).show();
        soundsDB.close();
    }

    @Override
    public void onDownloadDone(Sound sound) {
        // TODO: THIS PROBABLY ISNT USED ANYMORE!!!
        // TODO: refactor this method, sounds should not be downloaded in bulk anymore!
        // File is downloaded store in database
    }

    @Override
    public void onDownloadFailed(int status) {
        Toast.makeText(context, "Could not download this sound, maybe try again?", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "download failed with status: "+status);
    }

    public void downloadComplete(Intent intent) {
        // file is downloaded
        // TODO: update the sound, that file is downloaded
    }

    @Override
    public void onHttpSuccess(JSONArray json_sounds) {
        // request was done and JSONArray could be made
        if(json_sounds.length() > 0) {
            try {
                // Go through all sounds and store in array
                for(int i = 0; i < json_sounds.length(); ++i) {
                    JSONObject sound_info = json_sounds.getJSONObject(i);
                    Sound sound = new Sound();
                    sound.remote_id     = sound_info.getInt("id");
                    sound.name          = sound_info.getString("name");
                    sound.setRemoteFileName(sound_info.getString("filename"));
                    sound.downloadLink  = sound_info.getString("download_link");
                    sound.createdAt     = sound_info.getInt("created_at");
                    sound.updatedAt     = sound_info.getInt("updated_at");
                    long new_id = soundsDB.createSound(sound);
                    Log.d(TAG, "Sound with id ["+new_id+"] added to database - "+sound);
                }
                syncLocalSounds();
            } catch(JSONException e) {
                Log.d(TAG, "Could not retrieve JSONObject: "+e.getMessage());
                e.printStackTrace();
            }
        } else {
            Toast.makeText(context, "Already synced with server!", Toast.LENGTH_SHORT).show();
        }
        // Set the new syncing time to be now
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(Config.Preferences.LAST_SYNC_TIME, (int) (System.currentTimeMillis() / 1000L))
                .apply();
    }

    @Override
    public void onHttpFailed(int status, String result) {
        Toast.makeText(context, "HTTP Request failed with status: "+status, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onHttpFailed: "+result);
    }

    public boolean deleteAllSounds(String yesiamsure) {
        if(yesiamsure.equals("yesiamsure")) {
            if(soundsDB.deleteAllSounds(true)) {
                syncLocalSounds();
                return true;
            }
        }
        return false;
    }

    public interface onSoundsArrayUpdateListener {
        void soundsRenewed();
    }
}
