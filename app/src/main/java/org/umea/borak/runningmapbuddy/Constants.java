package org.umea.borak.runningmapbuddy;

/**
 * Serves as a holder for constants used in the application. The constants includes bundle keys,
 * Google Map settings and intent request codes.
 */
public class Constants {
    // Bundle Keys
    public final static String CURRENT_PHOTO_PATH_KEY = "currentPhotoPath";
    public final static String PHOTO_URI_KEY = "lastCapturedImageURI";
    public final static String DATA_OBJECT_KEY = "currentMapDataMonitor";
    public final static String HAS_INITIALIZED_ZOOM_KEY = "hasInitZoom";
    public final static String TIMER_KEY = "mainTimer";
    public final static String IS_SHOWING_MARKERINFO_KEY = "isShowingMarkerInfo";
    public final static String PHOTO_ID_KEY = "lastAndHighestPhotoID";
    public static final String DOCUMENT_FOLDER_PATH_KEY = "documentAbsolutePath";
    public static final String PHOTO_FOLDER_PATH_KEY = "photoAbsolutePath";
    public static final String LAST_ACTIVITY = "LAST_ACTIVITY";
    public static String DATA_PHOTOMARKER_LIST_KEY = "sessionPhotoMarkerList";
    public static String DATA_POINTS_LIST_KEY = "MarkerAndPhotoMarkerDTOHashmap";

    // Google Map constants
    public static final float ZOOM_DEFAULT = 15f;
    public static final int HIGHEST_LOCATION_ACCURACY_ALLOWED = 16;
    public static final double HIGHEST_DISTANCE_ALLOWED = 50d;
    public static final double LOWEST_DISTANCE_ALLOWED = 20d;

    // Intent Request Codes
    public final static int REQUEST_TAKE_PHOTO = 97;
    public final static int REQUEST_PICK_PHOTO = 103;
}
