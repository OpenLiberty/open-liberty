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

package com.ibm.ws.sib.processor.utils.am;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleLinkedListEntry;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An alarm object based on a simple linked list entry.
 */
public class MPAlarmImpl extends SimpleLinkedListEntry implements Alarm
{
  //the latest time at which the alarm should be run
  private long latest;
  //the alarm context object
  private Object context;
  //the target time for the alarm
  private long time;
  //flag to indicate if the alarm has been canceled
  private volatile boolean active;
  //flag to indicate if the alarm's listener is a group alarm
  private boolean groupListener = false;
  
  private long index;
  
  //trace
  private static TraceComponent tc =
    SibTr.register(
      MPAlarmImpl.class,
      TraceGroups.TRGRP_UTILS,
      SIMPConstants.RESOURCE_BUNDLE);

 
  /**
   * Create a new MPAlarmImpl
   * 
   * @param time
   * @param latest
   * @param listener
   * @param context
   */
  public MPAlarmImpl(long time,
                     long latest,
                     AlarmListener listener,
                     Object context,
                     long index)
  {
    super(listener);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "MPAlarmImpl",
        new Object[] { Long.valueOf(time), Long.valueOf(latest), listener, context, Long.valueOf(index) });
        
    reset(time,latest,listener,context,index);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPAlarmImpl", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.utils.am2.MPAlarm#cancel()
   */
  public void cancel()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancel", toString());  
    
    //flag this alarm as canceled (we don't remove it from the list immediately,
    // we leave it to be picked up by the alarm thread)
    active = false;
    
    // If this is a GroupAlarmListener it supports a cancel method, that we can call
    if(groupListener && (data != null))
      ((GroupAlarmListener)data).cancel();

    // Forget about the registered listener object and context
    data = null;
    groupListener = false;
    context = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancel");
  }

  /*
   * It's possible that this is a grouped alarm and as such has a GroupAlarmListener.
   * Normally, if we cancel an alarm we cancel the whole group. In this case we only
   * want to cancel this specific alarm.
   */
  protected void cancelSingleEntry()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelSingleEntry", toString());  
    
    //flag this alarm as canceled (we don't remove it from the list immediately,
    // we leave it to be picked up by the alarm thread)
    active = false;

    // Forget about the registered listener object and context
    data = null;
    groupListener = false;
    context = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelSingleEntry");
  }

  /**
   * reset this alarm's values
   * 
   * @param time
   * @param latest
   * @param listener
   * @param context
   */
  void reset(long time,
             long latest,
             AlarmListener listener,
             Object context,
             long index)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "reset",
        new Object[] { new Long(time), new Long(latest), listener, context, new Long(index) });
    
    data = listener;
    this.time = time;
    this.latest = latest;
    this.context = context;
    this.index = index;
    //check if this alarm has a group listener
    groupListener = (listener instanceof GroupAlarmListener);
    //flag as not canceled 
    active = true;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reset");
  }

  //some helper methods, not traced
  
  long time()
  {
    return time;
  }
  
  long index()
  {
    return index;
  }
  
  long latest()
  {
    return latest;
  }
  
  AlarmListener listener()
  {
    return (AlarmListener) data;
  }
  
  Object context()
  {
    return context;
  }

  boolean hasGroupListener()
  {
    return groupListener;
  }

  boolean active()
  {
    return active;
  }

  MPAlarmImpl next()
  {
    return (MPAlarmImpl) getNext();
  }
  
  MPAlarmImpl previous()
  {
    return (MPAlarmImpl) getPrevious();
  }
  
  /**
   * return a String representation of this alarm, suitable for
   * use within the parent LinkedList's toString
   */
  public synchronized String toString(String indent)
  {    
    StringBuffer buffer = new StringBuffer();
    
    long now = System.currentTimeMillis();
    buffer.append(indent);
    buffer.append("Alarm("+index+","+time+","+latest+","+active+","+(time-now));        
    
    if(data != null)
      buffer.append(",@" + Integer.toHexString(data.hashCode()));
    else
      buffer.append(",-");
      
    if(context != null)
      buffer.append(",@" + Integer.toHexString(context.hashCode()));
    else
      buffer.append(",-");

    if(groupListener)
      buffer.append(",Y");
    else
      buffer.append(",N");

    buffer.append(")");
    
    if(parentList == null)
    {
      buffer.append(" Not in list");
    }
    
    return buffer.toString();
  }
}
