package org.umea.borak.runningmapbuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This activity creates a list of previous sessions and fetches data from them in order to easily
 * navigate and overview them directly in the list.
 */
public class SessionListViewActivity extends AppCompatActivity {

    private String mapName;
    public final String TAG = getClass().getName();
    private List<Integer> markedCheckboxes = new ArrayList<>();

    /**
     * Creates the list by initializing the list items from the application's folder.
     * @param savedInstanceState Passed to the super method.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapName = getResources().getString(R.string.app_name);

        checkAndInitActivityUI();
    }

    /**
     * If the folder has files, and then possibly have saved sessions, go forward and try to read
     * them. Otherwise, create an empty list.
     */
    private void checkAndInitActivityUI() {
        File folder = getFilesDir();
        if (folder.listFiles().length > 0) {
            initListActivity(folder);
        } else {
            initEmptyListActivity();
        }
    }

    /**
     * Iterates through each file in the given folder and tries to parse them to MapDataMonitors.
     *
     * During this method's execution the Thread Policy is changed to permit both disk reads and
     * writes. It may delete files that gives IncompatibleClassChangeError due to there nt being an
     * earlier version of this application published.
     *
     * @param folder The folder that contains the saved sessions.
     */
    private void initListActivity(File folder) {
        setContentView(R.layout.activity_session_listview);
        Log.d(TAG, ": Creating list activity with listed items.");
        File[] files = folder.listFiles();
        Log.d(TAG, ": Number of files found=" + files.length);
        List<MapDataMonitor> dataList = new ArrayList<>();
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                    .permitDiskReads()
                    .permitDiskWrites()
                    .build());
            for (File file : files) {
                Log.d(TAG, ": found file with name = " + file.getName());
                try {
                    FileInputStream fin = openFileInput(file.getName());
                    ObjectInputStream ois = new ObjectInputStream(fin);
                    MapDataMonitor data = (MapDataMonitor) ois.readObject();
                    ArrayList<PhotoMarkerDTO> markerList = (ArrayList<PhotoMarkerDTO>) ois.readObject();
                    List<StorablePoint> pointsList = (List<StorablePoint>) ois.readObject();
                    ois.close();
                    Log.d(TAG, ": read object = " + data);
                    if (data != null) {
                        data.setPhotoMarkersList(markerList);
                        data.setStorablePoints(pointsList);
                        dataList.add(data);
                        Log.d(TAG, ": Read data with ID=" + data.getSessionID());
                        for(PhotoMarkerDTO p : data.getPhotoMarkersList()) {
                            Log.d(TAG, ": read p="+p.getLabel());
                        }
                    } else {
                        Log.e(TAG, ": Could not parse file.");
                    }
                } catch (IncompatibleClassChangeError e) {
                    file.delete();
                    Log.e(TAG, ": Could not parse file. Deleting file. Error="+e.getMessage());
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, ": Could not parse file. Ignoring File. ClassNotFoundException Error=" + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, ": Could not parse file. Ignoring File. IOException Error=" + e.getMessage());
                }
            }
        } finally {
            StrictMode.setThreadPolicy(old);
        }

        if(dataList.isEmpty()) {
            initEmptyListActivity();
            return;
        }

        final SessionArrayAdapter adapter = new SessionArrayAdapter(this, dataList);
        final ListView listView = (ListView) findViewById(R.id.sessionlist);
        listView.setAdapter(adapter);
    }

    /**
     * Displays an empty list view.
     */
    private void initEmptyListActivity() {
        setContentView(R.layout.activity_session_listview_empty);
        Log.d(TAG, ": Creating empty list activity.");
        Button button = (Button) findViewById(R.id.list_empty_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewMapActivity();
            }
        });
    }

    /**
     * Starts a new MapActivity without any session in the bundle.
     */
    private void startNewMapActivity() {
        Log.d(TAG, ": startNewMapActivity()");
        Intent intent = new Intent(SessionListViewActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    /**
     * Starts a MapActivity.class toogheter with a previous session.
     * @param data The object that holds the data of the previous session.
     */
    private void startMapActivity(MapDataMonitor data) {
        Log.d(TAG, ": startMapActivity()");
        Intent intent = new Intent(SessionListViewActivity.this, MapsActivity.class);
        intent.putExtra(Constants.DATA_OBJECT_KEY, (Parcelable) data);
        intent.putExtra(Constants.DATA_PHOTOMARKER_LIST_KEY, (Serializable) data.getPhotoMarkersList());
        intent.putExtra(Constants.DATA_POINTS_LIST_KEY, (Serializable) data.getStorablePoints());
        startActivity(intent);
    }

    /**
     * Redirects the user to the folder with the photos and sends the uri to the
     * onActivityResult(..) method.
     * @param finalizedPath The path in which the photos can be found. This will be parsed to
     *                      an URI object.
     */
    void showPhotosInFolder(String finalizedPath) {
        Log.d(TAG, ": showPhotosInFolder");
        Uri uri = Uri.parse(finalizedPath);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(uri, "*/*");
        startActivityForResult(intent, Constants.REQUEST_PICK_PHOTO);
    }

    /**
     * This method only logs the call. In the future, this method could perhaps show a larger image
     * of the given uri in the given param data.
     * @param requestCode Ignored.
     * @param resultCode Ignored.
     * @param data Ignored.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, ": requestCode=" + requestCode + ", resultCode=" + (resultCode == RESULT_OK ? "OK" : resultCode));
        Log.d(TAG, ": data=" + data);

        switch (requestCode) {
            case Constants.REQUEST_PICK_PHOTO:
                if (resultCode == RESULT_OK && data != null) {
                    Log.d(TAG, " data.getData()="+data.getData());
                }
                break;
        }
    }

    /**
     * Creates an actionbar containing a trashcan icon which deletes the marked list items.
     * @param menu The menu to inflate the trashcan to.
     * @return true if successful and false otherwise.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.session_list_menu, menu);
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        deleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(markedCheckboxes.isEmpty()) {
                    Toast.makeText(SessionListViewActivity.this, "Please select the sessions from the list you wish to " +
                            "delete.", Toast.LENGTH_LONG);
                    return false;
                }
                AlertDialog.Builder adb=new AlertDialog.Builder(SessionListViewActivity.this);
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete the selected items?");
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File[] files = getFilesDir().listFiles();
                        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
                        try {
                            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                                    .permitDiskReads()
                                    .permitDiskWrites()
                                    .build());
                            for (File file : files) {
                                Log.d(TAG, ": found file with name = " + file.getName());
                                try {
                                    FileInputStream fin = openFileInput(file.getName());
                                    ObjectInputStream ois = new ObjectInputStream(fin);
                                    Object o = ois.readObject();
                                    ois.close();
                                    Log.d(TAG, ": read object = " + o);
                                    MapDataMonitor data = (MapDataMonitor) o;
                                    if (data != null && markedCheckboxes.contains(data.getSessionID())) {
                                        file.delete();
                                    }
                                } catch (IncompatibleClassChangeError e) {
                                    file.delete();
                                    Log.e(TAG, ": Could not parse file. Deleting file. Error="+e.getMessage());
                                } catch (ClassNotFoundException e) {
                                    Log.e(TAG, ": Could not parse file. Ignoring File. ClassNotFoundException Error=" + e.getMessage());
                                } catch (IOException e) {
                                    Log.e(TAG, ": Could not parse file. Ignoring File. IOException Error=" + e.getMessage());
                                }
                            }

                            initListActivity(getFilesDir());
                        } finally {
                            StrictMode.setThreadPolicy(old);
                        }
                    }});
                adb.show();

                return true;
            }
        });
        return true;
    }

    /**
     * This class holds the list items that are represented in the activity. It sorts the list
     * accordingly to the session's creation date in a TreeSet.class.
     */
    private class SessionArrayAdapter extends ArrayAdapter<MapDataMonitor> {
        SortedSet<MapDataMonitor> sessions;
        final Comparator dateComparator = new Comparator<MapDataMonitor>() {
            @Override
            public int compare(MapDataMonitor lhs, MapDataMonitor rhs) {
                if(lhs == null) {
                    if(rhs != null) {
                        return 1;
                    }
                    return 0;
                } else if(rhs == null) {
                    return -1;
                }
                Log.d(TAG, ": l="+lhs.getCreationDate()+", r="+rhs.getCreationDate());
                return lhs.getCreationDate().compareTo(rhs.getCreationDate());
            }
        };
        HashMap<Integer, MapDataMonitor> sessionIDs = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM HH:mm");

        /**
         * Initializes the list adapter by creating a sorted (by date) TreeSet of MapDataMonitor.
         */
        public SessionArrayAdapter(Context context, List<MapDataMonitor> dataList) {
            super(context, -1, dataList);
            Log.d(TAG, ": creating SessionArrayAdapter. dataList size=" + dataList.size());
            sessions = new TreeSet<>(dateComparator);
            sessions.addAll(dataList);
            Log.d(TAG, ": SessionArrayAdapter. sessions size=" + sessions.size());
            Iterator<MapDataMonitor> iterator = sessions.iterator();
            int id = 0;
            while(iterator.hasNext()) {
                sessionIDs.put(id++, iterator.next());
            }
        }

        /**
         * Creates a new row containing some data to easily recognize the session it corresponds to.
         *
         * @param position Used for fetching the MapDataMonitor.class object.
         * @param convertView Ignored.
         * @param parent Passed along to the context's inflater. Ignored elsewhere.
         * @return The created row view.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, ": getView. (1) Creating a new row.");
            Context context = SessionListViewActivity.this;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_item_session, parent, false);
            final MapDataMonitor data = sessionIDs.get(position);

            if(data == null) {
                Log.e(TAG, ": getView. data null. position="+position);
                return null;
            }
            Log.d(TAG, ": getView. (2) A new row has been created and data has been fetched. ");

            TextView dateView = (TextView) rowView.findViewById(R.id.list_item_date);
            try {
                dateView.setText(dateFormat.format(data.getCreationDate()));
            } catch (NullPointerException e) {
                Log.e(TAG, "Creation date was null when editing a row. session id="+data.getSessionID());
            }

            TextView numPhotosView = (TextView) rowView.findViewById(R.id.list_item_number_of_photos);
            numPhotosView.setText(data.getPhotoMarkersList().size() + " photos");

            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Integer id = data.getSessionID();
                    if (!isChecked) {
                        markedCheckboxes.remove(id);
                    } else if (!markedCheckboxes.contains(id)) {
                        markedCheckboxes.add(id);
                    }
                    Log.d(TAG, "onCheckedChanged " + isChecked + ", number of checked items="
                            + markedCheckboxes.size());
                }
            });

            TextView timerView = (TextView) rowView.findViewById(R.id.list_item_timer);
            timerView.setText(data.getTimerAsString());

            TextView distanceView = (TextView) rowView.findViewById(R.id.list_item_distance);
            distanceView.setText(new DecimalFormat("##.##").format(data.getDistance()) + " km");

            if(position == 0 || position % 2 == 0) {
                rowView.setBackgroundResource(R.color.custom_listBackgroundEven);
            } else {
                rowView.setBackgroundResource(R.color.custom_listBackgroundOdd);
            }

            Log.d(TAG, ": getView. (3) Starting initializing buttons and locating photos.");

            Button gotoMapButton = (Button) rowView.findViewById(R.id.view_button);
            Button gotoPhotosButton = (Button) rowView.findViewById(R.id.view_photos_button);

            // Attempts to get path for the photos
            String path = null;
            try {
                PhotoMarkerDTO marker = data.getPhotoMarkersList().get(0);
                Log.d(TAG, ": marker=" + marker);
                if (marker != null) {
                    String[] split = marker.getAbsolutePath().split(File.separator);
                    Log.d(TAG, ": markerpath="+marker.getAbsolutePath()
                        + " split="+split);
                    StringBuffer buffer = new StringBuffer();
                    for (int i = 0; i < split.length - 1; i++) {
                        buffer.append(split[i]);
                        buffer.append(File.separator);
                    }

                    path = buffer.toString();
                    Log.d(TAG, "path from BUFFER="+path);
                    if (path.length() == 0) {
                        path = null;
                    }
                }
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                Log.d(TAG, ": getView. (4) path=" + path);
            }
            final String finalizedPath = path;

            if(path == null) {
                gotoPhotosButton.setTextColor(
                        getResources().getColor(R.color.common_signin_btn_light_text_disabled));
            } else {
                gotoPhotosButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(finalizedPath == null) {
                            Log.e(TAG, ": path == null");
                            return;
                        }
                        showPhotosInFolder(finalizedPath);
                    }
                });
            }

            gotoMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startMapActivity(data);
                }
            });


            return rowView;
        }
    }

}
