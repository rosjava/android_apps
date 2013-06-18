package com.github.ros_java.android_apps.application_management.rapp_manager;

import android.util.Log;
import android.widget.Toast;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.master.client.TopicSystemState;
import org.ros.master.client.SystemState;
import org.ros.master.client.MasterStateClient;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import rocon_app_manager_msgs.Icon;
import rocon_app_manager_msgs.PlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfoRequest;
import rocon_app_manager_msgs.GetPlatformInfoResponse;

/**
 * Communicates with the robot app manager's platform info service.
 */
public class PlatformInfoServiceClient extends AbstractNodeMain {
    private String namespace; // this is the namespace under which all app manager services reside.
    private String robotUniqueName; // unique robot name, simply the above with stripped '/''s.
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
        this._createListener();
    }

    public PlatformInfoServiceClient() { this._createListener(); }

    private void _createListener() {
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
    public void waitForResponse() {
        while (platformInfo == null) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public String getRobotAppManagerNamespace() {
        return this.namespace;
    }

    /**
     * Robot unique name is simply the unadorned app manager namespace (i.e. no '/').
     * @return
     */
    public String getRobotUniqueName() {
        return this.robotUniqueName;
    }

    public String getRobotType() {
        return this.platformInfo.getRobot();
    }

    public Icon getRobotIcon() {
        return this.platformInfo.getIcon();
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("ApplicationManagement", "service client instances may only ever be executed once.");
            return;
        }
        this.connectedNode = connectedNode;

        if ( this.namespace == null ) {
            // Find the app manager namespace
            MasterStateClient masterClient = new MasterStateClient(this.connectedNode, this.connectedNode.getMasterUri());
            SystemState systemState = masterClient.getSystemState();
            for (TopicSystemState topic : systemState.getTopics()) {
                String name = topic.getTopicName();
                GraphName graph_name = GraphName.of(name);
                if ( graph_name.getBasename().toString().equals("app_list") ) {
                    this.namespace = graph_name.getParent().toString();
                    this.robotUniqueName = graph_name.getParent().toRelative().toString();
                    Log.i("ApplicationManagement", "found the namespace for the robot app manager [" + this.namespace + "]");
                    break;
                }
            }
        }

        // Find the platform information
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
        } catch (RosRuntimeException e) {
            Log.e("ApplicationManagement", "failed to create platform info client [" + e.getMessage() + "]");
            throw e;
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
