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

public class ConcertDescription implements java.io.Serializable {
    public static final String CONNECTING = "connecting...";
    public static final String OK = "ok";
    public static final String ERROR = "exception";
    public static final String WIFI = "invalid wifi";
    public static final String UNAVAILABLE = "unavailable";  // when the concert app manager is busy serving another remote controller
    public static final String CONTROL = "not started";
    private static final String NAME_UNKNOWN = "Unknown";
    private static final String TYPE_UNKNOWN = "Unknown";
    private static final long serialVersionUID = 1L;
    private MasterId masterId;
    private String concertName;
    private String description;
    private String[] userRoles;
    private int currentRole = -1;
    // Icon stored piecemeal because msg arrays (stored as jboss ChannelBuffers) can't
    // be dumped and reloaded by the snakeyaml library.
    private String concertIconFormat;
    private byte[] concertIconData;
    private int concertIconDataOffset;
    private int concertIconDataLength;

    private String connectionStatus;
    private Date timeLastSeen;
    // Unique identifier for keys passed between android apps.
    public static final String UNIQUE_KEY = "com.github.rosjava.android_apps.application_management.ConcertDescription";

    // TODO(kwc): add in canonicalization of concertName
    public ConcertDescription() {
    }

    public ConcertDescription(MasterId masterId, String concertName, String description,
                              rocon_std_msgs.Icon concertIcon, Date timeLastSeen) {
            setConcertName(concertName);
            setDescription(description);
            setMasterId(masterId);
            this.concertName = concertName;
            if ( concertIcon != null ) {
                this.concertIconFormat = concertIcon.getFormat();
                this.concertIconData = concertIcon.getData().array();
                this.concertIconDataOffset = concertIcon.getData().arrayOffset();
                this.concertIconDataLength = concertIcon.getData().readableBytes();
            }
            this.timeLastSeen = timeLastSeen;
    }
    public void copyFrom(ConcertDescription other) {
            masterId = other.masterId;
            userRoles = other.userRoles.clone();
            concertName = other.concertName;
            description = other.description;
            concertIconFormat = other.concertIconFormat;
            concertIconData = other.concertIconData;
            concertIconDataOffset = other.concertIconDataOffset;
            concertIconDataLength = other.concertIconDataLength;
            connectionStatus = other.connectionStatus;
            timeLastSeen = other.timeLastSeen;
    }
    public MasterId getMasterId() { return masterId; }
    public String getDescription()  { return description; }
    public String getConcertName()  { return concertName; }
    public String[] getUserRoles()  { return userRoles;}
    public String getCurrentRole()  {
        if (userRoles != null && currentRole >= 0 && currentRole <  userRoles.length)
            return userRoles[currentRole];
        else
            return null;
    }

    /**
     * Convenience accessor to dig into the master uri for this concert.
     * @return String : the ros master uri for this concert.
     */
    public String getMasterUri() {
        return masterId.getMasterUri();
    }

    public void setMasterId(MasterId masterId) {
            // TODO: ensure the concert id is sane.
//              if(false) {
//                      throw new InvalidConcertDescriptionException("Empty Master URI");
//              }
            // TODO: validate
            this.masterId = masterId;
    }
    public void setUserRoles(concert_msgs.Roles roles)
    {
        java.util.List<String> tmp = roles.getList();
        userRoles = new String[tmp.size()];
        tmp.toArray(userRoles);
    }
    public void setCurrentRole(int role) { currentRole = role; }

    /** Provide a human-friendly interpretation of the concert name
     *
     * Often we use concert names with a uuid suffix to ensure the concert
     * name in a multi-concert group is unique. This checks for the
     * suffix and if found, strips it.
     *
     * It also converts '_'s to spaces and first character of each word
     * to uppercase.
     *
     * @return human friendly string name
     */
    public String getConcertFriendlyName() {
        String friendlyName = concertName;
        // The uuid is a 16 byte hash in hex format = 32 characters
        if (concertName.length() > 32) {
            String possibleUuidPart = concertName.substring(concertName.length() - 32);
            Pattern p = Pattern.compile("[^a-f0-9]");
            Matcher m = p.matcher(possibleUuidPart);
            if (!m.find()) {
                friendlyName = concertName.substring(0, concertName.length() - 32);
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

    public void setConcertName(String concertName) { this.concertName = concertName; }
    public void setDescription(String description) { this.description = description; }

    public String getConcertIconFormat() {
        return concertIconFormat;
    }

    public ChannelBuffer getConcertIconData() {
        if ( concertIconData == null ) {
            return null;
        } else {
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(concertIconData, concertIconDataOffset, concertIconDataLength);
            return channelBuffer;
        }
    }

    public void setConcertIcon(rocon_std_msgs.Icon concertIcon) {
        this.concertIconFormat = concertIcon.getFormat();
        this.concertIconData = concertIcon.getData().array();
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
            return this.concertName.equals(NAME_UNKNOWN);
    }
    public static ConcertDescription createUnknown(MasterId masterId)  {
            return new ConcertDescription(masterId, NAME_UNKNOWN, TYPE_UNKNOWN, null, new Date());
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
            if(!(o instanceof ConcertDescription)) {
                    return false;
            }
            // Cast to the appropriate type.
            // This will succeed because of the instanceof, and lets us access private fields.
            ConcertDescription lhs = (ConcertDescription) o;
            // Check each field. Primitive fields, reference fields, and nullable reference
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