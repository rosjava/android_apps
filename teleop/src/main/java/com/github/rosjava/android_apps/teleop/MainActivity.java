/*
 * Copyright (C) 2013 OSRF.
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

package com.github.rosjava.android_apps.teleop;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.github.rosjava.android_apps.application_management.ConcertAppActivity;
import com.github.rosjava.android_apps.application_management.RosAppActivity;
import org.ros.android.view.RosImageView;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.VirtualJoystickView;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends ConcertAppActivity {
    private static final String JOYSTICK_TOPIC = "cmd_vel";
    private static final String CAMERA_TOPIC = "image_color";

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private Button backButton;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and ticker messages.
		super("android teleop", "android teleop");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String defaultRobotName = getString(R.string.default_robot);
		String defaultAppName = getString(R.string.default_app);
//		setDefaultRobotName(defaultRobotName);
		setDefaultAppName(defaultAppName);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);


		cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
		cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
		cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
		virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
		backButton = (Button) findViewById(R.id.back_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		
		super.init(nodeMainExecutor);

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());

        String joyTopic = remaps.containsKey(JOYSTICK_TOPIC) ?
                remaps.get(JOYSTICK_TOPIC) : getString(R.string.default_joystick_topic);

        String camTopic = remaps.containsKey(CAMERA_TOPIC) ?
                remaps.get(CAMERA_TOPIC) : getString(R.string.default_camera_topic);

        NameResolver appNameSpace = getAppNameSpace();
        if (appNameSpace != null) {
            // Resolve app namespace; if we are not in paired mode, appNameSpace will be null
            // TODO: make a dummy appNameSpace that do this transparent to avoid the if ! null everywhere
            joyTopic = appNameSpace.resolve(joyTopic).toString();
            camTopic = appNameSpace.resolve(camTopic).toString();
        }

		cameraView.setTopicName(camTopic);
        virtualJoystickView.setTopicName(joyTopic);
		
		nodeMainExecutor.execute(cameraView, nodeConfiguration
				.setNodeName("android/camera_view"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("android/virtual_joystick"));

		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());
	}
	
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu){
		  menu.add(0,0,0,R.string.stop_app);

		  return super.onCreateOptionsMenu(menu);
	  }
	  
	  @Override
	  public boolean onOptionsItemSelected(MenuItem item){
		  super.onOptionsItemSelected(item);
		  switch (item.getItemId()){
		  case 0:
			  onDestroy();
			  break;
		  }
		  return true;
	  }
	
}
