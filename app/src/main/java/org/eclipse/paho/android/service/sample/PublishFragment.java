/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service.sample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.sample.R;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import 	android.app.Activity;

import android.util.Log;

/**
 * Fragment for the publish message pane.
 *
 */
public class PublishFragment extends Fragment  implements SensorEventListener{

  /**
   * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
   */
  private static final String TAG = "PUBLISH: ";
  /** The handle to a {@link Connection} object which contains the {@link MqttAndroidClient} associated with this object **/
  private String clientHandle = null;

  /** {@link ConnectionDetails} reference used to perform some actions**/
  private ConnectionDetails connectionDetails = null;
  /** {@link ClientConnections} reference used to perform some actions**/
  private ClientConnections clientConnections = null;
  /** {@link Context} used to load and format strings **/
  private Context context = null;

  private int subscribed = 0;

  private final int HANDLER_STOPPED   = -1;
  private final int HANDLER_PAUSED    = 0;
  private final int HANDLER_STARTED   = 1;

  Handler handler = new Handler();
  int handler_status = HANDLER_STOPPED;

  SensorManager sensorManager;
  Sensor sensorLight;
  Sensor sensorAccelerometer;

  float[] lightData;
  float[] accelerometerData;
  int lightAccuracy;
  int accelerometerAccuracy;
  int saysAboutStarting = 0;
  int count = 0;

  View view;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    sensorManager = (SensorManager) getActivity().getSystemService(Singleton.connectionDetails.SENSOR_SERVICE);
    sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    Singleton.publishActivity = getActivity();
    Singleton.needToStop = true;
  }

  @Override
  public void onResume() {
    super.onResume();
    sensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
    sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    startHandler(false);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopHandler(false);
    sensorManager.unregisterListener(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    view = inflater.inflate(R.layout.activity_publish,
            container, false);

    Button button = (Button) view.findViewById(R.id.startPublish);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startHandler(true);
      }
    });

    button = (Button) view.findViewById(R.id.stopPublish);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        stopHandler(true);
      }
    });

    return view;
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
      lightData = sensorEvent.values;
      lightAccuracy = sensorEvent.accuracy;
    }
    else {
      accelerometerData = sensorEvent.values;
      accelerometerAccuracy = sensorEvent.accuracy;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
  }


  public void startHandler(boolean userEvent) {
    if (Singleton.needToStop)
      return;
    if ((handler_status == HANDLER_STARTED) || (userEvent && handler_status == HANDLER_STOPPED) || (!userEvent && handler_status == HANDLER_PAUSED)) {
      count++;
      if (count == 2)
        return;
      if (subscribed != 1)
        subscribe();

      handler_status = HANDLER_STARTED;
      handler.postDelayed(runnable, 1000);
      if (saysAboutStarting == 0) {
        Toast.makeText(Singleton.mainActivity,
                "Publishing sensor data started!", Toast.LENGTH_SHORT).show();
        Button b = (Button) view.findViewById(R.id.startPublish);
        b.setEnabled(false);
        saysAboutStarting = 1;
      }
      count--;
    }
  }

  public void stopHandler(boolean userEvent) {
    if (Singleton.needToStop)
      return;
    if (handler_status == HANDLER_STARTED) {
      handler_status = (userEvent? HANDLER_STOPPED: HANDLER_PAUSED);
      handler.removeCallbacks(runnable);
      Toast.makeText(Singleton.mainActivity,
              "Publishing sensor data stopped!", Toast.LENGTH_SHORT).show();
      Button b = (Button) view.findViewById(R.id.startPublish);
      b.setEnabled(true);
      saysAboutStarting = 0;
    }
    unsubscribe();
  }

  private Runnable runnable = new Runnable() {
    @Override
    public void run() {
      JSONArray message = new JSONArray();
      JSONObject sensor;
      JSONArray values;
      if (lightData == null) {
        Toast.makeText(Singleton.mainActivity, "Can't get light data info", Toast.LENGTH_SHORT).show();
        return;
      }
      try {
        values = new JSONArray();
        for (int i = 0; i < lightData.length; i++) {
          values.put(lightData[i]);
        }
        sensor = new JSONObject()
                .put("name", "light")
                .put("accuracy", lightAccuracy)
                .put("values", values)
                .put("createdAt", new java.sql.Timestamp(new java.util.Date().getTime()).toString());
        message.put(sensor);

        values = new JSONArray();
        for (int i = 0; i < accelerometerData.length; i++) {
          values.put(accelerometerData[i]);
        }
        sensor = new JSONObject()
                .put("name", "accelerometer")
                .put("accuracy", accelerometerAccuracy)
                .put("values", values)
                .put("createdAt", new java.sql.Timestamp(new java.util.Date().getTime()).toString());
        message.put(sensor);
      } catch (JSONException ex) {
        Log.e("SensorMode", "JSON content cannot be created", ex);
      } catch (Exception e) {
        Log.e("SoncorMode", "Can't get light info");
      }

      if (Singleton.connectionDetails.findViewById(R.id.lastWillTopic) != null)
        Singleton.topic = ((EditText) Singleton.connectionDetails.findViewById(R.id.lastWillTopic))
                .getText().toString();
      String topic = Singleton.topic;

      if (Singleton.connectionDetails.findViewById(R.id.qosRadio) != null)
        Singleton.checked = ((RadioGroup) Singleton.connectionDetails.findViewById(R.id.qosRadio)).getCheckedRadioButtonId();
      int checked = Singleton.checked;
      int qos = ActivityConstants.defaultQos;

      switch (checked) {
        case R.id.qos0:
          qos = 0;
          break;
        case R.id.qos1:
          qos = 1;
          break;
        case R.id.qos2:
          qos = 2;
          break;
      }

      if (Singleton.connectionDetails.findViewById(R.id.retained) != null)
        Singleton.retained = ((CheckBox) Singleton.connectionDetails.findViewById(R.id.retained))
                .isChecked();
      boolean retained = Singleton.retained;

      String messageStr = message.toString();
      String[] args = new String[2];
      args[0] = messageStr;
      args[1] = topic + ";qos:" + qos + ";retained:" + retained;

      System.out.println("FRAGMENT CONNECTION: ");
      System.out.println("FRAGMENT CONNECTION: ");

      try {
        Connections.getInstance(Singleton.connectionDetails).getConnection(Singleton.clientHandle).getClient()
                .publish(topic, messageStr.getBytes(), qos, retained, null, new ActionListener(Singleton.connectionDetails, ActionListener.Action.PUBLISH, Singleton.clientHandle, args));
      } catch (MqttSecurityException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
      } catch (MqttException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
      } catch (Exception e) {
        Log.e(this.getClass().getCanonicalName(), "Strange error");
      }

      if (handler_status == HANDLER_STARTED) {
        startHandler(true);
      }
    }
  };

  private void subscribe()
  {
    Singleton.topic = ((EditText) Singleton.connectionDetails.findViewById(R.id.lastWillTopic))
            .getText().toString();

    String topic = Singleton.topic + "/M";

    RadioGroup radio = (RadioGroup) Singleton.connectionDetails.findViewById(R.id.qosRadio);
    int checked = radio.getCheckedRadioButtonId();
    int qos = ActivityConstants.defaultQos;

    switch (checked) {
      case R.id.qos0:
        qos = 0;
        break;
      case R.id.qos1:
        qos = 1;
        break;
      case R.id.qos2:
        qos = 2;
        break;
    }

    boolean retained = ((CheckBox) Singleton.connectionDetails.findViewById(R.id.retained))
            .isChecked();

    try {
      String[] topics = new String[1];
      topics[0] = topic;
      Connections.getInstance(Singleton.connectionDetails).getConnection(Singleton.clientHandle).getClient()
              .subscribe(topic, qos, null, new ActionListener(Singleton.connectionDetails, ActionListener.Action.SUBSCRIBE, Singleton.clientHandle, topics));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + Singleton.clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + Singleton.clientHandle, e);
    }

    subscribed = 1;
  }

  private void unsubscribe() {
    try {
      Connections.getInstance(Singleton.connectionDetails).getConnection(Singleton.clientHandle).getClient().unsubscribe(Singleton.topic + "/M");
    } catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to unsubscribe to" + Singleton.topic + "/M" + " the client with the handle " + Singleton.clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to unsubscribe to" + Singleton.topic + "/M"+ " the client with the handle " + Singleton.clientHandle, e);
    }


    subscribed = 0;
  }


}
