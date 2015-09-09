package org.umea.borak.runningmapbuddy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * This class is used for the activity_maps.xml layout. This class instantiates and holds the UI for
 * controlling the Google Map v2. A few other functionalities includes: redirection and handling of
 * taking photos with the camera, saving the current state/session to a file and can review a
 * session by giving objects in the bundle for the onCreate method.
 *
 * Saved photoimages will be stored at: the path for
 *      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
 *      + "/Running Explorer/{session ID}/".
 *      , which can look like: "/storage/emulated/0/Pictures/Running Explorer/{session ID}/".
 *
 * Saved MapDataMonitor.class will be stored at: getFilesDir(), which returns
 * "The path of the directory holding application files."
 */
public class MapsActivity extends AppCompatActivity implements Timer.TimerListener {

    public final String TAG = getClass().getName();
    private String mCurrentPhotoPath = null;
    private Uri mCapturedImageURI = null;
    private ImageButton cameraButton, playButton, stopButton;
    private GoogleMap mMap;
    private boolean hasInitializedZoom = false;
    private MapDataMonitor mapDataMonitor;
    private Uri mImageUri;
    private HashMap<Marker, PhotoMarkerDTO> mMarkersHashMap;
    private File extStore = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    private String mapName;
    private String absolutePhotoPath;
    private boolean showingMarkerInfo = false;
    private BitmapFactory.Options bmOptions = null;
    private int photoId = 1;
    private PolylineOptions polylineOptions = null;
    private TextView distanceTv, speedTv, timeTv;
    private Timer timer = new Timer();
    private DecimalFormat df;
    /**
     * A task which refreshes the UI data.
     */
    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {
        distanceTv.setText(df.format(mapDataMonitor.getDistance()));
        speedTv.setText(df.format(mapDataMonitor.getSpeed()));
        timeTv.setText(timer.toSimpleFormat()+"");
        }
    };
    /**
     * This is used to listen for any location change that Google Map will notice. By using this
     * implementation, the camera will be zoomed in to Constants.ZOOM_DEFAULT and update the
     * state of the data in MapDataMonitor.class.
     */
    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if(mMap != null){
                LatLng newPos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPos, Constants.ZOOM_DEFAULT));
                if(timer.isActive() && location.hasAccuracy()
                        && location.getAccuracy() <= Constants.HIGHEST_LOCATION_ACCURACY_ALLOWED) {
                    mapDataMonitor.addPoint(newPos);
                    mapDataMonitor.setSpeed(location.getSpeed());
                    addNewPoint(newPos);
                }
            }
        }
    };

    /**
     * Initializes the UI, listeners, map, session (including unique session ID), folders if not
     * already existing and able to recreate the state togheter with the given bundle.
     * @param savedInstanceState If recreating an old session, three things need to be given in the
     *                           extras bundle in order for this to work:
     *                           Constants.DATA_OBJECT_KEY ==> MapDataMonitor,
     *                           Constants.DATA_PHOTOMARKER_LIST_KEY ==> ArrayList<PhotoMarkerDTO>,
     *                           Constants.DATA_POINTS_LIST_KEY ==> ArrayList<StorablePoints>.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        df.applyPattern("###0.0");

        // Init UI
        cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        playButton = (ImageButton) findViewById(R.id.playButton);
        stopButton = (ImageButton) findViewById(R.id.stopButton);
        distanceTv = (TextView) findViewById(R.id.distanceNumber);
        speedTv = (TextView) findViewById(R.id.speedNumber);
        timeTv = (TextView) findViewById(R.id.time);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager packageManager = getPackageManager();
                if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
                    Toast.makeText(MapsActivity.this, "This device does not have a camera.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(MapsActivity.this, "Please activate your GPS in order " +
                            "to make a picture marker.", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                File file = createImageFile();
                if(file != null) {
                    mImageUri = Uri.fromFile(file);
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                    startActivityForResult(cameraIntent, Constants.REQUEST_TAKE_PHOTO);
                }
            }
        });

        mapName = getResources().getString(R.string.app_name);
        boolean success = false;
        try {
            Bundle extras = getIntent().getExtras();
            mapDataMonitor = extras.getParcelable(Constants.DATA_OBJECT_KEY);
            mapDataMonitor.setPhotoMarkersList((ArrayList) extras.getSerializable(Constants.DATA_PHOTOMARKER_LIST_KEY));
            mapDataMonitor.setStorablePoints((ArrayList) extras.getSerializable(Constants.DATA_POINTS_LIST_KEY));
        } catch (Exception e) {
            Log.e(TAG, " error occurred when restoring data: "+ e.getMessage());
        }

        if(mapDataMonitor != null){
            Log.d(TAG, ": onCreate() creating map from old data. ID = " + mapDataMonitor.getSessionID());
            success = true;
            Log.d(TAG, ": onCreate(): timer="+mapDataMonitor.getTimerAsString());
            initTimerFromString((String) mapDataMonitor.getTimerAsString());
        }

        if(!success) {
            Log.d(TAG, ": onCreate() savedInstanceState is null. Creating a new session.");
            int id = MapDataMonitor.generateSessionID();
            boolean hasUniqueID = false;
            final int ATTEMPTS = 100;
            int attemptCount = 0;
            while (!hasUniqueID) {
                File f = new File(extStore, mapName + File.separator + id);
                if (!f.exists()) {
                    hasUniqueID = true;
                    f.mkdirs();
                    absolutePhotoPath = extStore.getAbsolutePath() + File.separator + mapName + File.separator + id;
                } else {
                    id = MapDataMonitor.generateSessionID();
                    attemptCount++;
                    if(ATTEMPTS <= attemptCount) {
                        Log.e(TAG, "Could not create folder after " + ATTEMPTS + " attempts.");
                        Toast.makeText(this, "You have too many saved sessions.", Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            }

            final int sessionID = id;
            mapDataMonitor = new MapDataMonitor(sessionID);

            timer = new Timer();
            timer.addObserver(this);
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playButton.setVisibility(View.GONE);
                        stopButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.stop();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopButton.setVisibility(View.GONE);
                        playButton.setVisibility(View.VISIBLE);
                    }
                });
                saveDataToDisk();
            }
        });

        setUpMapIfNeeded();

        if(success) {
            Log.d(TAG, ": onCreate() plotting saved markers.");
            try {
                if (mapDataMonitor.getPhotoMarkersList() != null) {
                    for (PhotoMarkerDTO marker : mapDataMonitor.getPhotoMarkersList()) {
                        Log.d(TAG, ": read marker="+marker.getLabel());
                        plotMarker(marker);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, ": An error occurred when triying to restore state from a saved " +
                        "session. Error: " + e.getMessage());
            }
        }
    }

    /**
     * Creates an empty image file for the camera to write the photo to.
     * @return The file link for the newly created image file.
     */
    private File createImageFile() {
        File f = new File(extStore, mapName + File.separator + mapDataMonitor.getSessionID());
        if (!f.exists()) {
            f.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("HH:mm").format(new Date());
        String imageFileName = timeStamp + "_" + photoId;
        File image = new File(absolutePhotoPath + File.separator + imageFileName + ".jpg");
        photoId++;
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    /**
     * Setup of the map if needed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment))
                    .getMap();

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     * This may also recreate lines stored in the MapDataMonitor object if any exists.
     */
    private void setUpMap() {
        GoogleMapOptions options = new GoogleMapOptions();
        options.compassEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.setMyLocationEnabled(true);
        mMap.setPadding(0, 0, 0, convertDpToPx(100));
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        mMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final com.google.android.gms.maps.model.Marker marker) {
                if (showingMarkerInfo) {
                    marker.hideInfoWindow();
                    showingMarkerInfo = false;
                    return false;
                } else if (marker == null) {
                    Log.e(TAG, ": marker is null and cannot show info window.");
                    return false;
                }

                marker.showInfoWindow();
                showingMarkerInfo = true;
                return true;
            }
        });
        mMarkersHashMap = new HashMap<>();

        polylineOptions = new PolylineOptions()
                .color(Color.BLUE)
                .width(5)
                .visible(true)
                .zIndex(30);
        mMap.addPolyline(polylineOptions);

        for(LatLng point : mapDataMonitor.getPoints()) {
            addNewPoint(point);
        }
    }

    /**
     * Adds a coordinate to the map and saves it to the MapDataMonitor object.
     * @param point
     */
    private void addNewPoint(LatLng point) {
        polylineOptions.add(point);
        mapDataMonitor.addPoint(point);
        mMap.addPolyline(polylineOptions);
        Log.d(TAG, ": added point=" + point);
    }

    /**
     * This method handles the result after taking a photo with the camera.
     *
     * Creates a marker on the map by creating a thumbnail of the taken photo and using the
     * current location.
     *
     * @param requestCode Does only support handling of a new photo, recognized by
     *                    Constants.REQUEST_TAKE_PHOTO.
     * @param resultCode If this isn't equal to RESULT_OK, the method will end.
     * @param data Ignored here but is sent to the super method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, ": requestCode=" + requestCode + ", resultCode=" + (resultCode == RESULT_OK ? "OK" : resultCode));

        switch (requestCode) {
            case Constants.REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    boolean success = createMarkerFromLastPhotoTaken();
                    if(!success) {
                        Toast.makeText(this, "An unexpected error occurred while creating marker.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        saveDataToDisk();
                    }
                }
                break;
        }
    }

    /**
     * @return true if successful and false otherwise.
     */
    private boolean createMarkerFromLastPhotoTaken() {
        if(mCurrentPhotoPath == null) {
            Log.e(TAG, "mCurrentPhotoPath is null and onActivityResult(..) could not continue.");
            return false;
        }

        String[] pathSplit = mCurrentPhotoPath.split(File.separator);
        if(pathSplit.length-1 < 0) {
            Log.e(TAG, "image path is unobtainable and onActivityResult(..) could not continue.");
            return false;
        }
        String imageName = pathSplit[pathSplit.length-1];
        File imageFile = new File(absolutePhotoPath, imageName);

        Log.d(TAG, ": imageName="+imageName);
        Log.d(TAG, ": imageFile=" + imageFile + " size="+imageFile.length() + " bytes");
        Log.d(TAG, ": mCurrentPhotoPath=" + mCurrentPhotoPath);

        Location myLocation = mMap.getMyLocation();
        if(myLocation!=null) {
            double dLatitude = myLocation.getLatitude();
            double dLongitude = myLocation.getLongitude();
            PhotoMarkerDTO marker = new PhotoMarkerDTO(imageName, dLatitude, dLongitude,
                    imageFile.getAbsolutePath());
            plotMarker(marker);
            Log.d(TAG, "onActivityResult(..) successfully created a marker. name="
                    + imageName + ", dLatitude=" + dLatitude + ", dLongitude=" + dLongitude);

            mapDataMonitor.addPhotoMarker(marker);
            Log.d(TAG, "onActivityResult(..) lists="+mapDataMonitor.getPoints().size()
                    +", "+mapDataMonitor.getPhotoMarkersList());
            return true;
        }
        Log.e(TAG, "onActivityResult(..) could not continue due to not being able to get my location.");
        return false;
    }

    private Bitmap createThumbnail(String path) {
        if(bmOptions == null) {
            bmOptions = createDownScaledOptions();
        }
        return BitmapFactory.decodeFile(path, bmOptions);
    }

    /**
     * Calculates the given dp value to a value in number of pixels.
     * @param dp The number of dp to calculate from.
     * @return The number of pixels the given dp corresponds to.
     */
    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * Call this method if any the options for downscaling the photo's bitmap is not instanciated
     * already.
     * @return The options for downscaling a photo.
     */
    private BitmapFactory.Options createDownScaledOptions() {
        Camera mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size size = params.getPictureSize();

        int sourceWidth = size.width;
        int sourceHeight = size.height;
        int targetWidth = convertDpToPx((int) R.dimen.thumbnail_preferred_width_dp);
        int targetHeight = convertDpToPx((int) R.dimen.thumbnail_preferred_height_dp);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int scaleFactor = Math.min(sourceWidth/targetWidth, sourceHeight/targetHeight);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Log.d(TAG, ": targetWidth=" + targetWidth + ", targetHeight=" + targetHeight
                + ", sourceWidth=" + sourceWidth + ", sourceHeight=" + sourceHeight
                + ", scaleFactor=" + scaleFactor);

        return bmOptions;
    }

    /**
     * Saves the necessary variables to recreate this state. Example for things that is not saved:
     * the google map fragment and BitmapFactory.Options.
     * @param savedInstanceState The bundle to write the state to.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(Constants.CURRENT_PHOTO_PATH_KEY, mCurrentPhotoPath);
        if(mCapturedImageURI != null) {
            savedInstanceState.putString(Constants.PHOTO_URI_KEY, mCapturedImageURI.toString());
        }
        savedInstanceState.putParcelable(Constants.DATA_OBJECT_KEY, mapDataMonitor);
        savedInstanceState.putBoolean(Constants.HAS_INITIALIZED_ZOOM_KEY, hasInitializedZoom);
        savedInstanceState.putBoolean(Constants.IS_SHOWING_MARKERINFO_KEY, showingMarkerInfo);
        savedInstanceState.putInt(Constants.PHOTO_ID_KEY, photoId);
        savedInstanceState.putString(Constants.PHOTO_FOLDER_PATH_KEY, absolutePhotoPath);

        saveDataToDisk();

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * This method takes the current session data stored in MapDataMonitor and but it in the
     * application folder. The timer is saved as a string representation. The lists in the
     * MapDataMonitor is written separately for avoiding null values inside the lists.
     *
     * During the execution of this method the Thread Policy is changed to permit disk writes,
     * but is then set to the previous policy.
     *
     * @return true if successful and false otherwise.
     */
    private boolean saveDataToDisk() {
        Log.d(TAG, "saveDataToDisk() with ID=" + mapDataMonitor.getSessionID());
        String fileName = ""+mapDataMonitor.getSessionID();
        File file = new File(getFilesDir(), fileName); // creates the file
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                    .permitDiskWrites()
                    .build());
            try {
                Log.d(TAG, "timer="+timer.toSimpleFormat());
                mapDataMonitor.setTimerAsString((String)timer.toSimpleFormat());
                FileOutputStream fout = openFileOutput(fileName, Context.MODE_PRIVATE);;
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(mapDataMonitor);
                oos.writeObject(mapDataMonitor.getPhotoMarkersList());
                oos.writeObject(mapDataMonitor.getStorablePoints());
                oos.flush();
                oos.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, ": Unable to save instance state. " + e.getMessage() + " e="+e.toString());
            }
        } finally {
            StrictMode.setThreadPolicy(old);
        }
        return false;
    }

    /**
     * Saves the state to the disk for reassurring the state to be saved even if the application dies.
     */
    @Override
    public void onBackPressed() {
        saveDataToDisk();
        super.onBackPressed();
    }

    /**
     * Takes a string and initializes the timer according to it's value. The string should be
     * formatted as HH:MM:SS.
     * @param time The time as a string to use for recreating the timer.
     */
    private void initTimerFromString(String time) {
        timer = new Timer();
        String[] split = ((String)time).split(":");
        for(int i=0; i<split.length; i++) {
            if(split[i].startsWith("0")) {
                split[i] = split[i].substring(1);
            }
            Log.d(TAG, "split["+i+"]="+split[i]);
        }
        Log.d(TAG, ""+split + ", time="+time);
        try {
            timer.setHours(Integer.parseInt(split[0]));
            timer.setMinutes(Integer.parseInt(split[1]));
            timer.setSeconds(Integer.parseInt(split[2]));
        } catch(Exception e) {
            Log.e(TAG, "time=" + time + " Error="+e.getMessage());
        }
        Log.d(TAG, "init timer gave: " + timer.toSimpleFormat());
        timer.addObserver(this);
        runOnUiThread(refreshDataTask);
    }

    /**
     * Restores variables from the bundle. Exceptions include the timer and the map that will be
     * recreated with the help of variables in the bundle instead.
     * @param savedInstanceState The bundle that contains variables for recreating the state of
     *                           a session.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(Constants.DATA_OBJECT_KEY)) {
            try {
                mapDataMonitor = savedInstanceState.getParcelable(Constants.DATA_OBJECT_KEY);
            } catch(Exception e) {
                mapDataMonitor = null;
                Log.e(TAG, " Could not restore the mapDataMonitor object.");
                return;
            }
        }
        if (savedInstanceState.containsKey(Constants.CURRENT_PHOTO_PATH_KEY)) {
            mCurrentPhotoPath = savedInstanceState.getString(Constants.CURRENT_PHOTO_PATH_KEY);
        }
        if (savedInstanceState.containsKey(Constants.TIMER_KEY)) {
            timer = savedInstanceState.getParcelable(Constants.TIMER_KEY);
            timer.addObserver(this);
        } else if(mapDataMonitor.getTimerAsString().length() > 0) {
            initTimerFromString(mapDataMonitor.getTimerAsString());
        }
        if (savedInstanceState.containsKey(Constants.PHOTO_URI_KEY)) {
            mCapturedImageURI = Uri.parse(savedInstanceState.getString(Constants.PHOTO_URI_KEY));
        }
        if (savedInstanceState.containsKey(Constants.HAS_INITIALIZED_ZOOM_KEY)) {
            hasInitializedZoom = savedInstanceState.getBoolean(Constants.HAS_INITIALIZED_ZOOM_KEY);
        }
        if (mapDataMonitor != null) {
            mMarkersHashMap = new HashMap<>();
            for(PhotoMarkerDTO pmarker : mapDataMonitor.getPhotoMarkersList()) {
                plotMarker(pmarker);
            }
        }
        if (savedInstanceState.containsKey(Constants.PHOTO_ID_KEY)) {
            photoId = savedInstanceState.getInt(Constants.PHOTO_ID_KEY);
        }
        if (savedInstanceState.containsKey(Constants.IS_SHOWING_MARKERINFO_KEY)) {
            showingMarkerInfo = savedInstanceState.getBoolean(Constants.IS_SHOWING_MARKERINFO_KEY);
        }

        setUpMapIfNeeded();

        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Creates and displays a PhotoMarkerDTO as a Marker in the Google Map.
     * @param marker The data for the marker to create.
     */
    private void plotMarker(PhotoMarkerDTO marker) {
        if(mMap == null || marker == null || marker.getAbsolutePath() == null) {
            Log.e(TAG, ": creating marker failed due to nullpointer.");
            return;
        }
        Log.d(TAG, ": creating marker=" + marker.getLabel());

        MarkerOptions markerOption = new MarkerOptions()
                .position(new LatLng(marker.getLatitude(), marker.getLongitude()))
                .title(marker.getLabel())
                .visible(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        try {
            Marker currentMarker = mMap.addMarker(markerOption);
            mMarkersHashMap.put(currentMarker, marker);

            addNewPoint(new LatLng(marker.getLatitude(), marker.getLongitude()));
        } catch (NullPointerException e) {
            Log.e(TAG, ": null. " + e.getMessage());
        }
    }

    /**
     *
     * @param uri
     */
    private void showPhotoInGallery(Uri uri){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    /**
     * This method will be called by the Timer. This will refresh the UI with any data from
     * the MapDataMonitor object.
     *
     * Furthermore, every 20-seconds, the data will be saved to disk.
     *
     * @param time The time in seconds.
     */
    @Override
    public void onTimerChange(int time) {
        runOnUiThread(refreshDataTask);
        if(time % 20 == 0) {
            saveDataToDisk();
        }
    }

    /**
     * This InfoWindowAdapter will be the adapter in use for every marker. In this class is the
     * memory control of the number of bitmaps that may be on displayed on the Google Map.
     */
    class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter, Serializable {
        private Bitmap lastBitmap = null;

        public MarkerInfoWindowAdapter() {
        }

        /**
         * Ignored.
         * @param marker Ignored.
         * @return null
         */
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        /**
         * Creates a info window for the given marker. The PhotoMarkerDTO which it correspnds to
         * will be fetched and used for creating and instansiating the UI for the window.
         * @param marker The Google Maps Marker.
         * @return The view for the info window.
         */
        @Override
        public View getInfoContents(Marker marker) {
            if (marker == null) {
                Log.e(TAG, ": Failed to show info window. Marker is null.");
                return null;
            }
            if(lastBitmap != null) {
                lastBitmap.recycle();
            }
            View v  = getLayoutInflater().inflate(R.layout.infowindow_layout, null);
            final PhotoMarkerDTO photoMarker = mMarkersHashMap.get(marker);
            if (photoMarker == null) {
                Log.e(TAG, ": Failed to show info window. PhotoMarker is null.");
                return null;
            }

            ImageView markerIcon = (ImageView) v.findViewById(R.id.marker_icon);
            markerIcon.setImageBitmap(lastBitmap = createThumbnail(photoMarker.getAbsolutePath()));
            markerIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = null;
                    try {
                        showPhotoInGallery(uri = Uri.parse(photoMarker.getAbsolutePath()));
                        Log.d(TAG, ": showPhotoInGallery for path=" + uri);
                    } catch (Exception e) {
                        Log.e(TAG, ": exception when displaying photo. photomarker="
                                + photoMarker + " uri=" + uri
                                + "Error: " + e.getMessage());
                    }
                }
            });

            TextView markerLabel = (TextView)v.findViewById(R.id.marker_label);
            markerLabel.setText(photoMarker.getLabel());
            return v;
        }
    }
}
