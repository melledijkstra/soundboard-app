package nl.melledijkstra.mellesoundboard;

/**
 * Configuration for the app
 * Created by melle on 10-10-2016.
 */

public class Config {

    public static final String API_URL = "http://soundapi.melledijkstra.nl/";
    public static final int API_VERSION = 1;

    private Config() throws Exception {
        throw new Exception("Don't create a Config instance, this class is only for configuration!");
    }

    public static String getApiUrl() {
        return API_URL+"v"+API_VERSION+"/";
    }

    public class Preferences {
        public static final String LAST_SYNC_TIME = "last_sync_time";
    }
}
