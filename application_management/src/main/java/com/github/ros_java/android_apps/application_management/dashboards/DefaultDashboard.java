/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


package com.github.ros_java.android_apps.application_management.dashboards;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.github.ros_java.android_apps.application_management.Dashboard.DashboardInterface;
import com.github.ros_java.android_apps.application_management.R;
import com.github.ros_java.android_extras.gingerbread.view.BatteryLevelView;

import org.ros.exception.RosException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import java.util.HashMap;
import java.util.List;

import diagnostic_msgs.DiagnosticArray;
import diagnostic_msgs.DiagnosticStatus;
import diagnostic_msgs.KeyValue;

public class DefaultDashboard extends LinearLayout implements DashboardInterface {
    private BatteryLevelView robotBattery;
    private BatteryLevelView laptopBattery;
    private ConnectedNode connectedNode;
    private Subscriber<DiagnosticArray> diagnosticSubscriber;
    private boolean powerOn = false;

    public DefaultDashboard(Context context) {
            super(context);
            inflateSelf(context);
    }
    public DefaultDashboard(Context context, AttributeSet attrs) {
            super(context, attrs);
            inflateSelf(context);
    }
    private void inflateSelf(Context context) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.default_dashboard, this);
            robotBattery = (BatteryLevelView) findViewById(R.id.robot_battery);
            laptopBattery = (BatteryLevelView) findViewById(R.id.laptop_battery);
    }
    /**
     * Set the ROS Node to use to get status data and connect it up. Disconnects
     * the previous node if there was one.
     *
     * @throws org.ros.exception.RosException
     */
    /**
     * Populate view with new diagnostic data. This must be called in the UI
     * thread.
     */
    private void handleDiagnosticArray(DiagnosticArray msg) {
            for(DiagnosticStatus status : msg.getStatus()) {
                    if(status.getName().equals("/Power System/Battery")) {
                            populateBatteryFromStatus(robotBattery, status);
                    }
                    if(status.getName().equals("/Power System/Laptop Battery")) {
                            populateBatteryFromStatus(laptopBattery, status);
                    }
            }
    }


    private void populateBatteryFromStatus(BatteryLevelView view, DiagnosticStatus status) {
            HashMap<String, String> values = keyValueArrayToMap(status.getValues());
            try {
                    float percent = 100 * Float.parseFloat(values.get("Charge (Ah)")) / Float.parseFloat(values.get("Capacity (Ah)"));
                    view.setBatteryPercent((int) percent);
                    // TODO: set color red/yellow/green based on level (maybe with
                    // level-set
                    // in XML)
            } catch(NumberFormatException ex) {
                    // TODO: make battery level gray
            } catch(ArithmeticException ex) {
                    // TODO: make battery level gray
            } catch(NullPointerException ex) {
                    // Do nothing: data wasn't there.
            }
            try {
                    view.setPluggedIn(Float.parseFloat(values.get("Current (A)")) > 0);
            } catch(NumberFormatException ex) {
            } catch(ArithmeticException ex) {
            } catch(NullPointerException ex) {
            }
    }
    private HashMap<String, String> keyValueArrayToMap(List<KeyValue> list) {
            HashMap<String, String> map = new HashMap<String, String>();
            for(KeyValue kv : list) {
                    map.put(kv.getKey(), kv.getValue());
            }
            return map;
    }

	@Override
	public void onShutdown(Node node) {
        if(diagnosticSubscriber != null) {
            diagnosticSubscriber.shutdown();
    }
    diagnosticSubscriber = null;
    connectedNode = null;

	}

	@Override
	public void onStart(ConnectedNode connectedNode) {

		this.connectedNode = connectedNode;
        try {
                diagnosticSubscriber = connectedNode.newSubscriber("diagnostics_agg", "diagnostic_msgs/DiagnosticArray");
                diagnosticSubscriber.addMessageListener(new MessageListener<DiagnosticArray>() {
                        @Override
                        public void onNewMessage(final DiagnosticArray message) {
                                DefaultDashboard.this.post(new Runnable() {
                                        @Override
                                        public void run() {
                                                DefaultDashboard.this.handleDiagnosticArray(message);
                                        }
                                });
                        }
                });
                NameResolver resolver = connectedNode.getResolver().newChild(GraphName.of("/turtlebot_node"));
        } catch(Exception ex) {
                this.connectedNode = null;
                try {
					throw (new RosException(ex));
				} catch (RosException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        }
	}
}