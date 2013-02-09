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

package org.ros.android.map_manager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.ros.android.robotapp.RosAppActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;
import org.ros.address.InetAddressFactory;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.exception.RemoteException;

import map_store.DeleteMapResponse;
import map_store.ListMapsResponse;
import map_store.MapListEntry;
import map_store.PublishMapResponse;
import map_store.RenameMapResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
@SuppressLint("NewApi")
public class MainActivity extends RosAppActivity {

	private static final String ROBOT_FRAME = "base_link";
	private static final int NAME_MAP_DIALOG_ID = 0;

	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;

	private VisualizationView mapView;
	private Button backButton;
	private Button renameButton;
	private ListView mapListView;
	private ArrayList<MapListEntry> mapList = new ArrayList<MapListEntry>();
	public OnTouchListener gestureListener;
	private int radioFocus = 0;
	private int viewPosition = -1;
	private boolean startMapManager = true;
	private boolean showDeleteDialog = false;
	private boolean visibleMapView = true;
	private ProgressDialog waitingDialog;
	private AlertDialog errorDialog;
	private OccupancyGridLayer occupancyGridLayer;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("map manager", "map manager");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		setDefaultAppName("new_turtlebot_android_apps/android_map_manager");
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		WindowManager windowManager = getWindowManager();
		final Display display = windowManager.getDefaultDisplay();

		mapListView = (ListView) findViewById(R.id.map_list);
		mapView = (VisualizationView) findViewById(R.id.map_view);
		backButton = (Button) findViewById(R.id.back_button);
		renameButton = (Button) findViewById(R.id.rename_button);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});

		renameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (radioFocus != -1) {
					showDialog(NAME_MAP_DIALOG_ID);
				}
			}
		});

		gestureListener = new View.OnTouchListener() {
			private int padding = 0;
			private int initialx = 0;
			private int currentx = 0;

			public boolean onTouch(View v, MotionEvent event) {
				if (!showDeleteDialog) {

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						padding = 0;
						initialx = (int) event.getX();
						currentx = (int) event.getX();
					}
					if (event.getAction() == MotionEvent.ACTION_MOVE) {
						currentx = (int) event.getX();
						padding = currentx - initialx;
					}

					if (event.getAction() == MotionEvent.ACTION_UP
							|| event.getAction() == MotionEvent.ACTION_CANCEL) {
						padding = 0;
						initialx = 0;
						currentx = 0;
					}

					if (Math.abs(padding) > display.getWidth() * 0.2f) {
						deleteMap(v.getId());
						v.setAlpha(0);
						showDeleteDialog = true;
						return true;
					}
					v.setPadding(padding, 0, -padding, 0);
					float alpha = 1 - Math.abs(padding)
							/ (display.getWidth() * 0.2f);
					if (alpha > 0) {
						v.setAlpha(alpha);

					} else {
						v.setAlpha(0);
					}
				}
				return true;
			}
		};

		mapView.getCamera().jumpToFrame(ROBOT_FRAME);

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		super.init(nodeMainExecutor);

		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());

		CameraControlLayer cameraControlLayer = new CameraControlLayer(this,
				nodeMainExecutor.getScheduledExecutorService());
		cameraControlLayer.addListener(new CameraControlListener() {
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
		mapView.addLayer(cameraControlLayer);
		occupancyGridLayer = new OccupancyGridLayer("map");
		mapView.addLayer(occupancyGridLayer);
		nodeMainExecutor.execute(mapView, nodeConfiguration);

		updateMapList();
	}

	protected void updateMapView(int position) {
		radioFocus = position;
		updateMapView(MainActivity.this.mapList.get(position));
	}

	private void updateMapView(MapListEntry map) {

		MapManager mapManager = new MapManager();
		mapManager.setFunction("publish");
		mapManager.setMapId(map.getMapId());
		safeShowWaitingDialog("Loading...");


		mapManager
				.setPublishService(new ServiceResponseListener<PublishMapResponse>() {

					@Override
					public void onFailure(RemoteException e) {
						e.printStackTrace();
						safeDismissWaitingDialog();
						safeShowErrorDialog("Error loading map: "
								+ e.toString());

					}

					@Override
					public void onSuccess(PublishMapResponse message) {
						safeDismissWaitingDialog();
						if (!visibleMapView) {
							mapView.addLayer(occupancyGridLayer);
							visibleMapView = true;
						}
					}
				});

		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("publish_map"));

	}

	private void updateMapListGui(final List<MapListEntry> list) {

		final ArrayList<MapListData> availableMapNames = new ArrayList<MapListData>();
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
			MapListData mapListData = new MapListData();
			mapListData.setText(displayString);
			if (i == radioFocus) {
				mapListData.setChecked(true);
			} else
				mapListData.setChecked(false);
			mapListData.setId(i);
			availableMapNames.add(mapListData);
		}

		mapList = (ArrayList<MapListEntry>) list;

		if (startMapManager) {
			startMapManager = false;
			updateMapView(0);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final MapListArrayAdapter ad = new MapListArrayAdapter(
						MainActivity.this, 0, availableMapNames,
						gestureListener);
				mapListView.setAdapter(ad);
				if (viewPosition != -1) {
					mapListView.setSelection(viewPosition);
				}

			}
		});
	}

	private void updateMapList() {

		MapManager mapManager = new MapManager();
		mapManager.setFunction("list");
		safeShowWaitingDialog("Waiting for maps...");

		mapManager
				.setListService(new ServiceResponseListener<ListMapsResponse>() {

					@Override
					public void onSuccess(ListMapsResponse message) {
						Log.i("MapManager", "readAvailableMapList() Success");
						safeDismissWaitingDialog();
						updateMapListGui(message.getMapList());
					}

					@Override
					public void onFailure(RemoteException arg0) {
						Log.i("MapManager", "readAvailableMapList() Failure");
						safeDismissWaitingDialog();
						
					}

				});

		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("list_maps"));

	}

	public void deleteMap(int position) {
		final String id = mapList.get(position).getMapId();
		viewPosition = mapListView.getFirstVisiblePosition();

		if (id == null) {
			return;
		}
		final int radioFocusRelation;
		if (position == radioFocus) {
			radioFocusRelation = 0;
		} else if (position < radioFocus) {
			radioFocusRelation = 1;
		} else
			radioFocusRelation = -1;

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Are You Sure?");
		dialog.setMessage("Are you sure you want to delete this map?");
		dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dlog, int i) {
				dlog.dismiss();
				safeShowWaitingDialog("Deleting...");
				try {
					MapManager mapManager = new MapManager();
					mapManager.setFunction("delete");
					mapManager.setMapId(id);

					mapManager
							.setDeleteService(new ServiceResponseListener<DeleteMapResponse>() {

								@Override
								public void onFailure(RemoteException e) {
									showDeleteDialog = false;
									e.printStackTrace();

								}

								@Override
								public void onSuccess(DeleteMapResponse arg0) {

									if (viewPosition != 0) {
										viewPosition += 1;
									}
									switch (radioFocusRelation) {
									case 0:
										mapView.hideLayer(occupancyGridLayer);
										visibleMapView = false;
										radioFocus = -1;
										break;
									case 1:
										radioFocus -= 1;
										break;
									}
									showDeleteDialog = false;

									MainActivity.this
											.runOnUiThread(new Runnable() {
												public void run() {
													safeDismissWaitingDialog();
													updateMapList();
												}
											});
								}
							});

					nodeMainExecutor.execute(mapManager,
							nodeConfiguration.setNodeName("delete_map"));

				} catch (Exception e) {
					e.printStackTrace();
					safeShowErrorDialog("Error during map delete: "
							+ e.toString());
				}
			}
		});

		dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dlog, int i) {
				if (viewPosition != 0) {
					viewPosition += 1;
				}
				showDeleteDialog = false;
				updateMapList();
				dlog.dismiss();
			}
		});
		dialog.show();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		Button button;
		switch (id) {
		case NAME_MAP_DIALOG_ID:
			viewPosition = mapListView.getFirstVisiblePosition();
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.name_map_dialog);
			dialog.setTitle("Rename Map");

			final String targetMapId = mapList.get(radioFocus).getMapId();
			final EditText nameField = (EditText) dialog
					.findViewById(R.id.name_editor);
			nameField.setText(mapList.get(radioFocus).getName());
			nameField.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(final View view, int keyCode,
						KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_DOWN
							&& keyCode == KeyEvent.KEYCODE_ENTER) {
						String newName = nameField.getText().toString();
						if (newName != null && newName.length() > 0) {
							safeShowWaitingDialog("Waiting for rename...");
							try {
								MapManager mapManager = new MapManager();
								mapManager.setFunction("rename");
								mapManager.setMapId(targetMapId);
								mapManager.setMapName(newName);

								mapManager
										.setRenameService(new ServiceResponseListener<RenameMapResponse>() {

											@Override
											public void onFailure(
													RemoteException e) {
												e.printStackTrace();
												safeShowErrorDialog("Error during rename: "
														+ e.toString());
											}

											@Override
											public void onSuccess(
													RenameMapResponse arg0) {
												MainActivity.this
														.runOnUiThread(new Runnable() {
															public void run() {
																safeDismissWaitingDialog();
																updateMapList();
															}
														});
											}

										});
								nodeMainExecutor.execute(mapManager,
										nodeConfiguration
												.setNodeName("rename_map"));

							} catch (Exception e) {
								e.printStackTrace();
								safeShowErrorDialog("Error during rename: "
										+ e.toString());
							}
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

	private void safeShowErrorDialog(final CharSequence message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (errorDialog != null) {
					errorDialog.dismiss();
					errorDialog = null;
				}
				if (waitingDialog != null) {
					waitingDialog.dismiss();
					waitingDialog = null;
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						MainActivity.this);
				dialog.setTitle("Error");
				dialog.setMessage(message);
				dialog.setNeutralButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dlog, int i) {
								dlog.dismiss();
							}
						});
				errorDialog = dialog.show();
			}
		});
	}

	private void safeDismissErrorDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (errorDialog != null) {
					errorDialog.dismiss();
					errorDialog = null;
				}
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
