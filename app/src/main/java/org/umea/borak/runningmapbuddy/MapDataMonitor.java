package org.umea.borak.runningmapbuddy;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * This class is a transaction-safe data object. This is done by having the operations (methods)
 * only able to be performed one at a time. The data in this class holds any data needed for a
 * user's session, including variables for: map coordinates, google map markers, speed, distance,
 * session ID and similar. By saving an instance of this object, any view in this application can
 * function properly and recreate its states necessary for retake/review sessions.
 */
public class MapDataMonitor implements Parcelable, Timer.TimerListener, Serializable {
    public static String BUNDLE_NAME = "MapDataMonitor";
    private double distance = 0f;
    private double speed = 0f;
    private List<StorablePoint> points;
    private ArrayList<PhotoMarkerDTO> photoMarkersList;
    private float zoomLevel;
    private Date creationDate;
    private final int sessionID;
    private String timerAsString = null;
    private StorablePoint lastPoint = null;

    /**
     * Initializes this instance's variables, exclusively for the given session ID. This object
     * will then always be bound to that session.
     * @param sessionID The ID number for the session. The number's uniqueness and validation is
     *                  not performed in this class.
     */
    public MapDataMonitor(int sessionID) {
        this.sessionID = sessionID;
        points = new ArrayList<>();
        photoMarkersList = new ArrayList<>();
        zoomLevel = Constants.ZOOM_DEFAULT;
        creationDate = new Date();
    }

    /**
     * A constructor for recreating an instance of this object with the Parcelable tool.
     * @param in The parcel which contains the object's state.
     */
    MapDataMonitor(Parcel in){
        this.points = new ArrayList<>();
        this.photoMarkersList = new ArrayList<>();

        this.sessionID = in.readInt();
        this.distance = in.readDouble();
        this.speed = in.readDouble();
        this.timerAsString = in.readString();

        this.points = in.createTypedArrayList(StorablePoint.CREATOR);
        this.photoMarkersList = in.createTypedArrayList(PhotoMarkerDTO.CREATOR);

        this.lastPoint = in.readParcelable(StorablePoint.class.getClassLoader());
        this.zoomLevel = in.readFloat();
        this.creationDate = (Date) in.readSerializable();

        if(this.creationDate == null) {
            this.creationDate = new Date();
        }
    }

    /**
     * @return The speed or velocity in metre per second.
     */
    public synchronized double getSpeed() {
        return speed;
    }

    /**
     * @return The distance in kilometres.
     */
    public synchronized double getDistance() {
        return distance;
    }

    /**
     * @return 0
     */
    @Override
    public synchronized int describeContents() {
        return 0;
    }

    /**
     * Writes this instance's data to the given parcel.
     * @param dest The parcel to write to.
     * @param flags Any flag for this operation, altough, it is not used in this class but may be
     *              passed to other used object's classes.
     */
    @Override
    public synchronized void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(sessionID);
        dest.writeDouble(distance);
        dest.writeDouble(speed);
        dest.writeString(timerAsString);

        dest.writeTypedList(points);
        dest.writeTypedList(photoMarkersList);

        dest.writeParcelable(lastPoint, flags);
        dest.writeFloat(zoomLevel);
        dest.writeSerializable(creationDate);
    }

    /**
     * A creator that is needed for using an instance of this class as a Parcelable.class.
     */
    public static final Parcelable.Creator<MapDataMonitor> CREATOR = new Parcelable.Creator<MapDataMonitor>() {

        @Override
        public MapDataMonitor createFromParcel(Parcel source) {
            return new MapDataMonitor(source);
        }

        @Override
        public MapDataMonitor[] newArray(int size) {
            return new MapDataMonitor[size];
        }
    };

    /**
     * This method should be used to update the speed if not any other tool is available for this
     * calculation.
     * @param time The time in seconds.
     */
    @Override
    public synchronized void onTimerChange(int time) {
        speed = distance / time;
        Log.d(BUNDLE_NAME, "onTimerChange: time="+time +", distance="+distance+ ", speed="+distance / time);
    }

    /**
     * @return The zoom level used for the Google Map.
     */
    public synchronized float getZoomLevel() {
        return zoomLevel;
    }

    /**
     * @param zoomLevel The zoom level used for the Google Map.
     */
    public synchronized void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    /**
     * @return A list containing the photo markers for example the google map.
     */
    public synchronized ArrayList<PhotoMarkerDTO> getPhotoMarkersList() {
        return photoMarkersList;
    }

    /**
     * Adding a photo marker.
     * @param photoMarker The photo marker to add.
     */
    public synchronized void addPhotoMarker(PhotoMarkerDTO photoMarker) {
        this.photoMarkersList.add(photoMarker);
    }

    /**
     * @return the date for when this object was created.
     */
    public synchronized Date getCreationDate() {
        return creationDate;
    }

    /**
     * @return the immutable session ID.
     */
    public synchronized int getSessionID() {
        return sessionID;
    }

    /**
     * A simple random generator for the session ID. No uniqueness is validated here.
     * @return A new session ID number.
     */
    public static int generateSessionID() {
        return new Random().nextInt(88888) + 11111;
    }

    /**
     * @return a list containing coordinates-objects for the google map.
     */
    public synchronized List<LatLng> getPoints() {
        List<LatLng> list = new ArrayList<>();
        for(StorablePoint p : points) {
            list.add(new LatLng(p.x, p.y));
        }
        return list;
    }

    /**
     * @return a list of the points, which is used as coordinates for the google map.
     */
    public synchronized List<StorablePoint> getStorablePoints() {
        return points;
    }

    /**
     * Takes the longitude and latitude in the given LatLng.class and creates a StorablePoint.class.
     * By calling this method, the distance will also be updated.
     * @param point The newest coordinate to add and use.
     */
    public synchronized void addPoint(LatLng point) {
        if(point != null) {
            StorablePoint p = new StorablePoint(point.latitude, point.longitude);
            this.points.add(p);
            if(lastPoint != null) {
                distance += CalculationUtil.distance(new LatLng(lastPoint.x, lastPoint.y), point);
            }
            lastPoint = p;
        }
    }

    /**
     * @param points Overrides any stored points to the given list.
     */
    public synchronized void setStorablePoints(List<StorablePoint> points) {
        this.points = points;
    }

    /**
     * @param photoMarkersList Overrides any stored photo markers to the given list.
     */
    public synchronized void setPhotoMarkersList(ArrayList<PhotoMarkerDTO> photoMarkersList) {
        this.photoMarkersList = photoMarkersList;
    }

    /**
     * This method is an alternative to using the onTimerChange method.
     * @param speed The speed to set to.
     */
    public synchronized void setSpeed(float speed) {
        this.speed = speed;
        Log.d(BUNDLE_NAME, "speed="+speed);
    }

    /**
     * This is an alternative for saving and recreating the timer. This will never return
     * "00:00:00" if the current value is null.
     * @return
     */
    public synchronized String getTimerAsString() {
        if(timerAsString == null) {
            return "00:00:00";
        }
        return timerAsString;
    }

    /**
     * This is an alternative for saving and recreating the timer. Sets the timer as a representable
     * time in the format HH:MM:SS. No validation for this is made here.
     * @param timerAsString The timer representation to set.
     */
    public synchronized void setTimerAsString(String timerAsString) {
        this.timerAsString = timerAsString;
    }
}
