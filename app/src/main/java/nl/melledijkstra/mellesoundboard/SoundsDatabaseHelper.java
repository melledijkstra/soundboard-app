package nl.melledijkstra.mellesoundboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class has connection to the database and has all the CRUD operations for the database
 * Created by melle on 14-10-2016.
 */

public class SoundsDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = SoundsDatabaseHelper.class.getSimpleName();
    Context context;

    public static final String DB_NAME = "soundsdatabase.db";
    public static final int DB_VERSION = 5;

    public SoundsDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    public boolean deleteAllSounds(boolean are_you_sure) {
        if(are_you_sure) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(Sound.TABLE_NAME, "", new String[]{});
            Log.d(TAG, "All sounds deleted from database");
            db.close();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Sound.TABLE_NAME + "("
                + Sound.Columns.ID + " INTEGER PRIMARY KEY,"
                + Sound.Columns.REMOTE_ID + " INTEGER,"
                + Sound.Columns.NAME + " VARCHAR(255),"
                + Sound.Columns.LOCAL_FILE_NAME + " VARCHAR(255),"
                + Sound.Columns.FILE_NAME + " VARCHAR(255),"
                + Sound.Columns.DOWNLOAD_LINK + " VARCHAR(255),"
                + Sound.Columns.DOWNLOADED + " TINYINT(1),"
                + Sound.Columns.CREATED_AT + " INTEGER,"
                + Sound.Columns.UPDATED_AT + " INTEGER"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + Sound.TABLE_NAME);

        onCreate(db);
    }

    public long createSound(Sound sound) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Sound.Columns.REMOTE_ID, sound.remote_id);
        values.put(Sound.Columns.NAME, sound.name);
        values.put(Sound.Columns.LOCAL_FILE_NAME, sound.getLocalFileName());
        values.put(Sound.Columns.FILE_NAME, sound.getRemoteFileName());
        values.put(Sound.Columns.DOWNLOAD_LINK, sound.downloadLink);
        values.put(Sound.Columns.DOWNLOADED, sound.isDownloaded());
        values.put(Sound.Columns.CREATED_AT, sound.createdAt);
        values.put(Sound.Columns.UPDATED_AT, sound.updatedAt);

        // insert row
        long generated_id = db.insert(Sound.TABLE_NAME, null, values);
        db.close();
        return generated_id;
    }

    public ArrayList<Sound> getAllSounds() {
        SQLiteDatabase db = getReadableDatabase();

        ArrayList<Sound> sounds = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM "+Sound.TABLE_NAME, null);

        int count = 0;
        if(c != null && c.moveToFirst()) {
            do {
                Sound sound = fillSound(c);
                sounds.add(sound);
                Log.d(TAG, "Sound retrieved from database - "+sound);
                ++count;
            } while(c.moveToNext());
            c.close();
        }
        Log.d(TAG, "Number of sounds loaded from database: "+count);

        db.close();

        return sounds;
    }

    public Sound getSound(long sound_id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM "+Sound.TABLE_NAME+" WHERE "+Sound.Columns.ID+" = "+sound_id, null);

        Sound sound = null;
        if(c != null) {
            c.moveToFirst();
            sound = fillSound(c);
            c.close();
        }

        db.close();
        return sound;
    }

//    public int updateSound(Sound sound) {
//        SQLiteDatabase db = getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(Sound.Columns.NAME, sound.name);
//        values.put(Sound.Columns.UPDATED_AT, sound.createdAt);
//        values.put(Sound.Columns.FILE_NAME, sound.getRemoteFileName());
//        values.put(Sound.Columns.REMOTE_ID, sound.remote_id);
//
//        return db.update(Sound.TABLE_NAME, values, Sound.Columns.ID+" = ?", new String[] { String.valueOf(sound.getId()) });
//    }

    public boolean deleteSound(long sound_id) {
        SQLiteDatabase db = getWritableDatabase();
        boolean result = db.delete(Sound.TABLE_NAME, Sound.Columns.ID+" = ?", new String[] { String.valueOf(sound_id) }) > 0;
        db.close();
        return result;
    }

    private Sound fillSound(Cursor c) {
        Sound sound = new Sound();
        sound.id            = c.getLong(c.getColumnIndex(Sound.Columns.ID));
        sound.remote_id     = c.getLong(c.getColumnIndex(Sound.Columns.REMOTE_ID));
        sound.name          = c.getString(c.getColumnIndex(Sound.Columns.NAME));
        sound.setLocalFileName(c.getString(c.getColumnIndex(Sound.Columns.LOCAL_FILE_NAME)));
        sound.setRemoteFileName(c.getString(c.getColumnIndex(Sound.Columns.FILE_NAME)));
        sound.setDownloaded(c.getInt(c.getColumnIndex(Sound.Columns.DOWNLOADED)) > 0);
        sound.downloadLink  = c.getString(c.getColumnIndex(Sound.Columns.DOWNLOAD_LINK));
        sound.createdAt     = c.getInt(c.getColumnIndex(Sound.Columns.CREATED_AT));
        sound.updatedAt     = c.getInt(c.getColumnIndex(Sound.Columns.UPDATED_AT));
        return sound;
    }

    public boolean soundExists(long sound_id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM "+Sound.TABLE_NAME+" WHERE "+Sound.Columns.ID+" = "+sound_id, null);
        int count = c.getCount();
        c.close();
        db.close();

        return count > 0;
    }
}
