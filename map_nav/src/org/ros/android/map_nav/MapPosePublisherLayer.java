package org.ros.android.map_nav;

import geometry_msgs.PoseStamped;

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
	  private Publisher<geometry_msgs.PoseStamped> initialPosePublisher;
	  private Publisher<geometry_msgs.PoseStamped> androidGoalPublisher;
	  private Publisher<move_base_msgs.MoveBaseActionGoal> goalPublisher;
	  private PoseStamped  initialPoseStamped = null;
	  private boolean visible;
	  private GraphName topic;
	  private GestureDetector gestureDetector;
	  private Transform pose;
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
	      if (event.getAction() == MotionEvent.ACTION_MOVE) {
	        Vector3 poseVector = pose.apply(Vector3.zero());
	        Vector3 pointerVector = camera.toMetricCoordinates((int) event.getX(), (int) event.getY());
	        double angle =
	            angle(pointerVector.getX(), pointerVector.getY(), poseVector.getX(), poseVector.getY());
	        pose = Transform.translation(poseVector).multiply(Transform.zRotation(angle));
	        shape.setTransform(pose);
	        return true;
	      }
	      if (event.getAction() == MotionEvent.ACTION_UP) {

	    	  PoseStamped poseStamped = pose.toPoseStampedMessage(GraphName.of("/base_link"),
	  	            connectedNode.getCurrentTime(), initialPosePublisher.newMessage());
	    	  switch(mode) {
	    	case POSE_MODE:
	    		initialPosePublisher.publish(poseStamped);
	    		//initialPoseStamped = initialPosePublisher.newMessage();
	    		//initialPoseStamped.setHeader(poseStamped.getHeader());
	    		//initialPoseStamped.setPose(poseStamped.getPose());
	    		break;
	    	case GOAL_MODE:
	    		/*if(initialPoseStamped != null) {
	    			double poseX = poseStamped.getPose().getPosition().getX() + initialPoseStamped.getPose().getPosition().getX();
	    			double poseY = poseStamped.getPose().getPosition().getY() + initialPoseStamped.getPose().getPosition().getY();
	    			double poseZ = poseStamped.getPose().getPosition().getZ() + initialPoseStamped.getPose().getPosition().getZ();
	    			double px = poseStamped.getPose().getOrientation().getX();
	    			double py = poseStamped.getPose().getOrientation().getY();
	    			double pz = poseStamped.getPose().getOrientation().getZ();
	    			double pw = poseStamped.getPose().getOrientation().getW();
	    			double ix = initialPoseStamped.getPose().getOrientation().getX();
	    			double iy = initialPoseStamped.getPose().getOrientation().getY();
	    			double iz = initialPoseStamped.getPose().getOrientation().getZ();
	    			double iw = initialPoseStamped.getPose().getOrientation().getW();

	    			double orientationX = pw * ix + px * iw + py * iz - pz * iy;
	    			double orientationY = pw * iy - px * iz + py * iw + pz * ix;
	    			double orientationZ = pw * iz + px * iy - py * ix + pz * iw;
	    			double orientationW = pw * iw - px * ix - py * iy - pz * iz;
	    			
	    			poseStamped.getPose().getPosition().setX(poseX);
	    			poseStamped.getPose().getPosition().setY(poseY);
	    			poseStamped.getPose().getPosition().setZ(poseZ);
	    			poseStamped.getPose().getOrientation().setX(orientationX);
	    			poseStamped.getPose().getOrientation().setY(orientationY);
	    			poseStamped.getPose().getOrientation().setZ(orientationZ);
	    			poseStamped.getPose().getOrientation().setW(orientationW);
	    		}*/
	    		
		    	androidGoalPublisher.publish(poseStamped);
		        move_base_msgs.MoveBaseActionGoal message = goalPublisher.newMessage();
		        message.setHeader(poseStamped.getHeader());
		        message.getGoalId().setStamp(connectedNode.getCurrentTime());
		        message.getGoalId().setId("/move_base/move_base_client_android"+connectedNode.getCurrentTime().toString());
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
	    initialPosePublisher = connectedNode.newPublisher("/initialpose", "geometry_msgs/PoseStamped");
	    androidGoalPublisher = connectedNode.newPublisher("/android/goal", "geometry_msgs/PoseStamped");
	    goalPublisher = connectedNode.newPublisher("/move_base/goal", "move_base_msgs/MoveBaseActionGoal");
	    handler.post(new Runnable() {
	      @Override
	      public void run() {
	        gestureDetector =
	            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
	              @Override
	              public void onLongPress(MotionEvent e) {
	                pose =
	                    Transform.translation(camera.toMetricCoordinates((int) e.getX(), (int) e.getY()));
	                shape.setTransform(pose);
	                visible = true;
	              }
	            });
	      }
	    });
	  }
	  


	  @Override
	  public void onShutdown(VisualizationView view, Node node) {
	    initialPosePublisher.shutdown();
	    goalPublisher.shutdown();
	  }
	}
