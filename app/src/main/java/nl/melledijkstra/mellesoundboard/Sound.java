package nl.melledijkstra.mellesoundboard;

import android.graphics.Bitmap;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import java.io.File;

/**
 * The Sound model class represents a Sound object which is used on the soundboard
 * Created by melle on 10-7-2016.
 */
public class Sound {

    public static final String MODEL_NAME = "sounds";
    // TABLE NAME
    public static final String TABLE_NAME = "sound";

    public class Columns {
        public static final String ID = BaseColumns._ID;
        public static final String NAME = "name";
        public static final String FILE_NAME = "remote_file_name";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
        public static final String REMOTE_ID = "remote_id";
        public static final String LOCAL_FILE_NAME = "local_file_name";
        public static final String DOWNLOADED = "downloaded";
        public static final String DOWNLOAD_LINK = "download_link";
    }

    /** The id of the Sound, this represents the id of the database */
    public long id;

    /**
     * The remote id is the id stored in the server database
     * TODO: do we need this info? and does it need to be different from normal id
     */
    public long remote_id;

    /** Name of the soundboard effect */
    public String name;

    /** The url where the sound can be downloaded */
    public String downloadLink;

    /** Whether the Sound is downloaded */
    private boolean downloaded;

    /** The local sound file */
    private File soundFile;

    /** The local filename // TODO: remove remote filename if the same filename is used */
    private String localFileName;

    /** The remote filename */
    private String remoteFileName;

    /** When the sound was created */
    public int createdAt;

    /** When the sound was updated */
    public int updatedAt;

    /** The image to display as background on the grid */
    private Bitmap image;

    public Sound(String name, File soundFile, @Nullable Bitmap image) {
        this(name);
        this.soundFile = soundFile;
        this.image = image;
    }

    public Sound(String name) {
        this.name = name;
    }

    public Sound() {}

    @Override
    public String toString() {
        return (this.soundFile != null) ? String.format("Sound{name: %s, soundFile: %s, downloaded: %b, downloadLink: %s }",name,soundFile.getPath(),downloaded,downloadLink) : String.format("Sound{name: %s, downloaded: %b, downloadLink: %s }",name,downloaded,downloadLink);
    }

    public File getSoundFile() {
        return soundFile;
    }

    public String getRemoteFileName() {
        return remoteFileName;
    }

    public long getId() {
        return id;
    }

    public void setRemoteFileName(String remoteFileName) {
        this.remoteFileName = remoteFileName;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getLocalFileName() {
        return localFileName;
    }

    public void setLocalFileName(@Nullable String localFileName) {
        if(localFileName != null && Utils.stringContainsItemFromList(localFileName, SoundManager.allowedExtensions) && new File(SoundManager.MEDIA_PATH + localFileName).exists()) {
            soundFile = new File(SoundManager.MEDIA_PATH + localFileName);
        } else {
            soundFile = null;
        }
    }

    /**
     * Deletes the file associated with this sound object
     * @return true if this file was deleted, false otherwise.
     */
    public boolean deleteFile() {
        return soundFile.delete();
    }

}
