package com.example.livecoached.Model;

public class CriticalPoint {
    private float latitude;
    private float longitude;
    private double bearing;

    public CriticalPoint(String lat, String lng) {
        this.latitude = Float.parseFloat(lat);
        this.longitude = Float.parseFloat(lng);
        this.bearing = 0.0;
    }

    public CriticalPoint(String lat, String lng, String bear) {
        this.latitude = Float.parseFloat(lat);
        this.longitude = Float.parseFloat(lng);
        this.bearing = Double.parseDouble(bear);
    }

    public double getBearing() {
        return bearing;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public boolean matches(CriticalPoint dest, float errorMargin) {
        if (dest.getLatitude() <= (this.latitude + errorMargin) || dest.getLatitude() >= (this.latitude - errorMargin)) {
            if (dest.getLongitude() <= (this.longitude + errorMargin) || dest.getLongitude() >= (this.longitude - errorMargin)) {
                return true;
            }
        }
        return false;
    }
}
