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

package com.github.ros_java.android_apps.application_management;

import android.util.Log;

import com.github.ros_java.android_apps.application_management.rapp_manager.PlatformInfoServiceClient;
import com.github.ros_java.android_apps.application_management.rapp_manager.StatusServiceClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.android.NodeMainExecutorService;

import rocon_app_manager_msgs.Icon;

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
     * Start the checker thread with the given robotId. If the thread is
     * already running, kill it first and then start anew. Returns immediately.
     */
    public void beginChecking(RobotId robotId) {
        stopChecking();
        if (robotId.getMasterUri() == null) {
            failureCallback.handleFailure("empty master URI");
            return;
        }
        URI uri;
        try {
            uri = new URI(robotId.getMasterUri());
        } catch (URISyntaxException e) {
            failureCallback.handleFailure("invalid master URI");
            return;
        }
        checkerThread = new CheckerThread(robotId, uri);
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
        private RobotId robotId;

        public CheckerThread(RobotId robotId, URI masterUri) {
            this.masterUri = masterUri;
            this.robotId = robotId;
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
                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                        InetAddressFactory.newNonLoopback().getHostAddress(),
                        masterUri);
                PlatformInfoServiceClient client = new PlatformInfoServiceClient();
                nodeMainExecutorService.execute(client, nodeConfiguration.setNodeName("platform_info_client_node"));
                client.waitForResponse();
                String robotName = client.getRobotUniqueName();
                String robotType = client.getRobotType();
                Icon robotIcon = client.getRobotIcon();
                StatusServiceClient statusClient = new StatusServiceClient(client.getRobotAppManagerNamespace());
                nodeMainExecutorService.execute(statusClient, nodeConfiguration.setNodeName("status_client_node"));
                statusClient.waitForResponse();
                nodeMainExecutorService.shutdownNodeMain(client);
                nodeMainExecutorService.shutdownNodeMain(statusClient);

                Date timeLastSeen = new Date();
                RobotDescription robotDescription = new RobotDescription(robotId, robotName, robotType, robotIcon,
                        timeLastSeen);
                if (statusClient.isAvailable()) {
                    robotDescription.setConnectionStatus(RobotDescription.OK);
                } else {
                    robotDescription.setConnectionStatus(RobotDescription.UNAVAILABLE);
                }

                foundMasterCallback.receive(robotDescription);
                return;
            } catch (Throwable ex) {
                Log.w("ApplicationManagement", "Exception while creating node in MasterChecker for master URI "
                        + masterUri, ex);
                failureCallback.handleFailure(ex.toString());
            }
        }
    }
}