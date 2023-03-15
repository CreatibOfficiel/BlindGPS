package com.example.blindGPS.ui;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import com.example.blindGPS.R;
import com.example.blindGPS.model.GPX;
import com.example.blindGPS.pdr.DeviceAttitudeHandler;
import com.example.blindGPS.pdr.StepDetectionHandler;
import com.example.blindGPS.pdr.StepPositioningHandler;
import com.example.blindGPS.viewer.GoogleMapTracer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AppActivity extends FragmentActivity implements OnMapReadyCallback {

    // declaration des variables
    private GPX gpx;
    private GoogleMapTracer mGoogleMapTracer;
    private GoogleMap mMap;
    private StepPositioningHandler mStepPositioningHandler;
    protected GoogleMap.OnMapClickListener mOnMapClickListener;
    private SensorManager sensorManager;
    private StepDetectionHandler stepDetectionHandler;
    private DeviceAttitudeHandler deviceAttitudeHandler;

    // definition de la taille du pas
    private final float TAILLEPAS = (float) 0.4;
    private boolean tracing = false; // status du tracé (en cours ou non)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        // On initialise le fichier .gpx
        gpx = new GPX();
        // On essaie d'ouvrir le fichier .gpx
        try {
            InputStream is = getAssets().open("simple.gpx");
            gpx = GPX.parse(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  on ajoute le fragment de la carte
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Récupération de la taille de l'écran
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetectionHandler = new StepDetectionHandler(sensorManager);
        deviceAttitudeHandler = new DeviceAttitudeHandler(sensorManager);
        stepDetectionHandler.setStepDetectionListener(stepListener);
    }

    @Override
    protected void onResume() {
        // on reprend la détection de pas et de l'orientation
        super.onResume();
        stepDetectionHandler.start();
        deviceAttitudeHandler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // on ne met pas en pause les capteurs lors de la veille de l'appareil, sinon ne perd la localisation
        //        stepDetectionHandler.stop();
        //        deviceAttitudeHandler.stop();
    }

    private StepDetectionHandler.StepDetectionListener stepListener = new StepDetectionHandler.StepDetectionListener() {
        // quand un nouveau pas est détecté, on récupère l'angle puis on créé un nouveau point pour faire avancer la trace
        @Override
        public void onNewStepDetected() {
            double bearing = deviceAttitudeHandler.getBearing();
            LatLng newPos = mStepPositioningHandler.computeNextStep(TAILLEPAS, (float) bearing);
            if (mMap != null && tracing)
                mGoogleMapTracer.newPoint(newPos);
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // on récupère la map
        mMap = googleMap;

        // on met la map en mode satelite
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        askLocalisationPermission();

        // on se place sur l'IUT 1
        LatLng grenoble = new LatLng(45.19275317406673, 5.7176893570083545);

        SetLocalisation(grenoble);

        // on instancie le tracer de map
        mGoogleMapTracer = new GoogleMapTracer(mMap, deviceAttitudeHandler);

        // on instancie le gestionnaire de positionnement
        mStepPositioningHandler = new StepPositioningHandler(null);

        mOnMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) { // au clic sur la map
                // si il y a deja une trace en cours, on la termine
                if (tracing) {
                    tracing = false;
                    mGoogleMapTracer.endSegment();
                    Toast toast = Toast.makeText(AppActivity.this, "Fin de la trace", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    // sinon on en démarre une nouvelle
                    tracing = true;
                    if (mStepPositioningHandler.getmCurrentPosition() == null) {
                        mStepPositioningHandler.setmCurrentPosition(latLng);
                    }
                    Toast toast = Toast.makeText(AppActivity.this, "Début de la trace", Toast.LENGTH_SHORT);
                    toast.show();
                    mGoogleMapTracer.newSegment();
                    mGoogleMapTracer.newPoint(mStepPositioningHandler.getmCurrentPosition());
                }
            }
        };
        mMap.setOnMapClickListener(mOnMapClickListener);
    }

    //  fonction d'export d'un fichier gpx selon le tracé de l'utilisateur
    public void export(View view) {
        //  création du fichier gpx
        mGoogleMapTracer.exportToXML("tracks.gpx");

        //  affichage d'un message de confirmation de la sauvegarde du fichier gpx
        Toast toast = Toast.makeText(AppActivity.this, "Exportation faite dans le dossier téléchargement", Toast.LENGTH_SHORT);
        toast.show();
    }

    // fonction d'affichage du tracer d'un fichier gpx
    public void showTrack(View view) {
        //  parcour du fichier gpx
        for (GPX.Track t : gpx) {
            // on instancie un polyline pour afficher la trace
            Polyline line = mMap.addPolyline(new PolylineOptions());

            //  ainsi qu'un tableau de LatLng pour récupérer les points de la trace
            List<LatLng> latLngList = new ArrayList<LatLng>();

            // on parcourt les traces pour récupérer les points
            for (GPX.TrackSegment ts : t) {
                for (GPX.TrackPoint tp : ts) {
                    // on ajoute les points à la liste
                    latLngList.add(tp.position);
                }
            }

            // on ajoute les points au polyline
            line.setPoints(latLngList);
            // avec une epaisseur
            line.setWidth(5);
            // et une couleur
            line.setColor(Color.RED);
        }

        // on recupère la taille de l'écran
        Display d = getWindowManager().getDefaultDisplay();
        DisplayMetrics m = new DisplayMetrics();
        d.getMetrics(m);

        // pour ensuite centrer la map sur la trace
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(gpx.getLatLngBounds(), m.widthPixels, m.heightPixels, 30));
    }

    public void localisation(View view) {
        SetLocalisation(GetLocalisation());
    }

    public void SetLocalisation(LatLng position) {

        // on centre la map sur la dernière localisation connue
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));

        // on zoom sur la dernière localisation connue
        mMap.moveCamera(CameraUpdateFactory.zoomTo(20));
    }

    public LatLng GetLocalisation() {
        // on récupère le service de localisation
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // on récupère la dernière localisation connue
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // point de test de l'IUT 1
        LatLng grenoble = new LatLng(45.19275317406673, 5.7176893570083545);

        LatLng derniereLocalisation = grenoble;

        //  on vérifie que la localisation n'est pas null
        if (location != null) {
            // on crée un LatLng avec les coordonnées de la localisation
            derniereLocalisation = new LatLng(location.getLatitude(), location.getLongitude());
        }

        return derniereLocalisation;
    }

    public void askLocalisationPermission () {
        // on vérifie que l'on a bien les permissions pour accéder à la localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // si on n'a pas les permissions, on les demande
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

    }

}
