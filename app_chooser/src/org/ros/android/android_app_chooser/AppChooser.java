package org.ros.android.android_app_chooser;

import java.util.ArrayList;

import org.ros.address.InetAddressFactory;
import org.ros.android.AppManager;
import org.ros.android.RosAppActivity;
import org.ros.exception.RemoteException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import app_manager.App;
import app_manager.ListAppsResponse;
import app_manager.StartAppResponse;

public class AppChooser extends RosAppActivity
{
	
    private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private TextView robotNameView;
	private ArrayList<App> availableAppsCache;
	private ArrayList<App> runningAppsCache;
	private AppManager appManager;

	
	
	public AppChooser() {
		super("app chooser", "app chooser");
		availableAppsCache = new ArrayList<App>();
		runningAppsCache = new ArrayList<App>();
	}

    
	/** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	setDefaultAppName(null);
    	setDashboardResource(R.id.top_bar);
    	setMainWindowResource(R.layout.main);
        super.onCreate(savedInstanceState);
        
        robotNameView = (TextView)findViewById(R.id.robot_name_view);
    }

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		
		super.init(nodeMainExecutor);

		
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());
		listApps();
	}
	
	public void onAppClicked(final App app, final boolean isClientApp) {
	    /*if( appManager == null ) {
	      //safeSetStatus("Failed: appManager is not ready.");
	      return;
	    }*/
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
    	appManager.setListService(new ServiceResponseListener<ListAppsResponse>() {
            @Override
            public void onSuccess(ListAppsResponse message) {
                    Log.i("RosAndroid", "App got lists successfully");
                    availableAppsCache = (ArrayList<App>)message.getAvailableApps();
                    runningAppsCache = (ArrayList<App>)message.getRunningApps();
                    ArrayList<String> runningAppsNames = new ArrayList<String>();
                    int i = 0;
                    for (i = 0; i<availableAppsCache.size(); i++) {
                      App item = availableAppsCache.get(i);
                      ArrayList<String> clients = new ArrayList<String>();
                      for (int j = 0; j< item.getClientApps().size(); j++) {
                        clients.add(item.getClientApps().get(j).getClientType());
                      }
                      if (!clients.contains("android") && item.getClientApps().size() != 0) {
                        availableAppsCache.remove(i);
                        i--;
                      }
                      
                      if (item.getClientApps().size() == 0) {
                        Log.i("AppChooser", "Item name: " + item.getName() );
                        runningAppsNames.add(item.getName());
                      }
                    }
                    Log.i("RosAndroid", "ListApps.Response: " + availableAppsCache.size() + " apps");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                          updateAppList(availableAppsCache, runningAppsCache);
                        }});
                    
            }
            @Override
            public void onFailure(RemoteException e) {
                    Log.e("RosAndroid", "App failed to get lists!");
            }
        });
        
       nodeMainExecutor.execute(appManager, nodeConfiguration.setNodeName("list_app"));
    	
       
	}
	

	  

	  protected void updateAppList(final ArrayList<App> apps, final ArrayList<App> runningApps) {
		    Log.i("RosAndroid", "updating gridview");
		    GridView gridview = (GridView) findViewById(R.id.gridview);
		    AppAdapter appAdapter = new AppAdapter(AppChooser.this, apps, runningApps);
		    gridview.setAdapter(appAdapter);
		    registerForContextMenu(gridview);
		    gridview.setOnItemClickListener(new OnItemClickListener() {
		      @Override
		      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

		        if (runningAppsCache.size() > 0) {
		          Log.i("AppChooser", "RunningAppsCache greater than zero.");
		        }
		       /* if ( mode == REG) {
		          Log.i("AppChooser", "MODE is REG" );
		        }*/

		        boolean running = false;
		        App app = availableAppsCache.get(position);
		        for (App i : runningAppsCache) {
		          if (i.getName().equals(app.getName())) {
		            running = true;
		          }
		        }


		        /*if (!running && (runningAppsCache.size() > 0 && mode == REG)) {
		          showDialog(CLOSE_EXISTING);
		          return;
		        }*/
		        
		        AppLauncher.launch(AppChooser.this, apps.get(position), getMasterUri());
				onDestroy();

		      }
		    });
		    if (runningApps != null) {
		      if (runningApps.toArray().length != 0) {
		        //stopApps.setVisibility(stopApps.VISIBLE);
		      } else {
		        //stopApps.setVisibility(stopApps.GONE);
		      }
		    }
		    Log.i("RosAndroid", "gridview updated");
		  }

	  public void chooseNewMasterClicked(View view){
	  }
	  
	  public void exchangeButtonClicked(View view){
	  }
	  
	  public void deactivateRobotClicked(View view){
	  }
	  
	  public void stopApplicationsClicked(View view){
	  }
}
