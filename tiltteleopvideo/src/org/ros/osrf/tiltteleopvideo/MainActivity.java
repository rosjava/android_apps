/*
 * Copyright (C) 2011 OSRF
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.osrf.tiltteleopvideo;

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import org.ros.osrf.tiltteleopvideo.TiltJoystickView;

public class MainActivity extends RosActivity {

  private RosImageView<sensor_msgs.CompressedImage> image;
  private TiltJoystickView joystick;

  public MainActivity() {
    super("TiltTeleopVideo", "TiltTeleopVideo");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    image = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
    image.setTopicName("camera/image_raw/compressed");
    image.setMessageType(sensor_msgs.CompressedImage._TYPE);
    image.setMessageToBitmapCallable(new BitmapFromCompressedImage());
    joystick = (TiltJoystickView) findViewById(R.id.joystick);
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeMainExecutor.execute(image, nodeConfiguration.setNodeName("android/video_view"));
    nodeMainExecutor.execute(joystick, nodeConfiguration.setNodeName("android/joystick_view"));
  }
}

