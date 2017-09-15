/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.trm.status;

import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * This class contains general status information. The only field that will
 * definitely not be null is the remoteEngineName all other field may be
 * null or 0 depending on the exact status of the communications session.
 */

public class Status {

  private String   remoteEngineName;
  private SIBUuid8 remoteEngineUuid;

  private String status;

  public final static String STATUS_STARTED_OK  = "Started ok";
  public final static String STATUS_STARTED_NOK = "Started nok";
  public final static String STATUS_STOPPED     = "Stopped";

  private Object meConnection;    // Don't use real MEConnection to avoid
                                  // circular build dependency
  private long timeStarted;       // Milli seconds (if status started)
  private long timeStopped;       // Milli seconds (if status stopped)
  private long timeFailed;        // Milli seconds (if status failed)

  // Setter methods

  public void setRemoteEngineName (String n) {
    remoteEngineName = n;
  }

  public void setRemoteEngineUuid (SIBUuid8 u) {
    remoteEngineUuid = u;
  }

  public void setStatus (String s) {
    if (!s.equals(STATUS_STARTED_OK) && !s.equals(STATUS_STARTED_NOK) && !s.equals(STATUS_STOPPED)) {
      throw new IllegalArgumentException("Valid values:"+STATUS_STARTED_OK+","+STATUS_STARTED_NOK+","+STATUS_STOPPED);
    }

    status = s;
  }

  public void setMEConnection (Object o) {
    meConnection = o;
  }

  public void setTimeStarted (long t) {
    timeStarted = t;
  }

  public void setTimeStopped (long t) {
    timeStopped = t;
  }

  public void setTimeFailed (long t) {
    timeFailed = t;
  }

  // Getter methods

  public String getRemoteEngineName () {
    return remoteEngineName;
  }

  public SIBUuid8 getRemoteEngineUuid () {
    return remoteEngineUuid;
  }

  public String getStatus () {
    return status;
  }

  public Object getMEConnection () {
    return meConnection;
  }

  public long getTimeStarted () {
    return timeStarted;
  }

  public long getTimeStopped () {
    return timeStopped;
  }

  public long getTimeFailed () {
    return timeFailed;
  }

  // Utility methods

  public String toString () {
    return "remoteEngineName="+remoteEngineName+",remoteEngineUuid="+remoteEngineUuid+",status="+status+",meConnection="+meConnection+",timeStarted="+timeStarted+",timeStopped="+timeStopped+",timeFailed="+timeFailed;
  }

}
