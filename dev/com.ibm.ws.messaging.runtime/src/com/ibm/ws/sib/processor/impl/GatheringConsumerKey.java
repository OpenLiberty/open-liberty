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

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

public class GatheringConsumerKey extends AbstractConsumerKey
{

  private static final TraceComponent tc =
    SibTr.register(
      GatheringConsumerKey.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);



  // NLS for component
//private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  HashMap<SIBUuid8,ConsumableKey> consumerKeys;
  private JSConsumerManager dispatcher;
  private DispatchableConsumerPoint consumerPoint;
  private SIBUuid12 connectionUuid;
  private boolean forwardScanning;
  
  // Last DME we chose to receive a msg from, for round robin algorithm
  private SIBUuid8 lastDMERequestedUuid;
    
  // flag to indicate this gck has been closed
  boolean closed = false;
  
  // are all keys ready
  boolean ready = false;
  
  // This value is time the longest request timeout received so far by this
  // consumer will expire. If an ME appears in the future we need to request a msg with the 
  // remainder of this time.
  private long outstandingRequestExpiryTime = LocalConsumerPoint.NO_WAIT;
  
  // remember the recoverable flag for when an ME reconnects
  private Reliability unrecoverable;


  public GatheringConsumerKey(DispatchableConsumerPoint consumerPoint,
                              JSConsumerManager dispatcher,
                              HashMap<SIBUuid8, ConsumableKey> consumerKeys,
                              SelectionCriteria criteria,
                              SIBUuid12 connectionUuid,
                              boolean forwardScanning)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "GatheringConsumerKey",
      new Object[]{consumerPoint, dispatcher, consumerKeys, criteria, connectionUuid, Boolean.valueOf(forwardScanning)});

    this.dispatcher = dispatcher;
    this.consumerPoint = consumerPoint;
    this.connectionUuid = connectionUuid;
    this.forwardScanning = forwardScanning;
    this.consumerKeys = consumerKeys;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "GatheringConsumerKey", this);
  }

  public void detach()
    throws SIResourceException, SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "detach");

    Iterator<ConsumableKey> it = null;
    synchronized(this)
    {
      it = ((HashMap<SIBUuid8,ConsumableKey>)consumerKeys.clone()).values().iterator();
    }
    
    // Cant hold the lock while we are detaching 
    while (it.hasNext()) 
      it.next().detach();
    
    
    // Remove this GCK from the GCD
    dispatcher.detachConsumerPoint(this);

    // Remove us from any group we are a member of
    if(keyGroup != null)
      keyGroup.removeMember(this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "detach");
  }

  public synchronized void start()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "start");

    Iterator<ConsumableKey> it = consumerKeys.values().iterator();
    while (it.hasNext()) 
      it.next().start();
    
    if(keyGroup != null)
      keyGroup.startMember();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "start");
  }

  public DispatchableConsumerPoint getConsumerPoint()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getConsumerPoint");
      SibTr.exit(this, tc, "getConsumerPoint", consumerPoint);
    }

    return consumerPoint;
  }

  public synchronized LockingCursor getGetCursor(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getGetCursor", msg);

    SIBUuid8 meUuid = msg.getLocalisingMEUuid();
    LockingCursor getCursor =
      consumerKeys.get(meUuid).getGetCursor(msg);
    
    if(keyGroup != null)
      getCursor = keyGroup.getGetCursor(msg);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "getGetCursor", getCursor);
    return getCursor;
  }

  public synchronized void notReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "notReady");

    //get the readyConsumer list lock
    synchronized (dispatcher.getDestination().getReadyConsumerPointLock())
    {
      Iterator<ConsumableKey> it = consumerKeys.values().iterator();
      while (it.hasNext()) 
        it.next().notReady();
      
      if(keyGroup != null)
        keyGroup.notReady();
      
      ready = false;
      
      outstandingRequestExpiryTime = LocalConsumerPoint.NO_WAIT;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "notReady");
  }

  public synchronized void ready(Reliability unrecoverable) throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "ready", unrecoverable);

    //get the readyConsumer list lock
    synchronized (dispatcher.getDestination().getReadyConsumerPointLock())
    {
      Iterator<ConsumableKey> it = consumerKeys.values().iterator();
      while (it.hasNext()) 
        it.next().ready(unrecoverable);
      
      if(keyGroup != null)
        keyGroup.ready(null);
      
      ready = true;
      
      // remember the unrecoverable flag
      this.unrecoverable = unrecoverable;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "ready");
  }

  public synchronized void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "stop");

    Iterator<ConsumableKey> it = consumerKeys.values().iterator();
    while (it.hasNext()) 
      it.next().stop();
    
    if(keyGroup != null)
      keyGroup.stopMember();

    // Create a new consumerKey to represent
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "stop");

  }

  /**
   * Mark this consumer as waiting for a message. This is only of interest
   * when consuming remotely. In this case we also reserve the right to
   * modify the suggested timeout.
   *
   * @param timeout  Supplied timeout
   * @param modifyTimeout  Indicate whether the method should adjust the given timeout
   */
  public long waiting(long timeout, boolean modifyTimeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "waiting", new Object[]{Long.valueOf(timeout), Boolean.valueOf(modifyTimeout)});

    // Of the timout values returned to us by each queue point return the longest to our caller
    long rc = timeout;

    Iterator<ConsumableKey> it = null;
    synchronized(this)
    {
      it = ((HashMap<SIBUuid8,ConsumableKey>)consumerKeys.clone()).values().iterator();
      
      // If an ME in the cluster was unavailable when this request was made, we
      // need to remember the request timeout in order to forward a request on
      // when the ME appears (unless the request time has expired).
      // Therefore we update the longestRequestTime that occurred while the ME was
      // away.
      if (timeout == LocalConsumerPoint.INFINITE_WAIT)
        outstandingRequestExpiryTime = timeout;
      else if (timeout != LocalConsumerPoint.NO_WAIT)
      {
        // Work out when this request would have expired
        long expiryTime = System.currentTimeMillis() + timeout;
        
        // If we dont have a current refillTime that extends to the period of this request
        // then set the new refillTime
        if (outstandingRequestExpiryTime != LocalConsumerPoint.INFINITE_WAIT && outstandingRequestExpiryTime < expiryTime)
          outstandingRequestExpiryTime = expiryTime;
      }
    }
    
    // We dont want to hold the gathering consumer key lock while we call waiting as
    // this creates contention betwen the sending and receiving threads.
    // E.g A receive can deadlock with a RCD.resolve with the gck/streamStatus locks.
    while (it.hasNext()) 
    {
      final LocalQPConsumerKey key = (LocalQPConsumerKey)it.next();
      if (key.isRefillAllowed())
      {
        // Iterate over all our keys. If we are in the process of refilling or have
        // decided we need to refill, then allow waiting to be called so we can 
        // either issue a get or register our interest in a msg for when the current get expires.
        final long to = key.waiting(timeout, modifyTimeout);
        if (to > rc) rc = to;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "waiting", rc);

    return rc;
  }

  /**
   * Determine if this key is ready
   * No need to synchronize as this should be called under the readyConsumerPointLock
   */
  public boolean isKeyReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isKeyReady");
    
    boolean returnValue = false;
    if(keyGroup == null)
      returnValue = ready;
    else
      //check if the ordering context is ready
      returnValue = keyGroup.isKeyReady();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isKeyReady", Boolean.valueOf(returnValue));

    return returnValue;
  }

  // No-op for gathering 
  public synchronized void markNotReady() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "markNotReady");

    // No-op

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "markNotReady");
  }

  /**
   * Return the consumer's forward scanning setting
   */
  public boolean getForwardScanning()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getForwardScanning");
      SibTr.exit(this, tc, "getForwardScanning", forwardScanning);
    }
    return forwardScanning;
  }

  /**
   * Return the consumer's connection Uuid
   */
  public SIBUuid12 getConnectionUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getConnectionUuid");
      SibTr.exit(this, tc, "getConnectionUuid", connectionUuid);
    }
    return connectionUuid;
  }

  /**
   * Returns the consumerDispatcher.
   * @return ConsumerDispatcher
   */
  public JSConsumerManager getConsumerManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getConsumerPoint");
      SibTr.exit(this, tc, "getConsumerPoint", dispatcher);
    }

    return dispatcher;
  }

  /*
   * The following logic should be implemented by a combination of this and the "waiting" methods :
   * 
   * + On "getMessageLocked" 
   *  Iterate over the "bucket"s for each remote queue point
   *    if (hasMsg) - return msg
   *    else
   *      if (hasOutstandingRefill) - return no msg
   *      else - issue new refill request
   *    
   *  + On receipt of a VALUE msg for a refill request (i.e. refill satisfied)
   *  msg is now available for consumers
   *  
   * + On receipt of a COMPLETED msg for a refill request (i.e. refill cancelled)
   *  if (requestArrivedInMeantime) - issue new request for (requestTime - currentTime)
   *  
   *       
   */

  public SIMPMessage getMessageLocked() throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "getMessageLocked");

    SIMPMessage msg = null;
    SIBUuid8 currentUuid = null;
   
    synchronized(this)
    {
      Iterator<SIBUuid8> it = consumerKeys.keySet().iterator();
      
      // We either do a round robin of the queue points OR we pick the local one a
      // certain percentage of the time.
      int localQPWeighting = dispatcher.getMessageProcessor().getCustomProperties().get_gathering_local_weighting();
      
      if (Math.random() < ((double)localQPWeighting/100.0))
      {
        // Attempt to get a message from the local queue point
        currentUuid = dispatcher.getMessageProcessor().getMessagingEngineUuid();
        msg = (SIMPMessage)((LocalQPConsumerKey)consumerKeys.get(currentUuid)).getDefaultGetCursor().next();       
      }
      
      // If its time to choose a remote qp, or we looked on the local one but couldnt find one,
      // revert to round robin on all the queue points (note this does include the local one)
      if (msg == null)
      {
        boolean startedSearch = false;
        int iterations = 0;
        int searchCount = 0;
        int numberOfKeys = consumerKeys.keySet().size();
        
        // Round robin the DMEs
        while(iterations < numberOfKeys) 
        {
          if (it.hasNext())
            currentUuid = it.next(); // Get next uuid
          else
            currentUuid = (it = consumerKeys.keySet().iterator()).next(); // End of list so get first uuid         
          
          searchCount++;
          
          if (startedSearch)
          {  
            // We now found the last queue point we looked on so start the search proper
            iterations++;
            
            LocalQPConsumerKey key = (LocalQPConsumerKey)consumerKeys.get(currentUuid);
            msg = (SIMPMessage)key.getDefaultGetCursor().next();
            if (msg != null) 
            {
              // Save the last queue point we received from.
              lastDMERequestedUuid = currentUuid;
              break;
            }
            else
              key.initiateRefill();
              
          }
          
          // We have found the last dme we requested so start looking for msgs from the next uuid
          // in the list onwards.
          if (lastDMERequestedUuid == null ||                                 // First time round
              searchCount == numberOfKeys ||                                  // Last DME used no longer in list
              (!startedSearch && currentUuid.equals(lastDMERequestedUuid)))   // Found last DME 
            startedSearch = true;          
        }
      }
    }
    
    // Set the source uuid into the msg
    if (msg != null)
      msg.setLocalisingME(currentUuid); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "getMessageLocked", new Object[] {msg, currentUuid});
    return msg;
  }

  public boolean close(int closedReason, SIBUuid8 qpoint) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close", new Object[] { Integer.valueOf(closedReason), qpoint});
    
    ConsumableKey ck = null;
    synchronized(this)
    {
      // Remove the consumerkey from our list
      ck = consumerKeys.remove(qpoint);
      // If this was the uuid we were attached to then close off the gathering consumer
      closed = consumerKeys.isEmpty();
      
      // The reason for closing the gathering consumer was the reason that the last 
      // partition got removed for so set the reason if this qpoint was the last to be
      // removed.
      if (closed)
        this.closedReason = closedReason;        
    }
    
    if (ck!=null)
    {
      // Close off the individual consumer.
      ck.close(closedReason, null);
      
      // Take the lock on the consumerpoint and set it to notReady
      consumerPoint.lock();
      try
      {
        ck.stop();
        ck.notReady();
      }
      finally
      {
        consumerPoint.unlock();
      }
      
      // Finally detach the consumer key 
      try
      {
        ck.detach();
      }
      catch (SIException e)
      {
        // No FFDC code needed
        // Should never get here - we only throw exceptions from detach if we are pubsub
      }
                 
    }
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "close", closed);
    
    return closed;
  }
  
  public boolean isSpecific() {
    // TODO Auto-generated method stub
    return false;
  }
  
  // Called when an ME in the cluster becomes reachable. We have kept
  // a record of any outstanding requests which we now issue to the new ME.
  // Also add the consumer key back into the list.
  public void reattachConsumer(SIBUuid8 uuid, ConsumableKey ck)
  throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "reattachConsumer", new Object[] {uuid, ck});

    long timeout = LocalConsumerPoint.NO_WAIT;
    
    synchronized(this)
    {
      if (!closed)
      {
        // Set the new consumer key to be the same ready state as the rest 
        // of the gathering consumer        
        if (isKeyReady())
        {
          //get the readyConsumer list lock
          synchronized (dispatcher.getDestination().getReadyConsumerPointLock())
          {
            ck.ready(unrecoverable);
          }
        }
        
        consumerKeys.put(uuid, ck);
  
        // Work out the expiry time if any of the outstanding requests
        if (outstandingRequestExpiryTime != LocalConsumerPoint.NO_WAIT)
        {
          timeout = outstandingRequestExpiryTime;
          
          if (timeout != LocalConsumerPoint.INFINITE_WAIT)
            timeout = outstandingRequestExpiryTime - System.currentTimeMillis(); 
        }
      }
    }
      
    // outside of the lock - reissue the request
    if (timeout == LocalConsumerPoint.INFINITE_WAIT || timeout > 0)
    {
      ((LocalQPConsumerKey)ck).initiateRefill();
      ck.waiting(timeout, true);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "reattachConsumer");
  }
   
  public synchronized boolean isAttached(SIBUuid8 uuid) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "isAttached",uuid);
    
    boolean isAttached = false;
    if (consumerKeys.keySet().contains(uuid))
      isAttached = true;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "isAttached", Boolean.valueOf(isAttached));
    return isAttached;
  }

}
