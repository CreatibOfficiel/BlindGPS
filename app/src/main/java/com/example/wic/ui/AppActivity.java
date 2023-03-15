package com.example.wic.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.wic.R;
import com.example.wic.pdr.DeviceAttitudeHandler;
import com.example.wic.pdr.StepDetectionHandler;
import com.example.wic.pdr.StepPositioningHandler;
import com.example.wic.viewer.GoogleMapTracer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class AppActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMapTracer mGoogleMapTracer;
    private GoogleMap mMap;
    private StepPositioningHandler mStepPositioningHandler;
    protected GoogleMap.OnMapClickListener mOnMapClickListener;
    private SensorManager sensorManager;
    private StepDetectionHandler stepDetectionHandler;
    private DeviceAttitudeHandler deviceAttitudeHandler;
    private final float TAILLEPAS = (float) 0.4;
    private boolean tracing = false; // une trace est en cours ou non

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetectionHandler = new StepDetectionHandler(sensorManager);
        deviceAttitudeHandler = new DeviceAttitudeHandler(sensorManager);
        stepDetectionHandler.setStepDetectionListener(stepListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stepDetectionHandler.start();
        deviceAttitudeHandler.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        stepDetectionHandler.stop();
        deviceAttitudeHandler.stop();
    }

    private StepDetectionHandler.StepDetectionListener stepListener = new StepDetectionHandler.StepDetectionListener() {
        // quand un nouveau pas est détecté, on récupère l'angle puis on créé un nouveau point pour faire avancer la trace
        @Override
        public void onNewStepDetected() {
            double bearing = deviceAttitudeHandler.getBearing();
            LatLng newPos = mStepPositioningHandler.computeNextStep(TAILLEPAS, (float) bearing);
            if(mMap != null && tracing)
                mGoogleMapTracer.newPoint(newPos);
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // vu satelitte de la map
        mMap.moveCamera(CameraUpdateFactory.zoomTo(20)); // on zoom pour un meilleur confort au départ

        // centre la map sur un point pour ne pas trop cherche pour les tests
        LatLng maison = new LatLng(45.594757, 5.872533);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(maison));

        mGoogleMapTracer = new GoogleMapTracer(mMap, deviceAttitudeHandler);
        mStepPositioningHandler = new StepPositioningHandler(null); // on défini une position initiale null pour ne pas avoir de point/marker avant un click de l'user

        mOnMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) { // au clic sur la map
                // si une trace est en cours, on la stop, on ajoute un drapeau de fin et on affiche une bulle d'information
                if(tracing){
                    tracing = false;
                    mGoogleMapTracer.endSegment();
                    Toast toast = Toast.makeText(AppActivity.this, "Fin du segment", Toast.LENGTH_SHORT);
                    toast.show();
                }else{
                    // si aucune trace n'est en cours de création, on la démarre avec un drapeau de départ, un nouveau point et un message d'information
                    tracing = true;
                    if(mStepPositioningHandler.getmCurrentPosition() == null){
                        mStepPositioningHandler.setmCurrentPosition(latLng);
                    }
                    Toast toast = Toast.makeText(AppActivity.this, "Début du segment", Toast.LENGTH_SHORT);
                    toast.show();
                    mGoogleMapTracer.newSegment();
                    mGoogleMapTracer.newPoint(mStepPositioningHandler.getmCurrentPosition());
                }
            }
        };
        mMap.setOnMapClickListener(mOnMapClickListener);
    }

    // au clic sur le bouton d'export en haut à droite de la map on lance la fonction de création du fichier et de téléchargement, puis on affiche une bulle d'infotmaition
    public void export(View view){
        mGoogleMapTracer.exportToXML("tracks2.gpx");

        Toast toast = Toast.makeText(AppActivity.this, "Fichier exporté dans les téléchargement", Toast.LENGTH_SHORT);
        toast.show();
    }
}
