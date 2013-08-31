package com.github.rosjava.android_apps.application_management.rapp_manager;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

/**
 * Used to publish the name (or unique identifier) of the android
 * application to a ros system on /android/app_name.
 *
 * Currently we just use this for introspection and with a pairing
 * mode watchdog that looks out for when the android application
 * may crash so that it can clean things up on the robot side.
 */
public class PairingApplicationNamePublisher extends AbstractNodeMain {
    private String name;
    private static final String TOPIC_NAME = "/pairing_master/android_app_name";
    private Publisher<std_msgs.String> publisher;

    public PairingApplicationNamePublisher(String name) {
        this.name = name;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/application_name");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher(PairingApplicationNamePublisher.TOPIC_NAME, std_msgs.String._TYPE);
        publisher.setLatchMode(Boolean.TRUE);
        std_msgs.String str = publisher.newMessage();
        str.setData(this.name);
        publisher.publish(str);
    }
}

