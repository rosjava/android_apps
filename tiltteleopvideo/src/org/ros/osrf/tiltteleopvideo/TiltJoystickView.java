/*
 * Copyright (C) 2011 OSRF
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.osrf.tiltteleopvideo;

import java.lang.Math;

import geometry_msgs.Twist;

import org.ros.exception.RosException;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.namespace.GraphName;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * View for screen-based joystick teleop.
 */
public class TiltJoystickView extends View implements SensorEventListener, NodeMain {
  private String baseControlTopic;
  private Twist touchCmdMessage;
  private Thread pubThread;
  private Publisher<Twist> twistPub;
  private boolean sendMessages = true;
  private boolean nullMessage = true;
  private Sensor orientationSensor;
  private SensorManager sensorManager;
  private double orientationDeadband = 0.05;

  public TiltJoystickView(Context ctx) {
    super(ctx);
    init(ctx);
  }

  public TiltJoystickView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public TiltJoystickView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    baseControlTopic = "cmd_vel";
    sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
  }

  public void setBaseControlTopic(String t) {
    baseControlTopic = t;
  }

  private <T extends org.ros.internal.message.Message> void createPublisherThread(final Publisher<T> pub, final T message,
      final int rate) {
    pubThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          while (true) {
            if (sendMessages) {
              //Log.i("TiltJoystickView", "send joystick message");
              pub.publish(message);
              if (nullMessage) {
                sendMessages = false;
              }
            } else {
              //Log.i("TiltJoystickView", "skipping joystick");
            }
            Thread.sleep(1000 / rate);
          }
        } catch (InterruptedException e) {
        }
      }
    });
    Log.i("TiltJoystickView", "started pub thread");
    pubThread.start();
  }

  @Override
  public void onStart(ConnectedNode node) { 
    Log.i("TiltJoystickView", "init twistPub");
    touchCmdMessage = node.getTopicMessageFactory().newFromType(Twist._TYPE);
    twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
    createPublisherThread(twistPub, touchCmdMessage, 10);
    // enable our sensor when the activity is resumed, ask for
    // 10 ms updates.
    sensorManager.registerListener(this, orientationSensor, 10000);
  }
  @Override
  public void onShutdown(Node node) {
    // make sure to turn our sensor off when the activity is paused
    sensorManager.unregisterListener(this);
    if (pubThread != null) {
      pubThread.interrupt();
      pubThread = null;
    }
    if (twistPub != null) {
      twistPub.shutdown();
      twistPub = null;
    }
  }
  @Override
  public void onShutdownComplete(Node node) {
  }
  @Override
  public void onError(Node node, Throwable throwable) {
  }
  
  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android/joystick_view");
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
      double pitch = event.values[1];
      double roll = event.values[2];
      if(Math.abs(pitch) < orientationDeadband) {
        pitch = 0.0;
      }
      if(Math.abs(roll) < orientationDeadband) {
        roll = 0.0;
      }

      touchCmdMessage.getLinear().setX(-0.005 * roll);
      touchCmdMessage.getLinear().setY(0);
      touchCmdMessage.getLinear().setZ(0);
      touchCmdMessage.getAngular().setX(0);
      touchCmdMessage.getAngular().setY(0);
      touchCmdMessage.getAngular().setZ(0.02 * pitch);
      sendMessages = true;
      nullMessage = false;
    } else {
      touchCmdMessage.getLinear().setX(0); 
      touchCmdMessage.getLinear().setY(0);            
      touchCmdMessage.getLinear().setZ(0);            
      touchCmdMessage.getAngular().setX(0);             
      touchCmdMessage.getAngular().setY(0);             
      touchCmdMessage.getAngular().setZ(0);  
      nullMessage = true;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }
}

