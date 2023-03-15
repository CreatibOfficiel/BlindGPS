package com.example.blindGPS.pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.blindGPS.Utils;

import java.util.ArrayList;

public class StepDetectionHandler implements SensorEventListener {
    private Sensor sensor;
    private SensorManager sensorManager;
    private boolean overSeuil = false;
    private float seuil = (float) 3;
    private ArrayList<Float> pas = new ArrayList<>();

    public StepDetectionHandler(SensorManager sm) {
        sensorManager = sm;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void start(){
        Log.d(Utils.LOG_TAG,"Detection de pas lancé");
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop(){
        Log.d(Utils.LOG_TAG,"Detection de pas arrêté  !");
        sensorManager.unregisterListener(this);
    }

    @Override

    // On detecte les pas grace a lacceleration lineaire, on prend la valeur de l'acceleration sur l'axe Z selon le seuil minimum et maximum
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            pas.add(event.values[2]);
            if(pas.size()>5){ // si une valeur de movement est supérieur à 5 on considère que le mouvement est trop grand, c'est peut-être une erreur de l'utilisateur, on ne le prend pas en compte
                pas.remove(0);
            }
            float moy = 0;
            for (float val: pas) {
                moy+=val;
            }
            moy /= pas.size();

            if(overSeuil) {
                if(moy < seuil)
                    overSeuil = false;
            } else {
                if (moy >= seuil) { // si la valeur moyenne est supérieur au seuil on considère que c'est un pas
                    stepDetectionListener.onNewStepDetected();
                    overSeuil = true;
                }
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private StepDetectionListener stepDetectionListener;

    public void setStepDetectionListener(StepDetectionListener listener){
        stepDetectionListener = listener;
    }

    public interface StepDetectionListener{
        public void onNewStepDetected();
    }
}
