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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.StoppableThread;
import com.ibm.ws.sib.processor.utils.StoppableThreadCache;
import com.ibm.ws.sib.processor.utils.linkedlist.LinkedList;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;

/**
 * MPAlarmManager uses a separate thread to run given code after a specified
 * interval.
 * 
 * The MP Alarm Manager has two main parts; an ordered list of alarms
 * and a dedicated alarm thread. Each element in the list of alarms
 * contains the desired time at which the alarm should be fired, the
 * latest time the alarm can be fired and a reference to the alarm
 * callback which should be run when the alarm fires (there is also an
 * optional Object which will be passed back to the alarm callback
 * when the alarm is fired). The list of alarms is sorted by the desired
 * alarm time so that the next alarm which needs to be fired is at
 * the top of the list. The latest time an alarm can be fired is
 * calculated using a percentage of the of the length of the alarm
 * (the time between when it is created and the desired alarm time).
 * If an alarm is requested for 10 seconds time then the earliest is
 * can be run might be 10 per cent (a configurable constant) more,
 * i.e. 11 seconds time. This leeway in the alarm time means that we can
 * batch up multiple alarms together to be run at once. This reduces the
 * number of times at which the alarm thread needs to be woken up.
 *
 * When a new alarm is added to the list, the alarm thread is woken up
 * and then waits for the amount of time until the next alarm (the one
 * at the top of the list). When it next wakes up, a lock on the list
 * of alarms is taken and we begin to iterate down the list. Any alarms
 * which can be fired together (and have not been cancelled) are
 * removed from the list and added to a separate list of alarms which
 * are 'to be run'. The alarms are transferred from one list to another
 * so that we do not need to hold the lock on the list of 'future'
 * alarms while we are actually running alarms. This avoids any
 * possibility of deadlock with another thread which is trying to add a
 * new alarm while holding a lock which is required by the running alarm.
 * Once the end of the list is reached, the 'future' alarms list lock
 * is released. If it was possible to batch multiple alarms together,
 * the latest time at which they can be fired is recorded. If we have
 * reached this time we begin to iterate over the 'to be run' list, calling
 * each alarm callback in turn. When all of the alarms in the 'to be
 * run' list have been fired (or cancelled), the alarm thread is set
 * to wait until the next alarm in the 'future' list or until the time for
 * the next batch. If there are no alarms left to be run then the thread
 * waits indefinitely. If a new alarm is added, which has a shorter desired
 * alarm time than the current next alarm, we have to wake up the alarm
 * thread prematurely and reset it's wait time.
 *
 * In addition to batching of alarms it is also possible to group alarm
 * callbacks. If a GroupAlarmListener is used for multiple alarms then it's
 * alarm method is only called once per batch.
 *
 * When the MP is being shutdown we also need to cancel any pending alarms,
 * wait for any running alarms to complete and stop the MP Alarm thread.
 * To do this we must first take a lock on the 'to be run' list. This will
 * block until the alarm currently being run has completed. No more alarms
 * can be added to the 'to be run' list nor can existing entries begin to
 * run. We cancel all existing alarms by removing all alarms from both
 * the 'future' and 'to be run' list and putting them all in a temporary
 * 'to be cancelled' list. We can then iterate over this complete list
 * and mark all of the alarms as cancelled.
 * 
 * Due to the use of an object pool for the alarm objects, once an alarm has
 * been fired or canceled, it must not be used again. It is the user's
 * responisbility to enforce this.
 */
public class MPAlarmManager implements StoppableThread
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      MPAlarmManager.class,
      TraceGroups.TRGRP_UTILS,
      SIMPConstants.RESOURCE_BUNDLE);

  //flag to indicate that all of the existing alarms are being canceled
  private boolean _cancelAll = false;
  //the target time for the next alarm
  private long _nextAlarm = MPAlarmThread.SUSPEND;
  //the alarm thread
  private MPAlarmThread _alarmThread;
  //the list of 'future' alarms
  private LinkedList _pendingAlarms;
  //the list of alarms to be fired next
  private LinkedList _firedAlarms;
  //a lock for the list of fired alarms
  private Object _firedAlarmsLock = new Object();
  //a lock for the list of future alarms
  private Object _pendingAlarmsLock = new Object();
  //an object pool to hold alarm objects
  private ObjectPool _alarmPool;
  //the default percentage by which an alarm can be late
  private int _percentLateDefault = 50;
  //the time at which a batch of delayed alarms is to be fired
  private long _batchAlarmTime = -1;
  //a list of group listeners on the current batch
  private HashMap<SIBUuid12, AlarmListener> _groupListeners;
  // Cache the ME's name for debug
  private String _meName = null;
  
  private long index = 0;

  /**
   * Create a new MPAlarmManager
   * 
   * @param percentLateDefault a default late alarm percentage
   * @param alarmPoolSize the size of the alarm object pool
   */
  public MPAlarmManager(MessageProcessor mp, int percentLateDefault, int alarmPoolSize)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPAlarmManager", 
        new Object[] { mp, Integer.valueOf(percentLateDefault), Integer.valueOf(alarmPoolSize) });

    index = 0;
    
    //set up the lists    
    _pendingAlarms = new LinkedList();
    _firedAlarms = new LinkedList();
    //store the default percent late value
    _percentLateDefault = percentLateDefault;
    //create the alarm object pool
    _alarmPool = new ObjectPool("MPAlarmPool",alarmPoolSize);
    //create the map of group listeners
    _groupListeners = new HashMap<SIBUuid12, AlarmListener>();
    
    //create a new MPAlarmThread
    _alarmThread = new MPAlarmThread(this);    
    
    _meName = mp.getMessagingEngineName();
    
    try
    {
      mp.startNewSystemThread(_alarmThread);
    }
    catch(InterruptedException e)
    {
      // FFDC since this shouldn't happen
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.utils.am.MPAlarmManager",
        "1:207:1.28",
        this);
      SibTr.exception(tc, e);          
    }
    
    // Register this with the SystemThreadCache
    mp.getStoppableThreadCache().registerThread(this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPAlarmManager", this);
  }

  /**
   * Get the default percent late value. Only intended for testing.
   * 
   * @return the default percent late value
   */
  public int getPercentLateDefault()
  {    
    return _percentLateDefault;
  }

  public Alarm create(long delta, AlarmListener listener)
  {
    return create(delta,_percentLateDefault,listener,null);
  }

  public Alarm create(long delta, AlarmListener listener, Object context)
  {
    return create(delta,_percentLateDefault,listener,context);
  }

  /**
   * Create a new alarm.
   * 
   * @param delta The amount of time before the alarm should be fired, in ms
   * @param percentLate The percentage by which the alarm can be late
   * @param listener The alarm listener which is called when the alarm fires.
   * @param context A context object for the alarm
   * @return An Alarm object through which the alarm can be canceled
   */
  public Alarm create(long delta, int percentLate, AlarmListener listener, Object context)
   throws SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "create",
        new Object[] { this, Long.valueOf(delta), Integer.valueOf(percentLate), listener, context });

    //calculate the target time for the new alarm
    long time = System.currentTimeMillis() + delta;

    // Defect 516583 - catch and report the situation where an arithmetic overflow has occurred
    if(time <0)
    {
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.am.MPAlarmManager",
              "1:269:1.28" },
            null));
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.utils.am.MPAlarmManager.create",
        "1:274:1.28",
        this);
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.utils.am.MPAlarmManager",
          "1:280:1.28" });
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "MPAlarmManager", e);
      throw e;      
    }
    
    //calculate the latest time the alarm can be fired
    long latest = time + ((delta / 100) * percentLate);
    //try to get an existing alarm object from the pool
    MPAlarmImpl alarm = (MPAlarmImpl) _alarmPool.remove();
    //if we were unable to get one from the pool
    if(alarm == null)
    {
      //create a new one
      alarm = new MPAlarmImpl(time,latest,listener,context,index++);
    }
    else
    {
      //reset the values on the alarm retrieved from the pool
      alarm.reset(time,latest,listener,context,index++);
    }
    //take the pending alarms list lock
    synchronized(_pendingAlarmsLock)
    {
      //start at the bottom of the list
      //new alarms should generally come chronologically after existing ones
      //the insertPoint is the entry before where we are going to put the new alarm
      MPAlarmImpl insertPoint = (MPAlarmImpl) _pendingAlarms.getLast();
      //if the list is empty      
      if(insertPoint == null)
      {
        //just put the new alarm in to the list
        _pendingAlarms.put(alarm);
      }
      else //otherwise search for the correct place in the list
      {
        //if the current entry is after the time for the new alarm
        if(insertPoint.time() > time)
        {      
          do
          {
            //go to the next entry up
            insertPoint = insertPoint.previous();           
          }//until either we reach the top of the list or the current one is before the new one
          while(insertPoint !=null && insertPoint.time() > time);
        }
        //if we did reach the top of the list
        if(insertPoint == null)
        {
          //up the new alarm in at the top
          _pendingAlarms.insertAtTop(alarm);
        }
        else //if we are somewhere in the middle of the list
        {
          //insert the new alarm after the one we just found
          _pendingAlarms.insertAfter(alarm, insertPoint);
        }
      }
      //if the next alarm is MPAlarmThread.SUSPEND (i.e. the alarm thread is suspended)
      //or the time for the new alarm is before the time that the
      //alarm thread is currently due to wake up
      if(_nextAlarm == MPAlarmThread.SUSPEND || time < _nextAlarm)
      {
        //set the new target wakeup time
        _nextAlarm = time;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        long now = System.currentTimeMillis();
        SibTr.debug(tc, "now " + now + "Alarm Start : "+listener+
          " - "+(time - now)+
          " ("+(latest - now)+")");
        
        SibTr.debug(tc, "" + _pendingAlarms);
      }
      
    }
    
    //and wake up the the alarm thread with the new time
    _alarmThread.requestNextWakeup(_nextAlarm);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "create", alarm);

    //return the alarm reference
    return alarm;
  }

  /**
   * cancel all of the alarms which are currenly registered.
   */
  public synchronized void cancelAll()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelAll", this);

    //flag that we are cancelling all of the existing alarms
    _cancelAll = true;
    //variables to hold copies of the lists of existing alarms
    LinkedList oldPendingList = null;
    LinkedList oldFiredList = null;
    synchronized(_firedAlarmsLock)
    {
      synchronized(_pendingAlarmsLock)
      {
        //no need to suspend the alarm thread because after this the two lists will be empty
        
        //take a copy of the two lists and create new empty lists in their place
        oldPendingList = _pendingAlarms;
        _pendingAlarms = new LinkedList(); 
        oldFiredList = _firedAlarms;
        _firedAlarms = new LinkedList();
        //reset the next target alarm time
        _nextAlarm = MPAlarmThread.SUSPEND;
        //flag that all of the alarms have been canceled
        //(we haven't actually canceled them yet but they are not in the
        //active lists)
        _cancelAll = false;
      }
    }
    //release the locks for the two lists so that new alarms can be started
    //while we are still cleaning up the old ones
        
    //iterate over the first list
    MPAlarmImpl alarm = (MPAlarmImpl) oldPendingList.getFirst();
    while(alarm != null)
    {
      MPAlarmImpl next = alarm.next();
      //cancel each alarm
      alarm.cancel();
      //remove it from the list
      oldPendingList.remove(alarm);
      //return the alarm object to the object pool
      _alarmPool.add(alarm);
      alarm = next;
    }
    
    //iterate over the second list
    alarm = (MPAlarmImpl) oldFiredList.getFirst();
    while(alarm != null)
    {
      MPAlarmImpl next = alarm.next();
//    cancel each alarm
      alarm.cancel();
//    remove it from the list
      oldFiredList.remove(alarm);
//    return the alarm object to the object pool
      _alarmPool.add(alarm);
      alarm = next;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelAll");
  }
  
  /**
   * Tell the alarm manager to finish and stop it's alarm thread.
   */
  public void stopThread(StoppableThreadCache threadCache)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stopThread");
    
    cancelAll();
 
    _alarmThread.finishAlarmThread();
    
    // Remove this object from the thread cache
    threadCache.deregisterThread(this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stopThread");
  }

  /**
   * The callback from the alarm thread. This is the main alarm
   * method which co-ordinates and runs alarms.
   * 
   * @return the time, in ms, at which the alarm thread should next wakeup
   * and call this method ... or SUSPEND if the alarm thread should be
   * suspended
   */
  void fireInternalAlarm()
  {
//  what time is it now?
    long now = System.currentTimeMillis();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "fireInternalAlarm", new Object[] {this, Long.valueOf(now)});
    
    //lock the lists
    synchronized(_firedAlarmsLock)
    {      
      synchronized(_pendingAlarmsLock)
      {
        //find the next alarm to be run
        MPAlarmImpl alarm = (MPAlarmImpl) _pendingAlarms.getFirst();
        //while cancelAll has not been called and
        //      the list of pending alarms is not empty and
        //      the current alarm's target time has been passed
        //        or is earlier than the current batch alarm time 
        while(!_cancelAll && 
              alarm != null && 
              (alarm.time() <= now || 
               alarm.time() <= _batchAlarmTime))
        {
          //remove the next alarm from the pending list
          _pendingAlarms.remove(alarm);
          //and put it in the list of alarms to be fired
          _firedAlarms.put(alarm);
          //if the batch alarm time has not been set
          //or the latest time for the current alarm is earlier than that
          //of the batch Alarm time
          if(_batchAlarmTime == -1 || alarm.latest() < _batchAlarmTime)
          {
            //set the batch alarm time to be the latest time the current alarm
            //can be run
            _batchAlarmTime = alarm.latest();          
          }
          //get the next alarm in the list, which should now be at the top
          alarm = (MPAlarmImpl) _pendingAlarms.getFirst();
        }//while
        
        //if the last alarm in the list to be fired (if any) has a target
        //time earlier than the batch alarm time 
        alarm = (MPAlarmImpl)_firedAlarms.getLast();
        if(alarm != null && alarm.time() < _batchAlarmTime)
        {
          //set the batch alarm time to be the target time of the last alarm.
          //since it is the last alarm which can be fired, there is no point
          //in leaving the batch alarm time as the latest time for the first
          //alarm in the list ... i.e. fire as soon as possible for the given batch
          _batchAlarmTime = alarm.time();
        }                        
      }//release the pending alarm list lock
      //new alarms can now be added by other threads at the same time
      //as the existing once are being executed
      
      //if we have reach any given batch alarm time      
      if(now >= _batchAlarmTime)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
          SibTr.debug(tc, "Pending " + _pendingAlarms);
          SibTr.debug(tc, "Firing " + _firedAlarms);
        }
        
        //iterate over the list of alarms to be fired
        //until the list is empty or cancelAll is called
        MPAlarmImpl alarm = (MPAlarmImpl) _firedAlarms.getFirst();
        while(!_cancelAll && alarm != null)
        {
          //remove the current alarm from the list
          _firedAlarms.remove(alarm);
          // Take a copy of the variables from the alarm
          AlarmListener alarmListener = alarm.listener();
          boolean alarmHasGroupListener = alarm.hasGroupListener();
          Object alarmContext = alarm.context();
          boolean alarmIsActive = alarm.active();
          //if it has not been canceled
          if(alarmIsActive && alarmListener != null)
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Alarm Fire : " + alarm);
            
            //if it does not have a group listener
            if(!alarmHasGroupListener)
            {
              try
              {
                //fire the alarm, passing in it's context object
                alarmListener.alarm(alarmContext);
              }
              catch(Throwable e)
              {
                // FFDC the problem with the alarm but swallow the exception
                // and carry on, in the hope that the ME can limp on regardless
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.utils.am.MPAlarmManager.fireInternalAlarm",
                  "1:560:1.28",
                  new Object[] {this, alarmListener, alarmContext});
                
                SibTr.exception(tc, e);          
              }
            }//if it does have a group listener which is not in the list for this batch
            else if(!_groupListeners.containsKey(((GroupAlarmListener)alarmListener).getGroupUuid()))
            {
              //put the listener in the list for this batch
              _groupListeners.put(((GroupAlarmListener)alarmListener).getGroupUuid(), alarmListener);
              //start the group alarm sequence, passing in the first alarm context
              ((GroupAlarmListener)alarmListener).beginGroupAlarm(alarmContext);
            }
            else //if it does have a group listener which is already in the list for this batch
            {
              //add the alarm context to the group listener
              ((GroupAlarmListener)alarmListener).addContext(alarmContext);
            }
            // return the alarm object to the pool
            // we only do this in the active case, as if we found the alarm had been
            // cancelled by another thread, it's safer to simply discard the object
            // rather than assuming it's safe for re-use.
            _alarmPool.add(alarm); 
          }
          //get the next alarm to be fired
          alarm = (MPAlarmImpl) _firedAlarms.getFirst();
        }//while
        
        //if group listeners were found in the batch
        if(_groupListeners.size() > 0)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.debug(tc, "GroupListeners size is: " + _groupListeners.size());
          //iterate over the group listeners for this batch
          Set groupNames = _groupListeners.keySet();
          Iterator itr = groupNames.iterator();
          while(itr.hasNext())
          {
            //get the next group listener
            GroupAlarmListener listener =
              (GroupAlarmListener) _groupListeners.get(itr.next());
            try
            {
              //call it's alarm method with a null context
              listener.alarm(null);
            }
            catch(Throwable e)
            {
              // FFDC the problem with the alarm but swallow the exception
              // and carry on, in the hope that the ME can limp on regardless
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.utils.am.MPAlarmManager.fireInternalAlarm",
                "1:613:1.28",
                new Object[] {this, listener});
              
              SibTr.exception(tc, e);          
            }
          }
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.debug(tc, "Finished calling group alarms");
          //clear the list of group listeners ready for the next batch
          _groupListeners.clear();        
        }
        //the batch is done so reset the batch alarm time
        _batchAlarmTime = -1;        
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.debug(tc, "About to get _pendingAlarmsLock");
      //retake the pending alarms list lock while we work out when the next alarm is      
      synchronized(_pendingAlarmsLock)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.debug(tc, "Got _pendingAlarmsLock");
        //if there is still a pending batch to be run
        if(_batchAlarmTime > -1)
        {
          //use the batch alarm time as the next target wakeup time          
          _nextAlarm = _batchAlarmTime;
        }
        else //otherwise look at the list of pending alarms
        {
          MPAlarmImpl alarm = (MPAlarmImpl)_pendingAlarms.getFirst();
          //if there are not any alarms in the list, set the target time to MPAlarmThread.SUSPEND
          //in order to suspend the alarm thread
          if(alarm == null) _nextAlarm = MPAlarmThread.SUSPEND;
          //if there is at least one pending alarm, the target time is that of
          //the first alarm
          else _nextAlarm = alarm.time();
        }                
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "fireInternalAlarm", Long.valueOf(_nextAlarm));
    }            
  }
  
  long getNextWakeup()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNextWakeup");
    
    synchronized (_pendingAlarmsLock)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getNextWakeup", _nextAlarm);
      
      return _nextAlarm;
    }
  }
  
  public String toString()
  {
  	return super.toString() + " " + _meName;
  }
}
