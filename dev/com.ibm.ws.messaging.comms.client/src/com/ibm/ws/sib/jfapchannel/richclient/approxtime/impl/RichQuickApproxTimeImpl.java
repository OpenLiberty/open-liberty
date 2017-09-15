/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.approxtime.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Rich client implementation of QuickApproxTime which uses the real TCP channel QuickApproxTime
 * under the covers to get an approximate time.
 */
public class RichQuickApproxTimeImpl implements QuickApproxTime {
  private static final TraceComponent tc = SibTr.register(RichQuickApproxTimeImpl.class, JFapChannelConstants.MSG_GROUP,  JFapChannelConstants.MSG_BUNDLE);

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.4 SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/approxtime/impl/RichQuickApproxTimeImpl.java, SIB.comms, WASX.SIB, uu1215.01 08/05/21 05:29:28 [4/12/12 22:14:18]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

   /**
   * @see com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime#getApproxTime()
   */
  public long getApproxTime() {
    // For performance reasons there is no trace entry statement here
    // For performance reasons there is no trace exit statement here
      return com.ibm.wsspi.timer.QuickApproxTime.getApproxTime();
  }

  /**
   * @see com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime#setInterval(long)
   */
  public void setInterval(long interval) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setInterval", Long.valueOf(interval));
    //Venu Liberty COMMS .. TODO
    //wasQuickApproxTime.setInterval(interval);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setInterval");
  }
}
