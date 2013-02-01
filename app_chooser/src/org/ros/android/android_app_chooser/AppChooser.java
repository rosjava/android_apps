/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * * Neither the name of Willow Garage, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
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

package org.ros.android.android_app_chooser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import org.ros.address.InetAddressFactory;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;
import org.ros.android.robotapp.AppManager;
import org.ros.android.robotapp.ControlChecker;
import org.ros.android.robotapp.MasterChecker;
import org.ros.android.robotapp.RobotId;
import org.ros.android.robotapp.RosAppActivity;
import org.ros.android.robotapp.RobotDescription;
import org.ros.android.robotapp.WifiChecker;

import app_manager.App;
import app_manager.ListAppsResponse;
import app_manager.StopAppResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class AppChooser extends RosAppActivity {

	private static final int ROBOT_MASTER_CHOOSER_REQUEST_CODE = 0;

	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private TextView robotNameView;
	private ArrayList<App> availableAppsCache;
	private ArrayList<App> runningAppsCache;
	private AppManager appManager;
	private Button deactivate;
	private Button stopApps;
	private Button exchangeButton;
	private ProgressDialogWrapper progressDialog;
	private AlertDialogWrapper wifiDialog;
	private AlertDialogWrapper evictDialog;
	private AlertDialogWrapper errorDialog;
	private RobotDescription currentRobot;
	private boolean validatedRobot;

	/**
	 * Wraps the alert dialog so it can be used as a yes/no function
	 */
	private class AlertDialogWrapper {
		private int state;
		private AlertDialog dialog;
		private RosAppActivity context;

		public AlertDialogWrapper(RosAppActivity context,
				AlertDialog.Builder builder, String yesButton, String noButton) {
			state = 0;
			this.context = context;
			dialog = builder
					.setPositiveButton(yesButton,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									state = 1;
								}
							})
					.setNegativeButton(noButton,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									state = 2;
								}
							}).create();
		}

		public AlertDialogWrapper(RosAppActivity context,
				AlertDialog.Builder builder, String okButton) {
			state = 0;
			this.context = context;
			dialog = builder.setNeutralButton(okButton,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							state = 1;
						}
					}).create();
		}

		public void setTitle(String m) {
			dialog.setTitle(m);
		}

		public void setMessage(String m) {
			dialog.setMessage(m);
		}

		public boolean show(String m) {
			setMessage(m);
			return show();
		}

		public boolean show() {
			state = 0;
			context.runOnUiThread(new Runnable() {
				public void run() {
					dialog.show();
				}
			});
			// Kind of a hack. Do we know a better way?
			while (state == 0) {
				try {
					Thread.sleep(1L);
				} catch (Exception e) {
					break;
				}
			}
			dismiss();
			return state == 1;
		}

		public void dismiss() {
			if (dialog != null) {
				dialog.dismiss();
			}
			dialog = null;
		}
	}

	/**
	 * Wraps the progress dialog so it can be used to show/vanish easily
	 */
	private class ProgressDialogWrapper {
		private ProgressDialog progressDialog;
		private RosAppActivity activity;

		public ProgressDialogWrapper(RosAppActivity activity) {
			this.activity = activity;
			progressDialog = null;
		}

		public void dismiss() {
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			progressDialog = null;
		}

		public void show(String title, String text) {
			if (progressDialog != null) {
				this.dismiss();
			}
			progressDialog = ProgressDialog.show(activity, title, text, true,
					true);
			progressDialog.setCancelable(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
	}

	public AppChooser() {
		super("app chooser", "app chooser");
		availableAppsCache = new ArrayList<App>();
		runningAppsCache = new ArrayList<App>();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setDefaultAppName(null);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		robotNameView = (TextView) findViewById(R.id.robot_name_view);
		deactivate = (Button) findViewById(R.id.deactivate_robot);
		deactivate.setVisibility(deactivate.GONE);
		stopApps = (Button) findViewById(R.id.stop_applications);
		stopApps.setVisibility(stopApps.GONE);
		exchangeButton = (Button) findViewById(R.id.exchange_button);
		exchangeButton.setVisibility(deactivate.GONE);
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		super.init(nodeMainExecutor);
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());
		listApps();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED) {
			onDestroy();
		} else if (resultCode == RESULT_OK) {
			if (requestCode == ROBOT_MASTER_CHOOSER_REQUEST_CODE) {
				if (data == null) {
					nodeMainExecutorService.startMaster();
				} else {
					URI uri;
					try {

						currentRobot = (RobotDescription) data
								.getSerializableExtra(RobotMasterChooser.ROBOT_DESCRIPTION_EXTRA);

						validatedRobot = false;
						validateRobot(currentRobot.getRobotId());

						uri = new URI(currentRobot.getRobotId().getMasterUri());
					} catch (URISyntaxException e) {
						throw new RosRuntimeException(e);
					}
					nodeMainExecutorService.setMasterUri(uri);
				}

				// Run init() in a new thread as a convenience since it often
				// requires
				// network access.
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						while (!validatedRobot) {
						}

						AppChooser.this.init(nodeMainExecutorService);
						return null;
					}
				}.execute();
			} else {
				// Without a master URI configured, we are in an unusable state.
				nodeMainExecutorService.shutdown();
				finish();
			}
		}
	}

	@Override
	public void startMasterChooser() {
		if (!fromApplication) {
			super.startActivityForResult(new Intent(this,
					RobotMasterChooser.class), 0);
		} else
			super.startMasterChooser();
	}

	public void validateRobot(final RobotId id) {
		wifiDialog = new AlertDialogWrapper(this, new AlertDialog.Builder(this)
				.setTitle("Change Wifi?").setCancelable(false), "Yes", "No");
		evictDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Evict User?")
						.setCancelable(false), "Yes", "No");
		errorDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Could Not Connect")
						.setCancelable(false), "Ok");
		progressDialog = new ProgressDialogWrapper(this);
		final AlertDialogWrapper wifiDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Change Wifi?")
						.setCancelable(false), "Yes", "No");

		// Run a set of checkers in series.
		// The last step - ensure the master is up.
		final MasterChecker mc = new MasterChecker(
				new MasterChecker.RobotDescriptionReceiver() {
					public void receive(RobotDescription robotDescription) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						validatedRobot = true;
					}
				}, new MasterChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot contact ROS master: "
								+ reason2);
						errorDialog.dismiss();
						finish();

					}
				});

		// Ensure the robot is in a good state
		final ControlChecker cc = new ControlChecker(
				new ControlChecker.SuccessHandler() {
					public void handleSuccess() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...",
											"Connecting to ROS master");
								}
							}
						});
						mc.beginChecking(id);
					}
				}, new ControlChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot connect to control robot: "
								+ reason2);
						errorDialog.dismiss();
						finish();

					}
				}, new ControlChecker.EvictionHandler() {
					public boolean doEviction(String current, String message) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						String m = "";
						if (message != null) {
							m = " The user says: \"" + message + "\"";
						}
						evictDialog
								.setMessage(current
										+ " is running custom software on this robot. Do you want to evict this user?"
										+ m);
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.show("Connecting...",
											"Deactivating robot");
								}
							}
						});
						return evictDialog.show();
					}

					@Override
					public boolean doEviction(String user) {
						// TODO Auto-generated method stub
						return false;
					}
				}, new ControlChecker.StartHandler() {
					public void handleStarting() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...", "Starting robot");
								}
							}
						});
					}
				});

		// Ensure that the correct WiFi network is selected.
		final WifiChecker wc = new WifiChecker(
				new WifiChecker.SuccessHandler() {
					public void handleSuccess() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...",
											"Checking robot state");
								}
							}
						});
						cc.beginChecking(id);
					}
				}, new WifiChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot connect to robot WiFi: "
								+ reason2);
						errorDialog.dismiss();
						finish();
					}
				}, new WifiChecker.ReconnectionHandler() {
					public boolean doReconnection(String from, String to) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						if (from == null) {
							wifiDialog
									.setMessage("To use this robot, you must connect to a wifi network. You are currently not connected to a wifi network. Would you like to connect to the correct wireless network?");
						} else {
							wifiDialog
									.setMessage("To use this robot, you must switch wifi networks. Do you want to switch from "
											+ from + " to " + to + "?");
						}
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.show("Connecting...",
											"Switching wifi networks");
								}
							}
						});
						return wifiDialog.show();
					}
				});
		progressDialog.show("Connecting...", "Checking wifi connection");
		wc.beginChecking(id, (WifiManager) getSystemService(WIFI_SERVICE));
	}

	// What this function for?
	public void onAppClicked(final App app, final boolean isClientApp) {

		boolean running = false;
		for (App i : runningAppsCache) {
			if (i.getName().equals(app.getName())) {
				running = true;
			}
		}
	}

	private void listApps() {
		Log.i("RosAndroid", "listing application");
		appManager = new AppManager("");
		appManager.setFunction("list");
		appManager
				.setListService(new ServiceResponseListener<ListAppsResponse>() {
					@Override
					public void onSuccess(ListAppsResponse message) {
						Log.i("RosAndroid", "App got lists successfully");
						availableAppsCache = (ArrayList<App>) message
								.getAvailableApps();
						runningAppsCache = (ArrayList<App>) message
								.getRunningApps();
						ArrayList<String> runningAppsNames = new ArrayList<String>();
						int i = 0;
						for (i = 0; i < availableAppsCache.size(); i++) {
							App item = availableAppsCache.get(i);
							ArrayList<String> clients = new ArrayList<String>();
							for (int j = 0; j < item.getClientApps().size(); j++) {
								clients.add(item.getClientApps().get(j)
										.getClientType());
							}
							if (!clients.contains("android")
									&& item.getClientApps().size() != 0) {
								availableAppsCache.remove(i);
								i--;
							}

							if (item.getClientApps().size() == 0) {
								Log.i("AppChooser",
										"Item name: " + item.getName());
								runningAppsNames.add(item.getName());
							}
						}
						Log.i("RosAndroid", "ListApps.Response: "
								+ availableAppsCache.size() + " apps");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateAppList(availableAppsCache,
										runningAppsCache);
							}
						});

					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to get lists!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("list_app"));
	}

	protected void updateAppList(final ArrayList<App> apps,
			final ArrayList<App> runningApps) {
		Log.i("RosAndroid", "updating gridview");
		GridView gridview = (GridView) findViewById(R.id.gridview);
		AppAdapter appAdapter = new AppAdapter(AppChooser.this, apps,
				runningApps);
		gridview.setAdapter(appAdapter);
		registerForContextMenu(gridview);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				if (runningAppsCache.size() > 0) {
					Log.i("AppChooser", "RunningAppsCache greater than zero.");
				}

				boolean running = false;
				App app = availableAppsCache.get(position);
				for (App i : runningAppsCache) {
					if (i.getName().equals(app.getName())) {
						running = true;
					}
				}

				if (AppLauncher.launch(AppChooser.this, apps.get(position),
						getMasterUri()) == true) {
					onDestroy();
				}
			}
		});
		if (runningApps != null) {
			if (runningApps.toArray().length != 0) {
				stopApps.setVisibility(stopApps.VISIBLE);
			} else {
				stopApps.setVisibility(stopApps.GONE);
			}
		}
		Log.i("RosAndroid", "gridview updated");
	}

	public void chooseNewMasterClicked(View view) {

		nodeMainExecutor.shutdownNodeMain(appManager);
		releaseDashboardNode(); // TODO this work costs too many times
		availableAppsCache.clear();
		runningAppsCache.clear();
		startActivityForResult(new Intent(this, RobotMasterChooser.class), 0);
	}

	public void exchangeButtonClicked(View view) {
	}

	public void deactivateRobotClicked(View view) {
	}

	public void stopApplicationsClicked(View view) {
		final AppChooser activity = this;

		for (App i : runningAppsCache) {
			Log.i("AppLauncher", "Sending intent.");
			AppLauncher.launch(this, i, getMasterUri());
		}

		progressDialog = new ProgressDialogWrapper(this);
		progressDialog.show("Stopping Applications",
				"Stopping all applications...");

		AppManager appManager = new AppManager("*");
		appManager.setFunction("stop");
		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
						Log.i("RosAndroid", "App stopped successfully");
						availableAppsCache = new ArrayList<App>();
						runningAppsCache = new ArrayList<App>();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateAppList(availableAppsCache,
										runningAppsCache);
							}
						});
						listApps();
						progressDialog.dismiss();

					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to stop!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("stop_app"));
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