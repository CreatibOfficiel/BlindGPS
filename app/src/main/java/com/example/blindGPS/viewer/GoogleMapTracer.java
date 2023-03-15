package com.example.blindGPS.viewer;

import android.os.Environment;
import android.util.Xml;

import com.example.blindGPS.R;
import com.example.blindGPS.pdr.DeviceAttitudeHandler;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

public class GoogleMapTracer {
    private GoogleMap mGoogleMap;
    private ArrayList<Polyline> track;
    private ArrayList<LatLng> bufferPolyline;
    private Marker markerSimple;
    public static String GPX = "gpx";
    public static String TRK = "trk";
    public static String TRKSEG = "trkseg";
    public static String TRKPT = "trkpt";
    public static String LON = "lon";
    public static String LAT = "lat";

    public GoogleMapTracer(GoogleMap mGoogleMap, DeviceAttitudeHandler deviceAttitudeHandler) {
        this.mGoogleMap = mGoogleMap;
        track = new ArrayList<>();
        bufferPolyline = new ArrayList<>();
        deviceAttitudeHandler.setDeviceAttitudeListener(deviceAttitudeListener);
    }

    // Création d'un nouveau segment
    public void newSegment(){
        bufferPolyline.clear();
        track.add(mGoogleMap.addPolyline(new PolylineOptions()));
    }

    // On cloture le segment en cours et on affiche le marker de fin
    public void endSegment(){
        if(!bufferPolyline.isEmpty()){
            if(markerSimple != null) {
                markerSimple.remove();
                markerSimple = null;
            }
            mGoogleMap.addMarker(new MarkerOptions().position(bufferPolyline.get(bufferPolyline.size()-1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            // mGoogleMap.addMarker(new MarkerOptions().position(bufferPolyline.get(bufferPolyline.size()-1)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.trace_end)));
        }
    }

    // Création d'un nouveau point sur la trace, on supprime la flèche de suivi et on l'affiche au nouvel androi
    public void newPoint(LatLng point){
        if(markerSimple != null) {
            markerSimple.remove();
            markerSimple = null;
        }
        if(bufferPolyline.isEmpty()){
            mGoogleMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            // mGoogleMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.fromResource(R.mipmap.trace_start)));
        }else{
            // markerSimple = mGoogleMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            markerSimple = mGoogleMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.fromResource(R.mipmap.arrow)));
        }

        // on fait suivre la caméra sur le nouveau point pour le garder centré
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(point));

        bufferPolyline.add(point);
        track.get(track.size()-1).setPoints(bufferPolyline);
    }

    private DeviceAttitudeHandler.DeviceAttitudeListener deviceAttitudeListener = new DeviceAttitudeHandler.DeviceAttitudeListener() {
        @Override
        public void rotationChanged(double angle) {
            if(markerSimple != null) {
                // au changement de point, si l'angle à changé on change l'angle de l'icône utilisateur
                markerSimple.setRotation((float)Math.toDegrees(angle));
            }
        }
    };

    // Exportation des traces en GPX
    public void exportToXML(String fileName){
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        // on construit le fichier gpx à partir du tableau track
        try{
            xmlSerializer.setOutput(writer);
            // début du document
            xmlSerializer.startDocument("UTF-8", true);
            // tag: <GPX>
            xmlSerializer.startTag("", GPX);
            xmlSerializer.attribute("", "version", "1.1");
            xmlSerializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
            xmlSerializer.attribute("", "xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
            // tag: <TRK>
            xmlSerializer.startTag("", TRK);
            for (Polyline segment : track) {
                // tag: <TRKSEG>
                xmlSerializer.startTag("",TRKSEG);
                for (LatLng point : segment.getPoints()){
                    // tag: <TRKPT>
                    xmlSerializer.startTag("", TRKPT);
                    xmlSerializer.attribute("", LON, String.valueOf(point.longitude));
                    xmlSerializer.attribute("", LAT, String.valueOf(point.latitude));
                    xmlSerializer.endTag("",TRKPT);
                }
                xmlSerializer.endTag("",TRKSEG);
            }
            xmlSerializer.endTag("", TRK);
            xmlSerializer.endTag("", GPX);
            // fin du document
            xmlSerializer.endDocument();
            // création du fichier physique
            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName);
            if(!f.exists()){
                f.createNewFile();
            }
            FileWriter fw = new FileWriter(f);
            // Ecriture dans le fichier
            fw.write(writer.toString());
            // clôture du fichier
            fw.close();
        }catch (IOException io){
            System.err.println(io.getMessage());
        }
    }
}
