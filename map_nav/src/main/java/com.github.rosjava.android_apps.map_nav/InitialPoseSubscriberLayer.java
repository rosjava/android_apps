package com.github.rosjava.android_apps.map_nav;

import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.android.view.visualization.shape.GoalShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransform;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;


public class InitialPoseSubscriberLayer extends
		SubscriberLayer<geometry_msgs.PoseStamped> implements TfLayer {

	private final GraphName targetFrame;

	private Shape shape;

	public InitialPoseSubscriberLayer(String topic, String robotFrame) {
		this(GraphName.of(topic), robotFrame);
	}

	public InitialPoseSubscriberLayer(GraphName topic, String robotFrame) {
		super(topic, "geometry_msgs/PoseStamped");
		targetFrame = GraphName.of(robotFrame);
	}

	@Override
	public void draw(VisualizationView view, GL10 gl) {
			shape.draw(view, gl);
	}

	@Override
	public void onStart(final VisualizationView view, ConnectedNode connectedNode) {
        super.onStart(view, connectedNode);
        shape = new GoalShape();
		getSubscriber().addMessageListener(
				new MessageListener<geometry_msgs.PoseStamped>() {
					@Override
					public void onNewMessage(geometry_msgs.PoseStamped pose) {
                        GraphName source = GraphName.of(pose.getHeader()
								.getFrameId());
						FrameTransform frameTransform = view.getFrameTransformTree().transform(source, targetFrame);
						if (frameTransform != null) {
							Transform poseTransform = Transform
									.fromPoseMessage(pose.getPose());
							shape.setTransform(frameTransform.getTransform()
									.multiply(poseTransform));
						}
					}
				});
	}

	@Override
	public GraphName getFrame() {
		return targetFrame;
	}
}
