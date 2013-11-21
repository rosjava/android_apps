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

package com.github.rosjava.android_apps.application_management;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

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

import com.github.rosjava.android_apps.application_management.rapp_manager.PairingApplicationNamePublisher;

import rocon_app_manager_msgs.StartAppResponse;
import rocon_app_manager_msgs.StopAppResponse;

import org.yaml.snakeyaml.Yaml;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 * 
 * This is a clone of RosAppActivity adapted for concert remocon apps; TODO: unify
 */
public abstract class ConcertAppActivity extends RosActivity {

    public enum AppMode {
        STANDALONE, // unmanaged app
        PAIRED,     // paired with master, normally a robot
        CONCERT;    // running inside a concert

        public String toString() { return name().toLowerCase(); }
    }
    /*
      By default we assume the ros app activity is launched independently. The following attribute is
      used to identify when it has instead been launched by a controlling application (e.g. remocons)
      in paired, one-to-one, or concert mode.
     */
    private AppMode appMode = AppMode.STANDALONE;
	private String masterAppName = null;
	private String defaultMasterAppName = null;
	private String defaultMasterName = "";
    private String androidApplicationName; // descriptive helper only
//    private boolean managedApplication = false;
    private String managedApplicationActivity = null; // e.g. com.github.rosjava.android_remocons.robot_remocon.RobotRemocon
    private PairingApplicationNamePublisher managedPairingApplicationNamePublisher;

	private int dashboardResourceId = 0;
	private int mainWindowId = 0;
	private Dashboard dashboard = null;
	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private URI uri;
	protected MasterNameResolver masterNameResolver;
    protected MasterDescription masterDescription;

    // By now params and remaps are only available for concert apps; that is, appMode must be CONCERT
    protected LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
    protected LinkedHashMap<String, String> remaps = new LinkedHashMap<String, String>();

	protected void setDashboardResource(int resource) {
		dashboardResourceId = resource;
	}

	protected void setMainWindowResource(int resource) {
		mainWindowId = resource;
	}

	protected void setDefaultMasterName(String name) {
		defaultMasterName = name;
	}

	protected void setDefaultAppName(String name) {
        defaultMasterAppName = name;
	}

	protected void setCustomDashboardPath(String path) {
		dashboard.setCustomDashboardPath(path);
	}

	protected ConcertAppActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
        this.androidApplicationName = notificationTitle;
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

		masterNameResolver = new MasterNameResolver();

		if (defaultMasterName != null) {
			masterNameResolver.setMasterName(defaultMasterName);
		}

//        getIntent().putExtra(AppManager.PACKAGE + ".concert_app_name", "KKKK");
//        getIntent().putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.concert_remocon.ConcertRemocon");
//        getIntent().putExtra("ChooserURI", "http://192.168.10.124:11311");
//        getIntent().putExtra("Parameters", "{pickup_point: pickup}");
//        getIntent().putExtra("Remappings", "{map: map_store/map,list_maps: map_store/list_maps,publish_map: map_store/publish_map,save_annotations: annotations/save_annotations,markers: annotations/markers,tables: annotations/tables,columns: annotations/columns,walls: annotations/walls}");

        for (AppMode mode : AppMode.values()) {
            // The remocon specifies its type in the app name extra content string, useful information for the app
            masterAppName = getIntent().getStringExtra(AppManager.PACKAGE + "." + mode + "_app_name");
            if (masterAppName != null) {
                appMode = mode;
                break;
            }
        }

        if (masterAppName == null) {
            // App name extra content key not present on intent; no remocon started the app, so we are standalone app
            masterAppName = defaultMasterAppName;
            appMode = AppMode.STANDALONE;
		}
        else {
            // Extract parameters and remappings from a YAML-formatted strings; translate into hash maps
            Yaml yaml = new Yaml();

            String paramsStr = getIntent().getStringExtra("Parameters");
            if (paramsStr != null) {
                params = (LinkedHashMap<String, Object>) yaml.load(paramsStr);
                Log.d("ApplicationManagement", "Parameters: " + paramsStr);
            }

            String remapsStr = getIntent().getStringExtra("Remappings");
            if (remapsStr != null) {
                remaps = (LinkedHashMap<String, String>) yaml.load(remapsStr);
                Log.d("ApplicationManagement", "Remappings: " + remapsStr);
            }

            // Only on paired mode
            if (appMode == AppMode.PAIRED) {
                managedApplicationActivity = getIntent().getStringExtra("PairedManagerActivity");
                managedPairingApplicationNamePublisher = new PairingApplicationNamePublisher(this.androidApplicationName);
            }
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

        if (appMode == AppMode.STANDALONE) {
            // Unmanaged apps need a master namespace resolver  TODO paired do too????
            nodeMainExecutor.execute(masterNameResolver,
                    nodeConfiguration.setNodeName("masterNameResolver"));
            masterNameResolver.waitForResolver();

            dashboard.setRobotName(getMasterNameSpace().getNamespace().toString());
        }
        else if (getIntent().hasExtra(MasterDescription.UNIQUE_KEY)) {
            if (appMode == AppMode.CONCERT) {
                try {
                    masterDescription =
                        (ConcertDescription) getIntent().getSerializableExtra(MasterDescription.UNIQUE_KEY);
                } catch (ClassCastException e) {
                    Log.e("ApplicationManagement", "Concert description expected on intent on " + appMode + " mode");
                    throw new RosRuntimeException("Concert description expected on intent on " + appMode + " mode");
                }
            }
            else if (appMode == AppMode.PAIRED) {
                try {
                    masterDescription =
                        (RobotDescription) getIntent().getSerializableExtra(MasterDescription.UNIQUE_KEY);
                } catch (ClassCastException e) {
                    Log.e("ApplicationManagement", "Robot description expected on intent on " + appMode + " mode");
                    throw new RosRuntimeException("Robot description expected on intent on " + appMode + " mode");
                }

                nodeMainExecutor.execute(managedPairingApplicationNamePublisher,
                        nodeConfiguration.setNodeName("pairingApplicationNamePublisher"));

                dashboard.setRobotName(masterDescription.getMasterType());
            }
            masterNameResolver.setMaster(masterDescription);
        }
        else {
            Log.e("ApplicationManagement", "Master description missing on intent on " + appMode + " mode");
            throw new RosRuntimeException("Master description missing on intent on " + appMode + " mode");
        }

        nodeMainExecutor.execute(dashboard, nodeConfiguration.setNodeName("dashboard"));

        // probably need to reintegrate this restart mechanism
//        if (managedApplication && startMasterApplication) {
//			if (getIntent().getBooleanExtra("runningNodes", false)) {
//				restartApp();
        if ( managePairedRobotApplication() ) {
            Log.w("ApplicationManagement", "starting a rapp");
            startApp();
        }
    }

	protected NameResolver getAppNameSpace() {
		return masterNameResolver.getAppNameSpace();
	}

	protected NameResolver getMasterNameSpace() {
		return masterNameResolver.getMasterNameSpace();
	}

	protected void onAppTerminate() {
		com.github.rosjava.android_apps.application_management.ConcertAppActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(com.github.rosjava.android_apps.application_management.ConcertAppActivity.this)
						.setTitle("App Termination")
						.setMessage(
								"The application has terminated on the server, so the client is exiting.")
						.setCancelable(false)
						.setNeutralButton("Exit",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
                                        com.github.rosjava.android_apps.application_management.ConcertAppActivity.this.finish();
									}
								}).create().show();
			}
		});
	}

	@Override
	public void startMasterChooser() {
		if (appMode == AppMode.STANDALONE) {
			super.startMasterChooser();
		} else {
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".app_name", "AppChooser");
			try {
				uri = new URI(getIntent().getStringExtra("ChooserURI"));
			} catch (URISyntaxException e) {
				throw new RosRuntimeException(e);
			}

			nodeMainExecutorService.setMasterUri(uri);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					ConcertAppActivity.this.init(nodeMainExecutorService);
					return null;
				}
			}.execute();
		}

	}

	private void restartApp() {
		Log.i("ApplicationManagement", "Restarting application");
		AppManager appManager = new AppManager("*", getMasterNameSpace());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
						Log.i("ApplicationManagement", "App stopped successfully");
						try {
							Thread.sleep(1000);
						} catch (Exception e) {

						}
						startApp();
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("ApplicationManagement", "App failed to stop!");
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
		Log.i("ApplicationManagement", "android application starting a rapp [" + masterAppName + "]");

		AppManager appManager = new AppManager(masterAppName,
				getMasterNameSpace());
		appManager.setFunction("start");

		appManager
				.setStartService(new ServiceResponseListener<StartAppResponse>() {
					@Override
					public void onSuccess(StartAppResponse message) {
						if (message.getStarted()) {
							Log.i("ApplicationManagement", "rapp started successfully [" + masterAppName + "]");
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
		Log.i("ApplicationManagement", "android application stopping a rapp [" + masterAppName + "]");
		AppManager appManager = new AppManager(masterAppName,
				getMasterNameSpace());
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

	protected void releaseMasterNameResolver() {
		nodeMainExecutor.shutdownNodeMain(masterNameResolver);
	}

	protected void releaseDashboardNode() {
		nodeMainExecutor.shutdownNodeMain(dashboard);
	}

    /**
     * Whether this ros app activity should be responsible for
     * starting and stopping a paired master application.
     *
     * This responsibility is relinquished if the application
     * is controlled from a remocon, but required if the
     * android application is connecting and running directly.
     *
     * @return boolean : true if it needs to be managed.
     */
    private boolean managePairedRobotApplication() {
        return ((appMode == AppMode.STANDALONE) && (masterAppName != null));
    }

	@Override
	protected void onDestroy() {
        if ( ( getMasterNameSpace() != null ) && managePairedRobotApplication() ) {
			stopApp();
		}
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (appMode != AppMode.STANDALONE) {  // i.e. it's a managed app
            Log.i("ApplicationManagement", "app terminating and returning control to the remocon.");
            // Restart the remocon, supply it with the necessary information and stop this activity
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".app_name", "AppChooser");
            intent.putExtra(MasterDescription.UNIQUE_KEY, masterDescription);
			intent.putExtra("ChooserURI", uri.toString());
//            intent.putExtra("RobotType",concertDescription.getRobotType());
//            intent.putExtra("RobotName",concertDescription.getRobotName());
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
