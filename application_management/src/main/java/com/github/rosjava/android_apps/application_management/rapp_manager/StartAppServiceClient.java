package com.github.rosjava.android_apps.application_management.rapp_manager;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceException;
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

import rocon_app_manager_msgs.StartApp;
import rocon_app_manager_msgs.StartAppRequest;
import rocon_app_manager_msgs.StartAppResponse;

/**
 * Communicates with the robot app manager and attempts to start
 * a robot app.
 */
public class StartAppServiceClient extends AbstractNodeMain {
    private NameResolver resolver; // resolves namespace under which the service resides.
    private ServiceResponseListener<StartAppResponse> startAppListener;
    private String appName;
    private ConnectedNode connectedNode;
    private StartAppResponse response;
    private String errorMessage = "";

    /**
     * Configures the service client.
     *
     * @param appName : the resource name for the robot app (e.g. 'rocon_apps/talker')
     * @param resolver : master name resolver that later helps resolve 'start_app'.
     */
    public StartAppServiceClient(final String appName, NameResolver resolver) {
        this.appName = appName;
        this.resolver = resolver;
        this.response = null;
        this._createListeners();
    }

    public StartAppServiceClient() { this._createListeners(); }

    private void _createListeners() {
        this.startAppListener = new ServiceResponseListener<StartAppResponse>() {
            @Override
            public void onSuccess(StartAppResponse message) {
                if (message.getStarted()) {
                    Log.i("ApplicationManagement", "rapp started successfully [" + appName + "]");
                } else {
                    Log.e("ApplicationManagement", "rapp failed to start [" + appName + "][" + message.getMessage() + "]");
                    errorMessage = message.getMessage();
                }
                response = message;
                Log.i("ApplicationManagement", "start app response retrieved successfully");
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("ApplicationManagement", "failed to start app [" + appName + "][" + e.toString() + "]");
                errorMessage = e.toString();
            }
        };
    }

    /**
     * Utility function to block until the service's callback gets processed.
     *
     * @throws org.ros.exception.ServiceNotFoundException : when it can't find the service.
     * @throws org.ros.exception.ServiceException : for any other service error (incl. timeout)
     * @throws org.ros.exception.RosRuntimeException : for any ros related exceptin (e.g. shutdown).
     */
    public void waitForResponse() throws ServiceNotFoundException, ServiceException, RosRuntimeException {
        int count = 0;
        while ( response == null ) {
            if ( errorMessage != "" ) {  // errorMessage gets set by an exception in the run method
                throw new ServiceNotFoundException(errorMessage);
            }
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                throw new ServiceException(e);
            }
            if ( count == 20 ) {  // timeout.
                throw new ServiceException("timed out waiting for a start app service response");
            }
            count = count + 1;
        }
    }

    /**
     * This gets executed by the nodeMainExecutor. Note that any exception handling here will set an error
     * message that can be picked up when calling the waitForResponse() method.
     *
     * @param connectedNode
     */
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            errorMessage = "service client instances may only ever be executed once";
            Log.e("ApplicationManagement", errorMessage + ".");
            return;
        }
        this.connectedNode = connectedNode;

        String startTopic = resolver.resolve("start_app").toString();
        ServiceClient<StartAppRequest, StartAppResponse> startAppClient;
        try {
            Log.d("ApplicationManagement", "start app service client created [" + startTopic + "]");
            startAppClient = connectedNode.newServiceClient(startTopic,
                    StartApp._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("ApplicationManagement", "start app service not found [" + startTopic + "]");
            errorMessage = "start app service not found";
            return;
        }
        final StartAppRequest request = startAppClient.newMessage();
        request.setName(appName);
        startAppClient.call(request, startAppListener);
        Log.d("ApplicationManagement", "start app service call done [" + startTopic + "]");
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
