package org.umea.borak.runningmapbuddy;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * This class serves as a data transfer object for a Google Maps Marker. This class is not
 * thread-safe as a monitor but has support for both Parcelable and Serializable.
 */
public class PhotoMarkerDTO implements Parcelable, Serializable {
    private String label;
    private String absolutePath;
    private double latitude;
    private double longitude;

    /**
     * Initializes the object.
     * @param label The label to be displayed in the marker's info window.
     * @param latitude The latitude-position of the marker.
     * @param longitude The longitude-position of the marker.
     * @param absolutePath The absolute path to the image which this marker should display.
     */
    public PhotoMarkerDTO(String label, double latitude, double longitude, String absolutePath) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.absolutePath = absolutePath;
    }

    /**
     * @param in The parcel that contains the object's saved variables.
     */
    PhotoMarkerDTO(Parcel in) {
        this.label = in.readString();
        this.absolutePath = in.readString();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
    }

    public String getLabel() {
        return label;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(absolutePath);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    /**
     * Simple Creator which enables calls to PhotoMarkerDTO(Parcel in).
     */
    public static final Parcelable.Creator<PhotoMarkerDTO> CREATOR = new Parcelable.Creator<PhotoMarkerDTO>() {

        @Override
        public PhotoMarkerDTO createFromParcel(Parcel source) {
            return new PhotoMarkerDTO(source);
        }

        @Override
        public PhotoMarkerDTO[] newArray(int size) {
            return new PhotoMarkerDTO[size];
        }
    };
}
