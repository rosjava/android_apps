package com.github.ros_java.android_apps.application_management.rapp_manager;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import rocon_app_manager_msgs.GetPlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfoRequest;
import rocon_app_manager_msgs.GetPlatformInfoResponse;
import rocon_app_manager_msgs.PlatformInfo;

/**
 * Communicates with the robot app manager's platform info service.
 */
public class PlatformInfoServiceClient extends AbstractNodeMain {
    private String namespace;
    private ServiceResponseListener<GetPlatformInfoResponse> listener;
    private PlatformInfo platformInfo;
    private ConnectedNode connectedNode;

    /**
     * Configures a platform info service client.
     *
     * @param namespace : configured with the namespace for the app manager
     */
    public PlatformInfoServiceClient(String namespace) {
        this.namespace = namespace;
        this.listener = new ServiceResponseListener<GetPlatformInfoResponse>() {
            @Override
            public void onSuccess(GetPlatformInfoResponse message) {
                Log.i("ApplicationManagement", "platform info retrieved successfully");
                platformInfo = message.getPlatformInfo();
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("ApplicationManagement", "failed to get platform information!");
            }
        };
    }

    /**
     * Utility function to block until platform info's callback gets processed.
     */
    public PlatformInfo waitForResponse() {
        while (platformInfo == null) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        return platformInfo;
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("ApplicationManagement", "service client instances may only ever be executed once.");
            return;
        }
        this.connectedNode = connectedNode;
        NameResolver resolver = this.connectedNode.getResolver().newChild(this.namespace);
        String serviceName = resolver.resolve("platform_info").toString();
        ServiceClient<GetPlatformInfoRequest, GetPlatformInfoResponse> client;
        try {
            Log.d("ApplicationManagement", "platform info service client created [" + serviceName + "]");
            client = connectedNode.newServiceClient(serviceName,
                    GetPlatformInfo._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("ApplicationManagement", "platform_info service not found [" + serviceName + "]");
            throw new RosRuntimeException(e);
        }
        final GetPlatformInfoRequest request = client.newMessage();
        client.call(request, listener);
        Log.d("ApplicationManagement", "platform info service call done [" + serviceName + "]");
    }

    /**
     * This is unused, but abstract, so have to override.
     * @return
     */
    @Override
    public GraphName getDefaultNodeName() {
        return null;
    }
}
