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

package com.github.rosjava.android_apps.map_nav;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;
import com.google.common.collect.Lists;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PathLayer;
import org.ros.exception.RemoteException;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;
import org.ros.time.NtpTimeProvider;
import org.ros.time.TimeProvider;
import org.ros.time.WallTimeProvider;

import java.sql.Date;
import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import world_canvas_msgs.ListMapsResponse;
import world_canvas_msgs.MapListEntry;
import world_canvas_msgs.PublishMapResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {
	private static final String TAG = "MapNav";

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private VisualizationView mapView;
    private ViewGroup mainLayout;
	private ViewGroup sideLayout;
	private Button backButton;
	private Button chooseMapButton;
	private com.github.rosjava.android_apps.map_nav.MapPosePublisherLayer mapPosePublisherLayer;
	private ProgressDialog waitingDialog;
	private AlertDialog chooseMapDialog;
	private NodeMainExecutor nodeMainExecutor;
	private NodeConfiguration nodeConfiguration;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("Map nav", "Map nav");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		String defaultRobotName = getString(R.string.default_robot);
		String defaultAppName = getString(R.string.default_app);
        setDefaultMasterName(defaultRobotName);
		setDefaultAppName(defaultAppName);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
		cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
		cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
		virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
		backButton = (Button) findViewById(R.id.back_button);
		chooseMapButton = (Button) findViewById(R.id.choose_map_button);
        mapView = (VisualizationView) findViewById(R.id.map_view);
        mapView.onCreate(Lists.<Layer>newArrayList());

        backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});
		chooseMapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onChooseMapButtonPressed();
			}
		});

		mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));
		mainLayout = (ViewGroup) findViewById(R.id.main_layout);
		sideLayout = (ViewGroup) findViewById(R.id.side_layout);

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		super.init(nodeMainExecutor);
		
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());

        String joyTopic = remaps.get(getString(R.string.joystick_topic));
        String camTopic = remaps.get(getString(R.string.camera_topic));

		NameResolver appNameSpace = getMasterNameSpace();
        cameraView.setTopicName(appNameSpace.resolve(camTopic).toString());
        virtualJoystickView.setTopicName(appNameSpace.resolve(joyTopic).toString());

		nodeMainExecutor.execute(cameraView,
				nodeConfiguration.setNodeName("android/camera_view"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("android/virtual_joystick"));

        com.github.rosjava.android_apps.map_nav.ViewControlLayer viewControlLayer =
                new com.github.rosjava.android_apps.map_nav.ViewControlLayer(this,
                		nodeMainExecutor.getScheduledExecutorService(), cameraView,
                		mapView, mainLayout, sideLayout, params);

        String mapTopic      = remaps.get(getString(R.string.map_topic));
        String costmapTopic  = remaps.get(getString(R.string.costmap_topic));
        String scanTopic     = remaps.get(getString(R.string.scan_topic));
        String planTopic     = remaps.get(getString(R.string.global_plan_topic));
        String initTopic     = remaps.get(getString(R.string.initial_pose_topic));
        String robotFrame    = (String) params.get("robot_frame", getString(R.string.robot_frame));

        OccupancyGridLayer mapLayer = new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString());
        OccupancyGridLayer costmapLayer = new OccupancyGridLayer(appNameSpace.resolve(costmapTopic).toString());
        LaserScanLayer laserScanLayer = new LaserScanLayer(appNameSpace.resolve(scanTopic).toString());
        PathLayer pathLayer = new PathLayer(appNameSpace.resolve(planTopic).toString());
        mapPosePublisherLayer = new com.github.rosjava.android_apps.map_nav.MapPosePublisherLayer(this, appNameSpace, params, remaps);
        com.github.rosjava.android_apps.map_nav.InitialPoseSubscriberLayer initialPoseSubscriberLayer =
                new com.github.rosjava.android_apps.map_nav.InitialPoseSubscriberLayer(appNameSpace.resolve(initTopic).toString(), robotFrame);

        mapView.addLayer(viewControlLayer);
        mapView.addLayer(mapLayer);
        mapView.addLayer(costmapLayer);
        mapView.addLayer(laserScanLayer);
        mapView.addLayer(pathLayer);
        mapView.addLayer(mapPosePublisherLayer);
        mapView.addLayer(initialPoseSubscriberLayer);

        mapView.init(nodeMainExecutor);
        viewControlLayer.addListener(new CameraControlListener() {
            @Override
            public void onZoom(float focusX, float focusY, float factor) {}
            @Override
            public void onDoubleTap(float x, float y) {}
            @Override
            public void onTranslate(float distanceX, float distanceY) {}
            @Override
            public void onRotate(float focusX, float focusY, double deltaAngle) {}
        });

		TimeProvider timeProvider = null;
		try {
			NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
					InetAddressFactory.newFromHostString("pool.ntp.org"),
					nodeMainExecutor.getScheduledExecutorService());
			ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
			timeProvider = ntpTimeProvider;
		} catch (Throwable t) {
			Log.w(TAG, "Unable to use NTP provider, using Wall Time. Error: " + t.getMessage(), t);
			timeProvider = new WallTimeProvider();
		}
		nodeConfiguration.setTimeProvider(timeProvider);
		nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));
	}

	private void onChooseMapButtonPressed() {
		readAvailableMapList();
	}

	public void setPoseClicked(View view) {
		setPose();
	}

	public void setGoalClicked(View view) {
		setGoal();
	}

	private void setPose() {
		mapPosePublisherLayer.setPoseMode();
	}

	private void setGoal() {
		mapPosePublisherLayer.setGoalMode();
	}

	private void readAvailableMapList() {
		safeShowWaitingDialog("Waiting...", "Waiting for map list");

        com.github.rosjava.android_apps.map_nav.MapManager mapManager = new com.github.rosjava.android_apps.map_nav.MapManager(this, remaps);
        mapManager.setNameResolver(getMasterNameSpace());
		mapManager.setFunction("list");
		safeShowWaitingDialog("Waiting...", "Waiting for map list");
		mapManager.setListService(new ServiceResponseListener<ListMapsResponse>() {
					@Override
					public void onSuccess(ListMapsResponse message) {
						Log.i(TAG, "readAvailableMapList() Success");
						safeDismissWaitingDialog();
						showMapListDialog(message.getMapList());
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.i(TAG, "readAvailableMapList() Failure");
						safeDismissWaitingDialog();
					}
				});

		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("android/list_maps"));
	}

	/**
	 * Show a dialog with a list of maps. Safe to call from any thread.
	 */
	private void showMapListDialog(final List<MapListEntry> list) {
		// Make an array of map name/date strings.
		final CharSequence[] availableMapNames = new CharSequence[list.size()];
		for (int i = 0; i < list.size(); i++) {
			String displayString;
			String name = list.get(i).getName();
			Date creationDate = new Date(list.get(i).getDate() * 1000);
			String dateTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.SHORT).format(creationDate);
			if (name != null && !name.equals("")) {
				displayString = name + " " + dateTime;
			} else {
				displayString = dateTime;
			}
			availableMapNames[i] = displayString;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				builder.setTitle("Choose a map");
				builder.setItems(availableMapNames,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int itemIndex) {
								loadMap(list.get(itemIndex));
							}
						});
				chooseMapDialog = builder.create();
				chooseMapDialog.show();
			}
		});
	}

	private void loadMap(MapListEntry mapListEntry) {

        com.github.rosjava.android_apps.map_nav.MapManager mapManager = new com.github.rosjava.android_apps.map_nav.MapManager(this, remaps);
        mapManager.setNameResolver(getMasterNameSpace());
		mapManager.setFunction("publish");
		mapManager.setMapId(mapListEntry.getMapId());

		safeShowWaitingDialog("Waiting...", "Loading map");
		try {
			mapManager
					.setPublishService(new ServiceResponseListener<PublishMapResponse>() {
						@Override
						public void onSuccess(PublishMapResponse message) {
							Log.i(TAG, "loadMap() Success");
							safeDismissWaitingDialog();
							// poseSetter.enable();
						}

						@Override
						public void onFailure(RemoteException e) {
							Log.i(TAG, "loadMap() Failure");
							safeDismissWaitingDialog();
						}
					});
		} catch (Throwable ex) {
			Log.e(TAG, "loadMap() caught exception.", ex);
			safeDismissWaitingDialog();
		}
		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("android/publish_map"));
	}

	private void safeDismissChooseMapDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (chooseMapDialog != null) {
					chooseMapDialog.dismiss();
					chooseMapDialog = null;
				}
			}
		});
	}

	private void showWaitingDialog(final CharSequence title,
			final CharSequence message) {
		dismissWaitingDialog();
		waitingDialog = ProgressDialog.show(MainActivity.this, title, message,
				true);
		waitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}

	private void dismissWaitingDialog() {
		if (waitingDialog != null) {
			waitingDialog.dismiss();
			waitingDialog = null;
		}
	}

	private void safeShowWaitingDialog(final CharSequence title,
			final CharSequence message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showWaitingDialog(title, message);
			}
		});
	}

	private void safeDismissWaitingDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dismissWaitingDialog();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.stop_app);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 0:
			onDestroy();
			break;
		}
		return true;
	}
}