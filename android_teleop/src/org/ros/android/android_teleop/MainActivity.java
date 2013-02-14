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

package org.ros.android.android_teleop;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.ros.android.robotapp.RosAppActivity;
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
public class MainActivity extends RosAppActivity {

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private Button backButton;
	private static final String cameraTopic = "camera/rgb/image_color/compressed_throttle";


	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("android teleop", "android teleop");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String defaultRobotName = getString(R.string.default_robot);
		String defaultAppName = getString(R.string.default_app);
		setDefaultRobotName(defaultRobotName);
		setDefaultAppName(defaultAppName);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		
		cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
		cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
		cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
		virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
		virtualJoystickView.setTopicName("/cmd_vel");
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

		NameResolver appNameSpace = getAppNameSpace();
			cameraView.setTopicName(appNameSpace.resolve(cameraTopic).toString());
		
		nodeMainExecutor.execute(cameraView, nodeConfiguration
				.setNodeName("camera_view"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("virtual_joystick"));


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
