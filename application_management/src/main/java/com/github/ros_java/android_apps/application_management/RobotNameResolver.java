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
	private NameResolver appNameResolver;
	private NameResolver robotNameResolver;
	private GraphName name;
	private GraphName app;
	private ConnectedNode connectedNode;

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


	protected NameResolver getAppNameSpace() {
		return appNameResolver;
	}

	protected NameResolver getRobotNameSpace() {
		return robotNameResolver;
	}

	@Override
    /**
     * Resolves the namespace under which robot apps can be started
     * and stopped. Sometimes this will already have been provided
     * via setRobot(), for example by appchooser or remocons which
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
            // Could actually construct a RobotDescription here if we wished.
            ParameterTree parameterTree = this.connectedNode.getParameterTree();
            try {
                name = GraphName.of(parameterTree.getString("/robot/name"));
            } catch (ParameterClassCastException e) {
                Log.i("RosApplicationManagement", "Couldn't find the robot name on the parameter server, falling back to defaults.");
            }
        }
		app = name.join(GraphName.of("application"));
		appNameResolver = connectedNode.getResolver().newChild(app);
		robotNameResolver = connectedNode.getResolver().newChild(name);
	}
}
