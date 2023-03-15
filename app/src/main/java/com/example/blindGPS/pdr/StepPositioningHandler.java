package com.example.blindGPS.pdr;

import com.google.android.gms.maps.model.LatLng;

public class StepPositioningHandler {
    private LatLng mCurrentPosition;

    public StepPositioningHandler(LatLng mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }

    // on calcul la position grace à la dernière position, à la taille du pas et à l'angle de rotation
    public LatLng computeNextStep(float taillePas, float bearing){
        if(mCurrentPosition != null){
            double angDist = taillePas/6371000;
            double lat = Math.toRadians(mCurrentPosition.latitude);
            double lng = Math.toRadians(mCurrentPosition.longitude);

            double newLat = Math.asin(Math.sin(lat) * Math.cos(angDist) + Math.cos(lat) * Math.sin(angDist) * Math.cos(bearing));
            double newLng = lng + Math.atan2(Math.sin(bearing) * Math.sin(angDist) * Math.cos(lat), Math.cos(angDist) - Math.sin(lat)*Math.sin(newLat) );

            mCurrentPosition = new LatLng(Math.toDegrees(newLat),Math.toDegrees(newLng));
        }
        return mCurrentPosition;
    }

    public LatLng getmCurrentPosition() {
        return mCurrentPosition;
    }

    public void setmCurrentPosition(LatLng mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }
}
