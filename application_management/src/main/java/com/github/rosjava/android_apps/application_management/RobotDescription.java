/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_apps.application_management;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rocon_app_manager_msgs.Icon;

public class RobotDescription implements java.io.Serializable {
    public static final String CONNECTING = "connecting...";
    public static final String OK = "ok";
    public static final String ERROR = "exception";
    public static final String WIFI = "invalid wifi";
    public static final String UNAVAILABLE = "unavailable";  // when the robot app manager is busy serving another remote controller
    public static final String CONTROL = "not started";
    private static final String NAME_UNKNOWN = "Unknown";
    private static final String TYPE_UNKNOWN = "Unknown";
    private static final long serialVersionUID = 1L;
    private RobotId robotId;
    private String robotName;
    private String robotType;
    private String gatewayName; // unique id used as the signature of the remote app manager (used to check that the remote controller is us).
    // Icon stored piecemeal because msg arrays (stored as jboss ChannelBuffers) can't
    // be dumped and reloaded by the snakeyaml library.
    private String robotIconFormat;
    private byte[] robotIconData;
    private int robotIconDataOffset;
    private int robotIconDataLength;

    private String connectionStatus;
    private Date timeLastSeen;
    // Unique identifier for keys passed between android apps.
    public static final String UNIQUE_KEY = "com.github.rosjava.android_apps.application_management.RobotDescription";

    // TODO(kwc): add in canonicalization of robotName
    public RobotDescription() {
    }

    public RobotDescription(RobotId robotId, String robotName, String robotType, Icon robotIcon, String gatewayName, Date timeLastSeen) {
            setRobotName(robotName);
            setRobotId(robotId);
            this.robotName = robotName;
            this.robotType = robotType;
            this.gatewayName = gatewayName;
            if ( robotIcon != null ) {
                this.robotIconFormat = robotIcon.getFormat();
                this.robotIconData = robotIcon.getData().array();
                this.robotIconDataOffset = robotIcon.getData().arrayOffset();
                this.robotIconDataLength = robotIcon.getData().readableBytes();
            }
            this.timeLastSeen = timeLastSeen;
    }
    public void copyFrom(RobotDescription other) {
            robotId = other.robotId;
            robotName = other.robotName;
            robotType = other.robotType;
            gatewayName = other.gatewayName;
            robotIconFormat = other.robotIconFormat;
            robotIconData = other.robotIconData;
            robotIconDataOffset = other.robotIconDataOffset;
            robotIconDataLength = other.robotIconDataLength;
            connectionStatus = other.connectionStatus;
            timeLastSeen = other.timeLastSeen;
    }
    public RobotId getRobotId() {
            return robotId;
    }

    public String getGatewayName() { return gatewayName; }

    /**
     * Convenience accessor to dig into the master uri for this robot.
     * @return String : the ros master uri for this robot.
     */
    public String getMasterUri() {
        return robotId.getMasterUri();
    }

    public void setRobotId(RobotId robotId) {
            // TODO: ensure the robot id is sane.
//              if(false) {
//                      throw new InvalidRobotDescriptionException("Empty Master URI");
//              }
            // TODO: validate
            this.robotId = robotId;
    }
    public String getRobotName() {
            return robotName;
    }
    /** Provide a human-friendly interpretation of the robot name
     *
     * Often we use robot names with a uuid suffix to ensure the robot
     * name in a multi-robot group is unique. This checks for the
     * suffix and if found, strips it.
     *
     * It also converts '_'s to spaces and first character of each word
     * to uppercase.
     *
     * @return human friendly string name
     */
    public String getRobotFriendlyName() {
        String friendlyName = robotName;
        // The uuid is a 16 byte hash in hex format = 32 characters
        if (robotName.length() > 32) {
            String possibleUuidPart = robotName.substring(robotName.length() - 32);
            Pattern p = Pattern.compile("[^a-f0-9]");
            Matcher m = p.matcher(possibleUuidPart);
            if (!m.find()) {
                friendlyName = robotName.substring(0, robotName.length() - 32);
            }
        }
        friendlyName = friendlyName.replace('_',' ');
        final StringBuilder result = new StringBuilder(friendlyName.length());
        String[] words = friendlyName.split("\\s");
        for(int i=0, l=words.length; i<l; ++i) {
          if(i > 0) result.append(" ");
          result.append(Character.toUpperCase(words[i].charAt(0)))
                .append(words[i].substring(1));
        }
        return result.toString();
    }

    public void setRobotName(String robotName) {
            // TODO: GraphName validation was removed. What replaced it?
            // if (!GraphName.validate(robotName)) {
            // throw new InvalidRobotDescriptionException("Bad robot name: " +
            // robotName);
            // }
            this.robotName = robotName;
    }
    public String getRobotType() {
            return robotType;
    }
    public void setRobotType(String robotType) {
            this.robotType = robotType;
    }

    public String getRobotIconFormat() {
        return robotIconFormat;
    }

    public ChannelBuffer getRobotIconData() {
        if ( robotIconData == null ) {
            return null;
        } else {
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(robotIconData, robotIconDataOffset, robotIconDataLength);
            return channelBuffer;
        }
    }

    public void setRobotIcon(Icon robotIcon) {
        this.robotIconFormat = robotIcon.getFormat();
        this.robotIconData = robotIcon.getData().array();
    }

    public String getConnectionStatus() {
            return connectionStatus;
    }
    public void setConnectionStatus(String connectionStatus) {
            this.connectionStatus = connectionStatus;
    }
    public Date getTimeLastSeen() {
            return timeLastSeen;
    }
    public void setTimeLastSeen(Date timeLastSeen) {
            this.timeLastSeen = timeLastSeen;
    }
    public boolean isUnknown() {
            return this.robotName.equals(NAME_UNKNOWN);
    }
    public static RobotDescription createUnknown(RobotId robotId)  {
            return new RobotDescription(robotId, NAME_UNKNOWN, TYPE_UNKNOWN, null, NAME_UNKNOWN, new Date());
    }
    @Override
    public boolean equals(Object o) {
            // Return true if the objects are identical.
            // (This is just an optimization, not required for correctness.)
            if(this == o) {
                    return true;
            }
            // Return false if the other object has the wrong type.
            // This type may be an interface depending on the interface's
            // specification.
            if(!(o instanceof RobotDescription)) {
                    return false;
            }
            // Cast to the appropriate type.
            // This will succeed because of the instanceof, and lets us access
            // private fields.
            RobotDescription lhs = (RobotDescription) o;
            // Check each field. Primitive fields, reference fields, and nullable
            // reference
            // fields are all treated differently.
            return (robotId == null ? lhs.robotId == null : robotId.equals(lhs.robotId));
    }
    // I need to override equals() so I'm also overriding hashCode() to match.
    @Override
    public int hashCode() {
            // Start with a non-zero constant.
            int result = 17;
            // Include a hash for each field checked by equals().
            result = 31 * result + (robotId == null ? 0 : robotId.hashCode());
            return result;
    }
}