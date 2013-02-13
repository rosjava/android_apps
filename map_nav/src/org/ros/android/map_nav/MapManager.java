package org.ros.android.map_nav;

import map_store.ListMaps;
import map_store.ListMapsRequest;
import map_store.ListMapsResponse;
import map_store.PublishMap;
import map_store.PublishMapRequest;
import map_store.PublishMapResponse;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

public class MapManager extends AbstractNodeMain {

	
	
	private ConnectedNode connectedNode;
	private String function;
	private ServiceResponseListener<ListMapsResponse> listServiceResponseListener;
	private ServiceResponseListener<PublishMapResponse> publishServiceResponseListener;


	private String mapId;

	
	public MapManager() {
	}
	
	public void setMapId(String mapId) {
		this.mapId = mapId;
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
		try {
			listMapsClient = connectedNode.newServiceClient(
					"/list_maps", ListMaps._TYPE);
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
		
		try {
			publishMapClient = connectedNode.newServiceClient(
					"/publish_map", PublishMap._TYPE);
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

