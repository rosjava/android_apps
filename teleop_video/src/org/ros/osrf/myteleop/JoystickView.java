/*
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Willow Garage, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
 
package org.ros.osrf.myteleop;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

/**
 * View for screen-based joystick teleop.
 */
public class JoystickView extends ImageView implements OnTouchListener, NodeMain {
  private String baseControlTopic;
  private Twist touchCmdMessage;
  private Thread pubThread;
  private float motionY;
  private float motionX;
  private Publisher<Twist> twistPub;
  private boolean sendMessages = true;
  private boolean nullMessage = true;

  public JoystickView(Context ctx) {
    super(ctx);
    init(ctx);
  }

  public JoystickView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public JoystickView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    baseControlTopic = "cmd_vel";
    this.setOnTouchListener(this);
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
              //Log.i("JoystickView", "send joystick message");
              pub.publish(message);
              if (nullMessage) {
                sendMessages = false;
              }
            } else {
              //Log.i("JoystickView", "skipping joystick");
            }
            Thread.sleep(1000 / rate);
          }
        } catch (InterruptedException e) {
        }
      }
    });
    Log.i("JoystickView", "started pub thread");
    pubThread.start();
  }
  
  @Override
  public void onStart(ConnectedNode node) { 
    Log.i("JoystickView", "init twistPub");
    touchCmdMessage = node.getTopicMessageFactory().newFromType(Twist._TYPE);
    twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
    createPublisherThread(twistPub, touchCmdMessage, 10);
  }
  @Override
  public void onShutdown(Node node) {
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
  public boolean onTouch(View arg0, MotionEvent motionEvent) {
	  if(touchCmdMessage == null)
		  return false;
	  
    int action = motionEvent.getAction();
    if (arg0 == this && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
      motionX = (motionEvent.getX() - (arg0.getWidth() / 2)) / (arg0.getWidth());
      motionY = (motionEvent.getY() - (arg0.getHeight() / 2)) / (arg0.getHeight());

      touchCmdMessage.getLinear().setX(-2 * motionY);
      touchCmdMessage.getLinear().setY(0);
      touchCmdMessage.getLinear().setZ(0);
      touchCmdMessage.getAngular().setX(0);
      touchCmdMessage.getAngular().setY(0);
      touchCmdMessage.getAngular().setZ(-5 * motionX);
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
    return true;
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("myteleop/joystick_view");
  }
}
