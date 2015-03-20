package com.github.rosjava.android_apps.map_nav;

import android.content.Context;

import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import world_canvas_msgs.ListMaps;
import world_canvas_msgs.ListMapsRequest;
import world_canvas_msgs.ListMapsResponse;
import world_canvas_msgs.PublishMap;
import world_canvas_msgs.PublishMapRequest;
import world_canvas_msgs.PublishMapResponse;

public class MapManager extends AbstractNodeMain {

	private ConnectedNode connectedNode;
	private String function;
	private ServiceResponseListener<ListMapsResponse> listServiceResponseListener;
	private ServiceResponseListener<PublishMapResponse> publishServiceResponseListener;

	private String mapId;
    private String listSrvName;
    private String pubSrvName;
    private NameResolver nameResolver;
    private boolean nameResolverSet = false;
	
	public MapManager(final Context context, final AppRemappings remaps) {
        // Apply remappings
        listSrvName = remaps.get(context.getString(R.string.list_maps_srv));
        pubSrvName = remaps.get(context.getString(R.string.publish_map_srv));
	}
	
	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

    public void setNameResolver(NameResolver newNameResolver) {
        nameResolver = newNameResolver;
        nameResolverSet = true;
    }

    public void setFunction(String function) {
		this.function = function;
	}
	
	public void setListService(
			ServiceResponseListener<ListMapsResponse> listServiceResponseListener) {
		this.listServiceResponseListener = listServiceResponseListener;
	}
	
	public void setPublishService(
			ServiceResponseListener<PublishMapResponse> publishServiceResponseListener) {
		this.publishServiceResponseListener = publishServiceResponseListener;
	}

	public void listMaps() {
		ServiceClient<ListMapsRequest, ListMapsResponse> listMapsClient;
		try
        {
            if (nameResolverSet)
            {
                listSrvName = nameResolver.resolve(listSrvName).toString();
            }
			listMapsClient = connectedNode.newServiceClient(listSrvName, ListMaps._TYPE);
		} catch (ServiceNotFoundException e) {
		          try {
		            Thread.sleep(1000L);
		            listMaps();
		            return;
		          } catch (Exception ex) {}
		        
		        e.printStackTrace();
			throw new RosRuntimeException(e);
		}
		final ListMapsRequest request = listMapsClient.newMessage();
		listMapsClient.call(request, listServiceResponseListener);
	}
	
	public void publishMap() {
		ServiceClient<PublishMapRequest, PublishMapResponse> publishMapClient;
		
		try
        {
            if (nameResolverSet)
            {
                pubSrvName = nameResolver.resolve(pubSrvName).toString();
            }
			publishMapClient = connectedNode.newServiceClient(pubSrvName, PublishMap._TYPE);
		} catch (ServiceNotFoundException e) {
			 try {
		            Thread.sleep(1000L);
		            listMaps();
		            return;
		          } catch (Exception ex) {}
			throw new RosRuntimeException(e);
		}
		final PublishMapRequest request = publishMapClient.newMessage();
		request.setMapId(mapId);
		publishMapClient.call(request, publishServiceResponseListener);
	}
	

	
	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}
	
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		if (function.equals("list")) {
			listMaps();
		} else if (function.equals("publish")) {
			publishMap();
		}
	}
}

