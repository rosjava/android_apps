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

package org.ros.osrf.@APPNAME_LOWER@;

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.android.MessageCallable;

import org.ros.android.view.RosTextView;

public class MainActivity extends RosActivity {

  private RosTextView<std_msgs.String> text;

  public String toString(std_msgs.String msg) {
      return msg.getData();
  }

  public MainActivity() {
    super("@APPNAME_CAMEL@", "@APPNAME_CAMEL@");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    text = (RosTextView<std_msgs.String>) findViewById(R.id.text);
    text.setTopicName("chatter");
    text.setMessageType(std_msgs.String._TYPE);
    text.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
      @Override
      public String call(std_msgs.String message) {
        return message.getData();
      }
    });
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeMainExecutor.execute(text, nodeConfiguration.setNodeName("android/listener"));
  }
}
