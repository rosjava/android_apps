package com.github.rosjava.android_apps.map_nav;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.github.rosjava.android_remocons.common_tools.apps.AppParameters;
import com.github.rosjava.android_remocons.common_tools.apps.AppRemappings;
import com.google.common.base.Preconditions;

import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.DefaultLayer;
import org.ros.android.view.visualization.shape.PixelSpacePoseShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;
import move_base_msgs.MoveBaseActionGoal;

//import com.github.rosjava.android_apps.application_management.rapp_manager.AppParameters;
//import com.github.rosjava.android_apps.application_management.rapp_manager.AppRemappings;

public class MapPosePublisherLayer extends DefaultLayer {

	private Shape shape;
	private Publisher<geometry_msgs.PoseWithCovarianceStamped> initialPosePublisher;
	private Publisher<geometry_msgs.PoseStamped> androidGoalPublisher;
	private Publisher<MoveBaseActionGoal> goalPublisher;
	private boolean visible;
	private NameResolver nameResolver;
	private GestureDetector gestureDetector;
	private Transform pose;
	private Transform fixedPose;
	private ConnectedNode connectedNode;
	private int mode;
	private static final int POSE_MODE = 0;
	private static final int GOAL_MODE = 1;

    private String mapFrame;
    private String robotFrame;
    private String initialPoseTopic;
    private String simpleGoalTopic;
    private String moveBaseGoalTopic;

	public MapPosePublisherLayer(final Context context,
                                 final NameResolver newNameResolver,
                                 final AppParameters params, final AppRemappings remaps) {
		this.nameResolver = newNameResolver;
		visible = false;

        this.mapFrame = (String) params.get("map_frame",context.getString(R.string.map_frame));
        this.robotFrame = (String) params.get("robot_frame", context.getString(R.string.robot_frame));

        this.initialPoseTopic = remaps.get(context.getString(R.string.initial_pose_topic));
        this.simpleGoalTopic = remaps.get(context.getString(R.string.simple_goal_topic));
        this.moveBaseGoalTopic = remaps.get(context.getString(R.string.move_base_goal_topic));

    }

	public void setPoseMode() {
		mode = POSE_MODE;
	}

	public void setGoalMode() {
		mode = GOAL_MODE;
	}

	@Override
	public void draw(VisualizationView view, GL10 gl) {
		if (visible) {
			Preconditions.checkNotNull(pose);
			shape.draw(view, gl);
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
				pointerVector = view.getCamera().toCameraFrame((int) event.getX(),
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
                    view.getCamera().setFrame(mapFrame);
					poseVector = fixedPose.apply(Vector3.zero());
					pointerVector = view.getCamera().toCameraFrame(
							(int) event.getX(), (int) event.getY());
					double angle2 = angle(pointerVector.getX(),
							pointerVector.getY(), poseVector.getX(),
							poseVector.getY());
					fixedPose = Transform.translation(poseVector).multiply(
							Transform.zRotation(angle2));
					view.getCamera().setFrame(robotFrame);
					poseStamped = fixedPose.toPoseStampedMessage(
							GraphName.of(robotFrame),
							connectedNode.getCurrentTime(),
							androidGoalPublisher.newMessage());

					PoseWithCovarianceStamped initialPose = initialPosePublisher.newMessage();
					initialPose.getHeader().setFrameId(mapFrame);
					initialPose.getPose().setPose(poseStamped.getPose());
					double[] covariance = initialPose.getPose().getCovariance();
					covariance[6 * 0 + 0] = 0.5 * 0.5;
					covariance[6 * 1 + 1] = 0.5 * 0.5;
					covariance[6 * 5 + 5] = (float) (Math.PI / 12.0 * Math.PI / 12.0);

					initialPosePublisher.publish(initialPose);
					break;
				case GOAL_MODE:
					poseStamped = pose.toPoseStampedMessage(
							GraphName.of(robotFrame),
							connectedNode.getCurrentTime(),
							androidGoalPublisher.newMessage());
					androidGoalPublisher.publish(poseStamped);

					move_base_msgs.MoveBaseActionGoal message = goalPublisher.newMessage();
					message.setHeader(poseStamped.getHeader());
					message.getGoalId().setStamp(connectedNode.getCurrentTime());
					message.getGoalId().setId("move_base/move_base_client_android"
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
	public void onStart(final VisualizationView view, ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		shape = new PixelSpacePoseShape();
		mode = GOAL_MODE;

		initialPosePublisher = connectedNode.newPublisher(nameResolver.resolve(initialPoseTopic).toString(),
				                                          "geometry_msgs/PoseWithCovarianceStamped");
		androidGoalPublisher = connectedNode.newPublisher(nameResolver.resolve(simpleGoalTopic).toString(),
				                                          "geometry_msgs/PoseStamped");
		goalPublisher = connectedNode.newPublisher(nameResolver.resolve(moveBaseGoalTopic).toString(),
				                                   "move_base_msgs/MoveBaseActionGoal");
		view.post(new Runnable() {
			@Override
			public void run() {
				gestureDetector = new GestureDetector(view.getContext(),
						new GestureDetector.SimpleOnGestureListener() {
							@Override
							public void onLongPress(MotionEvent e) {
								pose = Transform.translation(view.getCamera().toCameraFrame(
                                        (int) e.getX(), (int) e.getY()));
								shape.setTransform(pose);
								view.getCamera().setFrame(mapFrame);
								fixedPose = Transform.translation(view.getCamera().toCameraFrame(
                                        (int) e.getX(),	(int) e.getY()));
                                view.getCamera().setFrame(robotFrame);
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
