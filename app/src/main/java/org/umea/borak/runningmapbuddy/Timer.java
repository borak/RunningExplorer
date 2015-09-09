package org.umea.borak.runningmapbuddy;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * This class is a timer which enables observers to listen to its changes in time. This timer will schedule
 * a runnable that increases the seconds by one every second.
 *
 * This class' storable options: Parcelable and Serializable is not reliable after some tests.
 */
public class Timer implements Parcelable, Serializable {
    private java.util.Timer timer = new java.util.Timer();
    private int seconds = 0;
    private int minutes = 0;
    private int hours = 0;
    private List<TimerListener> observers = new ArrayList<>();
    private Object observersLock = new Object();
    private TimerTask timerTask = createTimerTask();
    private boolean isActive = false;
    private final Runnable timerTaskBasicRunnable = new Runnable() {

        @Override
        public void run() {
            seconds++;

            if (seconds == 60) {
                seconds = 0;
                minutes++;
            }

            if (minutes == 60) {
                minutes = 0;
                hours++;
            }

            int secs = hours*60*60 + minutes * 60 + seconds;
            for(TimerListener observer : observers) {
                observer.onTimerChange(secs);
            }
        }
    };

    /**
     * Creates a timer with values 00:00:00, in the format HH:MM:SS.
     */
    public Timer() {
    }

    /**
     * Initializes the timer with the seconds, minutes and hours from the given parcel.
     */
    Timer(Parcel in){
        seconds = in.readInt();
        minutes = in.readInt();
        hours = in.readInt();
    }

    /**
     * Simple creator for enabling Parcelable.
     */
    public static final Creator<Timer> CREATOR = new Creator<Timer>() {
        @Override
        public Timer createFromParcel(Parcel in) {
            return new Timer(in);
        }

        @Override
        public Timer[] newArray(int size) {
            return new Timer[size];
        }
    };

    /**
     * Writes seconds, minutes and hurs to the given Parcel.
     * @param dest The parcel to write to.
     * @param flags Ignored.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(seconds);
        dest.writeInt(minutes);
        dest.writeInt(hours);
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getHours() {
        return hours;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    /**
     * Creates a TimerTask which can be used for scheduling.
     * @return The created TimerTask. .
     */
    private TimerTask createTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                timerTaskBasicRunnable.run();
            }
        };
    }

    /**
     * Schedules and starts a timer for every second to run.
     */
    public synchronized void start() {
        timer.schedule(timerTask = createTimerTask(), 0, 1000);
        this.isActive = true;
    }

    /**
     * Stops the timer.
     */
    public synchronized void stop() {
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if(timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        timer = new java.util.Timer();
        this.isActive = false;
    }

    /**
     * Adds an observer to listen to any changes in the timer. The observer will be called every
     * second if the timer is active.
     *
     * @param observer The observer to start listening.
     */
    public void addObserver(TimerListener observer) {
        synchronized (observersLock) {
            observers.add(observer);
        }
    }

    /**
     * Removes the observer as a listener to the timer.
     * @param observer The observer to remove.
     */
    public void removeObserver(TimerListener observer) {
        synchronized (observersLock) {
            observers.remove(observer);
        }
    }

    public List<TimerListener> getObservers() {
        synchronized (observersLock) {
            return observers;
        }
    }

    public void setObservers(List<TimerListener> observers) {
        synchronized (observersLock) {
            this.observers = observers;
        }
    }

    public void clearObservers() {
        synchronized (observersLock) {
            this.observers = new ArrayList<>();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public int describeContents() {
        return PARCELABLE_WRITE_RETURN_VALUE;
    }

    /**
     * Checks if any second has passed in the timer.
     * @return true if the timer has ever been active by looking at the hours, minutes and seconds
     * alone. false otherwise.
     */
    public boolean hasStarted() {
        if(getSeconds() != 0) {
            return true;
        }
        if(getMinutes() != 0) {
            return true;
        }
        if(getHours() != 0) {
            return true;
        }
        return false;
    }

    /**
     * Implement this interface to enable a class to listen to any changes made to this timer.
     */
    public interface TimerListener {
        /**
         * @param time The time parsed as total seconds.
         */
        void onTimerChange(int time);
    }

    /**
     * Takes the values of the timer and creates a representable string in the format HH:MM:SS.
     * @return A string in the format HH:MM:SS.
     */
    public String toSimpleFormat() {
        String seconds = String.valueOf(getSeconds());
        String minutes = String.valueOf(getMinutes());
        String hours = String.valueOf(getHours());

        String secondsFormatted = seconds.length()<=1 ? 0+String.valueOf(getSeconds()) : String.valueOf(getSeconds());
        String minutesFormatted = minutes.length()<=1 ? 0+String.valueOf(getMinutes()) : String.valueOf(getMinutes());
        String hoursFormatted = hours.length()<=1 ? 0+String.valueOf(getHours()) : String.valueOf(getHours());

        return hoursFormatted + ":" + minutesFormatted + ":" + secondsFormatted;
    }
}
