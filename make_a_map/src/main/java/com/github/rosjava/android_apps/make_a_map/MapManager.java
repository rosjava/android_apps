package com.github.rosjava.android_apps.make_a_map;

import android.content.Context;

import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;

import map_store.SaveMap;
import map_store.SaveMapRequest;
import map_store.SaveMapResponse;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

public class MapManager extends AbstractNodeMain {

	private ConnectedNode connectedNode;
	private ServiceResponseListener<SaveMapResponse> saveServiceResponseListener;

	private String mapName;
    private String saveSrvName;
    private NameResolver nameResolver;
    private boolean nameResolverSet = false;

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

	public void setSaveService(
			ServiceResponseListener<SaveMapResponse> saveServiceResponseListener) {
		this.saveServiceResponseListener = saveServiceResponseListener;
	}

	public void saveMap() {
		ServiceClient<SaveMapRequest, SaveMapResponse> saveMapClient;
		if (connectedNode != null) {
			try
            {
                if (nameResolverSet)
                {
                    saveSrvName = nameResolver.resolve(saveSrvName).toString();
                }
                saveMapClient = connectedNode.newServiceClient(saveSrvName,	SaveMap._TYPE);
			} catch (ServiceNotFoundException e) {
				try {
					Thread.sleep(1000L);
					return;
				} catch (Exception ex) {
				}
				throw new RosRuntimeException(e);
			}
			final SaveMapRequest request = saveMapClient.newMessage();
			request.setMapName(mapName);
			saveMapClient.call(request, saveServiceResponseListener);
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		saveMap();
	}
}
