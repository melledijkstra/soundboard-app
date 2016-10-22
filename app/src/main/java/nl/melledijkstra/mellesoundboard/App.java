package nl.melledijkstra.mellesoundboard;

import android.app.Application;
import android.util.Log;

/**
 * The App class holds all general information which needs to be accessible from the whole application
 * Created by melle on 5-10-2016.
 */

public class App extends Application {

    private static final String TAG = App.class.getSimpleName();

    public App() {
        Log.d(TAG, "Application instantiated");
    }

}
