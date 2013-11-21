package com.github.rosjava.android_apps.application_management;

import android.util.Log;

import org.ros.master.client.MasterStateClient;
import org.ros.master.client.SystemState;
import org.ros.master.client.TopicSystemState;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

public class MasterNameResolver extends AbstractNodeMain {

	private MasterDescription currentMaster;
	private NameResolver applicationNamespaceResolver;
	private NameResolver masterNameResolver;
	private GraphName name;
	private GraphName applicationNamespace;
	private ConnectedNode connectedNode;
    private boolean resolved = false;

	public MasterNameResolver() {
	}

	public void setMaster(MasterDescription currentMaster) {
		this.currentMaster = currentMaster;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	public void setMasterName(String name) {
		this.name = GraphName.of(name);
	}
	
	public void resetMasterName(String name) {
		masterNameResolver = connectedNode.getResolver().newChild(name);
	}


	public NameResolver getAppNameSpace() {
		return applicationNamespaceResolver;
	}

    public NameResolver getMasterNameSpace() {
		return masterNameResolver;
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
                Log.w("MasterRemocon", "Master name waitForResolver caught an arbitrary exception");
            }
        }
    }

	@Override
    /**
     * Resolves the namespace under which master apps can be started
     * and stopped. Sometimes this will already have been provided
     * via setMaster() by managing applications (e.g. remocons) which
     * use the MasterChecker.
     *
     * In other cases, such as when the
     * application directly connects, we do a simple parameter
     * lookup, falling back to a default if provided.
     */
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
        if (currentMaster != null) {
            name = GraphName.of(currentMaster.getMasterName());
        } else {
            // This is duplicated in PlatformInfoServiceClient and could be better stored somewhere, but it's not much.
            MasterStateClient masterClient = new MasterStateClient(this.connectedNode, this.connectedNode.getMasterUri());
            SystemState systemState = masterClient.getSystemState();
            for (TopicSystemState topic : systemState.getTopics()) {
                String name = topic.getTopicName();
                GraphName graph_name = GraphName.of(name);
                if ( graph_name.getBasename().toString().equals("app_list") ) {
                    this.name = graph_name.getParent().toRelative();
                    Log.i("ApplicationManagement", "Configuring master namespace resolver [" + this.name + "]");
                    break;
                }
            }
        }
        applicationNamespace = name.join(GraphName.of("application"));  // hard coded, might we need to change this?
        applicationNamespaceResolver = connectedNode.getResolver().newChild(applicationNamespace);
        masterNameResolver = connectedNode.getResolver().newChild(name);
        resolved = true;
	}
}
