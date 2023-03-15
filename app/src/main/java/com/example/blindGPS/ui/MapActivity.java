package com.example.blindGPS.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;

import com.example.blindGPS.R;
import com.example.blindGPS.model.GPX;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GPX gpx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        gpx = new GPX();
        // Ouverture du fichier .gpx
        try {
            InputStream is = getAssets().open("run.gpx");
            gpx = GPX.parse(is);
        }catch (IOException e){
            e.printStackTrace();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Récupération de la taille de l'écran
        // Peut-être déprécié
        Display d = getWindowManager().getDefaultDisplay();
        DisplayMetrics m = new DisplayMetrics();
        d.getMetrics(m);

        // Le marker pointé sur Grenoble
        LatLng grenoble = new LatLng(45.19275317406673, 5.7176893570083545);

        mMap.addMarker(new MarkerOptions().position(grenoble).title("Marker Grenoble"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(gpx.getLatLngBounds(), m.widthPixels, m.heightPixels, 30));

        showTrack();
    }

    // on charge la structure gpx pour l'afficher sur la carte
    public void showTrack(){
        for(GPX.Track t:gpx){
            Polyline line = mMap.addPolyline(new PolylineOptions());
            List<LatLng> latLngList = new ArrayList<LatLng>();

            for(GPX.TrackSegment ts: t){
                for(GPX.TrackPoint tp: ts){
                    latLngList.add(tp.position);
                }
            }

            line.setPoints(latLngList);
            line.setWidth(5);
            line.setColor(Color.RED);
        }
    }

    public static class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
        }
    }
}