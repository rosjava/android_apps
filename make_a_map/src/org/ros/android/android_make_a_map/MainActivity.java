/*
 * Copyright (C) 2011 Google Inc.
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

package org.ros.android.android_make_a_map;

import java.util.concurrent.TimeUnit;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.ros.android.RosAppActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.time.NtpTimeProvider;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

	private static final String MAP_FRAME = "map";
	private static final String ROBOT_FRAME = "base_link";
	private final SystemCommands systemCommands;

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private VisualizationView mapView;
	private ViewGroup mainLayout;
	private ViewGroup sideLayout;
	private Button refreshButton;
	private Button saveButton;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("Make a map", "Make a map");
		systemCommands = new SystemCommands();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
		cameraView.setTopicName("/turtlebot/application/camera/rgb/image_color/compressed_throttle");
		cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
		cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
		mapView = (VisualizationView) findViewById(R.id.map_view);
		virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
		virtualJoystickView.setTopicName("/cmd_vel");
		refreshButton = (Button) findViewById(R.id.refresh_button);
		saveButton = (Button) findViewById(R.id.save_map);

		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				systemCommands.reset();
				Toast.makeText(MainActivity.this, "refreshing map...",
						Toast.LENGTH_SHORT).show();
				mapView.getCamera().jumpToFrame(ROBOT_FRAME);
			}
		});
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(MainActivity.this, "saving map...",
						Toast.LENGTH_SHORT).show();
				systemCommands.saveGeotiff();
			}
		});
		mapView.getCamera().jumpToFrame(ROBOT_FRAME);

		mainLayout = (ViewGroup) findViewById(R.id.main_layout);
		sideLayout = (ViewGroup) findViewById(R.id.side_layout);

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		
		super.init(nodeMainExecutor);

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());

		
		nodeMainExecutor.execute(cameraView, nodeConfiguration
				.setNodeName("/turtlebot/application/camera/rgb/image_color/compressed_throttle"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("virtual_joystick"));

		ViewControlLayer viewControlLayer = new ViewControlLayer(this,
				nodeMainExecutor.getScheduledExecutorService(), cameraView,
				mapView, mainLayout, sideLayout);

		viewControlLayer.addListener(new CameraControlListener() {
			@Override
			public void onZoom(double focusX, double focusY, double factor) {

			}

			@Override
			public void onTranslate(float distanceX, float distanceY) {

			}

			@Override
			public void onRotate(double focusX, double focusY, double deltaAngle) {

			}
		});

		mapView.addLayer(viewControlLayer);
		mapView.addLayer(new OccupancyGridLayer("map"));
		mapView.addLayer(new LaserScanLayer("scan"));
		mapView.addLayer(new RobotLayer(ROBOT_FRAME));
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());
		NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
				InetAddressFactory.newFromHostString("192.168.0.1"),
				nodeMainExecutor.getScheduledExecutorService());
		ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
		nodeConfiguration.setTimeProvider(ntpTimeProvider);
		nodeMainExecutor.execute(mapView, nodeConfiguration);
		
	}
}
