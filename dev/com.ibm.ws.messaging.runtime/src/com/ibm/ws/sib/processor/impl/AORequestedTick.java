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

package com.ibm.ws.sib.processor.impl;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.processor.utils.linkedlist.Entry;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
/**
 * A Requested Tick waiting for an assignment. Associated with an JSRemoteConsumerPoint.
 * The tick starts of in the state (!satisfied and !expired). There are 2 possible transitions
 * from this state into two ending states: (!satisfied and expired), (satisfied and !expired).
 * The expired transition will result in the AOStream containing this tick to transition this tick to
 * the completed state. The satisfied transition will result in the AOStream containing this tick
 * to transition this tick to the value state.
 *
 * SYNCHRONIZATION: No method in this class is synchronized. Since two different threads can compete
 * with making these 2 transitions concurrently, using the expire() and satisfy() methods respectively,
 * the parent (JSRemoteConsumerPoint) is responsible for synchronization.
 */
public final class AORequestedTick extends Entry implements AlarmListener
{
  
  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);
  private static TraceComponent tc =
  SibTr.register(
    AORequestedTick.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);


  private final JSRemoteConsumerPoint parent;
  public final long tick; // the tick in the stream
  public final Long objTick; // the tick as a Long object
  public final long timeout; // the time taken for this request to expire
  public final long requestTime; // the time that the tick was requested
  private Alarm expiryHandle; // the expiry handle for the alarm

  private boolean satisfied;
  private SIMPMessage msg; // satisfied implies msg!=null

  private boolean expired;

  /**
   * Constructor
   * @param parent The containing JSRemoteConsumerPoint
   * @param tick The tick position in the stream
   * @param expiryTimeout The time period in milliseconds, after which this tick should expire
   * @param listEntry The entry of this in the parent's linked list
   */
  public AORequestedTick(JSRemoteConsumerPoint parent, long tick, Long objTick,
      long expiryTimeout, MPAlarmManager am)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AORequestedTick",
        new Object[]{parent, Long.valueOf(tick), objTick, Long.valueOf(expiryTimeout), am});

    this.parent = parent;
    this.tick = tick;
    this.objTick = objTick;
    this.timeout = expiryTimeout;
    this.requestTime = System.currentTimeMillis();
    if ((expiryTimeout != SIMPConstants.INFINITE_TIMEOUT) && (expiryTimeout > 0L))
      this.expiryHandle = am.create(expiryTimeout, this);
    else
    {
      // for INFINITE_TIMEOUT we don't do any expiry processing. For 0 timeout, the JSRemoteConsumerPoint does the expiry
      // without using the MPAlarmManager.
      this.expiryHandle = null;
    }

    this.satisfied = false;
    this.expired = false;
    this.msg = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AORequestedTick", this);

  }

  /**
   * The get request has expired. Will tell the parent to expire this request (i.e. change the tick in the stream
   * to completed) only if the transition to (!satisfied and expired) is successful
   * @param thandle
   */
  public void alarm(Object thandle)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", thandle);

    parent.expiryAlarm(this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");
  }

  /**
   * Try to transition to (!satisfied and expired) state
   * @param cancelTimer Set to true if this method should also cancel the timer
   * @return true if the transition to (!satisfied and expired) was successful
   */
  public boolean expire(boolean cancelTimer)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "expire", new Boolean(cancelTimer));

    if ((expiryHandle != null) && cancelTimer)
    {
      expiryHandle.cancel();
    }
    expiryHandle = null;
    if (!satisfied)
    {
      expired = true;
      if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
        SibTr.debug(UserTrace.tc_mt,       
           nls_mt.getFormattedMessage(
           "REMOTE_REQUEST_EXPIRED_CWSJU0033",
           new Object[] {
             tick},
           null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "expire", new Boolean(expired));

    return expired;
  }

  /**
   * Try to transition to (satisfied and !expired) state
   * @return true if the transition to (satisfied and !expired) was successful
   */
  public boolean satisfy(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "satisfy");

    if (expiryHandle != null)
      expiryHandle.cancel();
    if (!expired)
    {
      satisfied = true;
      this.msg = msg;
      
      if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
        SibTr.debug(UserTrace.tc_mt,       
           nls_mt.getFormattedMessage(
           "REMOTE_REQUEST_SATISFIED_CWSJU0032",
           new Object[] {
             Long.valueOf(tick),
             msg},
           null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "satisfy", new Boolean(satisfied));

    return satisfied;
  }

  public final SIMPMessage getMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessage");
      SibTr.exit(tc, "getMessage",msg);
    }
    return msg;
  }
}
