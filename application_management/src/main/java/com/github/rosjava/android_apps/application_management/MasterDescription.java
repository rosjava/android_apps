/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2013, Yujin Robot.
 *
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

import rocon_std_msgs.Icon;

/**
 * Mostly a clone of RobotDescription but generic enough to work also for concert apps.
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class MasterDescription implements java.io.Serializable {
    // Unique identifier for keys passed between android apps.
    public static final String UNIQUE_KEY = "com.github.rosjava.android_apps.application_management.MasterDescription";
    private static final long serialVersionUID = 1L;

    public static final String CONNECTING = "connecting...";
    public static final String OK = "ok";
    public static final String ERROR = "exception";
    public static final String WIFI = "invalid wifi";
    public static final String UNAVAILABLE = "unavailable";  // when the master app manager is busy serving another remote controller
    public static final String CONTROL = "not started";
    public static final String NAME_UNKNOWN = "Unknown";
    public static final String TYPE_UNKNOWN = "Unknown";

    private MasterId masterId;
    private String masterName;
    private String masterType; // TODO this contains robot type, but in concerts don't make a lot of sense; move to RobotDescription?
    // TODO same for gateway, no sense in concerts
    private String gatewayName; // unique id used as the signature of the remote app manager (used to check that the remote controller is us).
    // Icon stored piecemeal because msg arrays (stored as jboss ChannelBuffers) can't
    // be dumped and reloaded by the snakeyaml library.
    private String masterIconFormat;
    private byte[] masterIconData;
    private int masterIconDataOffset;
    private int masterIconDataLength;

    private String connectionStatus;
    private Date timeLastSeen;

    // TODO(kwc): add in canonicalization of masterName
    public MasterDescription() {
    }

    public MasterDescription(MasterId masterId, String masterName, String masterType,
                             Icon masterIcon, String gatewayName, Date timeLastSeen) {
        setMasterName(masterName);
        setMasterId(masterId);
        this.masterName = masterName;
        this.masterType = masterType;
        this.gatewayName = gatewayName;
        if (masterIcon != null) {
            this.masterIconFormat = masterIcon.getFormat();
            this.masterIconData = masterIcon.getData().array();
            this.masterIconDataOffset = masterIcon.getData().arrayOffset();
            this.masterIconDataLength = masterIcon.getData().readableBytes();
        }
        this.timeLastSeen = timeLastSeen;
    }

    public void copyFrom(MasterDescription other) {
        masterId = other.masterId;
        masterName = other.masterName;
        masterType = other.masterType;
        gatewayName = other.gatewayName;
        masterIconFormat = other.masterIconFormat;
        masterIconData = other.masterIconData;
        masterIconDataOffset = other.masterIconDataOffset;
        masterIconDataLength = other.masterIconDataLength;
        connectionStatus = other.connectionStatus;
        timeLastSeen = other.timeLastSeen;
    }

    public MasterId getMasterId() {
        return masterId;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    /**
     * Convenience accessor to dig into the master uri for this master.
     *
     * @return String : the ros master uri for this master.
     */
    public String getMasterUri() {
        return masterId.getMasterUri();
    }

    public void setMasterId(MasterId masterId) {
        // TODO: ensure the master id is sane.
//              if(false) {
//                      throw new InvalidMasterDescriptionException("Empty Master URI");
//              }
        // TODO: validate
        this.masterId = masterId;
    }

    public String getMasterName() {
        return masterName;
    }

    /**
     * Provide a human-friendly interpretation of the master name
     * <p/>
     * Often we use master names with a uuid suffix to ensure the master name in a
     * multi-master group is unique. This checks for the suffix and if found, strips it.
     * <p/>
     * It also converts '_'s to spaces and first character of each word to uppercase.
     *
     * @return human friendly string name
     */
    public String getMasterFriendlyName() {
        String friendlyName = masterName;
        // The uuid is a 16 byte hash in hex format = 32 characters
        if (masterName.length() > 32) {
            String possibleUuidPart = masterName.substring(masterName.length() - 32);
            Pattern p = Pattern.compile("[^a-f0-9]");
            Matcher m = p.matcher(possibleUuidPart);
            if (!m.find()) {
                friendlyName = masterName.substring(0, masterName.length() - 32);
            }
        }
        friendlyName = friendlyName.replace('_', ' ');
        final StringBuilder result = new StringBuilder(friendlyName.length());
        String[] words = friendlyName.split("\\s");
        for (int i = 0, l = words.length; i < l; ++i) {
            if (i > 0) result.append(" ");
            result.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1));
        }
        return result.toString();
    }

    public void setMasterName(String masterName) {
        // TODO: GraphName validation was removed. What replaced it?
        // if (!GraphName.validate(masterName)) {
        // throw new InvalidMasterDescriptionException("Bad master name: " +
        // masterName);
        // }
        this.masterName = masterName;
    }

    public String getMasterType() {
        return masterType;
    }

    public void setMasterType(String masterType) {
        this.masterType = masterType;
    }

    public String getMasterIconFormat() {
        return masterIconFormat;
    }

    public ChannelBuffer getMasterIconData() {
        if (masterIconData == null) {
            return null;
        } else {
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(masterIconData, masterIconDataOffset, masterIconDataLength);
            return channelBuffer;
        }
    }

    public void setMasterIconFormat(String iconFormat) {
        this.masterIconFormat = iconFormat;
    }

    public void setMasterIconData(ChannelBuffer iconData) {
        this.masterIconData = iconData.array();
    }

    public void setMasterIcon(Icon masterIcon) {
        this.masterIconFormat = masterIcon.getFormat();
        this.masterIconData = masterIcon.getData().array();
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
        return this.masterName.equals(NAME_UNKNOWN);
    }

    public static MasterDescription createUnknown(MasterId masterId) {
        return new MasterDescription(masterId, NAME_UNKNOWN, TYPE_UNKNOWN, null, NAME_UNKNOWN, new Date());
    }

    @Override
    public boolean equals(Object o) {
        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }
        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's
        // specification.
        if (!(o instanceof MasterDescription)) {
            return false;
        }
        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access
        // private fields.
        MasterDescription lhs = (MasterDescription) o;
        // Check each field. Primitive fields, reference fields, and nullable
        // reference
        // fields are all treated differently.
        return (masterId == null ? lhs.masterId == null : masterId.equals(lhs.masterId));
    }

    // I need to override equals() so I'm also overriding hashCode() to match.
    @Override
    public int hashCode() {
        // Start with a non-zero constant.
        int result = 17;
        // Include a hash for each field checked by equals().
        result = 31 * result + (masterId == null ? 0 : masterId.hashCode());
        return result;
    }
}