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
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A background thread used to run code at specified times. It will wait until
 * there is something to do rather than loop continuously.
 */
public class MPAlarmThread implements Runnable
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      MPAlarmThread.class,
      TraceGroups.TRGRP_UTILS,
      SIMPConstants.RESOURCE_BUNDLE);

  public static final long SUSPEND = Long.MAX_VALUE;

  //reference to the owning alarm manager
  private MPAlarmManager manager;
  //flag to indicate if this thread should terminate
  private boolean finished = false;
  //flag to indicate if this thread should be suspended
  private boolean running = false;
  //the target time in ms at which to wake up next
  private long nextWakeup = SUSPEND;
  //a lock to synchronize the setting of the wakeup time
  private Object wakeupLock = new Object();
  //the number of ms until the next wake up
  private long wakeupDelta = 0;
  /**
   * NLS for component
   */
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);


  /**
   * Create a new MPAlarmThread for a given MPAlarmManager
   *
   * @param manager The owning MPAlarmManager
   */
  public MPAlarmThread(MPAlarmManager manager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPAlarmThread",manager);

    //store the owning MPAlarmManager
    this.manager = manager;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPAlarmThread", this);
  }

  /**
   * Set the next target time at which the alarm thread should wake up and
   * call the alarm method on the MPAlarmManager
   *
   * if the target time is set to -1 then this indicates that no wakeup is required
   * and the thread should be suspended
   *
   * @param wakeup the next target time in ms
   */
  void requestNextWakeup(long wakeup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "requestNextWakeup", new Object[] { Long.valueOf(wakeup) });


    //synchronize on the target time
    synchronized(wakeupLock)
    {
      if(wakeup < nextWakeup)
      {
        setNextWakeup(wakeup);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestNextWakeup", new Object[]{Boolean.valueOf(running),Long.valueOf(wakeupDelta)});
  }

  /**
   * Set the next target time at which the alarm thread should wake up and
   * call the alarm method on the MPAlarmManager
   *
   * if the target time is set to -1 then this indicates that no wakeup is required
   * and the thread should be suspended
   *
   * @param wakeup the next target time in ms
   */
  private void setNextWakeup(long wakeup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setNextWakeup", new Object[] { Long.valueOf(wakeup) });

    //synchronize on the target time
    synchronized(wakeupLock)
    {
      //set the target time
      nextWakeup = wakeup;
      //calculate the time until the target time
      wakeupDelta = nextWakeup - System.currentTimeMillis();

      //if the target time is SUSPEND then set running to false, otherwise true
      running = nextWakeup != SUSPEND;
      //wake up the alarm thread so that the new wake up time can be picked up
      if(running) wakeupLock.notify();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setNextWakeup", new Object[]{Boolean.valueOf(running),Long.valueOf(wakeupDelta)});
  }

  /**
   * Terminate this alarm thread. This is final, the thread should not
   * be restarted.
   */
  void finishAlarmThread()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finishAlarmThread");

    //wake up the thread so that it will exit it's main loop and end
    synchronized(wakeupLock)
    {
      //flag this alarm thread as finished
      finished = true;

      wakeupLock.notify();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finishAlarmThread");
  }

  /**
   * The main loop for the MPAlarmThread. Loops until the alarm thread
   * is marked as finished. If the alarm thread is suspended, it will
   * wait forever. Otherwise the it will wait inside the loop until a
   * specified time and then call the MPAlarmManager.fireInternalAlarm
   * method.
   */
  public void run()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "run");

    try
    {
      //loop until finished
      while(!finished)
      {
        //what time is it now
        long now = System.currentTimeMillis();

        boolean fire = false;

        //synchronize on the wake up lock
        synchronized(wakeupLock)
        {
          //if not suspended and we've reached or passed the target wakeup time
          if(running)
          {
            fire = (now >= nextWakeup);
          }
        }
        if(fire)
        {
            //call the internal alarm method which should return the
            //time for the next wakeup ... or SUSPEND if the thread should be suspended
            manager.fireInternalAlarm();
            synchronized (wakeupLock)
            {
              setNextWakeup(manager.getNextWakeup());
            }
        }

        synchronized(wakeupLock)
        {
          //if we are still not suspended (another thread could have got in before
          // the re-lock and changed things)
          if(running)
          {
            //if there is still time until the next wakeup
            if(wakeupDelta > 0)
            {
              try
              {
                if (!finished)
                {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc,  "Thread wait : "+wakeupDelta);

                  //wait until the next target wakeup time
                  long start = System.currentTimeMillis();
                  wakeupLock.wait(wakeupDelta+10);
                  long end = System.currentTimeMillis();

                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Thread slept for " + (end-start));

                   if(end < nextWakeup)
                     setNextWakeup(nextWakeup);
                }
              }
              catch (InterruptedException e)
              {
                // No FFDC code needed
                // swallow InterruptedException ... we'll just loop round and try again
              }
            }
          }
          else
          {
            try
            {
              if (!finished)
              {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc,  "Thread wait : Inifinite");
                wakeupLock.wait();
              }
            }
            catch (InterruptedException e)
            {
              // No FFDC code needed
              // swallow InterruptedException ... we'll just loop round and try again
            }
          }
        } // synchronized
      }
    }
    catch (RuntimeException e)
    {
      // FFDC
      FFDCFilter.processException(e,
                                  "com.ibm.ws.sib.processor.utils.am.MPAlarmThread.run",
                                  "1:284:1.8.1.7",
                                  this);

      SibTr.error(tc,
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] { "com.ibm.ws.sib.processor.utils.am.MPAlarmThread", "1:290:1.8.1.7", e },
          null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "run", e);
      }

      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "run");
  }
}
