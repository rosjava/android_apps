package com.github.rosjava.android_apps.map_manager;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import world_canvas_msgs.DeleteMap;
import world_canvas_msgs.DeleteMapRequest;
import world_canvas_msgs.DeleteMapResponse;
import world_canvas_msgs.ListMaps;
import world_canvas_msgs.ListMapsRequest;
import world_canvas_msgs.ListMapsResponse;
import world_canvas_msgs.PublishMap;
import world_canvas_msgs.PublishMapRequest;
import world_canvas_msgs.PublishMapResponse;
import world_canvas_msgs.RenameMap;
import world_canvas_msgs.RenameMapRequest;
import world_canvas_msgs.RenameMapResponse;


public class MapManager extends AbstractNodeMain {

	private ConnectedNode connectedNode;
	private String function;
	private ServiceResponseListener<ListMapsResponse> listServiceResponseListener;
	private ServiceResponseListener<PublishMapResponse> publishServiceResponseListener;
	private ServiceResponseListener<DeleteMapResponse> deleteServiceResponseListener;
	private ServiceResponseListener<RenameMapResponse> renameServiceResponseListener;
	private String mapId;
	private String mapName;
    private NameResolver nameResolver;
    private boolean nameResolverSet = false;
	
	public MapManager() {
	}
	
	public void setMapId(String mapId) {
		this.mapId = mapId;
	}
	
	public void setMapName(String mapName) {
		this.mapName = mapName;
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
	
	public void setDeleteService(
			ServiceResponseListener<DeleteMapResponse> deleteServiceResponseListener) {
		this.deleteServiceResponseListener = deleteServiceResponseListener;
	}
	
	public void setRenameService(
			ServiceResponseListener<RenameMapResponse> renameServiceResponseListener) {
		this.renameServiceResponseListener = renameServiceResponseListener;
	}

    public void setNameResolver(NameResolver newNameResolver) {
        nameResolver = newNameResolver;
        nameResolverSet = true;
    }
	
	public void listMaps() {
		ServiceClient<ListMapsRequest, ListMapsResponse> listMapsClient;
		try {
            String srvName = "list_maps";
            if (nameResolverSet)
            {
                srvName = nameResolver.resolve(srvName).toString();
            }
			listMapsClient = connectedNode.newServiceClient(srvName, ListMaps._TYPE);
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
            String srvName = "publish_map";
            if (nameResolverSet)
            {
                srvName = nameResolver.resolve(srvName).toString();
            }
            publishMapClient = connectedNode.newServiceClient(srvName, PublishMap._TYPE);
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
	
	public void deleteMap() {
		ServiceClient<DeleteMapRequest, DeleteMapResponse> deleteMapClient;
		
		try {
            String srvName = "delete_map";
            if (nameResolverSet)
            {
                srvName = nameResolver.resolve(srvName).toString();
            }
            deleteMapClient = connectedNode.newServiceClient(srvName, DeleteMap._TYPE);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
		final DeleteMapRequest request = deleteMapClient.newMessage();
		request.setMapId(mapId);
		deleteMapClient.call(request, deleteServiceResponseListener);
	}
	
	public void renameMap() {
		ServiceClient<RenameMapRequest, RenameMapResponse> renameMapClient;
		
		try {
            String srvName = "rename_map";
            if (nameResolverSet)
            {
                srvName = nameResolver.resolve(srvName).toString();
            }
            renameMapClient = connectedNode.newServiceClient(srvName, RenameMap._TYPE);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
		final RenameMapRequest request = renameMapClient.newMessage();
		request.setMapId(mapId);
		request.setNewName(mapName);
		renameMapClient.call(request, renameServiceResponseListener);
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
		} else if (function.equals("delete")) {
			deleteMap();
		} else if (function.equals("rename")) {
			renameMap();
		} 
	}
}
