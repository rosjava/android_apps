package org.ros.android.map_nav;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.android.view.visualization.shape.RobotShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransform;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.os.Handler;

public class InitialPoseSubscriberLayer extends
		SubscriberLayer<geometry_msgs.PoseStamped> implements TfLayer {

	private static final String ROBOT_FRAME = "base_link";
	private final GraphName targetFrame;

	private Shape shape;

	public InitialPoseSubscriberLayer(String topic) {
		this(GraphName.of(topic));
	}

	public InitialPoseSubscriberLayer(GraphName topic) {
		super(topic, "geometry_msgs/PoseStamped");
		targetFrame = GraphName.of(ROBOT_FRAME);
	}

	@Override
	public void draw(GL10 gl) {
			shape.draw(gl);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler,
			final FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
	    shape = new RobotShape();
		getSubscriber().addMessageListener(
				new MessageListener<geometry_msgs.PoseStamped>() {
					@Override
					public void onNewMessage(geometry_msgs.PoseStamped pose) {
						GraphName source = GraphName.of(pose.getHeader()
								.getFrameId());
						FrameTransform frameTransform = frameTransformTree
								.transform(source, targetFrame);
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
