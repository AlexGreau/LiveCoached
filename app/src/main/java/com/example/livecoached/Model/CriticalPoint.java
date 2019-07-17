package com.example.livecoached.Model;

public class CriticalPoint {
    private float latitude;
    private float longitude;

    public CriticalPoint(String lat, String lng){
        this.latitude = Float.parseFloat(lat);
        this.longitude = Float.parseFloat(lng);
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }
}
