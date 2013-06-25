package com.github.ros_java.android_apps.application_management.rapp_manager;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.master.client.MasterStateClient;
import org.ros.master.client.SystemState;
import org.ros.master.client.TopicSystemState;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import rocon_app_manager_msgs.Invite;
import rocon_app_manager_msgs.InviteRequest;
import rocon_app_manager_msgs.InviteResponse;

/**
 * Service with which the android client can invite the robot app manager
 * to permit this device to remotely control its applications.
 */
public class InvitationServiceClient extends AbstractNodeMain {
    private String namespace; // this is the namespace under which all rapp manager services reside.
    private ServiceResponseListener<InviteResponse> listener;
    private Boolean invitationAccepted = null;
    private ConnectedNode connectedNode;

    /**
     * Configures the service client.
     *
     * @param namespace : namespace for the app manager's services
     */
    public InvitationServiceClient(String namespace) {
        this.namespace = namespace;
        this.listener = new ServiceResponseListener<InviteResponse>() {
            @Override
            public void onSuccess(InviteResponse message) {
                if ( invitationAccepted = message.getResult() ) {
                    Log.i("ApplicationManagement", "invitation accepted");
                } else {
                    Log.w("ApplicationManagement", "invitation rejected");
                }
            }
            @Override
            public void onFailure(RemoteException e) {
                Log.e("ApplicationManagement", "failed to send invitation!");
            }
        };
    }

    /**
     * Utility function to block until the callback gets processed.
     *
     * It returns an error if it's timed out.
     */
    public Boolean waitForResponse() {
        int count = 0;
        while ( (invitationAccepted == null) && (count < 20) ) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            count = count + 1;
        }
        if ( count < 20 ) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public Boolean getInvitationResult() {
        return invitationAccepted;
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("ApplicationManagement", "service client instances may only ever be executed once.");
            return;
        }
        this.connectedNode = connectedNode;
        NameResolver resolver = this.connectedNode.getResolver().newChild("pairing_master"); //.newChild(this.namespace);
        String serviceName = resolver.resolve("invite").toString();
        ServiceClient<InviteRequest, InviteResponse> client;
        try {
            Log.d("ApplicationManagement", "service client created [" + serviceName + "]");
            client = connectedNode.newServiceClient(serviceName,
                    Invite._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("ApplicationManagement", "service not found [" + serviceName + "]");
            throw new RosRuntimeException(e);
        } catch (RosRuntimeException e) {
            Log.e("ApplicationManagement", "failed to create client [" + e.getMessage() + "]");
            throw e;
        }
        final InviteRequest request = client.newMessage();
        client.call(request, listener);
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
