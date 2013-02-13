package org.ros.android.map_nav;

import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.shape.PoseShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;
import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.common.base.Preconditions;

public class MapPosePublisherLayer extends DefaultLayer {

	private final Context context;

	private Shape shape;
	private Publisher<geometry_msgs.PoseWithCovarianceStamped> initialPosePublisher;
	private Publisher<geometry_msgs.PoseStamped> androidGoalPublisher;
	private Publisher<move_base_msgs.MoveBaseActionGoal> goalPublisher;
	private boolean visible;
	private GraphName topic;
	private GestureDetector gestureDetector;
	private Transform pose;
	private Transform fixedPose;
	private Camera camera;
	private ConnectedNode connectedNode;
	private int mode;
	private static final int POSE_MODE = 0;
	private static final int GOAL_MODE = 1;

	public MapPosePublisherLayer(String topic, Context context) {
		this(GraphName.of(topic), context);
	}

	public MapPosePublisherLayer(GraphName topic, Context context) {
		this.topic = topic;
		this.context = context;
		visible = false;
	}

	public void setPoseMode() {
		mode = POSE_MODE;
	}

	public void setGoalMode() {
		mode = GOAL_MODE;
	}

	@Override
	public void draw(GL10 gl) {
		if (visible) {
			Preconditions.checkNotNull(pose);
			shape.draw(gl);
		}
	}

	private double angle(double x1, double y1, double x2, double y2) {
		double deltaX = x1 - x2;
		double deltaY = y1 - y2;
		return Math.atan2(deltaY, deltaX);
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		if (visible) {
			Preconditions.checkNotNull(pose);

			Vector3 poseVector;
			Vector3 pointerVector;

			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				poseVector = pose.apply(Vector3.zero());
				pointerVector = camera.toMetricCoordinates((int) event.getX(),
						(int) event.getY());

				double angle = angle(pointerVector.getX(),
						pointerVector.getY(), poseVector.getX(),
						poseVector.getY());
				pose = Transform.translation(poseVector).multiply(
						Transform.zRotation(angle));

				shape.setTransform(pose);
				return true;
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {

				PoseStamped poseStamped;
				switch (mode) {
				case POSE_MODE:
					camera.setFrame("/map");
					poseVector = fixedPose.apply(Vector3.zero());
					pointerVector = camera.toMetricCoordinates(
							(int) event.getX(), (int) event.getY());
					double angle2 = angle(pointerVector.getX(),
							pointerVector.getY(), poseVector.getX(),
							poseVector.getY());
					fixedPose = Transform.translation(poseVector).multiply(
							Transform.zRotation(angle2));
					camera.setFrame("/base_link");
					poseStamped = fixedPose.toPoseStampedMessage(
							GraphName.of("/base_link"),
							connectedNode.getCurrentTime(),
							androidGoalPublisher.newMessage());

					PoseWithCovarianceStamped initialPose = initialPosePublisher
							.newMessage();
					initialPose.getHeader().setFrameId("/map");
					initialPose.getPose().setPose(poseStamped.getPose());
					double[] covariance = initialPose.getPose().getCovariance();
					covariance[6 * 0 + 0] = 0.5 * 0.5;
					covariance[6 * 1 + 1] = 0.5 * 0.5;
					covariance[6 * 5 + 5] = (float) (Math.PI / 12.0 * Math.PI / 12.0);

					initialPosePublisher.publish(initialPose);
					break;
				case GOAL_MODE:
					poseStamped = pose.toPoseStampedMessage(
							GraphName.of("/base_link"),
							connectedNode.getCurrentTime(),
							androidGoalPublisher.newMessage());
					androidGoalPublisher.publish(poseStamped);

					move_base_msgs.MoveBaseActionGoal message = goalPublisher
							.newMessage();
					message.setHeader(poseStamped.getHeader());
					message.getGoalId()
							.setStamp(connectedNode.getCurrentTime());
					message.getGoalId()
							.setId("/move_base/move_base_client_android"
									+ connectedNode.getCurrentTime().toString());
					message.getGoal().setTargetPose(poseStamped);
					goalPublisher.publish(message);
					break;
				}
				visible = false;
				return true;
			}
		}
		gestureDetector.onTouchEvent(event);
		return false;
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler,
			FrameTransformTree frameTransformTree, final Camera camera) {
		this.connectedNode = connectedNode;
		this.camera = camera;
		shape = new PoseShape(camera);
		mode = POSE_MODE;
		initialPosePublisher = connectedNode.newPublisher("/initialpose",
				"geometry_msgs/PoseWithCovarianceStamped");
		androidGoalPublisher = connectedNode.newPublisher("/android/goal",
				"geometry_msgs/PoseStamped");
		goalPublisher = connectedNode.newPublisher("/move_base/goal",
				"move_base_msgs/MoveBaseActionGoal");
		handler.post(new Runnable() {
			@Override
			public void run() {
				gestureDetector = new GestureDetector(context,
						new GestureDetector.SimpleOnGestureListener() {
							@Override
							public void onLongPress(MotionEvent e) {
								pose = Transform.translation(camera
										.toMetricCoordinates((int) e.getX(),
												(int) e.getY()));
								shape.setTransform(pose);
								camera.setFrame("/map");
								fixedPose = Transform.translation(camera
										.toMetricCoordinates((int) e.getX(),
												(int) e.getY()));
								camera.setFrame("/base_link");
								visible = true;
							}
						});
			}
		});
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		initialPosePublisher.shutdown();
		androidGoalPublisher.shutdown();
		goalPublisher.shutdown();
	}
}
