package org.umea.borak.runningmapbuddy;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * This class is a simple savable coordinate as both Parcelable and Serializable. The LatLng.class
 * is not storable to file due to it's lacking attributes.
 */
class StorablePoint implements Parcelable, Serializable {
    double x, y;

    StorablePoint(double x, double y) {
        this.x=x;
        this.y=y;
    }

    StorablePoint(Parcel in) {
        this.x=in.readDouble();
        this.y=in.readDouble();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof StorablePoint &&
                ((StorablePoint) o).x == this.x && ((StorablePoint) o).y == this.y) {
            return true;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(x);
        dest.writeDouble(y);
    }

    /**
     * Simple creator for enabling Parcelable.
     */
    public static final Parcelable.Creator<StorablePoint> CREATOR = new Parcelable.Creator<StorablePoint>() {

        @Override
        public StorablePoint createFromParcel(Parcel source) {
            return new StorablePoint(source);
        }

        @Override
        public StorablePoint[] newArray(int size) {
            return new StorablePoint[size];
        }
    };
}
