package com.github.ros_java.android_apps.application_management.rapp_manager;

import android.util.Log;
import android.widget.Toast;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.master.client.TopicSystemState;
import org.ros.master.client.SystemState;
import org.ros.master.client.MasterStateClient;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;

import rocon_app_manager_msgs.Icon;
import rocon_app_manager_msgs.PlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfoRequest;
import rocon_app_manager_msgs.GetPlatformInfoResponse;
import gateway_msgs.GatewayInfo;

/**
 * Communicates with the robot app manager and determines various facets of
 * the platform information. Actually does a bit more than platform info.
 *
 * - Determines the local gateway name belonging to this master.
 * - Determines the robot app manager's namespace.
 * - Retrieves details from the PlatformInfo service.
 */
public class PlatformInfoServiceClient extends AbstractNodeMain {
    private String namespace; // this is the namespace under which all rapp manager services reside.
    private String robotUniqueName; // unique robot name, simply the above with stripped '/''s.
    private String localGatewayName = null;
    private ServiceResponseListener<GetPlatformInfoResponse> platformInfoListener;
    private PlatformInfo platformInfo;
    private ConnectedNode connectedNode;
    private MessageListener<GatewayInfo> gatewayInfoListener;
    private Subscriber<GatewayInfo> gatewayInfoSubscriber;

    /**
     * Configures the service client.
     *
     * @param namespace : namespace for the app manager's services
     */
    public PlatformInfoServiceClient(String namespace) {
        this.namespace = namespace;
        this._createListeners();
    }

    public PlatformInfoServiceClient() { this._createListeners(); }

    private void _createListeners() {
        this.platformInfoListener = new ServiceResponseListener<GetPlatformInfoResponse>() {
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
        this.gatewayInfoListener = new MessageListener<GatewayInfo>() {
            @Override
            public void onNewMessage(GatewayInfo message) {
                localGatewayName = (String) message.getName();
            }
        };
    }

    /**
     * Utility function to block until platform info's callback gets processed.
     */
    public void waitForResponse() {
        while( (platformInfo == null) || (localGatewayName == null) ) {
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

        // Find the rapp manager namespace
        if ( this.namespace == null ) {
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

        // Find the gateway information
        NameResolver resolver = this.connectedNode.getResolver().newChild(this.namespace);
        gatewayInfoSubscriber = this.connectedNode.newSubscriber(resolver.resolve("gateway_info"),"gateway_msgs/GatewaqyInfo");
        gatewayInfoSubscriber.addMessageListener(gatewayInfoListener);

        // Find the platform information
        String serviceName = resolver.resolve("platform_info").toString();
        ServiceClient<GetPlatformInfoRequest, GetPlatformInfoResponse> client;
        try {
            Log.d("ApplicationManagement", "service client created [" + serviceName + "]");
            client = connectedNode.newServiceClient(serviceName,
                    GetPlatformInfo._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("ApplicationManagement", "service not found [" + serviceName + "]");
            throw new RosRuntimeException(e);
        } catch (RosRuntimeException e) {
            Log.e("ApplicationManagement", "failed to create client [" + e.getMessage() + "]");
            throw e;
        }
        final GetPlatformInfoRequest request = client.newMessage();
        client.call(request, platformInfoListener);
        Log.d("ApplicationManagement", "service call done [" + serviceName + "]");
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
