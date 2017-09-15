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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.ClosedException;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.processor.utils.linkedlist.LinkedList;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * A ConsumerKey and other information, associated with an AOStream.
 * The JSRemoteConsumerPoint starts in the (!closed) state, in which it accepts new requests (the newRequest() method).
 * It transitions to the (closed) state and stops accepting new requests. The transition can occur either due
 * to the parent AOStream asking it to close, or asynchronous activity such as an idle timeout, or an exception
 * on the ConsumerKey. When the JSRemoteConsumerPoint transitions to (closed) state due to such asynchronous activity
 * it issues a callback to its parent notifying it of the change.
 *
 * SYNCHRONIZATION: All state is read and written using synchronized (this). All callbacks to parent methods
 * occur outside synchronized (this), to avoid any deadlocks wrt parent locks.
 */

public final class JSRemoteConsumerPoint extends ReentrantLock implements AlarmListener, DispatchableConsumerPoint
{
  /**
   * 
   */
  private static final long serialVersionUID = -4278192555858958128L;

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      JSRemoteConsumerPoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private MPAlarmManager am;

  /** The encapsulating AOStream */
  private AOStream parent;

  private JSConsumerManager dispatcher;

  /** The ConsumerKeys */
  private ConsumableKey[] cks;

  /* an arbitrary ConsumerKey in the above list, that is used for all ready and not ready transitions */
  private ConsumableKey ck;

  private String selectionCriteriasAsString;
  private SelectionCriteria[] selectionCriterias;

  /** true if called ready() on the ConsumerKey, else false.
   * We don't depend on ConsumerKeyGroup for managing group ready status since the same JSRemoteConsumerPoint
   * represents all the members of the group */
  private boolean isready;

  /** true if temporarily stopped because receive is not allowed. temporarilyStopped implies !isready */
  private boolean temporarilyStopped;

  /** Set to true when this object is not able to accept any more requests */
  private boolean closed;

  /** The timeout period when this object is idle
   * Note: After a lot of pondering the reason for this (and not just use the AOStream inactivity timeout)
   * I've come up with a useful scenario for it, so that must be it! Basically, we do this cleanup because
   * it's possible for a single AOStream to need multiple consumers (for different selectors), and therefore,
   * if we didn't clean them up we could end up with a load of redundant consumers associated with the
   * AOStream that we no longer need. e.g. If this was a reply queue and every request had a different selector. 
   */
  private long idleTimeout;

  /** The idle alarm handler. A non-null value indicates an existing alarm since listOfRequests is empty */
  private Alarm idleHandler;

  /** A linked list of requests. New requests are placed at the end of the list
   *  The following invariant is always true: any element in listOfRequests is in its initial state, i.e.,
   *  (!expired && !satisfied)
   */
  private LinkedList listOfRequests;

  private HashMap<Long, AORequestedTick> tableOfRequests;
  
  private boolean _consumerSuspended = false;
  private int _suspendFlags = 0;

  // Are we on an IME and performing gathering
  private boolean gatherMessages;

  /**
   * Constructor
   * @param parent
   * @param selector The selector
   * @param ck
   * @param idleTimeout When there is no waiting request for more than this period of time (in ms), this object will close itself
   */
  public void init(
    AOStream parent,
    String selectionCriteriasAsString,
    ConsumableKey[] consumerKeys,
    long idleTimeout,
    MPAlarmManager am,
    SelectionCriteria[] selectionCriterias)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "init",
        new Object[] {
          parent,
          selectionCriteriasAsString,
          consumerKeys,
          Long.valueOf(idleTimeout),
          am,
          selectionCriterias });

    this.lock();
    try
    {
      this.parent = parent;
      this.selectionCriteriasAsString = selectionCriteriasAsString;
      this.cks = consumerKeys;
      this.ck = consumerKeys[0];
      this.selectionCriterias = selectionCriterias;

      this.isready = false;
      this.dispatcher = cks[0].getConsumerManager(); // all cks[i] have the same ConsumerDispatcher
      this.temporarilyStopped = !dispatcher.getDestination().isReceiveAllowed();
      this.closed = false;
      this.listOfRequests = new LinkedList();
      this.tableOfRequests = new HashMap<Long, AORequestedTick>();
      this.idleTimeout = idleTimeout;
      this.am = am;
      
      if (ck instanceof GatheringConsumerKey)
        this.gatherMessages = true;
      
      if (idleTimeout > 0)
        this.idleHandler = am.create(idleTimeout, this);
    }
    finally
    {
      this.unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "init");

  }

  /**
   * Closes this object and all contained AORequestedTicks.
   * For requests that are cancelled by this method, it will issue a callback to the parent.
   * Note that even after this method returns, the parent may receive expiredRequest()
   * and satisfiedRequest() callbacks. This is because other threads may have exited their
   * synchronized (this) block before the close() executed, but are 'slow' so their callbacks
   * have not occured yet.
   */
  public final void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close");

    ArrayList<AORequestedTick> expiredTicks = new ArrayList<AORequestedTick>(tableOfRequests.size()); // approximately allocate correct size
    closeInternal(expiredTicks);
    int length = expiredTicks.size();
    for (int i = 0; i < length; i++)
    {
      AORequestedTick rt = (AORequestedTick) expiredTicks.get(i);
      parent.expiredRequest(rt.tick);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "close");

  }

  private final void closeInternal(ArrayList<AORequestedTick> expiredTicks)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeInternal");

    ConsumableKey[] tempCks = null;

    this.lock();
    try
    {
      closed = true;
      if (cks != null)
      {
        tempCks = cks;
        cks = null;
      }
      if (idleHandler != null)
      {
        idleHandler.cancel();
        idleHandler = null;
      }
      // cleanup listOfRequests
      AORequestedTick rt = (AORequestedTick) listOfRequests.getFirst();
      while (rt != null)
      {
        listOfRequests.remove(rt);
        boolean expired = rt.expire(true);
        if (!expired)
        {
          // this should never occur since a competing transition to (satisfied and !expired) also occurs
          // in synchronized (this) and that also removed rt from the listOfRequests

          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
                  "1:293:1.43.2.26" },
                null));

          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.closeInternal",
            "1:299:1.43.2.26",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
              "1:306:1.43.2.26" });
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeInternal", e);
          throw e;
        }

        expiredTicks.add(rt);

        rt = (AORequestedTick) listOfRequests.getFirst();
        // get the next element in the list
      }
      tableOfRequests.clear(); // clear the tableOfRequests
    } // end this.lock()
    finally
    {
      this.unlock();
    }

    // do the detach calls outside the synchronized block to avoid deadlock with the ConsumerDispatcher
    try
    {
      if (tempCks != null)
      {
        for (int i=0; i < tempCks.length; i++)
        {
          tempCks[i].detach();
        }
        // once all these are detached the ConsumerKeyGroup, if any, will be automatically removed from the CD.
      }
    }
    catch (Exception e)
    {
      // No FFDC code needed
      // ignore all exceptions at close() since close() may have been caused by some other exception, which
      // has already logged the problem
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeInternal");

  }

  /**
   * Callback from the AORequestedTick when the expiry alarm occurs.
   * @param requestedTick
   */
  public void expiryAlarm(AORequestedTick requestedTick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "expiryAlarm", requestedTick);

    boolean transitionOccured = false;
    ArrayList<AORequestedTick> satisfiedTicks = null;
    try
    {
      this.lock();
      try
      {
        // it is possible that this event occurs after this object is closed.
        // it is safe to ignore it since we must have already transitioned the requestedTick to its final state
        if (closed)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "expiryAlarm");
  
          return;
        }
  
        transitionOccured = cancelRequestInternal(requestedTick, true);
        
        // If we expired the request and have more then process those
        // requests now
        if (transitionOccured && !listOfRequests.isEmpty())
          satisfiedTicks = processQueuedMsgs(null);
      }
      finally
      {
        this.unlock();
      }
    }
    catch(SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed
      notifyException(e);
    }

    if (transitionOccured)
    { // parent needs to be informed
      parent.expiredRequest(requestedTick.tick);
    }
    
    if (satisfiedTicks != null)
    {
      // inform parent about satisfied ticks - outside lock
      int length = satisfiedTicks.size();
      for (int i = 0; i < length; i++)
      {
        AORequestedTick aotick = (AORequestedTick) satisfiedTicks.get(i);
        long tick = aotick.tick;
        SIMPMessage m = aotick.getMessage();
        parent.satisfiedRequest(tick, m);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "expiryAlarm");

  }

  /**
   * A new request
   * @param tick The tick in the stream for this request
   * @param expiryTimeout The time before the request expires. Note that we do not use the LocalConsumerPoint constants,
   * and instead use 0L for NO_WAIT and AnycastConstantsAndConfig.INFINITE_TIMEOUT for INFINITE_WAIT
   * @return The message, which is non-null if the request has been satisfied, and null if the request has been queued.
   * @throws ClosedException This object is already in the closed state
   * @throws MessageStoreException The message store threw an exception
   * @throws SINotPossibleInCurrentConfigurationException The ConsumerKey.ready() methods threw an exception
   */
  public final AORequestedTick newRequest(long tick, long expiryTimeout)
    throws ClosedException, MessageStoreException, SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "newRequest",
        new Object[] { Long.valueOf(tick), Long.valueOf(expiryTimeout)});

    SIMPMessage msg = null;
    AORequestedTick aotick = null;
    try
    {
      this.lock();
      try
      {
        if (closed)
        {
          ClosedException e = new ClosedException();
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "newRequest", e);
  
          throw e;
        }
        // cancel the idle timer
        if (idleHandler != null)
        {
          idleHandler.cancel();
          idleHandler = null;
        }

        // try to satisfy it immediately
        if (!temporarilyStopped)
        {
          if (!isready)
          {
            // have not indicated isready, so listOfRequests must be empty prior to inserting this AORequestedTick
            ck.ready(Reliability.NONE);
            isready = true;
            msg = (SIMPMessage) cks[0].getMessageLocked();
            if (msg != null)
            {
              // no longer ready
              ck.notReady();
              isready = false;
            }
            if (msg == null || gatherMessages) // Drive any refills if we are gathering
            {            
              long timeout = cks[0].waiting(convertTimeoutSIMPtoLCP(expiryTimeout), false);
              if (msg == null && timeout != LocalConsumerPoint.NO_WAIT && timeout != LocalConsumerPoint.INFINITE_WAIT )
                expiryTimeout = convertTimeoutLCPtoSIMP(timeout);
            }
          }             
        } // end if (!temporarilyStopped)    
             
        // create a new AORequestedTick and add it to listOfRequests, tableOfRequests
        aotick =
          new AORequestedTick(this, tick, Long.valueOf(tick), expiryTimeout, am);
      
        if (msg!=null)
        {
          // satisfy the tick
          boolean satisfied = aotick.satisfy(msg);
          if (!satisfied)
          {
            // unlock the msg and return null (dont increment unlock count)
            msg.unlockMsg(msg.getLockID(),null, false);
            msg = null;
            
            // The idle timeout is for periods of time when we have no requests. If a msg is
            // locked it must not be running. 
            // At this point we have no locked msg.
            if( idleTimeout > 0) 
                this.idleHandler = am.create(idleTimeout, this);    
          }         
        }  
        else
        {
          listOfRequests.put(aotick);
          tableOfRequests.put(Long.valueOf(tick), aotick);
        }
      } // end this.lock
      finally
      {
        this.unlock();
      }
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      
      // Things have gone wrong with the message store, best to close down
      // this consumer, forcing a new one to be created for new requests.
      // The existing requests are in a bit of a funny state now, but hey,
      // it's not our fault, blame the message store.
      
      // notify the parent first to try to stop new requests using this one
      parent.removeConsumerKey(selectionCriteriasAsString, this);
      
      // close this and throw exception
      close(); //Never call close() from withing synchronized (this)!!
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "newRequest", e);
      throw e;
    }
    catch (SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed

      // close this and throw exception
      close(); //Never call close() from withing synchronized (this)!!
      // notify the parent since this is an asynchronous close
      parent.removeConsumerKey(selectionCriteriasAsString, this);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "newRequest", e);
      throw e;
    }

    /**
     * If the request had a 0 timeout and we couldnt find a message then we expire the 
     * request 
     */
    if (expiryTimeout == 0L && msg == null)
    {
      // Since we are not prepared to wait we will expire this request.
      // The aotick handles its own expiry timer when expiryTimeout > 0L
      expiryAlarm(aotick);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "newRequest", msg);

    return aotick;
  }

  /**
   * This method is called by the parent to cancel a request. Note that it is possible that the
   * request has already been satisfied, and parent.satisfiedRequest() is being called concurrently
   * with this method.
   * @param tick The tick identifying the request
   */
  public final void cancelRequest(long tick)
  {
    Long objTick = Long.valueOf(tick);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelRequest", objTick);

    ArrayList<AORequestedTick> satisfiedTicks = null;
    
    try
    {
      this.lock();
      try
      {
        // it is possible that this event occurs after this object is closed.
        // it is safe to ignore it since we must have already transitioned the tick to its final state
        if (closed)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cancelRequest");
  
          return;
        }
        AORequestedTick requestedTick =
          (AORequestedTick) tableOfRequests.get(objTick);
        
        if (requestedTick != null)
        {
          boolean transitionOccured = cancelRequestInternal(requestedTick, false);
        
          // If we expired the request and have more then process those
          // requests now
          if (transitionOccured && !listOfRequests.isEmpty())
            satisfiedTicks = processQueuedMsgs(null);
        }
  
      }
      finally
      {
        this.unlock();
      }
    }
    catch(SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed
      notifyException(e);
    }
    
    if (satisfiedTicks != null)
    {
      // inform parent about satisfied ticks - outside lock
      int length = satisfiedTicks.size();
      for (int i = 0; i < length; i++)
      {
        AORequestedTick aotick = (AORequestedTick) satisfiedTicks.get(i);
        parent.satisfiedRequest(aotick.tick, aotick.getMessage());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelRequest");
  }

  /**
   * Should be called in a synchronized (this) block, and when !closed
   * @param requestedTick
   * @param expiry true if this is called due to expiry of the tick, else false
   * @return true if this method transitioned the state of requestedTick, else false
   */
  private boolean cancelRequestInternal(
    AORequestedTick requestedTick,
    boolean expiry)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "cancelRequestInternal",
        new Object[] { requestedTick, Boolean.valueOf(expiry)});

    // cancel the timer only if !expiry, since expiry implies timer has already occured.
    boolean transitionOccured = requestedTick.expire(!expiry);
    if (transitionOccured)
    { // successfully expired
      listOfRequests.remove(requestedTick);
      tableOfRequests.remove(requestedTick.objTick);

      // start the idle timer if no element left in listOfRequests
      // and set isready to false
      if (listOfRequests.isEmpty())
      {
        if (idleTimeout > 0)
          this.idleHandler = am.create(idleTimeout, this);
        if (isready)
        {
          isready = false;
          ck.notReady();
        }
      }
    } // end if (transitionOccured)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelRequestInternal", Boolean.valueOf(transitionOccured));

    return transitionOccured;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.ConsumerPoint#put(com.ibm.ws.sib.processor.impl.ItemReference)
   */
  public boolean put(SIMPMessage msg, boolean isOnItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { msg, Boolean.valueOf(isOnItemStream)});

    boolean returnValue = false;
    // chose small value for ArrayList initial size since typically not many new messages will be produced
    // during the short time this method is being executed
    ArrayList<AORequestedTick> satisfiedTicks = new ArrayList<AORequestedTick>(5);

    // We need to lock down the whole AOStream so that another thread can't jump in between
    // us satisfying the request and actually creating an AOValue and updating the AOStream.
    // Otherwise, the other thread may modify the latestTick value, based on their request,
    // and we'll put a 'future' tick as our 'previous' tick and end up with an inconsistent
    // stream.
    synchronized(parent) // AOStream
    {
      try
      {
        this.lock();
        try
        {
          if (closed)
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "put", Boolean.FALSE);
            return false;
          }
          if (!isOnItemStream)
          {
            // this should not really occur. we ought to log this error
            SIErrorException e =
              new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
                    "1:713:1.43.2.26" },
                  null));
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.put",
              "1:718:1.43.2.26",
              this);
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
                "1:724:1.43.2.26" });
  
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "put", Boolean.valueOf(false));
  
            return false; // we only accept recoverable messages
          }
          if (!isready)
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "put", Boolean.valueOf(false));
  
            return false; // no one is interested in a message
          }
  
          // set isready to false since ConsumerKey is no longer ready
          isready = false;
  
          // possibly someone interested in the message
          AORequestedTick rt = (AORequestedTick) listOfRequests.getFirst();
          if (rt == null)
          {
            // no request is waiting, so do nothing
          }
          else
          { // there is atleast one waiting request (which is !expired and !satisfied)
            boolean locked = false;
            try
            {
              locked = msg.lockItemIfAvailable(cks[0].getGetCursor(msg).getLockID());
            }
            catch(MessageStoreException e)
            {
              // FFDC
              FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.put",
                  "1:761:1.43.2.26",
                  this);  
                          
              SibTr.exception(tc, e); 
              
              // TODO : We need a much wider handling of exception here and around this area. If anything
              // goes wrong we probably need to close off the consumerSession otherwise msgs could get "stuck"
              
            }
            // lock this particular message
            if (!locked)
              msg = null;
  
            if (msg != null)
            {
              AORequestedTick satTick = satisfyRequest(rt, msg);
              if (satTick != null)
                satisfiedTicks.add(satTick);
  
              rt = null;
              msg = null;
              returnValue = true; // the message was accepted
            }
            // At this point there are 2 possibilities: (1) msg == null && rt == null, or
            // (2) msg == null && rt != null. Case #2 occurs if the msg was not successfully locked.
  
            // while the ConsumerKey was not ready, many messages may have been made available but they were missed
            // so loop till either there are no more waiting requests, or there are no more available messages
            satisfiedTicks.addAll(processQueuedMsgs(rt));
  
          } // end else
        } // end this.lock()
        finally
        {
          this.unlock();
        }
      }
      catch(SINotPossibleInCurrentConfigurationException e)
      {
        // No FFDC code needed
        notifyException(e);
      }
      
      // inform parent about satisfied ticks - outside lock
      int length = satisfiedTicks.size();
      for (int i = 0; i < length; i++)
      {
        AORequestedTick aotick = (AORequestedTick) satisfiedTicks.get(i);
        parent.satisfiedRequest(aotick.tick, aotick.getMessage());
      }
    } // synchronized(parent)
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "put", Boolean.valueOf(returnValue));

    return returnValue;
  }
  
  private ArrayList<AORequestedTick> processQueuedMsgs(AORequestedTick rt)
    throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processQueuedMsgs", rt);
    
    ArrayList<AORequestedTick> satisfiedTicks = 
      new ArrayList<AORequestedTick>();
    
    // If we werent told which request to start from, get the first one
    if (rt==null)
      rt = (AORequestedTick) listOfRequests.getFirst();
    
    try
    {
      if (rt != null)
      {
        ck.ready(Reliability.NONE); // first set ready to true
        isready = true;
  
        while (rt != null)
        {
          SIMPMessage msg = (SIMPMessage) cks[0].getMessageLocked();
          if (msg == null || gatherMessages) // Drive any refills if we are gathering
          {
            // No local message so make sure we drive a request for a remote
            // message if necessary (e.g. we are a gathering IME)
            
            long timeout = rt.timeout;
            // We send a remote request with a timeout which is the remaining time left
            // before this request expires (unless its infinite/no_wait)
            if (timeout > 0L)
              timeout -= (System.currentTimeMillis() - rt.requestTime);
            
            if (timeout == SIMPConstants.INFINITE_TIMEOUT || timeout >= 0L)
              ck.waiting(convertTimeoutSIMPtoLCP(timeout), false);  
            
            if (msg == null)
             break; // break out of loop if we found no msg
          }
          AORequestedTick satTick = satisfyRequest(rt, msg);
          if (satTick != null)
            satisfiedTicks.add(satTick);
  
          rt = (AORequestedTick) listOfRequests.getFirst();
          // get the next listEntry
        }
      } // end if (listEntry != null)
  
      if (rt == null)
      { // satisfied all requests
        ck.notReady();
        isready = false;
  
        // start the idle timer if idleHandler==null
        if ((idleHandler == null) && (idleTimeout > 0))
          this.idleHandler = am.create(idleTimeout, this);
      }
    }
    catch (MessageStoreException e)
    {
      // FFDC
      // We failed to get a message for a particular tick, we still return the list
      // of ticks that did get satisfied
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.processQueuedMsgs",
          "1:886:1.43.2.26",
          this);
      SibTr.exception(tc, e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processQueuedMsgs", satisfiedTicks);
    
    return satisfiedTicks;
  }

  /**
   * Convert a SIMPConstants format timeout into a LocalConsumerPoint format
   * (they have different values for infinite and zero!)
   * @param simpTimeout
   * @return lcpTimeout
   */
  private long convertTimeoutSIMPtoLCP(long simpTimeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "convertTimeoutSIMPtoLCP", simpTimeout);
    
	long lcpTimeout = simpTimeout;
	
	if(simpTimeout == 0L)
		lcpTimeout = LocalConsumerPoint.NO_WAIT;
	else if(simpTimeout == SIMPConstants.INFINITE_TIMEOUT)
		lcpTimeout = LocalConsumerPoint.INFINITE_WAIT;
	
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "convertTimeoutSIMPtoLCP", lcpTimeout);

    return lcpTimeout;
  }

  /**
   * Convert a LocalConsumerPoint format timeout into a SIMPConstants format
   * (they have different values for infinite and zero!)
   * @param lcpTimeout
   * @return simpTimeout
   */
  private long convertTimeoutLCPtoSIMP(long lcpTimeout)
  {
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	   SibTr.entry(tc, "convertTimeoutLCPtoSIMP", lcpTimeout);
	    
	long simpTimeout = lcpTimeout;
	
	if(lcpTimeout == LocalConsumerPoint.NO_WAIT)
	  simpTimeout = 0;
	else if(lcpTimeout == LocalConsumerPoint.INFINITE_WAIT)
		simpTimeout = SIMPConstants.INFINITE_TIMEOUT;
	
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		   SibTr.exit(tc, "convertTimeoutLCPtoSIMP", simpTimeout);

	return simpTimeout;
  }

/* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.ConsumerPoint#notifyException(com.ibm.ws.sib.processor.SIMPException)
   */
  public void notifyException(Throwable e)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyException", e);

    close();
    // notify the parent since this is an asynchronous close
    parent.removeConsumerKey(selectionCriteriasAsString, this);

    // log error
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.notifyException",
      "1:961:1.43.2.26",
      this);
    SibTr.exception(tc, e);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyException");

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#notifyReceiveAllowed(boolean)
   */
  public void notifyReceiveAllowed(boolean isAllowed)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyReceiveAllowed", new Object[] { Boolean.valueOf(isAllowed)});

    ArrayList<AORequestedTick> satisfiedTicks = null;
    try
    {
      this.lock();
      try
      {
        if ((!closed) && isAllowed)
        {
          temporarilyStopped = false; // no longer stopped
          // while isAllowed was false, many messages may have been made available but they were missed
          // so loop till either there are no more waiting requests, or there are no more available messages
          satisfiedTicks = processQueuedMsgs(null);
  
        } // end if (isAllowed)
        else
        {
          temporarilyStopped = true;
          isready = false;
          ck.notReady();
        }
      } // end this.lock()
      finally
      {
        this.unlock();
      }
    }
    catch(SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed
      notifyException(e);
    }
    
    // inform parent about satisfied ticks - outside lock
    if (satisfiedTicks != null)
    {
      int length = satisfiedTicks.size();
      for (int i = 0; i < length; i++)
      {
        AORequestedTick aotick = (AORequestedTick) satisfiedTicks.get(i);
        parent.satisfiedRequest(aotick.tick, aotick.getMessage());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyReceiveAllowed");
  }

  public SelectionCriteria[] getSelectionCriterias()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelectionCriterias");
      SibTr.exit(tc, "getSelectionCriterias", selectionCriterias);
    }
    return this.selectionCriterias;
  }

  /**
   * Internal method for satisfying a waiting request (for a given tick) with a non-persistently locked message
   * This method must be called from within a synchronized (this) block, and the cursor of listOfRequests must be
   * positioned at listEntry
   * @param aotick The entry containing the waiting request
   * @param msg The non-persistently locked message
   * @return The satisfied AORequestedTick, or null if the tick has already transitioned to expired state.
   */
  private AORequestedTick satisfyRequest(
    AORequestedTick aotick,
    SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "satisfyRequest", new Object[] { aotick, msg });

    if (!aotick.satisfy(msg))
    {
      aotick = null;

      // this should never occur. throw serious exception and log this error!
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
              "1:1061:1.43.2.26" },
            null));

      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.satisfyRequest",
        "1:1067:1.43.2.26",
        this);
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
        "1:1073:1.43.2.26" });
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "satisfyRequest", e);
      throw e;
    }

    // remove the satisfied tick from the list
    listOfRequests.remove(aotick);
    // remove from tableOfRequests
    tableOfRequests.remove(aotick.objTick);


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "satisfyRequest");

    return aotick;
  }

  /**
   * The idle timeout has expired
   */
  public void alarm(Object thandle)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", thandle);

    boolean doClose = false;

    // We're probably about to close this consumer, which needs us to remove it from the AOStream's
    // consciousness first (so that it doesn't allow it to be used while we're closing it - as that's 
    // done outside of the lock). To do this we need to take the AOStream's (parent's) lock first and
    // remove it from the parent's list prior to releasing the lock.
    synchronized(parent)
    {
      this.lock();
      try
      {
        if (idleHandler != null)
        { // so we are still idle
          if (!listOfRequests.isEmpty())
          { // we have an outstanding request. idleHandler should be null, and therefore != thandle
            // Since this should never occur, log this error
            SIErrorException e =
              new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
                    "1:1121:1.43.2.26" },
                  null));
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint.alarm",
              "1:1126:1.43.2.26",
              this);
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSRemoteConsumerPoint",
                "1:1132:1.43.2.26" });
          }
          else
          {
            // Remove this consumer so that it is no longer handed out for new requests (they'll
            // have to create a new consumer to use)
            parent.removeConsumerKey(selectionCriteriasAsString, this);
            
            doClose = true;
          }
        }
      }
      finally
      {
        this.unlock();
      }
    } // synchronised(parent)
    
    if (doClose)
    {
      close(); //Never call close() from within synchronized (this)!!
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#destinationMatches(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
   */
  public boolean destinationMatches(DestinationHandler destinationHandlerToCompare,
                                    JSConsumerManager consumerDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "destinationMatches", new Object[] {destinationHandlerToCompare, consumerDispatcher});

    //For remote get, at the destination localising ME, the destination attached
    //to is never an alias as the alias resolution is done at the getting ME
    boolean matches = (consumerDispatcher.getDestination() == destinationHandlerToCompare);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "destinationMatches", Boolean.valueOf(matches));

    return matches;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#getNamedDestination(com.ibm.ws.sib.processor.impl.ConsumerDispatcher)
   */
  public DestinationHandler getNamedDestination(ConsumerDispatcher cd)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getNamedDestination", cd);
      SibTr.exit(tc, "getNamedDestination",  cd.getDestination());
    }
    return cd.getDestination();
  }

  public void checkForMessages()
  {
    // ignore since we never stop or detach individual members of the group
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#resumeConsumer()
   */
  public void resumeConsumer(int suspendFlag)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resumeConsumer", this);
    
    ArrayList<AORequestedTick> satisfiedTicks = null;

    synchronized(parent) // AOStream
    {
      try
      {
        // Take the lock
        this.lock();
        try
        {    
          if (_consumerSuspended)
          {
            // clear the bit provided in the _suspendFlags
            _suspendFlags &= ~suspendFlag;
      
            if (_suspendFlags == 0) // No flags set so resume the consumer
            {
              _consumerSuspended = false;
      
              // If the consumer is still active (started) we need
              // to kickstart the consumer back into life to check for more
              // messages
              if(!closed)
                satisfiedTicks = processQueuedMsgs(null);
            }
          }
        }
        finally
        {
          this.unlock();
        }   
      }
      catch(SINotPossibleInCurrentConfigurationException e)
      {
        // No FFDC code needed
        notifyException(e);
      }
      
      if (satisfiedTicks!=null)
      {
        // inform parent about satisfied ticks - outside lock
        int length = satisfiedTicks.size();
        for (int i = 0; i < length; i++)
        {
          AORequestedTick aotick = (AORequestedTick) satisfiedTicks.get(i);
          parent.satisfiedRequest(aotick.tick, aotick.getMessage());
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resumeConsumer");
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#suspendConsumer()
   */
  public boolean suspendConsumer(int suspendFlag)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "suspendConsumer", Integer.valueOf(suspendFlag));
    
    boolean didSuspendConsumer;
    // Lock down the consumerpoint and suspend it
    this.lock();
    try
    {
      if (!isConsumerSuspended(suspendFlag)) // Is the consumer already suspended
      {
        _consumerSuspended = true;
        _suspendFlags |= suspendFlag;
        this.isready = false;
        ck.notReady();
        didSuspendConsumer = true;
      }
      else
      {
        didSuspendConsumer = false;
      }
    }
    finally
    {
      this.unlock();
    }
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "suspendConsumer", didSuspendConsumer);
    return didSuspendConsumer;
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#getConsumerManager()
   */
  public ConsumerManager getConsumerManager()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerManager");
      SibTr.exit(tc, "getConsumerManager", dispatcher);
    }
    return dispatcher;
  }

  public void closeSession(Throwable e)
  throws SIConnectionLostException, SIResourceException, SIErrorException
  {
    //no-op
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint#ignoreInitialIndoubts()
   */
  public boolean ignoreInitialIndoubts() {
    // no-op
    return true;
  }
  
  public void implicitClose(SIBUuid12 deletedUuid, SIException exception, SIBUuid8 qpoint) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "implicitClose", new Object[] { deletedUuid, exception });
    
    boolean closed = false;
    if (deletedUuid!=null)
      // Close due to delete
      closed = ck.close(ConsumerKey.CLOSED_DUE_TO_DELETE, qpoint);
    else if (exception != null)
      // Close due to ME unreachable
      closed = ck.close(ConsumerKey.CLOSED_DUE_TO_RECEIVE_EXCLUSIVE, qpoint);
    else
      // Close due to receive exclusive
      closed = ck.close(ConsumerKey.CLOSED_DUE_TO_ME_UNREACHABLE, qpoint);
    
    if (closed)
    {
      close();
      
      // notify the parent since this is an asynchronous close
      parent.removeConsumerKey(selectionCriteriasAsString, this);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "implicitClose"); 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint#isConsumerSuspended()
   */
  public boolean isConsumerSuspended(int suspendFlag)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isConsumerSuspended", Integer.valueOf(suspendFlag));
    
    boolean suspended;
    this.lock();
    try 
    {
      if (suspendFlag == 0)
      {
        // the passed in suspend flag was 0 this means the caller wants to know if the consumer
        // is suspended for any reason
        suspended = _consumerSuspended;
      }
      else if (_suspendFlags == 0)  // No flags set so it must not be suspended
      {
        suspended = false;
      }
      else if ((_suspendFlags & suspendFlag) == suspendFlag)
      {
        //We were asked about certain type of suspended and we are suspended for that reason
        suspended = true;
      }
      else 
      {
        suspended = false;
      }
    }
    finally
    {
      this.unlock();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isConsumerSuspended", Boolean.valueOf(suspended));
    
    return suspended;
  }

  public ConsumableKey getConsumerKey() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerKey" );
      SibTr.exit(tc, "getConsumerKey", ck);
    }
    return ck;
  }
  
  public boolean isGatheringConsumer() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isGatheringConsumer");
      SibTr.exit(tc, "isGatheringConsumer", Boolean.valueOf(gatherMessages));
    }
    return gatherMessages;
  }
}
