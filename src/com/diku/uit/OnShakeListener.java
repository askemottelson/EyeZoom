package com.diku.uit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.widget.ImageView;

// based on http://stackoverflow.com/questions/2317428/android-i-want-to-shake-it
public class OnShakeListener implements SensorEventListener {

  private ImageView view;
  
  private float threshold = 8.0f;
  
  public float mAccel; // acceleration apart from gravity
  public float mAccelCurrent; // current acceleration including gravity
  public float mAccelLast; // last acceleration including gravity
  
  public OnShakeListener(){

  }
  
  public void setView(View v) {
    this.view = (ImageView) v;
  }
  
  @Override
  public void onSensorChanged(SensorEvent se) {
    float x = se.values[0];
    float y = se.values[1];
    float z = se.values[2];
    mAccelLast = mAccelCurrent;
    mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
    float delta = mAccelCurrent - mAccelLast;
    mAccel = mAccel * 0.9f + delta; // perform low-cut filter
    
    // detect shake
    if(mAccel > threshold){
      // reset zoom
      view.getImageMatrix().setScale(1.0f, 1.0f);
      // this will force a redraw in a later cycle (not ui thread)
      view.postInvalidate();
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // TODO Auto-generated method stub
  }

}
