package nl.melledijkstra.mellesoundboard;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utils class helps with common programming problems
 * Created by melle on 5-10-2016.
 */

public class Utils {

    private Utils() throws Exception {
        throw new Exception("Don't create a Utils instance, this class is only for the helper methods!");
    }

    public static boolean stringContainsItemFromList(String inputString, String[] items) {
        for (String item : items) {
            if (inputString.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public static String implode(String seperator, String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;i < parts.length - 1;i++) {
            // parts.length - 1 to not get seperator on the end
            if(!parts[i].matches(" *")) { // omit empty strings
                sb.append(parts[i]);
                sb.append(seperator);
            }
        }
        sb.append(parts[parts.length - 1].trim());
        return sb.toString();
    }

    /**
     * Checks if this device has internet connection to make request
     * @param context The context from which to check the internet connection
     * @return True if the device has internet connection false otherwise
     */
    public static boolean deviceHasInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected();
    }
}
