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
	private NameResolver masterNameResolver;
	private GraphName    masterName;
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
        this.masterName = GraphName.of(name);
    }

    /**
     * Return the master name as is that will be resolved when actually
     * connected to a node.
     *
     * @return the name,  e.g. 'turtlebot'.
     */
    public String getMasterName() {
        return this.masterName.toString();
    }
	
	public void resetMasterName(String name) {
		masterNameResolver = connectedNode.getResolver().newChild(name);
	}

    /**
     * The resolved master namespace (after connecting to a master).
     *
     * Warning: Do not call this before actually starting the resolver,
     * or else it will return a null object.
     *
     * todo : get this to throw an exception if null
     *
     * @return the master name resolver
     */
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
     * via setMaster() by managed applications (e.g. remocons) which
     * use the MasterChecker.
     *
     * In other cases, such as standalone application we do a simple
     * parameter lookup, falling back to a default if provided.
     */
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
        if (currentMaster != null) {
            masterName = GraphName.of(currentMaster.getAppsNameSpace());
        } else {
            // This is duplicated in PlatformInfoServiceClient and could be better stored somewhere, but it's not much.
            MasterStateClient masterClient = new MasterStateClient(this.connectedNode, this.connectedNode.getMasterUri());
            SystemState systemState = masterClient.getSystemState();
            for (TopicSystemState topic : systemState.getTopics()) {
                String topicName = topic.getTopicName();
                GraphName graph_name = GraphName.of(topicName);
                if ( graph_name.getBasename().toString().equals("app_list") ) {
                    masterName = graph_name.getParent().toRelative();
                    Log.i("ApplicationManagement", "Configuring master namespace resolver [" + masterName + "]");
                    break;
                }
            }
        }
        masterNameResolver = connectedNode.getResolver().newChild(masterName);
        resolved = true;
	}
}
