package com.github.rosjava.android_apps.make_a_map;

import android.content.Context;
import android.os.AsyncTask;

import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import world_canvas_msgs.SaveMap;
import world_canvas_msgs.SaveMapRequest;
import world_canvas_msgs.SaveMapResponse;

public class MapManager extends AbstractNodeMain {

	private ConnectedNode connectedNode;
	private ServiceResponseListener<SaveMapResponse> saveServiceResponseListener;

	private String mapName;
    private String saveSrvName;
    private NameResolver nameResolver;
    private boolean nameResolverSet = false;
    private boolean waitingFlag = false;

    private StatusCallback statusCallback;

    public interface StatusCallback {
        public void timeoutCallback();
        public void onSuccessCallback(SaveMapResponse arg0);
        public void onFailureCallback(Exception e);
    }
    public void registerCallback(StatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

    public MapManager(final Context context, final AppRemappings remaps) {
        // Apply remappings
        saveSrvName = remaps.get(context.getString(R.string.save_map_srv));
		mapName = "";
	}

	public void setMapName(String name) {
		mapName = name;
	}

    public void setNameResolver(NameResolver newNameResolver) {
        nameResolver = newNameResolver;
        nameResolverSet = true;
    }

	private void clearWaitFor(){
        waitingFlag = false;
    }

    private boolean waitFor(final int timeout) {
        waitingFlag = true;
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                int count = 0;
                int timeout_count = timeout * 1000 / 200;
                while(waitingFlag){
                    try { Thread.sleep(200); }
                    catch (InterruptedException e) { return false; }
                    if(count < timeout_count){
                        count += 1;
                    }
                    else{
                        return false;
                    }
                }
                return true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            return asyncTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        } catch (TimeoutException e) {
            return false;
        }
    }

	public void saveMap(){
        ServiceClient<SaveMapRequest, SaveMapResponse> saveMapClient = null;
		if (connectedNode != null) {
			try{
                if (nameResolverSet){
                    saveSrvName = nameResolver.resolve(saveSrvName).toString();
                }
                saveMapClient = connectedNode.newServiceClient(saveSrvName,	SaveMap._TYPE);
			} catch (ServiceNotFoundException e) {
				try {
					Thread.sleep(1000L);
				} catch (Exception ex) {
				}
                statusCallback.onFailureCallback(e);
			}
            if (saveMapClient != null){
                final SaveMapRequest request = saveMapClient.newMessage();
                request.setMapName(mapName);
                saveMapClient.call(request, new ServiceResponseListener<SaveMapResponse>(){
                    @Override
                    public void onSuccess(SaveMapResponse saveMapResponse) {
                        if (waitingFlag){
                            clearWaitFor();
                            statusCallback.onSuccessCallback(saveMapResponse);
                        }
                    }
                    @Override
                    public void onFailure(RemoteException e) {
                        if (waitingFlag) {
                            clearWaitFor();
                            statusCallback.onFailureCallback(e);
                        }
                    }
                });
                if(!waitFor(10)){
                    statusCallback.timeoutCallback();
                }
            }

		}
	}
	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

    @Override
	public void onStart(final ConnectedNode connectedNode){
		super.onStart(connectedNode);
        this.connectedNode = connectedNode;
        saveMap();
    }
}
