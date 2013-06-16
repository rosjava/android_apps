package com.github.ros_java.android_apps.application_management;

import android.util.Log;

import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.exception.ParameterClassCastException;

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
            // Could simple scan the master graph looking for the set of app manager services,
            // validating that and pulling the name from the first name in the tree.
            // Everything else would then come from platform info.
            // Would save a parameter, but maybe just easier all around with a parameter.
            ParameterTree parameterTree = this.connectedNode.getParameterTree();
            try {
                name = GraphName.of(parameterTree.getString("/robot/name"));
            } catch (ParameterClassCastException e) {
                Log.w("ApplicationManagement", "Couldn't find the robot name on the parameter server, falling back to defaults.");
            }
        }
        applicationNamespace = name.join(GraphName.of("application"));  // hard coded, might we need to change this?
        applicationNamespaceResolver = connectedNode.getResolver().newChild(applicationNamespace);
		robotNameResolver = connectedNode.getResolver().newChild(name);
        resolved = true;
	}
}
