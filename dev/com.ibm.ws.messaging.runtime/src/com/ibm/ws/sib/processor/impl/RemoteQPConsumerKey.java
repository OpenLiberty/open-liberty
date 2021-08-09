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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.RefillKey;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The subclass of ConsumerKey that is used at the RME (for remote get)
 */
public final class RemoteQPConsumerKey extends LocalQPConsumerKey implements RemoteDispatchableKey, RefillKey
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      RemoteQPConsumerKey.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   final SelectionCriteria[] criteria; // always an array of length 1

  private final RemoteConsumerDispatcher rcd;

  //prefetching stuff. reads and writes are synchronized on this

  final boolean readAhead; // no prefetching is done if !readAhead

  /** the current size of the prefetch window. Computed using the consumptionInterval */
  private int prefetchWindowSize;

  /** the average interval between consuming two messages if messages were immediately available to the consumer */
  private int consumptionInterval;

  /** the parameter for the low-pass filter used to average the consumptionInterval */
  static final double alpha = 0.5;

  /** The last time the consumer became not ready. When the consumer transitions to ready, say at time T,
   * we use T - lastNotReadyTime as an estimate of the consumption interval (assuming the last time the
   * consumer became not ready was because the consumer got a message to consume).
   */
  private long lastNotReadyTime;

  /** Whether the lastNoteReadyTime should be used. Set to true when update lastNotReadyTime,
   *  and set to false when go to ready state */
  private boolean usable;

  /** The count of outstanding gets that have infinite timeout. These can be used to satisfy a consumer when it
   * goes into waiting state (by calling waiting()), since such get requests are guaranteed to be eventually
   * satisfied or an exception notification is given to the consumer.
   */
  private int countOfOutstandingInfiniteTimeoutGets;

  /**
   * The count of messages in the itemStream that were requested on behalf of this consumer, but are currently in
   * unlocked state.
   */
  private int countOfUnlockedMessages;
  
  /**
   * If we are part of a gathering consumer then we use the waiting method to drive
   * refilling of the queue point at certain times. If a refill is currently in progress
   * then no other requests will be sent from this remote key until the refill has
   * been satisfied.
   */
  private boolean isRefilling;
  
  /** If we are being used as part of a gathering consumer key, we may need to 
   *  initiate a refill of a queue point if there are no messages on it. This
   *  state tells us we need to initiate a refill
   */
  protected boolean doRefill;
  
  /**
   * This is the tick of the request that is currently outstanding for a refill.
   * If a msg or Completed comes back for this tick we can allow normal requests to resume.
   */
  private long refillTick = AnycastInputHandler.INVALID_TICK;
  
  /**
   * When we send a refill we dont allow other request to be sent. Therefore while we are
   * refilling we need to keep a track of any requests that get sent in the meantime.
   * When the refill msg comes in we can check if any requests need to be driven and if
   * so we send another refill request for the time of the longest request that came in. 
   */
  private long refillTime = LocalConsumerPoint.NO_WAIT;

  
  /**
   * Constructor
   */
  RemoteQPConsumerKey(
    DispatchableConsumerPoint consumerPoint,
    ConsumerDispatcher consumerDispatcher,
    SelectionCriteria criteria,
    SIBUuid12 connectionUuid, boolean readAhead,
    boolean forwardScanning)
    throws SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException
  {
    super(consumerPoint, consumerDispatcher, criteria, connectionUuid, forwardScanning, null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteQPConsumerKey",
        new Object[]{consumerPoint, consumerDispatcher, criteria, connectionUuid, Boolean.valueOf(readAhead), Boolean.valueOf(forwardScanning)});

    this.criteria = new SelectionCriteria[1];
    if (super.isSpecific())
    {
      this.criteria[0] = criteria;
    }
    else
    {
      SelectionCriteria theCriteria = null;
      // The following maps to no selector
      theCriteria = consumerDispatcher.
                      getMessageProcessor().
                      getSelectionCriteriaFactory().
                      createSelectionCriteria();

      this.criteria[0] = theCriteria;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "RemoteQPConsumerKey", this);
    }

    this.rcd = (RemoteConsumerDispatcher) consumerDispatcher;
    this.readAhead = readAhead;

    synchronized (this)
    {
      this.prefetchWindowSize = 0; // set it to 0 to begin with
      //  set it to a large value to begin with, so prefetch doesn't begin immediately
      this.consumptionInterval = 2 * consumerDispatcher.getMessageProcessor().getCustomProperties().get_max_interval_for_prefetch();
      this.lastNotReadyTime = 0;
      this.usable = false; // since lastNotReadyTime is not usable yet

      this.countOfOutstandingInfiniteTimeoutGets = 0;
      this.countOfUnlockedMessages = 0;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.ws.sib.processor.impl.ConsumerKey#waiting(long, boolean)
   */
  public long waiting(long timeout, boolean modifyTimeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "waiting", new Object[] { new Long(timeout),  Boolean.valueOf(modifyTimeout) });

    boolean processGets = true;
    synchronized(this)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "refilling: " + Boolean.valueOf(isRefilling) +
                        "doRefill: " + Boolean.valueOf(doRefill));
      if (isRefilling)
      {
        processGets = false;
        
        // A request came in while we were busy doing refills. Therfore we
        // record the timeout (if we dont have a longer one) in case we have to redrive
        // a new refill request when the current one is satisfied.
        if (timeout == LocalConsumerPoint.INFINITE_WAIT)
          refillTime = timeout;
        else if (timeout != LocalConsumerPoint.NO_WAIT)
        {
          // Work out when this request would have expired
          long expiryTime = System.currentTimeMillis() + timeout;
          
          // If we dont have a current refillTime that extends to the period of this request
          // then set the new refillTime
          if (refillTime != LocalConsumerPoint.INFINITE_WAIT && refillTime < expiryTime)
            refillTime = expiryTime;
        }
      }
      else
      {
        if (doRefill) // Has a refill been inititated
        {
          isRefilling = true;
          doRefill = false;
        }
      }        
    }
    
    if (processGets)
    {     
      try
      {
        long protocolTimeout;
        // Either modify the timeout with a roundTrip time or not.
        // But always convert the protocol time into a SIMP format
        // (from the LCP format)
        if (modifyTimeout)
        {
          if (timeout == LocalConsumerPoint.INFINITE_WAIT)
          { // no need to modify timeout
    
            // we are using a different constant for the protocol
            protocolTimeout = SIMPConstants.INFINITE_TIMEOUT;
          }
          else if (timeout == LocalConsumerPoint.NO_WAIT)
          {
            timeout = getRoundTripTime(); // the consumer needs to wait atleast
                                          // for this time
            protocolTimeout = 0L; // this ensures that the DME knows not to wait
          }
          else
          {
            long rtt = getRoundTripTime();
            protocolTimeout = timeout + rtt;
            timeout = protocolTimeout;
          }
        }
        else
        {
          if (timeout == LocalConsumerPoint.INFINITE_WAIT)
            protocolTimeout = SIMPConstants.INFINITE_TIMEOUT;
          else if (timeout == LocalConsumerPoint.NO_WAIT)
            protocolTimeout = 0L; // this ensures that the DME knows not to wait
          else
            protocolTimeout = timeout;
        }
        
        boolean callIssueGet = true;
        if (readAhead || protocolTimeout == SIMPConstants.INFINITE_TIMEOUT)
        {
          synchronized (this)
          {
            if (countOfOutstandingInfiniteTimeoutGets > 0)
            {
              callIssueGet = false; // already scheduled requests
            }
            else
            {
              if (keyGroup == null)
              {
  
                // will issue get, so update counts within the synchronized block
                if (protocolTimeout == SIMPConstants.INFINITE_TIMEOUT)
                {
                  countOfOutstandingInfiniteTimeoutGets++;
  
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets++ " + countOfOutstandingInfiniteTimeoutGets);
                }
              }
              // else do nothing, since gets issued on the key group don't count
              // towards
              // countOfOutstandingInfiniteTimeoutGets
            }
          }
          if (readAhead)
              tryPrefetching(); // call try prefetching since window may have
                                // increased
        }
  
        if (callIssueGet)
        {
          AIStreamKey key;
          if (keyGroup == null)
            key = rcd.issueGet(criteria, protocolTimeout, this, this);
          else
            key = ((RemoteQPConsumerKeyGroup) keyGroup).issueGet(protocolTimeout, this);
         
          if (key == null)
          {
            // decrement the counts. this will be very rare!
            synchronized (this)
            {
              if ((keyGroup == null)
                  && (protocolTimeout == SIMPConstants.INFINITE_TIMEOUT))
              {
                countOfOutstandingInfiniteTimeoutGets--;
                
  
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets-- " + countOfOutstandingInfiniteTimeoutGets);
              }
            }
            // log and throw exception to consumer
            SIResourceException e = new SIResourceException(nls
                .getFormattedMessage("ANYCAST_STREAM_UNAVAILABLE_CWSIP0481",
                    new Object[] { rcd.getDestName(),
                        rcd.getLocalisationUuid().toString() }, null));
            FFDCFilter.processException(e,
                "com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey.waiting",
                "1:357:1.47.1.26", this);
            SibTr.exception(tc, e);
  
            consumerPoint.notifyException(e);
          }
        }
      }
      catch (SIResourceException e)
      {
        // FFDC
        FFDCFilter.processException(e,
            "com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey.waiting",
            "1:369:1.47.1.26", this);
  
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
          SibTr.exception(tc, e);
  
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "waiting", "SIErrorException");
        throw new SIErrorException(e);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "waiting", Long.valueOf(timeout));

    return timeout;
  }

  /** overiding the method in the superclass */
  public void notReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "notReady");

    updateLastNotReadyTime();
    super.notReady();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "notReady");
  }

  /** overiding the method in the superclass */
  public void ready(Reliability unrecoverability) throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "ready", unrecoverability);

    updateConsumptionInterval();
    super.ready(unrecoverability);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "ready");
  }

  /**
   * Update the time when the consumer became not ready
   */
  protected final void updateLastNotReadyTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "updateLastNotReadyTime");
    if (readAhead)
    { // only relevant when prefetching
      synchronized (this)
      {
        // it is possible that both the CP and the RCD try to transition to !ready concurrently.
        // we only remember the time from the first transition.
        if (!usable)
        {
          lastNotReadyTime = System.currentTimeMillis();
          usable = true;
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "updateLastNotReadyTime");
  }

  protected final void updateConsumptionInterval()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "updateConsumptionInterval");
    if (readAhead)
    {
      synchronized (this)
      {

        if (usable)
        { // only update the prefetchWindowSize if usable==true
          usable = false;
          long latestConsumptionInterval = System.currentTimeMillis() - lastNotReadyTime;
          // apply a standard low-pass filter to compute running average
          consumptionInterval = (int) ((alpha * latestConsumptionInterval) + (1 - alpha) * consumptionInterval);
          if (consumptionInterval == 0)
          {
            prefetchWindowSize = rcd.getMessageProcessor().getCustomProperties().get_max_prefetch_window();
          }
          else if (consumptionInterval > rcd.getMessageProcessor().getCustomProperties().get_max_interval_for_prefetch())
          {
            prefetchWindowSize = 0;
          }
          else if (consumptionInterval > getRoundTripTime())
          {
            // note that consumptionInterval is < MAX_INTERVAL_FOR_PREFETCH. Setting the prefetchWindowSize to
            // 1 allows us to prefetch a message in the time interval between the last time a message was
            // consumed and the next time the consumer goes to ready state.
            prefetchWindowSize = 1;
          }
          else
          { // consumptionInterval > 0 and < RTT
            prefetchWindowSize = (int) Math.ceil((double)getRoundTripTime()/(double)consumptionInterval);
            if (prefetchWindowSize > rcd.getMessageProcessor().getCustomProperties().get_max_prefetch_window())
              prefetchWindowSize = rcd.getMessageProcessor().getCustomProperties().get_max_prefetch_window();
          }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "readAhead change: prefetchWindowSize(" + prefetchWindowSize + ") consumptionInterval(" + consumptionInterval + ")");
      } // end synchronized (this)
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "updateConsumptionInterval");
  }

  /**
   * message received from the DME corresponding to a get request issued by this consumer.
   * Note that this method will never be called on messages received due to gets issued by the
   * RemoteQPConsumerKeyGroup
   * @param key
   */
  protected final void messageReceived(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "messageReceived", key);
    
    long timeout = refillTime;
    boolean reissueGet = false;
    
    synchronized (this)
    {
      countOfUnlockedMessages++;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "readAhead change: countOfUnlockedMessages++ " + countOfUnlockedMessages);

      if (key.getOriginalTimeout() == SIMPConstants.INFINITE_TIMEOUT)
      {
        countOfOutstandingInfiniteTimeoutGets--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets-- " + countOfOutstandingInfiniteTimeoutGets);
      }      
      
      // If the msg that came in was for a refill request then we can allow normal
      // operations to resume or redrive a new request for any attempted requests while
      // we were busy
      if (isRefilling && key.getTick() == refillTick)
      {
        // redrive a request that encompasses any requests that came in while we
        // were busy trying to refill.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "we are refilling, refillTime: " + Long.valueOf(refillTime));
        
        isRefilling = false;
        refillTick = AnycastInputHandler.INVALID_TICK;
        
        if (refillTime != LocalConsumerPoint.NO_WAIT)
        {          
          if (timeout != LocalConsumerPoint.INFINITE_WAIT)
            timeout = refillTime - System.currentTimeMillis();
          
          if (timeout == LocalConsumerPoint.INFINITE_WAIT || timeout > 0)
            reissueGet = true;
        }
      }              
    } // end of synch
    
    if (reissueGet) // outside of synch block
    {
      initiateRefill();
      waiting(timeout, true);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "messageReceived");
  }
  
  /**
   * completed tick received from the RME corresponding to a get request issued by this consumer.
   * Note that this method is called only when the RemoteConsumerDispatcher does not hide the completed
   * by reissuing the get.
   * This method will never be called for messages received due to gets issued by the
   * RemoteQPConsumerKeyGroup
   * @param key
   */
  protected final void completedReceived(AIStreamKey key, boolean reissueGet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "completedReceived", new Object[] {key, Boolean.valueOf(reissueGet)});
    completedReceivedNoPrefetch(key, reissueGet);
    try
    {
      if (readAhead)
        tryPrefetching();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey.completedReceived",
        "1:568:1.47.1.26",
        this);
      SibTr.exception(tc, e);
      // no need to throw this exception, since its only a failure in prefetching
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "completedReceived");
  }

  /**
   * This method will never be called for messages received due to gets issued by the
   * RemoteQPConsumerKeyGroup.
   * @param key
   */
  protected final void completedReceivedNoPrefetch(AIStreamKey key, boolean reissueGet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "completedReceivedNoPrefetch", new Object[] {key, Boolean.valueOf(reissueGet)});

    boolean initiateRefill = false;
    long timeout = key.getOriginalTimeout();
    
    synchronized (this)
    {      
      if (timeout == SIMPConstants.INFINITE_TIMEOUT)
      {
        countOfOutstandingInfiniteTimeoutGets--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets-- " + countOfOutstandingInfiniteTimeoutGets);
      }
      
      // If the completed that came in was for a refill request then we can allow normal
      // operations to resume
      if (isRefilling)
        checkAndResetRefillState(key.getTick());
      
      // redrive a request that encompasses any requests that came in while we
      // were busy trying to refill.
      if (timeout != 0L) // no wait
      {          
        if (timeout != SIMPConstants.INFINITE_TIMEOUT)
        {
          long currTime = System.currentTimeMillis();
          long expectedResponseTime = currTime + rcd.getRoundTripTime();
          long timeoutTime = key.getIssueTime() + key.getOriginalTimeout();
          if (expectedResponseTime < timeoutTime)
            timeout = timeoutTime - currTime;
          else
            timeout = 0L;
        }
                 
      }
    } // end of sync
    
    // Reissue request if we have a suitable timeout (infinite or positive)
    if (reissueGet && (timeout == SIMPConstants.INFINITE_TIMEOUT || timeout > 0))
    {
      if (initiateRefill)
        initiateRefill();
      waiting(timeout, false);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "completedReceivedNoPrefetch");
  }

  /**
   * Message that was requested by this consumer has been locked.
   * Note that it could have been locked by some other consumer.
   * This method will never be called for messages received due to gets issued by the
   * RemoteQPConsumerKeyGroup
   * @param key
   */
  public final void messageLocked(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "messageLocked", key);
    synchronized (this)
    {
      countOfUnlockedMessages--;
            
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "readAhead change: countOfUnlockedMessages-- " + countOfUnlockedMessages);
    }
    try
    {
      if (readAhead)
        tryPrefetching();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey.messageLocked",
        "1:663:1.47.1.26",
        this);
      SibTr.exception(tc, e);
      // no need to throw this exception, since its only a failure in prefetching
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "messageLocked");
  }

  /**
   * Message that was requested by this consumer has been unlocked.
   * This method will never be called for messages received due to gets issued by the
   * RemoteQPConsumerKeyGroup
   * @param key
   */
  public final void messageUnlocked(AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "messageUnlocked", key);
    synchronized (this)
    {
      countOfUnlockedMessages++;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "readAhead change: countOfUnlockedMessages++ " + countOfUnlockedMessages);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "messageUnlocked");
  }

  /**
   * Internal method. See if we need to prefetch more messages, and if yes, do the prefetch
   */
  private final void tryPrefetching() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "tryPrefetching");
    int toPrefetchCount = 0; // count of gets to issue
    synchronized (this)
    {
      if (!detached)
      {
        int count = countOfOutstandingInfiniteTimeoutGets + countOfUnlockedMessages;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "readAhead check: count(" + count + ") prefetchWindowSize(" + prefetchWindowSize + ")");
            	
        if (count < prefetchWindowSize) // note that it is possible that count > prefetchWindowSize
        {
          // perhaps we should prefetch
          if ( ((prefetchWindowSize - count)/(double) prefetchWindowSize) > SIMPConstants.MIN_PREFETCH_SIZE)
          {
            toPrefetchCount = prefetchWindowSize - count;
            countOfOutstandingInfiniteTimeoutGets += toPrefetchCount;
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets+= " + countOfOutstandingInfiniteTimeoutGets);
          }
        }
      }
    }

    if (toPrefetchCount > 0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "readAhead change: toPrefetchCount " + toPrefetchCount);
        
      AIStreamKey[] keys = rcd.issueGet(criteria, toPrefetchCount, this);
      int i;
      if (keys == null)
        i = 0;
      else
        i = keys.length;
      if (i < toPrefetchCount)
      {
        // this will be very rare!

        // decrement the counts.
        synchronized (this)
        {
          countOfOutstandingInfiniteTimeoutGets -= (toPrefetchCount -i); // the first i gets were successful
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "readAhead change: countOfOutstandingInfiniteTimeoutGets-= " + countOfOutstandingInfiniteTimeoutGets);
        }
        // log and throw exception to consumer
        SIResourceException e = new SIResourceException(
          nls.getFormattedMessage("ANYCAST_STREAM_UNAVAILABLE_CWSIP0481",
                                  new Object[]{rcd.getDestName(), rcd.getLocalisationUuid().toString()},
                                  null));
        FFDCFilter.processException(e,"com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey.tryPrefetching",
            "1:755:1.47.1.26",this);
        SibTr.exception(tc, e);

        consumerPoint.notifyException(e);

      }
    } // end if (toPrefetchCount > 0)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "tryPrefetching");
  }

  public final SelectionCriteria[] getSelectionCriteria()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getSelectionCriteria");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getSelectionCriteria", criteria);

    return criteria;
  }

  public void notifyException(SIException e)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "notifyException");

    consumerPoint.notifyException(e);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "notifyException");

  }

  public boolean hasNonSpecificConsumers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "hasNonSpecificConsumers");
    boolean value = !isSpecific();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "hasNonSpecificConsumers", Boolean.valueOf(value));
    return value;
  }

  public long getRoundTripTime () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "getRoundTripTime");
    long rc = rcd.getRoundTripTime();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "getRoundTripTime", Long.valueOf(rc));
    return rc;
  }

  public boolean getReadAhead () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "getReadAhead");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "getReadAhead","rc="+readAhead);
    return readAhead;
  }
  
  /**
   * Retrieve the next message using an appropriate cursor.
   * 
   * if the classification parameter is 0 then the default (unclassified) cursor
   * will be used.
   * 
   * @param classification
   * @return
   * @throws MessageStoreException
   */
  protected SIMPMessage getMessageLocked(int classification) 
    throws MessageStoreException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getMessageLocked", Integer.valueOf(classification));
    SIMPMessage msg = null;
    if(!classifyingMessages)
      msg = (SIMPMessage)getDefaultGetCursor().next();
    else
      msg = (SIMPMessage)getGetCursor(classification).next();
    
    if (msg != null)
      msg.setLocalisingME(rcd.getLocalisationUuid());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "getMessageLocked", msg);

    return msg;
  }

  @Override
  public boolean filterMatches(AbstractItem item) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "filterMatches", item);
    
    boolean result = super.filterMatches(item);
    
    // Skip AIMsgs that are reserved for AOValues on an IME
    if (result)
    {
      // This is a RQPConsumerKey so all items should be AIMessageItems
      if (((AIMessageItem)item).isReserved())
        result = false;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "filterMatches", Boolean.valueOf(result));
    return result;
  }
  
  public synchronized void initiateRefill() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "initiateRefill", "isRefilling: " + Boolean.valueOf(isRefilling));
    
    if (!isRefilling)
    {
      refillTime = LocalConsumerPoint.NO_WAIT;
      doRefill = true;  
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "initiateRefill", Boolean.valueOf(doRefill));
  }

  @Override
  public synchronized boolean isRefillAllowed() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "isRefillAllowed");
    
    boolean refillAllowed = doRefill || isRefilling; 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "isRefillAllowed", Boolean.valueOf(refillAllowed));
    return refillAllowed;
  }

  // When refilling the queue point, we remember the last request tick we sent out so that
  // we cannot send any other requests until it is satisfied (this is how we maintain only 1 request
  // outstanding). The request tick we sent is set via this callback from the RCD.
  public synchronized void setLatestTick(long tick) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "setLatestTick", Long.valueOf(tick));
    
    refillTick = tick; 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "setLatestTick", this);
  }

  public synchronized void checkAndResetRefillState(long tick) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "resetRefillState", Long.valueOf(tick));
    
    if (refillTick == tick)
    {
      refillTick = AnycastInputHandler.INVALID_TICK;
      refillTime = LocalConsumerPoint.NO_WAIT;
      isRefilling = false;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "resetRefillState");
  }
}
