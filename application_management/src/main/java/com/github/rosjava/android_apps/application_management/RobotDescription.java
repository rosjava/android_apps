/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
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

import java.util.Date;
import rocon_std_msgs.Icon;

/**
 * Extends MasterDescription with robot/pairing specific attributes. Almost all the
 * previous attributes and methods of this class are now on MasterDescription.
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class RobotDescription extends MasterDescription implements java.io.Serializable {
    private static final long serialVersionUID = -6338229744294822763L;
    private String robotType;
    /** Unique id used as the signature of the remote app manager (to check that the remote controller is us) */
    private String gatewayName;

    /**
     * Empty constructor required by snake yaml parsing
     */
    public RobotDescription() {
    }

    public static RobotDescription createUnknown(MasterId masterId)  {
        return new RobotDescription(masterId, NAME_UNKNOWN, TYPE_UNKNOWN, null, NAME_UNKNOWN, new Date());
    }

    public RobotDescription(MasterId masterId, String robotName, String robotType,
                            Icon robotIcon, String gatewayName, Date timeLastSeen) {
        super(masterId, robotName, robotType, robotIcon, robotName, timeLastSeen);
        this.robotType = robotType;
        this.gatewayName = gatewayName;

        // apps namespace equals robotName on robot remocon
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public String getRobotType() { return robotType; }

    public void setRobotType(String robotType) {
        this.robotType = robotType;
    }
}