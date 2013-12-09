/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_apps.application_management;

import android.util.Log;

import com.github.rosjava.android_apps.application_management.rapp_manager.GatewayInfoSubscriber;
import com.github.rosjava.android_apps.application_management.rapp_manager.PlatformInfoServiceClient;
import com.github.rosjava.android_apps.application_management.rapp_manager.StatusServiceClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.address.InetAddressFactory;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.android.NodeMainExecutorService;

import rocon_std_msgs.Icon;

/**
 * Threaded ROS-master checker. Runs a thread which checks for a valid ROS
 * master and sends back a {@link RobotDescription} (with robot name and type)
 * on success or a failure reason on failure.
 *
 * @author hersh@willowgarage.com
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MasterChecker {
    public interface RobotDescriptionReceiver {
        /**
         * Called on success with a description of the robot that got checked.
         */
        void receive(RobotDescription robotDescription);
    }

    public interface FailureHandler {
        /**
         * Called on failure with a short description of why it failed, like
         * "exception" or "timeout".
         */
        void handleFailure(String reason);
    }

    private CheckerThread checkerThread;
    private RobotDescriptionReceiver foundMasterCallback;
    private FailureHandler failureCallback;

    /**
     * Constructor. Should not take any time.
     */
    public MasterChecker(RobotDescriptionReceiver foundMasterCallback, FailureHandler failureCallback) {
        this.foundMasterCallback = foundMasterCallback;
        this.failureCallback = failureCallback;
    }

    /**
     * Start the checker thread with the given masterId. If the thread is
     * already running, kill it first and then start anew. Returns immediately.
     */
    public void beginChecking(MasterId masterId) {
        stopChecking();
        if (masterId.getMasterUri() == null) {
            failureCallback.handleFailure("empty master URI");
            return;
        }
        URI uri;
        try {
            uri = new URI(masterId.getMasterUri());
        } catch (URISyntaxException e) {
            failureCallback.handleFailure("invalid master URI");
            return;
        }
        checkerThread = new CheckerThread(masterId, uri);
        checkerThread.start();
    }

    /**
     * Stop the checker thread.
     */
    public void stopChecking() {
        if (checkerThread != null && checkerThread.isAlive()) {
            checkerThread.interrupt();
        }
    }

    private class CheckerThread extends Thread {
        private URI masterUri;
        private MasterId masterId;

        public CheckerThread(MasterId masterId, URI masterUri) {
            this.masterUri = masterUri;
            this.masterId = masterId;
            setDaemon(true);
            // don't require callers to explicitly kill all the old checker threads.
            setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    failureCallback.handleFailure("exception: " + ex.getMessage());
                }
            });
        }

        @Override
        public void run() {
            try {
                // Check if the master exists - no really good way in rosjava except by checking a standard parameter.
                ParameterClient paramClient = new ParameterClient(
                        NodeIdentifier.forNameAndUri("/master_checker", masterUri.toString()), masterUri);
                // getParam throws when it can't find the parameter.
                String unused_rosversion = (String) paramClient.getParam(GraphName.of("rosversion")).getResult();

                // Check for the platform information - be sure to check that master exists first otherwise you'll
                // start a thread which perpetually crashes and triest to re-register in .execute()
                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                        InetAddressFactory.newNonLoopback().getHostAddress(),
                        masterUri);
                GatewayInfoSubscriber gatewayInfoClient = new GatewayInfoSubscriber();
                nodeMainExecutorService.execute(gatewayInfoClient, nodeConfiguration.setNodeName("gateway_info_client_node"));
                gatewayInfoClient.waitForResponse();
                String gatewayName = gatewayInfoClient.getGatewayName();
                PlatformInfoServiceClient client = new PlatformInfoServiceClient();
                nodeMainExecutorService.execute(client, nodeConfiguration.setNodeName("platform_info_client_node"));
                client.waitForResponse();
                String robotName = client.getRobotUniqueName();
                String robotType = client.getRobotType();
                Icon robotIcon = client.getRobotIcon();
                StatusServiceClient statusClient = new StatusServiceClient(client.getRobotAppManagerNamespace(), gatewayName);
                nodeMainExecutorService.execute(statusClient, nodeConfiguration.setNodeName("status_client_node"));
                statusClient.waitForResponse();
                nodeMainExecutorService.shutdownNodeMain(client);
                nodeMainExecutorService.shutdownNodeMain(gatewayInfoClient);
                nodeMainExecutorService.shutdownNodeMain(statusClient);

                // configure robot description
                Date timeLastSeen = new Date();
                RobotDescription robotDescription = new RobotDescription(masterId, robotName, robotType, robotIcon, gatewayName,
                        timeLastSeen);
                if (statusClient.isAvailable()) {
                    Log.i("ApplicationManagement", "rapp manager is available");
                    robotDescription.setConnectionStatus(RobotDescription.OK);
                } else {
                    Log.i("ApplicationManagement", "rapp manager is unavailable");
                    robotDescription.setConnectionStatus(RobotDescription.UNAVAILABLE);
                }
                foundMasterCallback.receive(robotDescription);
                return;
            } catch ( java.lang.RuntimeException e) {
                // thrown if master could not be found in the getParam call (from java.net.ConnectException)
                Log.w("ApplicationManagement", "could not find the master [" + masterUri + "][" + e.toString() + "]");
                failureCallback.handleFailure(e.toString());
            } catch (ServiceNotFoundException e) {
                // thrown by client.waitForResponse() if it times out
                Log.w("ApplicationManagement", e.getMessage()); // e.getMessage() is a little less verbose (no org.ros.exception.ServiceNotFoundException prefix)
                failureCallback.handleFailure(e.getMessage());  // don't need the master uri, it's already shown above in the robot description from input method.
            } catch (Throwable e) {
                Log.w("ApplicationManagement", "exception while creating node in masterchecker for master URI "
                        + masterUri, e);
                failureCallback.handleFailure(e.toString());
            }
        }
    }
}