package com.github.rosjava.android_apps.application_management;

import android.util.Log;

import org.ros.master.client.MasterStateClient;
import org.ros.master.client.SystemState;
import org.ros.master.client.TopicSystemState;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

public class RobotNameResolver extends AbstractNodeMain {

	private RobotDescription currentRobot;
	private NameResolver applicationNamespaceResolver;
	private NameResolver robotNameResolver;
	private GraphName name;
	private GraphName applicationNamespace;
	private ConnectedNode connectedNode;
    private boolean resolved = false;

	public RobotNameResolver() {
	}

	public void setRobot(RobotDescription currentRobot) {
		this.currentRobot = currentRobot;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	public void setRobotName(String name) {
		this.name = GraphName.of(name);
	}
	
	public void resetRobotName(String name) {
		robotNameResolver = connectedNode.getResolver().newChild(name);
	}


	public NameResolver getAppNameSpace() {
		return applicationNamespaceResolver;
	}

    public NameResolver getRobotNameSpace() {
		return robotNameResolver;
	}

    /**
     * Call this to block until the resolver finishes its job.
     * i.e. after an execute is called to run the onStart method
     * below.
     *
     * Note - BLOCKING call!
     */
    public void waitForResolver() {
        while (!resolved) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                Log.w("RobotRemocon", "robot name waitForResolver caught an arbitrary exception");
            }
        }
    }

	@Override
    /**
     * Resolves the namespace under which robot apps can be started
     * and stopped. Sometimes this will already have been provided
     * via setRobot() by managing applications (e.g. remocons) which
     * use the MasterChecker.
     *
     * In other cases, such as when the
     * application directly connects, we do a simple parameter
     * lookup, falling back to a default if provided.
     */
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
        if (currentRobot != null) {
            name = GraphName.of(currentRobot.getRobotName());
        } else {
            // This is duplicated in PlatformInfoServiceClient and could be better stored somewhere, but it's not much.
            MasterStateClient masterClient = new MasterStateClient(this.connectedNode, this.connectedNode.getMasterUri());
            SystemState systemState = masterClient.getSystemState();
            for (TopicSystemState topic : systemState.getTopics()) {
                String name = topic.getTopicName();
                GraphName graph_name = GraphName.of(name);
                if ( graph_name.getBasename().toString().equals("app_list") ) {
                    this.name = graph_name.getParent().toRelative();
                    Log.i("ApplicationManagement", "configuring a robot namespace resolver [" + this.name + "]");
                    break;
                }
            }
        }
        applicationNamespace = name.join(GraphName.of("application"));  // hard coded, might we need to change this?
        applicationNamespaceResolver = connectedNode.getResolver().newChild(applicationNamespace);
        robotNameResolver = connectedNode.getResolver().newChild(name);
        resolved = true;
	}
}
