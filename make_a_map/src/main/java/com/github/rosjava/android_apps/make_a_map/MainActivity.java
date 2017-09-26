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

package com.github.rosjava.android_apps.make_a_map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.time.NtpTimeProvider;
import org.ros.time.TimeProvider;
import org.ros.time.WallTimeProvider;

import java.util.concurrent.TimeUnit;

import world_canvas_msgs.SaveMapResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

    private static final int NAME_MAP_DIALOG_ID = 0;

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private VisualizationView mapView;
	private ViewGroup mainLayout;
	private ViewGroup sideLayout;
	private ImageButton refreshButton;
	private ImageButton saveButton;
	private Button backButton;
	private NodeMainExecutor nodeMainExecutor;
	private NodeConfiguration nodeConfiguration;
	private ProgressDialog waitingDialog;
	private AlertDialog notiDialog;


    private OccupancyGridLayer occupancyGridLayer = null;
    private LaserScanLayer laserScanLayer = null;
    private RobotLayer robotLayer = null;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("Make a map", "Make a map");

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
		refreshButton = (ImageButton) findViewById(R.id.refresh_button);
		saveButton = (ImageButton) findViewById(R.id.save_map);
		backButton = (Button) findViewById(R.id.back_button);

        mapView = (VisualizationView) findViewById(R.id.map_view);
        mapView.onCreate(Lists.<Layer>newArrayList());

        refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO
				Toast.makeText(MainActivity.this, "refreshing map...",
						Toast.LENGTH_SHORT).show();
                mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));
			}
		});

		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(NAME_MAP_DIALOG_ID);

			}

		});

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});

        mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));

		mainLayout = (ViewGroup) findViewById(R.id.main_layout);
		sideLayout = (ViewGroup) findViewById(R.id.side_layout);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		Button button;

		switch (id) {
		case NAME_MAP_DIALOG_ID:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.name_map_dialog);
			dialog.setTitle("Save Map");
			final EditText nameField = (EditText) dialog
					.findViewById(R.id.name_editor);

			nameField.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(final View view, int keyCode,
						KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_DOWN
							&& keyCode == KeyEvent.KEYCODE_ENTER) {
						safeShowWaitingDialog("Saving map...");
						try {
                            final MapManager mapManager = new MapManager(MainActivity.this, remaps);
							String name = nameField.getText().toString();
							if (name != null) {
								mapManager.setMapName(name);
							}
                            mapManager.setNameResolver(getMasterNameSpace());
                            mapManager.registerCallback(new MapManager.StatusCallback() {
                                @Override
                                public void timeoutCallback() {
                                    safeDismissWaitingDialog();
                                    safeShowNotiDialog("Error", "Timeout");
                                }
                                @Override
                                public void onSuccessCallback(SaveMapResponse arg0) {
                                    safeDismissWaitingDialog();
                                    safeShowNotiDialog("Success", "Map saving success!");
                                }
                                @Override
                                public void onFailureCallback(Exception e) {
                                   safeDismissWaitingDialog();
                                    safeShowNotiDialog("Error", e.getMessage());
                                }
                            });

							nodeMainExecutor.execute(mapManager,
									nodeConfiguration.setNodeName("android/save_map"));

						} catch (Exception e) {
							e.printStackTrace();
                            safeShowNotiDialog("Error", "Error during saving: " + e.toString());
						}

						removeDialog(NAME_MAP_DIALOG_ID);
						return true;
					} else {
						return false;
					}
				}
			});
			button = (Button) dialog.findViewById(R.id.cancel_button);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					removeDialog(NAME_MAP_DIALOG_ID);
				}
			});
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void safeDismissWaitingDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (waitingDialog != null) {
					waitingDialog.dismiss();
					waitingDialog = null;
				}
			}
		});
	}

	private void safeShowWaitingDialog(final CharSequence message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (waitingDialog != null) {
					waitingDialog.dismiss();
					waitingDialog = null;
				}
				waitingDialog = ProgressDialog.show(MainActivity.this, "",
						message, true);
			}
		});
	}

	private void safeShowNotiDialog(final String title, final CharSequence message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (notiDialog != null) {
                    notiDialog.dismiss();
                    notiDialog = null;
				}
				if (waitingDialog != null) {
					waitingDialog.dismiss();
					waitingDialog = null;
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						MainActivity.this);
				dialog.setTitle(title);
				dialog.setMessage(message);
				dialog.setNeutralButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dlog, int i) {
								dlog.dismiss();
							}
						});
                notiDialog = dialog.show();
			}
		});
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
        joyTopic = appNameSpace.resolve(joyTopic).toString();
        camTopic = appNameSpace.resolve(camTopic).toString();
        cameraView.setTopicName(camTopic);
        virtualJoystickView.setTopicName(joyTopic);

		nodeMainExecutor.execute(cameraView,
				nodeConfiguration.setNodeName("android/camera_view"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("android/virtual_joystick"));

        ViewControlLayer viewControlLayer = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView,
                mapView, mainLayout, sideLayout, params);

        String mapTopic   = remaps.get(getString(R.string.map_topic));
        String scanTopic  = remaps.get(getString(R.string.scan_topic));
        String robotFrame = (String) params.get("robot_frame", getString(R.string.robot_frame));

        occupancyGridLayer = new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString());
        laserScanLayer = new LaserScanLayer(appNameSpace.resolve(scanTopic).toString());
        robotLayer = new RobotLayer(robotFrame);

        mapView.addLayer(viewControlLayer);
        mapView.addLayer(occupancyGridLayer);
        mapView.addLayer(laserScanLayer);
        mapView.addLayer(robotLayer);

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
			Log.w("MakeAMap", "Unable to use NTP provider, using Wall Time. Error: " + t.getMessage(), t);
			timeProvider = new WallTimeProvider();
		}
		nodeConfiguration.setTimeProvider(timeProvider);

		nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));
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
