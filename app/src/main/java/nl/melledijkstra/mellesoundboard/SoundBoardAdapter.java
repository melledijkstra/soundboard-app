package nl.melledijkstra.mellesoundboard;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The SoundBoardAdapter fills the GridView and creates the communication between the view and the data
 */
public class SoundBoardAdapter extends BaseAdapter {

    private static final String TAG = SoundBoardAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<Sound> sounds;
    private final LayoutInflater inflater;

    public SoundBoardAdapter(Context mContext, ArrayList<Sound> sounds) {
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
        this.sounds = sounds;
    }

    @Override
    public int getCount() {
        return sounds.size();
    }

    /**
     * Get Sound object at specified position or null if not exists
     */
    @Override
    public Object getItem(int position) {
        return (position >= 0 && position < sounds.size()) ? sounds.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ImageView soundPicture;
        TextView soundName;

        if(v == null) {
            v = inflater.inflate(R.layout.soundboard_item,parent,false);
            v.setTag(R.id.picture, v.findViewById(R.id.picture));
            v.setTag(R.id.text, v.findViewById(R.id.text));
        }

        soundPicture = (ImageView) v.getTag(R.id.picture);
        soundName = (TextView) v.getTag(R.id.text);

        Sound sound = (Sound) getItem(position);
        // TODO: set to sound cover
        soundPicture.setImageResource(R.drawable.default_cover);
        soundName.setText(sound.name);

        Log.d(TAG, "Grid item is generated");

        return v;
    }

}
