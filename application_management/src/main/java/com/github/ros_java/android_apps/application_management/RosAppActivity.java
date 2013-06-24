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

package com.github.ros_java.android_apps.application_management;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;

import rocon_app_manager_msgs.StartAppResponse;
import rocon_app_manager_msgs.StopAppResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public abstract class RosAppActivity extends RosActivity {

	private String robotAppName = null;
	private String defaultRobotAppName = null;
	private String defaultRobotName = null;
    /*
      By default we assume the rosappactivity is launched independantly.
      The following flag is used to identify when it has instead been
      launched by a controlling application (e.g. remocons).
     */
    private boolean managedApplication = false;
    private String managedApplicationActivity = null; // e.g. com.github.ros_java.android_remocons.robot_remocon.RobotRemocon

	private int dashboardResourceId = 0;
	private int mainWindowId = 0;
	private Dashboard dashboard = null;
	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private URI uri;
	protected RobotNameResolver robotNameResolver;
	protected RobotDescription robotDescription;

	protected void setDashboardResource(int resource) {
		dashboardResourceId = resource;
	}

	protected void setMainWindowResource(int resource) {
		mainWindowId = resource;
	}

	protected void setDefaultRobotName(String name) {
		defaultRobotName = name;
	}

	protected void setDefaultAppName(String name) {
        defaultRobotAppName = name;
	}

	protected void setCustomDashboardPath(String path) {
		dashboard.setCustomDashboardPath(path);
	}

	protected RosAppActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mainWindowId == 0) {
			Log.e("ApplicationManagement",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}
		if (dashboardResourceId == 0) {
			Log.e("ApplicationManagement",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(mainWindowId);

		robotNameResolver = new RobotNameResolver();

		if (defaultRobotName != null) {
			robotNameResolver.setRobotName(defaultRobotName);
		}

		robotAppName = getIntent().getStringExtra(AppManager.PACKAGE + ".robot_app_name");
		if (robotAppName == null) {
			robotAppName = defaultRobotAppName;
		} else {
            managedApplicationActivity = getIntent().getStringExtra("PairedManagerActivity");
			managedApplication = true;
		}

		if (dashboard == null) {
			dashboard = new Dashboard(this);
			dashboard.setView((LinearLayout) findViewById(dashboardResourceId),
					new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.WRAP_CONTENT,
							LinearLayout.LayoutParams.WRAP_CONTENT));
		}

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());

        if ( managedApplication ) {
            if (getIntent().hasExtra(RobotDescription.UNIQUE_KEY)) {
                robotDescription = (RobotDescription) getIntent()
                        .getSerializableExtra(RobotDescription.UNIQUE_KEY);
            }
        }
		if (robotDescription != null) {
            robotNameResolver.setRobot(robotDescription);
			dashboard.setRobotName(robotDescription.getRobotType());
		}
		nodeMainExecutor.execute(robotNameResolver,
				nodeConfiguration.setNodeName("robotNameResolver"));
        robotNameResolver.waitForResolver();

        if (robotDescription == null) {
            dashboard.setRobotName(getRobotNameSpace().getNamespace()
                .toString());
        }

        nodeMainExecutor.execute(dashboard,
				nodeConfiguration.setNodeName("dashboard"));


        // probably need to reintegrate this restart mechanism
//        if (managedApplication && startRobotApplication) {
//			if (getIntent().getBooleanExtra("runningNodes", false)) {
//				restartApp();
        if ( managePairedRobotApplication() ) {
            Log.w("ApplicationManagement", "starting a rapp");
            startApp();
        }
    }

	protected NameResolver getAppNameSpace() {
		return robotNameResolver.getAppNameSpace();
	}

	protected NameResolver getRobotNameSpace() {
		return robotNameResolver.getRobotNameSpace();
	}

	protected void onAppTerminate() {
		RosAppActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(RosAppActivity.this)
						.setTitle("App Termination")
						.setMessage(
								"The application has terminated on the server, so the client is exiting.")
						.setCancelable(false)
						.setNeutralButton("Exit",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
                                        RosAppActivity.this.finish();
									}
								}).create().show();
			}
		});
	}

	@Override
	public void startMasterChooser() {
		if (!managedApplication) {
			super.startMasterChooser();
		} else {
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".robot_app_name",
					"AppChooser");
			try {
				uri = new URI(getIntent().getStringExtra("ChooserURI"));
			} catch (URISyntaxException e) {
				throw new RosRuntimeException(e);
			}

			nodeMainExecutorService.setMasterUri(uri);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					RosAppActivity.this.init(nodeMainExecutorService);
					return null;
				}
			}.execute();
		}

	}

	private void restartApp() {
		Log.i("RosAndroid", "Restarting application");
		AppManager appManager = new AppManager("*", getRobotNameSpace());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
						Log.i("RosAndroid", "App stopped successfully");
						try {
							Thread.sleep(1000);
						} catch (Exception e) {

						}
						startApp();
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to stop!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("start_app"));

	}

    /**
     * This is currently only used by apps that start themselves. Apps launched by an external
     * program (e.g. remocons) will do their own handling of the appmanager.
     */
	private void startApp() {
		Log.i("ApplicationManagement", "android application starting a rapp [" + robotAppName + "]");

		AppManager appManager = new AppManager(robotAppName,
				getRobotNameSpace());
		appManager.setFunction("start");

		appManager
				.setStartService(new ServiceResponseListener<StartAppResponse>() {
					@Override
					public void onSuccess(StartAppResponse message) {
						if (message.getStarted()) {
							Log.i("ApplicationManagement", "rapp started successfully [" + robotAppName + "]");
						} else {
							Log.e("ApplicationManagement", "rapp failed to start! [" + message.getMessage() + "]");
                        }
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("ApplicationManagement", "rapp failed to start - no response!");
					}
				});

		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("start_app"));
	}

	protected void stopApp() {
		Log.i("ApplicationManagement", "android application stopping a rapp [" + robotAppName + "]");
		AppManager appManager = new AppManager(robotAppName,
				getRobotNameSpace());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
                        if ( message.getStopped() ) {
						    Log.i("ApplicationManagement", "App stopped successfully");
                        } else {
                            Log.i("ApplicationManagement", "Stop app request rejected [" + message.getMessage() + "]");
                        }
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("ApplicationManagement", "App failed to stop when requested!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("stop_app"));
	}

	protected void releaseRobotNameResolver() {
		nodeMainExecutor.shutdownNodeMain(robotNameResolver);
	}

	protected void releaseDashboardNode() {
		nodeMainExecutor.shutdownNodeMain(dashboard);
	}

    /**
     * Whether this ros app activity should be responsible for
     * starting and stopping a paired robot application.
     *
     * This responsibility is relinquished if the application
     * is controlled from a remocon, but required if the
     * android application is connecting and running directly.
     *
     * @return boolean : true if it needs to be managed.
     */
    private boolean managePairedRobotApplication() {
        return (!managedApplication && (robotAppName != null));
    }

	@Override
	protected void onDestroy() {
        if ( ( getRobotNameSpace() != null ) && managePairedRobotApplication() ) {
			stopApp();
		}
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (managedApplication) {
            Log.i("ApplicationManagement", "app terminating and returning control to the remocon.");
            // Restart the remocon, supply it with the necessary information and stop this activity
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".robot_app_name",
					"AppChooser");
			intent.putExtra("ChooserURI", uri.toString());
            intent.putExtra("RobotType",robotDescription.getRobotType());
            intent.putExtra("RobotName",robotDescription.getRobotName());
            intent.setAction(managedApplicationActivity);
            //intent.setAction("com.github.robotics_in_concert.rocon_android.robot_remocon.RobotRemocon");
			intent.addCategory("android.intent.category.DEFAULT");
			startActivity(intent);
			finish();
		} else {
            Log.i("ApplicationManagement", "backpress processing for RosAppActivity");
        }
		super.onBackPressed();
	}
}
