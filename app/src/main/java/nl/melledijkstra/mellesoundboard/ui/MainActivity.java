package nl.melledijkstra.mellesoundboard.ui;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.melledijkstra.mellesoundboard.Config;
import nl.melledijkstra.mellesoundboard.R;
import nl.melledijkstra.mellesoundboard.Sound;
import nl.melledijkstra.mellesoundboard.SoundBoardAdapter;
import nl.melledijkstra.mellesoundboard.SoundManager;
import nl.melledijkstra.mellesoundboard.Utils;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SoundManager.onSoundsArrayUpdateListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int CHOOSE_FILE_INTENT = 2323;
    private static final int REQUEST_EXTERNAL_STORAGE = 54;

    private SoundManager soundManager;

    SoundBoardAdapter adapter;

    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            soundManager.downloadComplete(intent);
        }
    };

    // Views
    SwipeRefreshLayout refresher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Instantiate soundmanager
        soundManager = new SoundManager(this, this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // FAB BUTTON
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addFabBtn);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent getContentIntent = FileUtils.createGetContentIntent();

                    Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                    startActivityForResult(intent, CHOOSE_FILE_INTENT);
                }
            });
        }

        adapter = new SoundBoardAdapter(this, soundManager.sounds);

        // Attach SoundBoardAdapter to gridview to fill grid when sounds update
        GridView gridView = (GridView) findViewById(R.id.theSoundBoard);
        if(gridView != null) {
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(this);
            gridView.setOnItemLongClickListener(this);
        }

        // initiate the refresher
        refresher = (SwipeRefreshLayout) findViewById(R.id.refresher);
        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshGridViewWithSounds();
            }
        });

        // Check if we can use storage permissions
        verifyStoragePermissions(this);

        soundManager.syncWithServer();

        refreshGridViewWithSounds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO: when broadcast listener doesn't know the file is downloaded then the sound in the database doesn't get updated
        unregisterReceiver(onDownloadComplete);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_FILE_INTENT:
                // TODO:
                if(resultCode == RESULT_OK) {
                    final Uri uri = data.getData();

                    // Get the File path from the Uri
                    String path = FileUtils.getPath(this, uri);

                    // Alternatively, use FileUtils.getFile(Context, Uri)
                    if(path != null && FileUtils.isLocal(path)) {
                        File file = new File(path);
                        Toast.makeText(MainActivity.this, "Chosen file: "+file.getName(), Toast.LENGTH_SHORT).show();
                        // If valid extension then copy the file to application media directory
                        if(Utils.stringContainsItemFromList(FileUtils.getExtension(file.getName()),SoundManager.allowedExtensions)) {
                            // copy the file
                            try {
                                Log.d(TAG,"Trying to copy "+file.getPath()+" to "+SoundManager.MEDIA_PATH+file.getName());
                                // verifyStoragePermissions(this);
                                copy(file,new File(SoundManager.MEDIA_PATH+file.getName()));
                                refresher.setRefreshing(true);
                                refreshGridViewWithSounds();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, "Could not copy file, try again", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "This extension is not supported only ("+Utils.implode(", ",SoundManager.allowedExtensions)+")", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },
                REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void refreshGridViewWithSounds() {
        soundManager.syncLocalSounds();
        Log.d(TAG, "Number of sounds loaded: "+soundManager.getSounds().size());
        adapter.notifyDataSetChanged();
        if(refresher.isRefreshing()) refresher.setRefreshing(false);
    }

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        // check if folder structure exists if not create
        if(!dst.getParentFile().isDirectory()) dst.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sync:
                Toast.makeText(this, "Starting synchronization...", Toast.LENGTH_SHORT).show();
                soundManager.syncWithServer();
                break;
            case R.id.action_delete_sounds:
                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to delete all local sounds?")
                        .setNegativeButton("NOOHH!", null)
                        .setPositiveButton("Ehh yeah?", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                soundManager.deleteAllSounds("yesiamsure");
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt(Config.Preferences.LAST_SYNC_TIME, 0).apply();
                                adapter.notifyDataSetChanged();
                            }
                        }).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        soundManager.playSound(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.destroy();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        final String name = soundManager.getSound(position).name;
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete "+name+" ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        soundManager.deleteSound(position);
                    }
                }).setNegativeButton("No, keep that shit", null)
                .show();
        return true;
    }

    @Override
    public void soundsRenewed() {
        adapter.notifyDataSetChanged();
    }
}
